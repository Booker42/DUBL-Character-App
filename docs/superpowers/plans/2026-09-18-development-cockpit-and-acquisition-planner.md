# Development Cockpit and Acquisition Planner Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build one shared development acquisition planner and expose the same character-building workflow on Desktop and Android.

**Architecture:** Add a pure shared planner that recursively resolves requirements against a simulated character and returns typed acquisition steps/choices. `DevelopmentApplication` re-plans and applies a request atomically. Desktop and Android independently render the same shared plan while retaining platform-appropriate layout.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose Android, Compose Desktop, existing Python/Kotlin structural harnesses.

**Spec:** `docs/superpowers/specs/2026-09-18-development-cockpit-and-acquisition-planner-design.md`

## Global Constraints

- DUBL 3.69 canonical catalog/rules remain source of truth.
- No platform may mutate character state outside `DublApplication`.
- Unsupported/manual requirements must remain explicit and never be guessed.
- Auto-acquisition must be atomic and shared by Android/Desktop.
- Existing manual/GM override behavior remains available.

---

### Task 1: Shared acquisition plan model and recursive planner

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/model/DevelopmentAcquisition.kt`
- Create: `tools/tests/kotlin/DevelopmentAcquisitionHarness.kt`
- Create: `tools/tests/test_development_acquisition_planner.py`

**Interfaces:**
- Produces: `DevelopmentAcquisitionRequest`, `DevelopmentAcquisitionPlan`, `DevelopmentAcquisitionStep`, `DevelopmentAcquisitionChoice`, `DevelopmentAcquisitionPlanner.plan()`.

- [ ] Write a Kotlin harness covering recursive development prerequisites, attribute/skill prerequisites, duplicate suppression, OR cheapest-path selection and override, unresolved requirements, and totals.
- [ ] Run the harness through pytest and verify RED because the planner API is absent.
- [ ] Implement the pure shared planner with simulated-character progression and typed steps.
- [ ] Run the focused harness and existing DevelopmentRules harnesses; verify GREEN.

### Task 2: Atomic shared application operation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/application/DevelopmentApplication.kt`
- Modify: `app/src/main/java/com/dubl/character/android/state/CharacterController.kt`
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt`
- Test: `tools/tests/test_development_acquisition_planner.py`

**Interfaces:**
- Produces: `DevelopmentApplication.acquire(catalog, request): DevelopmentAcquisitionResult`, platform `acquireDevelopment(...)` adapters.

- [ ] Extend the failing harness to assert one call applies all planned state and an unresolved plan applies nothing.
- [ ] Verify RED.
- [ ] Re-plan against current state inside `DevelopmentApplication`, apply one `session.updateActive` transform, and record one undo snapshot.
- [ ] Route Android/Desktop adapters to the shared operation.
- [ ] Verify focused and application-lock tests.

### Task 3: Desktop development cockpit

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt`
- Create: `tools/tests/test_desktop_development_cockpit.py`

**Interfaces:**
- Consumes: shared planner and `DesktopAppState.acquireDevelopment`.
- Produces: responsive two-column browser, persistent inspector, plan filter/summary, acquisition preview dialog.

- [ ] Write structural tests for two-column grid, inspector, availability filters, unlock navigation, plan controls, and both acquisition actions.
- [ ] Verify RED.
- [ ] Replace regular development list cards with responsive compact cards and inspector while preserving special/martial/CHI grouping semantics.
- [ ] Add build-plan state, merged-plan summary, acquisition preview with OR selectors and atomic confirm.
- [ ] Verify focused Desktop development tests.

### Task 4: Android parity

**Files:**
- Modify: `app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt`
- Create: `tools/tests/test_android_development_cockpit.py`

**Interfaces:**
- Consumes: shared planner and `CharacterController.acquireDevelopment`.
- Produces: mobile single-column browser + bottom-sheet inspector with the same functional actions and plan preview.

- [ ] Write structural parity tests for filters, plan, unlock navigation, missing-requirement display, OR choices, and both auto-acquisition actions.
- [ ] Verify RED.
- [ ] Adapt the existing bottom-sheet detail flow to the shared cockpit model and add plan/acquisition controls.
- [ ] Verify focused Android tests and Android/Desktop parity assertions.

### Task 5: Regression verification and artifact

**Files:**
- Modify: `PROJECT_FILES.txt` only if required by existing repository convention.
- Generate: narrow cumulative `.patch` against the current Skills-browser baseline.
- Generate: complete source `.zip`.

- [ ] Run `git diff --check`.
- [ ] Run all development/CHI/shared-application Python tests that do not require network.
- [ ] Attempt the available Gradle shared/Desktop compile in offline/current environment and report exact result.
- [ ] Produce a patch containing only development-planner/UI/test/doc changes so unrelated workflow/changelog drift cannot block `git apply`.
