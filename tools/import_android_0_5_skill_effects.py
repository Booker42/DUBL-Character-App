#!/usr/bin/env python3
"""Generate Android 0.5 skill-effect metadata from the approved v3 review document."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

from docx import Document

HEADERS = ["№", "Навык", "Эффект", "Куда", "Статус", "План реализации", "Реш."]
ROLL_CONTEXTS = [
    "ATTRIBUTE", "SKILL", "FORTITUDE", "REFLEXES", "INITIATIVE", "DODGE",
    "ATTACK", "PARRY", "FEINT", "GRAPPLE", "DISARM", "TRIP", "PUSH",
    "KNOCKDOWN", "BREAK_ITEM",
]
SKILL_NAMES = [
    "Атлетика", "Бартер", "Верховая езда", "Взлом", "Внимательность", "Вождение",
    "Пилотирование", "Выживание", "Дрессировка", "Запугивание", "Знание", "Исполнение",
    "Инженерное дело", "Лидерство", "Красноречие", "Медицина", "Поиск", "Ремесло",
    "Скрытность", "Стрельба", "Холодное оружие", "Метание", "Рукопашный бой", "Компьютеры",
]


def clean(value: str) -> str:
    return re.sub(r"\s+", " ", value.replace("\u00a0", " ")).strip()


def norm(value: str) -> str:
    return clean(value).lower().replace("ё", "е")


def stable_id(index: int, name: str) -> str:
    digest = hashlib.sha1(f"{index}:{norm(name)}".encode("utf-8")).hexdigest()[:12]
    return f"skill_effect_{index:03d}_{digest}"


def parse_value(effect: str, plan: str) -> tuple[int, bool]:
    combined = f"{effect} {plan}"
    per_rank = bool(re.search(r"(?:за каждый ранг|за ранг|/\s*ранг)", combined, re.I))
    # Prefer an explicit signed bonus in the rule text.
    m = re.search(r"(?<!\d)([+−-]\s*\d+)", effect)
    if not m:
        m = re.search(r"(?:добавлять|бонус|штраф)\s*([+−-]?\s*\d+)", plan, re.I)
    if not m:
        return 0, per_rank
    raw = m.group(1).replace(" ", "").replace("−", "-")
    try:
        return int(raw), per_rank
    except ValueError:
        return 0, per_rank


def infer_target_skills(target: str) -> list[str]:
    low = norm(target)
    matches = [name for name in SKILL_NAMES if norm(name) in low]
    # Generic names should not duplicate their specialized forms.
    if "Инженерное дело" in matches:
        pass
    return matches


def infer_roll_context(target: str) -> str:
    upper = target.upper().replace(" ", "_")
    for context in ROLL_CONTEXTS:
        if context in upper:
            return context
    if norm(target).startswith("skill") or "skill ·" in norm(target):
        return "SKILL"
    return ""


def infer_mode(status: str, effect: str, plan: str) -> str:
    if status == "0.5 · FORMULA":
        return "formula"
    if status != "0.5 · EXISTING":
        return "reminder"
    p = norm(plan)
    e = norm(effect)
    if "автоматически учитывать числовой бонус" in p or "автоматически +" in p:
        return "auto_bonus"
    if "чекбокс" in p or "toggle" in p:
        if "преимуществ" in e or "advantage" in p:
            return "toggle_advantage"
        if "помех" in e or "hindrance" in p:
            return "toggle_hindrance"
        value, _ = parse_value(effect, plan)
        if value != 0:
            return "toggle_bonus"
        return "toggle_rule"
    if "вариант" in p or "вместо" in p:
        return "alternative"
    if "preset" in p or "пресет" in p:
        return "preset"
    if "переброс" in p or "повтор" in p:
        return "reroll"
    return "reminder"


def infer_toggle_label(name: str, plan: str) -> str:
    quoted = re.search(r"[«\"]([^»\"]+)[»\"]", plan)
    if quoted:
        return clean(quoted.group(1))
    return clean(name)


def parse_review(path: Path) -> list[dict]:
    doc = Document(path)
    effects: list[dict] = []
    for table in doc.tables:
        headers = [clean(cell.text) for cell in table.rows[0].cells]
        if headers != HEADERS:
            continue
        for row in table.rows[1:]:
            cells = [clean(cell.text) for cell in row.cells]
            if len(cells) != 7 or not cells[0].isdigit():
                continue
            index = int(cells[0])
            name, effect, target, status, plan = cells[1:6]
            value, per_rank = parse_value(effect, plan)
            target_skills = infer_target_skills(target)
            effects.append({
                "id": stable_id(index, name),
                "reviewIndex": index,
                "sourceName": name,
                "effectText": effect,
                "targetText": target,
                "status": status,
                "plan": plan,
                "mode": infer_mode(status, effect, plan),
                "rollContext": infer_roll_context(target),
                "targetSkill": target_skills[0] if target_skills else "",
                "targetSkills": target_skills,
                "value": value,
                "perRank": per_rank,
                "toggleLabel": infer_toggle_label(name, plan),
            })
    effects.sort(key=lambda item: item["reviewIndex"])
    if len(effects) != 283 or [e["reviewIndex"] for e in effects] != list(range(1, 284)):
        raise ValueError(f"Expected review rows 1..283, got {len(effects)} rows")
    return effects


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--review", type=Path, required=True)
    parser.add_argument("--output", type=Path, default=Path("shared/src/commonMain/resources/skill_effects_catalog.json"))
    args = parser.parse_args()
    effects = parse_review(args.review)
    root = {"version": "0.5", "effects": effects}
    args.output.write_text(json.dumps(root, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    modes: dict[str, int] = {}
    for effect in effects:
        modes[effect["mode"]] = modes.get(effect["mode"], 0) + 1
    print(f"Imported {len(effects)} approved skill-effect review rows: {modes}")


if __name__ == "__main__":
    main()
