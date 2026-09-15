import json
from pathlib import Path

import pytest

from tools.rulebook.import_magic import (
    import_core_spells,
    import_archmage_spells,
    promote_spell_catalog,
)


def para(order, text, path, style='normal', pid=None):
    return {
        'id': pid or f'p-{order:06d}',
        'kind': 'paragraph',
        'order': order,
        'text': text,
        'normalizedText': text.casefold(),
        'style': style,
        'headingLevel': 5 if style.startswith('Heading') else None,
        'headingPath': path,
    }


def test_core_spell_card_imports_multiline_fields_and_source_refs():
    raw = {'blocks': [
        para(1, 'Искра', ['Магия', 'Описание заклинаний', 'Искра'], 'Heading 5'),
        para(2, 'Школа: Разрушение', ['Магия', 'Описание заклинаний', 'Искра']),
        para(3, 'Стоимость: 2', ['Магия', 'Описание заклинаний', 'Искра']),
        para(4, 'Время сотворения: 1 ОД', ['Магия', 'Описание заклинаний', 'Искра']),
        para(5, 'Дальность: 10 метров', ['Магия', 'Описание заклинаний', 'Искра']),
        para(6, 'Действие: Ловкость против Защиты', ['Магия', 'Описание заклинаний', 'Искра']),
        para(7, 'Описание: Первая строка.', ['Магия', 'Описание заклинаний', 'Искра']),
        para(8, 'Вторая строка.', ['Магия', 'Описание заклинаний', 'Искра']),
        para(9, 'Усиление:', ['Магия', 'Описание заклинаний', 'Искра']),
        para(10, '+1 мана → +1d4 урона.', ['Магия', 'Описание заклинаний', 'Искра']),
    ]}
    bindings = {'bindings': [{'id': 'spell_test', 'name': 'Искра'}]}
    payload, diagnostics = import_core_spells(raw, bindings, 'core')
    assert diagnostics == []
    assert len(payload['spells']) == 1
    spell = payload['spells'][0]
    assert spell['id'] == 'spell_test'
    assert spell['school'] == 'Разрушение'
    assert spell['cost'] == 2
    assert spell['time'] == '1 ОД'
    assert spell['range'] == '10 метров'
    assert spell['action'] == 'Ловкость против Защиты'
    assert spell['description'] == 'Первая строка.\nВторая строка.'
    assert spell['enhancement'] == '+1 мана → +1d4 урона.'
    assert spell['sourceRefs'][0] == 'core:p-000001'


def test_archmage_inline_definition_filters_to_supported_schools_and_keeps_binding_id():
    raw = {'blocks': [
        para(1, '1. Магический барьер', ['Ограждение'], 'Heading 3'),
        para(2, 'Школа: Ограждение\nСтоимость: 1\nВремя сотворения: Реакция\nДальность: На себя\nДлительность: 1 раунд\nОписание: Поглощает 10 урона.\nУсиление: +2 маны → 20 урона.', ['Ограждение']),
        para(3, '2. Огненная штука', ['Elemental'], 'Heading 3'),
        para(4, 'Школа: Элементалистика\nСтоимость: 1\nОписание: Не импортировать.', ['Elemental']),
    ]}
    bindings = {'bindings': [{'id': 'archmage_test', 'name': 'Магический барьер'}]}
    payload, diagnostics = import_archmage_spells(raw, bindings, 'archmage')
    assert diagnostics == []
    assert [x['id'] for x in payload['spells']] == ['archmage_test']
    spell = payload['spells'][0]
    assert spell['source'] == 'Книга Архимага'
    assert spell['school'] == 'Ограждение'
    assert spell['cost'] == 1
    assert spell['description'] == 'Поглощает 10 урона.'


def test_spell_promotion_marks_missing_required_mechanics_incomplete_without_guessing():
    source = {'spells': [{
        'id': 'spell_x', 'name': 'Неполное', 'school': '', 'cost': 1, 'manaText': '1',
        'time': '', 'range': '', 'area': '', 'action': '', 'duration': '',
        'description': '', 'enhancement': '', 'sourceRefs': ['core:p-1'],
    }]}
    promoted = promote_spell_catalog(source, version='3.69')
    spell = promoted['spells'][0]
    assert spell['incomplete'] is True
    assert 'описание' in spell['conflictNote'].casefold()


def test_real_magic_import_reaches_all_current_spell_bindings():
    root = Path(__file__).resolve().parents[2]
    core_path = root / 'build/rulesets/dubl-3.69/source/core_raw_ir.json'
    arch_path = root / 'build/rulesets/dubl-3.69/source/archmage_raw_ir.json'
    binding_path = root / 'rulesets/dubl-3.69/bindings/spells.json'
    if not (core_path.exists() and arch_path.exists() and binding_path.exists()):
        pytest.skip('real rulebook build artifacts/bindings are not present')
    core = json.loads(core_path.read_text())
    arch = json.loads(arch_path.read_text())
    bindings = json.loads(binding_path.read_text())
    core_payload, core_diag = import_core_spells(core, {'bindings': bindings['core']}, 'core')
    arch_payload, arch_diag = import_archmage_spells(arch, {'bindings': bindings['archmage']}, 'archmage')
    assert len(core_payload['spells']) == 206
    assert len(arch_payload['spells']) == 59
    assert not [d for d in core_diag + arch_diag if d['severity'] == 'error']
