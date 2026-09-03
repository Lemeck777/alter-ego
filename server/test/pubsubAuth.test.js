import { test, beforeEach } from "node:test";
import assert from "node:assert/strict";
import { verifyPubSubRequest } from "../src/lib/pubsubAuth.js";

function request(headers = {}) {
  return { headers };
}

beforeEach(() => {
  delete process.env.PUBSUB_AUDIENCE;
  delete process.env.PUBSUB_SERVICE_ACCOUNT_EMAIL;
  delete process.env.PUBSUB_SHARED_SECRET;
});

test("an unconfigured endpoint refuses everything rather than trusting the payload", async () => {
  const result = await verifyPubSubRequest(request());
  assert.equal(result.ok, false);
  assert.equal(result.reason, "pubsub_auth_not_configured");
});

test("the shared secret path accepts the right secret", async () => {
  process.env.PUBSUB_SHARED_SECRET = "correct-horse-battery-staple";
  const result = await verifyPubSubRequest(request({ "x-pubsub-secret": "correct-horse-battery-staple" }));
  assert.equal(result.ok, true);
  assert.equal(result.via, "shared_secret");
});

test("a wrong secret is refused", async () => {
  process.env.PUBSUB_SHARED_SECRET = "correct-horse-battery-staple";
  const result = await verifyPubSubRequest(request({ "x-pubsub-secret": "wrong-horse-battery-staple" }));
  assert.equal(result.ok, false);
  assert.equal(result.reason, "bad_shared_secret");
});

test("a secret of a different length is refused without throwing", async () => {
  process.env.PUBSUB_SHARED_SECRET = "correct-horse";
  const result = await verifyPubSubRequest(request({ "x-pubsub-secret": "short" }));
  assert.equal(result.ok, false);
  assert.equal(result.reason, "bad_shared_secret");
});

test("a missing secret header is refused", async () => {
  process.env.PUBSUB_SHARED_SECRET = "correct-horse";
  const result = await verifyPubSubRequest(request());
  assert.equal(result.ok, false);
  assert.equal(result.reason, "missing_shared_secret");
});

test("the OIDC path requires a bearer token", async () => {
  process.env.PUBSUB_AUDIENCE = "https://api.alterego.app/v1/billing/play-notification";
  process.env.PUBSUB_SERVICE_ACCOUNT_EMAIL = "play-notifications@example.iam.gserviceaccount.com";
  const result = await verifyPubSubRequest(request());
  assert.equal(result.ok, false);
  assert.equal(result.reason, "missing_bearer_token");
});

test("a forged bearer token is refused", async () => {
  process.env.PUBSUB_AUDIENCE = "https://api.alterego.app/v1/billing/play-notification";
  process.env.PUBSUB_SERVICE_ACCOUNT_EMAIL = "play-notifications@example.iam.gserviceaccount.com";
  const result = await verifyPubSubRequest(request({ authorization: "Bearer not-a-real-token" }));
  assert.equal(result.ok, false);
  assert.equal(result.reason, "invalid_oidc_token");
});
