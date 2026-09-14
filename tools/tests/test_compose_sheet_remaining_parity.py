from pathlib import Path
ROOT = Path(__file__).resolve().parents[2]
SHEET = (ROOT/'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/CharacterSheetScreen.kt').read_text(encoding='utf-8')
DEV = (ROOT/'desktopApp/src/main/kotlin/com/dubl/character/desktop/screens/DevelopmentScreen.kt').read_text(encoding='utf-8')


def test_health_has_android_style_amount_control():
    assert 'HealthControlDialog' in SHEET
    assert 'Получить урон' in SHEET
    assert 'Лечение' in SHEET
    assert 'Восстановить всё здоровье' in SHEET


def test_owned_development_on_sheet_opens_details():
    assert 'sheetDevelopmentEntry' in SHEET
    assert 'DevelopmentDetailsDialog(' in SHEET
    assert 'fun DevelopmentDetailsDialog(' in DEV


def test_portrait_is_visible_not_only_filename():
    assert 'Image(' in SHEET
    assert 'loadImageBitmap' in SHEET
    assert 'SwingPanel' not in SHEET
    assert 'portraitUri' in SHEET
