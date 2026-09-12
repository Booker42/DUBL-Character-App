# Publishing this repository to GitHub

This folder is ready to become the public `DUBL-Character-App` repository.

## 1. Create the repository on GitHub

Create a new **public** repository named `DUBL-Character-App`.

Do not ask GitHub to add a README, `.gitignore`, or license during creation; this project already contains the files it needs.

## 2. Push the source

From this folder:

```bash
git init
git add .
git commit -m "Public baseline: DUBL 0.14"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/DUBL-Character-App.git
git push -u origin main
```

The `CI` workflow will run automatically after the push.

## 3. Publish the first downloadable release

After CI is green:

```bash
git tag v0.14.0
git push origin v0.14.0
```

The `Build release` workflow will:

1. Test DUBL.
2. Build the app on Windows.
3. Build the app on Linux (Ubuntu 22.04 baseline).
4. Create `DUBL-0.14.0-Windows-x64.zip`.
5. Create `DUBL-0.14.0-Linux-x86_64.tar.gz`.
6. Create a GitHub Release and attach both downloads.

## 4. Future versions

For a patch release:

```bash
git tag v0.14.1
git push origin v0.14.1
```

For the next larger release:

```bash
git tag v0.15.0
git push origin v0.15.0
```

Never commit generated `dist/`, `build/`, executables, ZIPs, or local character files. GitHub Releases owns the downloadable binaries; the Git repository owns the source.

## Signing note

The temporary Windows build is unsigned, so Windows SmartScreen may warn users about an unknown publisher. Code signing can be added later without changing the repository layout.
