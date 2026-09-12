# Contributing to DUBL Character App

Bug reports and pull requests are welcome.

## Development setup

Use Python 3.10+; Python 3.12 is the release/CI baseline.

```bash
python -m venv .venv
source .venv/bin/activate   # Linux/macOS
# .venv\Scripts\activate   # Windows PowerShell
python -m pip install -r requirements.txt
python launcher.py
```

## Tests

```bash
QT_QPA_PLATFORM=offscreen python -m unittest discover -s tests -v
```

On Windows PowerShell:

```powershell
$env:QT_QPA_PLATFORM='offscreen'
python -m unittest discover -s tests -v
```

Keep mechanics changes separate from UI-only changes where practical, and include or update tests for rule/formula changes.
