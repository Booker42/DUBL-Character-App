from __future__ import annotations

import re
from typing import Iterable

from .diagnostics import make_diagnostic
from .source_index import normalize_lookup

_FIELD_RE = re.compile(
    r"^(Стоимость|Ранги?|Требовани[ея]|Выгода|Описание|Особое|Примечание)\s*:\s*(.*)$",
    re.IGNORECASE,
)


def _int_prefix(value: str) -> int | None:
    match = re.search(r"-?\d+", value)
    return int(match.group(0)) if match else None


def _paragraph_candidates(raw_ir: dict, name: str) -> list[int]:
    needle = normalize_lookup(name)
    return [
        index
        for index, block in enumerate(raw_ir.get("blocks", []))
        if block.get("kind") == "paragraph" and normalize_lookup(str(block.get("text", ""))) == needle
    ]




def _next_nonempty_paragraph(raw_ir: dict, start_index: int) -> str | None:
    blocks = raw_ir.get("blocks", [])
    heading_path = blocks[start_index].get("headingPath", [])
    for block in blocks[start_index + 1 : min(start_index + 6, len(blocks))]:
        if block.get("headingPath", []) != heading_path:
            break
        if block.get("kind") != "paragraph":
            continue
        text = str(block.get("text", "")).strip()
        if text:
            return text
    return None


def _candidate_reaches_mechanics(raw_ir: dict, start_index: int, name: str) -> bool:
    """Return true when this occurrence leads to its own mechanical card.

    Rulebook branches often repeat a title: first as prose/section heading and later
    as the actual card title, sometimes with a decorative ``[Branch]`` line between
    the title and ``Стоимость``. The earlier occurrence must not win merely because
    mechanics appear later in the same heading path.
    """
    blocks = raw_ir.get("blocks", [])
    heading_path = blocks[start_index].get("headingPath", [])
    normalized_name = normalize_lookup(name)
    for block in blocks[start_index + 1 : min(start_index + 10, len(blocks))]:
        if block.get("headingPath", []) != heading_path:
            break
        if block.get("kind") != "paragraph":
            continue
        text = str(block.get("text", "")).strip()
        if not text:
            continue
        if normalize_lookup(text) == normalized_name:
            return False
        if _FIELD_RE.match(text):
            return True
    return False


def _select_candidates(raw_ir: dict, candidates: list[int], binding: dict, preferred_heading_component: str | None = None) -> list[int]:
    if len(candidates) <= 1:
        return candidates

    blocks = raw_ir.get("blocks", [])

    mechanical_cards = [
        index
        for index in candidates
        if _candidate_reaches_mechanics(raw_ir, index, str(binding.get("name", "")))
    ]
    if len(mechanical_cards) == 1:
        return mechanical_cards
    if mechanical_cards:
        candidates = mechanical_cards

    source_context = [normalize_lookup(str(value)) for value in binding.get("sourceContext", []) if str(value).strip()]
    if source_context:
        contextual = []
        for index in candidates:
            path = [normalize_lookup(str(value)) for value in blocks[index].get("headingPath", [])]
            if all(context in path for context in source_context):
                contextual.append(index)
        if len(contextual) == 1:
            return contextual
        if contextual:
            candidates = contextual
    else:
        category = normalize_lookup(str(binding.get("category", "")))
        if category:
            category_matches = []
            for index in candidates:
                path = [normalize_lookup(str(value)) for value in blocks[index].get("headingPath", [])]
                if "навыки" in path and category in path:
                    category_matches.append(index)
            if len(category_matches) == 1:
                return category_matches
            if category_matches:
                candidates = category_matches

    if preferred_heading_component and len(candidates) > 1:
        preferred = normalize_lookup(preferred_heading_component)
        preferred_candidates = [
            index for index in candidates
            if preferred in [normalize_lookup(str(value)) for value in blocks[index].get("headingPath", [])]
        ]
        if len(preferred_candidates) == 1:
            return preferred_candidates
        if preferred_candidates:
            candidates = preferred_candidates

    source_occurrence = binding.get("sourceOccurrence")
    if isinstance(source_occurrence, int) and 0 <= source_occurrence < len(candidates):
        return [candidates[source_occurrence]]

    adjacent_mechanics = [
        index
        for index in candidates
        if (text := _next_nonempty_paragraph(raw_ir, index)) is not None and _FIELD_RE.match(text)
    ]
    if len(adjacent_mechanics) == 1:
        return adjacent_mechanics
    return candidates

