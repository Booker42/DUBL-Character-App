from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
MAGIC = (ROOT/'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/MagicScreen.kt').read_text(encoding='utf-8')
GEAR = (ROOT/'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/EquipmentScreen.kt').read_text(encoding='utf-8')
SKILLS = (ROOT/'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/SkillsScreen.kt').read_text(encoding='utf-8')


def test_magic_destructive_actions_require_confirmation():
    assert 'pendingDeleteSchoolIndex' in MAGIC
    assert 'pendingDeleteSpellUid' in MAGIC
    assert 'Удалить школу?' in MAGIC
    assert 'Удалить заклинание?' in MAGIC


def test_equipment_delete_requires_confirmation():
    assert 'pendingDeleteUid' in GEAR
    assert 'Удалить предмет?' in GEAR


def test_custom_skill_creation_surfaces_duplicate_or_invalid_error():
    assert 'customError' in SKILLS
    assert 'Введите корректное уникальное название' in SKILLS
    assert 'addCustomSkill(' in SKILLS
    assert 'addSpecializedSkill(' in SKILLS
