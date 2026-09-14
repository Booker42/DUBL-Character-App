from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHEET = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt'
ROLL = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/RollDialog.kt'


def test_group_manager_has_real_long_press_drag_drop():
    text = SHEET.read_text(encoding='utf-8')
    assert 'detectDragGesturesAfterLongPress' in text
    assert '.pointerInput(' in text
    assert 'boundsInWindow()' in text
    assert 'SheetGroupingRules.moveItems' in text
    assert 'SheetGroupingRules.moveGroupToIndex' in text
    assert 'subtreeBlock' in text


def test_attribute_quick_roll_preserves_selected_attribute():
    roll = ROLL.read_text(encoding='utf-8')
    sheet = SHEET.read_text(encoding='utf-8')
    assert 'initialAttribute: AttributeId? = null' in roll
    assert 'initialAttribute?.takeIf' in roll
    assert 'ContextRollRequest' in sheet
    assert 'initialAttribute = request.attribute' in sheet


def test_sheet_skills_roll_directly_without_forcing_navigation():
    text = SHEET.read_text(encoding='utf-8')
    assert 'sheetRollSkill' in text
    assert 'SkillRollDialog(' in text
    assert 'preferredSkillAttributes' in text

def test_context_rolls_show_owned_effect_reminders():
    roll = ROLL.read_text(encoding='utf-8')
    sheet = SHEET.read_text(encoding='utf-8')
    assert 'developmentCatalog: DevelopmentCatalog' in roll
    assert 'effectCatalog: SkillEffectCatalog' in roll
    assert '.forContext(context)' in roll
    assert 'developmentCatalog = state.developmentCatalog' in sheet
    assert 'effectCatalog = state.skillEffectCatalog' in sheet
