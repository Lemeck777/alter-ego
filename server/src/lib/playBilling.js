import { GoogleAuth } from "google-auth-library";

/**
 * Google Play subscription verification.
 *
 * When no service account is configured (local development, or a market where Play merchant
 * registration is not yet available) we return a clearly-labelled unverified result rather than
 * pretending the purchase is valid.
 */
let authClient = null;

function getAuth() {
  if (authClient) return authClient;
  const encoded = process.env.GOOGLE_SERVICE_ACCOUNT_B64;
  if (!encoded) return null;
  const credentials = JSON.parse(Buffer.from(encoded, "base64").toString("utf8"));
  authClient = new GoogleAuth({
    credentials,
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  return authClient;
}

export async function verifySubscription({ packageName, productId, purchaseToken }) {
  const auth = getAuth();
  if (!auth) {
    return { verified: false, reason: "play_verification_not_configured", expiresAt: null };
  }
  const pkg = packageName || process.env.ANDROID_PACKAGE_NAME;
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodeURIComponent(pkg)}` +
    `/purchases/subscriptions/${encodeURIComponent(productId)}/tokens/${encodeURIComponent(purchaseToken)}`;

  try {
    const client = await auth.getClient();
    const response = await client.request({ url });
    const data = response.data ?? {};
    const expiryMillis = Number(data.expiryTimeMillis ?? 0);
    const expiresAt = expiryMillis ? new Date(expiryMillis).toISOString() : null;
    const active = expiryMillis > Date.now();
    return {
      verified: active,
      reason: active ? "ok" : "expired",
      expiresAt,
      autoRenewing: Boolean(data.autoRenewing),
    };
  } catch (error) {
    return { verified: false, reason: `play_api_error:${error?.response?.status ?? "unknown"}`, expiresAt: null };
  }
}
