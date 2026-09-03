# Contributing

## Before you change anything

Read `docs/SCIENCE_POLICY.md`. It contains the one rule the product cannot break: we track the
commitment precisely, and we never pretend to measure the body. A change that displays a sperm
count, a testosterone level or a fertility percentage will be rejected by the validator, by the
server, and by review.

## Changing content

```bash
node scripts/validate-content.mjs   # must pass
node scripts/sync-content.mjs       # regenerates the bundle for app and server
```

`docs/CONTENT_GUIDE.md` covers voice, the category mix and the hard rules.

## Changing the app

```bash
cd android
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Logic that decides behaviour belongs in `domain/` or in a pure class under `core/`, so it can be
tested without an emulator. `MomentSelector`, `MomentPlanner`, `JourneyStatsCalculator` and
`ResetPatternAnalyzer` are the models to follow.

## Changing the server

```bash
cd server
npm test
```

## Rules that are enforced, not just preferred

| Rule | Enforced by |
|------|-------------|
| No fabricated biological measurement | `scripts/validate-content.mjs`, `server/src/lib/contentPolicy.js`, `ContentBundleTest` |
| No shaming language | the same three places |
| A reset never reduces lifetime committed days | `JourneyDao.resetAndStartNextChapter`, `JourneyStatsCalculatorTest` |
| Nothing is delivered during quiet hours | `MomentPlanner`, `DeliverMomentWorker`, `MomentPlannerTest` |
| The user's own words never reach the server | `LocalAnalytics.FORBIDDEN_KEYS`, `server/src/lib/analyticsPolicy.js` |
| Notifications default to private | `UserPreferences`, `NotificationPrivacy.fromId` |

If you need to change one of these, change the test and the policy deliberately, and say why in the
commit message.

## Style

Kotlin: official style, `val` over `var`, no `!!`, exhaustive `when` on sealed types and enums,
files under 800 lines, functions under 50.

JavaScript: ES modules, no framework beyond Fastify, parameterised SQL only.
