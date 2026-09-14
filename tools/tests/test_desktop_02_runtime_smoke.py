import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'packaging/linux/portable-src/com/dubl/character/portable/Main.kt'


class Desktop02RuntimeSmokeTest(unittest.TestCase):
    def test_release_smoke_exercises_every_persistent_gameplay_domain(self):
        text = MAIN.read_text(encoding='utf-8')
        for symbol in [
            'addCustomResource',
            'setDevelopmentRank',
            'setChiEnabled',
            'addCatalogSpell',
            'addCatalogGear',
            'createCharacter',
            'selectCharacter',
            'store.load()',
        ]:
            self.assertIn(symbol, text)


if __name__ == '__main__':
    unittest.main()
