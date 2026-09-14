# DUBL Kotlin Multiplatform Foundation Design

Android 0.6.2 remains the canonical product and rules implementation. The migration introduces one deliberately broad `shared` KMP module and one `desktopApp`; it does not recreate the old desktop client or change Android behavior.

## Module boundaries

- `shared/commonMain`: platform-independent character model, rules, grouping, conditions, magic, equipment, CI, persistence contracts, design tokens, and small reusable Compose primitives.
- `app`: the existing Android application, Android repositories, asset loading, `SharedPreferences`, controller, navigation, and mobile screens.
- `desktopApp`: desktop window, navigation, responsive character shell, keyboard/mouse affordances, and local desktop composition.

The shared package may retain its historical package name during this foundation step to avoid a risky whole-application namespace rewrite. Platform independence is enforced by source-set boundaries and an architecture test, not inferred from the package string.

## Persistence boundary

`CharacterStore` describes character list/load/save/delete operations. Android's existing repository implements it without changing the JSON schema or save behavior. Desktop begins with an in-memory implementation so the shell operates on the same model without inventing a second serializer.

## Desktop layout

- Compact: below 900 dp, single vertical content column with compact navigation.
- Normal: 900–1319 dp, fixed navigation rail plus two-column character content.
- Wide: 1320 dp and above, fixed navigation rail plus bounded three-column content.

The root surface only scrolls vertically. Content widths are bounded; cards wrap text and never require an application-level horizontal scrollbar.

## Deliberate exclusions

No server, accounts, sync, Web target, complete desktop feature port, Android redesign, or old PySide/Electron architecture migration.
