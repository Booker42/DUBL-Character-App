# Building DUBL

The public release workflow builds DUBL separately on Windows and Linux. PyInstaller is not a cross-compiler, so each binary is produced on its target operating system.

## Local source run

```bash
python -m venv .venv
source .venv/bin/activate
python -m pip install -r requirements.txt
python launcher.py
```

On Windows, activate with `.venv\Scripts\Activate.ps1`.

## Local portable build

```bash
python -m pip install -r requirements-build.txt
pyinstaller --clean --noconfirm dubl.spec
```

The result is created in `dist/DUBL/`.

## Public release

Push a semantic version tag, for example:

```bash
git tag v0.14.0
git push origin v0.14.0
```

GitHub Actions will test the project, build Windows and Linux portable packages, and attach them to a GitHub Release.
