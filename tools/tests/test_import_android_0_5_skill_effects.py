import importlib.util
import sys
import pytest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / 'tools' / 'import_android_0_5_skill_effects.py'
SOURCE = Path('/mnt/data/DUBL-Android-0.5-skill-effects-roll-context-review-v3.docx')

if not SOURCE.exists():
    pytest.skip("historical importer source fixture is not part of this source snapshot", allow_module_level=True)


def load_module():
    spec = importlib.util.spec_from_file_location('effect_importer', MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def test_review_parser_keeps_all_approved_rows():
    module = load_module()
    effects = module.parse_review(SOURCE)
    assert len(effects) == 283
    assert [effect['reviewIndex'] for effect in effects] == list(range(1, 284))


def test_key_effects_are_structured_for_runtime():
    module = load_module()
    effects = {effect['sourceName']: effect for effect in module.parse_review(SOURCE)}

    diplomat = effects['Дипломат']
    assert diplomat['mode'] == 'toggle_bonus'
    assert diplomat['targetSkill'] == 'Красноречие'
    assert diplomat['value'] == 1
    assert diplomat['perRank'] is True

    athletic = effects['Атлетичность']
    assert athletic['mode'] == 'auto_bonus'
    assert athletic['targetSkill'] == 'Атлетика'
    assert athletic['value'] == 1
    assert athletic['perRank'] is True

    fencer = effects['Фехтовальщик']
    assert fencer['rollContext'] == 'PARRY'
    assert fencer['status'] == '0.5 · NEW'

    block = effects['Блокирование']
    assert block['mode'] == 'reminder'
    assert block['status'] == 'СТОП · RULE GAP'
