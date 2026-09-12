import os
os.environ.setdefault('QT_QPA_PLATFORM','offscreen')
import copy,tempfile,unittest
from pathlib import Path
from unittest.mock import patch
from PySide6.QtCore import Qt,QRect
from PySide6.QtTest import QTest
from PySide6.QtWidgets import QApplication,QPushButton,QDialog,QDockWidget
from dubl.shell import CharacterWindow
from dubl.model import fresh,normalize,load_file,CATALOG
from dubl.widgets import Number
from dubl import engine

class ShellTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):cls.app=QApplication.instance() or QApplication([])
    def setUp(self):self.w=CharacterWindow(fresh(),False);self.w.show();self.app.processEvents()
    def tearDown(self):self.w.close();self.w.deleteLater();self.app.processEvents()
    def test_workspace_replaces_legacy_docks_and_columns(self):
        self.assertEqual(len(self.w.findChildren(QDockWidget)),0)
        self.assertIsNotNone(self.w.workspace_canvas);self.assertIsNotNone(self.w.workspace_area)
        self.assertTrue(self.w.docks['cyber'].isHidden());self.assertFalse(self.w.docks['magic'].isHidden())
        self.assertGreater(self.w.docks['skills'].x(),self.w.docks['profile'].x())
    def test_number_changes_character_and_totals(self):
        n=self.w.docks['attributes'].findChild(Number,'attribute_Сила');self.assertIsNotNone(n);plus=next(b for b in n.findChildren(QPushButton) if b.text()=='+');QTest.mouseClick(plus,Qt.LeftButton)
        self.assertEqual(self.w.s['attributes']['Сила'],1);self.assertEqual(engine.derived(self.w.s)['Здоровье'],1)
    def test_customize_mode_is_explicit(self):
        card=self.w.docks['profile'];self.assertFalse(card._customizing)
        self.w.layout_button.setChecked(True);self.app.processEvents();self.assertTrue(card._customizing);self.assertTrue(card.resize_corner.isVisible())
        self.w.layout_button.setChecked(False);self.app.processEvents();self.assertFalse(card._customizing);self.assertFalse(card.resize_corner.isVisible())
    def test_card_size_is_bounded(self):
        card=self.w.docks['profile']
        self.assertLess(card.min_user_width(),card.preferred_width());self.assertLess(card.preferred_width(),card.max_user_width())
        self.assertEqual(card.clamp_user_width(1),card.min_user_width());self.assertEqual(card.clamp_user_width(99999),card.max_user_width())
        self.assertEqual(card.clamp_user_height(1),card.min_user_height());self.assertEqual(card.clamp_user_height(99999),card.max_user_height())
    def test_manual_collapse_is_stable(self):
        card=self.w.docks['magic'];saved=card.user_height;card.set_collapsed(True);self.app.processEvents();self.assertEqual(card.height(),48)
        card.set_collapsed(False);self.app.processEvents();self.assertGreater(card.height(),48);self.assertEqual(card.user_height,saved)
    def test_detach_return_keeps_editor_values(self):
        c=self.w.docks['attributes'];c.detach();self.app.processEvents();self.assertIsNotNone(c.popup)
        n=c.body.findChild(Number,'attribute_Сила');self.assertIsNotNone(n);n.spin.setValue(4);c.popup.close();self.app.processEvents();self.assertIsNone(c.popup);self.assertIs(c.body.parent(),c);self.assertEqual(self.w.s['attributes']['Сила'],4)
    def test_restore_geometry_hidden_and_collapsed(self):
        card=self.w.docks['skills'];card.user_x=420;card.user_y=500;card.user_width=710;card.user_height=410;card.setGeometry(420,500,710,410);card.set_collapsed(True)
        self.w.docks['profile'].hide_card();self.w.capture_layout();restored=normalize(copy.deepcopy(self.w.s));self.w.s=restored;self.w.rebuild(True);self.app.processEvents()
        card=self.w.docks['skills'];self.assertEqual((card.x(),card.y(),card.user_width,card.user_height),(420,500,710,410));self.assertTrue(card.collapsed);self.assertTrue(self.w.docks['profile'].isHidden())
    def test_overlap_recovery_finds_free_space(self):
        a=self.w.docks['profile'];b=self.w.docks['resources'];candidate=QRect(b.geometry());candidate.moveTop(a.y())
        free=self.w.find_free_geometry(b,candidate);self.assertFalse(free.intersects(a.geometry()))
    def test_rebuild_preserves_hidden_and_custom_resources(self):
        self.w.docks['magic'].hide_card();self.w.s['resources']['custom'].append({'id':'ki','name':'Ци','current':2,'max':4});self.w.rebuild();self.assertTrue(self.w.docks['magic'].isHidden());self.assertEqual(self.w.s['resources']['custom'][0]['current'],2)
    def test_source_option_second_choice_does_not_crash(self):
        entry=next(e for e in CATALOG['entries'] if len(e.get('abilityOptions',[]))>=2)
        f={'id':entry['id'],'uid':'test','rank':1,'choice':'','note':'','option':0};self.w.s['feats'].append(f)
        result={'rank':1,'choice':'','note':'','overrideReason':'','optionText':entry['abilityOptions'][1]['source']}
        with patch.object(self.w,'edit',return_value=result):self.w.edit_feat(f)
        self.assertEqual(f['option'],1)
    def test_save_shortcut_and_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmp:
            self.w.path=Path(tmp)/'hero.json';self.w.s['profile']['name']='Тест';self.w.changed();self.w.activateWindow();self.w.setFocus();self.app.processEvents();QTest.keyClick(self.w,Qt.Key_S,Qt.ControlModifier);self.app.processEvents()
            self.assertTrue(self.w.path.exists());state=load_file(self.w.path);self.assertEqual(state['profile']['name'],'Тест');self.assertEqual(state['ui']['shell']['version'],7)
    def test_ui_rebuild_does_not_change_mechanics(self):
        before=copy.deepcopy(self.w.s);self.w.rebuild();self.w.toggle_theme();self.w.reset_layout()
        self.assertEqual(engine.derived(before),engine.derived(self.w.s));self.assertEqual(engine.costs(before),engine.costs(self.w.s));self.assertEqual(before['feats'],self.w.s['feats'])
    def test_card_titles_match_settings_menu(self):
        for card in self.w.docks.values():self.assertEqual(card.toggle.text(),card.title)
    def test_catalog_contains_structured_spell_metadata(self):
        from dubl.catalog import CatalogDialog
        d=CatalogDialog(self.w,self.w.s,'spells');d.search.setText('Агония');text=d.details.toPlainText();self.assertIn('Школа',text);self.assertIn('Дальность',text);self.assertIn('Усиление',text);d.close()
if __name__=='__main__':unittest.main()
