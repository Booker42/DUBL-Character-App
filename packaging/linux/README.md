# Linux packaging

## End-user artifact

Linux x86_64 users should receive a single executable file:

`DUBL-Character-<version>-linux-x86_64.AppImage`

They do not need Gradle, a JDK, Android Studio, or the Android SDK.

## Canonical release build

`packaging/linux/build-appimage.sh` first builds the real Compose Desktop distributable (including its JVM runtime) and then converts it to an AppImage. `.github/workflows/linux-appimage.yml` runs this in CI for tags and manual dispatches.

## Legacy portable fallback

`packaging/linux/build-portable-appimage.sh` and `portable-src` are retained temporarily as a regression oracle/fallback for restricted environments with no Gradle/Maven access. They are frozen for feature development and are **not** the canonical release path. New Linux releases must come from `desktopApp` through `build-appimage.sh`.
