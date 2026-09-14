from pathlib import Path
MAGIC = (Path(__file__).resolve().parents[2] / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/MagicScreen.kt').read_text(encoding='utf-8')


def test_magic_defaults_to_hiding_unlearned_schools_and_can_add_school():
    assert 'mutableStateOf(true)' in MAGIC
    assert 'addSchool' in MAGIC
    assert '+ Школа' in MAGIC
    assert 'addMagicSchool' in MAGIC


def test_spellbook_and_catalog_have_school_filter():
    assert 'schoolFilter' in MAGIC
    assert 'Школа:' in MAGIC
    assert 'schoolFilter ?: "Все"' in MAGIC
    assert 'schoolFilter == null' in MAGIC
    assert 'MagicSchoolCatalog.parseSchools(spell.school).contains(schoolFilter)' in MAGIC


def test_school_errors_are_reported_instead_of_silent_failure():
    assert 'schoolError' in MAGIC
    assert 'Не удалось сохранить школу' in MAGIC
