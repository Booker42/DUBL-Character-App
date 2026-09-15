# DUBL — Android 0.6.2 + Desktop 0.2.0

This source snapshot contains the canonical Android 0.6.2 application and the Compose Desktop 0.2 parity migration. Android 0.6.2 remains the behavioral reference while shared Kotlin now owns the executable rules, application mutations, catalog parsers, and the single canonical catalog payloads. Shared model/rules/application code is Kotlin Multiplatform. Web/Wasm, server accounts, and sync are intentionally out of scope.

## Modules

- `shared` — platform-independent model/rules, application sessions, persistence contracts/codecs, canonical catalogs, design tokens, and reusable Compose primitives;
- `app` — canonical Android 0.6.2 application and Android persistence;
- `desktopApp` — primary Compose Desktop frontend with Character Sheet, Skills/Rolls, Development/Martial Arts/Chi, Magic, Equipment, and Characters;
- `packaging/linux/portable-src` — legacy parity oracle/fallback retained only for regression comparison in restricted environments. It is no longer the canonical desktop release target.

## Current versions

- **Android 0.6.2** — canonical behavior/rules reference.
- **Desktop 0.2.0** — Compose Desktop functional-parity target over the same shared application/domain layer.

## Desktop 0.2 functionality

The Compose desktop frontend is wired to the real desktop stores, shared `CharacterSession`, shared extras session, and canonical catalogs. It contains all six Android-equivalent workflows:

- Character Sheet: identity, XP/creation economy, attributes, resources/overrides, portrait, conditions, derived details, quick checks, recent-change Undo, learned summaries, and persistent grouping/order;
- Skills/Rolls: search/filter, ranks/XP, multiple attributes, preferred attribute, modifiers/notes, hide/restore, custom/specialized skills, and rule-aware roll modes/follow-up;
- Development: regular/special progression, prerequisites, force availability, branches, martial arts, Chi resource/techniques, XP/AP economy;
- Magic: mana progression/recovery, schools, power, spellbook/catalog, custom spells, learned state and XP overrides;
- Equipment: catalog/custom gear, quantity, carried state, automatic/manual load, capacity and burden;
- Characters: create, list, switch active character, delete, and persistent roster state.

Android and Desktop use schema-8 character persistence with an explicit `dubl` / `3.69` ruleset reference; schema-7 saves migrate to that identity automatically. Game formulas are not duplicated in either platform UI. Canonical catalog JSON lives only in `shared/src/commonMain/resources`; Android exposes those same files as assets and delegates parsing to shared code.

## Linux release

The canonical Linux release path is now Compose Desktop:

```bash
DUBL_VERSION=0.2.0 packaging/linux/build-appimage.sh
```

The script builds `:desktopApp:createDistributable`, bundles the JVM runtime produced by Compose Desktop, then wraps the distributable as an AppImage. `.github/workflows/linux-appimage.yml` runs parity tests, `:shared:desktopTest`, `:desktopApp:compileKotlin`, and the AppImage build before publishing artifacts.

The legacy `build-portable-appimage.sh` remains only as a restricted-environment fallback/oracle and must not be used for normal releases.

## Local verification

```bash
./gradlew :shared:desktopTest :desktopApp:compileKotlin
./gradlew :desktopApp:run
```

Android verification remains:

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The repository uses Kotlin 2.4.20, Compose Multiplatform 1.12.0, AGP 9.3.0, Gradle 9.7.0, Android compileSdk 37 / targetSdk 36, and JVM toolchain 17 for project bytecode.

## Release tags

Android release tags remain `v0.6.2`-style. Desktop Linux releases use separate tags such as:

```bash
git tag -a desktop-v0.2.0 -m "DUBL Desktop 0.2.0"
git push origin desktop-v0.2.0
```

That tag invokes the Compose Desktop Linux release workflow.
