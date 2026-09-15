import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SHARED = ROOT / 'shared/src/commonMain/resources'
DEV_LAYERS = {
    'development_regular_catalog.json': 287,
    'development_special_catalog.json': 304,
    'development_ability_roots_catalog.json': 41,
    'development_martial_catalog.json': 122,
    'development_chi_catalog.json': 27,
    'development_magic_catalog.json': 15,
    'development_catalog.json': 0,
}
NAMES = ['conditions_catalog.json', *DEV_LAYERS, 'chi_catalog.json', 'magic_equipment_catalog.json', 'skill_effects_catalog.json']

class Desktop02CatalogsTest(unittest.TestCase):
    def test_shared_resources_are_the_canonical_catalog_payloads(self):
        for name in NAMES:
            self.assertTrue((SHARED / name).exists(), f'missing shared resource {name}')

    def test_catalog_counts_match_rulebook_promoted_runtime(self):
        development_total = 0
        for name, expected in DEV_LAYERS.items():
            root = json.loads((SHARED / name).read_text())
            self.assertEqual(len(root['entries']), expected, name)
            development_total += expected
        self.assertEqual(development_total, 796)

        chi = json.loads((SHARED / 'chi_catalog.json').read_text())
        magic = json.loads((SHARED / 'magic_equipment_catalog.json').read_text())
        effects = json.loads((SHARED / 'skill_effects_catalog.json').read_text())
        self.assertEqual(len(chi['schools']), 9)
        self.assertEqual(len(chi['techniques']), 68)
        self.assertEqual(len(magic['spells']), 265)
        self.assertEqual(len(magic['gear']), 260)
        self.assertEqual(len(effects['effects']), 283)

    def test_common_parser_source_exists(self):
        parser = ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/data/CatalogData.kt'
        self.assertTrue(parser.exists())
        text = parser.read_text()
        for symbol in ['parseConditionCatalog', 'parseDevelopmentCatalog', 'parseChiCatalog', 'parseMagicEquipmentCatalog', 'parseSkillEffectCatalog']:
            self.assertIn(f'fun {symbol}', text)

if __name__ == '__main__':
    unittest.main()
