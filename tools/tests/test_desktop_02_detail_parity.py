import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WINDOW = ROOT / 'packaging/linux/portable-src/com/dubl/character/portable/DublWindow.kt'


class Desktop02DetailParityTest(unittest.TestCase):
    def test_attribute_details_keep_android_xp_cost_and_refund(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'showAttributeInfoDialog',
            'CharacterEconomy.nextAttributeCost',
            'CharacterEconomy.previousAttributeRefund',
            'Следующий ранг',
            'возврат',
        ]:
            self.assertIn(symbol, text)

    def test_stat_details_keep_android_quick_edit_and_fortitude_roll(self):
        text = WINDOW.read_text(encoding='utf-8')
        for symbol in [
            'showStatInfoDialog',
            'Быстрое редактирование',
            'Сохранить размер',
            'Количество ног',
            'showContextRollDialog(RollContext.FORTITUDE)',
        ]:
            self.assertIn(symbol, text)


if __name__ == '__main__':
    unittest.main()
