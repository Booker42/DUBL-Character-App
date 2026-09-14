# KMP foundation verification report

Date: 2026-09-14

## Scope

Continued the approved KMP foundation from the uploaded in-progress snapshot. No Web work was added, and no old PySide/Electron desktop source was used.

## Implemented in this continuation

- canonical Android theme moved to `shared/commonMain`;
- reusable shared card/stat/resource primitives;
- shared compact/normal/wide layout policy;
- Compose Desktop application entry point and responsive shell;
- shared-model character preview using real derived formulas;
- source-level regression tests for theme migration, desktop overflow policy, and module ownership;
- Kotlin breakpoint unit test.

## Behavior preserved

- Android application ID/version/release variants were not changed;
- Android repositories/assets/controller/screens were not redesigned;
- `CharacterRepository` remains the Android storage implementation;
- save schema remains 7;
- rules/models remain shared and platform-independent;
- no desktop copy of rules or formulas was introduced.

## Automated checks available in this environment

```text
python3 -m unittest discover -s tools/tests -p 'test_*.py' -v
```

Result: pass.

A standalone `kotlinc` boundary harness also compiled and executed `DublLayoutPolicy` successfully at 0, 899, 900, 1319, 1320, and 4096dp.

Static scans confirm:

- no Android/JVM/JavaFX/platform Compose imports in `shared/commonMain`;
- no desktop `horizontalScroll` call;
- desktop content uses vertical scrolling;
- Android theme imports resolve to the shared theme declarations;
- no duplicate Android `Color.kt`, `Theme.kt`, or `Type.kt` remains.

## Checks blocked by sandbox environment

Full Gradle verification is blocked because:

1. `gradle-wrapper.jar` is absent from the uploaded snapshot;
2. Gradle is not installed globally;
3. the sandbox cannot resolve `services.gradle.org`, so `bootstrap-wrapper.sh` cannot download Gradle 9.6;
4. Android SDK 36 is not installed in this container.

Therefore this report does **not** claim that Android or Compose Desktop binaries were compiled in the current sandbox.

## Required local verification

With network access and Android SDK 36:

```bash
./bootstrap-wrapper.sh
./gradlew :shared:desktopTest :desktopApp:compileKotlin :app:testDebugUnitTest :app:assembleDebug
./gradlew :desktopApp:run
```

Runtime desktop review should exercise window widths immediately below/above 900dp and 1320dp, minimized/maximized states, and increased UI scaling. The acceptance criterion is no app-level horizontal scrollbar and readable, bounded content at every layout class.
