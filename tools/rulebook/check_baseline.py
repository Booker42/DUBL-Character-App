#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
from pathlib import Path

from .baseline import compare_baseline, make_baseline
from .render_skill_catalog_kotlin import render_skill_catalog_kotlin


def _load(path: Path) -> dict:
    return json.loads(path.read_text(encoding="utf-8"))


def _compiled_artifact_text(meta: dict, payload: dict) -> str | None:
    compiled = meta.get("compiledArtifact")
    if not compiled:
        return None
    artifact_type = compiled.get("type")
    if artifact_type == "skill_catalog_kotlin":
        return render_skill_catalog_kotlin(payload)
    raise ValueError(f"unsupported compiledArtifact type: {artifact_type}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Check or explicitly update the DUBL rulebook import baseline")
    parser.add_argument("bundle", type=Path, help="Generated bundle directory, e.g. build/rulesets/dubl-3.69")
    parser.add_argument("--baseline", type=Path, default=Path("rulesets/dubl-3.69/baseline.json"))
    parser.add_argument("--repo-root", type=Path, default=Path("."), help="Repository root used to resolve promoted runtimeArtifact paths")
    parser.add_argument("--update", action="store_true", help="Explicitly replace the baseline after reviewing source/import drift")
    args = parser.parse_args()

    manifest = _load(args.bundle / "manifest.json")
    diagnostics = _load(args.bundle / "diagnostics.json").get("diagnostics", [])
    current = make_baseline(manifest, diagnostics)

    generated_domains = {
        domain: meta
        for domain, meta in manifest.get("domains", {}).items()
        if meta.get("status") == "source_generated" and meta.get("output")
    }

    def promoted_path(domain: str, meta: dict) -> Path:
        runtime_artifact = meta.get("runtimeArtifact")
        if runtime_artifact:
            return args.repo_root / runtime_artifact
        return args.baseline.parent / "generated" / f"{domain}.json"

    if args.update:
        args.baseline.parent.mkdir(parents=True, exist_ok=True)
        args.baseline.write_text(json.dumps(current, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        for domain, meta in sorted(generated_domains.items()):
            payload = _load(args.bundle / meta["output"])
            target = promoted_path(domain, meta)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(
                json.dumps(payload, ensure_ascii=False, indent=2) + "\n",
                encoding="utf-8",
            )
            compiled = meta.get("compiledArtifact")
            compiled_text = _compiled_artifact_text(meta, payload)
            if compiled and compiled_text is not None:
                compiled_target = args.repo_root / compiled["path"]
                compiled_target.parent.mkdir(parents=True, exist_ok=True)
                compiled_target.write_text(compiled_text, encoding="utf-8")
        print(f"UPDATED: {args.baseline}")
        return

    if not args.baseline.exists():
        print(f"ERROR: baseline does not exist: {args.baseline}")
        raise SystemExit(1)

    expected = _load(args.baseline)
    errors = compare_baseline(expected, manifest, diagnostics)
    for domain, meta in sorted(generated_domains.items()):
        committed_path = promoted_path(domain, meta)
        if not committed_path.exists():
            errors.append(f"generated artifact missing for {domain}: {committed_path}")
            continue
        built_payload = _load(args.bundle / meta["output"])
        committed_payload = _load(committed_path)
        if built_payload != committed_payload:
            errors.append(f"generated artifact drift for {domain}")
        compiled = meta.get("compiledArtifact")
        compiled_text = _compiled_artifact_text(meta, built_payload)
        if compiled and compiled_text is not None:
            compiled_path = args.repo_root / compiled["path"]
            if not compiled_path.exists():
                errors.append(f"compiled artifact missing for {domain}: {compiled_path}")
            elif compiled_path.read_text(encoding="utf-8") != compiled_text:
                errors.append(f"compiled artifact drift for {domain}")
    if errors:
        for error in errors:
            print(f"DRIFT: {error}")
        raise SystemExit(1)
    print("OK: rulebook import matches committed baseline")


if __name__ == "__main__":
    main()
