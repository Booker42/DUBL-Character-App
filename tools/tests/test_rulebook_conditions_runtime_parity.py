from __future__ import annotations

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PROMOTED = ROOT / "shared/src/commonMain/resources/conditions_catalog.json"
KOTLIN = ROOT / "shared/src/commonMain/kotlin/com/dubl/character/android/model/CharacterSheetExtras.kt"
ANDROID = ROOT / "app/src/main/java/com/dubl/character/android/ui/screens/OverviewScreen.kt"
DESKTOP = ROOT / "desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt"


def _enum_titles() -> list[str]:
    text = KOTLIN.read_text(encoding="utf-8")
    block = text.split("enum class CharacterConditionId", 1)[1].split("enum class CharacterSheetResourceId", 1)[0]
    return re.findall(r'^\s*[A-Z_]+\(\s*"([^"]+)"\s*\)', block, flags=re.M)


def test_promoted_rulebook_conditions_match_runtime_condition_ids_and_own_the_text():
    payload = json.loads(PROMOTED.read_text(encoding="utf-8"))
    source_titles = [item["name"] for item in payload["conditions"]]
    assert source_titles == _enum_titles()
    assert len(source_titles) == len(set(source_titles)) == 23
    assert all(item["description"].strip() for item in payload["conditions"])
    assert all(ref.startswith("core:") for item in payload["conditions"] for ref in item["sourceRefs"])

    enum_text = KOTLIN.read_text(encoding="utf-8")
    assert "rulesSummary" not in enum_text
    assert "В разделе состояний отдельно не расписан" not in enum_text


def test_android_and_desktop_condition_details_use_shared_condition_catalog():
    android = ANDROID.read_text(encoding="utf-8")
    desktop = DESKTOP.read_text(encoding="utf-8")
    assert "ConditionCatalogRepository" in android
    assert "conditionCatalog.summary(condition)" in android
    assert "state.conditionCatalog.summary(condition)" in desktop
    assert ".rulesSummary" not in android
    assert ".rulesSummary" not in desktop
