#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Iterator

from docx import Document
from docx.document import Document as DocumentType
from docx.oxml.ns import qn
from docx.table import Table
from docx.text.paragraph import Paragraph

SCHEMA_VERSION = 1


def normalize_text(value: str) -> str:
    value = value.replace("\u00a0", " ").replace("ё", "е").replace("Ё", "Е")
    value = value.replace("—", "-").replace("–", "-")
    return re.sub(r"\s+", " ", value).strip().lower()


def _iter_blocks(document: DocumentType) -> Iterator[Paragraph | Table]:
    for child in document.element.body.iterchildren():
        if child.tag == qn("w:p"):
            yield Paragraph(child, document)
        elif child.tag == qn("w:tbl"):
            yield Table(child, document)


def _heading_level(paragraph: Paragraph) -> int | None:
    name = paragraph.style.name or ""
    match = re.fullmatch(r"Heading\s+([1-9])", name, flags=re.IGNORECASE)
    return int(match.group(1)) if match else None


def extract_docx(path: Path) -> dict:
    path = Path(path)
    raw = path.read_bytes()
    document = Document(path)
    heading_stack: list[str] = []
    blocks: list[dict] = []

    for order, block in enumerate(_iter_blocks(document), start=1):
        if isinstance(block, Paragraph):
            text = block.text
            level = _heading_level(block)
            if level is not None:
                heading_stack = heading_stack[: level - 1]
                while len(heading_stack) < level - 1:
                    heading_stack.append("")
                if len(heading_stack) == level - 1:
                    heading_stack.append(text.strip())
                else:
                    heading_stack[level - 1] = text.strip()
            item = {
                "id": f"p-{order:06d}",
                "kind": "paragraph",
                "order": order,
                "text": text,
                "normalizedText": normalize_text(text),
                "style": block.style.name or "",
                "headingLevel": level,
                "headingPath": [x for x in heading_stack if x],
            }
        else:
            rows = [[cell.text for cell in row.cells] for row in block.rows]
            item = {
                "id": f"t-{order:06d}",
                "kind": "table",
                "order": order,
                "rows": rows,
                "normalizedRows": [[normalize_text(cell) for cell in row] for row in rows],
                "headingPath": [x for x in heading_stack if x],
            }
        blocks.append(item)

    return {
        "schemaVersion": SCHEMA_VERSION,
        "source": {
            "filename": path.name,
            "sha256": hashlib.sha256(raw).hexdigest(),
            "sizeBytes": len(raw),
        },
        "stats": {
            "blocks": len(blocks),
            "paragraphs": sum(1 for b in blocks if b["kind"] == "paragraph"),
            "tables": sum(1 for b in blocks if b["kind"] == "table"),
            "headings": sum(1 for b in blocks if b["kind"] == "paragraph" and b["headingLevel"] is not None),
        },
        "blocks": blocks,
    }


def write_json(data: dict, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract a DUBL DOCX into deterministic Raw Source IR")
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    data = extract_docx(args.source)
    write_json(data, args.output)
    print(json.dumps(data["stats"], ensure_ascii=False))


if __name__ == "__main__":
    main()
