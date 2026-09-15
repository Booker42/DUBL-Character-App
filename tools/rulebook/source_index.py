from __future__ import annotations

import re
from collections import defaultdict


def normalize_lookup(value: str) -> str:
    value = value.replace("\u00a0", " ").replace("ё", "е").replace("Ё", "Е")
    value = value.replace("—", "-").replace("–", "-")
    value = re.sub(r"\s+", " ", value).strip().lower()
    return value


def _qualify(block_id: str, source_key: str | None) -> str:
    return f"{source_key}:{block_id}" if source_key else block_id


def build_source_index(raw_ir: dict, source_key: str | None = None) -> dict:
    headings: dict[str, list[str]] = defaultdict(list)
    text: dict[str, list[str]] = defaultdict(list)
    by_id: dict[str, dict] = {}

    for block in raw_ir.get("blocks", []):
        local_id = block["id"]
        block_id = _qualify(local_id, source_key)
        if block["kind"] == "paragraph":
            search_text = normalize_lookup(block.get("text", ""))
        else:
            search_text = " | ".join(normalize_lookup(cell) for row in block.get("rows", []) for cell in row if normalize_lookup(cell))
        by_id[block_id] = {
            "source": source_key,
            "localId": local_id,
            "kind": block["kind"],
            "headingPath": block.get("headingPath", []),
            "order": block.get("order"),
            "normalizedSearchText": search_text,
        }
        if block["kind"] == "paragraph":
            norm = normalize_lookup(block.get("text", ""))
            if norm:
                text[norm].append(block_id)
                if block.get("headingLevel") is not None:
                    headings[norm].append(block_id)
        elif block["kind"] == "table":
            for row in block.get("rows", []):
                for cell in row:
                    norm = normalize_lookup(cell)
                    if norm:
                        text[norm].append(block_id)

    return {
        "schemaVersion": 1,
        "headings": dict(sorted(headings.items())),
        "text": dict(sorted(text.items())),
        "blocks": by_id,
    }


def merge_source_indexes(indexes: dict[str, dict]) -> dict:
    headings: dict[str, list[str]] = defaultdict(list)
    text: dict[str, list[str]] = defaultdict(list)
    blocks: dict[str, dict] = {}
    for source_key in sorted(indexes):
        index = indexes[source_key]
        for norm, refs in index.get("headings", {}).items():
            headings[norm].extend(refs)
        for norm, refs in index.get("text", {}).items():
            text[norm].extend(refs)
        blocks.update(index.get("blocks", {}))
    return {
        "schemaVersion": 1,
        "sources": sorted(indexes),
        "headings": {k: v for k, v in sorted(headings.items())},
        "text": {k: v for k, v in sorted(text.items())},
        "blocks": blocks,
    }
