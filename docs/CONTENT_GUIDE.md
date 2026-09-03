# Writing for Alter Ego

A Moment is one to three short lines spoken by a companion the user chose. It is read in five to
fifteen seconds and then it is gone.

## The shape

```
buzz
Sage wants a word.
[tap]

"Hey."
        (a beat)
"You're thinking about it again, aren't you?"
        (a beat)
"Don't trade what you want long-term for ten minutes."

[ I'm good ]  [ I need help ]
```

Three lines maximum. Ninety characters per line maximum. Two actions maximum.

## Hard rules

1. **Never invent biology.** No number, percentage, hormone level or sperm count, ever. A health
   Moment must cite a `claim_id` from `content/science/claims.json` and may only paraphrase it. The
   validator and the server both refuse violations.
2. **Never shame.** No "pathetic", "disgusting", "worthless", "loser", "weakling", "you failed". A
   reset is a chapter ending, not a failure.
3. **Get out of the way.** If it needs a fourth line, it is a lesson, not a Moment.
4. **Not everything is about retention.** Someone who only ever hears "don't masturbate" will
   uninstall. Hold the mix.
5. **No real people.** No quotes attributed to living or historical figures. Paraphrase Stoic ideas
   freely. Scripture only from public-domain translations (KJV, WEB), always with book chapter:verse
   in `source`.
6. **No medical instruction.** No diagnoses, no advice about medication.

## The mix

| Category | Share | What it is |
|----------|-------|------------|
| Accountability | 30% | The commitment, directly |
| Humour | 20% | Kind, never at the user's expense |
| Perspective | 15% | Identity, time, who they are becoming |
| Health | 10% | Cited, evidence-labelled, never invented |
| Faith | 10% | Only when the user chose faith |
| Wellbeing | 10% | Water, jaw, shoulders, sleep, outside |
| Random | 5% | Small surprises |

`MomentSelector.weight` enforces this at runtime, so the library needs to be able to supply it.

## Voice per persona

**Sage** writes short declarative sentences and no exclamation marks. He asks one question at a time
and talks about identity rather than feeling.

**Coach** gives instructions. "Stand up." "Ten minutes." "Go." He corrects the action and respects
the person, always ending with a next step.

**Grace** invites rather than commands. Grace is in the name, so nothing condemns. She never claims
God is disappointed and never guarantees an outcome.

**Sunny** is fast and light. One joke, one line. At most one emoji, usually none. Accountability
arrives as a wink.

**Brother** is casual, loyal and slightly teasing. He says "I've been there" and means it. No crude
sexual language, ever.

## Writing a health Moment

Wrong:

> Day 17. Your sperm count is up 12% this week.

Right:

> Your body has been doing its own quiet work.
> More days doesn't automatically mean better. It's a trade-off.

with `evidence_type: "health"` and `source: "abstinence_motility_dna_tradeoff"`.

## Checking your work

```bash
node scripts/validate-content.mjs
```

It reports the category mix and fails on any rule violation. Fix it before syncing.
