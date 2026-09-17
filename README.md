# FURY

This source snapshot contains the Android 0.6.2 application and the Compose Desktop 0.2 parity migration over a shared Kotlin Multiplatform rules/application layer. **DUBL 3.69 rulebooks are the authority for rules and canonical content**; Android is the mature implementation/UX reference, not an authority when it conflicts with the books. Shared Kotlin owns executable rules, application mutations, catalog parsers, and runtime catalog payloads. Web/Wasm, server accounts, and sync are intentionally out of scope.

## Modules

- `shared` — platform-independent model/rules, application sessions, persistence contracts/codecs, canonical catalogs, design tokens, and reusable Compose primitives;
- `app` — canonical Android 0.6.2 application and Android persistence;
- `desktopApp` — primary Compose Desktop frontend with Character Sheet, Skills/Rolls, Development/Martial Arts/Chi, Magic, Equipment, and Characters;
- `packaging/linux/portable-src` — legacy parity oracle/fallback retained only for regression comparison in restricted environments. It is no longer the canonical desktop release target.

## Current versions

- **FURY 0.5** — unified public release line for Android, Linux, and Windows.
- **Android 0.6.2** — historical mature implementation/UX reference used during the parity migration.
- **Desktop 0.2.0** — historical Compose parity milestone; desktop packages now ship under the FURY version line.

## Desktop 0.2 functionality

The Compose desktop frontend is wired to the real desktop stores, shared `CharacterSession`, shared extras session, and canonical catalogs. It contains all six Android-equivalent workflows:

- Character Sheet: identity, XP/creation economy, attributes, resources/overrides, portrait, conditions, derived details, quick checks, recent-change Undo, learned summaries, and persistent grouping/order;
- Skills/Rolls: search/filter, ranks/XP, multiple attributes, preferred attribute, modifiers/notes, hide/restore, custom/specialized skills, and rule-aware roll modes/follow-up;
- Development: regular/special progression, prerequisites, force availability, branches, martial arts, Chi resource/techniques, XP/AP economy;
- Magic: mana progression/recovery, schools, power, spellbook/catalog, custom spells, learned state and XP overrides;
- Equipment: catalog/custom gear, quantity, carried state, automatic/manual load, capacity and burden;
- Characters: create, list, switch active character, delete, and persistent roster state.

Android and Desktop use schema-8 character persistence with an explicit `dubl` / `3.69` ruleset reference; schema-7 saves migrate to that identity automatically. Game formulas are not duplicated in either platform UI. Canonical catalog JSON lives only in `shared/src/commonMain/resources`; Android exposes those same files as assets and delegates parsing to shared code.


## Rulebook import pipeline

DUBL 3.69 content is being moved to an auditable rulebook-first pipeline. The stock book plus approved module books are development inputs; they are **not** runtime dependencies and are not duplicated into release packages.

The current source set is:

- `core` — `Dубль All Stars 3.69 REWORK(1)(2).docx`; authoritative core DUBL 3.69 rules;
- `melee` — `Dубль, Мастера ближнего боя.docx`; authoritative for the imported Martial Arts / Chi module content;
- `archmage` — `Книга Архимага.docx`; authoritative only for approved additions to already-supported magic schools.

A local rebuild writes the complete Raw IR, source index, mirrored migration catalogs, diagnostics, and manifest under ignored `build/rulesets/dubl-3.69`:

```bash
python3 -m tools.rulebook.build_ruleset \
  --source 'core=/path/to/Dубль All Stars 3.69 REWORK(1)(2).docx' \
  --source 'melee=/path/to/Dубль, Мастера ближнего боя.docx' \
  --source 'archmage=/path/to/Книга Архимага.docx' \
  --repo-root .
python3 -m tools.rulebook.validate_ruleset build/rulesets/dubl-3.69
python3 -m tools.rulebook.check_baseline build/rulesets/dubl-3.69 \
  --baseline rulesets/dubl-3.69/baseline.json
```

If a reviewed rulebook/importer change is intentional, update the compact baseline and promoted generated artifacts explicitly:

```bash
python3 -m tools.rulebook.check_baseline build/rulesets/dubl-3.69 \
  --baseline rulesets/dubl-3.69/baseline.json --update
```

`validate_ruleset` is a structural/executable-promotion gate, not a claim that the prose rulebook has no contradictions. The existing full semantic audit found many blocker/critical ambiguities; those become blocking domain diagnostics as the affected domains are promoted. A bootstrap mirror may therefore validate structurally while still carrying warning-level ambiguous provenance.

`rulesets/dubl-3.69/resolutions.json` is the only place where an ambiguous/contradictory source may receive an explicit executable interpretation. Importers do not infer a resolution from current Android/Desktop behavior. `conditions` is the first source-generated domain; the larger Development, Chi, Magic/Equipment, and skill-effect catalogs remain explicitly marked migration mirrors until promoted domain-by-domain.

## Linux release

The canonical Linux release path is now Compose Desktop:

```bash
DUBL_VERSION=0.5.0 packaging/linux/build-appimage.sh
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

FURY uses one product version and one release tag for every shipped platform. For 0.5:

```bash
git tag -a v0.5 -m "FURY 0.5"
git push origin v0.5
```

`.github/workflows/release.yml` validates the tag, builds the signed Android APK, Linux AppImage, and Windows EXE/MSI, then publishes all artifacts into one GitHub Release titled `FURY 0.5`. The DUBL ruleset version remains `3.69` and is independent of the FURY product version.
