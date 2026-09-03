#!/usr/bin/env node
import { readFileSync } from "node:fs";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import { pool, withTransaction } from "./pool.js";

const SEED_PATH = join(dirname(fileURLToPath(import.meta.url)), "..", "content", "seed", "bundle.json");

/**
 * Publishes the curated bundle produced by scripts/sync-content.mjs, and mirrors its evidence
 * claims into the queryable evidence table so the admin console can review them one by one.
 */
async function run() {
  const bundle = JSON.parse(readFileSync(SEED_PATH, "utf8"));

  await withTransaction(async (client) => {
    await client.query(
      `INSERT INTO content_bundles (version, payload, moment_count, claim_count, published_by, notes)
       VALUES ($1, $2, $3, $4, 'seed', 'Seeded from content/ via scripts/sync-content.mjs')
       ON CONFLICT (version) DO UPDATE SET payload = EXCLUDED.payload`,
      [bundle.version, JSON.stringify(bundle), bundle.moments.length, bundle.claims.length],
    );

    for (const claim of bundle.claims) {
      await client.query(
        `INSERT INTO evidence_claims
           (claim_id, claim, topic, age_min, age_max, evidence_level, direction, source_url,
            source_title, publication_year, study_type, review_date, medical_reviewer, review_note, status)
         VALUES ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15)
         ON CONFLICT (claim_id) DO UPDATE SET
           claim = EXCLUDED.claim, evidence_level = EXCLUDED.evidence_level,
           source_url = EXCLUDED.source_url, source_title = EXCLUDED.source_title,
           review_date = EXCLUDED.review_date, updated_at = now()`,
        [
          claim.claim_id, claim.claim, claim.topic, claim.age_min ?? null, claim.age_max ?? null,
          claim.evidence_level, claim.direction ?? "", claim.source_url ?? null, claim.source_title ?? "",
          claim.publication_year ?? null, claim.study_type ?? "", claim.review_date,
          claim.medical_reviewer ?? "pending", claim.review_note ?? null, claim.status ?? "active",
        ],
      );
    }
  });

  console.log(`seeded bundle version ${bundle.version}: ${bundle.moments.length} moments, ${bundle.claims.length} claims`);
  await pool.end();
}

run().catch((error) => {
  console.error(error);
  process.exit(1);
});
