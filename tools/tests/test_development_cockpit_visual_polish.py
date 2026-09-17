from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DESKTOP = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt').read_text(encoding='utf-8')
ANDROID = (ROOT / 'app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt').read_text(encoding='utf-8')


def test_desktop_budget_and_selection_follow_polished_mockup_hierarchy():
    assert 'DevelopmentBudgetPanel(' in DESKTOP
    assert 'Осталось XP' in DESKTOP
    assert 'ОС' in DESKTOP and 'свободно из' in DESKTOP
    assert 'selected: Boolean' in DESKTOP
    assert 'selected = selectedEntryId == entry.id' in DESKTOP
    assert 'Не хватает $failedCount требований' in DESKTOP
    assert 'Открывает ${unlocksCount} навыков' in DESKTOP


def test_desktop_inspector_promotes_primary_action_requirements_and_unlocks():
    assert 'DevelopmentRequirementsCard(' in DESKTOP
    assert 'DevelopmentUnlocksCard(' in DESKTOP
    assert 'По правилу:' in DESKTOP
    assert 'Взять ранг' in DESKTOP
    assert 'Добрать и взять' in DESKTOP
    assert 'var advancedExpanded' in DESKTOP
    assert 'Дополнительно' in DESKTOP
    assert 'widthIn(min = 360.dp, max = 430.dp)' in DESKTOP


def test_android_keeps_same_builder_information_and_advanced_controls_collapsed():
    assert 'Осталось XP' in ANDROID
    assert 'DevelopmentRequirementsCard(' in ANDROID
    assert 'DevelopmentUnlocksCard(' in ANDROID
    assert 'По правилу:' in ANDROID
    assert 'Взять ранг' in ANDROID
    assert 'Добрать и взять' in ANDROID
    assert 'var advancedExpanded' in ANDROID
    assert 'Дополнительно' in ANDROID


def test_requirements_and_unlocks_are_navigation_blocks_on_both_platforms():
    for src in (DESKTOP, ANDROID):
        assert 'Требования' in src
        assert 'Открывает' in src
        assert 'onOpenEntry' in src
        assert 'Все требования выполнены' in src
