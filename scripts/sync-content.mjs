#!/usr/bin/env node
// Copies content/ into the Android assets folder and the server seed folder.
// Usage: node scripts/sync-content.mjs
import { readFileSync, writeFileSync, mkdirSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = join(dirname(fileURLToPath(import.meta.url)), "..");
const CONTENT = join(ROOT, "content");
const TARGETS = [
  join(ROOT, "android", "app", "src", "main", "assets", "content"),
  join(ROOT, "server", "src", "content", "seed"),
];

const moments = readdirSync(join(CONTENT, "moments"))
  .filter((f) => f.endsWith(".json"))
  .flatMap((f) => JSON.parse(readFileSync(join(CONTENT, "moments", f), "utf8")));

const bundle = {
  version: Number(process.env.CONTENT_VERSION || Math.floor(Date.now() / 1000)),
  generated_at: new Date().toISOString(),
  personas: JSON.parse(readFileSync(join(CONTENT, "personas.json"), "utf8")),
  moments,
  interventions: JSON.parse(readFileSync(join(CONTENT, "interventions.json"), "utf8")),
  claims: JSON.parse(readFileSync(join(CONTENT, "science", "claims.json"), "utf8")),
  lessons: JSON.parse(readFileSync(join(CONTENT, "science", "lessons.json"), "utf8")),
  timeline: JSON.parse(readFileSync(join(CONTENT, "science", "timeline.json"), "utf8")),
};

for (const dir of TARGETS) {
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, "bundle.json"), JSON.stringify(bundle));
  console.log(`wrote ${join(dir, "bundle.json")} (${moments.length} moments, version ${bundle.version})`);
}
