from __future__ import annotations

import hashlib
import re

from .diagnostics import make_diagnostic


def clean(value: object) -> str:
    return re.sub(r"\s+", " ", str(value or "").replace("\u00a0", " ")).strip()


def normalize(value: object) -> str:
    return clean(value).casefold().replace("ё", "е").replace("—", "-").replace("–", "-").strip(" .,:;!?")


def chi_id(kind: str, name: str) -> str:
    digest=hashlib.sha1(f"chi:{kind}:{normalize(name)}".encode("utf-8")).hexdigest()[:16]
    return f"chi_{kind}_{digest}"


def short_school_name(title: str) -> str:
    title=clean(re.sub(r"^\d+\.\s*", "", title))
    return clean(re.sub(r"\s*\([^)]*\)\s*$", "", title))


def _ref(source: str, block: dict) -> str:
    return f"{source}:{block.get('id')}"


def ability_entry(name: str, cost: int, requirements: str, benefit: str, category: str, refs: list[str]) -> dict:
    return {"id":chi_id("development",name),"name":clean(name),"section":"ЦИ","category":category,
            "cost":0,"costType":"ability","ranks":1,"requirements":clean(requirements) or "-","benefit":clean(benefit),
            "tags":["ЦИ"],"abilityOptions":[{"source":"ЦИ","value":max(0,int(cost))}],"sourceRefs":list(dict.fromkeys(refs))}


def xp_entry(name: str, cost: int, ranks: int, requirements: str, benefit: str, category: str, refs: list[str]) -> dict:
    return {"id":chi_id("development",name),"name":clean(name),"section":"ЦИ","category":category,"cost":max(0,int(cost)),
            "costType":"xp","ranks":max(1,int(ranks)),"requirements":clean(requirements) or "-","benefit":clean(benefit),
            "tags":["ЦИ"],"sourceRefs":list(dict.fromkeys(refs))}


def technique_entry(name: str, cost: int, action: str, effect: str, requirements: str, school: str, refs: list[str]) -> dict:
    return {"id":chi_id("technique",name),"name":clean(name),"school":clean(school),"chiCost":max(0,int(cost)),
            "action":clean(action),"effect":clean(effect),"requirements":clean(requirements) or "Внутренняя Ци",
            "sourceRefs":list(dict.fromkeys(refs))}


MASTER_ABILITY_NAMES={"Быстрое восстановление Ци","Ци и оружие","Ци мечника","Слияние с природой","Медитация на ходу"}
NEW_ABILITY_NAMES={"Пробуждённая Ци","Ци тела","Ци разума","Дыхание жизни","Драконья броня"}


def _inline_ability(text: str, refs: list[str], category: str, default_requirement: str="Внутренняя Ци") -> dict|None:
    m=re.match(r"^(.+?)\s*\(стоимость\s*(\d+)(?:\s*очк\w*)?\s*,?\s*(?:требуется\s*)?([^)]*)\)\s*(.*)$",clean(text),re.I)
    if not m: return None
    name,cost,req_blob,benefit=m.groups(); req=clean(req_blob)
    if req:
        req=re.sub(r"^требуется\s+","",req,flags=re.I)
        requirements=f"{default_requirement}, {req}" if default_requirement and normalize(default_requirement) not in normalize(req) else req
    else: requirements=default_requirement
    if clean(name)=="Пробуждённая Ци": requirements="Мастер Ци, Воля 5"
    return ability_entry(name,int(cost),requirements,benefit,category,refs)


def _named_technique(text: str, requirements: str, school: str, refs: list[str]) -> dict|None:
    m=re.match(r"^(.+?)\s*\((\d+)\s*Ци\s*,\s*([^)]+)\)\s*(.*)$",clean(text),re.I)
    if not m: return None
    name,cost,action,effect=m.groups()
    return technique_entry(name,int(cost),action,effect,requirements,school,refs)


def _unique(items: list[dict]) -> list[dict]:
    seen=set(); out=[]
    for item in items:
        if item['id'] in seen: continue
        seen.add(item['id']); out.append(item)
    return out


