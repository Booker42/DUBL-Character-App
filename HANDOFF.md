# DUBL KMP / Desktop 0.2 handoff

## Current target

Android **0.6.2** remains the source of truth. Desktop **0.2.0** is now implemented as a real **Compose Desktop** frontend over the shared KMP application/domain layer. Do not resume feature work in the legacy portable/Swing shell; keep it only as a parity oracle/fallback until a Compose AppImage has been built and smoke-tested in an environment with Gradle/Maven network access.

Web/Wasm, server/accounts, and sync remain out of scope. The old PySide/Electron desktop project is not a source of rules, architecture, or layout.

## Compose Desktop status

Implemented and wired to shared state/persistence:

- `DesktopAppState` owns `DesktopCharacterStore`, `DesktopCharacterExtrasStore`, `CharacterSession`, `CharacterExtrasSession`, and the four canonical catalog loaders; the catalog payloads physically live once in shared resources and Android reads those same files;
- six top-level workflows: Character Sheet, Skills, Development/Martial Arts/Chi, Magic, Equipment, Characters;
- persistent roster/active character and schema-8 character data with `dubl` / `3.69` ruleset identity plus schema-7 migration;
- persistent character-sheet extras/grouping;
- rule-aware rolls and skill effects;
- custom skills/resources/spells/schools/gear where Android supports them;
- destructive-action confirmation and custom-skill validation;
- native Compose portrait rendering with a desktop file chooser;
- no app-level horizontal scrolling; compact/normal/wide responsive policy remains shared.

## Release architecture

Canonical Linux packaging is `packaging/linux/build-appimage.sh` -> `:desktopApp:createDistributable` -> AppImage. `.github/workflows/linux-appimage.yml` now builds Compose, not portable/Swing, and requires:

```text
:shared:desktopTest
:desktopApp:compileKotlin
```

before packaging. `packaging/linux/build-portable-appimage.sh` and `packaging/linux/portable-src` are legacy fallback/oracle code only.

## Verification already completed in this sandbox

- Exact offline Linux parity-release test list: **102/102 passed** when run in bounded groups (87 source/parity checks plus Development/Chi 2/2, Magic 2/2, Equipment 4/4, rules-boundary 6/6, and desktop persistence 1/1);
- additional Kotlin harnesses independently confirmed CharacterSession 1/1, desktop catalog parsing 1/1, and Character Sheet workflow 3/3;
- a cumulative patch was applied to a clean copy of the last compile-hotfixed baseline with `git apply --check`, `git apply`, and `git diff --check`, then the 87 fast guards and all newly introduced backend/rules harnesses were rerun successfully on that applied copy;
- static rules-boundary scans confirm Android/Desktop UI no longer owns direct `DevelopmentEffectIds` rank formulas, Chi/Magic XP arithmetic, run size/legs multiplier tables, or selected roll-effect summation;
- release workflow source tests require the Compose packager, full current parity guards, compile/test gate, and a clean-HOME Xvfb AppImage startup smoke;
- Gradle bootstrap and wrapper are synchronized to 9.7.0; Compose 1.12.0 dependencies use its aligned Material3 1.12.0-alpha03 set; Linux CI uses the project JDK 17 toolchain.

## Environment limitation

The sandbox does not currently have a Gradle distribution or Compose/Maven dependency cache and shell DNS is blocked. Therefore an actual `:desktopApp:compileKotlin`/Compose AppImage build cannot be honestly claimed from this environment yet. CI/local Linux with normal network access is the intended final binary verification path. Do not relabel any older portable 0.2 artifact as the new Compose build.

## Next execution step

1. Run the networked Gradle/Compose compile and AppImage smoke gate for the completed parity-lock slice.
2. Treat shared rules/catalogs/application behavior plus the executable parity contracts as the baseline for all subsequent desktop work.
3. Continue future rulebook-import work by extending the ruleset boundary rather than reintroducing Android/Desktop catalog or formula copies.
4. Defer server/remote repository and generic rule scripting until a real second ruleset/server milestone exists; the schema-8 ruleset identity and repository boundary are the compatibility hooks for that future work.

See `docs/DESKTOP_0_2_PARITY.md` and `docs/superpowers/plans/2026-09-15-desktop-0.2-android-parity.md`.
