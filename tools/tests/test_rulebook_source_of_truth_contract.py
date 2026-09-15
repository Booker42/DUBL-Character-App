import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "shared/src/commonMain/resources"
CONFIG = ROOT / "rulesets/dubl-3.69/config.json"


def _load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def test_all_nonempty_canonical_runtime_catalogs_are_source_generated():
    config = _load(CONFIG)["domains"]

    required = {
        "conditions",
        "skills",
        "development_regular",
        "development_special",
        "development_ability_roots",
        "development_martial",
        "development_chi",
        "development_magic",
        "chi",
        "magic_equipment",
        "skill_effects",
    }
    assert required <= set(config)
    assert all(config[name]["status"] == "source_generated" for name in required)
    assert all(config[name].get("runtimeArtifact") for name in required)

    # The old monolithic Development catalog is retained only as an empty
    # compatibility shell. Canonical content lives in the six generated layers.
    assert _load(RES / "development_catalog.json").get("entries") == []


def test_skill_effect_rule_text_is_not_review_document_authority_anymore():
    effects = _load(RES / "skill_effects_catalog.json")["effects"]
    assert len(effects) == 283
    assert all(effect.get("developmentId") for effect in effects)
    assert all(effect.get("sourceRefs") for effect in effects)
