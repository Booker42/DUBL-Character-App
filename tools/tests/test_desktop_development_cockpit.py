from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEV = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt').read_text(encoding='utf-8')


def test_desktop_development_is_two_column_browser_with_persistent_inspector():
    assert 'LazyVerticalGrid' in DEV
    assert 'GridCells.Fixed' in DEV
    assert 'DevelopmentInspector(' in DEV
    assert 'selectedEntryId' in DEV
    assert 'DevelopmentCompactCard(' in DEV


def test_desktop_cockpit_has_build_filters_plan_and_unlock_navigation():
    for token in ('DevelopmentBrowserFilter', 'Можно взять', 'Почти доступно', 'План'):
        assert token in DEV
    assert 'plannedDevelopmentIds' in DEV
    assert 'DevelopmentAcquisitionPlanner' in DEV
    assert '.unlocks(entry.id)' in DEV
    assert 'Открывает' in DEV


def test_desktop_cockpit_can_preview_and_atomically_acquire_requirements_or_target():
    assert 'DevelopmentAcquisitionPreview' in DEV
    assert 'Добрать требования' in DEV
    assert 'Добрать и взять' in DEV
    assert 'choiceSelections' in DEV
    assert 'state.acquireDevelopment' in DEV
    assert 'Недостаточно' in DEV


def test_desktop_ability_acquisition_preserves_selected_source_and_chi_uses_same_inspector():
    assert 'Источник способности' in DEV
    assert 'optionIndex = optionIndex' in DEV
    assert 'DevelopmentTab.CHI' in DEV
    chi_block = DEV.split('if (tab == DevelopmentTab.CHI)', 1)[1].split('} else {', 1)[0]
    assert 'DevelopmentInspector(' in chi_block
