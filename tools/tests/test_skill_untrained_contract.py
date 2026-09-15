from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/SkillUnspecifiedHarness.kt"


def test_unspecified_untrained_rule_is_non_executable_but_learned_skill_works():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "skill-unspecified.jar"
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), str(HARNESS), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "SKILL_UNSPECIFIED_OK" in result.stdout
