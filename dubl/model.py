"""Character data, migration and atomic local storage. No UI or network dependencies."""
from __future__ import annotations
import copy,json,os,tempfile,uuid,math,sys
from pathlib import Path

ROOT=Path(__file__).resolve().parent.parent
CATALOG=json.loads((ROOT/'data/catalog.json').read_text(encoding='utf-8'))
ENTRIES={e['id']:e for e in CATALOG['entries']}
SPELLS={e['id']:e for e in CATALOG['spells']}
ATTRS=CATALOG['attributes']

def number(v,default=0):
    try:
        n=float(v)
        return n if math.isfinite(n) else default
    except (ValueError,TypeError):return default

def fresh():
    return {'schema':10,'id':str(uuid.uuid4()),'profile':{'name':'','concept':'','size':5,'xpTotal':0,'creationXp':0,'abilityBudget':None,'notes':'','portrait':''},
      'attributes':{a:0 for a in ATTRS},'skills':{},'customSkills':[], 'feats':[],
      'magic':{'manaRank':0,'power':0,'currentMana':None,'schools':[],'spells':[]},
      'resources':{'healthCurrent':None,'staminaCurrent':None,'manaVisible':False,'custom':[]},
      'cyber':{'enabled':False,'implants':[],'limitMode':'scale'},'gear':[],
      'modifiers':[],'customBlocks':[],'ui':{'hiddenSkills':[],'layout':'','geometry':'','fontSize':11,'theme':'dark','locked':False,'layoutGeneration':3},
      'options':{'sizeModifiers':True,'loadAutomatic':True,'loadManual':0},'migrationNotes':[], 'legacyArchive':{}}