def _looks_like_next_card_title(raw_ir: dict, start_index: int) -> bool:
    """Detect a new card title inside a heading path without mistaking notes for it."""
    blocks = raw_ir.get("blocks", [])
    heading_path = blocks[start_index].get("headingPath", [])
    for block in blocks[start_index + 1 : min(start_index + 6, len(blocks))]:
        if block.get("headingPath", []) != heading_path:
            break
        if block.get("kind") != "paragraph":
            continue
        text = str(block.get("text", "")).strip()
        if not text:
            continue
        # Decorative branch labels such as [Авангард] may sit between title and fields.
        if text.startswith("[") and text.endswith("]"):
            continue
        match = _FIELD_RE.match(text)
        if not match:
            return False
        label = normalize_lookup(match.group(1))
        return label in {"стоимость", "ранг", "ранги", "требование", "требования"}
    return False


def _parse_record(raw_ir: dict, start_index: int, source_key: str) -> tuple[dict, list[str]]:
    blocks = raw_ir.get("blocks", [])
    start = blocks[start_index]
    heading_path = start.get("headingPath", [])
    refs = [f"{source_key}:{start['id']}"]
    fields: dict[str, str] = {}
    current_key: str | None = None
    seen_mechanical = False

    def append_value(key: str, value: str) -> None:
        if not value:
            return
        if key in fields and fields[key]:
            fields[key] = f"{fields[key]}\n{value}"
        else:
            fields[key] = value

    for offset, block in enumerate(blocks[start_index + 1 : start_index + 64], start=start_index + 1):
        if block.get("headingPath", []) != heading_path:
            break

        if block.get("kind") == "table":
            if current_key is not None:
                flat_cells = [str(cell).strip() for row in block.get("rows", []) for cell in row if str(cell).strip()]
                if flat_cells:
                    append_value(current_key, "\n".join(flat_cells))
                    refs.append(f"{source_key}:{block['id']}")
            continue

        if block.get("kind") != "paragraph":
            continue
        text = str(block.get("text", "")).strip()
        if not text:
            if seen_mechanical:
                next_meaningful = None
                for future in blocks[offset + 1 : min(offset + 6, len(blocks))]:
                    if future.get("headingPath", []) != heading_path:
                        break
                    if future.get("kind") == "table":
                        next_meaningful = future
                        break
                    if future.get("kind") == "paragraph" and str(future.get("text", "")).strip():
                        next_meaningful = future
                        break
                if next_meaningful is not None and next_meaningful.get("kind") == "table":
                    continue
                break
            continue

        if seen_mechanical and str(block.get("style", "")).strip().casefold() == "title":
            break

        match = _FIELD_RE.match(text)
        if match:
            label = normalize_lookup(match.group(1))
            value = match.group(2).strip()
            if label in {"ранг", "ранги"}:
                key = "ranks"
            elif label.startswith("требован"):
                key = "requirements"
            elif label in {"выгода", "описание"}:
                key = "benefit"
            elif label == "стоимость":
                key = "cost"
            elif label == "особое":
                key = "special"
            else:
                key = "notes"
            append_value(key, value)
            current_key = key
            refs.append(f"{source_key}:{block['id']}")
            seen_mechanical = True
            continue

        if not seen_mechanical:
            continue

        # Same-path cards are common. Stop on a fieldless title only when it is
        # followed by the start of a new mechanical card; a following Notes/Special
        # field still belongs to the current card.
        if seen_mechanical and current_key in {"benefit", "notes", "special"} and _looks_like_next_card_title(raw_ir, offset):
            break

        if current_key is not None:
            append_value(current_key, text)
            refs.append(f"{source_key}:{block['id']}")

    return fields, refs


