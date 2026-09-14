from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/Main.kt'
STATE = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt'
SCREENS = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens'


def text(path: Path) -> str:
    assert path.exists(), f'missing {path}'
    return path.read_text(encoding='utf-8')


def test_compose_host_uses_real_shared_sessions_and_file_persistence():
    host = text(MAIN)
    state = text(STATE)
    for token in [
        'CharacterSession', 'CharacterExtrasSession',
        'DesktopCharacterStore', 'DesktopCharacterExtrasStore',
        'DesktopCatalogLoader',
    ]:
        assert token in state
    assert 'CHARACTERS' in host
    assert 'DesktopAppState' in host
    assert 'InMemoryCharacterStore' not in host + state
    assert 'FeaturePlaceholder' not in host + state


def test_all_parity_screens_exist():
    required = {
        'CharacterSheetScreen.kt': ['CharacterSheetScreen', 'CharacterSheetExtras', 'RollContext', 'SheetGroupingRules'],
        'CharactersScreen.kt': ['CharactersScreen', 'createCharacter', 'selectCharacter', 'deleteActive'],
        'SkillsScreen.kt': ['SkillsScreen', 'resolvedSkills', 'changeSkillRank', 'SkillRollDialog'],
        'DevelopmentScreen.kt': ['DevelopmentScreen', 'DevelopmentRules', 'ChiRules', 'setDevelopmentRank'],
        'MagicScreen.kt': ['MagicScreen', 'setMagicManaRank', 'setMagicSchoolPower', 'addCatalogSpell'],
        'EquipmentScreen.kt': ['EquipmentScreen', 'equipmentLoad', 'setGearLoadAutomatic', 'addCatalogGear'],
        'RollDialog.kt': ['SkillRollDialog', 'RollMode', 'rollPreset'],
    }
    for name, tokens in required.items():
        src = text(SCREENS / name)
        for token in tokens:
            assert token in src, f'{name} missing {token}'


def test_compose_screens_have_no_horizontal_scroll():
    for path in SCREENS.glob('*.kt'):
        assert 'horizontalScroll' not in text(path), path.name


def test_compose_host_does_not_keep_fake_persistence_markers():
    src = text(MAIN)
    assert 'persistentStoreMarker' not in src
    assert 'persistentAdapters' not in src
