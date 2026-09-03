import { timingSafeEqual } from "node:crypto";
import { validateBundle } from "../lib/contentPolicy.js";
import { query, withTransaction } from "../db/pool.js";

/**
 * Admin publishing. Guarded by a shared secret and by the content policy: a bundle containing a
 * fabricated biological measurement or shaming language is rejected before it can be stored.
 */
/** Constant-time comparison so the admin key cannot be recovered by timing the 401. */
function constantTimeEquals(provided, expected) {
  if (typeof provided !== "string" || typeof expected !== "string") return false;
  const a = Buffer.from(provided, "utf8");
  const b = Buffer.from(expected, "utf8");
  if (a.length !== b.length) return false;
  return timingSafeEqual(a, b);
}

export default async function adminRoutes(fastify) {
  fastify.addHook("onRequest", async (request, reply) => {
    if (!request.url.startsWith("/v1/admin")) return;
    const expected = process.env.ADMIN_API_KEY;
    if (!expected) return reply.code(503).send({ error: "admin_disabled" });
    const provided = request.headers["x-admin-key"];
    if (!constantTimeEquals(provided, expected)) return reply.code(401).send({ error: "unauthorised" });
  });

  fastify.post("/v1/admin/content/publish", async (request, reply) => {
    const bundle = request.body;
    const { ok, errors } = validateBundle(bundle);
    if (!ok) return reply.code(422).send({ error: "content_policy_violation", errors });

    const existing = await query("SELECT version FROM content_bundles WHERE version = $1", [bundle.version]);
    if (existing.rowCount > 0) return reply.code(409).send({ error: "version_already_published" });

    await withTransaction(async (client) => {
      await client.query(
        `INSERT INTO content_bundles (version, payload, moment_count, claim_count, published_by, notes)
         VALUES ($1,$2,$3,$4,$5,$6)`,
        [
          bundle.version, JSON.stringify(bundle), bundle.moments.length, bundle.claims.length,
          request.headers["x-admin-user"] ?? "admin", request.headers["x-publish-notes"] ?? null,
        ],
      );

      for (const claim of bundle.claims) {
        const prior = await client.query("SELECT * FROM evidence_claims WHERE claim_id = $1", [claim.claim_id]);
        if (prior.rowCount > 0) {
          // Keep the old wording so a corrected claim can always be traced back.
          await client.query(
            "INSERT INTO evidence_claim_revisions (claim_id, previous, changed_by, reason) VALUES ($1,$2,$3,$4)",
            [claim.claim_id, JSON.stringify(prior.rows[0]), request.headers["x-admin-user"] ?? "admin", request.headers["x-publish-notes"] ?? null],
          );
        }
        await client.query(
          `INSERT INTO evidence_claims
             (claim_id, claim, topic, age_min, age_max, evidence_level, direction, source_url,
              source_title, publication_year, study_type, review_date, medical_reviewer, review_note, status)
           VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15)
           ON CONFLICT (claim_id) DO UPDATE SET
             claim = EXCLUDED.claim, topic = EXCLUDED.topic, evidence_level = EXCLUDED.evidence_level,
             direction = EXCLUDED.direction, source_url = EXCLUDED.source_url, source_title = EXCLUDED.source_title,
             publication_year = EXCLUDED.publication_year, study_type = EXCLUDED.study_type,
             review_date = EXCLUDED.review_date, medical_reviewer = EXCLUDED.medical_reviewer,
             review_note = EXCLUDED.review_note, status = EXCLUDED.status, updated_at = now()`,
          [
            claim.claim_id, claim.claim, claim.topic, claim.age_min ?? null, claim.age_max ?? null,
            claim.evidence_level, claim.direction ?? "", claim.source_url ?? null, claim.source_title ?? "",
            claim.publication_year ?? null, claim.study_type ?? "", claim.review_date,
            claim.medical_reviewer ?? "pending", claim.review_note ?? null, claim.status ?? "active",
          ],
        );
      }
    });

    return reply.code(201).send({ published: bundle.version, moments: bundle.moments.length, claims: bundle.claims.length });
  });

  /** Dry run so an editor can check a bundle before publishing it. */
  fastify.post("/v1/admin/content/validate", async (request) => validateBundle(request.body));

  fastify.get("/v1/admin/content/versions", async () => {
    const { rows } = await query(
      "SELECT version, moment_count, claim_count, published_at, published_by, notes FROM content_bundles ORDER BY version DESC LIMIT 50",
    );
    return { versions: rows };
  });

  fastify.get("/v1/admin/metrics", async () => {
    const { rows } = await query(
      `SELECT name, count(*)::int AS count, date_trunc('day', occurred_at) AS day
       FROM analytics_events
       WHERE occurred_at > now() - interval '30 days'
       GROUP BY name, day ORDER BY day DESC, count DESC`,
    );
    return { metrics: rows };
  });
}
