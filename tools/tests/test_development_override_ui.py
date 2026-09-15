from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID_CONTROLLER = ROOT / "app/src/main/java/com/dubl/character/android/state/CharacterController.kt"
ANDROID_SCREEN = ROOT / "app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt"
DESKTOP_STATE = ROOT / "desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt"
DESKTOP_SCREEN = ROOT / "desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt"
WORKFLOW = ROOT / ".github/workflows/linux-appimage.yml"


def test_android_development_ui_exposes_local_override_and_custom_escape_hatch():
    controller = ANDROID_CONTROLLER.read_text()
    screen = ANDROID_SCREEN.read_text()
    assert "setDevelopmentOverride" in controller
    assert "resetDevelopmentOverride" in controller
    assert "addCustomDevelopment" in controller
    assert "updateCustomDevelopment" in controller
    assert "removeCustomDevelopment" in controller
    assert "effectiveDevelopmentCatalog" in screen
    assert "Локальная правка" in screen
    assert "Сбросить к рулбуку" in screen
    assert "Своя запись" in screen
    assert ".filter { entry -> !entry.incomplete" not in screen


def test_desktop_development_ui_exposes_local_override_and_custom_escape_hatch():
    state = DESKTOP_STATE.read_text()
    screen = DESKTOP_SCREEN.read_text()
    assert "effectiveDevelopmentCatalog" in state
    assert "Локальная правка" in screen
    assert "Сбросить к рулбуку" in screen
    assert "Своя запись" in screen
    assert "setDevelopmentOverride" in screen
    assert "addCustomDevelopment" in screen
    assert ".filter { entry -> !entry.incomplete" not in screen


def test_development_escape_hatch_contracts_are_in_linux_release_gate():
    workflow = WORKFLOW.read_text()
    for name in (
        "test_development_local_override_contract.py",
        "test_development_override_persistence.py",
        "test_development_override_session.py",
        "test_development_override_ui.py",
    ):
        assert name in workflow
