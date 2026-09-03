# Privacy and Play Data Safety

## Principle

Local-first. Sexual-health behaviour is sensitive, so the safest architecture is one where the
server never receives it.

## What stays on the device, always

- Commitment titles, rules and custom rule text
- Chapters, reset timestamps, reset reasons and notes
- Urge events and which interventions were used
- Personal quotes ("Teach me what to say") and Future Me messages
- Age band
- Persona choice, quiet hours, reminder settings, notification privacy mode

Stored in Room (`alterego.db`) and DataStore. Both are excluded from Android auto-backup and device
transfer in `res/xml/backup_rules.xml` and `res/xml/data_extraction_rules.xml`, so this data does not
leave the phone through a system backup the user did not ask for.

## What can leave the device

| Data | When | Why |
|------|------|-----|
| Install id and aggregate event names | Only while analytics is enabled | Retention and product-quality metrics |
| Purchase token and product id | Only on a purchase | Verify the subscription with Google Play |
| Content bundle version | On content sync | Ask the server whether newer curated content exists |

Analytics props are filtered twice: on the client in `LocalAnalytics.FORBIDDEN_KEYS` and again on the
server in `src/lib/analyticsPolicy.js`. Note text, quotes, messages and custom rules are dropped in
both places.

## Notification privacy

Three modes, defaulting to the most private:

| Mode | Lock screen shows |
|------|-------------------|
| Private (default) | "Sage wants a word." |
| Normal | "Your accountability check-in is ready." |
| Explicit | "Day 17 check-in" (and is marked private so it is hidden on the lock screen) |

Nobody glancing at the phone learns anything about the user's sexual habits from the default.

## App lock

Optional PIN or biometric. The PIN is salted and hashed with 10,000 SHA-256 rounds inside
EncryptedSharedPreferences. It is never stored in plain text and never leaves the device.

## User rights

Three one-tap actions in Privacy:

- **Export my journey** produces a plain-text copy of everything the app holds.
- **Delete my history** clears commitments, chapters, resets and urges.
- **Delete everything** additionally clears preferences, quotes, future messages and the PIN.

## Play Data Safety declaration

Declare, for the analytics-enabled path only:

- **App activity → App interactions**: collected, not shared, optional, not linked to identity.
- **Purchases**: collected via Google Play, not shared, required for subscriptions.
- Health and fitness: **not collected**. The commitment tracker never leaves the device unless the
  user turns on cloud backup, which is not in V1.

Encryption in transit: yes. Deletion mechanism: yes, in-app.

Because the app has health-related functionality, it also falls under Google's Health Content and
Services policy. `docs/PLAY_LAUNCH_CHECKLIST.md` covers what that requires.

## Permissions

| Permission | Why | When requested |
|------------|-----|----------------|
| POST_NOTIFICATIONS | The companion cannot speak otherwise | Last step of onboarding, after the user has met their persona |
| SCHEDULE_EXACT_ALARM | Only for reminders the user explicitly marks exact | Never during onboarding |
| VIBRATE | The buzz before a Moment | Install time |
| RECEIVE_BOOT_COMPLETED | Alarms do not survive a reboot | Install time |
| PACKAGE_USAGE_STATS | Optional Smart Sensing, V2 | Never in V1; requires the user to enable Usage Access in system settings themselves |
