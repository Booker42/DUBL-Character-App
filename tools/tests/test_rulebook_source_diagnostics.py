import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.source_diagnostics import detect_source_diagnostics


def test_detects_draft_markers_and_variant_duplicate_headings():
    raw = {
        "blocks": [
            {"id":"p-000001","kind":"paragraph","order":1,"text":"Spell","normalizedText":"spell","headingLevel":5,"headingPath":["Magic","Spell"]},
            {"id":"p-000002","kind":"paragraph","order":2,"text":"Damage 5","normalizedText":"damage 5","headingLevel":None,"headingPath":["Magic","Spell"]},
            {"id":"p-000003","kind":"paragraph","order":3,"text":"Spell","normalizedText":"spell","headingLevel":5,"headingPath":["Other","Spell"]},
            {"id":"p-000004","kind":"paragraph","order":4,"text":"Damage 7","normalizedText":"damage 7","headingLevel":None,"headingPath":["Other","Spell"]},
            {"id":"p-000005","kind":"paragraph","order":5,"text":"Позже напишу ???","normalizedText":"позже напишу ???","headingLevel":None,"headingPath":["Other","Spell"]},
        ]
    }
    diags = detect_source_diagnostics({"core": raw})
    kinds = {d["kind"] for d in diags}
    assert "duplicate-heading-variant" in kinds
    assert "draft-marker" in kinds
    dup = next(d for d in diags if d["kind"] == "duplicate-heading-variant")
    assert dup["sourceRefs"] == ["core:p-000001", "core:p-000003"]


def test_identical_duplicate_heading_is_info_not_error():
    raw = {"blocks":[
        {"id":"p-000001","kind":"paragraph","order":1,"text":"X","normalizedText":"x","headingLevel":3,"headingPath":["X"]},
        {"id":"p-000002","kind":"paragraph","order":2,"text":"same","normalizedText":"same","headingLevel":None,"headingPath":["X"]},
        {"id":"p-000003","kind":"paragraph","order":3,"text":"X","normalizedText":"x","headingLevel":3,"headingPath":["X"]},
        {"id":"p-000004","kind":"paragraph","order":4,"text":"same","normalizedText":"same","headingLevel":None,"headingPath":["X"]},
    ]}
    diags=detect_source_diagnostics({"core":raw})
    d=next(x for x in diags if x["kind"] == "duplicate-heading-repeat")
    assert d["severity"] == "info"
