import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
PORTABLE = ROOT / 'packaging/linux/portable-src/com/dubl/character/portable'
WINDOW = PORTABLE / 'DublWindow.kt'
MAIN = PORTABLE / 'Main.kt'
UIKIT = PORTABLE / 'UiKit.kt'

class Desktop02WorkflowsTest(unittest.TestCase):
    def test_portable_runtime_exposes_every_android_top_level_workflow(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'SHEET("Лист")',
            'SKILLS("Умения")',
            'DEVELOPMENT("Навыки")',
            'MAGIC("Магия")',
            'EQUIPMENT("Снаряжение")',
            'CHARACTERS("Персонажи")',
            'buildCharacterSheet',
            'buildSkills',
            'buildDevelopment',
            'buildMagic',
            'buildEquipment',
            'buildCharacters',
        ]:
            self.assertIn(symbol, text)

    def test_skills_keep_android_search_category_trained_hidden_custom_and_roll_features(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'skillCategoryFilter', 'skillTrainedOnly', 'SkillCategory.entries',
            'addCustomSkill', 'addSpecializedSkill', 'hideSkill', 'restoreAllSkills',
            'setSkillAttributes', 'setSkillModifier', 'setSkillFormulaNote',
            'SkillEffectRules', 'rollCheck',
        ]:
            self.assertIn(symbol, text)

    def test_skills_expose_android_rank_cost_breakdown_and_auto_success_details(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'showSkillDetails',
            'SkillCatalog.nextRankCost',
            'skillCalculationOptions',
            'Автоуспех 6',
            'Автоуспех 12',
        ]:
            self.assertIn(symbol, text)

    def test_development_keeps_requirements_force_purchase_and_chi_use(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'DevelopmentRules', 'availability', 'canForceIncrease', 'setDevelopmentRank', 'developmentAvailableOnly', 'canIncrease',
            'ChiRules', 'setChiBonusRanks', 'changeChi(-technique.chiCost)', '.unlocked',
            'showDevelopmentDetails', 'entry.notes',
        ]:
            self.assertIn(symbol, text)

    def test_magic_keeps_school_filter_spellbook_and_custom_spell_features(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'hideUnlearnedMagicSchools', 'spellSchoolFilter', 'setMagicManaRank',
            'setMagicSchoolPower', 'removeMagicSchool', 'addCatalogSpell', 'addCustomSpell', 'updateSpell', 'removeSpell',
            'MagicEquipmentRules.spellUsability',
            'val school = JTextField',
            'school = school.text.trim()',
            'MagicEquipmentRules.manaRankXp', 'MagicEquipmentRules.magicSchoolPowerXp',
            'catalogSpellDetails',
        ]:
            self.assertIn(symbol, text)

    def test_equipment_keeps_load_catalog_custom_and_catalog_load_sync(self):
        window = WINDOW.read_text(encoding='utf-8')
        main = MAIN.read_text(encoding='utf-8')
        for symbol in ['setGearLoadAutomatic', 'setGearManualLoad', 'addCatalogGear', 'addCustomGear', 'updateGearItem', 'removeGearItem', 'MagicEquipmentRules.burden']:
            self.assertIn(symbol, window)
        self.assertIn('syncCatalogGearLoads(bundle.magicEquipment.gear)', main)

    def test_equipment_search_filters_inventory_and_exposes_item_details(self):
        text = WINDOW.read_text(encoding='utf-8')
        self.assertIn('visibleItems = character.gear.items.filter', text)
        self.assertIn('gearDetails(item)', text)
        self.assertIn('sumOf { it.quantity }', text)

    def test_sheet_has_android_quick_combat_checks_and_no_horizontal_scroll(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in ['RollContext.DODGE', 'RollContext.ATTACK', 'RollContext.PARRY', 'showContextRollDialog']:
            self.assertIn(symbol, text)
        all_text = '\n'.join(p.read_text(encoding='utf-8') for p in PORTABLE.glob('*.kt'))
        self.assertNotIn('HORIZONTAL_SCROLLBAR_ALWAYS', all_text)
        self.assertNotIn('HORIZONTAL_SCROLLBAR_AS_NEEDED', all_text)
        self.assertIn('JScrollPane.HORIZONTAL_SCROLLBAR_NEVER', all_text)

    def test_portable_runtime_has_gui_smoke_covering_all_sections(self):
        window = WINDOW.read_text(encoding='utf-8')
        main = MAIN.read_text(encoding='utf-8')
        self.assertIn('smokeNavigateAllSections', window)
        self.assertIn('--gui-smoke', main)
        self.assertIn('DUBL_GUI_SMOKE_OK', main)

    def test_portable_window_fits_small_linux_displays_and_wraps_long_text(self):
        window = WINDOW.read_text(encoding='utf-8')
        ui = UIKIT.read_text(encoding='utf-8')
        self.assertIn('Toolkit.getDefaultToolkit().screenSize', window)
        self.assertIn('minimumSize = Dimension(720, 520)', window)
        self.assertIn('JTextArea(value)', ui)
        self.assertNotIn('width:${width}px', ui)

if __name__ == '__main__':
    unittest.main()
