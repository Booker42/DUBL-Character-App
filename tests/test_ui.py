import os
os.environ.setdefault('QT_QPA_PLATFORM','offscreen')
import copy,tempfile,unittest
from pathlib import Path
from PySide6.QtWidgets import QApplication,QPushButton,QTableWidget
from PySide6.QtCore import Qt
from PySide6.QtTest import QTest
from dubl.app import Window
from dubl.widgets import Number
from dubl.catalog import CatalogDialog
from dubl.model import fresh,load_file

class UiTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):cls.app=QApplication.instance() or QApplication([])
    def setUp(self):self.w=Window(fresh(),autosave=False);self.w.show();self.app.processEvents()
    def tearDown(self):self.w.close();self.app.processEvents()
    def test_number_updates_actual_character(self):
        n=self.w.docks['attributes'].findChildren(Number)[0]
        plus=next(b for b in n.findChildren(QPushButton) if b.text()=='+');QTest.mouseClick(plus,Qt.LeftButton)
        self.assertEqual(self.w.s['attributes']['Сила'],1)
    def test_rebuild_retains_hidden_dock_and_custom_resource(self):
        self.w.docks['notes'].hide();self.w.s['resources']['custom'].append({'id':'ci','name':'Ци','current':2,'max':4});self.w.rebuild();self.app.processEvents()
        self.assertTrue(self.w.docks['notes'].isHidden());self.assertEqual(self.w.s['resources']['custom'][0]['current'],2)
    def test_save_restore_layout_and_data(self):
        with tempfile.TemporaryDirectory() as temp:
            self.w.s['profile']['name']='Проверка';self.w.docks['notes'].hide();self.w.path=Path(temp)/'test.json';self.assertTrue(self.w.save());n=load_file(self.w.path);self.assertEqual(n['profile']['name'],'Проверка');self.assertTrue(n['ui']['layout'])
            other=Window(n,autosave=False);other.show();self.app.processEvents();self.assertTrue(other.docks['notes'].isHidden());other.close()
    def test_catalog_search_and_clear(self):
        d=CatalogDialog(self.w,self.w.s);d.search.setText('Ассас');self.assertGreater(d.list.count(),0);self.assertIn('Требования',d.details.toPlainText());d.search.setText('zzzz-нет-записи');self.assertEqual(d.list.count(),0);self.assertFalse(d.add.isEnabled());d.close()
    def test_every_dock_and_theme(self):
        for dock in self.w.docks.values():dock.show();dock.raise_();self.app.processEvents();self.assertFalse(dock.grab().isNull())
        self.w.toggle_theme();self.assertEqual(self.w.s['ui']['theme'],'light')
    def test_hiding_skill_preserves_rank(self):
        self.w.s['skills']['Атлетика']={'rank':4};self.w.hide_skill({'name':'Атлетика'});self.assertEqual(self.w.s['skills']['Атлетика']['rank'],4)
        t=self.w.docks['skills'].findChild(QTableWidget);self.assertNotIn('Атлетика',[t.item(i,0).text() for i in range(t.rowCount())])
if __name__=='__main__':unittest.main()
