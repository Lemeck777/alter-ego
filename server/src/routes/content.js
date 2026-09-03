import { query } from "../db/pool.js";

/**
 * Content delivery. The app ships with a bundle in assets, so this endpoint exists purely to push
 * corrections and additions between releases. A client that is already current gets a 304.
 */
export default async function contentRoutes(fastify) {
  fastify.get("/v1/content/bundle", async (request, reply) => {
    const since = Number(request.query?.since ?? 0);

    const { rows } = await query(
      "SELECT version, payload FROM content_bundles ORDER BY version DESC LIMIT 1",
    );
    if (rows.length === 0) return reply.code(404).send({ error: "no_bundle_published" });

    const latest = rows[0];
    if (Number.isFinite(since) && since >= Number(latest.version)) {
      return reply.code(304).send();
    }
    reply.header("Cache-Control", "public, max-age=3600");
    return latest.payload;
  });

  /** The evidence database on its own, for the app's Learn section and for external review. */
  fastify.get("/v1/content/claims", async (request) => {
    const topic = request.query?.topic;
    const { rows } = topic
      ? await query("SELECT * FROM evidence_claims WHERE status = 'active' AND topic = $1 ORDER BY claim_id", [topic])
      : await query("SELECT * FROM evidence_claims WHERE status = 'active' ORDER BY topic, claim_id");
    return { claims: rows };
  });

  fastify.get("/v1/content/claims/:claimId/history", async (request, reply) => {
    const { claimId } = request.params;
    const { rows } = await query(
      "SELECT previous, changed_at, changed_by, reason FROM evidence_claim_revisions WHERE claim_id = $1 ORDER BY changed_at DESC",
      [claimId],
    );
    if (rows.length === 0) {
      const exists = await query("SELECT 1 FROM evidence_claims WHERE claim_id = $1", [claimId]);
      if (exists.rowCount === 0) return reply.code(404).send({ error: "unknown_claim" });
    }
    return { claimId, revisions: rows };
  });
}
