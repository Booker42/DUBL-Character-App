# Architecture

## Source of truth

Android 0.6.2 defines DUBL behavior and catalog semantics. Rules/formulas live in shared Kotlin and must not be reimplemented in a platform UI.

## Shared core

`shared` contains the platform-independent character model, rules, roll engine, development/magic/equipment logic, catalog payloads/parsers, application sessions, snapshot/extras codecs, persistence interfaces, responsive policy, theme tokens, and reusable Compose primitives. `commonMain` must not depend on Android or desktop APIs.

`CharacterSession` is the primary mutation/application boundary. Android and desktop call the same session/rule APIs rather than maintaining separate formulas.

## Persistence

`CharacterStore` is the character persistence boundary.

- Android uses its Android repository/SharedPreferences adapter while preserving schema 7.
- Desktop uses `DesktopCharacterStore` and `DesktopCharacterExtrasStore` under the user's local data directory.
- Compose Desktop is wired to those real stores through `DesktopAppState`; it does not use `InMemoryCharacterStore` for the shipped workflow.

## Frontends

### Android

Jetpack Compose mobile application. It remains the behavioral reference while consuming shared model/rules.

### Compose Desktop

`desktopApp` is the primary desktop frontend. It exposes the six parity workflows through desktop-native responsive Compose layouts and uses the same shared sessions/catalogs/persistence semantics.

### Legacy portable frontend

`packaging/linux/portable-src` is retained temporarily as a restricted-environment parity oracle/fallback. It is not the canonical release frontend and receives no new feature development. Remove it after a Compose AppImage is independently built and smoke-tested.

## Release

Linux releases are built from `desktopApp` with `packaging/linux/build-appimage.sh`. CI must run shared desktop tests and compile the Compose desktop application before packaging.
