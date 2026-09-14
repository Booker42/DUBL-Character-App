import hashlib
import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ANDROID = ROOT / 'app/src/main/assets'
SHARED = ROOT / 'shared/src/commonMain/resources'
NAMES = ['development_catalog.json', 'chi_catalog.json', 'magic_equipment_catalog.json', 'skill_effects_catalog.json']

class Desktop02CatalogsTest(unittest.TestCase):
    def test_shared_resources_are_byte_identical_to_android_canonical_assets(self):
        for name in NAMES:
            source = ANDROID / name
            target = SHARED / name
            self.assertTrue(target.exists(), f'missing shared resource {name}')
            self.assertEqual(source.read_bytes(), target.read_bytes(), name)

    def test_catalog_counts_match_expected_android_062_baseline(self):
        dev = json.loads((ANDROID / 'development_catalog.json').read_text())
        chi = json.loads((ANDROID / 'chi_catalog.json').read_text())
        magic = json.loads((ANDROID / 'magic_equipment_catalog.json').read_text())
        effects = json.loads((ANDROID / 'skill_effects_catalog.json').read_text())
        self.assertEqual(len(dev['entries']), 796)
        self.assertEqual(len(chi['schools']), 9)
        self.assertEqual(len(chi['techniques']), 68)
        self.assertEqual(len(magic['spells']), 265)
        self.assertEqual(len(magic['gear']), 260)
        self.assertEqual(len(effects['effects']), 283)

    def test_common_parser_source_exists(self):
        parser = ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/data/CatalogData.kt'
        self.assertTrue(parser.exists())
        text = parser.read_text()
        for symbol in ['parseDevelopmentCatalog', 'parseChiCatalog', 'parseMagicEquipmentCatalog', 'parseSkillEffectCatalog']:
            self.assertIn(f'fun {symbol}', text)

if __name__ == '__main__':
    unittest.main()
