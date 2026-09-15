from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/linux-appimage.yml"
BUILDER = ROOT / "tools/rulebook/build_ruleset.py"

RULEBOOK_TESTS = (
    "test_rulebook_raw_ir.py",
    "test_rulebook_source_index.py",
    "test_rulebook_diagnostics.py",
    "test_rulebook_source_diagnostics.py",
    "test_rulebook_ruleset_build.py",
    "test_rulebook_validator.py",
    "test_rulebook_baseline.py",
    "test_rulebook_baseline_cli.py",
    "test_rulebook_conditions_import.py",
    "test_rulebook_conditions_runtime_parity.py",
    "test_rulebook_skills_import.py",
    "test_rulebook_skill_kotlin_renderer.py",
    "test_rulebook_skills_runtime_parity.py",
    "test_rulebook_development_import.py",
    "test_rulebook_ability_roots_import.py",
    "test_rulebook_martial_arts_import.py",
    "test_rulebook_chi_import.py",
    "test_rulebook_magic_import.py",
    "test_rulebook_equipment_import.py",
    "test_rulebook_magic_equipment_runtime_promotion.py",
    "test_magic_unresolved_escape_hatch.py",
    "test_development_rulebook_unresolved_runtime.py",
    "test_skill_untrained_contract.py",
    "test_skill_local_override_contract.py",
    "test_skill_override_persistence.py",
    "test_skill_local_override_ui.py",
    "test_rulebook_development_runtime_promotion.py",
    "test_rulebook_release_gate.py",
)


def test_linux_release_gate_runs_rulebook_pipeline_contracts():
    text = WORKFLOW.read_text(encoding="utf-8")
    missing = [name for name in RULEBOOK_TESTS if name not in text]
    assert not missing, f"rulebook pipeline tests missing from Linux release gate: {missing}"


def test_rulebook_builder_defaults_to_ignored_build_output_not_committed_ruleset_tree():
    text = BUILDER.read_text(encoding="utf-8")
    assert 'default=Path("build/rulesets/dubl-3.69")' in text
    assert 'default=Path("rulesets/dubl-3.69")' not in text
