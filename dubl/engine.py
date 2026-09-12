"""Deterministic rules. Unknown prerequisites stay unknown, never silently pass."""
from __future__ import annotations
import re,math
from dataclasses import dataclass
from .model import CATALOG,ENTRIES,ATTRS,number

def norm(s):
    return re.sub(r'\s+',' ',str(s).casefold().replace('ё','е')).strip(' .,:;')
ALIASES={'ассасин':'ассассин','боевой крик':'боевые крики','мастер ловушек':'капканщик / мастер ловушек','ул. инициатива':'улучшенная инициатива','инженерное дело':'инженерное дело (ремонт)','тело':'телосложение'}
def alias(s):return ALIASES.get(norm(s),norm(s))

def record(f):
    if f.get('custom'):return {'name':f.get('name','Навык'),'benefit':f.get('description',''),'requirements':f.get('requirements',''),'cost':number(f.get('cost')),'costType':f.get('costType','xp'),'ranks':99,'section':'Свои записи','repeatable':True}
    return ENTRIES.get(f.get('id'),{})

def rank(s,name,access=False):
    return max([int(number(f.get('rank'),1)) for f in s['feats'] if alias(record(f).get('name'))==alias(name) and (record(f).get('costType')=='ability')==access]+[0])

def attrs(s):
    a={k:number(s['attributes'].get(k)) for k in ATTRS}
    if s['options'].get('sizeModifiers',True):a['Сила']+=s['profile']['size']-5;a['Скорость']+=5-s['profile']['size']
    for k in ATTRS:a[k]+=modifier(s,k)
    return a

def modifier(s,target):return sum(number(m.get('value')) for m in s['modifiers'] if m.get('active',True) and m.get('target')==target)

def load(s):
    return sum(number(g.get('load'))*max(0,number(g.get('qty'),1)) for g in s['gear'] if g.get('carried',True)) if s['options'].get('loadAutomatic',True) else max(0,number(s['options'].get('loadManual')))

def burden(s,capacity):
    w=load(s)
    if w<=0 or (capacity>0 and w<capacity):return ('Нет нагрузки',0)
    if capacity<=0:return ('Нагрузка при нулевой вместимости',-4)
    if w<=capacity*1.5:return ('Лёгкая нагрузка',-1)
    if w<=capacity*2:return ('Средняя нагрузка',-2)
    if w<=capacity*3:return ('Тяжёлая нагрузка',-4)
    return ('Свыше таблицы нагрузки',-4)

def derived(s):
    a=attrs(s);z=s['profile']['size'];cfg=CATALOG['runTable'][str(z)]
    capacity=a['Сила']+a['Телосложение']+modifier(s,'Экипировка');bn,bp=burden(s,capacity)
    d={'Защита':10-z+a['Скорость']+a['Ловкость']+bp,
       'Здоровье':a['Телосложение']*z+a['Сила']+rank(s,'Невероятное здоровье')*(2**max(0,z-5)),
       'Рефлексы':a['Скорость']+a['Ловкость']+rank(s,'Быстрые рефлексы')+bp,
       'Инициатива':a['Скорость']+a['Восприятие']+rank(s,'Улучшенная инициатива'),
       'Стойкость':a['Телосложение']+a['Воля']+rank(s,'Стойкий'),
       'Выносливость':3+rank(s,'Выносливый'),
       'Бег':max(0,math.ceil(cfg['base']+cfg['mult']*a['Скорость'])+rank(s,'Бегун')+bp),
       'Рывок':max(0,cfg['base']),'Экипировка':capacity,
       'Природная броня':rank(s,'Прочная мускулатура'),'ОД':3,'Реакции':1}
    for k in d:
        if k!='Экипировка':d[k]+=modifier(s,k)
    d['load']=load(s);d['burden']=bn;d['loadPenalty']=bp
    return d

def mana(s):
    r=s['magic']['manaRank'];p=s['magic']['power']
    if r<=0 or p<=0:return 0
    value=CATALOG['manaTable'][str(min(p,20))][r-1]
    if p>20:value+=(p-20)*[2,7,17,40,55][r-1]
    return max(0,value+rank(s,'Увеличенный запас маны')*r+modifier(s,'Мана'))

