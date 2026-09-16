# DUBL Cross-Platform Character Transfer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Export one DUBL character to a portable `.dubl` file on Android/Desktop and import it on the other platform through the shared application boundary.

**Architecture:** Add a common JSON transfer codec that embeds the existing `SnapshotCodec` character representation and a portable extras representation. Add a dedicated `CharacterTransferApplication` capability to `DublApplication`; platform UIs only perform file selection/I/O and delegate import/export semantics to shared code.

**Tech Stack:** Kotlin 2.4.x, Kotlin Multiplatform commonMain/commonTest, Jetpack Compose Android activity result contracts, Compose Multiplatform Desktop + AWT `FileDialog`, existing `MiniJson`/`SnapshotCodec`, Python source-contract tests.

**Spec:** `docs/superpowers/specs/2026-09-16-character-transfer-design.md`

## Global Constraints

- All state-changing import behavior must go through `DublApplication`; no platform adapter may mutate `CharacterStore`, `CharacterExtrasStore`, `CharacterSession`, or `CharacterExtrasSession` directly.
- Transfer v1 accepts only ruleset `dubl / 3.69`.
- Import always allocates a new top-level character ID and never overwrites an existing local character.
- Transfer preserves portable sheet extras but deliberately excludes `portraitUri` in v1.
- No new JSON/serialization dependency; reuse `SnapshotCodec` and common `MiniJson`.
- Rejected imports must leave roster and extras unchanged.

---

### Task 1: Shared transfer codec and application capability

**Files:**
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/data/CharacterTransferCodec.kt`
- Create: `shared/src/commonMain/kotlin/com/dubl/character/android/application/CharacterTransferApplication.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/application/DublApplication.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterSession.kt`
- Modify: `shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterExtrasSession.kt`
- Create: `shared/src/commonTest/kotlin/com/dubl/character/android/data/CharacterTransferCodecTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/dubl/character/android/application/SharedApplicationGoldenTest.kt`

**Interfaces:**
- Produces: `CharacterTransferCodec.encode(character, extras): String`.
- Produces: `CharacterTransferCodec.decode(raw, idFactory): CharacterTransferDecodeResult`.
- Produces: `DublApplication.transfer: CharacterTransferApplication`.
- Produces: `CharacterTransferApplication.exportActive(): String`.
- Produces: `CharacterTransferApplication.importCharacter(raw): CharacterTransferImportResult`.

- [x] **Step 1: Write failing common tests** for codec round-trip, portrait omission, invalid format/version rejection, application import with fresh ID + active selection, and no mutation on rejection.
- [x] **Step 2: Run the focused common/source test path and confirm RED** because transfer types/capability do not exist.
- [x] **Step 3: Implement the minimal codec** by embedding a one-character `SnapshotCodec` object and portable extras in the transfer root.
- [x] **Step 4: Implement the application capability** plus internal session operations needed to append a character with a fresh ID and replace extras for that ID.
- [x] **Step 5: Run focused tests and confirm GREEN**.

### Task 2: Android document import/export

**Files:**
- Modify: `app/src/main/java/com/dubl/character/android/state/CharacterController.kt`
- Modify: `app/src/main/java/com/dubl/character/android/ui/screens/CharactersScreen.kt`

**Interfaces:**
- Consumes: `application.transfer.exportActive()` and `application.transfer.importCharacter(raw)`.
- Produces: system-document-picker import/export of `.dubl` UTF-8 files and visible operation status.

- [x] **Step 1: Extend the source-contract test with failing Android assertions** for transfer controller methods and `CreateDocument`/`OpenDocument` launchers.
- [x] **Step 2: Run the source-contract test and confirm RED**.
- [x] **Step 3: Add thin controller methods** that mirror shared transfer state after import.
- [x] **Step 4: Add Android import/export UI** using activity-result document contracts and `ContentResolver` UTF-8 streams.
- [x] **Step 5: Run the source-contract and existing Android-facing source tests and confirm GREEN**.

### Task 3: Desktop file import/export

**Files:**
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt`
- Modify: `desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharactersScreen.kt`

**Interfaces:**
- Consumes: shared transfer capability through `DesktopAppState`.
- Produces: AWT-native `.dubl` open/save dialogs and UTF-8 file read/write with status feedback.

- [x] **Step 1: Extend the source-contract test with failing Desktop assertions** for state transfer methods and `.dubl` file-dialog actions.
- [x] **Step 2: Run the source-contract test and confirm RED**.
- [x] **Step 3: Add thin DesktopAppState transfer methods** routed through `DublApplication.transfer`.
- [x] **Step 4: Add Desktop Characters import/export actions** using the existing `FileDialog` approach.
- [x] **Step 5: Run the focused Desktop source/persistence tests and confirm GREEN**.

### Task 4: Release gate and artifacts

**Files:**
- Create: `tools/tests/test_character_transfer.py`
- Modify: `.github/workflows/linux-appimage.yml`
- Modify: `CHANGELOG.md`
- Modify: `PROJECT_FILES.txt`

**Interfaces:**
- Produces: source guardrails and CI coverage for transfer surfaces.
- Produces: one incremental patch and one complete source snapshot.

- [x] **Step 1: Add the transfer source-contract test to the Linux release-gate pytest command**.
- [x] **Step 2: Update changelog/project manifest** without claiming binary verification that was not run.
- [x] **Step 3: Run focused tests, then the available full fast/source suite**.
- [x] **Step 4: Run `:shared:desktopTest` and Compose compile checks if dependencies/toolchain are available; report any environment limitation exactly**.
- [x] **Step 5: Run `git diff --check`, generate an incremental `.patch`, and generate a complete source `.zip` excluding `.git`, build caches, and transient outputs**.
