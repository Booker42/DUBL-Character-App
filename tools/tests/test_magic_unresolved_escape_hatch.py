from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SESSION = (ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterSession.kt').read_text(encoding='utf-8')
ANDROID = (ROOT / 'app/src/main/java/com/dubl/character/android/ui/screens/MagicScreen.kt').read_text(encoding='utf-8')
DESKTOP = (ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/MagicScreen.kt').read_text(encoding='utf-8')


def test_shared_backend_does_not_hard_reject_incomplete_rulebook_spell():
    assert 'if (entry.incomplete) return false' not in SESSION
    assert 'incomplete = entry.incomplete' in SESSION
    assert 'conflictNote = entry.conflictNote' in SESSION


def test_android_catalog_keeps_incomplete_spell_addable_and_editable_after_add():
    assert 'enabled = !owned && !entry.incomplete' not in ANDROID
    assert 'if (!owned && !entry.incomplete) onAdd(entry)' not in ANDROID
    assert 'onEdit = { editSpell = spell' in ANDROID


def test_desktop_catalog_keeps_incomplete_spell_addable():
    assert 'enabled = !spell.incomplete' not in DESKTOP
    assert 'state.mutate { addCatalogSpell(spell) }' in DESKTOP