def import_chi(raw_ir: dict, source_key: str="melee") -> tuple[dict,list[dict]]:
    all_blocks=raw_ir.get('blocks',[]); blocks=[]; inside=False
    for block in all_blocks:
        if block.get('kind')=='paragraph':
            text=clean(block.get('text'))
            if text=='Ци' and block.get('style')=='Title': inside=True
            elif inside and text=='Оружейное кунг-фу' and block.get('style')=='Title': break
        if inside: blocks.append(block)
    if not blocks:
        return {"schemaVersion":1,"source":source_key,"developments":[],"schools":[],"techniques":[]}, [
            make_diagnostic('chi-source-section-missing','chi',[], 'error','Chi section not found in melee source.',sourcePolicy=[source_key])]

    developments=[]; schools=[]; techniques=[]; diagnostics=[]
    mode=''; i=0
    while i<len(blocks):
        block=blocks[i]
        if block.get('kind')=='paragraph':
            text=clean(block.get('text')); style=str(block.get('style') or '')
            if text.startswith('Базовые приёмы Ци'): mode='basic-techniques'
            elif text.startswith('Продвинутые приёмы Ци'): mode='advanced-techniques'
            elif text=='Развитие Ци': mode='core-development'
            elif text=='Основы Ци': mode='chi-basics'
            elif text.startswith('Мастерские способности Ци'): mode='master-abilities'
            elif text.startswith('Новые способности'): mode='new-abilities'
            elif style.startswith('Heading 2') or style.startswith('Heading 1'):
                if 'Школа ' in text: mode='school'
                elif text.startswith('Расширенный список'): mode='expanded'
                elif text.startswith('Комбинированные техники'): mode='combined'
                else: mode=''
            elif mode in {'basic-techniques','advanced-techniques'}:
                req='Внутренняя Ци' if mode=='basic-techniques' else 'Мастер Ци'
                school='Базовые приёмы' if mode=='basic-techniques' else 'Продвинутые приёмы'
                item=_named_technique(text,req,school,[_ref(source_key,block)])
                if item: techniques.append(item)
            elif mode=='core-development':
                dash='—' if '—' in str(block.get('text','')) else '-'
                raw_text=str(block.get('text',''))
                benefit=clean(raw_text.split(dash,1)[-1] if dash in raw_text else raw_text)
                if text.startswith('Внутренняя Ци'):
                    cost=int(re.search(r"стоимость\s*(\d+)",text,re.I).group(1)) if re.search(r"стоимость\s*(\d+)",text,re.I) else 1
                    developments.append(ability_entry('Внутренняя Ци',cost,'-',benefit,'Развитие ЦИ',[_ref(source_key,block)]))
                elif text.startswith('Мастер Ци'):
                    cost=int(re.search(r"стоимость\s*(\d+)",text,re.I).group(1)) if re.search(r"стоимость\s*(\d+)",text,re.I) else 2
                    developments.append(ability_entry('Мастер Ци',cost,'Внутренняя Ци, Воля 5',benefit,'Развитие ЦИ',[_ref(source_key,block)]))
                elif text.startswith('Восстановление Ци'):
                    cm=re.search(r"стоимость\s*(\d+)\s*опыта",text,re.I); rm=re.search(r"макс\.\s*(\d+)",text,re.I)
                    developments.append(xp_entry('Восстановление Ци',int(cm.group(1)) if cm else 30,int(rm.group(1)) if rm else 3,
                                                 'Внутренняя Ци',benefit,'Развитие ЦИ',[_ref(source_key,block)]))
            elif mode=='chi-basics' and text and not re.match(r"^(Стоимость|Ранги|Дальность|Длительность|Эффект):",text):
                name=text; fields={}; refs=[_ref(source_key,block)]; j=i+1
                while j<len(blocks) and blocks[j].get('kind')=='paragraph':
                    nxt=clean(blocks[j].get('text'))
                    if not nxt: j+=1; continue
                    if str(blocks[j].get('style') or '').startswith('Heading') or not re.match(r"^(Стоимость|Ранги|Дальность|Длительность|Эффект):",nxt): break
                    key,value=nxt.split(':',1); fields[clean(key)]=clean(value); refs.append(_ref(source_key,blocks[j])); j+=1
                if 'Стоимость' in fields and 'Эффект' in fields:
                    developments.append(xp_entry(name,int(fields['Стоимость']),int(fields.get('Ранги','1')),'Внутренняя Ци',fields['Эффект'],'Основы ЦИ',refs))
                    techniques.append(technique_entry(name,1,fields.get('Длительность','Активация'),fields['Эффект'],name,'Основы ЦИ',refs))
                    i=j-1
            elif mode in {'master-abilities','new-abilities'}:
                expected=MASTER_ABILITY_NAMES if mode=='master-abilities' else NEW_ABILITY_NAMES
                if any(text.startswith(name) for name in expected):
                    parsed=_inline_ability(text,[_ref(source_key,block)],'Мастерство ЦИ' if mode=='master-abilities' else 'Развитие ЦИ')
                    if parsed: developments.append(parsed)
        i+=1

    current_school=None; school_req=''; passives=[]; passive_refs=[]; collecting=False; table_mode=''; school_refs=[]
    for block in blocks:
        if block.get('kind')=='paragraph':
            text=clean(block.get('text')); style=str(block.get('style') or '')
            if 'Школа ' in text and (style.startswith('Heading 1') or style.startswith('Heading 2')):
                current_school=short_school_name(text); school_req=''; passives=[]; passive_refs=[]; collecting=False; table_mode='school'; school_refs=[_ref(source_key,block)]
            elif text.startswith('Расширенный список приёмов Ци'):
                current_school=None; table_mode='expanded'
            elif text.startswith('Комбинированные техники'):
                current_school=None; table_mode='combined'
            elif current_school and text.startswith('Требования:'):
                school_req=clean(text.split(':',1)[1]).strip('.'); school_refs.append(_ref(source_key,block))
            elif current_school and text.startswith('Пассивные преимущества школы'):
                collecting=True; school_refs.append(_ref(source_key,block))
            elif current_school and text.startswith('Уникальные приёмы'):
                collecting=False; school_refs.append(_ref(source_key,block))
                schools.append({'id':chi_id('school',current_school),'name':current_school,'requirements':school_req,'passives':list(passives),'sourceRefs':list(dict.fromkeys(school_refs+passive_refs))})
                developments.append(ability_entry(current_school,1,f"Внутренняя Ци, {school_req}" if school_req else 'Внутренняя Ци',
                                                  'Открывает пассивные преимущества школы и её уникальные приёмы. '+' '.join(passives),'Школы ЦИ',list(dict.fromkeys(school_refs+passive_refs))))
            elif current_school and collecting and text:
                passives.append(text); passive_refs.append(_ref(source_key,block))
        elif block.get('kind')=='table':
            rows=block.get('rows',[])
            if not rows: continue
            headers=[normalize(x) for x in rows[0]]
            ref=[_ref(source_key,block)]
            if table_mode=='school' and current_school and 'стоимость ци' in headers:
                for row in rows[1:]:
                    cells=[clean(x) for x in row]
                    if len(cells)<4 or not cells[0]: continue
                    m=re.search(r"\d+",cells[1]); cost=int(m.group()) if m else 0
                    techniques.append(technique_entry(cells[0],cost,cells[2],cells[3],current_school,current_school,ref))
            elif table_mode=='expanded' and 'стоимость ци' in headers:
                for row in rows[1:]:
                    cells=[clean(x) for x in row]
                    if len(cells)<5 or not cells[0]: continue
                    m=re.search(r"\d+",cells[1]); cost=int(m.group()) if m else 0
                    req=cells[2]; req='Внутренняя Ци' if normalize(req) in {'','-'} else f"Внутренняя Ци, {req}"
                    techniques.append(technique_entry(cells[0],cost,cells[3],cells[4],req,'Общие приёмы',ref))
            elif table_mode=='combined' and len(rows[0])>=2:
                for row in rows[1:]:
                    cells=[clean(x) for x in row]
                    if len(cells)<2 or not cells[0]: continue
                    techniques.append(technique_entry(cells[0],3,'2 ОД',cells[1],cells[0],'Комбинированные техники',ref))

    developments=_unique(developments); schools=_unique(schools); techniques=_unique(techniques)
    payload={'schemaVersion':1,'source':source_key,'developments':developments,'schools':schools,'techniques':techniques}
    return payload, diagnostics


