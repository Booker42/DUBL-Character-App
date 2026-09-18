from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FEATS = (ROOT / 'app/src/main/java/com/dubl/character/android/ui/screens/FeatsScreen.kt').read_text(encoding='utf-8')
PLANNER = (ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/model/DevelopmentAcquisition.kt').read_text(encoding='utf-8')


def test_default_android_development_browser_does_not_eagerly_evaluate_availability_for_every_entry():
    filtering = FEATS.split('val filteredEntries = remember(', 1)[1].split('val filteredChiTechniques', 1)[0]
    assert 'DevelopmentBrowserFilter.ALL -> true' in filtering
    all_branch = filtering.split('when (browserFilter)', 1)[1].split('}', 1)[0]
    assert all_branch.index('DevelopmentBrowserFilter.ALL -> true') < all_branch.find('localRules.availability(entry)') or 'localRules.availability(entry)' not in all_branch


def test_android_development_rows_do_not_scan_reverse_dependencies_inline_on_main_thread():
    assert 'unlocksCount = planner.unlocks(entry.id).size' not in FEATS
    assert 'developmentUnlockCounts' in FEATS
    assert 'withContext(Dispatchers.Default)' in FEATS


def test_shared_planner_builds_reverse_unlock_index_once():
    assert 'fun unlockCounts()' in PLANNER
    assert 'private val unlocksIndex' in PLANNER


def test_android_development_catalogs_load_off_the_main_thread():
    assert 'withContext(Dispatchers.IO)' in FEATS
    assert 'produceState<Pair<DevelopmentCatalog, ChiCatalog>?>' in FEATS
    assert 'Загрузка каталога развития' in FEATS
