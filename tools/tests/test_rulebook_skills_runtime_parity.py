from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SKILLS = ROOT / "shared/src/commonMain/kotlin/com/dubl/character/android/model/SkillModels.kt"
GENERATED = ROOT / "shared/src/commonMain/kotlin/com/dubl/character/android/model/GeneratedSkillCatalog.kt"


def test_shared_skill_catalog_delegates_to_rulebook_generated_artifact():
    source = SKILLS.read_text(encoding="utf-8")
    generated = GENERATED.read_text(encoding="utf-8")

    assert "val rankCosts: List<Int> = GeneratedSkillCatalog.rankCosts" in source
    assert "val definitions: List<SkillDefinition> = GeneratedSkillCatalog.definitions" in source
    assert 'skill("athletics"' not in source
    assert 'skill("computers"' not in source
    assert "internal object GeneratedSkillCatalog" in generated
    assert 'id = "computers"' in generated
    assert "untrained = UntrainedRule.UNSPECIFIED" in generated
