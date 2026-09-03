import { test, before, after } from "node:test";
import assert from "node:assert/strict";

process.env.ADMIN_API_KEY = "test-admin-key-0123456789";
process.env.DATABASE_URL = "postgres://unused:unused@127.0.0.1:1/unused";

const { buildServer } = await import("../src/index.js");
const { pool } = await import("../src/db/pool.js");

let server;

before(async () => {
  server = buildServer({ logger: false });
  await server.ready();
});

after(async () => {
  await server.close();
  await pool.end().catch(() => {});
});

test("health does not need a database", async () => {
  const response = await server.inject({ method: "GET", url: "/health" });
  assert.equal(response.statusCode, 200);
  assert.equal(response.json().ok, true);
});

test("readiness reports 503 when the database is unreachable", async () => {
  const response = await server.inject({ method: "GET", url: "/ready" });
  assert.equal(response.statusCode, 503);
  assert.equal(response.json().ready, false);
});

test("admin publishing rejects a request with no key", async () => {
  const response = await server.inject({
    method: "POST",
    url: "/v1/admin/content/publish",
    payload: { version: 1, moments: [], claims: [] },
  });
  assert.equal(response.statusCode, 401);
});

test("admin publishing rejects a wrong key", async () => {
  const response = await server.inject({
    method: "POST",
    url: "/v1/admin/content/publish",
    headers: { "x-admin-key": "not-the-key" },
    payload: { version: 1, moments: [], claims: [] },
  });
  assert.equal(response.statusCode, 401);
});

test("the validate endpoint reports policy violations without touching the database", async () => {
  const response = await server.inject({
    method: "POST",
    url: "/v1/admin/content/validate",
    headers: { "x-admin-key": process.env.ADMIN_API_KEY },
    payload: {
      version: 2,
      moments: [
        {
          id: "bad_0001",
          persona: "coach",
          lines: ["Your sperm count: 284,352,201 and rising."],
          evidence_type: "none",
          source: null,
          safety_level: "safe",
        },
      ],
      claims: [],
    },
  });
  assert.equal(response.statusCode, 200);
  const body = response.json();
  assert.equal(body.ok, false);
  assert.match(body.errors.join(" "), /fabricated biological measurement/);
});

test("the play notification webhook refuses an unauthenticated caller", async () => {
  const response = await server.inject({
    method: "POST",
    url: "/v1/billing/play-notification",
    payload: {
      message: {
        data: Buffer.from(
          JSON.stringify({ subscriptionNotification: { purchaseToken: "stolen-token", notificationType: 3 } }),
        ).toString("base64"),
      },
    },
  });
  // Without this the endpoint would revoke a stranger's subscription.
  assert.equal(response.statusCode, 401);
});

test("purchase verification requires an install id", async () => {
  const response = await server.inject({
    method: "POST",
    url: "/v1/billing/verify",
    payload: { productId: "alter_ego_plus_monthly", purchaseToken: "token" },
  });
  assert.equal(response.statusCode, 400);
});

test("analytics rejects an oversized batch rather than accepting it", async () => {
  const events = Array.from({ length: 500 }, () => ({ name: "app_opened", at: Date.now(), props: {} }));
  const response = await server.inject({
    method: "POST",
    url: "/v1/analytics/batch",
    payload: { installId: "abc", events },
  });
  assert.equal(response.statusCode, 413);
});
