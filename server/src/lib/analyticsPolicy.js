/**
 * What is allowed to leave the device as analytics.
 *
 * Sexual-health behaviour is sensitive data, so the safest design is one where the server never
 * receives the user's own words at all. This module is deliberately dependency-free so the rule can
 * be unit tested without a database.
 */
export const FORBIDDEN_PROP_KEYS = new Set([
  "note", "quote", "text", "message", "custom_rule", "email", "name",
]);

export const MAX_EVENTS_PER_BATCH = 200;
const MAX_PROP_LENGTH = 120;
const MAX_KEY_LENGTH = 40;
const MAX_NAME_LENGTH = 64;

export function sanitiseProps(props) {
  if (!props || typeof props !== "object" || Array.isArray(props)) return {};
  const safe = {};
  for (const [key, value] of Object.entries(props)) {
    if (FORBIDDEN_PROP_KEYS.has(key.toLowerCase())) continue;
    if (typeof value !== "string" && typeof value !== "number" && typeof value !== "boolean") continue;
    safe[key.slice(0, MAX_KEY_LENGTH)] = String(value).slice(0, MAX_PROP_LENGTH);
  }
  return safe;
}

export function sanitiseEvents(events) {
  if (!Array.isArray(events)) return [];
  return events
    .filter((event) => typeof event?.name === "string" && Number.isFinite(event?.at))
    .map((event) => ({
      name: event.name.slice(0, MAX_NAME_LENGTH),
      props: sanitiseProps(event.props),
      occurredAt: new Date(event.at).toISOString(),
    }));
}
