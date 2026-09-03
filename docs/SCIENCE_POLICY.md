# Scientific content policy

This is the document that keeps Alter Ego honest. It binds product, engineering and content.

## The one rule

**We track the commitment precisely. We never pretend to measure the body.**

The app knows, to the second, how long it has been since the user reported an ejaculation. It does
not know, and cannot know, anything about their sperm, hormones or fertility.

## Never build, never display

| Banned | Why |
|--------|-----|
| "Sperm count: 284,352,201" | Sperm concentration and count can only be measured by a laboratory semen analysis. |
| "Sperm health: 87%" | There is no such single score, and none can be derived from a timer. |
| "Testosterone +34%" | No strong evidence supports a lasting testosterone rise from abstinence. |
| "Fertility increased 23%" | Fertility is not a percentage an app can compute. |
| "Your body has stored 17 days of semen" | Sperm production is continuous; sperm are reabsorbed, not stockpiled. |
| Any per-year sperm decline formula | Large evidence syntheses do not support a universal fixed annual decline in concentration. |

These are enforced in three places, so a mistake in one layer cannot ship:

1. `scripts/validate-content.mjs` blocks them at authoring time.
2. `server/src/lib/contentPolicy.js` blocks them at publish time and refuses the bundle.
3. `content/SCHEMA.md` states the rule for anyone writing a Moment.

## What we do say

The Biology screen shows the phase of ejaculatory abstinence the user is in, describes what research
says about that phase *in general*, and states plainly that individual semen quality is measurable
only by a laboratory semen analysis.

## Evidence labels

Every health statement in the app carries one:

| Label | Meaning |
|-------|---------|
| Strong evidence | Supported by systematic reviews, meta-analyses or established guidelines. |
| Moderate evidence | Supported by good studies, with some inconsistency or indirectness. |
| Limited / mixed evidence | Small, unreplicated or conflicting studies. Presented as such. |
| Tradition / philosophy | Belief or practice, presented as belief. Never as science. |

The last row matters most. Claims that semen retention produces supernatural energy or magnetism get
the tradition label. We do not argue with anyone's beliefs; we simply do not label them as science.

## The claims database

Every claim lives in `content/science/claims.json` and mirrors into the server's `evidence_claims`
table. Each carries a source URL, source title, publication year, study type, evidence level, review
date and reviewer. Editing a claim writes the previous version into `evidence_claim_revisions`, so a
correction can always be traced.

Claims are served remotely and cached on device. When the evidence changes, we publish a new bundle;
no app release is required. Medical claims are never hardcoded into the Android source.

## Outstanding before public launch

Every claim currently carries `"medical_reviewer": "pending"`. A qualified reviewer must:

1. Confirm each source title and year against the cited record. Four entries are cited by PubMed ID
   with a placeholder title and are flagged in their `review_note`.
2. Confirm the wording does not overstate the evidence.
3. Replace `pending` with their name and set `review_date`.

`docs/PLAY_LAUNCH_CHECKLIST.md` treats this as a release blocker.

## Framing

Retention is a personal commitment, not a medical prescription. The product language is
"your commitment, your reason, your journey", never "every man needs this". Someone planning a
pregnancy is pointed to a clinician, because in some fertility settings a shorter interval is
preferred.
