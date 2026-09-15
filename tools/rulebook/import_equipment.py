from __future__ import annotations

import re

from .diagnostics import make_diagnostic
from .source_index import normalize_lookup


def _clean(value: str) -> str:
    return re.sub(r'[ \t\u00a0]+', ' ', str(value).replace('\r', ' ').replace('\n', ' ')).strip()


def _table_map(raw_ir: dict) -> tuple[list[dict], dict[str, dict]]:
    tables = [block for block in raw_ir.get('blocks', []) if block.get('kind') == 'table']
    return tables, {block['id']: block for block in tables}


def _header_and_rows(table: dict) -> tuple[list[str], list[list[str]], int]:
    rows = table.get('rows', [])
    if not rows:
        return [], [], 0
    first = [_clean(cell) for cell in rows[0]]
    first_nonempty = [cell for cell in first if cell]
    repeated_category = len(first_nonempty) > 1 and len({normalize_lookup(cell) for cell in first_nonempty}) == 1
    if repeated_category and len(rows) > 1:
        return [_clean(cell) for cell in rows[1]], rows[2:], 2
    return first, rows[1:], 1


def _row_fields(headers: list[str], row: list[str]) -> dict[str, str]:
    fields: dict[str, str] = {}
    for index, header in enumerate(headers[1:], start=1):
        key = _clean(header)
        if not key:
            continue
        value = _clean(row[index] if index < len(row) else '')
        if value:
            fields[key] = value
    return fields


def _description(fields: dict[str, str]) -> str:
    return '\n'.join(f'{key}: {value}' for key, value in fields.items())


def import_gear(raw_ir: dict, bindings: dict, source_key: str = 'core') -> tuple[dict, list[dict]]:
    tables, by_id = _table_map(raw_ir)
    gear: list[dict] = []
    diagnostics: list[dict] = []
    for binding in bindings.get('bindings', []):
        source_ref = str(binding.get('sourceRef', '')).strip()
        table: dict | None = None
        if source_ref:
            ref_id = source_ref.split(':', 1)[-1]
            table = by_id.get(ref_id)
        if table is None:
            source_table = binding.get('sourceTable')
            if isinstance(source_table, int):
                if 1 <= source_table <= len(tables):
                    # Tracked real bindings use the original DOCX's one-based table number.
                    table = tables[source_table - 1]
                else:
                    # Synthetic fixtures and future explicit bindings may use Raw-IR block order.
                    table = next((candidate for candidate in tables if int(candidate.get('order', -1)) == source_table), None)
        if table is None:
            diagnostics.append(make_diagnostic(
                'gear-source-table-missing', f"gear:{binding.get('id', binding.get('name'))}", [], 'error',
                f"Source table not found for {binding.get('name')}", sourcePolicy=[source_key],
            ))
            continue
        headers, rows, row_base = _header_and_rows(table)
        name = str(binding.get('name', '')).strip()
        candidates: list[tuple[int, list[str]]] = [
            (idx, row) for idx, row in enumerate(rows)
            if row and normalize_lookup(_clean(row[0])) == normalize_lookup(name)
        ]
        occurrence = int(binding.get('rowOccurrence', 0) or 0)
        if occurrence < 0 or occurrence >= len(candidates):
            diagnostics.append(make_diagnostic(
                'gear-source-row-missing', f"gear:{binding.get('id', name)}", [f"{source_key}:{table['id']}"], 'error',
                f"Row {occurrence} for {name} not found in source table", sourcePolicy=[source_key],
            ))
            continue
        _, row = candidates[occurrence]
        fields = _row_fields(headers, row)
        item = {
            'id': str(binding['id']),
            'name': name,
            'category': str(binding.get('category', 'Снаряжение')),
            'section': str(binding.get('section', 'Предметы')),
            'fields': fields,
            'description': _description(fields),
            'sourceRefs': [f"{source_key}:{table['id']}"],
        }
        gear.append(item)
    return {'schemaVersion': 1, 'source': source_key, 'gear': gear}, diagnostics


def promote_gear_catalog(source_payload: dict, version: str = '3.69') -> dict:
    return {
        'version': version,
        'gear': [{
            'id': item['id'], 'name': item['name'], 'category': item.get('category', 'Снаряжение'),
            'section': item.get('section', 'Предметы'), 'fields': dict(item.get('fields', {})),
            'description': item.get('description', ''),
        } for item in source_payload.get('gear', [])],
    }


def compare_gear_runtime(candidate: dict, runtime: dict) -> list[dict]:
    diagnostics: list[dict] = []
    by_id = {item.get('id'): item for item in runtime.get('gear', [])}
    for source in candidate.get('gear', []):
        current = by_id.get(source.get('id'))
        if current is None:
            diagnostics.append(make_diagnostic(
                'gear-runtime-missing', f"gear:{source.get('id')}", source.get('sourceRefs', []), 'warning',
                f"Runtime gear missing: {source.get('name')}",
            ))
            continue
        drift = []
        if normalize_lookup(str(source.get('name', ''))) != normalize_lookup(str(current.get('name', ''))):
            drift.append('name')
        def semantic_fields(value: dict) -> dict[str, str]:
            return {normalize_lookup(str(k)): normalize_lookup(str(v)) for k, v in value.items()}
        if semantic_fields(source.get('fields', {})) != semantic_fields(current.get('fields', {})):
            drift.append('fields')
        if normalize_lookup(source.get('description', '')) != normalize_lookup(current.get('description', '')):
            drift.append('description')
        if drift:
            diagnostics.append(make_diagnostic(
                'gear-runtime-drift', f"gear:{source.get('id')}", source.get('sourceRefs', []), 'warning',
                f"Runtime differs from rulebook for {source.get('name')}: {', '.join(drift)}", fields=drift,
            ))
    return diagnostics
