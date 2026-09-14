# GitHub build/release workflow

## Development APK

`android-ci.yml` runs on every push to `main` and on pull requests. It builds the `.dev` application and uploads `DUBL-Android-dev.apk` as a 14-day Actions artifact.

The development application ID is `com.dubl.character.android.dev`. Its signing key in `ci/dubl-debug.keystore` is intentionally public and is never used for production releases.

## Production APK

`android-release.yml` accepts both `vX.Y` and `vX.Y.Z` tags.

The tag determines `versionName`. `versionCode` is deterministically encoded as:

```text
major * 1,000,000 + minor * 1,000 + patch
```

Examples:

- `0.2` -> `2000`
- `0.2.1` -> `2001`
- `1.0.0` -> `1000000`

Release signing requires these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Run `./tools/setup-signing` once to create the private key locally and upload those secrets with GitHub CLI.

Never delete your only backup of the release keystore. Android updates require future releases to be signed by the same key.
