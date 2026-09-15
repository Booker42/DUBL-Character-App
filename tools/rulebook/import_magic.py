from __future__ import annotations

import re
from typing import Iterable

from .diagnostics import make_diagnostic
from .source_index import normalize_lookup

_FIELD_NAMES = {
    'школа': 'school',
    'стоимость': 'manaText',
    'время сотворения': 'time',
    'дальность': 'range',
    'область': 'area',
    'действие': 'action',
    'длительность': 'duration',
    'описание': 'description',
    'усиление': 'enhancement',
}
_FIELD_RE = re.compile(
    r'^(Школа|Стоимость|Время\s+сотворения|Дальность|Область|Действие|Длительность|Описание|Усиление)\s*:\s*(.*)$',
    re.IGNORECASE,
)

_SUPPORTED_SCHOOLS = {
    'боевая магия': 'Боевая магия',
    'воплощение': 'Воплощение',
    'друид': 'Друид',
    'магия крови': 'Магия крови',
    'молитва': 'Молитва',
    'молитвы': 'Молитва',
    'некромантия': 'Некромантия',
    'ограждение': 'Ограждение',
    'ограждения': 'Ограждение',
    'призыв': 'Призыв',
    'природа': 'Природа',
    'прорицание': 'Прорицание',
    'разрушение': 'Разрушение',
    'разум': 'Разум',
    'трансмутация': 'Трансмутация',
}


def _clean(value: str) -> str:
    lines = []
    for line in str(value).replace('\r', '').split('\n'):
        line = re.sub(r'[ \t\u00a0]+', ' ', line).strip()
        if line:
            lines.append(line)
    return '\n'.join(lines)


def _first_int(value: str) -> int | None:
    match = re.search(r'-?\d+', value)
    return int(match.group(0)) if match else None


def _append(fields: dict[str, str], key: str, value: str) -> None:
    value = _clean(value)
    if not value:
        return
    fields[key] = f"{fields[key]}\n{value}" if fields.get(key) else value


def _parse_lines(lines: Iterable[str]) -> dict[str, str]:
    fields: dict[str, str] = {}
    current: str | None = None
    for raw in lines:
        for line in str(raw).replace('\r', '').split('\n'):
            line = re.sub(r'[ \t\u00a0]+', ' ', line).strip()
            if not line:
                continue
            match = _FIELD_RE.match(line)
            if match:
                label = normalize_lookup(match.group(1))
                current = _FIELD_NAMES.get(label)
                if current:
                    _append(fields, current, match.group(2))
                continue
            if current:
                _append(fields, current, line)
    return fields


def _parse_core_card(raw_ir: dict, start_index: int, source_key: str) -> dict:
    blocks = raw_ir.get('blocks', [])
    start = blocks[start_index]
    start_path = start.get('headingPath', [])
    fields: dict[str, str] = {}
    current: str | None = None
    refs = [f"{source_key}:{start['id']}"]

    for block in blocks[start_index + 1 : start_index + 120]:
        if start_path and block.get('headingPath', []) != start_path:
            break

        if block.get('kind') == 'table':
            # Spell tables (for example Confusion behaviour) are part of the
            # current field, normally Description. Preserve their textual content.
            if current:
                for row in block.get('rows', []):
                    cells = [_clean(cell) for cell in row if _clean(cell)]
                    if cells:
                        _append(fields, current, ' | '.join(cells) if len(cells) > 1 else cells[0])
                refs.append(f"{source_key}:{block['id']}")
            continue

        if block.get('kind') != 'paragraph':
            continue
        text = str(block.get('text', '')).strip()
        if not text:
            # Empty paragraphs often separate a spell description from an embedded
            # table or enhancement; headingPath is the authoritative card boundary.
            continue
        if str(block.get('style', '')).startswith('Heading') and not _FIELD_RE.match(text):
            break
        consumed = False
        for line in str(block.get('text', '')).replace('\r', '').split('\n'):
            line = re.sub(r'[ \t\u00a0]+', ' ', line).strip()
            if not line:
                continue
            match = _FIELD_RE.match(line)
            if match:
                label = normalize_lookup(match.group(1))
                current = _FIELD_NAMES.get(label)
                if current:
                    _append(fields, current, match.group(2))
                consumed = True
                continue
            if current:
                _append(fields, current, line)
                consumed = True
        if consumed:
            refs.append(f"{source_key}:{block['id']}")

    return {'fields': fields, 'sourceRefs': refs, 'order': int(start.get('order', 0)), 'path': start_path}


def _is_mechanical(card: dict) -> bool:
    fields = card['fields']
    return 'manaText' in fields and 'description' in fields


def _core_candidates(raw_ir: dict, name: str, source_key: str, paragraph_lookup: dict[str, list[int]]) -> list[dict]:
    result = []
    for index in paragraph_lookup.get(normalize_lookup(name), []):
        card = _parse_core_card(raw_ir, index, source_key)
        if _is_mechanical(card):
            result.append(card)
    return result


