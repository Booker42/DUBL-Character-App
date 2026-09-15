from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/DevelopmentOverrideSessionHarness.kt"


def test_character_session_owns_development_override_and_custom_mutations():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += [
        SHARED / "com/dubl/character/android/data/CharacterStore.kt",
        SHARED / "com/dubl/character/android/state/CharacterSession.kt",
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "development-override-session.jar"
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "DEVELOPMENT_OVERRIDE_SESSION_OK" in result.stdout
