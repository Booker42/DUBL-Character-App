from __future__ import annotations

import re

from .diagnostics import make_diagnostic
from .source_index import normalize_lookup

SKILL_HEADERS = [
    "умение",
    "описание",
    "авто 6",
    "авто 12",
    "используется нетренированным",
]
COST_HEADERS = ["значение", "стоимость", "стоимость для поднятия ранга с ноля"]


def _norm_row(row: list[str]) -> list[str]:
    return [normalize_lookup(cell) for cell in row]


def _table_matches(block: dict, headers: list[str]) -> bool:
    rows = block.get("rows", [])
    if not rows:
        return False
    normalized = _norm_row(rows[0])
    return normalized[: len(headers)] == headers


def _qualified(source_key: str, block_id: str) -> str:
    return f"{source_key}:{block_id}"


def _untrained(value: str) -> str:
    normalized = normalize_lookup(value).replace("−", "-")
    if normalized in {"да (-2)", "да(-2)"}:
        return "YES_MINUS_2"
    if normalized == "да":
        return "YES"
    if normalized == "нет":
        return "NO"
    return "UNSPECIFIED"


def _cost(value: str) -> int:
    match = re.search(r"-?\d+", value.replace(" ", ""))
    if not match:
        raise ValueError(f"invalid cumulative skill cost: {value!r}")
    return int(match.group())


def import_skills(raw_ir: dict, bindings: dict, source_key: str = "core") -> tuple[dict, list[dict]]:
    tables = [block for block in raw_ir.get("blocks", []) if block.get("kind") == "table"]
    skill_tables = [block for block in tables if _table_matches(block, SKILL_HEADERS)]
    if len(skill_tables) != 1:
        raise ValueError(f"expected exactly one base skill table, found {len(skill_tables)}")
    cost_tables = [block for block in tables if _table_matches(block, COST_HEADERS)]
    if len(cost_tables) != 1:
        raise ValueError(f"expected exactly one skill rank cost table, found {len(cost_tables)}")

    binding_items = bindings.get("bindings", [])
    binding_by_name = {normalize_lookup(item.get("name", "")): item for item in binding_items if item.get("name")}
    seen_bindings: set[str] = set()
    diagnostics: list[dict] = []
    skills: list[dict] = []
    skill_table = skill_tables[0]

    for row_index, row in enumerate(skill_table.get("rows", [])[1:], start=1):
        padded = list(row) + [""] * max(0, 5 - len(row))
        name, description, auto6, auto12, untrained = (cell.strip() for cell in padded[:5])
        if not name:
            continue
        normalized_name = normalize_lookup(name)
        binding = binding_by_name.get(normalized_name)
        if binding is None:
            diagnostics.append(make_diagnostic(
                "skill-runtime-binding-missing",
                f"skills:{name}",
                [_qualified(source_key, skill_table["id"])],
                "warning",
                f"Rulebook base skill {name!r} has no stable runtime binding yet.",
                sourceRow=row_index,
            ))
            continue
        seen_bindings.add(normalized_name)
        skills.append({
            "id": binding["id"],
            "name": name,
            "description": description,
            "auto6": auto6,
            "auto12": auto12,
            "untrained": _untrained(untrained),
            "category": binding.get("category", "CUSTOM"),
            "defaultAttributeHint": binding.get("defaultAttributeHint", "INTELLIGENCE"),
            "template": bool(binding.get("template", False)),
            "sourceRefs": [_qualified(source_key, skill_table["id"])],
            "sourceRow": row_index,
        })

    for binding in binding_items:
        name = str(binding.get("name", ""))
        normalized_name = normalize_lookup(name)
        if not normalized_name or normalized_name in seen_bindings:
            continue
        heading_candidates = [
            block for block in raw_ir.get("blocks", [])
            if block.get("kind") == "paragraph"
            and block.get("headingLevel") is not None
            and normalize_lookup(block.get("text", "")) == normalized_name
            and any(normalize_lookup(part) == "навыки" for part in block.get("headingPath", []))
        ]
        if len(heading_candidates) == 1:
            heading = heading_candidates[0]
            skills.append({
                "id": binding["id"],
                "name": name,
                "description": "",
                "auto6": "Не указано в базовой таблице",
                "auto12": "Не указано в базовой таблице",
                "untrained": "UNSPECIFIED",
                "category": binding.get("category", "CUSTOM"),
                "defaultAttributeHint": binding.get("defaultAttributeHint", "INTELLIGENCE"),
                "template": bool(binding.get("template", False)),
                "sourceKind": "supplemental-heading",
                "sourceRefs": [_qualified(source_key, heading["id"])],
            })
            seen_bindings.add(normalized_name)
            diagnostics.append(make_diagnostic(
                "skill-base-table-fields-missing",
                f"skills:{binding.get('id', name)}",
                [_qualified(source_key, heading["id"])],
                "warning",
                f"Rulebook uses {name!r} as a skill outside the canonical base skill table, so Auto 6, Auto 12, and untrained-use fields are unspecified.",
            ))
        else:
            refs = [_qualified(source_key, block["id"]) for block in heading_candidates]
            diagnostics.append(make_diagnostic(
                "skill-binding-not-in-base-table",
                f"skills:{binding.get('id', name)}",
                refs,
                "warning",
                f"Runtime skill binding {name!r} is not present in the canonical base skill table and has no unique skill heading.",
            ))

    cost_table = cost_tables[0]
    rank_costs = [0]
    for row in cost_table.get("rows", [])[1:]:
        if len(row) < 3:
            continue
        rank_costs.append(_cost(row[2]))

    payload = {
        "schemaVersion": 1,
        "source": source_key,
        "rankCosts": rank_costs,
        "rankCostSourceRefs": [_qualified(source_key, cost_table["id"])],
        "skills": skills,
    }
    return payload, diagnostics
