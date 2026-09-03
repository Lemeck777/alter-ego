# Metrics

## The one that matters

**Companion survival rate**: the share of users who still have an active Alter Ego relationship 12
months after install, then 24, then 36.

This is deliberately the headline number instead of daily active users or session length. The product
succeeds when someone becomes less dependent on their phone, so optimising for screen time would mean
optimising against the user.

## Event names

Defined in `LocalAnalytics` and sanitised twice before storage.

| Event | Props | Question it answers |
|-------|-------|---------------------|
| onboarding_completed | goals, persona, intensity | Does the one-minute onboarding actually complete? |
| moment_delivered | category, trigger, persona | Is the content mix holding? |
| moment_opened | category | Which categories earn a tap? |
| moment_reaction | reaction, surface | Do people answer from the notification or the screen? |
| urge_started | none | How often is Urge Mode reached for? |
| urge_completed | final_level, interventions | Does the intervention actually help? |
| chapter_reset | none | Restart frequency |
| chapter_started | chapter | Chapter number over time |
| notifications_denied | none | Did the contextual permission ask work? |
| paywall_viewed | none | Paywall reach |
| trial_started | none | Trial conversion funnel |
| persona_changed | none | Does the relationship survive a persona change? |
| app_opened | none | Baseline liveness |

## What is never collected

Note text, personal quotes, Future Me messages, custom commitment rules, exact ages, and anything
identifying. Enforced in `LocalAnalytics.FORBIDDEN_KEYS` on the client and
`server/src/lib/analyticsPolicy.js` on the server.

## Health checks

| Metric | Watch for |
|--------|-----------|
| Notification disable rate | Rising means the mix is too heavy or too frequent |
| Positive moment rate | Falling means content quality is drifting |
| Urge intervention completion | The clearest signal the core feature works |
| Average active chapter duration | Real behavioural progress |
| D1 / D7 / D30, then 90-day, then 1-year retention | Whether the companion sticks |

## Anti-goals

Do not build, and do not optimise for: infinite scroll, streak fear, loss-aversion punishment,
leaderboards, or notification volume. If a metric can only be improved by making the app harder to
put down, it is the wrong metric.
