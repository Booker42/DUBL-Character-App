import copy,json,tempfile,unittest
from pathlib import Path
from dubl.model import fresh,normalize,atomic_save,load_file,CATALOG,ENTRIES
from dubl import engine as E

def feat(name,rank=1,ability=False,**kwargs):
    e=next(e for e in CATALOG['entries'] if E.alias(e['name'])==E.alias(name) and (e['costType']=='ability')==ability)
    return dict(id=e['id'],rank=rank,choice='',uid=name,**kwargs)

class RulesTests(unittest.TestCase):
    def setUp(self):self.s=fresh();self.s['attributes']={a:3 for a in CATALOG['attributes']}
    def test_core_formulas(self):
        d=E.derived(self.s);self.assertEqual((d['Здоровье'],d['Защита'],d['Рефлексы'],d['Инициатива'],d['Стойкость'],d['Бег'],d['Рывок']),(18,11,6,6,6,11,8))
    def test_size_and_health_bonus(self):
        self.s['profile']['size']=7;self.s['feats']=[feat('Невероятное здоровье',3)]
        self.assertEqual(E.derived(self.s)['Здоровье'],38);self.assertEqual(E.attrs(self.s)['Скорость'],1)
    def test_passive_feats(self):
        self.s['feats']=[feat('Выносливый',2),feat('Улучшенная инициатива',1),feat('Быстрые рефлексы',1),feat('Стойкий',2)]
        d=E.derived(self.s);self.assertEqual((d['Выносливость'],d['Инициатива'],d['Рефлексы'],d['Стойкость']),(5,7,7,8))
    def test_burden_boundaries(self):
        self.s['options']['loadAutomatic']=False
        for weight,penalty in [(5.9,0),(6,-1),(9,-1),(9.1,-2),(12,-2),(12.1,-4),(18,-4)]:
            self.s['options']['loadManual']=weight;self.assertEqual(E.derived(self.s)['loadPenalty'],penalty)
    def test_mana_table_and_extension(self):
        for p in range(1,21):
            for r in range(1,6):
                self.s['magic'].update(manaRank=r,power=p);self.assertEqual(E.mana(self.s),CATALOG['manaTable'][str(p)][r-1])
        for r,inc in enumerate([2,7,17,40,55],1):
            self.s['magic'].update(manaRank=r,power=23);self.assertEqual(E.mana(self.s),CATALOG['manaTable']['20'][r-1]+3*inc)
    def test_mana_feat_and_cost(self):
        self.s['magic'].update(manaRank=3,power=2);self.s['feats']=[feat('Увеличенный запас маны',2)];self.assertEqual(E.mana(self.s),12)
        self.assertEqual(E.costs(self.s)['Запас маны'],300)
    def test_spell_prices(self):
        self.assertEqual([E.learn_cost(x) for x in [0,3,4,8,9,12,13,16,17,20,21]],[10,10,20,20,30,30,40,40,50,50,None])
    def test_skill_bonuses(self):
        self.s['skills']['Медицина']={'rank':2};self.s['feats']=[feat('Лекарь',2)];sk=next(x for x in E.skills(self.s) if x['name']=='Медицина');self.assertEqual(E.skill_bonus(self.s,sk),7)
    def test_multi_attribute_skill_and_breakdown(self):
        self.s['skills']['Медицина']={'rank':2,'attrs':['Интеллект','Воля'],'formulaNote':'Полевой протокол'}
        sk=next(x for x in E.skills(self.s) if x['name']=='Медицина')
        self.assertEqual(E.skill_attrs(sk),['Интеллект','Воля'])
        self.assertEqual(E.skill_bonus(self.s,sk),8)
        formula=E.skill_formula_text(self.s,sk)
        self.assertIn('Интеллект',formula);self.assertIn('Воля',formula);self.assertIn('Полевой протокол',formula)
    def test_skill_attribute_migration_keeps_catalog_default(self):
        self.s['skills']['Атлетика']={'rank':1}
        n=normalize(self.s);sk=next(x for x in E.skills(n) if x['name']=='Атлетика')
        self.assertEqual(E.skill_attrs(sk),['Сила'])
        self.assertEqual(E.skill_bonus(n,sk),4)
        n2=normalize(dict(self.s,skills={'Атлетика':{'rank':1,'attr':'Ловкость'}}));sk2=next(x for x in E.skills(n2) if x['name']=='Атлетика')
        self.assertEqual(E.skill_attrs(sk2),['Ловкость'])
    def test_specific_knowledge_is_not_any_knowledge(self):
        self.s['customSkills']=[{'name':'Знание (История)','rank':8}]
        self.assertEqual(E.check_atom(self.s,'Знание (Магия) 3').state,'fail');self.assertEqual(E.check_atom(self.s,'Знание (Любое) 3').state,'ok')
    def test_shared_rank_or(self):
        self.s['skills']['Рукопашный бой']={'rank':4}
        self.assertEqual(E.check_atom(self.s,'Холодное оружие или Рукопашный бой 4').state,'ok')
    def test_assassin_branch(self):
        assassin=next(e for e in CATALOG['entries'] if e['name']=='Ассасин' and e['costType']=='xp')
        self.assertNotIn('accessId',assassin);self.assertTrue(assassin['perfectRoot'])
        child=next(e for e in CATALOG['entries'] if e['name']=='Незримая смерть')
        self.assertTrue(any(c.state=='fail' for c in E.requirements(self.s,child)))
        self.s['feats'].append(feat('Ассасин',overrideReason='Разрешено мастером'))
        self.assertEqual(E.check_atom(self.s,'Ассасин').state,'ok')
        self.s['feats'].clear();self.assertEqual(E.check_atom(self.s,'Ассасин').state,'fail')
    def test_one_perfect_branch(self):
        self.s['feats']=[feat('Одержимость')]
        assassin=next(e for e in CATALOG['entries'] if e['name']=='Ассасин' and e['costType']=='xp')
        self.assertTrue(any('одна совершенная' in c.text and c.state=='fail' for c in E.requirements(self.s,assassin)))
    def test_branch_access(self):
        e=next(e for e in CATALOG['entries'] if e.get('accessId') and not e.get('incomplete'))
        self.assertEqual(E.requirements(self.s,e)[0].state,'fail')
        self.s['feats'].append(dict(id=e['accessId'],rank=1,overrideReason='Допущен'))
        self.assertEqual(E.requirements(self.s,e)[0].state,'ok')
    def test_unknown_is_manual(self):self.assertEqual(E.check_atom(self.s,'Благословение древнего духа').state,'manual')
    def test_duplicates_do_not_charge_twice(self):
        self.s['feats']=[feat('Выносливый',1),feat('Выносливый',2)];n=normalize(self.s);self.assertEqual(len(n['feats']),1);self.assertEqual(n['feats'][0]['rank'],2)
        self.s['magic']['spells']=[{'name':'Щит','cost':3},{'name':'Щит','cost':3}];self.assertEqual(len(normalize(self.s)['magic']['spells']),1)
    def test_migrate_v09(self):
        idx=next(k for k,v in CATALOG['legacy'].items() if isinstance(v,str) and ENTRIES[v]['name']=='Невероятное здоровье')
        old={'profile':{'name':'Тест','size':5,'xpTotal':1000,'loadCurrent':4},'feats':[{'index':int(idx),'rank':2},{'index':int(idx),'rank':1}],'attributes':{'Сила':3},'abilities':[{'name':'Ци','effect':'Энергия'}]}
        n=normalize(old);self.assertEqual(n['profile']['name'],'Тест');self.assertEqual(len(n['feats']),2);self.assertEqual(n['feats'][0]['rank'],2);self.assertIn('v09',n['legacyArchive']);self.assertEqual(n['options']['loadManual'],4)
    def test_atomic_save_backup_roundtrip(self):
        with tempfile.TemporaryDirectory() as folder:
            p=Path(folder)/'hero.json';atomic_save(p,self.s);self.s['profile']['name']='Новый';atomic_save(p,self.s)
            self.assertEqual(load_file(p)['profile']['name'],'Новый');self.assertEqual(load_file(p.with_suffix('.json.bak'))['profile']['name'],'')
    def test_invalid_input(self):
        for raw in [[],{}, {'schema':99,'profile':{}},{'skills':[]},{'profile':{},'magic':{'spells':[4]}}]:
            with self.assertRaises(ValueError):normalize(raw)
    def test_catalog_identity_integrity(self):
        self.assertEqual(len(ENTRIES),len(CATALOG['entries']))
        for e in CATALOG['entries']:
            if e.get('accessId'):self.assertIn(e['accessId'],ENTRIES)
        for s in CATALOG['spells']:self.assertTrue(s['description'])
if __name__=='__main__':unittest.main()
