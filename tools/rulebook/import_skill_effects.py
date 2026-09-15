from __future__ import annotations

from copy import deepcopy

from .diagnostics import make_diagnostic


def _source_text(entry: dict) -> str:
    parts = []
    benefit = str(entry.get("benefit") or "").strip()
    notes = str(entry.get("notes") or "").strip()
    if benefit:
        parts.append(benefit)
    if notes:
        parts.append(notes)
    return "\n".join(parts)


def import_skill_effects(development: dict, bindings: dict) -> tuple[dict, list[dict]]:
    by_id = {str(entry.get("id")): entry for entry in development.get("entries", [])}
    effects: list[dict] = []
    diagnostics: list[dict] = []

    for binding in bindings.get("bindings", []):
        development_id = str(binding.get("developmentId") or "")
        source = by_id.get(development_id)
        if source is None:
            diagnostics.append(make_diagnostic(
                kind="skill-effect-development-missing",
                subject=f"skill_effects:{binding.get('id', development_id)}",
                source_refs=[],
                severity="error",
                message=f"Skill-effect binding points to missing Development entry {development_id!r}.",
            ))
            continue

        effect = deepcopy(binding)
        effect["developmentId"] = development_id
        effect["sourceName"] = str(source.get("name") or "")
        effect["effectText"] = _source_text(source)
        effect["sourceRefs"] = list(source.get("sourceRefs") or [])
        effects.append(effect)

    effects.sort(key=lambda item: (int(item.get("reviewIndex", 0)), str(item.get("id", ""))))
    return {"version": "3.69", "effects": effects}, diagnostics
