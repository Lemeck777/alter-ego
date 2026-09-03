#!/usr/bin/env node
// Validates every curated content file against content/SCHEMA.md.
// Usage: node scripts/validate-content.mjs
import { readFileSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..", "content");
const read = (p) => JSON.parse(readFileSync(join(ROOT, p), "utf8"));

const PERSONAS = new Set(["sage", "coach", "grace", "sunny", "brother", "future_me", "any"]);
const GOALS = new Set(["general", "retention", "porn_avoidance", "discipline", "faith", "focus", "fitness", "calm", "screen_time", "confidence", "custom"]);
const CATEGORIES = new Set(["accountability", "humor", "perspective", "health", "faith", "wellbeing", "random", "urge_management", "welcome_back", "late_night", "reset_recovery", "morning", "evening", "anniversary"]);
const TONES = new Set(["gentle", "direct", "playful", "reflective", "warm"]);
const AGE_BANDS = new Set(["18-24", "25-29", "30-34", "35-39", "40-44", "45-49", "50-59", "60+"]);
const TIME_CONTEXTS = new Set(["any", "morning", "midday", "afternoon", "evening", "late_night"]);
const TRIGGERS = new Set(["random", "scheduled", "accountability", "smart", "urge", "reset", "welcome_back", "anniversary", "high_risk_window"]);
const ACTIONS = new Set(["dismiss", "urge_mode", "breathe", "save", "open_journey", "open_learn", "snooze"]);
const ANIMATIONS = new Set(["idle", "enter", "look", "smile", "laugh", "think", "encourage", "serious", "breathe", "pray", "celebrate", "wave", "point", "nod"]);
const HAPTICS = new Set(["none", "tap", "double_tap", "heartbeat", "soft"]);
const EVIDENCE = new Set(["behavioral", "philosophical", "scripture", "humor", "health", "none"]);
const EVIDENCE_LEVELS = new Set(["strong", "moderate", "limited", "tradition"]);
const SHAME_WORDS = /\b(pathetic|disgusting|worthless|loser|failure|weakling)\b/i;
const FAKE_BIOLOGY = /(\d+\s*%|sperm count|testosterone\s*(up|\+|increase[sd]? by)|\d+\s*million)/i;

const errors = [];
const err = (file, id, msg) => errors.push(`${file} [${id}]: ${msg}`);

const claims = read("science/claims.json");
const claimIds = new Set(claims.map((c) => c.claim_id));
for (const c of claims) {
  if (!EVIDENCE_LEVELS.has(c.evidence_level)) err("claims", c.claim_id, `bad evidence_level ${c.evidence_level}`);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(c.review_date)) err("claims", c.claim_id, "review_date must be yyyy-mm-dd");
  if (c.evidence_level !== "tradition" && !c.source_url) err("claims", c.claim_id, "source_url required");
  if (!["active", "retired"].includes(c.status)) err("claims", c.claim_id, "bad status");
}

const lessons = read("science/lessons.json");
for (const l of lessons) {
  if (!["your_body", "retention", "urges_habits", "age"].includes(l.category)) err("lessons", l.lesson_id, "bad category");
  if (!(l.read_seconds >= 30 && l.read_seconds <= 90)) err("lessons", l.lesson_id, "read_seconds must be 30..90");
  for (const b of l.blocks) {
    if (b.type === "claim" && !claimIds.has(b.claim_id)) err("lessons", l.lesson_id, `unknown claim ${b.claim_id}`);
    if (b.type === "text" && /(d+s*%|d+s*million)/.test(b.text)) err("lessons", l.lesson_id, `possible fake-precision text: ${b.text}`);
  }
}

const timeline = read("science/timeline.json");
for (const p of timeline.phases) for (const id of p.claim_ids) if (!claimIds.has(id)) err("timeline", p.phase_id, `unknown claim ${id}`);

const personas = read("personas.json");
const personaIds = new Set(personas.map((p) => p.id));

