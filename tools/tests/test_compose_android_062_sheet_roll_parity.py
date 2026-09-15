from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHEET = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt').read_text(encoding='utf-8')
ROLL = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/RollDialog.kt').read_text(encoding='utf-8')


def test_character_sheet_roll_uses_explicit_attribute_choice_before_roll():
    assert 'SkillAttributeChoiceDialog' in SHEET
    assert 'sheetRollAttributeChoice' in SHEET
    assert 'sheetRollRequest' in SHEET


def test_sheet_attribute_choice_defaults_to_stock_and_allows_any_attribute():
    assert 'fun SkillAttributeChoiceDialog(' in ROLL
    assert 'mutableStateOf(skill.stockAttribute)' in ROLL
    assert 'AttributeId.entries' in ROLL
    assert 'skillCalculationForRoll(skill, selectedAttribute)' in ROLL


def test_skill_roll_accepts_explicit_initial_attribute_for_sheet_flow():
    assert 'initialAttribute: AttributeId? = null' in ROLL
    assert 'initialAttribute ?: preferredAttribute' in ROLL


def test_character_sheet_skill_summary_uses_stock_attribute_like_android_062():
    assert 'val selected = skill.stockAttribute' in SHEET
    assert 'skillCalculationForRoll(skill, selected)' in SHEET