def learn_cost(cost):
    c=number(cost,-1)
    if c<0 or c>20:return None
    return 10 if c<=3 else 20 if c<=8 else 30 if c<=12 else 40 if c<=16 else 50

def costs(s):
    d={'Характеристики':sum(CATALOG['attributeCost'][str(int(s['attributes'][a]))] for a in ATTRS),
       'Умения':sum(CATALOG['skillCost'][int(x.get('rank',0))] for x in s['skills'].values())+sum(CATALOG['skillCost'][int(x.get('rank',0))] for x in s['customSkills']),
       'Навыки':sum(number(record(f).get('cost'))*f.get('rank',1) for f in s['feats'] if record(f).get('costType','xp')!='ability'),
       'Запас маны':s['magic']['manaRank']*100,
       'Заклинания':sum(number(x.get('xpOverride'),learn_cost(x.get('cost')) or 0) if x.get('xpOverride') is not None else (learn_cost(x.get('cost')) or 0) for x in s['magic']['spells'] if x.get('learned',True)),
       'Поправка опыта':modifier(s,'Стоимость опыта')}
    d['total']=sum(d.values());return d

def ability_points(s):
    spent=0
    for f in s['feats']:
        e=record(f)
        if e.get('costType')!='ability':continue
        opts=e.get('abilityOptions',[])
        spent+=(opts[f.get('option',0)]['value'] if opts else number(e.get('cost'))*f.get('rank',1))
    budget=s['profile'].get('abilityBudget')
    if budget is None:budget=math.floor(max(0,s['profile'].get('creationXp',0))/1000)
    return spent,budget

SKILL_BONUSES={'Атлетика':'Атлетичность','Бартер':'Торгаш','Верховая езда':'Всадник','Взлом':'Медвежатник','Выживание':'Рейнджер','Дрессировка':'Укротитель зверей','Запугивание':'Устрашающий','Инженерное дело (Ремонт)':'Опытный ремонтник','Лидерство':'Прирожденный лидер','Медицина':'Лекарь','Поиск':'Сыщик','Скрытность':'Скрытный'}
def skill_rank(s,name):
    return max([number(v.get('rank')) for k,v in s['skills'].items() if alias(k)==alias(name)]+[number(v.get('rank')) for v in s['customSkills'] if alias(v.get('name'))==alias(name)]+[0])

def skills(s):
    out=[]
    for base in CATALOG['skills']:
        if base.get('template') and base['name'] not in s['skills']:continue
        x=dict(base);x.update(s['skills'].get(base['name'],{}));x['name']=base['name'];x['custom']=False;out.append(x)
    for x in s['customSkills']:out.append(dict(x,custom=True))
    known={x['name'] for x in out}
    for name,v in s['skills'].items():
        if name not in known:out.append(dict(v,name=name,custom=False,description=v.get('description','')))
    return sorted(out,key=lambda x:norm(x['name']))

def skill_attrs(sk):
    """Return the ordered, de-duplicated characteristics used by a skill.

    ``attr`` remains supported for old character files. Newer sheets persist an
    ``attrs`` list so a skill can deliberately use several characteristics.
    """
    raw=sk.get('attrs')
    values=[]
    if isinstance(raw,(list,tuple)):
        for a in raw:
            if a in ATTRS and a not in values:values.append(a)
    legacy=sk.get('attr')
    if not values and legacy in ATTRS:values=[legacy]
    default=sk.get('defaultAttr','Интеллект')
    if not values:values=[default if default in ATTRS else 'Интеллект']
    return values

