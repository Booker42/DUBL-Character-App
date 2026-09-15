from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared/src/commonMain/kotlin'
HARNESS = ROOT / 'tools/tests/kotlin/EquipmentParityHarness.kt'
ANDROID_CONTROLLER = ROOT / 'app/src/main/java/com/dubl/character/android/state/CharacterController.kt'
DESKTOP_STATE = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt'
DESKTOP_SCREEN = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/EquipmentScreen.kt'


def test_android_and_desktop_equipment_use_shared_session_and_rules():
    android = ANDROID_CONTROLLER.read_text(encoding='utf-8')
    desktop_state = DESKTOP_STATE.read_text(encoding='utf-8')
    desktop_screen = DESKTOP_SCREEN.read_text(encoding='utf-8')
    for method in (
        'setGearLoadAutomatic', 'setGearManualLoad', 'syncCatalogGearLoads',
        'addCatalogGear', 'addCustomGear', 'updateGearItem', 'removeGearItem',
    ):
        assert f'session.{method}' in android, method
    assert 'session.syncCatalogGearLoads(magicEquipmentCatalog.gear)' in desktop_state
    for rule in ('equipmentLoad', 'equipmentCapacity', 'burden', 'catalogGearLoad'):
        assert f'MagicEquipmentRules.{rule}' in desktop_screen, rule


def test_desktop_equipment_does_not_add_android_incompatible_input_caps():
    text = DESKTOP_SCREEN.read_text(encoding='utf-8')
    assert '.take(4)' not in text
    assert '.take(8)' not in text


def test_desktop_equipment_searches_canonical_content_fields_like_android():
    text = DESKTOP_SCREEN.read_text(encoding='utf-8')
    assert 'item.description' in text
    assert 'item.fields.values.joinToString(" ")' in text
    assert 'entry.description' in text
    assert 'entry.fields.values.joinToString(" ")' in text


def test_shared_equipment_behavior_harness():
    kotlinc = shutil.which('kotlinc')
    assert kotlinc is not None
    sources = sorted((SHARED / 'com/dubl/character/android/model').glob('*.kt'))
    sources += [
        SHARED / 'com/dubl/character/android/data/CharacterStore.kt',
        SHARED / 'com/dubl/character/android/state/CharacterSession.kt',
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / 'equipment-parity.jar'
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), '-include-runtime', '-d', str(jar)],
            cwd=ROOT, capture_output=True, text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(['java', '-jar', str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert 'EQUIPMENT_PARITY_OK' in result.stdout
