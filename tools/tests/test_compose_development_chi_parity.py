from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEV = ROOT / 'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt'
WORKFLOW = ROOT / '.github/workflows/linux-appimage.yml'


def read(path: Path) -> str:
    assert path.exists(), f'missing {path}'
    return path.read_text(encoding='utf-8')


def test_development_summary_uses_full_character_economy():
    text = read(DEV)
    assert 'CharacterEconomy.breakdown(character, state.developmentCatalog)' in text
    assert 'economy.remainingXp' in text
    assert 'economy.abilityPointsRemaining' in text
    assert 'economy.chiXp' in text


def test_chi_card_matches_android_automatic_access_semantics():
    text = read(DEV)
    assert 'DevelopmentEffectIds.INTERNAL_CHI' in text
    assert 'DevelopmentEffectIds.MASTER_CHI' in text
    assert 'DevelopmentEffectIds.AWAKENED_CHI' in text
    assert 'checked = character.chiActive' in text
    assert 'enabled = !automaticAccess' in text
    assert 'Ресурс открыт способностью «Внутренняя ЦИ»' in text


def test_chi_resource_controls_are_bounded_and_explain_cost_formula():
    text = read(DEV)
    assert 'enabled = character.chiCurrent > 0' in text
    assert 'enabled = character.chiCurrent < character.chiMaximum' in text
    assert '50 XP за ранг' in text
    assert 'Максимум: база $baseMaximum + купленный запас ${character.chiBonusRanks} + развитие $progressionBonus' in text


def test_development_details_can_follow_requirements_and_children():
    text = read(DEV)
    assert 'onOpenEntry: (String) -> Unit' in text
    assert 'state.developmentCatalog.childrenOf(entry.id)' in text
    assert 'check.targetEntryId' in text
    assert 'onOpenEntry(child.id)' in text
    assert 'enabled = owned.rank == 0' in text


def test_linux_release_gate_runs_development_chi_parity_guard():
    text = read(WORKFLOW)
    assert 'test_compose_development_chi_parity.py' in text


def test_owned_entries_surface_manual_or_failed_requirements_like_android():
    text = read(DEV)
    assert 'availability.checks.any { it.status != RequirementStatus.OK }' in text
    assert 'RequirementStatus.MANUAL' in text
    assert 'Требуется ручная проверка' in text