def normalize(raw):
    if not isinstance(raw,dict):raise ValueError('Файл персонажа должен содержать объект JSON.')
    if not any(k in raw for k in ['profile','attributes','skills','feats']):raise ValueError('Это не файл персонажа Дубль.')
    if number(raw.get('schema',0))>10:raise ValueError('Файл создан более новой версией приложения.')
    s=fresh();legacy=raw.get('schema')!=10
    for k in ['profile','attributes','magic','resources','cyber','ui','options']:
        if k in raw and not isinstance(raw[k],dict):raise ValueError(f'Повреждён раздел: {k}')
        s[k].update(copy.deepcopy(raw.get(k,{})))
    for k in ['customSkills','feats','gear','modifiers','customBlocks','migrationNotes']:
        if k in raw and not isinstance(raw[k],list):raise ValueError(f'Повреждён список: {k}')
        s[k]=copy.deepcopy(raw.get(k,[]))
    for owner,key in [('magic','schools'),('magic','spells'),('resources','custom'),('cyber','implants')]:
        if not isinstance(s[owner].get(key),list):raise ValueError(f'Повреждён список: {owner}.{key}')
        if not all(isinstance(x,dict) for x in s[owner][key]):raise ValueError(f'Неверная запись: {owner}.{key}')
    for key in ['customSkills','feats','gear','modifiers','customBlocks']:
        if not all(isinstance(x,dict) for x in s[key]):raise ValueError(f'Неверная запись: {key}')
    if not isinstance(raw.get('skills',{}),dict):raise ValueError('Повреждён список умений.')
    s['skills']=copy.deepcopy(raw.get('skills',{}));s['id']=str(raw.get('id',s['id']))
    if not all(isinstance(x,dict) for x in s['skills'].values()):raise ValueError('Неверная запись умения.')
    s['legacyArchive']=copy.deepcopy(raw.get('legacyArchive',{}))
    if not isinstance(s['legacyArchive'],dict):raise ValueError('Повреждён архив импорта.')
    if legacy:
        s['legacyArchive']['v09']=copy.deepcopy(raw)
        s['options']['sizeModifiers']=False
        s['options']['loadAutomatic']=False;s['options']['loadManual']=number(s['profile'].get('loadCurrent'))
        s['migrationNotes'].append('Импорт 0.9: исходные данные сохранены. Поправки размера отключены; прежняя нагрузка перенесена вручную.')
        s['profile']['creationXp']=number(s['profile'].get('xpTotal'))
        converted=[]
        for f in s['feats']:
            target=CATALOG['legacy'].get(str(f.get('index')))
            if isinstance(target,str):
                f['id']=target
                existing=next((x for x in converted if x.get('id')==target and x.get('choice','')==f.get('choice','')),None)
                if existing:
                    existing['rank']=max(number(existing.get('rank',1)),number(f.get('rank',1)))
                    s['migrationNotes'].append('Объединена повторная покупка: '+ENTRIES[target]['name'])
                    continue
            else:
                old=target.get('record',{}) if isinstance(target,dict) else {}
                f.update(id='custom_'+str(uuid.uuid4()),custom=True,name=old.get('name','Перенесённый навык'),description=old.get('benefit',''),cost=old.get('cost',0),costType=old.get('costType','xp'))
                s['migrationNotes'].append('Запись требует сверки: '+f['name'])
            converted.append(f)
        s['feats']=converted
        for a in raw.get('abilities',[]) if isinstance(raw.get('abilities'),list) else []:
            if isinstance(a,dict):s['feats'].append({'id':'custom_'+str(uuid.uuid4()),'custom':True,'name':a.get('name','Способность'),'description':a.get('description',a.get('effect',a.get('note',''))),'cost':number(a.get('cost')),'costType':'ability','rank':1})
    for a in ATTRS:s['attributes'][a]=int(max(-5,min(10,number(s['attributes'].get(a)))))
    for k in ['size','xpTotal','creationXp']:
        s['profile'][k]=int(number(s['profile'].get(k),5 if k=='size' else 0))
    s['profile']['size']=max(1,min(10,s['profile']['size']))
    if s['profile'].get('abilityBudget') is not None:s['profile']['abilityBudget']=max(0,int(number(s['profile']['abilityBudget'])))
    s['profile']['name']=str(s['profile'].get('name',''));s['profile']['concept']=str(s['profile'].get('concept',''));s['profile']['portrait']=str(s['profile'].get('portrait','') or '')
    def normalize_skill(sk,name=None):
        sk['rank']=int(max(0,min(10,number(sk.get('rank')))));sk['mod']=number(sk.get('mod',sk.get('bonus',0)))
        identity=name or sk.get('name','')
        default_attr=next((x['defaultAttr'] for x in CATALOG['skills'] if x['name']==identity),ATTRS[4])
        raw_attrs=sk.get('attrs');attrs=[]
        if isinstance(raw_attrs,(list,tuple)):
            for a in raw_attrs:
                if a in ATTRS and a not in attrs:attrs.append(a)
        legacy_attr=sk.get('attr')
        if not attrs and legacy_attr in ATTRS:attrs=[legacy_attr]
        # Keep untouched built-in skills sparse so their catalog default stays
        # authoritative; custom skills need an explicit fallback characteristic.
        if not attrs and name is None:attrs=[default_attr]
        if attrs:
            sk['attrs']=attrs;sk['attr']=attrs[0]
        else:
            sk.pop('attrs',None);sk['attr']=''
        sk['formulaNote']=str(sk.get('formulaNote','') or '')
    for name,sk in s['skills'].items():normalize_skill(sk,name)
    for sk in s['customSkills']:normalize_skill(sk)
    for f in s['feats']:
        e=ENTRIES.get(f.get('id'),{})
        f['rank']=int(max(1,min(e.get('ranks') or 99,number(f.get('rank'),1))))
        f['option']=max(0,min(len(e.get('abilityOptions',[]))-1,int(number(f.get('option'))))) if e.get('abilityOptions') else 0
        if not e and not f.get('custom'):raise ValueError('Неизвестный навык в файле: '+str(f.get('id')))
        f.setdefault('uid',str(uuid.uuid4()));f.setdefault('choice','');f.setdefault('note','')
    merged=[]
    for f in s['feats']:
        e=ENTRIES.get(f.get('id'),{})
        key=str(f.get('choice','')).casefold().strip() if e.get('repeatable') or f.get('custom') else ''
        old=next((x for x in merged if x['id']==f['id'] and (str(x.get('choice','')).casefold().strip() if e.get('repeatable') or f.get('custom') else '')==key),None)
        if old:
            old['rank']=max(old['rank'],f['rank'])
            s['legacyArchive'].setdefault('duplicates',[]).append(f)
            s['migrationNotes'].append('Объединены повторные покупки: '+e.get('name',f.get('name','Навык')))
        else:merged.append(f)
    s['feats']=merged
    # Base mana is represented in the magic section exactly once.
    for f in list(s['feats']):
        if ENTRIES.get(f.get('id'),{}).get('name')=='Базовый запас маны':
            s['magic']['manaRank']=max(number(s['magic']['manaRank']),f['rank']);s['feats'].remove(f)
            s['migrationNotes'].append('Базовый запас маны перенесён в раздел магии без двойной оплаты.')
    m=s['magic'];m['manaRank']=int(max(0,min(5,number(m.get('manaRank')))));m['power']=int(max(1 if m['manaRank'] else 0,min(999,number(m.get('power')))))
    unique_spells=[]
    for sp in m['spells']:
        key=str(sp.get('name','')).casefold().strip()
        if key and any(str(x.get('name','')).casefold().strip()==key for x in unique_spells):
            s['legacyArchive'].setdefault('duplicateSpells',[]).append(sp);s['migrationNotes'].append('Повторное заклинание сохранено в архиве импорта: '+sp['name']);continue
        unique_spells.append(sp)
        sp.setdefault('uid',str(uuid.uuid4()));sp['cost']=max(0,number(sp.get('cost')));sp.setdefault('learned',True)
    m['spells']=unique_spells
    for owner,key in [('resources','healthCurrent'),('resources','staminaCurrent'),('magic','currentMana')]:
        if s[owner].get(key) is not None:s[owner][key]=number(s[owner][key])
    for r in s['resources']['custom']:
        r.setdefault('id',str(uuid.uuid4()));r.setdefault('name','Ресурс');r['current']=number(r.get('current'));r['max']=max(0,number(r.get('max'),1))
    for sp in m['spells']:
        sp['name']=str(sp.get('name','Заклинание'));sp.setdefault('description',sp.get('note',''))
        if sp.get('xpOverride') is not None:sp['xpOverride']=max(0,number(sp['xpOverride']))
    for school in m['schools']:
        school['name']=str(school.get('name','Школа'));school['rank']=max(0,int(number(school.get('rank',school.get('level',0)))))
    for g in s['gear']:
        g.setdefault('uid',str(uuid.uuid4()));g.setdefault('name','Предмет');g.setdefault('qty',1);g.setdefault('load',number(g.get('strengthReq')));g.setdefault('carried',True);g.setdefault('description',g.get('note',''))
    for g in s['gear']:
        g['name']=str(g['name']);g['qty']=max(0,int(number(g['qty'],1)));g['load']=max(0,number(g.get('load')))
    for x in s['cyber']['implants']:
        x.setdefault('uid',str(uuid.uuid4()));x.setdefault('active',True);x['name']=str(x.get('name','Имплант'));x['limit']=max(0,number(x.get('limit')));x['limitBonus']=number(x.get('limitBonus'))
    for b in s['customBlocks']:b.setdefault('id',str(uuid.uuid4()));b.setdefault('name','Заметки');b.setdefault('text','')
    if not isinstance(s['ui'].get('hiddenSkills'),list):s['ui']['hiddenSkills']=[]
    s['ui']['layout']=str(s['ui'].get('layout') or '');s['ui']['geometry']=str(s['ui'].get('geometry') or '')
    generation=int(number(s['ui'].get('layoutGeneration'),0))
    if generation<3:
        # v3 changes the dock tree from rigid full-height columns to two
        # independently-resizable rows. Old QMainWindow state would otherwise
        # immediately restore the obsolete layout and hide the improvement.
        s['ui']['layout']='';s['ui']['layoutGeneration']=3
    else:s['ui']['layoutGeneration']=3
    s['ui']['fontSize']=int(max(9,min(18,number(s['ui'].get('fontSize'),11))))
    s['migrationNotes']=list(dict.fromkeys(str(x) for x in s['migrationNotes']))
    for x in s['modifiers']:
        x['target']=str(x.get('target','Здоровье'));x['value']=number(x.get('value'));x.setdefault('active',True)
    s['schema']=10
    return s

