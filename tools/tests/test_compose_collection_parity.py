from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SKILLS = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/SkillsScreen.kt').read_text(encoding='utf-8')
DEV = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt').read_text(encoding='utf-8')


def test_skills_expose_all_categories_and_group_results():
    assert 'SkillCategory.entries.take(4)' not in SKILLS
    assert 'SkillCategory.entries.chunked' in SKILLS
    assert 'skills.groupBy { it.category }' in SKILLS


def test_development_has_available_filter_and_android_grouping_semantics():
    assert 'availableOnly' in DEV
    assert 'Доступно сейчас' in DEV
    assert 'fun branchName(' in DEV
    assert 'groupBy(::branchName)' in DEV
    assert 'groupBy { it.category.ifBlank { "Общие" } }' in DEV
    assert 'groupBy { it.category.ifBlank { "Боевые искусства" } }' in DEV
    assert '&& !entry.incomplete' not in DEV
    assert 'filterNot { it.incomplete }' not in DEV
    assert 'MagicEquipmentRules.BASE_MANA_ENTRY_ID' in DEV


def test_chi_search_and_available_filter_cover_techniques():
    assert 'filteredChiTechniques' in DEV
    assert 'chiRules.availability(technique).unlocked' in DEV
    assert 'groupBy { it.school }' in DEV
