# DUBL Parity Contract, Ruleset Boundary, and Magic Design

## Goal

Make Android and Compose Desktop consume one canonical DUBL 3.69 rules/content implementation, establish executable parity guards, and close the remaining Magic backend/content behavior gaps without redesigning either UI.

## Boundaries

- `shared/commonMain` owns domain models, deterministic rules, mutations through `CharacterSession`, catalog parsers, and canonical catalog payloads.
- Android and Desktop may adapt storage/resources/UI, but must not own copies of rule formulas or catalog parsing semantics.
- Canonical DUBL catalog JSON files live once under `shared/src/commonMain/resources`.
- Android exposes those same shared files through its asset source set so existing platform loading remains cheap and compatible.
- `DublCharacter` carries a persisted ruleset identity. Existing schema-7 snapshots migrate to DUBL 3.69 automatically.
- No generic scripting language, server API, remote repository, or second ruleset is introduced now.

## Parity contract

Automated guards must prove:

1. Android and Desktop route mutations through the shared `CharacterSession`.
2. Android catalog repositories delegate parsing to shared parsers.
3. There is only one canonical copy of each DUBL catalog payload.
4. Catalog IDs are unique and critical imported Archmage content is present.
5. Ruleset identity survives persistence and old snapshots default to DUBL 3.69.
6. Magic mutations, costs, mana limits, schools, learned/custom spells, incomplete catalog entries, and XP semantics are enforced in shared code rather than presentation code.

## Magic behavior

- Mana rank remains 0..5 and is mutable only during character creation.
- Current mana is clamped to effective maximum on every mutation.
- During creation, changes that increase/decrease magic-school power keep mana synchronized to the resulting maximum, matching the creation-time behavior already used by mana rank and direct school-power controls.
- Canonical catalog entries marked `incomplete` cannot be added through `CharacterSession`; UI disablement is supplemental only.
- Catalog spell fields are preserved exactly when added to the character.
- Custom spell edits keep `manaText` consistent with the numeric mana cost.
- Desktop exposes the same current-mana +/- mutation path and creation lock as Android.

## Future compatibility

The persisted `ruleset` reference is the migration hook for future imported rulesets and a later remote repository/server. It does not make the current domain generic prematurely; DUBL 3.69 remains canonical ruleset #1.
