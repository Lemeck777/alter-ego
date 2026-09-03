import { test } from "node:test";
import assert from "node:assert/strict";
import { validateBundle } from "../src/lib/contentPolicy.js";

const claim = {
  claim_id: "abstinence_volume_count",
  claim: "Longer ejaculatory abstinence tends to increase semen volume.",
  topic: "retention",
  evidence_level: "strong",
  source_url: "https://pubmed.ncbi.nlm.nih.gov/29143943/",
  source_title: "Systematic review",
  review_date: "2026-09-03",
};

const moment = {
  id: "sage_0001",
  persona: "sage",
  goal: "general",
  category: "accountability",
  lines: ["Remember the man you're becoming."],
  evidence_type: "none",
  source: null,
  safety_level: "safe",
};

function bundle(overrides = {}) {
  return { version: 1, moments: [moment], claims: [claim], ...overrides };
}

test("a clean bundle passes", () => {
  const result = validateBundle(bundle());
  assert.equal(result.ok, true, result.errors.join("; "));
});

test("a fabricated sperm count is refused", () => {
  const bad = bundle({ moments: [{ ...moment, lines: ["Sperm count: 284,352,201 and climbing."] }] });
  const result = validateBundle(bad);
  assert.equal(result.ok, false);
  assert.match(result.errors.join(" "), /fabricated biological measurement/);
});

test("a testosterone percentage claim is refused", () => {
  const bad = bundle({ moments: [{ ...moment, lines: ["Testosterone increased by 34 percent today."] }] });
  assert.equal(validateBundle(bad).ok, false);
});

test("stored semen language is refused", () => {
  const bad = bundle({ moments: [{ ...moment, lines: ["Your body has stored 17 days of semen."] }] });
  assert.equal(validateBundle(bad).ok, false);
});

test("shaming language is refused", () => {
  const bad = bundle({ moments: [{ ...moment, lines: ["You are pathetic for slipping again."] }] });
  assert.equal(validateBundle(bad).ok, false);
});

test("a health moment must cite a claim that exists", () => {
  const bad = bundle({ moments: [{ ...moment, evidence_type: "health", source: "made_up_claim" }] });
  const result = validateBundle(bad);
  assert.equal(result.ok, false);
  assert.match(result.errors.join(" "), /must cite a claim_id/);
});

test("a health moment citing a real claim passes", () => {
  const good = bundle({ moments: [{ ...moment, evidence_type: "health", source: "abstinence_volume_count" }] });
  assert.equal(validateBundle(good).ok, true);
});

test("a non-tradition claim without a source is refused", () => {
  const bad = bundle({ claims: [{ ...claim, source_url: null }] });
  assert.equal(validateBundle(bad).ok, false);
});

test("a tradition claim may have no source", () => {
  const good = bundle({
    claims: [{ ...claim, claim_id: "tradition_claim", evidence_level: "tradition", source_url: null }],
  });
  assert.equal(validateBundle(good).ok, true);
});

test("an invalid evidence level is refused", () => {
  const bad = bundle({ claims: [{ ...claim, evidence_level: "proven" }] });
  assert.equal(validateBundle(bad).ok, false);
});

test("long lines are refused so a Moment stays readable in seconds", () => {
  const bad = bundle({ moments: [{ ...moment, lines: ["x".repeat(91)] }] });
  assert.equal(validateBundle(bad).ok, false);
});

test("more than three lines is refused", () => {
  const bad = bundle({ moments: [{ ...moment, lines: ["a", "b", "c", "d"] }] });
  assert.equal(validateBundle(bad).ok, false);
});
