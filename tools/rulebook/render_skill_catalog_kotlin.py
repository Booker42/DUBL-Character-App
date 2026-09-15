from __future__ import annotations

import json


def _string(value: object) -> str:
    return json.dumps(str(value), ensure_ascii=False)


def _bool(value: object) -> str:
    return "true" if bool(value) else "false"


def render_skill_catalog_kotlin(payload: dict) -> str:
    rank_costs = ", ".join(str(int(value)) for value in payload.get("rankCosts", []))
    lines = [
        "package com.dubl.character.android.model",
        "",
        "/** Generated from the validated DUBL 3.69 rulebook bundle. Do not edit by hand. */",
        "internal object GeneratedSkillCatalog {",
        f"    val rankCosts: List<Int> = listOf({rank_costs})",
        "",
        "    val definitions: List<SkillDefinition> = listOf(",
    ]
    for skill in payload.get("skills", []):
        lines.extend([
            "        SkillDefinition(",
            f"            id = {_string(skill['id'])},",
            f"            name = {_string(skill['name'])},",
            f"            description = {_string(skill.get('description', ''))},",
            f"            category = SkillCategory.{skill['category']},",
            f"            defaultAttribute = AttributeId.{skill['defaultAttributeHint']},",
            f"            untrained = UntrainedRule.{skill['untrained']},",
            f"            auto6 = {_string(skill.get('auto6', ''))},",
            f"            auto12 = {_string(skill.get('auto12', ''))},",
            f"            template = {_bool(skill.get('template', False))},",
            "        ),",
        ])
    lines.extend([
        "    )",
        "}",
        "",
    ])
    return "\n".join(lines)
