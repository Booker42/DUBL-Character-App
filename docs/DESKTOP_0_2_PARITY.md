# DUBL Desktop 0.2.0 — Android 0.6.2 parity report

## Scope

Desktop 0.2 targets **functional parity** with canonical Android 0.6.2. Android remains the source of truth for game behavior, rules, catalogs, and mutation semantics. Desktop layout is intentionally desktop-native rather than a pixel copy of the mobile UI.

The primary frontend is now `desktopApp` Compose Desktop. The legacy portable/Swing runtime remains only as a temporary oracle/fallback and is no longer the canonical release target.

## Compose parity matrix

| Domain | Status | Compose Desktop behavior |
| --- | --- | --- |
| Character Sheet | PASS (source/parity) | identity, XP/creation economy, attributes, resources/overrides, portrait, conditions, recent-change Undo, derived details, quick checks, learned summaries, grouping/order |
| Characters | PASS (source/parity) | create, list, select, delete, active-character persistence and rail switcher |
| Skills | PASS (source/parity) | search/filter, ranks/XP, attributes, modifiers/notes, hide/restore, custom and specialized skills |
| Rolls | PASS (source/parity) | preferred attribute, normal/advantage/hindrance, target comparison, skill effects/toggles/reminders/follow-up |
| Development | PASS (source/parity) | regular/special progression, ranks/options, requirements, force availability, XP/AP summaries |
| Special branches | PASS (source/parity) | parent access/grouping and owned hierarchy |
| Martial Arts | PASS (source/parity) | style grouping, requirements, ranks and owned hierarchy |
| Chi | PASS (source/parity) | enablement, current/max, bonus ranks, restore, techniques, requirements and spending |
| Magic | PASS (source + JVM parity) | mana/current clamping, creation-only rank, school power/add/edit/delete, creation-time mana sync, spellbook/catalog/custom spells, incomplete-entry rejection, learned/XP override/usability |
| Equipment | PASS (source/parity) | catalog/custom items, quantity, carried state, auto/manual load, capacity/burden, catalog load repair |
| XP economy | PASS (source/parity) | total/start/adjustment, attribute/development/magic costs, ability points and override |
| Conditions | PASS (source/parity) | manual conditions plus automatic Weakness at zero Endurance |
| Grouping/order | PASS (source/parity) | persistent groups, rename/delete/reorder, root subtree move and independent child movement |
| Persistence | PASS (JVM harness) | schema-9 snapshot with `dubl` / `3.69` ruleset identity, schema-7 migration, desktop extras and restart round-trip |
| Catalogs | PASS (JVM/source) | one physical shared copy of development, Chi, magic/equipment and skill-effect data; Android/Desktop use the same parsers/payloads |
| Custom content | PASS (source/parity) | custom resources, skills, specialized skills, spells, schools and gear where Android supports them |

## Verification layers

The Compose migration is guarded by source/parity tests, real Kotlin/JVM harnesses for the shared application/persistence layer, and a typed Kotlin compile gate used in this restricted sandbox. CI additionally requires the real Gradle tasks:

```text
:shared:desktopTest
:desktopApp:compileKotlin
```

before AppImage packaging. The release workflow then starts the built AppImage under Xvfb with a clean HOME and only uploads it if the process remains alive for the smoke window.

## Binary verification status

A real Compose Desktop AppImage has **not** been built inside this sandbox because it has no Gradle/Compose dependency cache and shell network access is blocked. The canonical CI/local release path is ready, but final binary PASS requires a networked build followed by a clean-HOME GUI smoke test. Older portable 0.2 binaries are not evidence for the new Compose binary.
