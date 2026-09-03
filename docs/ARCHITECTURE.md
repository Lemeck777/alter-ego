# Architecture

## Shape

```
content/            Curated source of truth (Moments, personas, science, interventions)
  |
  | scripts/validate-content.mjs  -> blocks fake precision and shaming at authoring time
  | scripts/sync-content.mjs      -> emits one bundle.json
  |
  +--> android/app/src/main/assets/content/bundle.json   (works offline from first launch)
  +--> server/src/content/seed/bundle.json               (seeds the backend)

android/            Single-module Kotlin app, packages by layer then feature
server/             Fastify + Postgres. Content delivery, evidence database, entitlements, metrics
```

## Android layers

```
domain/      models (no Android imports), usecases (pure, unit tested)
core/        database, datastore, content, scheduler, notifications, security, billing,
             analytics, design, animation, network, di
feature/     onboarding, today, moment, urge, reset, journey, me, science, alterego, premium, lock, root
```

Dependencies point inward: `feature` uses `core` and `domain`; `core` uses `domain`; `domain` uses
nothing. The rules that matter most to the product live in `domain` and are covered by unit tests
that do not need an emulator.

## The four engines

**Content.** `ContentRepository` boots from the bundled asset, then accepts newer bundles from the
backend. That is how a corrected scientific claim reaches users without an app release.

**Selection.** `MomentSelector` is pure and seeded, so its behaviour is testable. It filters by
persona, goal, age band, time of day, trigger and entitlement, then weights by category to hold the
target mix: roughly 30% accountability, 20% humour, 15% perspective, 10% health, 10% faith, 10%
wellbeing, 5% random. The user's own quotes outrank everything.

**Scheduling.** `MomentPlanner` decides when (pure, testable, quiet-hours aware).
`MomentScheduler` arms Android alarms. Friendly nudges use inexact alarms because Android recommends
them for anything that does not need second-perfect timing. Exact alarms are used only where the user
asked for one. `MomentAlarmReceiver` hands off to WorkManager immediately, since a broadcast receiver
has a very short window and WorkManager survives process death.

**Accountability.** `JourneyRepository` owns commitments, chapters, resets and urges.
`JourneyStatsCalculator` and `ResetPatternAnalyzer` turn that history into reflection, not a score.

## Why chapters, not streaks

A reset closes the current chapter and opens the next one in a single transaction
(`JourneyDao.resetAndStartNextChapter`). Nothing is deleted. `JourneyStatsCalculator` therefore
reports lifetime committed days that only ever grow, and the UI says "Chapter 4 ended at 97 days"
rather than "streak lost". This is a data-model decision, not a copy decision, which is why it cannot
quietly regress.

## The full-screen Moment

Android reserves full-screen intents for calls and alarms, so an ordinary nudge is a normal
notification. Tapping it opens `MomentActivity`, which is its own task and excluded from recents so
it never becomes something to scroll back through.

## Persona theming

`AlterEgoTheme` derives the whole colour scheme from the chosen persona, including deciding whether
the surface is light or dark from the background's luminance. Changing companion visibly changes the
app, which is the clearest signal that this is a relationship rather than a dashboard.

## The character

`AlterEgoCharacter` draws the companion with Compose Canvas against a state contract that mirrors a
Rive state machine (idle, look, smile, laugh, think, encourage, serious, breathe, pray, celebrate,
wave, point, nod). Swapping in .riv files later is a change behind that one composable, and needs no
change to content, selection or scheduling.

## Offline

Everything except purchase verification and content updates works with no network at all. The bundle
ships in assets, the database is local, and the scheduler is on-device.

## Backend

| Endpoint | Purpose |
|----------|---------|
| GET /v1/content/bundle?since= | Newer curated content, or 304 |
| GET /v1/content/claims | The evidence database on its own |
| GET /v1/content/claims/:id/history | Every previous wording of a claim |
| POST /v1/billing/verify | Confirm a Play purchase |
| GET /v1/entitlements/me | Current entitlement |
| POST /v1/billing/play-notification | Play real-time developer notifications |
| POST /v1/analytics/batch | Aggregate metrics, sanitised twice |
| POST /v1/admin/content/publish | Publish a bundle, rejected if it violates the content policy |
| POST /v1/admin/content/validate | Dry run for editors |

No microservices. One Fastify process and one Postgres database.