def _card_signature(fields: dict[str, str]) -> tuple:
    keys = ('school','manaText','time','range','area','action','duration','description','enhancement')
    return tuple(normalize_lookup(fields.get(key, '')) for key in keys)


def _preferred_core_card(cards: list[dict]) -> tuple[dict | None, list[dict]]:
    if not cards:
        return None, []
    primary = [c for c in cards if 'описание заклинаний' in [normalize_lookup(x) for x in c.get('path', [])]]
    pool = primary or cards
    pool = sorted(pool, key=lambda c: c['order'])
    chosen = pool[0]
    variants = [c for c in cards if _card_signature(c['fields']) != _card_signature(chosen['fields'])]
    return chosen, variants


def _canonical_school_text(raw: str) -> str:
    parts = []
    for item in re.split(r'\s*/\s*', _clean(raw)):
        if not item:
            continue
        canonical = _SUPPORTED_SCHOOLS.get(normalize_lookup(item), item.strip())
        if canonical not in parts:
            parts.append(canonical)
    return '/'.join(parts)


def _spell_from_fields(binding: dict, fields: dict[str, str], refs: list[str], *, source: str | None = None, conflict_note: str = '') -> dict:
    mana_text = _clean(fields.get('manaText', ''))
    cost = _first_int(mana_text)
    if cost is None:
        cost = 0
    school = _canonical_school_text(fields.get('school', ''))
    item = {
        'id': str(binding['id']),
        'name': str(binding['name']),
        'section': 'Заклинания',
        'category': school,
        'cost': cost,
        'school': school,
        'manaText': mana_text or str(cost),
        'time': _clean(fields.get('time', '')),
        'range': _clean(fields.get('range', '')),
        'area': _clean(fields.get('area', '')),
        'action': _clean(fields.get('action', '')),
        'duration': _clean(fields.get('duration', '')),
        'description': _clean(fields.get('description', '')),
        'enhancement': _clean(fields.get('enhancement', '')),
        'sourceRefs': refs,
    }
    if source:
        item['source'] = source
    if conflict_note:
        item['conflictNote'] = conflict_note
    return item


def import_core_spells(raw_ir: dict, bindings: dict, source_key: str = 'core') -> tuple[dict, list[dict]]:
    spells: list[dict] = []
    diagnostics: list[dict] = []
    paragraph_lookup: dict[str, list[int]] = {}
    for index, block in enumerate(raw_ir.get('blocks', [])):
        if block.get('kind') == 'paragraph':
            paragraph_lookup.setdefault(normalize_lookup(str(block.get('text', '')).strip()), []).append(index)
    for binding in bindings.get('bindings', []):
        name = str(binding.get('name', '')).strip()
        candidates = _core_candidates(raw_ir, name, source_key, paragraph_lookup)
        chosen, variants = _preferred_core_card(candidates)
        if chosen is None:
            diagnostics.append(make_diagnostic(
                'spell-source-missing', f"spell:{binding.get('id', name)}", [], 'error',
                f"No mechanical spell card found for {name}", sourcePolicy=[source_key],
            ))
            continue
        conflict_note = ''
        refs = list(chosen['sourceRefs'])
        if variants:
            variant_refs = [ref for card in variants for ref in card['sourceRefs'][:1]]
            refs.extend(variant_refs)
            conflict_note = 'В книге есть расходящиеся повторные описания этого заклинания; используется карточка из основного описательного раздела, если она существует.'
            diagnostics.append(make_diagnostic(
                'spell-source-conflict', f"spell:{binding.get('id', name)}", refs, 'warning',
                f"Rulebook contains differing spell cards for {name}", sourcePolicy=[source_key],
            ))
        spells.append(_spell_from_fields(binding, chosen['fields'], refs, conflict_note=conflict_note))
    return {'schemaVersion': 1, 'source': source_key, 'spells': spells}, diagnostics


def _archmage_heading_name(text: str) -> str:
    return re.sub(r'^\s*\d+\.\s*', '', str(text)).strip()


def _parse_archmage_card(raw_ir: dict, index: int, source_key: str) -> dict | None:
    blocks = raw_ir.get('blocks', [])
    start = blocks[index]
    lines: list[str] = []
    refs = [f"{source_key}:{start['id']}"]
    # Old source has complete inline definitions in the next paragraph. Allow a few
    # same-section paragraphs so the Raw-IR importer is resilient to paragraph splits.
    for block in blocks[index + 1 : index + 6]:
        if block.get('kind') != 'paragraph':
            continue
        text = str(block.get('text', '')).strip()
        if not text:
            if lines:
                break
            continue
        if lines and str(block.get('style', '')).startswith('Heading'):
            break
        lines.append(text)
        refs.append(f"{source_key}:{block['id']}")
        joined = '\n'.join(lines)
        if 'Описание:' in joined and ('Школа:' in joined or 'Источник:' in joined) and 'Стоимость:' in joined:
            # Complete inline records should not absorb the next prose block.
            fields = _parse_lines(lines)
            if fields.get('description'):
                return {'fields': fields, 'sourceRefs': refs, 'order': int(start.get('order', 0))}
    fields = _parse_lines(lines)
    if fields.get('description') and fields.get('manaText'):
        return {'fields': fields, 'sourceRefs': refs, 'order': int(start.get('order', 0))}
    return None


