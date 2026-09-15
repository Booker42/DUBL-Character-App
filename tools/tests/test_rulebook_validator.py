import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.rulebook.validate_ruleset import validate_bundle


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data), encoding="utf-8")


def make_bundle(root: Path):
    source = {"filename":"x.docx","sha256":"abc","sizeBytes":1}
    write(root/"source/raw_ir.json", {"schemaVersion":1,"source":source,"stats":{},"blocks":[{"id":"p-000001","kind":"paragraph","order":1,"text":"A","normalizedText":"a","style":"Heading 1","headingLevel":1,"headingPath":["A"]}]})
    write(root/"source/source_index.json", {"schemaVersion":1,"headings":{},"text":{},"blocks":{"p-000001":{}}})
    write(root/"content/development.json", {"entries":[{"id":"a","name":"A","sourceRefs":["p-000001"]}]})
    write(root/"content/chi.json", {"schools":[],"techniques":[]})
    write(root/"content/magic_equipment.json", {"spells":[],"gear":[]})
    write(root/"content/skill_effects.json", {"effects":[]})
    write(root/"diagnostics.json", {"schemaVersion":1,"ruleset":"dubl-3.69","diagnostics":[]})
    write(root/"resolutions.json", {"schemaVersion":1,"ruleset":"dubl-3.69","resolutions":[]})
    write(root/"manifest.json", {"schemaVersion":1,"rulesetId":"dubl","rulesetVersion":"3.69","source":source,"domains":{
        "development":{"output":"content/development.json"},"chi":{"output":"content/chi.json"},"magic_equipment":{"output":"content/magic_equipment.json"},"skill_effects":{"output":"content/skill_effects.json"}
    }})


def test_valid_bundle_passes(tmp_path: Path):
    make_bundle(tmp_path)
    assert validate_bundle(tmp_path) == []


def test_validator_rejects_duplicate_ids_and_bad_source_refs(tmp_path: Path):
    make_bundle(tmp_path)
    write(tmp_path/"content/development.json", {"entries":[{"id":"a","sourceRefs":["missing"]},{"id":"a","sourceRefs":[]}]})
    errors = validate_bundle(tmp_path)
    assert any("duplicate id" in e for e in errors)
    assert any("unknown source ref" in e for e in errors)


def test_validator_rejects_stale_hash_unknown_resolution_and_unresolved_error(tmp_path: Path):
    make_bundle(tmp_path)
    manifest = json.loads((tmp_path/"manifest.json").read_text())
    manifest["source"]["sha256"] = "stale"
    write(tmp_path/"manifest.json", manifest)
    write(tmp_path/"diagnostics.json", {"schemaVersion":1,"ruleset":"dubl-3.69","diagnostics":[{"id":"diag_real","severity":"error","kind":"x","subject":"x","message":"x","sourceRefs":[]}]})
    write(tmp_path/"resolutions.json", {"schemaVersion":1,"ruleset":"dubl-3.69","resolutions":[{"diagnosticId":"diag_missing","decision":"x","rationale":"x"}]})
    errors = validate_bundle(tmp_path)
    assert any("source hash" in e for e in errors)
    assert any("unknown diagnostic" in e for e in errors)
    assert any("unresolved error diagnostic diag_real" in e for e in errors)

def test_validator_accepts_multi_source_manifest_with_qualified_refs(tmp_path: Path):
    sources = {
        "core": {"filename":"core.docx","sha256":"corehash","sizeBytes":1},
        "melee": {"filename":"melee.docx","sha256":"meleehash","sizeBytes":1},
    }
    write(tmp_path/"source/core_raw_ir.json", {"schemaVersion":1,"source":sources["core"],"stats":{},"blocks":[{"id":"p-000001","kind":"paragraph","order":1,"text":"A","normalizedText":"a","style":"Heading 1","headingLevel":1,"headingPath":["A"]}]})
    write(tmp_path/"source/melee_raw_ir.json", {"schemaVersion":1,"source":sources["melee"],"stats":{},"blocks":[{"id":"p-000001","kind":"paragraph","order":1,"text":"M","normalizedText":"m","style":"Heading 1","headingLevel":1,"headingPath":["M"]}]})
    write(tmp_path/"source/source_index.json", {"schemaVersion":1,"sources":["core","melee"],"headings":{},"text":{},"blocks":{"core:p-000001":{},"melee:p-000001":{}}})
    write(tmp_path/"content/development.json", {"entries":[{"id":"a","sourceRefs":["core:p-000001"]},{"id":"m","sourceRefs":["melee:p-000001"]}]})
    write(tmp_path/"content/chi.json", {"schools":[],"techniques":[]})
    write(tmp_path/"content/magic_equipment.json", {"spells":[],"gear":[]})
    write(tmp_path/"content/skill_effects.json", {"effects":[]})
    write(tmp_path/"diagnostics.json", {"schemaVersion":1,"ruleset":"dubl-3.69","diagnostics":[]})
    write(tmp_path/"resolutions.json", {"schemaVersion":1,"ruleset":"dubl-3.69","resolutions":[]})
    write(tmp_path/"manifest.json", {"schemaVersion":1,"rulesetId":"dubl","rulesetVersion":"3.69","sources":sources,"domains":{
        "development":{"output":"content/development.json"},"chi":{"output":"content/chi.json"},"magic_equipment":{"output":"content/magic_equipment.json"},"skill_effects":{"output":"content/skill_effects.json"}
    }})
    assert validate_bundle(tmp_path) == []
