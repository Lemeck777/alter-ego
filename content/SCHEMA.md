# Alter Ego Content Schema

All curated content lives here as JSON and is the single source of truth for
both the Android app (bundled under `app/src/main/assets/content/`) and the
backend seed (`server/src/content/seed`). Never hand-edit the copies; edit here
and run `node scripts/sync-content.mjs`.

## Moment (content/moments/*.json)

```json
{
  "id": "sage_0001",
  "persona": "sage",
  "goal": "general",
  "category": "accountability",
  "tone": "reflective",
  "intensity": 2,
  "age_bands": [],
  "time_context": "any",
  "trigger": "random",
  "lines": ["Hey.", "Remember the man you're becoming."],
  "actions": [
    { "label": "I'm good", "type": "dismiss" },
    { "label": "I need help", "type": "urge_mode" }
  ],
  "animation": "look",
  "haptic": "double_tap",
  "evidence_type": "philosophical",
  "source": null,
  "safety_level": "safe",
  "premium": false
}
```

| Field | Allowed values |
|-------|----------------|
| persona | `sage`, `coach`, `grace`, `sunny`, `brother`, `any` |
| goal | `general`, `retention`, `porn_avoidance`, `discipline`, `faith`, `focus`, `fitness`, `calm`, `screen_time`, `confidence`, `custom` |
| category | `accountability`, `humor`, `perspective`, `health`, `faith`, `wellbeing`, `random`, `urge_management`, `welcome_back`, `late_night`, `reset_recovery`, `morning`, `evening`, `anniversary` |
| tone | `gentle`, `direct`, `playful`, `reflective`, `warm` |
| intensity | 1 (whisper) … 5 (strong accountability) |
| age_bands | subset of `18-24`,`25-29`,`30-34`,`35-39`,`40-44`,`45-49`,`50-59`,`60+`; empty = all |
| time_context | `any`, `morning`, `midday`, `afternoon`, `evening`, `late_night` |
| trigger | `random`, `scheduled`, `accountability`, `smart`, `urge`, `reset`, `welcome_back`, `anniversary`, `high_risk_window` |
| lines | 1–3 short lines, each ≤ 90 characters, spoken by the persona |
| actions | 1–2 of `dismiss`, `urge_mode`, `breathe`, `save`, `open_journey`, `open_learn`, `snooze` |
| animation | `idle`, `enter`, `look`, `smile`, `laugh`, `think`, `encourage`, `serious`, `breathe`, `pray`, `celebrate`, `wave`, `point`, `nod` |
| haptic | `none`, `tap`, `double_tap`, `heartbeat`, `soft` |
| evidence_type | `behavioral`, `philosophical`, `scripture`, `humor`, `health`, `none` |
| source | string or null. Required when `evidence_type` is `scripture` (book chapter:verse) or `health` (claim id from science/claims.json) |
| safety_level | `safe` only for shipped content |
| premium | boolean |

### Hard rules for Moments
1. **Never invent biology.** A Moment may say "Your body is doing its own work" but never a number, a percentage, a hormone level or a sperm count. Anything health-related must cite a `claim_id` from `content/science/claims.json` and only paraphrase that claim.
2. **No shame.** Never call the user a failure, weak, disgusting, or similar. Resets are chapters ending, not failures.
3. **Get out of the way.** 1–3 lines, readable in 5–15 seconds.
4. **Not every Moment is about retention.** Target mix: 30% accountability, 20% humor, 15% perspective, 10% health, 10% faith (Grace mostly), 10% wellbeing, 5% random.
5. **No medical instruction.** No "stop your medication", no diagnoses.
6. **Original voice.** No quotes from living people or copyrighted works. Public-domain scripture (KJV/WEB) and classical Stoic paraphrase are allowed with `source`.

## Persona (content/personas.json)
See file. Fields: id, name, tagline, archetype, description, default_tone, voice_rules, palette, recommended_for (goals), premium.

## Evidence claim (content/science/claims.json)
Fields: claim_id, claim, topic, age_min, age_max, evidence_level (`strong`|`moderate`|`limited`|`tradition`), direction, source_url, source_title, publication_year, study_type, review_date, medical_reviewer, status (`active`|`retired`).

## Lesson (content/science/lessons.json)
Fields: lesson_id, category (`your_body`|`retention`|`urges_habits`|`age`), title, read_seconds, blocks: [{type:`text`|`claim`|`callout`, text?, claim_id?}].

## Biology timeline (content/science/timeline.json)
Ordered phases with day ranges, title, summary, claim_ids.

## Interventions (content/interventions.json)
Urge-mode interventions: id, title, instruction lines, duration_seconds, category, persona_line overrides.
