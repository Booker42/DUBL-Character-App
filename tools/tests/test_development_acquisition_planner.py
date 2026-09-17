from pathlib import Path
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SHARED_MODEL = ROOT / "shared/src/commonMain/kotlin/com/dubl/character/android/model/DevelopmentAcquisition.kt"
SHARED_APP = ROOT / "shared/src/commonMain/kotlin/com/dubl/character/android/application/DevelopmentApplication.kt"
HARNESS = ROOT / "tools/tests/kotlin/DevelopmentAcquisitionHarness.kt"
ANDROID_CONTROLLER = ROOT / "app/src/main/java/com/dubl/character/android/state/CharacterController.kt"
DESKTOP_STATE = ROOT / "desktopApp/src/main/kotlin/com/dubl/character/desktop/DesktopAppState.kt"


def test_shared_planner_exposes_recursive_plan_choices_and_reverse_unlocks():
    src = SHARED_MODEL.read_text(encoding="utf-8")
    assert "class DevelopmentAcquisitionPlanner" in src
    assert "data class DevelopmentAcquisitionRequest" in src
    assert "sealed interface DevelopmentAcquisitionStep" in src
    assert "data class DevelopmentAcquisitionChoice" in src
    assert "fun plan(request: DevelopmentAcquisitionRequest)" in src
    assert "fun unlocks(entryId: String)" in src
    assert "satisfyAndAcquireDevelopment" in src
    assert "choiceSelections" in src
    assert "unresolvedRequirements" in src
    assert "projectedCharacter" in src


def test_shared_application_applies_the_resolved_plan_atomically_with_one_undo_boundary():
    src = SHARED_APP.read_text(encoding="utf-8")
    acquire = src.split("fun acquire(", 1)[1]
    assert "val before = session.active" in acquire
    assert "DevelopmentAcquisitionPlanner(before, catalog).plan(request)" in acquire
    assert "session.updateActive { plan.projectedCharacter }" in acquire
    assert "undo.record { session.updateActive { before } }" in acquire
    assert "if (!plan.canApply)" in acquire


def test_kotlin_harness_covers_recursive_or_batch_and_undo_scenarios():
    src = HARNESS.read_text(encoding="utf-8")
    assert "DEVELOPMENT_ACQUISITION_OK" in src
    assert "choiceSelections" in src
    assert "includeTarget = false" in src
    assert "application.acquire" in src
    assert "undo" in src.lower()


def test_platform_adapters_expose_the_same_shared_acquisition_operation():
    android = ANDROID_CONTROLLER.read_text(encoding="utf-8")
    desktop = DESKTOP_STATE.read_text(encoding="utf-8")
    for text in (android, desktop):
        assert "fun acquireDevelopment" in text
        assert "application.development.acquire" in text


def test_kotlin_harness_compiles_and_executes():
    kotlinc = shutil.which("kotlinc")
    assert kotlinc is not None
    shared = ROOT / "shared/src/commonMain/kotlin"
    sources = sorted((shared / "com/dubl/character/android/model").glob("*.kt"))
    sources += [
        shared / "com/dubl/character/android/data/CharacterStore.kt",
        shared / "com/dubl/character/android/state/CharacterSession.kt",
        shared / "com/dubl/character/android/application/ApplicationUndoManager.kt",
        shared / "com/dubl/character/android/application/DevelopmentApplication.kt",
        HARNESS,
    ]
    with tempfile.TemporaryDirectory() as td:
        jar = Path(td) / "development-acquisition.jar"
        compiled = subprocess.run(
            [kotlinc, "-language-version", "2.0", *map(str, sources), "-include-runtime", "-d", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=90,
        )
        assert compiled.returncode == 0, compiled.stderr
        result = subprocess.run(
            ["java", "-jar", str(jar)],
            cwd=ROOT,
            capture_output=True,
            text=True,
            timeout=30,
        )
        assert result.returncode == 0, result.stderr + result.stdout
        assert "DEVELOPMENT_ACQUISITION_OK" in result.stdout
