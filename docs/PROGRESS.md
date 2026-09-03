# Build status

Last verified: 4 September 2026.

## Verified green

| Check | Result |
|-------|--------|
| Content validation | 500 Moments, 17 claims, 10 lessons, 12 interventions, 6 personas, no violations |
| Android unit tests | 48 passing, 0 failing |
| Android debug APK | builds, 22.2 MB |
| Android release APK | builds with R8, 3.0 MB, content bundle intact |
| Server tests | 33 passing, 0 failing |
| Code review | 5 findings, all fixed |

## Built

**Content library** (`content/`)
- 500 Moments, 100 per persona, all validated against the schema
- Category mix: accountability 128, humour 82, wellbeing 70, perspective 62, urge management 34,
  health 30, faith 30, random 27, late night 13, reset recovery 12, welcome back 9, morning 3
- 30 health Moments, each citing a real claim; 38 Scripture Moments, each citing book chapter:verse
- 17 evidence claims with source, evidence level, study type and review metadata
- 10 lessons, a four-phase biology timeline, 12 urge interventions
- 50 Moments reserved for Alter Ego+

**Android app** (`android/`) - 86 Kotlin files, about 10,000 lines
- Onboarding in nine steps, ending with the contextual notification request
- Today, Journey and Me tabs; Moment screen and a standalone MomentActivity for notifications
- Urge Mode with interventions, a ten-minute timer and a check-in
- Reset flow with an optional three-tap reflection and a new chapter
- Science centre with evidence labels and live source links; biology timeline
- Persona picker, custom persona builder, premium screen
- Settings, privacy with export and delete, commitments, personal quotes, Future Me, reminders
- Optional PIN or biometric app lock with a working recovery path
- Room, DataStore, Hilt, WorkManager, alarms, notifications, Play Billing
- A Compose-drawn companion with a 15-state animation contract

**Backend** (`server/`) - 12 JavaScript files
- Content bundle delivery with 304 support; evidence claims API with full revision history
- Admin publishing gated by the content policy; Play verification; authenticated Play webhook
- Analytics with the user's own words stripped on both client and server
- Postgres migrations and seed, Dockerfile, DigitalOcean App Platform spec

**Docs** (`docs/`)
- Architecture, science policy, privacy and Data Safety, metrics, content guide, Play checklist

## Fixed after review

1. Play Billing could revoke a paying user's access when a purchase query failed transiently.
2. The Play notification webhook accepted unauthenticated cancellation requests.
3. `ContentSyncWorker` swallowed `CancellationException`, so cancelled work rescheduled itself.
4. A cancelled biometric prompt left the lock screen with no way forward.
5. The admin key was compared without constant-time semantics.

## Blocked on decisions outside the code

1. A qualified medical reviewer must sign off every claim in `content/science/claims.json`.
   All 17 read `"medical_reviewer": "pending"`.
2. Play merchant registration must be resolved for the developer's country before subscriptions can
   be sold. The billing code is complete and gated behind `EntitlementRepository`.

## Not built, deliberately

Smart sensing via usage stats, voice, AI-generated personas, weekly reflections, cloud backup,
accountability contacts, iOS, wearables, community. All listed in the blueprint as post-V1.
