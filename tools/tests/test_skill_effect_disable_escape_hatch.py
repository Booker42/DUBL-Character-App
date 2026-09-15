from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/kotlin"
HARNESS = ROOT / "tools/tests/kotlin/SkillEffectDisableHarness.kt"


def test_skill_effect_automation_can_be_disabled_per_character_and_persists():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    sources = sorted((SHARED / "com/dubl/character/android/model").glob("*.kt"))
    sources += sorted((SHARED / "com/dubl/character/android/data").glob("*.kt"))
    sources += [SHARED / "com/dubl/character/android/state/CharacterSession.kt", HARNESS]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "skill-effect-disable.jar"
        compiled = subprocess.run([kotlinc, *map(str, sources), "-include-runtime", "-d", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(["java", "-jar", str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert "SKILL_EFFECT_DISABLE_OK" in result.stdout


def test_android_and_desktop_expose_skill_effect_automation_escape_hatch():
    android_controller = (ROOT / "app/src/main/java/com/dubl/character/android/state/CharacterController.kt").read_text(encoding="utf-8")
    android_skills = (ROOT / "app/src/main/java/com/dubl/character/android/ui/screens/SkillsScreen.kt").read_text(encoding="utf-8")
    desktop_skills = (ROOT / "desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/SkillsScreen.kt").read_text(encoding="utf-8")
    assert "setSkillEffectEnabled" in android_controller
    assert "Автоматизация правил" in android_skills
    assert "setSkillEffectEnabled(effect.id, enabled)" in android_skills
    assert "Автоматизация правил" in desktop_skills
    assert "setSkillEffectEnabled(effect.id, enabled)" in desktop_skills
