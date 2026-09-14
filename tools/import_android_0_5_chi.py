#!/usr/bin/env python3
"""Deterministically import the DUBL Chi subsystem for Android 0.5."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from dataclasses import dataclass
from pathlib import Path
from typing import Iterator

from docx import Document
from docx.document import Document as DocumentType
from docx.oxml.ns import qn
from docx.table import Table
from docx.text.paragraph import Paragraph


def clean(value: str) -> str:
    return re.sub(r"\s+", " ", value.replace("\u00a0", " ")).strip()


def normalize(value: str) -> str:
    return clean(value).lower().replace("ё", "е").replace("—", "-").replace("–", "-").strip(" .,:;!?")


def chi_id(kind: str, name: str) -> str:
    digest = hashlib.sha1(f"chi:{kind}:{normalize(name)}".encode("utf-8")).hexdigest()[:16]
    return f"chi_{kind}_{digest}"


def iter_blocks(document: DocumentType) -> Iterator[Paragraph | Table]:
    for child in document.element.body.iterchildren():
        if child.tag == qn("w:p"):
            yield Paragraph(child, document)
        elif child.tag == qn("w:tbl"):
            yield Table(child, document)


def field_int(text: str, label: str, default: int = 1) -> int:
    m = re.search(rf"{re.escape(label)}\s*:\s*(\d+)", text, re.I)
    return int(m.group(1)) if m else default


def short_school_name(title: str) -> str:
    title = clean(re.sub(r"^\d+\.\s*", "", title))
    return clean(re.sub(r"\s*\([^)]*\)\s*$", "", title))


def ability_entry(name: str, cost: int, requirements: str, benefit: str, category: str = "Развитие ЦИ") -> dict:
    return {
        "id": chi_id("development", name),
        "name": clean(name),
        "section": "ЦИ",
        "category": category,
        "cost": 0,
        "costType": "ability",
        "ranks": 1,
        "requirements": clean(requirements) or "-",
        "benefit": clean(benefit),
        "tags": ["ЦИ"],
        "abilityOptions": [{"source": "ЦИ", "value": max(0, cost)}],
    }


def xp_entry(name: str, cost: int, ranks: int, requirements: str, benefit: str, category: str) -> dict:
    return {
        "id": chi_id("development", name),
        "name": clean(name),
        "section": "ЦИ",
        "category": category,
        "cost": max(0, cost),
        "costType": "xp",
        "ranks": max(1, ranks),
        "requirements": clean(requirements) or "-",
        "benefit": clean(benefit),
        "tags": ["ЦИ"],
    }


def technique_entry(name: str, chi_cost: int, action: str, effect: str, requirements: str, school: str = "Общие приёмы") -> dict:
    return {
        "id": chi_id("technique", name),
        "name": clean(name),
        "school": clean(school),
        "chiCost": max(0, chi_cost),
        "action": clean(action),
        "effect": clean(effect),
        "requirements": clean(requirements) or "Внутренняя Ци",
    }


@dataclass(frozen=True)
class ChiContent:
    developments: list[dict]
    schools: list[dict]
    techniques: list[dict]


CORE_DEVELOPMENTS = {
    "Внутренняя Ци": (1, "-"),
    "Мастер Ци": (2, "Внутренняя Ци, Воля 5"),
}

MASTER_ABILITY_NAMES = {
    "Быстрое восстановление Ци",
    "Ци и оружие",
    "Ци мечника",
    "Слияние с природой",
    "Медитация на ходу",
}
NEW_ABILITY_NAMES = {"Пробуждённая Ци", "Ци тела", "Ци разума", "Дыхание жизни", "Драконья броня"}


def parse_inline_ability(text: str, default_requirement: str = "Внутренняя Ци") -> dict | None:
    m = re.match(r"^(.+?)\s*\(стоимость\s*(\d+)(?:\s*очк\w*)?\s*,?\s*(?:требуется\s*)?([^)]*)\)\s*(.*)$", clean(text), re.I)
    if not m:
        return None
    name, cost, requirement_blob, benefit = m.groups()
    req = clean(requirement_blob)
    if req:
        req = re.sub(r"^требуется\s+", "", req, flags=re.I)
        requirements = f"{default_requirement}, {req}" if default_requirement and normalize(default_requirement) not in normalize(req) else req
    else:
        requirements = default_requirement
    if clean(name) == "Пробуждённая Ци":
        requirements = "Мастер Ци, Воля 5"
    return ability_entry(name, int(cost), requirements, benefit, "Мастерство ЦИ")


def parse_named_chi_technique(text: str, requirements: str, school: str) -> dict | None:
    m = re.match(r"^(.+?)\s*\((\d+)\s*Ци\s*,\s*([^)]+)\)\s*(.*)$", clean(text), re.I)
    if not m:
        return None
    name, cost, action, effect = m.groups()
    return technique_entry(name, int(cost), action, effect, requirements, school)


def parse_chi_content(path: Path) -> ChiContent:
    doc = Document(path)
    blocks: list[Paragraph | Table] = []
    inside = False
    for block in iter_blocks(doc):
        if isinstance(block, Paragraph):
            text = clean(block.text)
            if text == "Ци":
                inside = True
            elif inside and text == "Оружейное кунг-фу":
                break
        if inside:
            blocks.append(block)

    developments: list[dict] = []
    schools: list[dict] = []
    techniques: list[dict] = []

    # Core/basic/advanced techniques and core development lines.
    mode = ""
    i = 0
    while i < len(blocks):
        block = blocks[i]
        if isinstance(block, Paragraph):
            text = clean(block.text)
            style = block.style.name
            if text.startswith("Базовые приёмы Ци"):
                mode = "basic-techniques"
            elif text.startswith("Продвинутые приёмы Ци"):
                mode = "advanced-techniques"
            elif text == "Развитие Ци":
                mode = "core-development"
            elif text == "Основы Ци":
                mode = "chi-basics"
            elif text.startswith("Мастерские способности Ци"):
                mode = "master-abilities"
            elif text.startswith("Новые способности"):
                mode = "new-abilities"
            elif style.startswith("Heading 2") or style.startswith("Heading 1"):
                if "Школа " in text:
                    mode = "school"
                elif text.startswith("Расширенный список"):
                    mode = "expanded"
                elif text.startswith("Комбинированные техники"):
                    mode = "combined"
                else:
                    mode = ""
            elif mode in {"basic-techniques", "advanced-techniques"}:
                req = "Внутренняя Ци" if mode == "basic-techniques" else "Мастер Ци"
                item = parse_named_chi_technique(text, req, "Базовые приёмы" if mode == "basic-techniques" else "Продвинутые приёмы")
                if item:
                    techniques.append(item)
            elif mode == "core-development":
                if text.startswith("Внутренняя Ци"):
                    benefit = text.split("—", 1)[-1] if "—" in text else text
                    developments.append(ability_entry("Внутренняя Ци", 1, "-", benefit))
                elif text.startswith("Мастер Ци"):
                    benefit = text.split("—", 1)[-1] if "—" in text else text
                    developments.append(ability_entry("Мастер Ци", 2, "Внутренняя Ци, Воля 5", benefit))
                elif text.startswith("Восстановление Ци"):
                    developments.append(xp_entry(
                        "Восстановление Ци", 30, 3, "Внутренняя Ци",
                        text.split("—", 1)[-1] if "—" in text else text,
                        "Развитие ЦИ",
                    ))
            elif mode == "chi-basics" and text and not re.match(r"^(Стоимость|Ранги|Дальность|Длительность|Эффект):", text):
                # Five paragraph-based XP skills. Read their field paragraphs until the next name/heading.
                name = text
                fields: dict[str, str] = {}
                j = i + 1
                while j < len(blocks) and isinstance(blocks[j], Paragraph):
                    nxt = clean(blocks[j].text)
                    if not nxt:
                        j += 1
                        continue
                    if blocks[j].style.name.startswith("Heading") or not re.match(r"^(Стоимость|Ранги|Дальность|Длительность|Эффект):", nxt):
                        break
                    key, value = nxt.split(":", 1)
                    fields[clean(key)] = clean(value)
                    j += 1
                if "Стоимость" in fields and "Эффект" in fields:
                    developments.append(xp_entry(name, int(fields["Стоимость"]), int(fields.get("Ранги", "1")), "Внутренняя Ци", fields["Эффект"], "Основы ЦИ"))
                    techniques.append(technique_entry(name, 1, fields.get("Длительность", "Активация"), fields["Эффект"], name, "Основы ЦИ"))
                    i = j - 1
            elif mode in {"master-abilities", "new-abilities"}:
                expected = MASTER_ABILITY_NAMES if mode == "master-abilities" else NEW_ABILITY_NAMES
                if any(text.startswith(name) for name in expected):
                    parsed = parse_inline_ability(text)
                    if parsed:
                        parsed["category"] = "Мастерство ЦИ" if mode == "master-abilities" else "Развитие ЦИ"
                        developments.append(parsed)
        i += 1

    # Schools and table-based techniques are easier/safer as a second block-order pass.
    current_school: str | None = None
    current_school_requirements = ""
    current_school_passives: list[str] = []
    collecting_passives = False
    table_mode = ""
    for block in blocks:
        if isinstance(block, Paragraph):
            text = clean(block.text)
            style = block.style.name
            if "Школа " in text and (style.startswith("Heading 1") or style.startswith("Heading 2")):
                current_school = short_school_name(text)
                current_school_requirements = ""
                current_school_passives = []
                collecting_passives = False
                table_mode = "school"
            elif text.startswith("Расширенный список приёмов Ци"):
                current_school = None
                table_mode = "expanded"
            elif text.startswith("Комбинированные техники"):
                current_school = None
                table_mode = "combined"
            elif current_school and text.startswith("Требования:"):
                current_school_requirements = clean(text.split(":", 1)[1]).strip(".")
            elif current_school and text.startswith("Пассивные преимущества школы"):
                collecting_passives = True
            elif current_school and text.startswith("Уникальные приёмы"):
                collecting_passives = False
                schools.append({
                    "id": chi_id("school", current_school),
                    "name": current_school,
                    "requirements": current_school_requirements,
                    "passives": list(current_school_passives),
                })
                developments.append(ability_entry(
                    current_school,
                    1,
                    f"Внутренняя Ци, {current_school_requirements}" if current_school_requirements else "Внутренняя Ци",
                    "Открывает пассивные преимущества школы и её уникальные приёмы. " + " ".join(current_school_passives),
                    "Школы ЦИ",
                ))
            elif current_school and collecting_passives and text:
                current_school_passives.append(text)
        else:
            rows = [[clean(c.text) for c in row.cells] for row in block.rows]
            if not rows:
                continue
            headers = [normalize(x) for x in rows[0]]
            if table_mode == "school" and current_school and "стоимость ци" in headers:
                for row in rows[1:]:
                    if len(row) < 4 or not row[0]:
                        continue
                    techniques.append(technique_entry(row[0], field_int(f"Стоимость: {row[1]}", "Стоимость", 0), row[2], row[3], current_school, current_school))
            elif table_mode == "expanded" and "стоимость ци" in headers:
                for row in rows[1:]:
                    if len(row) < 5 or not row[0]:
                        continue
                    req = row[2]
                    req = "Внутренняя Ци" if normalize(req) in {"", "-", "—"} else f"Внутренняя Ци, {req}"
                    techniques.append(technique_entry(row[0], field_int(f"Стоимость: {row[1]}", "Стоимость", 0), row[3], row[4], req, "Общие приёмы"))
            elif table_mode == "combined" and len(rows[0]) >= 2:
                for row in rows[1:]:
                    if len(row) < 2 or not row[0]:
                        continue
                    techniques.append(technique_entry(row[0], 3, "2 ОД", row[1], row[0], "Комбинированные техники"))

    # Deduplicate while preserving deterministic document order.
    def unique(items: list[dict]) -> list[dict]:
        seen: set[str] = set()
        result: list[dict] = []
        for item in items:
            if item["id"] in seen:
                continue
            seen.add(item["id"])
            result.append(item)
        return result

    content = ChiContent(unique(developments), unique(schools), unique(techniques))
    if (len(content.developments), len(content.schools), len(content.techniques)) != (27, 9, 68):
        raise ValueError(
            f"Unexpected Chi scope: developments={len(content.developments)}, schools={len(content.schools)}, techniques={len(content.techniques)}"
        )
    return content


def update_development_catalog(path: Path, developments: list[dict]) -> None:
    root = json.loads(path.read_text(encoding="utf-8"))
    entries = [entry for entry in root.get("entries", []) if not str(entry.get("id", "")).startswith("chi_development_")]
    entries.extend(developments)
    root["version"] = "0.12.0"
    root["entries"] = entries
    path.write_text(json.dumps(root, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def write_chi_catalog(path: Path, content: ChiContent) -> None:
    root = {"version": "0.5", "schools": content.schools, "techniques": content.techniques}
    path.write_text(json.dumps(root, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, required=True)
    parser.add_argument("--development-catalog", type=Path, default=Path("app/src/main/assets/development_catalog.json"))
    parser.add_argument("--chi-catalog", type=Path, default=Path("app/src/main/assets/chi_catalog.json"))
    args = parser.parse_args()
    content = parse_chi_content(args.source)
    update_development_catalog(args.development_catalog, content.developments)
    write_chi_catalog(args.chi_catalog, content)
    print(f"Imported {len(content.developments)} Chi developments, {len(content.schools)} schools, {len(content.techniques)} techniques.")


if __name__ == "__main__":
    main()
