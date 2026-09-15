from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def _write_bundle(root: Path, source_hash: str = "abc") -> None:
    root.mkdir(parents=True, exist_ok=True)
    (root / "manifest.json").write_text(json.dumps({
        "rulesetId": "dubl",
        "rulesetVersion": "3.69",
        "sources": {"core": {"sha256": source_hash}},
        "sourceStats": {"blocks": 1, "paragraphs": 1, "tables": 0, "headings": 1},
        "domains": {"conditions": {
            "status": "source_generated",
            "output": "content/conditions.json",
            "coverage": {"total": 1, "linked": 1, "ambiguous": 0, "missing": 0},
        }},
    }), encoding="utf-8")
    (root / "content").mkdir(exist_ok=True)
    (root / "content/conditions.json").write_text(json.dumps({"conditions": [{"id": source_hash, "name": "Test"}]}), encoding="utf-8")
    (root / "diagnostics.json").write_text(json.dumps({"diagnostics": []}), encoding="utf-8")


def _run(*args: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, "-m", "tools.rulebook.check_baseline", *args],
        cwd=ROOT,
        text=True,
        capture_output=True,
    )


def test_baseline_cli_updates_checks_and_reports_drift(tmp_path: Path):
    bundle = tmp_path / "bundle"
    baseline = tmp_path / "baseline.json"
    _write_bundle(bundle)

    updated = _run(str(bundle), "--baseline", str(baseline), "--update")
    assert updated.returncode == 0, updated.stderr
    assert baseline.exists()
    generated = baseline.parent / "generated/conditions.json"
    assert generated.exists()

    checked = _run(str(bundle), "--baseline", str(baseline))
    assert checked.returncode == 0, checked.stdout + checked.stderr
    assert "OK" in checked.stdout

    _write_bundle(bundle, source_hash="changed")
    drifted = _run(str(bundle), "--baseline", str(baseline))
    assert drifted.returncode == 1
    assert "source hash drift" in drifted.stdout
    assert "generated artifact drift for conditions" in drifted.stdout


def test_baseline_cli_promotes_source_generated_domain_to_configured_runtime_artifact(tmp_path: Path):
    bundle = tmp_path / "bundle"
    _write_bundle(bundle)
    manifest = json.loads((bundle / "manifest.json").read_text(encoding="utf-8"))
    manifest["domains"]["conditions"]["runtimeArtifact"] = "shared/src/commonMain/resources/conditions_catalog.json"
    (bundle / "manifest.json").write_text(json.dumps(manifest), encoding="utf-8")

    repo = tmp_path / "repo"
    baseline = repo / "rulesets/dubl-3.69/baseline.json"
    updated = _run(str(bundle), "--baseline", str(baseline), "--repo-root", str(repo), "--update")
    assert updated.returncode == 0, updated.stdout + updated.stderr

    promoted = repo / "shared/src/commonMain/resources/conditions_catalog.json"
    assert promoted.exists()
    assert json.loads(promoted.read_text(encoding="utf-8")) == json.loads((bundle / "content/conditions.json").read_text(encoding="utf-8"))
    assert not (baseline.parent / "generated/conditions.json").exists()

    checked = _run(str(bundle), "--baseline", str(baseline), "--repo-root", str(repo))
    assert checked.returncode == 0, checked.stdout + checked.stderr

    promoted.write_text(json.dumps({"conditions": []}), encoding="utf-8")
    drifted = _run(str(bundle), "--baseline", str(baseline), "--repo-root", str(repo))
    assert drifted.returncode == 1
    assert "generated artifact drift for conditions" in drifted.stdout


def test_baseline_cli_promotes_skill_json_and_generated_kotlin_adapter(tmp_path: Path):
    bundle = tmp_path / "bundle"
    bundle.mkdir(parents=True)
    (bundle / "content").mkdir()
    skills = {
        "schemaVersion": 1,
        "source": "core",
        "rankCosts": [0, 10],
        "rankCostSourceRefs": ["core:t-1"],
        "skills": [{
            "id": "athletics",
            "name": "Атлетика",
            "description": "Прыжки",
            "auto6": "Да",
            "auto12": "Нет",
            "untrained": "YES",
            "category": "PHYSICAL",
            "defaultAttributeHint": "STRENGTH",
            "template": False,
            "sourceRefs": ["core:t-1"],
        }],
    }
    (bundle / "content/skills.json").write_text(json.dumps(skills, ensure_ascii=False), encoding="utf-8")
    (bundle / "manifest.json").write_text(json.dumps({
        "rulesetId": "dubl",
        "rulesetVersion": "3.69",
        "sources": {"core": {"sha256": "abc"}},
        "sourceStats": {"blocks": 1, "paragraphs": 0, "tables": 1, "headings": 1},
        "domains": {"skills": {
            "status": "source_generated",
            "output": "content/skills.json",
            "runtimeArtifact": "shared/src/commonMain/resources/skills_catalog.json",
            "compiledArtifact": {
                "type": "skill_catalog_kotlin",
                "path": "shared/src/commonMain/kotlin/com/dubl/character/android/model/GeneratedSkillCatalog.kt",
            },
            "coverage": {"total": 1, "linked": 1, "ambiguous": 0, "missing": 0},
        }},
    }), encoding="utf-8")
    (bundle / "diagnostics.json").write_text(json.dumps({"diagnostics": []}), encoding="utf-8")

    repo = tmp_path / "repo"
    baseline = repo / "rulesets/dubl-3.69/baseline.json"
    updated = _run(str(bundle), "--baseline", str(baseline), "--repo-root", str(repo), "--update")
    assert updated.returncode == 0, updated.stdout + updated.stderr

    runtime = repo / "shared/src/commonMain/resources/skills_catalog.json"
    compiled = repo / "shared/src/commonMain/kotlin/com/dubl/character/android/model/GeneratedSkillCatalog.kt"
    assert json.loads(runtime.read_text(encoding="utf-8")) == skills
    assert "internal object GeneratedSkillCatalog" in compiled.read_text(encoding="utf-8")
    assert 'id = "athletics"' in compiled.read_text(encoding="utf-8")

    checked = _run(str(bundle), "--baseline", str(baseline), "--repo-root", str(repo))
    assert checked.returncode == 0, checked.stdout + checked.stderr

    compiled.write_text("// drift\n", encoding="utf-8")
    drifted = _run(str(bundle), "--baseline", str(baseline), "--repo-root", str(repo))
    assert drifted.returncode == 1
    assert "compiled artifact drift for skills" in drifted.stdout