def _norm(value: object) -> str:
    return re.sub(r"\s+"," ",str(value or '')).strip()


def _strip_refs(item: dict) -> dict:
    return {k:v for k,v in item.items() if k not in {'sourceRefs'}}


def compare_chi_runtime(imported: dict, development_root: dict, chi_root: dict) -> list[dict]:
    diags=[]
    dev={str(x.get('id','')):x for x in development_root.get('entries',[]) if x.get('section')=='ЦИ'}
    for src in imported.get('developments',[]):
        cur=dev.get(src['id']);
        if not cur: continue
        diffs={}
        for f in ('cost','ranks'):
            if int(src.get(f) or 0)!=int(cur.get(f) or 0): diffs[f]={'rulebook':src.get(f),'runtime':cur.get(f)}
        if src.get('costType')!=cur.get('costType'): diffs['costType']={'rulebook':src.get('costType'),'runtime':cur.get('costType')}
        for f in ('requirements','benefit'):
            if _norm(src.get(f))!=_norm(cur.get(f)): diffs[f]={'rulebook':src.get(f) or '', 'runtime':cur.get(f) or ''}
        if src.get('costType')=='ability' and src.get('abilityOptions',[])!=cur.get('abilityOptions',[]): diffs['abilityOptions']={'rulebook':src.get('abilityOptions',[]),'runtime':cur.get('abilityOptions',[])}
        if diffs: diags.append(make_diagnostic('chi-development-runtime-drift',f"chi-development:{src['id']}",src.get('sourceRefs',[]),'warning',f"Runtime Chi development {src['name']!r} differs from source.",details={'differences':diffs}))
    schools={str(x.get('id','')):x for x in chi_root.get('schools',[])}
    for src in imported.get('schools',[]):
        cur=schools.get(src['id']);
        if not cur: continue
        diffs={}
        for f in ('name','requirements','passives'):
            a=src.get(f); b=cur.get(f)
            if f=='passives':
                if [_norm(x) for x in a or []]!=[_norm(x) for x in b or []]: diffs[f]={'rulebook':a or [],'runtime':b or []}
            elif _norm(a)!=_norm(b): diffs[f]={'rulebook':a or '', 'runtime':b or ''}
        if diffs: diags.append(make_diagnostic('chi-school-runtime-drift',f"chi-school:{src['id']}",src.get('sourceRefs',[]),'warning',f"Runtime Chi school {src['name']!r} differs from source.",details={'differences':diffs}))
    tech={str(x.get('id','')):x for x in chi_root.get('techniques',[])}
    for src in imported.get('techniques',[]):
        cur=tech.get(src['id']);
        if not cur: continue
        diffs={}
        if int(src.get('chiCost') or 0)!=int(cur.get('chiCost') or 0): diffs['chiCost']={'rulebook':src.get('chiCost'),'runtime':cur.get('chiCost')}
        for f in ('school','action','effect','requirements'):
            if _norm(src.get(f))!=_norm(cur.get(f)): diffs[f]={'rulebook':src.get(f) or '', 'runtime':cur.get(f) or ''}
        if diffs: diags.append(make_diagnostic('chi-technique-runtime-drift',f"chi-technique:{src['id']}",src.get('sourceRefs',[]),'warning',f"Runtime Chi technique {src['name']!r} differs from source.",details={'differences':diffs}))
    return diags


def promote_chi_development(imported: dict, version: str='3.69') -> dict:
    return {'version':version,'entries':[dict(x) for x in imported.get('developments',[])]}


def promote_chi_catalog(imported: dict, version: str='3.69') -> dict:
    return {'version':version,'schools':[dict(x) for x in imported.get('schools',[])],'techniques':[dict(x) for x in imported.get('techniques',[])]}
