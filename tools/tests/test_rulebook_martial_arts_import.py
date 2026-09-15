import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT))
from tools.rulebook.import_martial_arts import import_martial_arts, compare_martial_runtime


def _p(i, text, style='normal'):
    return {'id':f'p-{i:06d}','kind':'paragraph','order':i,'text':text,'style':style,'headingLevel':None,'headingPath':[]}


def _t(i, rows):
    return {'id':f't-{i:06d}','kind':'table','order':i,'rows':rows,'headingPath':[]}


def test_imports_style_paragraph_technique_and_table_stance_from_raw_ir():
    raw={'blocks':[
        _p(1,'Рукопашные искусства','Title'),
        _p(2,'Айкидо','Title'),
        _p(3,'Айкидо','Heading 2'),
        _p(4,'Описание: стиль'),
        _p(5,'Стоимость: 50\nРанг: 1\nТребование: Рукопашный бой 4'),
        _p(6,'Выгода: Перенаправление.'),
        _p(7,''), _p(8,'Приёмы'), _p(9,''),
        _p(10,'Фиксатор'), _p(11,'описание'), _p(12,'Стоимость: 40'), _p(13,'Ранги: 1'),
        _p(14,'Требование: Боевые искусства (Айкидо)'), _p(15,'Выгода: Захват.'), _p(16,''),
        _p(17,'Стойки','Heading 3'),
        _t(18,[['№','Название','Стоимость','Эффект'],['1','Камаэ','30, 1 ранг','+1 к защите']]),
        _p(19,'Оружейные искусства','Title'),
        _p(20,'Гибридные искусства','Title'),
    ]}
    payload, diagnostics=import_martial_arts(raw,'melee')
    assert diagnostics == []
    assert len(payload['styles']) == 1
    style=payload['styles'][0]
    assert style['name']=='Айкидо'
    assert style['cost']==50 and style['ranks']==1
    assert style['requirements']=='Рукопашный бой 4'
    assert style['benefit']=='Перенаправление.'
    assert style['sourceRefs']
    tech={x['name']:x for x in payload['techniques']}
    assert tech['Фиксатор']['cost']==40
    assert tech['Фиксатор']['requirements']=='Боевые искусства (Айкидо)'
    assert tech['Камаэ']['cost']==30
    assert 'Стойка' in tech['Камаэ']['tags']


def test_shared_technique_merges_style_sources_when_requirement_is_generated():
    raw={'blocks':[
        _p(1,'Рукопашные искусства','Title'),
        _p(2,'Стиль А','Title'), _p(3,'Стоимость: 50\nРанг: 1\nТребование: Сила 3'), _p(4,'Выгода: А'),
        _p(5,'Приёмы'), _p(6,''), _p(7,'Ускользание'), _p(8,'Стоимость: 30'), _p(9,'Ранги: 1'), _p(10,'Выгода: Уйти.'), _p(11,''),
        _p(12,'Стиль Б','Title'), _p(13,'Стоимость: 50\nРанг: 1\nТребование: Ловкость 3'), _p(14,'Выгода: Б'),
        _p(15,'Приёмы'), _p(16,''), _p(17,'Ускользание'), _p(18,'Стоимость: 30'), _p(19,'Ранги: 1'), _p(20,'Выгода: Уйти.'), _p(21,''),
        _p(22,'Гибридные искусства','Title'),
    ]}
    payload,_=import_martial_arts(raw,'melee')
    entry=payload['techniques'][0]
    assert entry['name']=='Ускользание'
    assert entry['requirements']=='Боевые искусства: Стиль А или Стиль Б'
    assert entry['category']=='Общие приёмы'
    assert set(entry['sourceStyles'])=={'Стиль А','Стиль Б'}


def test_compare_martial_runtime_reports_rule_field_drift_only():
    imported={'entries':[{'id':'x','name':'X','cost':40,'ranks':1,'requirements':'A','benefit':'B','notes':'','sourceRefs':['melee:x']}]}
    runtime={'entries':[{'id':'x','name':'X','section':'Боевые искусства','category':'UI','cost':50,'ranks':1,'requirements':'A','benefit':'B','notes':'','tags':['UI']}]}
    d=compare_martial_runtime(imported,runtime)
    assert len(d)==1
    assert d[0]['details']['differences']=={'cost':{'rulebook':40,'runtime':50}}
