from __future__ import annotations

import hashlib
import re
from collections import defaultdict

from .diagnostics import make_diagnostic
from .source_index import normalize_lookup

DRAFT_PATTERNS = [
    re.compile(r"\?\?\?"),
    re.compile(r"позже\s+напиш", re.IGNORECASE),
    re.compile(r"чернов(?:ое|ая|ой)", re.IGNORECASE),
]


def _block_text(block: dict) -> str:
    if block.get("kind") == "paragraph":
        return block.get("text", "")
    return "\n".join(" | ".join(row) for row in block.get("rows", []))


def _heading_bodies(raw: dict) -> list[tuple[dict, str]]:
    blocks = raw.get("blocks", [])
    mask = (1 << 64) - 1
    base = 1_000_003

    prefix_hash = [0]
    prefix_count = [0]
    powers = [1]
    for block in blocks:
        text = normalize_lookup(_block_text(block))
        previous_hash = prefix_hash[-1]
        previous_count = prefix_count[-1]
        if text:
            token = int.from_bytes(hashlib.sha1(text.encode("utf-8")).digest()[:8], "big")
            prefix_hash.append(((previous_hash * base) + token) & mask)
            prefix_count.append(previous_count + 1)
            powers.append((powers[-1] * base) & mask)
        else:
            prefix_hash.append(previous_hash)
            prefix_count.append(previous_count)

    boundary_by_index: dict[int, int] = {}
    next_heading_by_level: dict[int, int] = {}
    for index in range(len(blocks) - 1, -1, -1):
        block = blocks[index]
        level = block.get("headingLevel") if block.get("kind") == "paragraph" else None
        if level is None:
            continue
        candidates = [position for candidate_level, position in next_heading_by_level.items() if candidate_level <= level]
        boundary_by_index[index] = min(candidates) if candidates else len(blocks)
        next_heading_by_level[int(level)] = index

    out: list[tuple[dict, str]] = []
    for index, block in enumerate(blocks):
        boundary = boundary_by_index.get(index)
        if boundary is None:
            continue
        left = index + 1
        right = boundary
        token_count = prefix_count[right] - prefix_count[left]
        while len(powers) <= token_count:
            powers.append((powers[-1] * base) & mask)
        range_hash = (prefix_hash[right] - (prefix_hash[left] * powers[token_count])) & mask
        out.append((block, f"{token_count}:{range_hash:016x}"))
    return out


def detect_source_diagnostics(raw_by_source: dict[str, dict]) -> list[dict]:
    diagnostics: list[dict] = []
    for source_key in sorted(raw_by_source):
        raw = raw_by_source[source_key]
        for block in raw.get("blocks", []):
            text = _block_text(block)
            if text and any(pattern.search(text) for pattern in DRAFT_PATTERNS):
                ref = f"{source_key}:{block['id']}"
                diagnostics.append(make_diagnostic(
                    "draft-marker",
                    f"{source_key}:{block['id']}",
                    [ref],
                    "warning",
                    "Rulebook source contains an explicit draft/incomplete marker; do not infer executable behavior from it.",
                ))

        grouped: dict[str, list[tuple[dict, str]]] = defaultdict(list)
        for heading, body in _heading_bodies(raw):
            title = normalize_lookup(heading.get("text", ""))
            if title:
                grouped[title].append((heading, body))
        for title, occurrences in grouped.items():
            if len(occurrences) < 2:
                continue
            refs = [f"{source_key}:{heading['id']}" for heading, _ in occurrences]
            fingerprints = {body for _, body in occurrences}
            if len(fingerprints) == 1:
                diagnostics.append(make_diagnostic(
                    "duplicate-heading-repeat",
                    f"{source_key}:{title}",
                    refs,
                    "info",
                    f"Heading {occurrences[0][0].get('text')!r} is repeated with identical normalized body text.",
                ))
            else:
                diagnostics.append(make_diagnostic(
                    "duplicate-heading-variant",
                    f"{source_key}:{title}",
                    refs,
                    "warning",
                    f"Heading {occurrences[0][0].get('text')!r} has multiple source occurrences with different normalized bodies; a domain importer must resolve context explicitly.",
                ))
    return diagnostics
