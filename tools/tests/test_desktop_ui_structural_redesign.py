from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/Main.kt'
PRIMITIVES = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/UiPrimitives.kt'
SHEET = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt'


def read(path: Path) -> str:
    assert path.exists(), f'missing {path}'
    return path.read_text(encoding='utf-8')


def test_desktop_workspace_caps_ultrawide_stretch_and_rail_has_no_prototype_footer():
    main = read(MAIN)
    assert '.widthIn(max = 2160.dp)' in main
    assert 'contentAlignment = Alignment.TopCenter' in main
    assert 'Desktop 0.2 · Compose parity' not in main
    assert 'Modifier.width(230.dp)' in main
    assert 'DesktopSection.entries.filter { it != DesktopSection.CHARACTERS }' in main
    assert 'NavigationItem(DesktopSection.CHARACTERS' in main


def test_desktop_presentation_primitives_exist():
    src = read(PRIMITIVES)
    for primitive in (
        'DesktopPanel',
        'DesktopSectionHeader',
        'DesktopHeroPanel',
        'DesktopStatCell',
        'DesktopMetricCell',
        'DesktopResourceRow',
        'DesktopConditionChip',
        'DesktopSmallAction',
    ):
        assert f'fun {primitive}' in src, primitive


def test_character_sheet_uses_dashboard_primitives_and_collapses_conditions():
    sheet = read(SHEET)
    for token in (
        'BoxWithConstraints',
        'DesktopHeroPanel',
        'DesktopResourceTile',
        'DesktopDenseAttributeRow',
        'DesktopDenseMetricRow',
        'DesktopSkillRow',
        'DesktopConditionChip',
    ):
        assert token in sheet, token
    assert 'SectionCard("Состояния"' not in sheet
    assert 'private fun CharacterDashboard' in sheet
    assert 'private fun SheetSummaries' in sheet


def test_character_sheet_preserves_shared_application_callbacks():
    sheet = read(SHEET)
    for token in (
        'state.changeHp(',
        'state.changeEndurance(',
        'state.changeMana(',
        'state.changeChi(',
        'state.changeAttribute(',
        'state.undoLast()',
        'state.setPortrait(',
        'state.setSkillGroups(',
        'state.setDevelopmentGroups(',
    ):
        assert token in sheet, token
    assert 'horizontalScroll' not in sheet


def test_character_sheet_matches_dense_three_column_mock_and_notes():
    sheet = read(SHEET)
    primitives = read(PRIMITIVES)
    assert 'private fun DenseStatsSkillsRow' in sheet
    assert 'private fun CompactCharacteristicsPanel' in sheet
    assert 'private fun CompactMetricsPanel' in sheet
    assert 'private fun SheetSkillsPanel' in sheet
    assert 'private fun NotesPanel' in sheet
    assert 'private fun NotesDialog' in sheet
    assert 'private fun DerivedMetricsPanel' not in sheet
    assert 'DesktopIconKind' in primitives
    assert 'fun DesktopIcon' in primitives
    assert 'fun DesktopDenseAttributeRow' in primitives
    assert 'fun DesktopDenseMetricRow' in primitives
    assert 'fun DesktopSkillRow' in primitives
    assert 'fun DesktopResourceTile' in primitives
    assert 'state.setNotes(' in sheet


def test_live_screenshot_regression_uses_compact_hero_resources_and_content_height_dashboard():
    main = read(MAIN)
    sheet = read(SHEET)
    primitives = read(PRIMITIVES)
    for token in (
        'DesktopBackground',
        'DesktopSurface',
        'DesktopAccent',
        'DesktopGold',
        'DesktopMana',
    ):
        assert token in primitives, token
    assert 'DesktopVisualTheme' in main
    assert 'private fun HeroResources' in sheet
    assert 'maxVisible = if (compact) 4 else 6' in sheet
    assert 'if (shown.size <= 4) shown.size.coerceAtLeast(1) else 3' in sheet
    assert 'Ещё $overflow' in sheet
    assert 'private fun ResourcesPanel' not in sheet
    dense = sheet.split('private fun DenseStatsSkillsRow', 1)[1].split('private fun CompactCharacteristicsPanel', 1)[0]
    assert 'height(IntrinsicSize.Max)' not in dense
    assert '.fillMaxHeight()' not in dense
    assert 'Modifier.weight(if (wide) 0.53f else 0.48f)' in dense
    assert 'character.customResources.forEach { resource ->' in sheet
    assert 'TextButton(onClick = onCreateCustomResource) { Text("+ Свой ресурс") }' not in sheet


