from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
STATE = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt'
UI_FILES = [
    ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/Main.kt',
    *sorted((ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens').glob('*.kt')),
]


def test_desktop_state_exposes_snapshot_backed_active_character():
    text = STATE.read_text(encoding='utf-8')
    assert 'val activeCharacter: DublCharacter get() = snapshot.activeCharacter' in text


def test_compose_ui_never_reads_unobservable_session_active_directly():
    offenders = []
    for path in UI_FILES:
        text = path.read_text(encoding='utf-8')
        if 'state.session.active' in text:
            offenders.append(path.relative_to(ROOT).as_posix())
    assert not offenders, 'unobservable active-character reads: ' + ', '.join(offenders)


def test_desktop_session_is_private_behind_observable_state_adapter():
    text = STATE.read_text(encoding='utf-8')
    assert 'private val application = DublApplication' in text
    assert 'CharacterSession' not in text
