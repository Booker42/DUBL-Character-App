from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared/src/commonMain/kotlin'
HARNESS = ROOT / 'tools/tests/kotlin/MagicParityHarness.kt'
ANDROID_CONTROLLER = ROOT / 'app/src/main/java/com/dubl/character/android/state/CharacterController.kt'
DESKTOP_STATE = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt'


def test_android_and_desktop_magic_route_through_shared_application_capability():
    android = ANDROID_CONTROLLER.read_text(encoding='utf-8')
    desktop = DESKTOP_STATE.read_text(encoding='utf-8')
    mapping = {
        'setMagicManaRank': 'application.magic.setManaRank',
        'setMagicSchoolPower': 'application.magic.setSchoolPower',
        'addMagicSchool': 'application.magic.addSchool',
        'updateMagicSchool': 'application.magic.updateSchool',
        'removeMagicSchool': 'application.magic.removeSchool',
        'addCatalogSpell': 'application.magic.addCatalogSpell',
        'addCustomSpell': 'application.magic.addCustomSpell',
        'updateSpell': 'application.magic.updateSpell',
        'removeSpell': 'application.magic.removeSpell',
        'changeMana': 'application.magic.changeMana',
    }
    for method, call in mapping.items():
        assert f'fun {method}' in android, method
        assert call in android, method
        assert f'fun {method}' in desktop, method
        assert call in desktop, method
    assert 'CharacterSession' not in android + desktop
    assert 'fun mutate(' not in desktop


def test_shared_magic_behavior_harness():
    kotlinc = shutil.which('kotlinc')
    assert kotlinc is not None
    sources = sorted((SHARED / 'com/dubl/character/android/model').glob('*.kt'))
    sources += [
        SHARED / 'com/dubl/character/android/data/CharacterStore.kt',
        SHARED / 'com/dubl/character/android/state/CharacterSession.kt',
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / 'magic-parity.jar'
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), '-include-runtime', '-d', str(jar)],
            cwd=ROOT, capture_output=True, text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(['java', '-jar', str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr + result.stdout
        assert 'MAGIC_PARITY_OK' in result.stdout
