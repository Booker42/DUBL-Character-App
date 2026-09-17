from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCREENS = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens'
PRIMITIVES = SCREENS / 'UiPrimitives.kt'
SHEET = SCREENS / 'CharacterSheetScreen.kt'
ROLL = SCREENS / 'RollDialog.kt'


def read(path: Path) -> str:
    assert path.exists(), f'missing {path}'
    return path.read_text(encoding='utf-8')


def section(src: str, start: str, end: str) -> str:
    return src.split(start, 1)[1].split(end, 1)[0]


def test_resource_controls_use_one_adjacent_stepper_and_leave_progress_bar_visual_only():
    primitives = read(PRIMITIVES)
    assert 'internal fun FuryStepper(' in primitives
    tile = primitives.split('internal fun DesktopResourceTile', 1)[1]
    assert 'FuryStepper(' in tile
    assert 'onMinus = onMinus' in tile
    assert 'onPlus = onPlus' in tile
    assert 'DesktopTinyButton("−", onMinus' not in tile
    assert 'DesktopTinyButton("+", onPlus' not in tile
    assert 'minusEnabled = current > 0' not in tile
    assert tile.index('FuryStepper(') < tile.index('Box(Modifier.fillMaxWidth().height(5.dp)')


def test_grouping_editor_uses_clear_drag_handles_overflow_actions_and_explicit_create_action():
    sheet = read(SHEET)
    grouping = section(sheet, 'private fun GroupingManagerDialog', '@Composable\nprivate fun DraggableGroupingItem')
    draggable = section(sheet, 'private fun DraggableGroupingItem', 'private fun defaultSkillGroups')
    assert 'GroupingDragHandle(' in grouping
    assert 'GroupActionMenu(' in grouping
    assert 'Text("✎")' not in grouping
    assert 'Text("×")' not in grouping
    assert 'Text("тянуть"' not in draggable
    assert 'GroupingDragHandle(' in draggable
    assert '+ Создать группу' in grouping
    assert 'Перетаскивайте группы и элементы за ручку' in grouping


def test_development_children_have_a_compact_branch_row_distinct_from_roots():
    sheet = read(SHEET)
    tree_row = section(sheet, 'private fun DevelopmentTreeRow', '@Composable\nprivate fun SheetDevelopmentPanel')
    assert 'val child = displayDepth > 0' in tree_row
    assert 'DevelopmentBranchConnector(' in tree_row
    assert 'if (child) 3.dp else 5.dp' in tree_row
    assert 'if (child) DesktopSurfaceInset.copy(alpha = .46f)' in tree_row
    assert 'if (child) 16.dp else 18.dp' in tree_row


def test_roll_flow_uses_fury_segmented_mode_control_and_selected_attribute_buttons():
    primitives = read(PRIMITIVES)
    roll = read(ROLL)
    assert 'internal fun FurySegmentedControl(' in primitives
    assert 'internal fun FuryChoiceButton(' in primitives
    skill_roll = section(roll, 'fun SkillRollDialog', '@Composable\nfun ContextRollDialog')
    choice = section(roll, 'fun SkillAttributeChoiceDialog', '@Composable\nfun SkillRollDialog')
    assert 'FurySegmentedControl(' in skill_roll
    assert 'RadioButton(' not in skill_roll
    assert 'FuryChoiceButton(' in choice
    assert 'selected = selectedAttribute == option' in choice
    assert '"Бросить ${calculation.total?.let(::signed) ?: "—"}"' in choice


def test_economy_dialog_uses_user_facing_russian_labels_and_summary_cards():
    sheet = read(SHEET)
    economy = section(sheet, 'private fun EconomyDialog', '@Composable\nprivate fun MaximumDialog')
    assert 'XP adjustment' not in economy
    assert 'Text("ОС override' not in economy
    assert 'Корректировка опыта' in economy
    assert 'Очки способностей вручную' in economy
    assert 'EconomySummaryCard(' in economy
    assert 'Потрачено' in economy
    assert 'Осталось' in economy


def test_conditions_dialog_uses_compact_condition_rows_with_active_highlight_and_hover_action():
    sheet = read(SHEET)
    conditions = section(sheet, 'private fun ConditionsDialog', '@Composable\nprivate fun ConditionOverrideDialog')
    assert 'ConditionRow(' in conditions
    assert 'active = condition in state.extras.activeConditions' in conditions
    assert 'onEdit = { editCondition = condition }' in conditions
    row = section(sheet, 'private fun ConditionRow', '@Composable\nprivate fun ConditionsDialog')
    assert 'pointerMoveFilter' in row
    assert 'maxLines = 3' in row
    assert 'if (active)' in row
    assert 'Изменить' in row


def test_character_sheet_imports_layout_size_when_modifier_size_is_used():
    sheet = read(SHEET)
    assert 'Modifier.size(' in sheet
    assert 'import androidx.compose.foundation.layout.size' in sheet
