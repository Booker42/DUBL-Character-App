from pathlib import Path

from dubl.library_store import CharacterStore
from dubl.model import fresh, atomic_save


def test_character_library_create_save_duplicate_delete(tmp_path):
    store=CharacterStore(tmp_path)
    cid=store.create()
    assert store.exists(cid)
    state=store.load(cid);state['profile']['name']='Спорк';state['profile']['concept']='Дуэлянт'
    store.save(state)
    entries=store.list()
    assert entries[0].name=='Спорк'
    assert entries[0].concept=='Дуэлянт'
    dup=store.duplicate(cid)
    assert dup!=cid
    assert store.load(dup)['profile']['name'].endswith('— копия')
    store.delete(dup)
    assert not store.exists(dup)


def test_import_collision_gets_new_id_and_export_roundtrips(tmp_path):
    store=CharacterStore(tmp_path/'library')
    source=tmp_path/'source.json';state=fresh();state['profile']['name']='Тест';atomic_save(source,state)
    first=store.import_file(source)
    second=store.import_file(source)
    assert first!=second
    out=tmp_path/'test.dubl';store.export(first,out)
    assert out.exists()
    assert store.load(first)['profile']['name']=='Тест'


def test_legacy_autosave_imports_once(tmp_path):
    state=fresh();state['profile']['name']='Старый лист';atomic_save(tmp_path/'autosave.json',state)
    store=CharacterStore(tmp_path)
    imported=store.migrate_legacy_autosave()
    assert imported
    assert store.load(imported)['profile']['name']=='Старый лист'
    assert store.migrate_legacy_autosave() is None
