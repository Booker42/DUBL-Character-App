from __future__ import annotations

from copy import deepcopy
import re

from .diagnostics import make_diagnostic
from .source_index import normalize_lookup


def entity_name(entity: dict) -> str:
    for key in ("name", "sourceName", "title"):
        value = entity.get(key)
        if isinstance(value, str) and value.strip():
            return value.strip()
    return str(entity.get("id", ""))


def source_policy(domain: str, entity: dict) -> list[str]:
    entity_id = str(entity.get("id", ""))
    section = normalize_lookup(str(entity.get("section", "")))
    if domain == "chi":
        return ["melee"]
    if domain == "development":
        if entity_id.startswith(("martial_", "chi_")) or section in {"боевые искусства", "ци"}:
            return ["melee"]
        return ["core"]
    if domain == "magic_equipment":
        explicit_source = normalize_lookup(str(entity.get("source", "")))
        if explicit_source in {"книга архимага", "archmage"} or entity_id.startswith("archmage_"):
            return ["archmage"]
        return ["core"]
    if domain == "skill_effects":
        return ["core"]
    return ["core"]


def _filter_sources(refs: list[str], allowed_sources: list[str] | None) -> list[str]:
    if not allowed_sources:
        return list(refs)
    allowed = set(allowed_sources)
    qualified = [ref for ref in refs if ":" in ref]
    if not qualified:
        return list(refs)
    return [ref for ref in refs if ref.split(":", 1)[0] in allowed]


def find_source_candidates(name: str, source_index: dict, allowed_sources: list[str] | None = None) -> tuple[list[str], str]:
    norm = normalize_lookup(name)
    headings = _filter_sources(source_index.get("headings", {}).get(norm, []), allowed_sources)
    if headings:
        return list(headings), "heading-exact"
    exact = _filter_sources(source_index.get("text", {}).get(norm, []), allowed_sources)
    if exact:
        return list(dict.fromkeys(exact)), "text-exact"
    if len(norm) >= 4:
        pattern = re.compile(r"(?<!\w)" + re.escape(norm) + r"(?!\w)")
        refs: list[str] = []
        for ref, meta in source_index.get("blocks", {}).items():
            if allowed_sources and ":" in ref and ref.split(":", 1)[0] not in set(allowed_sources):
                continue
            if pattern.search(meta.get("normalizedSearchText", "")):
                refs.append(ref)
        refs = list(dict.fromkeys(refs))
        if refs:
            return refs, "inline-unique" if len(refs) == 1 else "inline-multiple"
    return [], "none"


def attach_provenance(root: dict, list_keys: list[str], domain: str, source_index: dict) -> tuple[dict, list[dict], dict]:
    out = deepcopy(root)
    diagnostics: list[dict] = []
    total = linked = ambiguous = missing = 0
    available_sources = set(source_index.get("sources", []))
    for list_key in list_keys:
        for entity in out.get(list_key, []):
            total += 1
            name = entity_name(entity)
            policy = source_policy(domain, entity)
            absent = [source for source in policy if available_sources and source not in available_sources]
            if absent:
                subject = f"{domain}:{entity.get('id', name)}"
                diagnostics.append(make_diagnostic(
                    "source-unavailable", subject, [], "warning",
                    f"Required rulebook source(s) are not registered: {', '.join(absent)}.",
                    sourcePolicy=policy,
                ))
                missing += 1
                continue
            candidates, method = find_source_candidates(name, source_index, policy)
            subject = f"{domain}:{entity.get('id', name)}"
            if len(candidates) == 1:
                entity["sourceRefs"] = candidates
                entity["sourceMatch"] = method
                entity["sourcePolicy"] = policy
                linked += 1
            elif len(candidates) > 1:
                entity["sourceCandidates"] = candidates
                entity["sourceMatch"] = method
                entity["sourcePolicy"] = policy
                diagnostics.append(make_diagnostic(
                    "ambiguous-source", subject, candidates, "warning",
                    f"Multiple rulebook source blocks match {name!r} within source policy {policy}; no source was selected automatically.",
                    sourcePolicy=policy,
                ))
                ambiguous += 1
            else:
                diagnostics.append(make_diagnostic(
                    "missing-source", subject, [], "warning",
                    f"No exact rulebook source block matches {name!r} within source policy {policy}.",
                    sourcePolicy=policy,
                ))
                missing += 1
    return out, diagnostics, {"total": total, "linked": linked, "ambiguous": ambiguous, "missing": missing}
