import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONFIG = json.loads((ROOT / 'rulesets/dubl-3.69/config.json').read_text(encoding='utf-8'))
CATALOG = json.loads((ROOT / 'shared/src/commonMain/resources/magic_equipment_catalog.json').read_text(encoding='utf-8'))
SPELL_BINDINGS = json.loads((ROOT / 'rulesets/dubl-3.69/bindings/spells.json').read_text(encoding='utf-8'))
GEAR_BINDINGS = json.loads((ROOT / 'rulesets/dubl-3.69/bindings/gear.json').read_text(encoding='utf-8'))


def test_magic_equipment_is_promoted_as_one_source_generated_runtime_artifact():
    meta = CONFIG['domains']['magic_equipment']
    assert meta['status'] == 'source_generated'
    assert meta['sources'] == ['core', 'archmage']
    assert meta['runtimeArtifact'] == 'shared/src/commonMain/resources/magic_equipment_catalog.json'


def test_runtime_counts_equal_tracked_source_bindings():
    assert len(CATALOG['spells']) == len(SPELL_BINDINGS['core']) + len(SPELL_BINDINGS['archmage']) == 265
    assert len(CATALOG['gear']) == len(GEAR_BINDINGS['bindings']) == 260


def test_rulebook_conflicts_are_visible_but_not_silently_resolved():
    conflicts = [spell for spell in CATALOG['spells'] if spell.get('conflictNote')]
    assert len(conflicts) == 57
    assert all(spell.get('incomplete') is True for spell in conflicts)


def test_runtime_artifact_does_not_carry_legacy_import_locator_fields():
    assert all('sourceStart' not in spell and 'sourceEnd' not in spell and 'alsoAt' not in spell for spell in CATALOG['spells'])
    assert all('sourceTable' not in gear for gear in CATALOG['gear'])