def import_archmage_spells(raw_ir: dict, bindings: dict, source_key: str = 'archmage') -> tuple[dict, list[dict]]:
    by_name: dict[str, list[dict]] = {}
    for index, block in enumerate(raw_ir.get('blocks', [])):
        if block.get('kind') != 'paragraph':
            continue
        style = str(block.get('style', ''))
        if style not in {'Heading 3', 'Heading 5'}:
            continue
        name = _archmage_heading_name(str(block.get('text', '')))
        if not name:
            continue
        card = _parse_archmage_card(raw_ir, index, source_key)
        if card:
            by_name.setdefault(normalize_lookup(name), []).append(card)

    spells: list[dict] = []
    diagnostics: list[dict] = []
    for binding in bindings.get('bindings', []):
        name = str(binding.get('name', '')).strip()
        cards = by_name.get(normalize_lookup(name), [])
        if not cards:
            diagnostics.append(make_diagnostic(
                'archmage-spell-source-missing', f"spell:{binding.get('id', name)}", [], 'error',
                f"No Archmage definition found for {name}", sourcePolicy=[source_key],
            ))
            continue
        chosen = sorted(cards, key=lambda c: c['order'])[0]
        variants = [c for c in cards[1:] if _card_signature(c['fields']) != _card_signature(chosen['fields'])]
        conflict_note = ''
        refs = list(chosen['sourceRefs'])
        if variants:
            refs.extend(ref for c in variants for ref in c['sourceRefs'][:1])
            conflict_note = 'В Книге Архимага есть расходящиеся повторные определения.'
            diagnostics.append(make_diagnostic(
                'archmage-spell-source-conflict', f"spell:{binding.get('id', name)}", refs, 'warning',
                f"Archmage contains differing definitions for {name}", sourcePolicy=[source_key],
            ))
        spells.append(_spell_from_fields(binding, chosen['fields'], refs, source='Книга Архимага', conflict_note=conflict_note))
    return {'schemaVersion': 1, 'source': source_key, 'spells': spells}, diagnostics


def promote_spell_catalog(source_payload: dict, version: str = '3.69') -> dict:
    promoted: list[dict] = []
    for source in source_payload.get('spells', []):
        missing: list[str] = []
        if not str(source.get('description', '')).strip():
            missing.append('описание')
        if not str(source.get('manaText', '')).strip():
            missing.append('стоимость')
        note_parts = []
        existing_note = str(source.get('conflictNote', '')).strip()
        if existing_note:
            note_parts.append(existing_note)
        if missing:
            note_parts.append('В источнике не определены обязательные поля: ' + ', '.join(missing) + '.')
        promoted.append({
            'id': source['id'],
            'name': source['name'],
            'section': 'Заклинания',
            'category': source.get('school', ''),
            'cost': int(source.get('cost', 0)),
            'school': source.get('school', ''),
            'manaText': source.get('manaText', ''),
            'time': source.get('time', ''),
            'range': source.get('range', ''),
            'area': source.get('area', ''),
            'action': source.get('action', ''),
            'duration': source.get('duration', ''),
            'description': source.get('description', ''),
            'enhancement': source.get('enhancement', ''),
            'incomplete': bool(missing or existing_note),
            'conflictNote': '\n'.join(note_parts),
            **({'source': source['source']} if source.get('source') else {}),
        })
    return {'version': version, 'spells': promoted}


def compare_spells_runtime(candidate: dict, runtime: dict) -> list[dict]:
    diagnostics: list[dict] = []
    runtime_by_id = {item.get('id'): item for item in runtime.get('spells', [])}
    fields = ('name','school','cost','manaText','time','range','area','action','duration','description','enhancement')
    for source in candidate.get('spells', []):
        current = runtime_by_id.get(source.get('id'))
        if current is None:
            diagnostics.append(make_diagnostic(
                'spell-runtime-missing', f"spell:{source.get('id')}", source.get('sourceRefs', []), 'warning',
                f"Runtime spell missing: {source.get('name')}",
            ))
            continue
        drift = []
        for field in fields:
            if field == 'school':
                left = tuple(sorted(_canonical_school_text(str(source.get(field, ''))).split('/')))
                right = tuple(sorted(_canonical_school_text(str(current.get(field, ''))).split('/')))
                if left != right:
                    drift.append(field)
                continue
            left = normalize_lookup(str(source.get(field, '')))
            right = normalize_lookup(str(current.get(field, '')))
            if left != right:
                drift.append(field)
        if drift:
            diagnostics.append(make_diagnostic(
                'spell-runtime-drift', f"spell:{source.get('id')}", source.get('sourceRefs', []), 'warning',
                f"Runtime differs from rulebook for {source.get('name')}: {', '.join(drift)}",
                fields=drift,
            ))
    return diagnostics
