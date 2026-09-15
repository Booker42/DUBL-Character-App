from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID = ROOT / "app/src/main/java/com/dubl/character/android/ui/screens/OverviewScreen.kt"
DESKTOP = ROOT / "desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt"


def test_android_conditions_expose_local_override_reset_and_custom_creation():
    text = ANDROID.read_text(encoding="utf-8")
    assert "Локальная правка состояния" in text
    assert "Сбросить к рулбуку" in text
    assert "Добавить своё состояние" in text
    assert "conditionOverrides" in text
    assert "customConditions" in text


def test_desktop_conditions_expose_local_override_reset_and_custom_creation():
    text = DESKTOP.read_text(encoding="utf-8")
    assert "Локальная правка состояния" in text
    assert "К рулбуку" in text
    assert "Добавить своё состояние" in text
    assert "setConditionOverride" in text
    assert "addCustomCondition" in text
