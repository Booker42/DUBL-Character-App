import json
from pathlib import Path

import pytest

from tools.rulebook.import_equipment import import_gear, promote_gear_catalog


def table(order, rows, path=('Снаряжение','Луки')):
    return {
        'id': f't-{order:06d}', 'kind': 'table', 'order': order,
        'rows': rows,
        'normalizedRows': [[str(c).casefold().strip() for c in row] for row in rows],
        'headingPath': list(path),
    }


def test_gear_table_import_preserves_fields_and_source_ref():
    raw = {'blocks': [table(1, [
        ['Название','Урон','Треб.','Дальность','Вес','Спец. свойства'],
        ['Короткий лук','1d4+1','1','10/50','0,3','Легкое, Натяжка'],
    ])]}
    bindings = {'bindings': [{
        'id': 'gear_test', 'name': 'Короткий лук', 'sourceTable': 1,
        'category': 'Снаряжение', 'section': 'Предметы',
    }]}
    payload, diagnostics = import_gear(raw, bindings, 'core')
    assert diagnostics == []
    item = payload['gear'][0]
    assert item['id'] == 'gear_test'
    assert item['fields']['Урон'] == '1d4+1'
    assert item['fields']['Вес'] == '0,3'
    assert item['sourceRefs'] == ['core:t-000001']
    assert 'Урон: 1d4+1' in item['description']


def test_duplicate_gear_names_are_resolved_by_source_table_binding():
    raw = {'blocks': [
        table(10, [['Название','Урон'],['Стрела','1d4']], ('Снаряжение','Луки')),
        table(11, [['Название','Урон'],['Стрела','1d6']], ('Снаряжение','Арбалеты')),
    ]}
    bindings = {'bindings': [
        {'id':'gear_a','name':'Стрела','sourceTable':10,'category':'Луки','section':'Предметы'},
        {'id':'gear_b','name':'Стрела','sourceTable':11,'category':'Арбалеты','section':'Предметы'},
    ]}
    payload, diagnostics = import_gear(raw, bindings, 'core')
    assert diagnostics == []
    assert {x['id']: x['fields']['Урон'] for x in payload['gear']} == {'gear_a':'1d4','gear_b':'1d6'}


def test_gear_promotion_keeps_source_content_and_stable_runtime_shape():
    source = {'gear': [{
        'id':'gear_x','name':'Молот','category':'Инструменты','section':'Предметы',
        'fields': {'Вес':'2'}, 'description':'Вес: 2', 'sourceRefs':['core:t-1'],
    }]}
    promoted = promote_gear_catalog(source, version='3.69')
    assert promoted['gear'][0] == {
        'id':'gear_x','name':'Молот','category':'Инструменты','section':'Предметы',
        'fields': {'Вес':'2'}, 'description':'Вес: 2'
    }


def test_real_equipment_import_reaches_all_current_bindings():
    root = Path(__file__).resolve().parents[2]
    raw_path = root / 'build/rulesets/dubl-3.69/source/core_raw_ir.json'
    binding_path = root / 'rulesets/dubl-3.69/bindings/gear.json'
    if not (raw_path.exists() and binding_path.exists()):
        pytest.skip('real rulebook build artifacts/bindings are not present')
    raw = json.loads(raw_path.read_text())
    bindings = json.loads(binding_path.read_text())
    payload, diagnostics = import_gear(raw, bindings, 'core')
    assert len(payload['gear']) == 260
    assert not [d for d in diagnostics if d['severity'] == 'error']
