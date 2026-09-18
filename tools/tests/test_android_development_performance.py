from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FEATS = (ROOT / 'app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt').read_text(encoding='utf-8')
APP = (ROOT / 'app/src/main/java/com/dubl/character/android/ui/DublApp.kt').read_text(encoding='utf-8')
DEV_REPO = (ROOT / 'app/src/main/java/com/dubl/character/android/data/DevelopmentCatalogRepository.kt').read_text(encoding='utf-8')
CHI_REPO = (ROOT / 'app/src/main/java/com/dubl/character/android/data/ChiCatalogRepository.kt').read_text(encoding='utf-8')
PLANNER = (ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/model/DevelopmentAcquisition.kt').read_text(encoding='utf-8')


def test_default_android_development_browser_filters_off_main_thread():
    assert 'produceState<List<DevelopmentEntry>?>' in FEATS
    assert 'withContext(Dispatchers.Default)' in FEATS
    assert 'val filteredEntries = filteredEntriesAsync.orEmpty()' in FEATS


def test_android_development_rows_only_evaluate_requirements_for_composed_rows():
    row = FEATS.split('private fun DevelopmentRow(', 1)[1].split('@Composable\nprivate fun DevelopmentStatusPill', 1)[0]
    assert 'rules: DevelopmentRules' in row
    assert 'remember(entry.id, rules) { rules.availability(entry) }' in row
    assert 'developmentAvailabilityById' not in FEATS


def test_android_reverse_unlock_index_is_deferred_until_detail_selection():
    assert 'DevelopmentUnlockUiIndex' not in FEATS
    assert 'developmentUnlockIndex' not in FEATS
    assert 'val selectedUnlocks by produceState<List<DevelopmentEntry>?>' in FEATS
    selected = FEATS.split('val selectedUnlocks by produceState', 1)[1].split('val filteredChiTechniques', 1)[0]
    assert 'selectedEntryId' in selected
    assert 'withContext(Dispatchers.Default) { planner.unlocks(id) }' in selected


def test_android_app_start_does_not_prewarm_large_development_catalogs():
    assert 'DevelopmentCatalogRepository(appContext).load()' not in APP
    assert 'ChiCatalogRepository(appContext).load()' not in APP
    assert 'delay(250)' not in APP


def test_android_catalogs_remain_cached_after_first_real_use():
    assert '@Volatile' in DEV_REPO and 'cached' in DEV_REPO and 'synchronized' in DEV_REPO
    assert '@Volatile' in CHI_REPO and 'cached' in CHI_REPO and 'synchronized' in CHI_REPO


def test_shared_planner_still_exposes_reverse_unlock_index_once():
    assert 'fun unlockCounts()' in PLANNER
    assert 'private val unlocksIndex' in PLANNER