def test_notes_flow_through_shared_application_boundary_and_persistence():
    extras = read(ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterSheetExtras.kt')
    session = read(ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterExtrasSession.kt')
    sheet_app = read(ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/application/SheetApplication.kt')
    desktop_state = read(ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt')
    desktop_store = read(ROOT / 'shared/src/desktopMain/kotlin/com/dubl/character/android/data/DesktopCharacterExtrasStore.kt')
    android_store = read(ROOT / 'app/src/main/java/com/dubl/character/android/data/CharacterSheetExtrasRepository.kt')
    assert 'val notes: String = ""' in extras
    assert 'fun setNotes(' in session
    assert 'fun setNotes(' in sheet_app
    assert 'fun setNotes(' in desktop_state
    assert '\"notes\"' in desktop_store
    assert 'extras.notes' in android_store


def test_dense_sheet_keeps_grouping_and_development_details_reachable():
    sheet = read(SHEET)
    assert 'onGrouping(GroupingKind.SKILLS)' in sheet
    assert 'onGrouping(GroupingKind.DEVELOPMENT)' in sheet
    assert 'onDevelopmentDetails(item.entry)' in sheet


def test_sheet_skills_show_all_visible_ranks_and_preserve_hidden_group_membership():
    sheet = read(SHEET)
    skills_panel = sheet.split('private fun SheetSkillsPanel', 1)[1].split('private fun skillIcon', 1)[0]
    assert 'character.resolvedSkills(includeHidden = true)' in skills_panel
    assert 'character.resolvedSkills()' in skills_panel
    assert 'filter { it.rank > 0 }' not in skills_panel
    assert 'SheetGroupHeaderCompact' in skills_panel
    assert 'SheetGroupingRules.balancedColumns' in skills_panel
    assert 'rank = skill.rank' in skills_panel
    assert 'bonus = calc.total?.let(::signed) ?: "—"' in skills_panel

    primitives = read(PRIMITIVES)
    skill_row = primitives.split('internal fun DesktopSkillRow', 1)[1].split('internal fun DesktopResourceTile', 1)[0]
    assert 'rank: Int' in skill_row
    assert 'Text("Ранг $rank"' in skill_row
    assert 'Text(bonus' in skill_row


def test_sheet_development_summary_restores_grouped_hierarchy_from_shared_parent_ids():
    sheet = read(SHEET)
    summary = sheet.split('private fun SheetSummaries', 1)[1].split('private fun rankLabel', 1)[0]
    for token in (
        'val parentById = developmentItems.associate { it.entry.id to it.parentId }',
        'SheetGroupingRules.hierarchicalOrder',
        'SheetGroupingRules.hierarchyBlocks',
        'SheetGroupingRules.localDepth',
        'SheetGroupHeaderCompact',
        'DevelopmentTreeRow',
    ):
        assert token in summary, token


def test_grouping_manager_includes_rank_zero_visible_skills_and_keeps_tree_drag_contract():
    sheet = read(SHEET)
    grouping = sheet.split('private fun GroupingManagerDialog', 1)[1].split('private fun DraggableGroupingItem', 1)[0]
    assert 'character.resolvedSkills(includeHidden = true)' in grouping
    assert 'filter { it.rank > 0 }' not in grouping
    assert 'SheetGroupingRules.subtreeBlock' in grouping
    assert 'SheetGroupingRules.hierarchicalOrder' in grouping
    assert 'movesTree' in grouping


def test_desktop_skills_screen_keeps_all_visible_default_and_hidden_restore_entrypoint():
    skills = read(ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/SkillsScreen.kt')
    assert 'val skills = character.resolvedSkills().filter { skill ->' in skills
    assert '(!learnedOnly || skill.rank > 0)' in skills
    assert 'character.hiddenSkillIds.size' in skills
    assert 'HiddenSkillsDialog' in skills
    assert 'state.hideSkill(skill.id)' in skills
    assert 'state.restoreSkill(skill.id)' in skills
    assert 'state.restoreAllSkills()' in skills
