from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/DevelopmentUnresolvedEscapeHarness.kt"


def test_incomplete_development_entries_can_be_force_added_instead_of_hard_locked():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += [HARNESS]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "development-unresolved.jar"
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr
        assert "DEVELOPMENT_UNRESOLVED_ESCAPE_OK" in result.stdout
