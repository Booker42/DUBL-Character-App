import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WINDOW = ROOT / 'packaging/linux/portable-src/com/dubl/character/portable/DublWindow.kt'

class Desktop02SheetParityTest(unittest.TestCase):
    def test_sheet_keeps_android_grouping_and_auto_condition_behavior(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'showGroupingManager',
            'SheetGroupingRules.normalize',
            'SheetGroupingRules.subtreeBlock',
            'SheetGroupingRules.moveItems',
            'CharacterConditionId.WEAKNESS',
        ]:
            self.assertIn(symbol, text)

    def test_skill_roll_applies_android_skill_effect_catalog(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'SkillEffectRules',
            'automaticContributions',
            'advantageDice',
            'hindranceDice',
            'numericBonus',
        ]:
            self.assertIn(symbol, text)

    def test_sheet_renders_owned_skills_and_development_using_saved_groups(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'ownedSkillGroupsCard',
            'ownedDevelopmentGroupsCard',
            'SheetGroupingRules.toggleCollapsed',
            'SheetGroupingRules.hierarchicalOrder',
        ]:
            self.assertIn(symbol, text)

    def test_sheet_keeps_health_control_and_mana_toggle_from_android(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'showHealthControlDialog',
            'Получить урон',
            'Лечение',
            'Восстановить всё здоровье',
            'Мана включена',
            'manaEnabled = manaEnabled.isSelected',
        ]:
            self.assertIn(symbol, text)

    def test_sheet_displays_saved_portrait_and_exposes_android_stat_formula_details(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'portraitPreview',
            'ImageIcon',
            'showStatInfo',
            '10 − Размер + Скорость + Ловкость',
            'Скорость + Ловкость',
            'Скорость + Восприятие',
            'Телосложение + Воля',
            'Базовый бег + Скорость × множитель размера/ног',
        ]:
            self.assertIn(symbol, text)

if __name__ == '__main__':
    unittest.main()
