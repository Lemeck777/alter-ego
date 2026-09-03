# Alter Ego

A lifelong personal accountability and reminder companion for Android.

The user lives their life. Alter Ego waits in the background, then appears for five to fifteen
seconds at a moment that matters, says one to three short lines in the voice of a companion the user
chose, and gets out of the way.

## What it is

Two engines, one relationship.

**Life companion.** Breathe. Drink water. Call your mum. Stand up. Go outside. Smile for no reason.

**Accountability.** Semen retention, porn avoidance, prayer, fitness, study, screen time, or any
commitment the user writes themselves, tracked precisely and without shame.

## The two decisions everything else follows from

**Chapters, not streaks.** A reset ends a chapter and starts the next one. Nothing is deleted, and
lifetime committed days only ever grow. This lives in the data model, not just the copy.

**We track the commitment, never the body.** The app knows to the second how long since the user's
last reported ejaculation. It never displays a sperm count, a testosterone level or a fertility
score, because none of those can be derived from a timer. See `docs/SCIENCE_POLICY.md`.

## Repository layout

| Path | Contents |
|------|----------|
| `content/` | The curated library: 500 Moments across 5 personas, 17 evidence claims, 10 lessons, the biology timeline, 12 urge interventions |
| `scripts/` | Content validation and the sync that produces one bundle for app and server |
| `android/` | The Kotlin app: Compose, Room, DataStore, Hilt, WorkManager, Play Billing |
| `server/` | Fastify and Postgres: content delivery, evidence database, entitlements, metrics |
| `docs/` | Architecture, science policy, privacy, metrics, Play launch checklist |

## Build the app

Requires JDK 17 and the Android SDK (API 35).

```bash
cd android
./gradlew :app:assembleDebug
```

The APK lands in `android/app/build/outputs/apk/debug/`.

```bash
./gradlew :app:testDebugUnitTest
```

## Work on the content

Edit files under `content/`, then:

```bash
node scripts/validate-content.mjs
node scripts/sync-content.mjs
```

The validator refuses fabricated biological measurements, shaming language, over-long lines, and
health Moments that cite a claim that does not exist. The sync writes `bundle.json` into both the
Android assets and the server seed.

## Run the backend

```bash
cd server
npm install
cp .env.example .env      # then fill in DATABASE_URL
npm run migrate
npm run seed
npm start
npm test
```

## The five companions

| Persona | Voice |
|---------|-------|
| Sage | Stoic, reflective, unhurried. Never uses an exclamation mark. |
| Coach | Direct, disciplined. Ends with a next action. |
| Grace | Christian, gentle. Public-domain Scripture, always cited. |
| Sunny | Optimistic, playful. Kind jokes, never at the user's expense. |
| Brother | Casual accountability. Teases, then backs you. |

Future Me is a sixth, premium persona that speaks only the user's own recorded messages.

## Principles

1. **Remember.** Remind the person who they intended to be.
2. **Interrupt.** Break an unhelpful pattern at the right moment.
3. **Accompany.** Stay over years, not for a 30-day challenge.
4. **Release.** Get them off the app and back into their life.

The product succeeds when someone becomes less dependent on their phone. Nothing in here optimises
for time on screen.

## Before public launch

Two blockers, both documented in `docs/PLAY_LAUNCH_CHECKLIST.md`: a qualified medical reviewer must
sign off every scientific claim, and Play merchant registration must be resolved for the developer's
country before subscriptions can be sold.
