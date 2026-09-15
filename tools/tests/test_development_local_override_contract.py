from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/DevelopmentLocalOverrideHarness.kt"


def test_development_canonical_entries_can_be_locally_overridden_and_custom_entries_added():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += [HARNESS]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "development-local-override.jar"
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "DEVELOPMENT_LOCAL_OVERRIDE_OK" in result.stdout
