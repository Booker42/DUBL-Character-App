from pathlib import Path
DEV = (Path(__file__).resolve().parents[2] / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt').read_text(encoding='utf-8')


def test_force_requirement_purchase_requires_confirmation():
    assert 'pendingRequirementOverride' in DEV
    assert 'Требования не выполнены' in DEV
    assert 'Добавить всё равно' in DEV or 'Открыть всё равно' in DEV
    assert 'availability.canForceIncrease' in DEV


def test_ability_purchase_has_explicit_ap_confirmation():
    assert 'pendingAbilityPurchase' in DEV
    assert 'Открыть спец. ветку?' in DEV
    assert 'Будет потрачено' in DEV
    assert 'ОС' in DEV


def test_selected_ability_option_recomputes_availability():
    assert 'rules.availability(entry, optionIndex)' in DEV
