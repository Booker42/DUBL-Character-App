import importlib.util
import sys
import pytest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / 'tools' / 'import_android_0_5_chi.py'
SOURCE = Path('/mnt/data/dubl_android_rebuild/Dубль, Мастера ближнего боя.docx')

if not SOURCE.exists():
    pytest.skip("historical importer source fixture is not part of this source snapshot", allow_module_level=True)


def load_module():
    spec = importlib.util.spec_from_file_location('chi_importer', MODULE_PATH)
    module = importlib.util.module_from_spec(spec)
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


def test_chi_parser_exact_scope():
    module = load_module()
    content = module.parse_chi_content(SOURCE)

    assert len(content.developments) == 27
    assert len(content.schools) == 9
    assert len(content.techniques) == 68
    assert len({item['id'] for item in content.developments}) == 27
    assert len({item['id'] for item in content.schools}) == 9
    assert len({item['id'] for item in content.techniques}) == 68


def test_chi_core_progression_and_school_requirements():
    module = load_module()
    content = module.parse_chi_content(SOURCE)
    developments = {item['name']: item for item in content.developments}
    techniques = {item['name']: item for item in content.techniques}

    assert developments['Внутренняя Ци']['costType'] == 'ability'
    assert developments['Внутренняя Ци']['abilityOptions'][0]['value'] == 1
    assert developments['Мастер Ци']['requirements'] == 'Внутренняя Ци, Воля 5'
    assert developments['Мастер Ци']['abilityOptions'][0]['value'] == 2
    assert developments['Пробуждённая Ци']['requirements'] == 'Мастер Ци, Воля 5'
    assert techniques['Вихрь ударов']['chiCost'] == 1
    assert techniques['Вихрь ударов']['requirements'] == 'Внутренняя Ци'
    assert techniques['Целительная Ци']['requirements'] == 'Мастер Ци'
    assert techniques['Рёв дракона']['school'] == 'Школа Дракона'