def skill_bonus_breakdown(s,sk):
    """Calculate a skill and return (total, contributions, unavailable_reason).

    Each selected characteristic contributes once. Contributions are structured
    so the UI can explain the result without duplicating rules logic.
    """
    r=number(sk.get('rank'));contrib=[];av=attrs(s);v=0
    for a in skill_attrs(sk):
        value=number(av.get(a,0));v+=value;contrib.append((a,value))
    v+=r;contrib.append(('Ранг',r))
    mod=number(sk.get('mod',sk.get('bonus',0)))
    if mod:v+=mod;contrib.append(('Поправка умения',mod))
    penalty=derived(s)['loadPenalty']
    if ('Ловкость' in skill_attrs(sk) or sk['name'] in ['Стрельба','Холодное оружие','Рукопашный бой','Метание']) and penalty:
        v+=penalty;contrib.append(('Нагрузка',penalty))
    if sk['name'] in SKILL_BONUSES:
        value=rank(s,SKILL_BONUSES[sk['name']])
        if value:v+=value;contrib.append((SKILL_BONUSES[sk['name']],value))
    if sk['name'] in ['Вождение','Пилотирование'] and r>=4:
        value=rank(s,'Ас')
        if value:v+=value;contrib.append(('Ас',value))
    if sk['name'].startswith('Знание') and r>=1:
        value=min(1,rank(s,'Эрудиция'))
        if value:v+=value;contrib.append(('Эрудиция',value))
    if sk['name'].startswith('Исполнение') and r>=4:
        value=rank(s,'Талантливый исполнитель')
        if value:v+=value;contrib.append(('Талантливый исполнитель',value))
    if sk['name'].startswith('Исполнение') and r>=1 and rank(s,'Разносторонний исполнитель'):
        value=max([r]+[number(x.get('rank')) for x in skills(s) if x['name'].startswith('Исполнение')])-r
        if value:v+=value;contrib.append(('Разносторонний исполнитель',value))
    for f in s['feats']:
        if alias(f.get('choice',''))==alias(sk['name']):
            if record(f).get('name')=='Углубленное изучение':v+=2;contrib.append(('Углубленное изучение',2))
            if record(f).get('name')=='Рукодельник':v+=f['rank'];contrib.append(('Рукодельник',f['rank']))
    global_mod=modifier(s,'Умение: '+sk['name'])
    if global_mod:v+=global_mod;contrib.append(('Общая поправка',global_mod))
    if not r:
        if sk.get('untrained')=='Нет' and not (sk['name'].startswith('Знание') and rank(s,'Эрудиция')):
            return None,contrib,'Нельзя использовать без обучения'
        if '-2' in sk.get('untrained',''):
            v-=2;contrib.append(('Без обучения',-2))
    return v,contrib,''

def skill_bonus(s,sk):
    return skill_bonus_breakdown(s,sk)[0]

def skill_formula_text(s,sk):
    total,contrib,reason=skill_bonus_breakdown(s,sk)
    attrs_text=' + '.join(skill_attrs(sk))
    lines=['Характеристики: '+attrs_text]
    if reason:lines.append(reason)
    if contrib:
        parts=[]
        for name,value in contrib:
            sign='+' if value>=0 else '−'
            parts.append(f'{name} {sign}{abs(value):g}')
        if total is not None:lines.append('Расчёт: '+'; '.join(parts)+f' = {total:+g}')
        else:lines.append('Состав: '+'; '.join(parts))
    note=str(sk.get('formulaNote','') or '').strip()
    if note:lines.append('Примечание: '+note)
    return '\n'.join(lines)

@dataclass
class Check:
    state:str # ok, fail, manual
    text:str

