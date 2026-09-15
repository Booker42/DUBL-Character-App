# Desktop UI Structural Redesign v1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a full-width desktop shell, reusable desktop UI primitives, and a dense Character Sheet dashboard without changing shared DUBL behavior.

**Architecture:** Keep all rules/state mutation behind the existing `DesktopAppState`/`DublApplication` boundary. Compose Desktop owns shell geometry, vector/icon presentation primitives, and sheet composition. Persisted notes extend only `CharacterSheetExtras` metadata and the typed sheet capability; rules and character math remain untouched.

**Tech Stack:** Kotlin 2.4.x, Compose Multiplatform Desktop 1.12.0, Material 3, existing Python source-contract tests.

**Spec:** `docs/superpowers/specs/2026-09-16-desktop-ui-structural-redesign-v1-design.md`

## Global Constraints

- Do not modify shared rules, catalogs, formulas, or character mutation semantics. A narrow `CharacterSheetExtras.notes` metadata field and typed `SheetApplication.setNotes` operation are permitted for persisted notes; no ruleset behavior may depend on them.
- Do not add app-level horizontal scrolling.
- Preserve every existing `DesktopAppState` callback used by the Character Sheet.
- Keep compact/normal/wide behavior.
- Redesign only the shell, desktop primitives, and Character Sheet in this slice.

---

### Task 1: Structural UI contract

**Files:**
- Create: `tools/tests/test_desktop_ui_structural_redesign.py`

**Interfaces:**
- Consumes: current desktop source files.
- Produces: source-level guardrails for Tasks 2–4.

- [ ] **Step 1: Write the failing source-contract tests** asserting removal of the page width cap/footer, existence of the new primitives, and Character Sheet composition tokens.
- [ ] **Step 2: Run `python -m pytest tools/tests/test_desktop_ui_structural_redesign.py -q` and confirm failure against the old UI.**
- [ ] **Step 3: Commit the red test.**

### Task 2: Full-width desktop shell

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/Main.kt`

**Interfaces:**
- Consumes: `DesktopSection`, `DublLayoutClass`, `DesktopAppState`.
- Produces: full-width `DesktopContent`, grouped primary navigation, separated Characters navigation, prototype-footer removal.

- [ ] **Step 1: Remove the NORMAL/WIDE `widthIn(max = ...)` page cap and use responsive outer padding.**
- [ ] **Step 2: Make the rail 220 dp, visually flatter, and separate Characters below a spacer/divider.**
- [ ] **Step 3: Remove the prototype/version footer and keep character switching/navigation semantics unchanged.**
- [ ] **Step 4: Run the structural test and existing host/parity tests.**

### Task 3: Desktop presentation primitives

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/UiPrimitives.kt`

**Interfaces:**
- Produces: `DesktopPanel`, `DesktopSectionHeader`, `DesktopHeroPanel`, `DesktopStatCell`, `DesktopMetricCell`, `DesktopResourceRow`, `DesktopConditionChip`, `DesktopSmallAction`.

- [ ] **Step 1: Add panel/header primitives using existing shared DUBL theme tokens.**
- [ ] **Step 2: Add compact stat/metric/resource/chip/action primitives with no domain mutation logic.**
- [ ] **Step 3: Run structural/source tests.**

### Task 4: Character Sheet dashboard

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt`

**Interfaces:**
- Consumes: Task 3 primitives and existing `DesktopAppState` callbacks.
- Produces: responsive hero, resource/stat dashboard, condition chips, metric strip, and responsive lower summaries.

- [ ] **Step 1: Wrap sheet content in `BoxWithConstraints` and compute compact/normal/wide sheet presentation from available content width.**
- [ ] **Step 2: Replace the identity card and standalone conditions card with `DesktopHeroPanel`, portrait placeholder/image, condition chips, and compact actions.**
- [ ] **Step 3: Replace the resource card rows with `DesktopResourceRow` and compose resources beside characteristics on desktop widths.**
- [ ] **Step 4: Replace nested attribute cards with `DesktopStatCell`, preserving XP metadata, mutations, Undo markers, and roll callbacks.**
- [ ] **Step 5: Replace the derived-stat key/value card with responsive `DesktopMetricCell` items and preserve quick-check callbacks.**
- [ ] **Step 6: Compose skills/development summaries side-by-side when width permits and keep grouped ordering/actions unchanged.**
- [ ] **Step 7: Run structural tests plus the complete available Compose/parity source suite.**

### Task 5: Release artifact and verification

**Files:**
- Modify: `CHANGELOG.md`
- Generate: cumulative `.patch`
- Generate: complete source `.zip`

**Interfaces:**
- Produces: user-applicable patch/source snapshot from the exact latest baseline plus the redesign.

- [ ] **Step 1: Record the desktop structural redesign without claiming binary verification that was not run.**
- [ ] **Step 2: Run `git diff --check`, focused source tests, and all feasible offline regression tests.**
- [ ] **Step 3: Generate the patch against the latest pre-UI baseline and a source zip excluding transient build/cache/git directories.**
