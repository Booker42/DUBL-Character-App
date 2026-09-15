from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
DESKTOP = ROOT / "shared/src/desktopMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/ConditionOverridePersistenceHarness.kt"


def test_desktop_condition_overrides_and_custom_conditions_round_trip():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += sorted((SHARED / "com/dubl/character/android/data").glob("*.kt"))
    sources += [
        DESKTOP / "com/dubl/character/android/data/DesktopCharacterStore.kt",
        DESKTOP / "com/dubl/character/android/data/DesktopCharacterExtrasStore.kt",
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "condition-override-persistence.jar"
        compiled = subprocess.run([kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "CONDITION_OVERRIDE_PERSISTENCE_OK" in result.stdout


def test_android_extras_repository_persists_condition_overlay():
    text = (ROOT / "app/src/main/java/com/dubl/character/android/data/CharacterSheetExtrasRepository.kt").read_text()
    assert "condition_overrides" in text
    assert "custom_conditions" in text
    assert "conditionOverrides" in text
    assert "customConditions" in text
