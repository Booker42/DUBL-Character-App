from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/SkillOverridePersistenceHarness.kt"


def test_skill_local_definition_overrides_round_trip_through_snapshot_codec():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += [
        SHARED / "com/dubl/character/android/data/MiniJson.kt",
        SHARED / "com/dubl/character/android/data/SnapshotCodec.kt",
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "skill-override-persistence.jar"
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "SKILL_OVERRIDE_PERSISTENCE_OK" in result.stdout
