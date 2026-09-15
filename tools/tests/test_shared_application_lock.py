from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android'
APPLICATION = SHARED / 'application'
STATE = SHARED / 'state'
ANDROID_CONTROLLER = ROOT / 'app/src/main/java/com/dubl/character/android/state/CharacterController.kt'
ANDROID_OVERVIEW = ROOT / 'app/src/main/java/com/dubl/character/android/ui/screens/OverviewScreen.kt'
ANDROID_SKILLS = ROOT / 'app/src/main/java/com/dubl/character/android/ui/screens/SkillsScreen.kt'
DESKTOP_STATE = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt'
DESKTOP_SCREENS = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens'

CAPABILITY_FILES = {
    'DublApplication.kt': 'class DublApplication',
    'CharacterApplication.kt': 'class CharacterApplication',
    'SkillsApplication.kt': 'class SkillsApplication',
    'DevelopmentApplication.kt': 'class DevelopmentApplication',
    'MagicApplication.kt': 'class MagicApplication',
    'EquipmentApplication.kt': 'class EquipmentApplication',
    'SheetApplication.kt': 'class SheetApplication',
}


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8')


def test_shared_application_aggregate_and_capabilities_exist():
    for filename, declaration in CAPABILITY_FILES.items():
        path = APPLICATION / filename
        assert path.exists(), filename
        assert declaration in read(path), filename
    root = read(APPLICATION / 'DublApplication.kt')
    for capability in ('character', 'skills', 'development', 'magic', 'equipment', 'sheet'):
        assert f'val {capability}' in root
    assert 'val snapshot' in root
    assert 'val active' in root
    assert 'val activeExtras' in root


def test_raw_sessions_are_shared_internal_details():
    character = read(STATE / 'CharacterSession.kt')
    extras = read(STATE / 'CharacterExtrasSession.kt')
    assert 'internal class CharacterSession' in character
    assert 'internal class CharacterExtrasSession' in extras
    assert 'internal fun updateActive' in character
    assert 'internal fun update(' in extras


def test_android_adapter_has_no_raw_session_or_transform_escape_hatch():
    controller = read(ANDROID_CONTROLLER)
    assert 'DublApplication' in controller
    assert 'CharacterSession' not in controller
    assert 'fun updateActive(' not in controller
    for screen in (ANDROID_OVERVIEW, ANDROID_SKILLS):
        text = read(screen)
        assert 'CharacterSheetExtrasRepository' not in text
        assert 'extrasRepository.save' not in text
        assert 'controller.updateActive' not in text


def test_desktop_adapter_has_no_raw_session_or_generic_mutation_escape_hatch():
    state = read(DESKTOP_STATE)
    assert 'DublApplication' in state
    assert 'CharacterSession' not in state
    assert 'CharacterExtrasSession' not in state
    assert 'fun mutate(' not in state
    assert 'fun updateExtras(' not in state
    for path in DESKTOP_SCREENS.glob('*.kt'):
        text = read(path)
        assert 'state.mutate {' not in text, path.name
        assert 'state.updateExtras {' not in text, path.name
        assert 'updateActive {' not in text, path.name


def test_public_application_and_platform_adapters_expose_no_arbitrary_domain_transforms():
    for path in APPLICATION.glob('*.kt'):
        text = read(path)
        assert 'transform: (' not in text, path.name
    for path in (ANDROID_CONTROLLER, DESKTOP_STATE):
        text = read(path)
        assert 'transform: (' not in text, path.name


def test_undo_is_owned_by_shared_application_not_reimplemented_as_platform_reverse_mutations():
    root = read(APPLICATION / 'DublApplication.kt')
    assert 'fun undoLast()' in root
    assert 'val canUndo' in root

    controller = read(ANDROID_CONTROLLER)
    desktop = read(DESKTOP_STATE)
    assert 'fun undoLast()' in controller and 'application.undoLast()' in controller
    assert 'fun undoLast()' in desktop and 'application.undoLast()' in desktop

    overview = read(ANDROID_OVERVIEW)
    sheet = read(DESKTOP_SCREENS / 'CharacterSheetScreen.kt')
    assert 'controller.undoLast()' in overview
    assert 'state.undoLast()' in sheet
