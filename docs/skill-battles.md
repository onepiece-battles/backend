# Versioned skill battles (engine `skills-v1`)

This release adds 50 curated character presets and 115 active skill configurations. It covers
single-target/area damage, healing, shields, guarding, evasion, attack buffs/debuffs, defense debuffs,
poison, bounded interruption, energy recovery and Conqueror coating. Area attacks hit the one
opponent in the existing single-battle mode; this does not introduce group battles or summoned units.

Values, costs, cooldowns, mastery levels and restrictions are **game balance choices**, not canonical
One Piece measurements. Source links and notes describe the ability basis. Several active move labels
are descriptive game adaptations. Publication of this preset catalogue is part of release review.
Do not infer an unlisted advanced ability from bounty, score, position, or ordinary Conqueror Haki.

## Identity, versions and future skills

`battle/catalog-v1.json` is the reviewed bootstrap catalogue. Only system characters whose full name
or curated exact alias matches a preset receive that preset. Matching is not substring search; fake
Luffy does not inherit Luffy's powers, and contradictory Chinese/English matches receive a generic
profile. User-created characters start with basic actions unless NIKA explicitly publishes a profile.

On first use, the complete profile is persisted **by character UUID**. A later rename/image edit or
catalogue edit cannot silently replace it. Existing profiles always come from storage. Skill cooldowns,
remaining uses, shields, control immunity and buffs live only inside the per-battle actor and reset
for the next battle. Fruit moves require the actual matching held fruit. Coating requires both a
nonzero reviewed coating level and the stored Haoshoku tag; possessing Haoshoku alone never grants it.
The Whitebeard preset uses Marineford and deliberately does not inherit his prime-era coating.

Publishing appends a new character-profile version. Changed skill definitions must also advance their
own skill version; removing a skill and later reintroducing it cannot reuse a changed old definition.
Future battles use the latest published profile. Earlier battles retain their full frozen definitions.
A new skill can combine the existing effect primitives; a genuinely new effect needs code, validation,
tests and a new engine version. Arbitrary scripts, executable expressions and AI-authored rule code
are not supported.

## Calculation and persistence

`CombatInputs.freeze` locks participants in UUID order and gathers character attributes, fruits, haki
and profile versions inside a database transaction. That transaction ends before the AI request.
`CombatSession` stores a full JSON snapshot, the engine version, the random seed (as a string in JSON
so browsers do not lose 64-bit precision), both participants and the accepted tactic preferences.

Every action records the actor, skill ID/version, actual energy consumed, misses and actual effects.
Effect units distinguish HP, energy, percentage basis points, mastery levels and action opportunities.
Existing process rows still expose readable `attack_skill` text, so existing PDF reports work. Structured
events add healing, status and control detail to the frontend without rewriting old battle rows.
Historical battles without the new snapshot keep using their original round data.

The final round/status/event writes, winner/draw and score writes share a transaction. Participant rows
are locked in UUID order during finalization; score calculation uses their latest ratings so concurrent
battles do not overwrite one another's rating changes. Duplicate finalization cannot apply scores twice.
Reports are scheduled after the result transaction completes. Exhaustion still counts as defeat, as in
the existing game. Mutual defeat records the existing draw representation (FAILED, no winner, no rating
change). The existing turn limit also remains.

Initiative now selects the first and subsequent attackers from speed/intelligence using a seeded RNG.
Dodge uses the actual defender, symmetric comparisons and floating-point arithmetic. Chance is bounded.
Damage is still calculated by the backend. Healing is capped at maximum HP; shields do not stack beyond
their cap; interruption lasts one action and grants an immunity window. Fruit reversion removes the
actor's fruit-derived buffs while preserving haki and opponent-inflicted effects. It retains HP as in
the previous engine. Skill energy must be affordable; special moves cannot overdraw the resource.

Coating is an active, costly window rather than a permanent base-stat edit. Each level adds 8% attack
multiplier, 5% defense penetration and 6% defensive reduction while active, with additional energy cost
per action and expiry on the actor's action opportunities. These initial values require balance review.
Usopp's sourced Negative Hollow immunity is a specific tag interaction, not immunity to all control or
Perona's explosive attacks.

## AI tactics

The existing server-held DeepSeek settings/client are reused. There is at most one short pre-battle
request ranking the supplied skill IDs for each participant. Only known IDs belonging to that actor
survive validation; attributes, damage, winners, new abilities and arbitrary tool calls are ignored.
The engine selects a legal, affordable action using current HP, status, cooldowns and bounded AI
preferences. The AI does not control every hit or generate new skills mid-battle.

AI is optional. Missing configuration, malformed output, provider failure, a six-second deadline or
both dedicated workers being occupied immediately selects built-in tactics. Network work is bounded
and not performed while participant rows are locked. Disable requests with:

```yaml
onepiece:
  battle:
    ai-tactics-enabled: false
```

This release records true structured outcomes and existing PDF skill descriptions. Free-form AI report
writing, a general reaction-skill trigger editor and multi-character battle resolution are later work.

## Additive endpoints

All endpoints require the existing authenticated client. Character/battle reads check IAM plus system,
owner or explicit share scope. Publication and the complete match inventory require NIKA.

- `GET /api/onepiece/combat/catalog`: preset catalogue.
- `GET /api/onepiece/combat/catalog/matches`: actual system-character IDs and exact preset matches.
  This is the way to audit coverage of the live ~300-character database; code tests do not claim that
  all 50 presets already match a production row.
- `GET /api/onepiece/combat/characters/{id}`: latest stored profile, or its read-only initial preview.
- `POST /api/onepiece/combat/characters/{id}/publish`: `{expectedVersion, confirmed:true, profile}`.
  Read the profile, review the entire next version, increment `profile.version`, and explicitly confirm.
  A preview at v1 can be published as v2; its initial v1 is saved in the same transaction. Brand-new
  custom publication may instead use expectedVersion 0 / profile.version 1. Stale versions return 409.
  Invalid effects/values return 400. AI never calls this endpoint automatically.
- `GET /api/onepiece/combat/battles/{id}`: `{snapshot, events}`; legacy battles return a null snapshot.

The frontend adds read-only skill descriptions and eligibility, frozen versions/tactic source on the
battle timeline, actor names from the frozen snapshot and actual recovery/status effects. It uses the
shared authenticated/session-expiry client. Missing additive endpoints on an older server preserve the
existing resource and battle screens.

## Deployment and verification

Flyway V032 adds `character_combat_profile` and `battle_combat_record`. No existing resource attributes,
ratings, skill-less historical rounds or avatars are backfilled. Presets bind lazily on first battle;
review `/combat/catalog/matches` after deployment to see which actual IDs match. Deploy backend before
frontend. No merge or production deployment is performed by the implementation task.

JDK 17:

```sh
./gradlew test --tests 'com.yichi.onepiece.battle.*' --tests 'com.yichi.onepiece.assistant.*' bootJar --console=plain
```

Tests cover immutable profiles, real persistence/rollback/concurrent first initialization, scope and
confirmation checks, explicit fruit requirements, cooldown/use resets, control immunity, healing caps,
seeded reproduction, coating strength and source-specific immunity. A 300-battle equal-stat mechanics
smoke simulation writes `build/reports/combat-simulation.txt`; it is not a canonical power ranking or a
proof of final balance. A disposable MySQL run verifies the full migration history and the persistence
suite without using the application's database. No live paid DeepSeek calls are required by tests.

Flutter Web tests that transitively import `package:web` run with `flutter test --platform chrome`.
The previous application-datasource-dependent full backend suite remains separate from these isolated
regressions.
