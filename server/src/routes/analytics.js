import { query } from "../db/pool.js";
import { MAX_EVENTS_PER_BATCH, sanitiseEvents } from "../lib/analyticsPolicy.js";

/**
 * Aggregate product analytics. The sanitising rules live in lib/analyticsPolicy.js so they can be
 * tested on their own; this route only persists what survives them.
 */
export default async function analyticsRoutes(fastify) {
  fastify.post("/v1/analytics/batch", async (request, reply) => {
    const { installId, events } = request.body ?? {};
    if (!installId || !Array.isArray(events)) return reply.code(400).send({ error: "bad_request" });
    if (events.length > MAX_EVENTS_PER_BATCH) return reply.code(413).send({ error: "batch_too_large" });

    const rows = sanitiseEvents(events);
    for (const row of rows) {
      await query(
        "INSERT INTO analytics_events (install_id, name, props, occurred_at) VALUES ($1,$2,$3,$4)",
        [installId, row.name, JSON.stringify(row.props), row.occurredAt],
      );
    }
    await query(
      "INSERT INTO users (install_id) VALUES ($1) ON CONFLICT (install_id) DO UPDATE SET last_seen_at = now()",
      [installId],
    );
    return { ok: true, accepted: rows.length };
  });
}
