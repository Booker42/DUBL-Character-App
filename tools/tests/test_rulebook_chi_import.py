import sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT))

from tools.rulebook.import_chi import import_chi, compare_chi_runtime


def p(i,text,style='normal'):
    return {'id':f'p-{i:06d}','kind':'paragraph','order':i,'text':text,'style':style,'headingLevel':None,'headingPath':[]}

def t(i,rows):
    return {'id':f't-{i:06d}','kind':'table','order':i,'rows':rows,'headingPath':[]}


def test_imports_core_chi_development_named_technique_and_school_table():
    raw={'blocks':[
        p(1,'Ци','Title'),
        p(2,'Базовые приёмы Ци (доступны после изучения способности «Внутренняя Ци»)','Heading 4'),
        p(3,'Вихрь ударов (1 Ци, 1 ОД)\nДополнительная атака.'),
        p(4,'Развитие Ци','Heading 3'),
        p(5,'Внутренняя Ци (способность, стоимость 1 очко способностей) — открывает доступ.'),
        p(6,'Мастер Ци (способность, стоимость 2 очка, требуется Воля 5) — продвинутые приёмы.'),
        p(7,'Восстановление Ци (навык, стоимость 30 опыта за ранг, макс. 3) — восстановление.'),
        p(8,'Школа Пылающего Кулака (Путь Огненной Ци)','Heading 2'),
        p(9,'Требования: Сила 4, Воля 3.'), p(10,'Пассивные преимущества школы:'), p(11,'+1 урон.'),
        p(12,'Уникальные приёмы школы (доступны после вступления):'),
        t(13,[['Название','Стоимость Ци','Действие','Эффект'],['Огненный кулак','1','1 ОД','+2 урона']]),
        p(14,'Оружейное кунг-фу','Title'),
    ]}
    payload,diags=import_chi(raw,'melee')
    assert diags == []
    by={x['name']:x for x in payload['developments']}
    assert by['Внутренняя Ци']['abilityOptions']==[{'source':'ЦИ','value':1}]
    assert by['Мастер Ци']['requirements']=='Внутренняя Ци, Воля 5'
    assert by['Восстановление Ци']['cost']==30 and by['Восстановление Ци']['ranks']==3
    tech={x['name']:x for x in payload['techniques']}
    assert tech['Вихрь ударов']['chiCost']==1
    assert tech['Огненный кулак']['school']=='Школа Пылающего Кулака'
    assert payload['schools'][0]['requirements']=='Сила 4, Воля 3'


def test_compare_chi_runtime_reports_development_and_catalog_drift():
    imported={'developments':[{'id':'d','name':'D','cost':30,'costType':'xp','ranks':1,'requirements':'R','benefit':'B','abilityOptions':[]}],
              'schools':[{'id':'s','name':'S','requirements':'R','passives':['P']}],
              'techniques':[{'id':'t','name':'T','school':'S','chiCost':1,'action':'1 ОД','effect':'E','requirements':'R'}]}
    development={'entries':[{'id':'d','section':'ЦИ','cost':40,'costType':'xp','ranks':1,'requirements':'R','benefit':'B'}]}
    chi={'schools':[{'id':'s','name':'S','requirements':'R','passives':['P']}],
         'techniques':[{'id':'t','name':'T','school':'S','chiCost':2,'action':'1 ОД','effect':'E','requirements':'R'}]}
    diags=compare_chi_runtime(imported,development,chi)
    assert {d['kind'] for d in diags}=={'chi-development-runtime-drift','chi-technique-runtime-drift'}
