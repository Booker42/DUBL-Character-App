from __future__ import annotations

import hashlib
import re


def _norm(value: str) -> str:
    value = value.replace("ё", "е").replace("Ё", "Е").replace("—", "-").replace("–", "-")
    return re.sub(r"\s+", " ", value).strip().lower()


def make_diagnostic(kind: str, subject: str, source_refs: list[str], severity: str, message: str, **extra) -> dict:
    refs = sorted(set(source_refs))
    key = "|".join([_norm(kind), _norm(subject), *refs])
    digest = hashlib.sha1(key.encode("utf-8")).hexdigest()[:16]
    item = {
        "id": f"diag_{digest}",
        "kind": kind,
        "subject": subject,
        "severity": severity,
        "message": message,
        "sourceRefs": refs,
    }
    item.update(extra)
    return item


def apply_resolutions(diagnostics: list[dict], resolutions: list[dict]) -> tuple[list[dict], list[dict]]:
    by_id = {item["id"]: item for item in diagnostics}
    applied: list[dict] = []
    resolved_ids: set[str] = set()
    for resolution in resolutions:
        diag_id = resolution.get("diagnosticId")
        if diag_id in by_id:
            applied.append(resolution)
            resolved_ids.add(diag_id)
    return [d for d in diagnostics if d["id"] not in resolved_ids], applied
