import { test } from "node:test";
import assert from "node:assert/strict";
import { sanitiseProps, sanitiseEvents } from "../src/lib/analyticsPolicy.js";

test("free-text keys never reach the server table", () => {
  const props = sanitiseProps({ note: "I relapsed after an argument", quote: "Dad said", category: "humor" });
  assert.deepEqual(props, { category: "humor" });
});

test("a custom rule is stripped even though it is user-authored config", () => {
  assert.deepEqual(sanitiseProps({ custom_rule: "no porn on weekdays" }), {});
});

test("non-scalar values are dropped", () => {
  assert.deepEqual(sanitiseProps({ nested: { a: 1 }, list: [1, 2], ok: "yes" }), { ok: "yes" });
});

test("values are truncated rather than rejected", () => {
  const props = sanitiseProps({ trigger: "x".repeat(500) });
  assert.equal(props.trigger.length, 120);
});

test("missing props is an empty object", () => {
  assert.deepEqual(sanitiseProps(undefined), {});
  assert.deepEqual(sanitiseProps(null), {});
});

test("events without a name or timestamp are dropped", () => {
  const rows = sanitiseEvents([{ name: "moment_opened", at: 1756880000000 }, { at: 1 }, { name: "x" }]);
  assert.equal(rows.length, 1);
  assert.equal(rows[0].name, "moment_opened");
});
