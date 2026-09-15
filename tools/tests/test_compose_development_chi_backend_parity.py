from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared/src/commonMain/kotlin'
HARNESS = ROOT / 'tools/tests/kotlin/DevelopmentChiParityHarness.kt'
ANDROID_CONTROLLER = ROOT / 'app/src/main/java/com/dubl/character/android/state/CharacterController.kt'
DESKTOP_STATE = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt'


def test_android_and_desktop_mutate_the_same_shared_session():
    android = ANDROID_CONTROLLER.read_text(encoding='utf-8')
    desktop = DESKTOP_STATE.read_text(encoding='utf-8')
    assert 'private val session = CharacterSession' in android
    assert 'private val session = CharacterSession' in desktop
    for method in ('setDevelopmentRank', 'setChiEnabled', 'setChiBonusRanks', 'restoreChi', 'changeChi'):
        assert f'session.{method}' in android
    assert 'fun mutate(action: CharacterSession.() -> Unit)' in desktop


def test_shared_development_martial_and_chi_behavior_harness():
    kotlinc = shutil.which('kotlinc')
    assert kotlinc is not None
    sources = sorted((SHARED / 'com/dubl/character/android/model').glob('*.kt'))
    sources += [
        SHARED / 'com/dubl/character/android/data/CharacterStore.kt',
        SHARED / 'com/dubl/character/android/state/CharacterSession.kt',
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / 'development-chi.jar'
        compiled = subprocess.run(
            [kotlinc, *map(str, sources), '-include-runtime', '-d', str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(['java', '-jar', str(jar)], cwd=ROOT, capture_output=True, text=True)
        assert result.returncode == 0, result.stderr
        assert 'DEVELOPMENT_CHI_PARITY_OK' in result.stdout
