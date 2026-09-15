# DUBL Parity Contract + Magic Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Establish one executable Android/Desktop rules/content contract and close Magic backend/content parity against Android 0.6.2.

**Architecture:** `shared/commonMain` is the single rule/catalog/application-behavior source. Android repositories remain platform adapters but read the same shared resources and call the same parsers; characters persist an explicit DUBL 3.69 ruleset reference for future migration/server work.

**Tech Stack:** Kotlin Multiplatform, Kotlin/JVM harnesses, Jetpack Compose, Compose Desktop, Python unittest/pytest-style guards, Gradle 9.7 CI.

**Spec:** `docs/superpowers/specs/2026-09-15-parity-contract-ruleset-magic-design.md`

## Global Constraints

- Android 0.6.2 behavior remains the current product reference.
- No Web work and no server implementation.
- No universal JSON/rule scripting language.
- No UI redesign; only behavior/accessibility changes needed for parity.
- No rule formula duplicated into Android or Desktop presentation code.

---

### Task 1: Canonical catalog source and parser contract

**Files:**
- Create: `tools/tests/test_ruleset_parity_contract.py`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/dubl/character/android/data/{DevelopmentCatalogRepository,ChiCatalogRepository,MagicEquipmentCatalogRepository,SkillEffectCatalogRepository}.kt`
- Delete: `app/src/main/assets/{development_catalog,chi_catalog,magic_equipment_catalog,skill_effects_catalog}.json`
- Modify: content tests that still address Android duplicate assets directly.

**Interfaces:**
- Consumes: `parseDevelopmentCatalog`, `parseChiCatalog`, `parseMagicEquipmentCatalog`, `parseSkillEffectCatalog`.
- Produces: one physical canonical resource tree under `shared/src/commonMain/resources`.

- [x] Write a failing contract test asserting one canonical resource copy, shared parser delegation, asset source mapping, unique IDs, expected catalog counts, and Archmage spell presence.
- [x] Run the contract test and confirm failure on duplicate Android assets/parser implementations.
- [x] Map shared resources into Android's main assets and replace Android JSON parsing with shared parser calls.
- [x] Remove duplicate JSON assets and retarget existing content tests to shared resources.
- [x] Run contract/content/import tests and confirm green.

### Task 2: Persist ruleset identity

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/model/RulesetModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/data/SnapshotCodec.kt`
- Modify: `tools/tests/kotlin/PersistenceHarness.kt`
- Modify: persistence/source regression tests.

**Interfaces:**
- Produces: `RulesetRef(id: String, version: String)` and `DublRuleset.reference` (`dubl`, `3.69`).
- `DublCharacter.ruleset` defaults to `DublRuleset.reference`.
- Snapshot schema becomes 8; schema <=7 without a ruleset decodes as DUBL 3.69.

- [x] Add failing persistence assertions for schema 8, encoded ruleset ID/version, and schema-7 migration.
- [x] Run persistence harness and confirm failure.
- [x] Implement minimal model/codec changes.
- [x] Run persistence harness and normalization tests; confirm green.

### Task 3: Magic shared-behavior parity

