# Desktop Hero Telemetry + Skills/Development Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Move characteristics and indicators into the hero, add quick rolls for Reflexes/Initiative/Fortitude/Run, and place Skills beside Development at roughly 35/65 width.

**Architecture:** Keep data/state mutations behind the existing DesktopAppState/Shared Application APIs. Add only the missing shared `RUN` roll context; all layout changes stay in Desktop Compose presentation and reuse current grouping/hidden-skill models.

**Tech Stack:** Kotlin Multiplatform, Compose Desktop, shared Kotlin model/rules, Python source-contract tests.

**Spec:** `docs/superpowers/specs/2026-09-16-desktop-hero-telemetry-workspace-design.md`

## Global Constraints

- Desktop version remains 0.2.0.
- Preserve Shared Application mutation boundaries.
- Preserve all visible skills including rank 0; hidden skills remain hidden without losing group placement.
- Preserve shared development parent/requirement hierarchy and group ordering.
- No horizontal scrolling.
- Defense and Size have no quick-roll button.
- Reflexes, Initiative, Fortitude, and Run have quick-roll buttons.

---

### Task 1: Shared Run quick-roll preset

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterRollContexts.kt`
- Modify: `shared/src/commonTest/kotlin/com/dubl/character/android/model/RulebookCoreSkillsRollsTest.kt`
- Modify: `tools/tests/test_rulebook_core_skills_rolls_contract.py`

**Interfaces:**
- Produces: `RollContext.RUN` handled by `DublCharacter.rollPreset(...)`.

- [x] Add failing source/Kotlin tests requiring a Run roll preset.
- [x] Run targeted tests and confirm failure is caused by missing `RollContext.RUN`.
- [x] Add `RUN("Бег")` and a preset using the integral `runFull` bonus.
- [x] Re-run targeted tests and confirm pass.

### Task 2: Hero telemetry strips

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt`
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/UiPrimitives.kt`
- Modify: `tools/tests/test_desktop_ui_structural_redesign.py`

**Interfaces:**
- Consumes: existing attribute mutations, `ContextRollRequest`, resource callbacks, `RollContext.RUN`.
- Produces: compact hero attribute and metric cells/strips.

- [x] Add failing structural assertions that attributes/metrics live in the hero and Run/Reflexes/Initiative/Fortitude expose dice actions.
- [x] Run the structural test and confirm failure on the old body panels.
- [x] Add compact hero primitives and render the telemetry strips inside `CharacterHero`.
- [x] Remove the standalone characteristics/metrics body columns.
- [x] Re-run the structural test and confirm pass.

### Task 3: Skills 35% + Development 65% workspace

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt`
- Modify: `tools/tests/test_desktop_ui_structural_redesign.py`

**Interfaces:**
- Consumes: existing `SheetSkillsPanel`, normalized `skillGroups`, `DevelopmentRules`, `developmentGroups`, `DevelopmentTreeRow`.
- Produces: `SkillsDevelopmentWorkspace` with wide/normal 35/65 row and compact stack; Notes below.

- [x] Add failing structural assertions for the 35/65 Skills/Development workspace and standalone Notes panel.
- [x] Run structural tests and confirm failure against old `SheetSummaries` layout.
- [x] Extract the development panel into a reusable composable and place it beside Skills.
- [x] Keep Notes as the next compact full-width section.
- [x] Re-run structural/UI/shared contract tests.

### Task 4: Packaging verification

**Files:**
- Generate incremental patch and complete source ZIP in `/mnt/data/dubl-hero-stats-skills-pass/`.

- [x] Run `git diff --no-index --check` equivalent / patch whitespace validation.
- [x] Run targeted Python UI/shared regression suite.
- [x] Attempt available Kotlin/Gradle verification; report environment limitations rather than claiming unrun checks.
- [x] Generate incremental patch from the previous skill-development-parity source baseline.
- [x] Zip the updated complete source snapshot.