const interventions = read("interventions.json");
for (const i of interventions) for (const pid of Object.keys(i.persona_lines)) if (!personaIds.has(pid)) err("interventions", i.id, `unknown persona ${pid}`);

const momentsDir = join(ROOT, "moments");
const allIds = new Set();
let total = 0;
const catCounts = {};
for (const file of readdirSync(momentsDir).filter((f) => f.endsWith(".json"))) {
  const moments = JSON.parse(readFileSync(join(momentsDir, file), "utf8"));
  for (const m of moments) {
    total++;
    if (allIds.has(m.id)) err(file, m.id, "duplicate id");
    allIds.add(m.id);
    if (!PERSONAS.has(m.persona)) err(file, m.id, `bad persona ${m.persona}`);
    if (!GOALS.has(m.goal)) err(file, m.id, `bad goal ${m.goal}`);
    if (!CATEGORIES.has(m.category)) err(file, m.id, `bad category ${m.category}`);
    catCounts[m.category] = (catCounts[m.category] || 0) + 1;
    if (!TONES.has(m.tone)) err(file, m.id, `bad tone ${m.tone}`);
    if (!(Number.isInteger(m.intensity) && m.intensity >= 1 && m.intensity <= 5)) err(file, m.id, "intensity 1..5");
    if (!Array.isArray(m.age_bands) || m.age_bands.some((b) => !AGE_BANDS.has(b))) err(file, m.id, "bad age_bands");
    if (!TIME_CONTEXTS.has(m.time_context)) err(file, m.id, `bad time_context ${m.time_context}`);
    if (!TRIGGERS.has(m.trigger)) err(file, m.id, `bad trigger ${m.trigger}`);
    if (!Array.isArray(m.lines) || m.lines.length < 1 || m.lines.length > 3) err(file, m.id, "lines must be 1..3");
    for (const line of m.lines || []) {
      if (typeof line !== "string" || line.length > 90) err(file, m.id, `line > 90 chars: ${line}`);
      if (SHAME_WORDS.test(line)) err(file, m.id, `shame language: ${line}`);
      if (FAKE_BIOLOGY.test(line)) err(file, m.id, `fake biological precision: ${line}`);
    }
    if (!Array.isArray(m.actions) || m.actions.length < 1 || m.actions.length > 2) err(file, m.id, "actions must be 1..2");
    for (const a of m.actions || []) if (!ACTIONS.has(a.type) || !a.label) err(file, m.id, `bad action ${JSON.stringify(a)}`);
    if (!ANIMATIONS.has(m.animation)) err(file, m.id, `bad animation ${m.animation}`);
    if (!HAPTICS.has(m.haptic)) err(file, m.id, `bad haptic ${m.haptic}`);
    if (!EVIDENCE.has(m.evidence_type)) err(file, m.id, `bad evidence_type ${m.evidence_type}`);
    if (m.evidence_type === "health" && !claimIds.has(m.source)) err(file, m.id, `health moment must cite a claim id, got ${m.source}`);
    if (m.evidence_type === "scripture" && !m.source) err(file, m.id, "scripture needs source");
    if (m.safety_level !== "safe") err(file, m.id, "safety_level must be safe");
    if (typeof m.premium !== "boolean") err(file, m.id, "premium must be boolean");
  }
}

console.log(`moments: ${total}, claims: ${claims.length}, lessons: ${lessons.length}, timeline phases: ${timeline.phases.length}, personas: ${personas.length}, interventions: ${interventions.length}`);
console.log("category mix:", Object.fromEntries(Object.entries(catCounts).sort((a, b) => b[1] - a[1]).map(([k, v]) => [k, `${v} (${((v / total) * 100).toFixed(0)}%)`])));
if (errors.length) {
  console.error(`\n${errors.length} problem(s):`);
  for (const e of errors) console.error(" - " + e);
  process.exit(1);
}
console.log("OK: all content valid");
