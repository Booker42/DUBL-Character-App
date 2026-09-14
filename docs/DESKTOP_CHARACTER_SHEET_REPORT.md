# Desktop Character Sheet report

The Character Sheet is now a real Compose Desktop workflow backed by `DesktopAppState`, persistent desktop stores, `CharacterSession`, `CharacterExtrasSession`, and shared DUBL rules.

Implemented behavior includes identity and creation economy, attributes, HP/Endurance/Mana/Chi, maximum overrides, custom resources, portrait import/rendering, conditions and automatic Weakness, formula/detail dialogs, quick combat checks, learned skill/development summaries, recent-change Undo, visibility controls, and persistent grouping/reorder including tree-root subtree movement.

The desktop sheet intentionally uses a desktop-native responsive layout and `LazyColumn` rather than copying Android bottom-sheet composition. There is no app-level horizontal scrolling.

The old portable/Swing sheet remains only as a regression oracle/fallback. New sheet work belongs in `desktopApp`.

Verification includes source-level Compose parity tests, the shared Character Sheet JVM harness, persistence tests, and a Kotlin compile gate. Final Compose binary verification awaits a Gradle/Maven-enabled environment.
