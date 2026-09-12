# DUBL Character App

Desktop character manager for **DUBL / «Дубль All Stars 3.69 REWORK»**.

> **Development build.** This repository is currently shared primarily for testing. Expect UI changes, incomplete polish, and occasional breakage between versions.

## Download the current build

Use **[Latest Release](../../releases/latest)**. You do not need Python or Git to test a release build.

**Windows:** download `DUBL-<version>-Windows-x64.zip`, extract it, run `DUBL.exe`.

**Linux:** download `DUBL-<version>-Linux-x86_64.tar.gz`, extract it, then run:

```bash
chmod +x DUBL
./DUBL
```

Windows builds are currently unsigned, so Windows SmartScreen may show an unknown-publisher warning.

## Before testing

Please note the exact DUBL version from the Release page. When reporting a problem, include that version and your operating system.

For focused testing instructions and the current checklist, see **[TESTING.md](TESTING.md)**.

## Reporting problems

Use **GitHub Issues → New issue → Bug report**.

A useful report includes:

- DUBL version.
- Operating system; on Linux, include the distribution and desktop environment if known.
- Whether this happened on a newly created character, an imported character, or an older save.
- Exact steps that reproduce the problem.
- What you expected and what actually happened.
- Screenshot/video when the problem is visual.
- A `.dubl` save that reproduces the problem, when you are comfortable sharing it.

If something is not technically broken but is confusing, awkward, hard to discover, or unpleasant to use, use the **Tester feedback / UX** issue template instead.

## Save locations

DUBL stores the local character library here by default:

- **Windows:** `%LOCALAPPDATA%\DUBL Character Sheet\characters\`
- **Linux:** `$XDG_DATA_HOME/dubl-character/characters/` or `~/.local/share/dubl-character/characters/`

Before testing risky import/migration behavior with an important character, make a copy of the relevant `.dubl`/JSON file.

## What is worth testing

The highest-value areas right now are character creation and persistence, calculated values, skill/ability requirements, magic/equipment data, import/export, and the free-workspace/card UI. The detailed scenarios are in **[TESTING.md](TESTING.md)**.

## Source and automated tests

The source is public in this repository. `main` is checked by GitHub Actions, and release tags are tested again before Windows and Linux packages are published.

To run the test suite from source:

```bash
python -m pip install -r requirements.txt -r requirements-test.txt
QT_QPA_PLATFORM=offscreen python -m pytest -q
```

Development/build notes are in [`docs/BUILDING.md`](docs/BUILDING.md). Version changes are tracked in [`CHANGELOG_0.14.md`](CHANGELOG_0.14.md).

## License

No open-source license has been selected for this development baseline yet. Public source availability does not by itself grant redistribution or modification rights.
