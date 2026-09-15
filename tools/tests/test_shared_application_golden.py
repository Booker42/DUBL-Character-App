from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GOLDEN_TEST = (
    ROOT
    / "shared/src/commonTest/kotlin/com/dubl/character/android/application/SharedApplicationGoldenTest.kt"
)
WORKFLOW = ROOT / ".github/workflows/linux-appimage.yml"


def test_typed_golden_scenarios_live_in_shared_common_test():
    assert GOLDEN_TEST.exists()
    text = GOLDEN_TEST.read_text(encoding="utf-8")
    assert "class SharedApplicationGoldenTest" in text
    assert "DublApplication" in text
    assert "kotlin.test.Test" in text
    for name in (
        "characterProfileAndResourceNormalization",
        "skillsAndSheetPreferenceShareOneApplicationBoundary",
        "chiMagicAndEquipmentMutateCanonicalCharacterState",
        "sheetExtrasAreTypedStateAndIsolatedPerCharacter",
        "sharedUndoRevertsSemanticMutationWithoutTouchingLaterNonUndoableState",
        "creationEconomyAndCompletionStayCanonical",
        "skillLifecycleCoversOverridesHideRestoreAndDelete",
        "specializedSkillLifecyclePreservesTemplateIdentity",
        "customDevelopmentLifecyclePersistsOwnershipAndRemoval",
        "chiSpendRestoreAndUndoStayShared",
        "magicLifecycleCoversSchoolSpellLearningAndManaUndo",
        "equipmentLifecycleCoversQuantityCarriedLoadAndRemoval",
        "customResourceLifecycleClampsUpdatesSpendsAndRemoves",
        "conditionGroupingAndPreferencesSurviveApplicationRestart",
        "characterRosterDeletionRemovesExtrasAndKeepsValidActiveCharacter",
    ):
        assert f"fun {name}()" in text


def test_release_gate_runs_golden_scenarios_with_project_kotlin_toolchain():
    workflow = WORKFLOW.read_text(encoding="utf-8")
    assert ":shared:desktopTest" in workflow
    assert "run_shared_application_golden.sh" not in workflow