def check_atom(s,text,entry=None,seen=None):
    t=text.strip(' .;');low=alias(t);seen=seen or set()
    if not t or t in ['-','—']:return Check('ok','Без требований')
    if re.search(r'при создании|на усмотрение|согласован',t,re.I):return Check('manual',t)
    if re.search(r'\sили\s',t,re.I):
        parts=re.split(r'\s+или\s+',t,flags=re.I)
        last=re.search(r'\s(\d+)$',parts[-1])
        if last:parts=[x+' '+last[1] if not re.search(r'\d$',x) and (alias(x) in [alias(y['name']) for y in skills(s)]+[alias(a) for a in ATTRS]) else x for x in parts]
        checks=[check_atom(s,x,entry,seen) for x in parts]
        state='ok' if any(x.state=='ok' for x in checks) else 'manual' if any(x.state=='manual' for x in checks) else 'fail'
        return Check(state,' или '.join(x.text for x in checks))
    m=re.fullmatch(r'(?:Базовый\s+)?[Зз]апас маны\s*\(?\s*(IV|III|II|I|V|\d+)\s*\)?',t,re.I)
    if m:
        need={'I':1,'II':2,'III':3,'IV':4,'V':5}.get(m[1].upper(),number(m[1]));have=s['magic']['manaRank'];return Check('ok' if have>=need else 'fail',f'Запас маны: {have:g} / {need:g}')
    m=re.fullmatch(r'(.+?)\s*:?\s*(\d+)(?:\s*ранг(?:а|ов)?)?',t,re.I)
    if m:
        name=alias(m[1]);need=int(m[2]);actual=None
        if name in [norm(a) for a in ATTRS]:actual=next(v for a,v in attrs(s).items() if norm(a)==name)
        elif name in ['сила магии','сила заклинаний']:actual=s['magic']['power']
        elif name=='стойкость':actual=derived(s)['Стойкость']
        elif name=='любая характеристика':actual=max(attrs(s).values())
        elif name=='любые две характеристики':actual=sorted(attrs(s).values(),reverse=True)[1]
        elif name=='любое умение':actual=max([0]+[number(x.get('rank')) for x in skills(s)])
        elif name.startswith('любые два умения'):actual=sorted([0,0]+[number(x.get('rank')) for x in skills(s)],reverse=True)[1]
        elif name=='любое умение ближнего боя':actual=max(skill_rank(s,'Холодное оружие'),skill_rank(s,'Рукопашный бой'))
        elif name in [alias(x['name']) for x in skills(s)]:actual=skill_rank(s,m[1])
        elif re.match(r'^знани[ея]\s*[:(]',name) and 'любое' not in name:
            spec=re.sub(r'^знани[ея]\s*[:(]\s*','',name).rstrip(') ')
            actual=skill_rank(s,'Знание ('+spec+')') # never substitute unrelated knowledge
        elif re.fullmatch(r'(Знани[ея]|Ремесло|Исполнение)(\s*\(Любое\))?',m[1],re.I):
            family=re.sub(r'\s*\(.*','',name)
            actual=max([0]+[number(x.get('rank')) for x in skills(s) if norm(x['name']).startswith(family)])
        if actual is not None:return Check('ok' if actual>=need else 'fail',f'{m[1]}: {actual:g} / {need}')
    else:name=low;need=1
    known=[e for e in CATALOG['entries'] if alias(e['name'])==name or name in [alias(x) for x in e['name'].split(' / ')]]
    nonaccess=[e for e in known if e.get('costType')!='ability']
    if nonaccess:known=nonaccess
    if known:
        if need>max(e.get('ranks') or 0 for e in known):return Check('manual',f'{t}: требование выше максимального ранга в книге')
        owned=[f for f in s['feats'] if f.get('id') in [e['id'] for e in known] and f.get('rank',1)>=need]
        if not owned:return Check('fail',f'{t}: не изучено')
        for f in owned:
            if f.get('overrideReason'):return Check('ok',t+' (решение мастера)')
            e=record(f)
            if e.get('id') in seen:return Check('manual',t+': цикл требований')
            checks=requirements(s,e,seen|{e.get('id')})
            if all(x.state=='ok' for x in checks):return Check('ok',t)
        return Check('fail' if any(x.state=='fail' for x in checks) else 'manual',t+': проверьте требования предшествующего навыка')
    m=re.match(r'^Знать\s*(\d+)\s*заклинани[яй].*дескриптором\s*[«“"]?([^»”"]+)',t,re.I)
    if m:return Check('manual',t+' — дескрипторы требуют проверки по описаниям')
    m=re.match(r'^Любые (два|три) боевых крика',t,re.I)
    if m:
        have=sum('Боевой крик' in record(f).get('tags',[]) for f in s['feats']);need=2 if m[1].lower()=='два' else 3
        return Check('ok' if have>=need else 'fail',f'Боевые крики: {have} / {need}')
    if low.startswith('заклинание:'):
        names=re.split(r'\s+и\s+',t.split(':',1)[1],flags=re.I);have={norm(x.get('name')) for x in s['magic']['spells'] if x.get('learned',True)}
        return Check('ok' if all(norm(x) in have for x in names) else 'fail',t)
    if norm(t) in {norm(x.get('name')) for x in s['magic']['spells'] if x.get('learned',True)}:return Check('ok',t)
    return Check('manual',t)

