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


def test_android_development_rows_do_not_evaluate_requirements_during_composition():
    row = FEATS.split('private fun DevelopmentRow(', 1)[1].split('@Composable\nprivate fun DevelopmentStatusPill', 1)[0]
    assert 'rules.availability(entry)' not in row
    assert 'availability: DevelopmentAvailability?' in row
    assert 'Проверяем…' in row


def test_android_development_availability_is_prepared_off_main_thread():
    assert 'val developmentAvailabilityById by produceState<Map<String, DevelopmentAvailability>>' in FEATS
    availability = FEATS.split('val developmentAvailabilityById by produceState', 1)[1].split('val developmentUnlockIndex', 1)[0]
    assert 'withContext(Dispatchers.Default)' in availability
    assert 'localRules.availability(entry)' in availability


def test_android_reverse_unlock_index_never_builds_from_a_card_or_detail_sheet():
    assert 'data class DevelopmentUnlockUiIndex' in FEATS
    assert 'val developmentUnlockIndex by produceState<DevelopmentUnlockUiIndex?>' in FEATS
    assert 'planner.unlocks(entry.id)' not in FEATS.split('private fun DevelopmentDetailSheet(', 1)[1]
    assert 'unlocks = developmentUnlockIndex?.entriesBySource?.get(entry.id).orEmpty()' in FEATS


def test_android_catalogs_are_cached_and_prewarmed_before_navigation():
    assert '@Volatile' in DEV_REPO and 'cached' in DEV_REPO and 'synchronized' in DEV_REPO
    assert '@Volatile' in CHI_REPO and 'cached' in CHI_REPO and 'synchronized' in CHI_REPO
    assert 'DevelopmentCatalogRepository(appContext).load()' in APP
    assert 'ChiCatalogRepository(appContext).load()' in APP
    assert 'withContext(Dispatchers.IO)' in APP


def test_shared_planner_still_exposes_reverse_unlock_index_once():
    assert 'fun unlockCounts()' in PLANNER
    assert 'private val unlocksIndex' in PLANNER