def _import_bound_development(
    raw_ir: dict,
    bindings: dict,
    source_key: str,
    section: str,
    category_as_context: bool = False,
    required_heading_component: str | None = None,
) -> tuple[dict, list[dict]]:
    entries: list[dict] = []
    diagnostics: list[dict] = []
    paragraph_lookup: dict[str, list[int]] = {}
    for index, block in enumerate(raw_ir.get("blocks", [])):
        if block.get("kind") != "paragraph":
            continue
        paragraph_lookup.setdefault(normalize_lookup(str(block.get("text", ""))), []).append(index)

    for binding in bindings.get("bindings", []):
        name = str(binding.get("name", "")).strip()
        entry_id = str(binding.get("id", "")).strip()
        subject = f"development:{entry_id or name}"
        selector_binding = binding
        if category_as_context and not binding.get("sourceContext") and str(binding.get("category", "")).strip():
            selector_binding = dict(binding)
            selector_binding["sourceContext"] = [str(binding.get("category"))]
        raw_candidates = paragraph_lookup.get(normalize_lookup(name), [])
        candidates = _select_candidates(
            raw_ir,
            raw_candidates,
            selector_binding,
            preferred_heading_component=required_heading_component,
        )
        if len(candidates) != 1:
            refs = [f"{source_key}:{raw_ir['blocks'][index]['id']}" for index in candidates]
            diagnostics.append(
                make_diagnostic(
                    "development-source-ambiguous" if candidates else "development-source-missing",
                    subject,
                    refs,
                    "warning",
                    f"Expected one source paragraph for {name!r}, found {len(candidates)}.",
                    sourcePolicy=[source_key],
                )
            )
            continue

        fields, refs = _parse_record(raw_ir, candidates[0], source_key)
        cost = _int_prefix(fields.get("cost", "")) if "cost" in fields else None
        ranks = _int_prefix(fields.get("ranks", "")) if "ranks" in fields else None
        requirements = fields.get("requirements")
        benefit = fields.get("benefit")
        entry = {
            "id": entry_id,
            "name": name,
            "section": section,
            "category": str(binding.get("category", "")),
            "cost": cost,
            "ranks": ranks,
            "requirements": requirements,
            "benefit": benefit,
            "sourceRefs": refs,
        }
        source_notes = [value for value in (fields.get("notes"), fields.get("special")) if value]
        if source_notes:
            entry["notes"] = "\n".join(source_notes)
        entries.append(entry)
        missing = [
            key for key, value in (("cost", cost), ("ranks", ranks), ("benefit", benefit)) if value is None
        ]
        if missing:
            diagnostics.append(
                make_diagnostic(
                    "development-fields-missing",
                    subject,
                    refs,
                    "warning",
                    f"Rulebook entry {name!r} is missing parsed field(s): {', '.join(missing)}. No value was guessed.",
                    sourcePolicy=[source_key],
                    missingFields=missing,
                )
            )

    return {
        "schemaVersion": 1,
        "source": source_key,
        "entries": entries,
    }, diagnostics


def import_regular_development(raw_ir: dict, bindings: dict, source_key: str = "core") -> tuple[dict, list[dict]]:
    return _import_bound_development(raw_ir, bindings, source_key, section="Навыки")


def import_special_development(raw_ir: dict, bindings: dict, source_key: str = "core") -> tuple[dict, list[dict]]:
    return _import_bound_development(
        raw_ir,
        bindings,
        source_key,
        section="Ветки способностей",
        category_as_context=True,
        required_heading_component="Особые навыки",
    )


def compare_regular_development_runtime(imported: dict, runtime_root: dict) -> list[dict]:
    runtime_by_id = {str(entry.get("id", "")): entry for entry in runtime_root.get("entries", [])}
    diagnostics: list[dict] = []
    for source_entry in imported.get("entries", []):
        entry_id = str(source_entry.get("id", ""))
        runtime = runtime_by_id.get(entry_id)
        if runtime is None:
            continue
        differences: dict[str, dict] = {}
        for field, runtime_field in (("cost", "cost"), ("ranks", "ranks"), ("requirements", "requirements"), ("benefit", "benefit")):
            source_value = source_entry.get(field)
            if source_value is None:
                continue
            runtime_value = runtime.get(runtime_field)
            if isinstance(source_value, str):
                source_cmp = re.sub(r"\s+", " ", source_value).strip()
                runtime_cmp = re.sub(r"\s+", " ", str(runtime_value or "")).strip()
            else:
                source_cmp = source_value
                runtime_cmp = runtime_value
            if source_cmp != runtime_cmp:
                differences[field] = {"rulebook": source_value, "runtime": runtime_value}
        if differences:
            diagnostics.append(
                make_diagnostic(
                    "development-runtime-drift",
                    f"development:{entry_id}",
                    list(source_entry.get("sourceRefs", [])),
                    "warning",
                    f"Runtime development entry {source_entry.get('name')!r} differs from parsed rulebook mechanics.",
                    differences=differences,
                )
            )
    return diagnostics


