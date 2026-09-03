/**
 * Server-side guard rails for curated content.
 *
 * This is the last line of defence for the product's central scientific promise: the app tracks a
 * commitment precisely and never pretends to measure the user's body. Anything that looks like a
 * fabricated biological measurement is refused at publish time, so it can never reach a device.
 */

const FAKE_MEASUREMENT_PATTERNS = [
  /sperm\s*count\s*[:=]?\s*[\d,]/i,
  /\bsperm\s*(health|quality|score)\s*[:=]?\s*\d/i,
  /testosterone\s*(is\s*)?(up|\+|increased?|boosted?)\s*(by\s*)?\d/i,
  /fertility\s*(score|increased?|up)\s*[:=]?\s*\d/i,
  /your\s+body\s+has\s+stored/i,
  /\b\d+\s*%\s*(more|increase|boost)\s*(testosterone|sperm|fertility)/i,
  /\b\d{3,}[\s,]*\d*\s*(million\s*)?sperm/i,
];

const SHAME_PATTERNS = [/\bpathetic\b/i, /\bdisgusting\b/i, /\bworthless\b/i, /\byou failed\b/i, /\bloser\b/i, /\bweakling\b/i];

const ALLOWED_EVIDENCE_LEVELS = new Set(["strong", "moderate", "limited", "tradition"]);

export function validateBundle(bundle) {
  const errors = [];
  const push = (where, message) => errors.push(`${where}: ${message}`);

  if (typeof bundle?.version !== "number") push("bundle", "version must be a number");
  if (!Array.isArray(bundle?.moments) || bundle.moments.length === 0) push("bundle", "moments must be a non-empty array");
  if (!Array.isArray(bundle?.claims)) push("bundle", "claims must be an array");

  const claimIds = new Set((bundle?.claims ?? []).map((c) => c.claim_id));

  for (const claim of bundle?.claims ?? []) {
    if (!ALLOWED_EVIDENCE_LEVELS.has(claim.evidence_level)) {
      push(`claim ${claim.claim_id}`, `evidence_level must be one of ${[...ALLOWED_EVIDENCE_LEVELS].join(", ")}`);
    }
    if (claim.evidence_level !== "tradition" && !claim.source_url) {
      push(`claim ${claim.claim_id}`, "a non-tradition claim needs a source_url");
    }
    if (!/^\d{4}-\d{2}-\d{2}$/.test(claim.review_date ?? "")) {
      push(`claim ${claim.claim_id}`, "review_date must be yyyy-mm-dd");
    }
    for (const pattern of FAKE_MEASUREMENT_PATTERNS) {
      if (pattern.test(claim.claim)) push(`claim ${claim.claim_id}`, "claim reads as a fabricated personal measurement");
    }
  }

  for (const moment of bundle?.moments ?? []) {
    const text = (moment.lines ?? []).join(" ");
    if (!Array.isArray(moment.lines) || moment.lines.length === 0 || moment.lines.length > 3) {
      push(`moment ${moment.id}`, "must have between one and three lines");
    }
    for (const line of moment.lines ?? []) {
      if (typeof line !== "string" || line.length > 90) push(`moment ${moment.id}`, "each line must be 90 characters or fewer");
    }
    for (const pattern of FAKE_MEASUREMENT_PATTERNS) {
      if (pattern.test(text)) push(`moment ${moment.id}`, "contains a fabricated biological measurement");
    }
    for (const pattern of SHAME_PATTERNS) {
      if (pattern.test(text)) push(`moment ${moment.id}`, "contains shaming language");
    }
    if (moment.evidence_type === "health" && !claimIds.has(moment.source)) {
      push(`moment ${moment.id}`, "a health moment must cite a claim_id that exists in this bundle");
    }
    if (moment.safety_level !== "safe") push(`moment ${moment.id}`, "safety_level must be safe");
  }

  return { ok: errors.length === 0, errors };
}

export const _internal = { FAKE_MEASUREMENT_PATTERNS, SHAME_PATTERNS };
