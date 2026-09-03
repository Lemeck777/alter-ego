# Google Play launch checklist

## Blockers

### 1. Medical review of every claim

Every entry in content/science/claims.json has "medical_reviewer": "pending". A qualified reviewer
must confirm each claim and its source, then set their name and the review date. Four claims are
cited by PubMed ID with a placeholder title and carry a review_note saying so:

- spermatogenesis_continuous (PubMed 35600584)
- abstinence_motility_dna_tradeoff (PubMed 38828413)
- age_effects_vary_between_studies (PubMed 16225533)
- fertility_context_shorter_abstinence (PubMed 41809803)

Do not ship health content with a pending reviewer.

### 2. Merchant registration for the developer's country

Selling subscriptions through Play requires Play merchant registration, and it is not available in
every country. As of the last check, Papua New Guinea supported developer registration but not
merchant registration. Apps distributed on Play that sell digital subscriptions generally must use
Play's billing system, so this cannot be worked around with an external payment link.

Practical options, in order of preference:

1. Launch free, with the subscription disabled by a remote flag, in markets where merchant
   registration is unavailable. The app is fully usable free.
2. Register the business entity in a supported country and complete merchant registration there.
3. Delay monetisation until registration becomes available.

Do not improvise around Play's billing rules. The engineering is done and gated behind
EntitlementRepository; this is a legal and business decision, not a code change.

### 3. Health content and services policy

The app has health-related functionality, so it falls under Google's Health Content and Services
policy in addition to Data Safety. Before submitting:

- Confirm the store listing does not claim health benefits the app cannot support.
- Confirm no screen implies a medical measurement. See docs/SCIENCE_POLICY.md for what is banned.
- Include the "not medical advice, see a clinician" line in the listing and in the Learn section.

## Store listing

- Content rating: complete the questionnaire honestly. The app discusses sexual behaviour in a
  self-improvement context with no explicit content. Expect Teen or Mature depending on region.
- Target audience: 18 and over. Do not enrol in Designed for Families.
- Screenshots: use the private notification mode in any screenshot showing a notification.
- Description: describe it as a personal accountability companion. Do not use "increase testosterone",
  "boost fertility" or any variant.

## Data Safety form

Filled in from docs/PRIVACY_DATA_SAFETY.md. The short version: App interactions collected and
optional; Purchases collected; Health and fitness not collected, because the commitment tracker never
leaves the device in V1.

## Technical

- [ ] Release signing configured (upload key in a keystore outside the repo, Play App Signing on)
- [ ] versionCode and versionName set
- [ ] R8 shrinking verified: install the release build and confirm content loads and serialisation works
- [ ] API_BASE_URL points at production
- [ ] Backend deployed with ADMIN_API_KEY and GOOGLE_SERVICE_ACCOUNT_B64 set
- [ ] POST /v1/billing/play-notification subscribed to Play real-time developer notifications
- [ ] Subscription products created in Play Console: alter_ego_plus_monthly, alter_ego_plus_yearly
- [ ] 7-day trial configured as a subscription offer
- [ ] Notification permission flow tested on Android 13, 14 and 15
- [ ] Exact-alarm behaviour tested on Android 12+ with the permission denied
- [ ] Tested with notifications denied entirely: the app must still be usable
- [ ] Privacy policy URL live and linked

## Pre-launch tests worth running

- Reboot the device and confirm reminders re-arm.
- Change the timezone and confirm the day counter and planner stay sane.
- Deny notification permission at onboarding and confirm nothing crashes and nothing nags.
- Turn on airplane mode and confirm the whole app still works.
