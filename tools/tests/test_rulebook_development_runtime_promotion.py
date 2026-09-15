import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / "shared/src/commonMain/resources"
CONFIG = ROOT / "rulesets/dubl-3.69/config.json"
CATALOG_DATA = ROOT / "shared/src/commonMain/kotlin/com/dubl/character/android/data/CatalogData.kt"
ANDROID_REPO = ROOT / "app/src/main/java/com/dubl/character/android/data/DevelopmentCatalogRepository.kt"
DESKTOP_LOADER = ROOT / "shared/src/desktopMain/kotlin/com/dubl/character/desktop/data/DesktopCatalogLoader.kt"

LAYERS = (
    ("development_regular", "development_regular_catalog.json", 287, "core"),
    ("development_special", "development_special_catalog.json", 304, "core"),
    ("development_ability_roots", "development_ability_roots_catalog.json", 41, "core"),
    ("development_martial", "development_martial_catalog.json", 122, "melee"),
    ("development_chi", "development_chi_catalog.json", 27, "melee"),
    ("development_magic", "development_magic_catalog.json", 15, "core"),
)


def _catalog(name: str):
    return json.loads((SHARED / name).read_text(encoding="utf-8"))


def test_all_development_domains_are_source_generated_and_bootstrap_is_empty():
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    total = 0
    for domain, filename, expected_count, source in LAYERS:
        meta = config["domains"][domain]
        assert meta["status"] == "source_generated"
        assert meta["sources"] == [source]
        assert meta["runtimeArtifact"] == f"shared/src/commonMain/resources/{filename}"
        catalog = _catalog(filename)
        assert len(catalog["entries"]) == expected_count
        total += expected_count
    assert total == 796
    assert _catalog("development_catalog.json")["entries"] == []


def test_promoted_regular_development_keeps_rulebook_mechanics_and_explicit_unresolved_fallback():
    regular = _catalog("development_regular_catalog.json")
    by_name = {entry["name"]: entry for entry in regular["entries"]}

    blocking = by_name["Блокирование"]
    assert blocking["requirements"] == "Ловкость 3, Скорость 3, Холодное оружие 3 или Рукопашный бой 3"

    last_stand = by_name["Сражаться до последнего"]
    assert last_stand["ranks"] == 1
    assert last_stand["incomplete"] is True
    assert "не указан ранг" in last_stand["mechanicsConflict"]
    assert "совместимости" in last_stand["conflictNote"]


def test_promoted_special_development_keeps_clean_source_text_and_real_conflicts_explicit():
    special = _catalog("development_special_catalog.json")
    by_id = {entry["id"]: entry for entry in special["entries"]}

    assert by_id["feat_a35cd71644363fca"]["benefit"] == "Вы получаете +1 за ранг к бегу если двигаетесь в сторону видимого противника."
    assert by_id["feat_e04b4d619a503480"]["benefit"].endswith("эффект энтропийного резонанса срабатывает дважды.")
    assert by_id["feat_29ff1cb23df7879d"]["benefit"] == "Вы способны превращаться в животных."
    assert by_id["feat_ab0ad31f46407f46"]["benefit"] == "Время превращения в зверя неограниченно."
    assert by_id["feat_1d9d4f8e6d850cae"]["benefit"].endswith("цель становится изумленной на 1 раунд.")
    assert by_id["feat_7bc39bc9c44dd41e"]["benefit"].endswith("4. Воля")

    killing = by_id["feat_52a4d6cee2756b48"]
    assert "не крит а сразу полуторный урон" in killing["benefit"]
    assert killing["incomplete"] is True
    assert "противореч" in killing["mechanicsConflict"].casefold()
    assert "локаль" in killing["conflictNote"].casefold()


def test_promoted_ability_roots_keep_source_semantics_and_draft_markers():
    roots = _catalog("development_ability_roots_catalog.json")
    assert all(entry["section"] == "Особые способности" for entry in roots["entries"])

    battle_rush = next(entry for entry in roots["entries"] if entry["name"] == "Боевой азарт")
    assert battle_rush["requirements"] == ""
    assert battle_rush["benefit"] == ""

    connections = next(entry for entry in roots["entries"] if entry["name"] == "Связи")
    assert connections["abilityOptions"] == [{"source": "???", "value": 1}]
    assert connections["repeatable"] is True
    assert connections["incomplete"] is True
    assert "локаль" in connections["conflictNote"].casefold()


def test_martial_chi_and_magic_development_have_expected_sections_and_counts():
    martial = _catalog("development_martial_catalog.json")
    chi_dev = _catalog("development_chi_catalog.json")
    magic = _catalog("development_magic_catalog.json")
    assert len(martial["entries"]) == 122
    assert all(entry["section"] == "Боевые искусства" for entry in martial["entries"])
    assert len(chi_dev["entries"]) == 27
    assert all(entry["section"] == "ЦИ" for entry in chi_dev["entries"])
    assert len(magic["entries"]) == 15
    assert all(entry["section"] == "Магические навыки" for entry in magic["entries"])


def test_chi_catalog_itself_is_promoted_from_melee_rulebook():
    config = json.loads(CONFIG.read_text(encoding="utf-8"))
    meta = config["domains"]["chi"]
    assert meta["status"] == "source_generated"
    assert meta["sources"] == ["melee"]
    assert meta["runtimeArtifact"] == "shared/src/commonMain/resources/chi_catalog.json"
    chi = _catalog("chi_catalog.json")
    assert len(chi["schools"]) == 9
    assert len(chi["techniques"]) == 68


def test_android_and_desktop_load_exact_same_seven_development_layers():
    parser = CATALOG_DATA.read_text(encoding="utf-8")
    android = ANDROID_REPO.read_text(encoding="utf-8")
    desktop = DESKTOP_LOADER.read_text(encoding="utf-8")
    assert "fun mergeDevelopmentCatalogs" in parser

    for _, filename, _, _ in LAYERS:
        assert f'"{filename}"' in android
        assert f'read("{filename}")' in desktop
    assert '"development_catalog.json"' in android
    assert 'read("development_catalog.json")' in desktop
    assert "mergeDevelopmentCatalogs(regular, special, roots, martial, chi, magic, bootstrap)" in android
    assert 'parseDevelopmentCatalog(read("development_magic_catalog.json"))' in desktop
