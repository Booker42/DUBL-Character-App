# DUBL KMP Foundation Implementation Plan

1. Record Android 0.6.2 baseline tests and environmental constraints.
2. Add an architecture test that rejects Android/JVM APIs in `shared/commonMain`.
3. Add `shared` with Android and desktop JVM targets; move existing model/rules and unit tests without changing behavior.
4. Replace the two JVM-only model helpers with common Kotlin equivalents and make the architecture test green.
5. Add shared persistence contracts, shared design tokens, and reusable cards/stat/resource components.
6. Make the existing Android app consume `shared`, preserving its repositories, controller, screens, save schema, assets, and release variants.
7. Add a responsive Compose Desktop shell backed by the shared character model and rules.
8. Run common tests, desktop build/runtime checks, Android tests/build where the environment permits, source-boundary checks, overflow/static layout checks, and package a source snapshot with a concrete report.
