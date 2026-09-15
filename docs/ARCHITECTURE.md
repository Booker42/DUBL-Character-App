# Architecture

## Source of truth

Android 0.6.2 remains the behavioral reference. The executable source of truth is the shared Kotlin rules/application layer plus the canonical shared catalog payloads; platform UIs must not reimplement formulas or catalog semantics.

## Shared core

`shared` contains the platform-independent character model, rules, roll engine, development/magic/equipment logic, the single canonical catalog payloads and parsers, application sessions, snapshot/extras codecs, persistence interfaces, responsive policy, theme tokens, and reusable Compose primitives. `commonMain` must not depend on Android or desktop APIs.

`CharacterSession` is the primary mutation/application boundary. Android and desktop call the same session/rule APIs rather than maintaining separate formulas.

## Persistence

`CharacterStore` is the character persistence boundary.

- Android uses its Android repository/SharedPreferences adapter with shared schema 9.
- Desktop uses `DesktopCharacterStore` and `DesktopCharacterExtrasStore` under the user's local data directory with the same shared schema 9.
- Each character persists `RulesetRef`; existing schema-7 saves migrate to canonical DUBL `dubl` / `3.69`.
- Compose Desktop is wired to those real stores through `DesktopAppState`; it does not use `InMemoryCharacterStore` for the shipped workflow.

## Frontends

### Android

Jetpack Compose mobile application. It remains the behavioral reference while consuming shared model/rules. Android catalog repositories are platform adapters only: assets are sourced from `shared/src/commonMain/resources` and parsed by common parsers.

### Compose Desktop

`desktopApp` is the primary desktop frontend. It exposes the six parity workflows through desktop-native responsive Compose layouts and uses the same shared sessions/catalogs/persistence semantics.

### Legacy portable frontend

`packaging/linux/portable-src` is retained temporarily as a restricted-environment parity oracle/fallback. It is not the canonical release frontend and receives no new feature development. Remove it after a Compose AppImage is independently built and smoke-tested.

## Release

Linux releases are built from `desktopApp` with `packaging/linux/build-appimage.sh`. CI must run shared desktop tests and compile the Compose desktop application before packaging.
