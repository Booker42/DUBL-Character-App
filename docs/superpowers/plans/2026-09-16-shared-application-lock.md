# DUBL Shared Application Lock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish a hard shared application mutation boundary and typed golden scenarios so Android and Desktop cannot implement divergent state-changing behavior.

**Architecture:** Add `DublApplication` as the public aggregate over internal character/extras sessions and split public operations into focused capability classes. Android `CharacterController` and Desktop `DesktopAppState` become observable adapters over those capabilities; source guards forbid raw mutation/session escape hatches. Typed Kotlin golden scenarios verify deterministic snapshot and extras outcomes.

**Tech Stack:** Kotlin Multiplatform/commonMain, Jetpack Compose, Compose Desktop, Kotlin/JVM harnesses, Python pytest contract guards, Gradle 9.7 CI.

**Spec:** `docs/superpowers/specs/2026-09-16-shared-application-lock-design.md`

## Global Constraints

- Preserve current DUBL 3.69 behavior; this is not a rulebook correctness audit.
- No Android/Desktop visual redesign.
- No server, Web, generic rules scripting, or full module engine.
- No arbitrary platform mutation lambda over `DublCharacter` or `CharacterSheetExtras`.
- Golden scenarios assert behavior parity, not canonical rulebook correctness.

---

### Task 1: Shared application aggregate and hard-lock source contract

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/DublApplication.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/CharacterApplication.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/SkillsApplication.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/DevelopmentApplication.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/MagicApplication.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/EquipmentApplication.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/SheetApplication.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterSession.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterExtrasSession.kt`
- Create: `tools/tests/test_shared_application_lock.py`

**Interfaces:**
- Produces: `DublApplication(characterStore, extrasStore, idFactory, customConditionIdFactory)`.
- Produces: `application.character`, `application.skills`, `application.development`, `application.magic`, `application.equipment`, `application.sheet`.
- Produces read-only `application.snapshot`, `application.active`, `application.activeExtras`.
- Makes raw session classes and raw transform functions internal to shared.

- [x] **Step 1: Write failing source-contract tests** proving the application package/capabilities are absent and current platform/session escape hatches are still public.
- [x] **Step 2: Run `python -m pytest -q tools/tests/test_shared_application_lock.py` and confirm RED for the missing application boundary.**
- [x] **Step 3: Implement the aggregate/capability skeleton and internalize raw sessions/transform methods without changing domain semantics.**
- [x] **Step 4: Run the source contract and attempt the existing CharacterSession harness; source contract is green, while both old and new standalone Kotlin harnesses are blocked by the sandbox Kotlin 1.9 compiler performance and are superseded by the project Kotlin 2.4.20 Gradle gate.**

### Task 2: Character/sheet capabilities and Android adapter migration

**Files:**
- Modify: `shared/.../application/CharacterApplication.kt`
- Modify: `shared/.../application/SheetApplication.kt`
- Modify: `app/src/main/java/com/dubl/character/android/state/CharacterController.kt`
- Modify: `app/src/main/java/com/dubl/character/android/ui/DublApp.kt`
- Modify: `app/src/main/java/com/dubl/character/android/ui/screens/OverviewScreen.kt`
- Modify: `app/src/main/java/com/dubl/character/android/ui/screens/SkillsScreen.kt`
- Test: `tools/tests/test_shared_application_lock.py`

**Interfaces:**
- Character capability owns profile/identity, XP/creation, attributes/resources, custom resources, roster lifecycle.
- Sheet capability owns portrait reference, conditions/custom conditions, resource visibility, preferred skill attribute, and skill/development groups.
- Android controller mirrors `snapshot` and `activeExtras` after application actions; screens never save extras repositories directly.

- [x] **Step 1: Extend source-contract tests to reject Android `updateActive`, direct extras repository save, and raw sessions.**
- [x] **Step 2: Confirm RED against current Android code.**
- [x] **Step 3: Add explicit shared character/sheet methods preserving existing normalization and creation semantics.**
- [x] **Step 4: Move Android controller to `DublApplication`, migrate Overview/Skills extras writes and identity edits to typed methods.**
- [x] **Step 5: Run Android source/parity tests and the shared lock test.**

### Task 3: Skills/development/magic/equipment capabilities and Desktop adapter migration

**Files:**
- Modify: shared application capability files
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt`
- Modify: Desktop CharacterSheet/Skills/Development/Magic/Equipment/Characters screens
- Test: `tools/tests/test_shared_application_lock.py`

**Interfaces:**
- Desktop state exposes only typed capability-backed operations that refresh observable state.
- `DesktopAppState.mutate(CharacterSession.() -> Unit)` and `updateExtras(CharacterExtrasSession.() -> Unit)` are removed.
- No Desktop screen imports a raw shared session or constructs arbitrary `DublCharacter` mutation transforms.

- [x] **Step 1: Add failing source assertions for Desktop raw mutation escape hatches.**
- [x] **Step 2: Confirm RED.**
- [x] **Step 3: Route each current Desktop workflow to the corresponding shared capability, adding explicit composite operations where the UI currently batches related fields.**
- [x] **Step 4: Run Desktop parity/source guards in bounded groups.**

### Task 4: Typed golden application scenarios

**Files:**
- Create: `shared/src/commonTest/kotlin/com/dubl/character/android/application/SharedApplicationGoldenTest.kt`
- Create: `tools/tests/test_shared_application_golden.py`

**Interfaces:**
- In-memory `CharacterStore`/`CharacterExtrasStore` fixtures.
- Deterministic ID sequences.
- Typed `kotlin.test` scenarios whose actions call real `DublApplication` capabilities through the same common code used by both platforms.
- Assertions compare canonical domain/extras values, never UI rendering. `:shared:desktopTest` is the executable release gate.

- [x] **Step 1: Write the typed commonTest scenarios first, referencing the expected capability API; confirm the source contract RED before adding the commonTest file.**
- [x] **Step 2: Fill only missing shared application methods required by scenarios.**
- [ ] **Step 3: Run `:shared:desktopTest` with the project Kotlin toolchain in CI/networked Gradle; local standalone Kotlin 1.9 is not a valid substitute for the project Kotlin 2.4.20 compiler.**

### Task 5: Release gate, docs, and artifact verification

**Files:**
- Modify: `.github/workflows/linux-appimage.yml`
- Modify: `tools/tests/test_compose_release_ready.py`
- Modify: `CHANGELOG.md`
- Modify: `HANDOFF.md`
- Modify: `PROJECT_FILES.txt`

**Interfaces:**
- Linux parity gate runs the hard-lock/source contract first, then executes typed golden scenarios as part of `:shared:desktopTest` before Compose packaging.

- [x] **Step 1: Add failing release-gate assertion for the two new test files.**
- [x] **Step 2: Wire tests into CI and update architecture docs.**
- [x] **Step 3: Run focused tests, then the existing fast/source suite in bounded groups.**
- [x] **Step 4: Run available Gradle compile/test checks; report any network/environment limitation exactly.**
- [x] **Step 5: Generate a clean incremental patch and complete source snapshot; verify patch application against the untouched baseline.**