def requirements(s,e,seen=None):
    checks=[];access=e.get('accessId');seen=set(seen or ())
    if access:
        owned=[f for f in s['feats'] if f.get('id')==access]
        state='fail'
        if owned:
            if any(f.get('overrideReason') for f in owned):state='ok'
            elif access in seen:state='manual'
            else:
                previous=requirements(s,ENTRIES[access],seen|{access})
                state='fail' if any(c.state=='fail' for c in previous) else 'manual' if any(c.state=='manual' for c in previous) else 'ok'
        checks.append(Check(state,'Доступ к ветке: '+ENTRIES[access]['name']))
    if e.get('perfectRoot'):
        other=[record(f)['name'] for f in s['feats'] if record(f).get('perfectRoot') and f['id']!=e['id']]
        if other:checks.append(Check('fail','Только одна совершенная способность; уже выбрана: '+', '.join(other)))
    raw=e.get('requirements','').strip(' .,;')
    parts=re.split(r'[,;\n]+(?![^()]*\))',raw)
    # Commas separate required clauses; OR is evaluated within a clause. Elliptical
    # characteristic alternatives across commas remain explicit manual decisions.
    ambiguous=bool(re.search(r'или',raw,re.I) and any(alias(t.strip()) in [alias(a) for a in ATTRS] for t in parts[:-1]))
    for t in parts:
        if t.strip():checks.append(check_atom(s,t,e,seen))
    if ambiguous:checks.append(Check('manual','Уточните группировку «и / или»: '+raw))
    if e.get('mechanicsConflict'):checks.append(Check('manual',e['mechanicsConflict']))
    if e.get('incomplete'):checks.append(Check('manual','В исходной записи не завершены механика или реквизиты'))
    return checks or [Check('ok','Без требований')]

def cyber_limit(s):
    a=attrs(s);z=s['profile']['size'];mode=s['cyber'].get('limitMode','scale')
    base=max(z,a['Воля'],a['Телосложение'])
    if mode=='body_will':base=max(a['Воля'],a['Телосложение'])
    if mode=='slots':base=max(0,math.floor(a['Телосложение']/2))
    if rank(s,'Жертва аугментации'):base=sum(sorted([z,a['Воля'],a['Телосложение'],a['Интеллект']],reverse=True)[:2])
    installed=[x for x in s['cyber']['implants'] if x.get('active',True)]
    used=len(installed) if mode=='slots' else sum(number(x.get('limit')) for x in installed)
    tags=sum('Киберпанк' in record(f).get('tags',[]) for f in s['feats']) if rank(s,'Киберпанк') else 0
    total=base+rank(s,'Механическая адаптация')+tags+sum(number(x.get('limitBonus')) for x in installed)+modifier(s,'Лимит имплантов')
    return used,total

def warnings(s):
    out=[];spent=costs(s)['total'];points,budget=ability_points(s)
    if spent>s['profile']['xpTotal']:out.append(f'Опыт: перерасход {spent-s["profile"]["xpTotal"]:g}.')
    if points>budget:out.append(f'Очки способностей: перерасход {points-budget:g}.')
    for f in s['feats']:
        bad=[x for x in requirements(s,record(f)) if x.state!='ok']
        if bad and not f.get('overrideReason'):out.append(record(f)['name']+': '+'; '.join(x.text for x in bad))
    for sp in s['magic']['spells']:
        if sp.get('learned',True) and number(sp.get('cost'))>s['magic']['power']:out.append(sp.get('name','Заклинание')+': недостаточно силы магии.')
        if sp.get('learned',True) and learn_cost(sp.get('cost')) is None and sp.get('xpOverride') is None:out.append(sp.get('name','Заклинание')+': цена изучения вне таблицы; укажите её вручную.')
    d=derived(s)
    if d['Здоровье']<=0:out.append('Максимальное здоровье не положительно: проверьте характеристики и поправки.')
    if d['burden'].startswith(('Свыше','Нагрузка при')):out.append(d['burden']+': штрафы за пределами таблицы уточняются у мастера; применён минимум −4.')
    if s['cyber']['enabled']:
        used,total=cyber_limit(s)
        if used>total:out.append(f'Лимит имплантов превышен на {used-total:g}.')
        implants=[x for x in s['cyber']['implants'] if x.get('active',True)]
        if not any(x.get('category')=='Операционные системы' or x.get('slot')=='ОС' for x in implants):
            for x in implants:
                if re.search(r'требуется.*(?:ОС|операцион)',x.get('effect',''),re.I):out.append(x.get('name','Имплант')+': требуется операционная система.')
    return out