**Files:**
- Create: `tools/tests/kotlin/MagicParityHarness.kt`
- Create: `tools/tests/test_magic_backend_parity.py`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterSession.kt`
- Modify: `shared/src/commonTest/kotlin/com/dubl/character/android/model/MagicEquipmentRulesTest.kt` only if rule-level coverage needs extension.

**Interfaces:**
- `CharacterSession.addCatalogSpell()` rejects incomplete entries.
- School add/update/remove during creation synchronizes current mana with effective maximum where appropriate.
- Existing `setMagicManaRank`, `setMagicSchoolPower`, `changeMana`, spell mutation APIs remain the only mutation surface.

- [x] Write a failing Kotlin harness covering incomplete-entry rejection, catalog field preservation, creation-time mana synchronization, creation lock, clamping, and XP formulas.
- [x] Run harness and confirm targeted failures.
- [x] Implement minimal shared-session fixes.
- [x] Run harness and shared rule tests; confirm green.

### Task 4: Compose Desktop Magic behavior access

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/UiPrimitives.kt`
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/MagicScreen.kt`
- Modify: `tools/tests/test_compose_magic_parity.py`

**Interfaces:**
- `RankStepper` gains an optional `enabled` argument with a default preserving all existing callers.
- Desktop Magic routes current-mana +/- through `CharacterSession.changeMana` and visibly locks mana rank after creation.

- [x] Add failing source regressions for mana +/- controls, creation lock, incomplete-entry disablement, learned-only usability warning, and custom `manaText` synchronization.
- [x] Run them and confirm failure.
- [x] Implement minimal Compose changes with no layout redesign.
- [x] Run Magic source tests and compile-oriented guards.

### Task 5: Release gate and artifact

**Files:**
- Modify: `.github/workflows/linux-appimage.yml`
- Modify: `tools/tests/test_compose_release_ready.py`
- Modify: `CHANGELOG.md` / `HANDOFF.md` only to document the architectural contract and verification status.

**Interfaces:**
- Linux CI runs the new ruleset and Magic backend parity guards before Compose compilation/AppImage packaging.

- [x] Add failing release-gate assertion for the new tests.
- [x] Wire tests into CI.
- [x] Run all focused tests, Kotlin harnesses, then the full fast source/parity suite.
- [x] Run available Gradle compile/test gate; if network blocks dependency resolution, report that exact limitation without claiming binary verification.
- [x] Generate an incremental patch against the hotfixed baseline and a complete source snapshot; verify `git apply --check` on a clean baseline copy.

### Task 6: Equipment backend/content parity

**Files:**
- Create: `tools/tests/kotlin/EquipmentParityHarness.kt`
- Create: `tools/tests/test_equipment_backend_parity.py`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/MagicEquipmentModels.kt`
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/EquipmentScreen.kt`
- Modify: `.github/workflows/linux-appimage.yml`

**Interfaces:**
- Shared equipment load calculation treats quantity as a minimum-1 model invariant even before persistence normalization.
- Desktop quantity/load editing delegates clamping/normalization to shared behavior rather than adding narrower presentation limits.
- Desktop catalog search addresses the same canonical equipment fields as Android.

- [x] Add a failing Kotlin harness for quantity/load/capacity/burden invariants.
- [x] Confirm the raw quantity=0 case fails before the rule fix.
- [x] Fix the invariant in shared rules and align Desktop mutation/search behavior.
- [x] Add the equipment harness to the Linux parity gate and confirm focused tests green.

### Task 7: Final Character Sheet / Skills / Rolls rules-boundary audit

**Files:**
- Create: `tools/tests/kotlin/RulesBoundaryHarness.kt`
- Create: `tools/tests/test_rules_boundary_contract.py`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterRollContexts.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/SkillEffectModels.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterEconomy.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterModels.kt`
- Modify: Android/Desktop presentation callers that still own duplicated rule constants or formula components.
- Modify: `.github/workflows/linux-appimage.yml`

**Interfaces:**
- `RollContext` owns allowed skill/attribute choices.
- `SkillRollEffectOption.selectedTotals()` owns selected roll-effect aggregation.
- Shared economy/rules own Chi bonus-rank XP and magic-school rank XP.
- `DublCharacter` owns size modifiers, passive derived-stat bonuses, run multiplier, and run passive components used both for totals and explanation UI.

- [x] Add failing contract/harness coverage for duplicated combat-roll choices, effect aggregation, Chi/Magic XP constants, and derived-stat explanation components.
- [x] Move those rule components to shared without changing canonical DUBL behavior.
- [x] Update Android/Desktop callers to consume shared components.
- [x] Make Character Sheet explanations enumerate every component actually used by Defense, Reflexes, Initiative, Fortitude, Run, and Size math.
- [x] Run focused contract/backend/source suites and confirm green.
