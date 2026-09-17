# GitHub build/release workflow

## Development builds

Normal pushes do not create GitHub Releases.

- `android-ci.yml` builds `FURY-Android-dev.apk` on `main`/PRs.
- `linux-appimage.yml` builds and smoke-tests the Compose Linux AppImage on `main` or manual dispatch.
- `windows-desktop.yml` builds the Compose Windows EXE/MSI on `main` or manual dispatch.

The development Android application ID remains `com.dubl.character.android.dev`. The repository debug keystore is intentionally public and is never used for production releases.

## Unified production release

`.github/workflows/release.yml` is the only workflow that publishes a GitHub Release. A tag such as `v0.5` produces one `FURY 0.5` release containing:

- `FURY-0.5-Android.apk`;
- `FURY-0.5-Linux-x86_64.AppImage`;
- `FURY-0.5-Linux-x86_64.AppImage.sha256`;
- `FURY-0.5-Windows-x64.exe`;
- `FURY-0.5-Windows-x64.msi`.

The public product version may use two components (`0.5`). Compose native packaging receives the normalized three-component version (`0.5.0`) internally, while published artifact names keep `0.5`.

Android release `versionCode` uses a dedicated FURY range. `v0.5` maps to `105000000`, which is intentionally higher than the legacy Android `6002000` code so existing installations can update normally even though the public product version has been unified at 0.5.

Release signing requires these GitHub Actions secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Run `./tools/setup-signing` once to create the private key locally and upload those secrets with GitHub CLI. Never delete the only backup of the release keystore: future Android updates must use the same signing identity.

## Releasing 0.5

After the desired commit is on `main`:

```bash
git tag -a v0.5 -m "FURY 0.5"
git push origin v0.5
```

The release is published only after Android, Linux, and Windows jobs all succeed.
