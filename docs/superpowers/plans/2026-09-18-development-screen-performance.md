# Development Screen Performance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Android development navigation responsive and lazily render grouped development content on Android and Desktop.

**Architecture:** Put platform-neutral group expansion/search behaviour and development indexing in shared Kotlin. Android retains prepared catalog-derived data outside the screen composition, while both Compose frontends emit child rows only for expanded or search-matching groups.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose, Compose Multiplatform, kotlin.test.

**Spec:** `docs/superpowers/specs/2026-09-18-development-screen-performance-design.md`

## Global Constraints

- The owned tab remains immediately visible.
- Search must not destroy manual expansion state.
- Closed groups must not emit or compose child cards.
- Android and Desktop must share the same visibility semantics.
- No new third-party dependency.

---

### Task 1: Shared group visibility policy

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/ui/development/DevelopmentGroupVisibility.kt`
- Test: `shared/src/commonTest/kotlin/com/dubl/character/android/ui/development/DevelopmentGroupVisibilityTest.kt`

**Interfaces:**
- Produces: `DevelopmentGroupVisibility`, `visibleGroupIds`, and `isGroupExpanded` for both frontends.

- [ ] Write tests proving collapsed defaults, multiple manual expansions, search expansion, and restoration after clearing search.
- [ ] Run the focused common test and verify it fails because the policy is absent.
- [ ] Implement the immutable policy with manual expanded IDs preserved independently from query state.
- [ ] Run the focused test and full shared tests.

### Task 2: Prepared development index

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/model/DevelopmentScreenIndex.kt`
- Test: `shared/src/commonTest/kotlin/com/dubl/character/android/model/DevelopmentScreenIndexTest.kt`

**Interfaces:**
- Consumes: `DevelopmentCatalog`, `DevelopmentEntry`.
- Produces: stable type/group/search metadata and filtered grouped results without repeated normalization.

- [ ] Write tests for martial grouping and normalized search matching.
- [ ] Run the focused test and verify the missing index failure.
- [ ] Implement one-time indexed metadata construction and selection.
- [ ] Run focused and full shared tests.

### Task 3: Android retained preparation and collapsed groups

**Files:**
- Modify: `app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt`
- Create: `app/src/main/java/com/dubl/character/android/ui/screens/DevelopmentScreenCache.kt`
- Test: `app/src/test/java/com/dubl/character/android/ui/screens/DevelopmentScreenCacheTest.kt`

**Interfaces:**
- Consumes: shared index and visibility policy.
- Produces: retained prepared availability/economy keyed by character and effective-catalog inputs.

- [ ] Write cache tests proving identical inputs reuse preparation and changed development inputs invalidate it.
- [ ] Run focused Android tests and confirm expected failure.
- [ ] Add bounded retained preparation and integrate indexed filtering.
- [ ] Replace eager group emission with clickable headers and conditional child `items`.
- [ ] Run Android tests and compile production Kotlin.

### Task 4: Desktop collapsed groups

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt`

**Interfaces:**
- Consumes: shared visibility policy and development index.
- Produces: Desktop group headers whose children exist only while expanded or searching.

- [ ] Map current Desktop tabs and grouped list emission to shared group IDs.
- [ ] Add expansion state and search override using the tested shared policy.
- [ ] Conditionally emit rows only for visible expanded groups.
- [ ] Compile Desktop Kotlin.

### Task 5: Regression and artifact verification

**Files:**
- Modify: `README.md`, `CHANGELOG.md`, `HANDOFF.md`

**Interfaces:**
- Produces: documented performance behaviour and distributable patch/source archive.

- [ ] Run shared and Android test suites.
- [ ] Compile Android and Desktop targets.
- [ ] Inspect the final diff for unrelated changes and placeholder text.
- [ ] Create the patch and updated source archive.
