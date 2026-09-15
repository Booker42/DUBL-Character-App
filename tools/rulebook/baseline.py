from __future__ import annotations

from collections import Counter


def make_baseline(manifest: dict, diagnostics: list[dict]) -> dict:
    return {
        "schemaVersion": 1,
        "rulesetId": "dubl",
        "rulesetVersion": "3.69",
        "sourceHashes": {k: v.get("sha256") for k, v in sorted(manifest.get("sources", {}).items())},
        "sourceStats": dict(manifest.get("sourceStats", {})),
        "domainCoverage": {
            k: dict(v.get("coverage", {}))
            for k, v in sorted(manifest.get("domains", {}).items())
        },
        "diagnosticsByKind": dict(sorted(Counter(d.get("kind", "unknown") for d in diagnostics).items())),
        "diagnosticsBySeverity": dict(sorted(Counter(d.get("severity", "unknown") for d in diagnostics).items())),
    }


def compare_baseline(baseline: dict, manifest: dict, diagnostics: list[dict]) -> list[str]:
    current = make_baseline(manifest, diagnostics)
    errors: list[str] = []
    expected_hashes = baseline.get("sourceHashes", {})
    for key in sorted(set(expected_hashes) | set(current["sourceHashes"])):
        if expected_hashes.get(key) != current["sourceHashes"].get(key):
            errors.append(f"source hash drift for {key}: expected {expected_hashes.get(key)!r}, got {current['sourceHashes'].get(key)!r}")
    if baseline.get("sourceStats") != current.get("sourceStats"):
        errors.append(f"source stats drift: expected {baseline.get('sourceStats')}, got {current.get('sourceStats')}")
    expected_coverage = baseline.get("domainCoverage", {})
    for domain in sorted(set(expected_coverage) | set(current["domainCoverage"])):
        if expected_coverage.get(domain) != current["domainCoverage"].get(domain):
            errors.append(f"coverage drift for {domain}: expected {expected_coverage.get(domain)}, got {current['domainCoverage'].get(domain)}")
    if baseline.get("diagnosticsByKind") != current.get("diagnosticsByKind"):
        errors.append(f"diagnostic kind drift: expected {baseline.get('diagnosticsByKind')}, got {current.get('diagnosticsByKind')}")
    if baseline.get("diagnosticsBySeverity") != current.get("diagnosticsBySeverity"):
        errors.append(f"diagnostic severity drift: expected {baseline.get('diagnosticsBySeverity')}, got {current.get('diagnosticsBySeverity')}")
    return errors
