import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED_RESOURCES = ROOT / 'shared/src/commonMain/resources'
ANDROID_ASSETS = ROOT / 'app/src/main/assets'
APP_BUILD = ROOT / 'app/build.gradle.kts'
ANDROID_DATA = ROOT / 'app/src/main/java/com/dubl/character/android/data'

CATALOGS = {
    'development_catalog.json': ('entries', 796),
    'chi_catalog.json': (('schools', 9), ('techniques', 68)),
    'magic_equipment_catalog.json': (('spells', 265), ('gear', 260)),
    'skill_effects_catalog.json': ('effects', 283),
}


def test_shared_resources_are_the_single_physical_canonical_catalog_copy():
    for name in CATALOGS:
        assert (SHARED_RESOURCES / name).exists(), name
        assert not (ANDROID_ASSETS / name).exists(), f'duplicate Android catalog asset remains: {name}'
    build = APP_BUILD.read_text(encoding='utf-8')
    assert 'assets.srcDir(rootProject.file("shared/src/commonMain/resources"))' in build


def test_android_catalog_repositories_delegate_to_shared_parsers():
    expected = {
        'DevelopmentCatalogRepository.kt': 'parseDevelopmentCatalog(raw)',
        'ChiCatalogRepository.kt': 'parseChiCatalog(raw)',
        'MagicEquipmentCatalogRepository.kt': 'parseMagicEquipmentCatalog(raw)',
        'SkillEffectCatalogRepository.kt': 'parseSkillEffectCatalog(raw)',
    }
    for filename, parser_call in expected.items():
        source = (ANDROID_DATA / filename).read_text(encoding='utf-8')
        assert parser_call in source, f'{filename} must call {parser_call}'
        assert 'org.json.' not in source, f'{filename} still owns JSON parsing semantics'


def test_canonical_catalog_counts_ids_and_archmage_content_are_locked():
    roots = {name: json.loads((SHARED_RESOURCES / name).read_text(encoding='utf-8')) for name in CATALOGS}
    for name, expectation in CATALOGS.items():
        root = roots[name]
        pairs = (expectation,) if isinstance(expectation[0], str) else expectation
        for key, count in pairs:
            assert len(root[key]) == count, (name, key, len(root[key]))
            ids = [item['id'] for item in root[key]]
            assert len(ids) == len(set(ids)), f'duplicate ids in {name}:{key}'

    spells = {item['name']: item for item in roots['magic_equipment_catalog.json']['spells']}
    expected_archmage = {
        'Клинок Короля-Ворона',
        'Копьё Последней Надежды',
        'Секира Ледяного Гиганта',
        'Лук Эльфийского Принца',
        'Щит Утерянного Легиона (реакция)',
    }
    assert expected_archmage <= spells.keys()
    assert all(spells[name]['school'] == 'Боевая магия' for name in expected_archmage)


def test_importers_write_to_shared_canonical_catalogs_by_default():
    importers = [
        ROOT / 'tools/import_android_0_4_content.py',
        ROOT / 'tools/import_android_0_5_chi.py',
        ROOT / 'tools/import_android_0_5_skill_effects.py',
    ]
    for path in importers:
        source = path.read_text(encoding='utf-8')
        assert 'app/src/main/assets/' not in source, f'{path.name} still writes duplicate Android assets'
        assert 'shared/src/commonMain/resources/' in source, f'{path.name} must target canonical shared resources'
