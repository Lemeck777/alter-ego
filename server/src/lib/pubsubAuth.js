import { timingSafeEqual } from "node:crypto";
import { OAuth2Client } from "google-auth-library";

/**
 * Authenticates Google Play real-time developer notifications.
 *
 * The endpoint revokes entitlements, so an unauthenticated caller who learned a purchase token could
 * cancel someone's subscription. Two accepted proofs, in order:
 *
 * 1. The OIDC bearer token Google Pub/Sub attaches to a push subscription (the recommended path).
 * 2. A shared secret in the `x-pubsub-secret` header, for setups where OIDC is not configured.
 *
 * If neither is configured the endpoint refuses every request rather than trusting the payload.
 */
const oauthClient = new OAuth2Client();

export async function verifyPubSubRequest(request) {
  const audience = process.env.PUBSUB_AUDIENCE;
  const serviceAccount = process.env.PUBSUB_SERVICE_ACCOUNT_EMAIL;
  const sharedSecret = process.env.PUBSUB_SHARED_SECRET;

  if (audience && serviceAccount) {
    const header = request.headers.authorization ?? "";
    if (!header.startsWith("Bearer ")) return { ok: false, reason: "missing_bearer_token" };
    try {
      const ticket = await oauthClient.verifyIdToken({ idToken: header.slice(7), audience });
      const payload = ticket.getPayload();
      if (payload?.email !== serviceAccount || payload?.email_verified !== true) {
        return { ok: false, reason: "unexpected_service_account" };
      }
      return { ok: true, via: "oidc" };
    } catch {
      return { ok: false, reason: "invalid_oidc_token" };
    }
  }

  if (sharedSecret) {
    const provided = request.headers["x-pubsub-secret"];
    if (typeof provided !== "string") return { ok: false, reason: "missing_shared_secret" };
    const a = Buffer.from(provided, "utf8");
    const b = Buffer.from(sharedSecret, "utf8");
    if (a.length !== b.length || !timingSafeEqual(a, b)) return { ok: false, reason: "bad_shared_secret" };
    return { ok: true, via: "shared_secret" };
  }

  return { ok: false, reason: "pubsub_auth_not_configured" };
}
