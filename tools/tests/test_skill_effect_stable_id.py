from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/SkillEffectStableIdHarness.kt"


def test_skill_effect_uses_bound_development_id_not_duplicate_name():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt")) + [HARNESS]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "skill-effect-stable-id.jar"
        compiled = subprocess.run([kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "SKILL_EFFECT_STABLE_ID_OK" in result.stdout
