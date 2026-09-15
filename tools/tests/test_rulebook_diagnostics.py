import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.diagnostics import make_diagnostic, apply_resolutions


def test_diagnostic_ids_are_stable_and_resolution_only_resolves_named_issue():
    a = make_diagnostic("conflict", "spell:Агония", ["p-1", "p-2"], "error", "Different spell definitions")
    b = make_diagnostic("missing-source", "gear:X", [], "warning", "Missing")
    assert a["id"] == make_diagnostic("conflict", "spell:Агония", ["p-2", "p-1"], "error", "Different wording")["id"]

    remaining, applied = apply_resolutions([a, b], [{"diagnosticId": a["id"], "decision": "use p-1", "rationale": "explicit decision"}])
    assert [d["id"] for d in remaining] == [b["id"]]
    assert applied[0]["diagnosticId"] == a["id"]


def test_unknown_resolution_does_not_hide_any_diagnostic():
    d = make_diagnostic("conflict", "x", ["p-1"], "error", "x")
    remaining, applied = apply_resolutions([d], [{"diagnosticId": "diag_unknown", "decision": "x", "rationale": "x"}])
    assert remaining == [d]
    assert applied == []
