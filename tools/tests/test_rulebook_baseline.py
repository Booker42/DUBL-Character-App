import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.baseline import make_baseline, compare_baseline


def sample_manifest():
    return {
        "sources": {"core":{"sha256":"a"}, "melee":{"sha256":"b"}},
        "sourceStats": {"blocks":10,"paragraphs":8,"tables":2,"headings":3},
        "domains": {"development":{"coverage":{"total":2,"linked":1,"ambiguous":1,"missing":0}}},
    }


def sample_diagnostics():
    return [{"kind":"ambiguous-source","severity":"warning"},{"kind":"draft-marker","severity":"warning"}]


def test_make_and_compare_baseline_are_exact_and_deterministic():
    baseline = make_baseline(sample_manifest(), sample_diagnostics())
    assert baseline["sourceHashes"] == {"core":"a","melee":"b"}
    assert baseline["diagnosticsByKind"] == {"ambiguous-source":1,"draft-marker":1}
    assert compare_baseline(baseline, sample_manifest(), sample_diagnostics()) == []


def test_compare_baseline_reports_source_and_coverage_drift():
    baseline = make_baseline(sample_manifest(), sample_diagnostics())
    changed = sample_manifest()
    changed["sources"]["core"]["sha256"] = "changed"
    changed["domains"]["development"]["coverage"]["missing"] = 1
    errors = compare_baseline(baseline, changed, sample_diagnostics())
    assert any("source hash drift" in e for e in errors)
    assert any("coverage drift" in e for e in errors)
