import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONTROLLER = ROOT / 'app/src/main/java/com/dubl/character/android/state/CharacterController.kt'
SESSION = ROOT / 'shared/src/commonMain/kotlin/com/dubl/character/android/state/CharacterSession.kt'

PUBLIC_MUTATIONS = {
    'updateActive', 'changeAttribute', 'setExperience', 'setCreationExperience',
    'setXpAdjustment', 'setAbilityPointsOverride', 'completeCreation', 'reopenCreation',
    'changeHp', 'changeEndurance', 'changeMana', 'changeChi', 'setChiEnabled',
    'setChiBonusRanks', 'restoreChi', 'setHealthMaximumOverride',
    'setEnduranceMaximumOverride', 'setManaMaximumOverride', 'addCustomResource',
    'updateCustomResource', 'changeCustomResource', 'removeCustomResource',
    'changeSkillRank', 'setSkillAttributes', 'setSkillModifier', 'setSkillFormulaNote',
    'hideSkill', 'restoreSkill', 'restoreAllSkills', 'setDevelopmentRank',
    'setMagicManaRank', 'setMagicPower', 'setMagicSchoolPower', 'addMagicSchool',
    'updateMagicSchool', 'removeMagicSchool', 'addCatalogSpell', 'addCustomSpell',
    'updateSpell', 'removeSpell', 'setGearLoadAutomatic', 'setGearManualLoad',
    'syncCatalogGearLoads', 'addCatalogGear', 'addCustomGear', 'updateGearItem',
    'removeGearItem', 'addSpecializedSkill', 'addCustomSkill', 'deleteDynamicSkill',
    'createCharacter', 'selectCharacter', 'deleteActive',
}


def public_functions(text: str) -> set[str]:
    return set(re.findall(r'^\s{4}fun\s+(\w+)\s*\(', text, flags=re.M))


class Desktop02ParityTest(unittest.TestCase):
    def test_shared_character_session_exists(self):
        self.assertTrue(SESSION.exists(), 'CharacterSession must exist in shared commonMain')

    def test_shared_session_covers_android_controller_mutations(self):
        controller = CONTROLLER.read_text()
        self.assertTrue(PUBLIC_MUTATIONS.issubset(public_functions(controller)))
        session_methods = public_functions(SESSION.read_text())
        self.assertEqual(PUBLIC_MUTATIONS - session_methods, set())

    def test_shared_session_has_no_platform_imports(self):
        text = SESSION.read_text()
        for forbidden in ('java.', 'javax.', 'android.', 'androidx.'):
            self.assertNotIn(f'import {forbidden}', text)


if __name__ == '__main__':
    unittest.main()