def _runtime_text(value: object) -> str:
    """Normalize DOCX spacing without changing paragraph boundaries."""
    return "\n".join(re.sub(r"[ \t]+", " ", line).strip() for line in str(value or "").splitlines()).strip()


def promote_regular_development(imported: dict, bindings: dict, version: str = "3.69") -> dict:
    """Build the runtime catalog from rulebook mechanics plus non-rule structural bindings.

    Mechanical fields come from ``imported``. Bindings may contribute persistence/UI
    structure (stable id/category/tags/accessId) and an explicit compatibility fallback
    for a field the rulebook does not define. Missing executable mechanics are never
    guessed implicitly.
    """
    binding_by_id = {str(item.get("id", "")): item for item in bindings.get("bindings", [])}
    runtime_entries: list[dict] = []

    for source in imported.get("entries", []):
        entry_id = str(source.get("id", ""))
        binding = binding_by_id.get(entry_id, {})
        ranks = source.get("ranks")
        fallback = binding.get("legacyFallback") or {}
        incomplete = False
        conflict = ""
        conflict_note = ""
        if ranks is None:
            if "ranks" not in fallback:
                raise ValueError(f"Development entry {entry_id!r} has no source rank and no explicit legacy fallback")
            ranks = int(fallback["ranks"])
            incomplete = True
            conflict = str(fallback.get("mechanicsConflict", "Rulebook field is unresolved"))
            conflict_note = str(fallback.get("conflictNote", "Compatibility fallback; user override is allowed."))

        runtime = {
            "id": entry_id,
            "name": str(source.get("name", "")),
            "section": "Навыки",
            "category": str(binding.get("category", source.get("category", "Общие"))),
            "cost": int(source.get("cost") or 0),
            "costType": "xp",
            "ranks": int(ranks),
            "requirements": _runtime_text(source.get("requirements")),
            "benefit": _runtime_text(source.get("benefit")),
            "sourceRefs": list(source.get("sourceRefs", [])),
        }

        notes = _runtime_text(source.get("notes"))
        if notes:
            runtime["notes"] = notes
        tags = [str(value) for value in binding.get("tags", []) if str(value)]
        if tags:
            runtime["tags"] = tags
        access_id = str(binding.get("accessId", "")).strip()
        if access_id:
            runtime["accessId"] = access_id

        repeatable_text = " ".join(
            str(source.get(key) or "") for key in ("benefit", "notes")
        ).casefold().replace("ё", "е")
        if "можно брать несколько раз" in repeatable_text:
            runtime["repeatable"] = True

        if incomplete:
            runtime["incomplete"] = True
            runtime["mechanicsConflict"] = conflict
            runtime["conflictNote"] = conflict_note

        runtime_entries.append(runtime)

    return {"version": version, "entries": runtime_entries}