def load_file(path):
    path=Path(path)
    if path.stat().st_size>20*1024*1024:raise ValueError('Файл слишком велик для листа персонажа (больше 20 МБ).')
    return normalize(json.loads(path.read_text(encoding='utf-8-sig')))

def atomic_save(path,state):
    path=Path(path);path.parent.mkdir(parents=True,exist_ok=True)
    blob=json.dumps(state,ensure_ascii=False,indent=2,allow_nan=False).encode('utf-8')
    tmp=None
    try:
        fd,tmp=tempfile.mkstemp(prefix='.'+path.name+'.',dir=path.parent)
        with os.fdopen(fd,'wb') as f:f.write(blob);f.flush();os.fsync(f.fileno())
        if path.exists():
            backup=path.with_suffix(path.suffix+'.bak')
            # Atomic backup replacement, retaining the most recent valid state.
            data=path.read_bytes()
            try:json.loads(data)
            except (ValueError,UnicodeError):pass
            else:
                bfd,btmp=tempfile.mkstemp(prefix='.backup.',dir=path.parent)
                try:
                    with os.fdopen(bfd,'wb') as f:f.write(data);f.flush();os.fsync(f.fileno())
                    os.replace(btmp,backup)
                finally:
                    if os.path.exists(btmp):os.unlink(btmp)
        os.replace(tmp,path)
    finally:
        if tmp and os.path.exists(tmp):os.unlink(tmp)

def data_dir():
    # Keep the existing Linux location for backwards compatibility, but use
    # native per-user application-data folders on other desktop platforms.
    if os.name=='nt':
        base=Path(os.environ.get('LOCALAPPDATA',str(Path.home()/'AppData'/'Local')))
        return base/'DUBL Character Sheet'
    if sys.platform=='darwin':
        return Path.home()/'Library'/'Application Support'/'DUBL Character Sheet'
    return Path(os.environ.get('XDG_DATA_HOME',str(Path.home()/'.local/share')))/'dubl-character'
