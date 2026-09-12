# DUBL Character App

Desktop character manager for the **DUBL / «Дубль All Stars 3.69 REWORK»** tabletop system.

This repository contains the application source code. Public releases provide ready-to-run builds, so users do **not** need Python, PySide6, or a development environment just to use the app.

> **Current public baseline:** 0.14.0. This is the temporary public baseline while the next polish pass is in development.

## Download

Open **[Releases](../../releases/latest)** and download the package for your system.

### Windows

Download `DUBL-<version>-Windows-x64.zip`, extract it, and run `DUBL.exe`.

### Linux

Download `DUBL-<version>-Linux-x86_64.tar.gz`, extract it, make sure `DUBL` is executable, and run it:

```bash
chmod +x DUBL
./DUBL
```

The release packages are built automatically from the tagged source by GitHub Actions.

## Main features

- Character library with local autosave.
- Attributes, derived statistics, resources, skills and abilities.
- Skill prerequisites and character validation.
- Magic, schools, spells, equipment and cybernetics.
- Custom resources and custom blocks.
- Import/export using `.dubl` and JSON character files.
- Free workspace/card layout with per-character positions and sizes.
- Dark/light UI support.

## Data and privacy

DUBL is a local desktop application. Character files are stored on the user's computer.

Default character-library locations:

- **Windows:** `%LOCALAPPDATA%\DUBL Character Sheet\characters\`
- **Linux:** `$XDG_DATA_HOME/dubl-character/characters/` or `~/.local/share/dubl-character/characters/`

## Run from source

Requires Python 3.10+ and PySide6 6.x. The release/CI baseline is Python 3.12.

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python launcher.py
```

Windows PowerShell activation:

```powershell
.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python launcher.py
```

Linux users can also use the existing convenience launcher:

```bash
bash run.sh
```

## Tests

```bash
QT_QPA_PLATFORM=offscreen python -m unittest discover -s tests -v
```

The `main` branch is tested automatically on GitHub. Release tags are tested again before Windows/Linux packages are produced.

## Repository layout

```text
dubl/                   Application code and UI
data/                   Rules/catalog data
tests/                  Mechanics and UI tests
docs/                   Development/build documentation
.github/workflows/       CI and release automation
launcher.py              Packaging-friendly application entry point
dubl.spec                PyInstaller build definition
```

## Building releases

See [`docs/BUILDING.md`](docs/BUILDING.md).

For a release such as 0.14.0:

```bash
git tag v0.14.0
git push origin v0.14.0
```

GitHub then builds the Windows and Linux packages and publishes them to the release page.

## Project documentation

- [`CHANGELOG_0.14.md`](CHANGELOG_0.14.md) — 0.14 changes.
- [`WORKSPACE_014.md`](WORKSPACE_014.md) — free-workspace behavior.
- [`RULES_AUDIT.md`](RULES_AUDIT.md) — rules/formula audit notes.
- [`CHARACTER_LIBRARY.md`](CHARACTER_LIBRARY.md) — character library design.
- [`docs/README_0.14_SOURCE_PACKAGE.md`](docs/README_0.14_SOURCE_PACKAGE.md) — original Linux/source-package README.

## License

A public repository makes the source visible, but reuse rights are a separate decision. No open-source license has been selected for this temporary public baseline yet.