def promote_special_development(imported: dict, bindings: dict, version: str = "3.69") -> dict:
    """Build source-generated special-branch children with explicit escape hatches.

    Rule mechanics come from the rulebook import. Bindings only carry stable/runtime
    structure plus explicit compatibility fallbacks for fields the source omits.
    Draft placeholders and semantic conflicts remain executable only as incomplete
    entries so the user can resolve them through a local character override.
    """
    binding_by_id = {str(item.get("id", "")): item for item in bindings.get("bindings", [])}
    runtime_entries: list[dict] = []

    for source in imported.get("entries", []):
        entry_id = str(source.get("id", ""))
        binding = binding_by_id.get(entry_id, {})
        fallback = binding.get("legacyFallback") or {}
        unresolved = binding.get("unresolved") or {}
        missing_fields: list[str] = []

        cost = source.get("cost")
        if cost is None:
            if "cost" not in fallback:
                raise ValueError(f"Special development entry {entry_id!r} has no source cost and no explicit legacy fallback")
            cost = int(fallback["cost"])
            missing_fields.append("cost")

        ranks = source.get("ranks")
        if ranks is None:
            if "ranks" not in fallback:
                raise ValueError(f"Special development entry {entry_id!r} has no source rank and no explicit legacy fallback")
            ranks = int(fallback["ranks"])
            missing_fields.append("ranks")

        benefit = source.get("benefit")
        if benefit is None:
            benefit = fallback.get("benefit", "")
            missing_fields.append("benefit")

        runtime = {
            "id": entry_id,
            "name": str(source.get("name", "")),
            "section": "Ветки способностей",
            "category": str(binding.get("category", source.get("category", ""))),
            "cost": int(cost),
            "costType": "xp",
            "ranks": int(ranks),
            "requirements": _runtime_text(source.get("requirements")),
            "benefit": _runtime_text(benefit),
            "sourceRefs": list(source.get("sourceRefs", [])),
        }

        notes = _runtime_text(source.get("notes"))
        if notes:
            runtime["notes"] = notes
        tags = [str(value) for value in binding.get("tags", []) if str(value)]
        if tags:
            runtime["tags"] = tags
        access_id = str(binding.get("accessId", "")).strip()
        if access_id:
            runtime["accessId"] = access_id
        if binding.get("perfectRoot"):
            runtime["perfectRoot"] = True

        repeatable_text = " ".join(str(source.get(key) or "") for key in ("benefit", "notes")).casefold().replace("ё", "е")
        if "можно брать несколько раз" in repeatable_text:
            runtime["repeatable"] = True

        incomplete = bool(missing_fields)
        conflict = ""
        conflict_note = ""
        if missing_fields:
            conflict = str(
                fallback.get(
                    "mechanicsConflict",
                    f"В рулбуке отсутствуют обязательные поля: {', '.join(missing_fields)}.",
                )
            )
            conflict_note = str(
                fallback.get(
                    "conflictNote",
                    "Использован только явный compatibility fallback; локальная правка разрешена.",
                )
            )

        normalized_name = normalize_lookup(str(source.get("name", "")))
        draft_text = normalize_lookup(f"{source.get('benefit') or ''} {source.get('notes') or ''}")
        if normalized_name in {"???", "название"} or "позже напишу" in draft_text:
            incomplete = True
            if not conflict:
                conflict = "В рулбуке запись содержит черновой/шаблонный текст и не считается завершённой механикой."
                conflict_note = "Используйте локальную правку персонажа, чтобы зафиксировать рабочую версию для вашей группы."

        if unresolved:
            incomplete = True
            conflict = str(unresolved.get("mechanicsConflict", conflict or "Правило в рулбуке неоднозначно."))
            conflict_note = str(
                unresolved.get(
                    "conflictNote",
                    conflict_note or "Трактовка не выбрана автоматически; локальная правка разрешена.",
                )
            )

        if incomplete:
            runtime["incomplete"] = True
            runtime["mechanicsConflict"] = conflict
            runtime["conflictNote"] = conflict_note

        runtime_entries.append(runtime)

    return {"version": version, "entries": runtime_entries}


def import_magic_development(raw_ir: dict, bindings: dict, source_key: str = "core") -> tuple[dict, list[dict]]:
    return _import_bound_development(
        raw_ir,
        bindings,
        source_key,
        section="Магические навыки",
        category_as_context=True,
        required_heading_component="Магия",
    )


def promote_magic_development(imported: dict, bindings: dict, version: str = "3.69") -> dict:
    promoted = promote_regular_development(imported, bindings, version=version)
    binding_by_id = {str(item.get("id", "")): item for item in bindings.get("bindings", [])}
    for entry in promoted.get("entries", []):
        entry["section"] = "Магические навыки"
        binding = binding_by_id.get(str(entry.get("id", "")), {})
        if binding.get("repeatable"):
            entry["repeatable"] = True
    return promoted
