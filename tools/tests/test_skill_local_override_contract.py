from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/SkillLocalOverrideHarness.kt"


def test_builtin_skill_supports_local_override_and_reset_without_losing_progression():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += [
        SHARED / "com/dubl/character/android/data/CharacterStore.kt",
        SHARED / "com/dubl/character/android/state/CharacterSession.kt",
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "skill-local-override.jar"
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "SKILL_LOCAL_OVERRIDE_OK" in result.stdout
