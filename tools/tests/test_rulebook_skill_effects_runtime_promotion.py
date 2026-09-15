import json
from pathlib import Path

from tools.rulebook.import_skill_effects import import_skill_effects

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "shared/src/commonMain/resources"
BINDINGS = ROOT / "rulesets/dubl-3.69/bindings/skill_effects.json"
DEV_LAYERS = [
    "development_regular_catalog.json",
    "development_special_catalog.json",
    "development_ability_roots_catalog.json",
    "development_martial_catalog.json",
    "development_chi_catalog.json",
    "development_magic_catalog.json",
]


def _load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def test_runtime_skill_effect_catalog_is_generated_from_development_and_annotation_bindings():
    entries = []
    for name in DEV_LAYERS:
        entries.extend(_load(RES / name).get("entries", []))
    generated, diagnostics = import_skill_effects(
        {"entries": entries},
        _load(BINDINGS),
    )

    assert diagnostics == []
    assert len(generated["effects"]) == 283
    assert generated == _load(RES / "skill_effects_catalog.json")
