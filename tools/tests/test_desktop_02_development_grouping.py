import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WINDOW = ROOT / 'packaging/linux/portable-src/com/dubl/character/portable/DublWindow.kt'


class Desktop02DevelopmentGroupingTest(unittest.TestCase):
    def test_development_catalog_keeps_android_semantic_groups(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'developmentGroupsFor',
            'developmentBranchName',
            'DevelopmentFilter.REGULAR ->',
            'DevelopmentFilter.SPECIAL ->',
            'DevelopmentFilter.MARTIAL ->',
            'DevelopmentFilter.OWNED ->',
            'Спец. ветка',
            'Боевые искусства',
        ]:
            self.assertIn(symbol, text)

    def test_special_branch_group_orders_access_ability_before_children(self):
        text = WINDOW.read_text(encoding='utf-8')
        self.assertIn('if (it.isAbility) 0 else 1', text)
        self.assertIn('entry.accessId', text)


if __name__ == '__main__':
    unittest.main()
