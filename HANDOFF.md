# DUBL KMP / Desktop 0.2 handoff

## Current target

Android **0.6.2** remains the source of truth. Desktop **0.2.0** is now implemented as a real **Compose Desktop** frontend over the shared KMP application/domain layer. Do not resume feature work in the legacy portable/Swing shell; keep it only as a parity oracle/fallback until a Compose AppImage has been built and smoke-tested in an environment with Gradle/Maven network access.

Web/Wasm, server/accounts, and sync remain out of scope. The old PySide/Electron desktop project is not a source of rules, architecture, or layout.

## Compose Desktop status

Implemented and wired to shared state/persistence:

- `DesktopAppState` owns `DesktopCharacterStore`, `DesktopCharacterExtrasStore`, `CharacterSession`, `CharacterExtrasSession`, and the four canonical catalog loaders;
- six top-level workflows: Character Sheet, Skills, Development/Martial Arts/Chi, Magic, Equipment, Characters;
- persistent roster/active character and schema-7-compatible character data;
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

- Fast/source suite: 91 passed, 2 skipped (only missing external rulebook `.docx` fixtures) after the final Compose/CI hardening pass;
- stale Character Sheet workflow regression updated for the new architecture and now 3/3 green;
- real Kotlin harnesses confirmed CharacterSession 1/1, desktop catalog parsing 1/1, desktop persistence 1/1, and Character Sheet workflow 3/3 independently;
- the full new Compose source tree plus real shared model/state/data compiles with the local Kotlin compiler against a typed Compose stub classpath; this caught and fixed real Kotlin/API issues during migration;
- release workflow source tests require the Compose packager, full current Compose parity guards, compile/test gate, and a clean-HOME Xvfb AppImage startup smoke;
- Gradle bootstrap and wrapper are synchronized to 9.7.0; Compose 1.12.0 dependencies use its aligned Material3 1.12.0-alpha03 set; Linux CI uses the project JDK 17 toolchain.

## Environment limitation

The sandbox does not currently have a Gradle distribution or Compose/Maven dependency cache and shell DNS is blocked. Therefore an actual `:desktopApp:compileKotlin`/Compose AppImage build cannot be honestly claimed from this environment yet. CI/local Linux with normal network access is the intended final binary verification path. Do not relabel any older portable 0.2 artifact as the new Compose build.

## Next execution step

1. Run the full source/parity gates and heavy Kotlin harnesses.
2. In a networked environment run `./gradlew :shared:desktopTest :desktopApp:compileKotlin`.
3. Build `DUBL_VERSION=0.2.0 packaging/linux/build-appimage.sh`.
4. Smoke-test the resulting Compose AppImage under a clean HOME.
5. Once that passes, delete/deprecate the legacy portable frontend rather than evolving it further.

See `docs/DESKTOP_0_2_PARITY.md` and `docs/superpowers/plans/2026-09-15-desktop-0.2-android-parity.md`.
