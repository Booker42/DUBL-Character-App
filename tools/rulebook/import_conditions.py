from __future__ import annotations

import hashlib
import re

from .source_index import normalize_lookup

MECHANIC_TITLES = {"повреждение характеристик", "высасывание характеристик"}
DERIVED_PATTERNS = [
    (re.compile(r"считается\s+беспомощн", re.I), "Беспомощный"),
    (re.compile(r"становится\s+парализованн", re.I), "Парализованный"),
    (re.compile(r"теряет\s+сознани", re.I), "Без сознания"),
    (re.compile(r"становится\s+уставш", re.I), "Уставший"),
]


def _stable_id(prefix: str, name: str) -> str:
    digest = hashlib.sha1(normalize_lookup(name).encode("utf-8")).hexdigest()[:16]
    return f"{prefix}_{digest}"


def _qualified(source_key: str, block_id: str) -> str:
    return f"{source_key}:{block_id}"


def _body_text(block: dict) -> str:
    if block.get("kind") == "paragraph":
        return block.get("text", "").strip()
    return "\n".join(" | ".join(cell.strip() for cell in row) for row in block.get("rows", []))


def import_conditions(raw_ir: dict, source_key: str = "core") -> dict:
    blocks = raw_ir.get("blocks", [])
    start = None
    stop = None
    for i, block in enumerate(blocks):
        if block.get("kind") != "paragraph":
            continue
        title = normalize_lookup(block.get("text", ""))
        if start is None and title == "состояния и эффекты":
            start = i
            continue
        if start is not None and title == "типы урона" and block.get("headingLevel") == 2:
            stop = i
            break
    if start is None:
        raise ValueError("Rulebook section 'Состояния и эффекты' not found")
    if stop is None:
        stop = len(blocks)

    conditions: list[dict] = []
    mechanics: list[dict] = []
    seen_condition_names: set[str] = set()

    i = start + 1
    while i < stop:
        heading = blocks[i]
        if heading.get("kind") != "paragraph" or heading.get("headingLevel") != 4:
            i += 1
            continue
        name = heading.get("text", "").strip()
        norm_name = normalize_lookup(name)
        body: list[dict] = []
        j = i + 1
        while j < stop:
            nxt = blocks[j]
            nxt_level = nxt.get("headingLevel") if nxt.get("kind") == "paragraph" else None
            if nxt_level is not None and nxt_level <= 4:
                break
            text = _body_text(nxt)
            if text:
                body.append(nxt)
            j += 1
        description = "\n".join(_body_text(block) for block in body if _body_text(block)).strip()
        refs = [_qualified(source_key, heading["id"]), *[_qualified(source_key, block["id"]) for block in body]]
        target = mechanics if norm_name in MECHANIC_TITLES else conditions
        prefix = "condition_mechanic" if target is mechanics else "condition"
        target.append({
            "id": _stable_id(prefix, name),
            "name": name,
            "description": description,
            "sourceKind": "heading",
            "sourceRefs": refs,
        })
        if target is conditions:
            seen_condition_names.add(normalize_lookup(name))

        for block in body:
            text = _body_text(block)
            norm_text = normalize_lookup(text)
            for pattern, derived_name in DERIVED_PATTERNS:
                if pattern.search(norm_text) and normalize_lookup(derived_name) not in seen_condition_names:
                    conditions.append({
                        "id": _stable_id("condition", derived_name),
                        "name": derived_name,
                        "description": text,
                        "sourceKind": "inline-derived",
                        "sourceRefs": [_qualified(source_key, block["id"])],
                    })
                    seen_condition_names.add(normalize_lookup(derived_name))
        i = j

    return {
        "schemaVersion": 1,
        "source": source_key,
        "conditions": conditions,
        "mechanics": mechanics,
    }
