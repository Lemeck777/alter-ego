import { query } from "../db/pool.js";
import { verifySubscription } from "../lib/playBilling.js";
import { verifyPubSubRequest } from "../lib/pubsubAuth.js";

/**
 * Purchase verification.
 *
 * The client grants access locally the moment Play reports a purchase, so this endpoint only ever
 * confirms or corrects. A verification outage must never lock a paying user out of their companion.
 */
export default async function billingRoutes(fastify) {
  fastify.post("/v1/billing/verify", async (request, reply) => {
    const { productId, purchaseToken, packageName } = request.body ?? {};
    if (!productId || !purchaseToken) return reply.code(400).send({ error: "missing_purchase_details" });

    const installId = request.headers["x-install-id"];
    if (!installId) return reply.code(400).send({ error: "missing_install_id" });

    const result = await verifySubscription({ packageName, productId, purchaseToken });
    if (!result.verified) {
      return reply.code(402).send({ isPlus: false, source: "unverified", reason: result.reason });
    }

    const user = await upsertUser(installId);
    await query(
      `INSERT INTO entitlements (user_id, is_plus, source, product_id, purchase_token, expires_at, updated_at)
       VALUES ($1, true, 'play', $2, $3, $4, now())
       ON CONFLICT (user_id) DO UPDATE SET
         is_plus = true, source = 'play', product_id = EXCLUDED.product_id,
         purchase_token = EXCLUDED.purchase_token, expires_at = EXCLUDED.expires_at, updated_at = now()`,
      [user.id, productId, purchaseToken, result.expiresAt],
    );

    return { isPlus: true, source: "play", expiresAt: result.expiresAt };
  });

  fastify.get("/v1/entitlements/me", async (request) => {
    const installId = request.headers["x-install-id"];
    if (!installId) return { isPlus: false, source: "free", expiresAt: null };
    const { rows } = await query(
      `SELECT e.is_plus, e.source, e.expires_at FROM entitlements e
       JOIN users u ON u.id = e.user_id WHERE u.install_id = $1`,
      [installId],
    );
    if (rows.length === 0) return { isPlus: false, source: "free", expiresAt: null };
    const row = rows[0];
    const expired = row.expires_at && new Date(row.expires_at) < new Date();
    return {
      isPlus: row.is_plus && !expired,
      source: expired ? "expired" : row.source,
      expiresAt: row.expires_at ? new Date(row.expires_at).toISOString() : null,
    };
  });

  /** Real-time developer notifications from Play, so a cancellation is reflected promptly. */
  fastify.post("/v1/billing/play-notification", async (request, reply) => {
    // This endpoint revokes access, so an unauthenticated caller must never reach the update below.
    const auth = await verifyPubSubRequest(request);
    if (!auth.ok) {
      request.log.warn({ reason: auth.reason }, "rejected play notification");
      return reply.code(401).send({ error: "unauthorised" });
    }
    const message = request.body?.message?.data;
    if (!message) return reply.code(204).send();
    let decoded;
    try {
      decoded = JSON.parse(Buffer.from(message, "base64").toString("utf8"));
    } catch {
      return reply.code(400).send({ error: "bad_notification" });
    }
    const token = decoded?.subscriptionNotification?.purchaseToken;
    const notificationType = decoded?.subscriptionNotification?.notificationType;
    // 3 = CANCELED, 12 = REVOKED, 13 = EXPIRED
    if (token && [3, 12, 13].includes(notificationType)) {
      await query("UPDATE entitlements SET is_plus = false, source = 'expired', updated_at = now() WHERE purchase_token = $1", [token]);
    }
    return reply.code(204).send();
  });
}

async function upsertUser(installId) {
  const { rows } = await query(
    `INSERT INTO users (install_id) VALUES ($1)
     ON CONFLICT (install_id) DO UPDATE SET last_seen_at = now()
     RETURNING id`,
    [installId],
  );
  return rows[0];
}
