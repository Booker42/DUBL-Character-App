#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from .diagnostics import apply_resolutions

LIST_KEYS = {
    "development": ["entries"],
    "development_regular": ["entries"],
    "chi": ["schools", "techniques"],
    "magic_equipment": ["spells", "gear"],
    "skill_effects": ["effects"],
    "conditions": ["conditions", "mechanics"],
    "skills": ["skills"],
}


def _load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def validate_bundle(root: Path) -> list[str]:
    root = Path(root)
    errors: list[str] = []
    try:
        manifest = _load(root / "manifest.json")
        diagnostics_root = _load(root / "diagnostics.json")
        resolutions_root = _load(root / "resolutions.json")
        source_index = _load(root / "source/source_index.json")
    except (FileNotFoundError, json.JSONDecodeError) as exc:
        return [f"bundle structure error: {exc}"]

    if manifest.get("rulesetId") != "dubl" or manifest.get("rulesetVersion") != "3.69":
        errors.append("manifest ruleset identity must be dubl/3.69")

    declared_sources = manifest.get("sources")
    if declared_sources:
        for source_key, source_meta in declared_sources.items():
            try:
                raw = _load(root / f"source/{source_key}_raw_ir.json")
            except (FileNotFoundError, json.JSONDecodeError) as exc:
                errors.append(f"source {source_key} raw IR error: {exc}")
                continue
            if source_meta.get("sha256") != raw.get("source", {}).get("sha256"):
                errors.append(f"manifest source hash does not match Raw IR source hash for {source_key}")
        index_sources = set(source_index.get("sources", []))
        if index_sources != set(declared_sources):
            errors.append("source index sources do not match manifest sources")
    else:
        try:
            raw = _load(root / "source/raw_ir.json")
        except (FileNotFoundError, json.JSONDecodeError) as exc:
            return [f"bundle structure error: {exc}"]
        if manifest.get("source", {}).get("sha256") != raw.get("source", {}).get("sha256"):
            errors.append("manifest source hash does not match Raw IR source hash")

    source_ids = set(source_index.get("blocks", {}).keys())
    if not source_ids and not declared_sources:
        source_ids = {b.get("id") for b in raw.get("blocks", [])}

    for domain, meta in manifest.get("domains", {}).items():
        output = meta.get("output")
        if not output:
            errors.append(f"domain {domain} has no output")
            continue
        try:
            content = _load(root / output)
        except (FileNotFoundError, json.JSONDecodeError) as exc:
            errors.append(f"domain {domain} content error: {exc}")
            continue
        for key in LIST_KEYS.get(domain, []):
            seen: set[str] = set()
            for item in content.get(key, []):
                item_id = item.get("id")
                if not item_id:
                    errors.append(f"{domain}:{key} entity missing id")
                    continue
                if item_id in seen:
                    errors.append(f"duplicate id {item_id} in {domain}:{key}")
                seen.add(item_id)
                for ref in item.get("sourceRefs", []):
                    if ref not in source_ids:
                        errors.append(f"unknown source ref {ref} on {domain}:{item_id}")
                for ref in item.get("sourceCandidates", []):
                    if ref not in source_ids:
                        errors.append(f"unknown source ref {ref} on {domain}:{item_id}")

    diagnostics = diagnostics_root.get("diagnostics", [])
    diagnostic_ids = {d.get("id") for d in diagnostics}
    resolutions = resolutions_root.get("resolutions", [])
    for resolution in resolutions:
        diag_id = resolution.get("diagnosticId")
        if diag_id not in diagnostic_ids:
            errors.append(f"resolution references unknown diagnostic {diag_id}")
        if not resolution.get("decision") or not resolution.get("rationale"):
            errors.append(f"resolution {diag_id} requires decision and rationale")

    remaining, _ = apply_resolutions(diagnostics, resolutions)
    for diagnostic in remaining:
        if diagnostic.get("severity") == "error":
            errors.append(f"unresolved error diagnostic {diagnostic.get('id')}")
        for ref in diagnostic.get("sourceRefs", []):
            if ref not in source_ids:
                errors.append(f"diagnostic {diagnostic.get('id')} references unknown source ref {ref}")
    return errors


def main() -> None:
    parser = argparse.ArgumentParser(description="Validate a generated DUBL ruleset bundle")
    parser.add_argument("bundle", type=Path)
    args = parser.parse_args()
    errors = validate_bundle(args.bundle)
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        raise SystemExit(1)
    print("OK: ruleset bundle is structurally valid")


if __name__ == "__main__":
    main()
