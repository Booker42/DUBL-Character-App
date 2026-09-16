package com.dubl.character.android.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RulebookCoreSkillsRollsTest {
    private fun burdenedCharacter(): DublCharacter {
        val attributes = defaultAttributes().toMutableMap().apply {
            this[AttributeId.STRENGTH] = AttributeValue(base = 2)
            this[AttributeId.CONSTITUTION] = AttributeValue(base = 2)
            this[AttributeId.DEXTERITY] = AttributeValue(base = 3)
            this[AttributeId.SPEED] = AttributeValue(base = 2)
            this[AttributeId.PERCEPTION] = AttributeValue(base = 3)
            this[AttributeId.WILL] = AttributeValue(base = 2)
        }
        return DublCharacter(
            id = "rulebook-core",
            attributes = attributes,
            skills = mapOf("shooting" to CharacterSkill(id = "shooting", definitionId = "shooting", rank = 1)),
            gear = CharacterGear(loadAutomatic = false, loadManual = 4.0),
            development = mapOf(
                DevelopmentEffectIds.STILL_MOUNTAIN_SCHOOL to OwnedDevelopment(rank = 1),
                DevelopmentEffectIds.STORM_LORD_SCHOOL to OwnedDevelopment(rank = 1),
            ),
        )
    }

    @Test
    fun loadPenalizesAttacksAndDexterityChecksExactlyOnce() {
        val character = burdenedCharacter()
        assertEquals(-1, character.equipmentLoadPenalty)

        assertEquals(2, character.rollPreset(RollContext.ATTRIBUTE, attribute = AttributeId.DEXTERITY).bonus)

        val shooting = character.resolveSkill("shooting")!!
        assertEquals(3, character.skillCalculationForRoll(shooting, AttributeId.DEXTERITY).total)
        assertEquals(3, character.rollPreset(RollContext.ATTACK, "shooting", AttributeId.PERCEPTION).bonus)
        assertEquals(3, character.rollPreset(RollContext.ATTACK, "shooting", AttributeId.DEXTERITY).bonus)
    }

    @Test
    fun quickFortitudeAndInitiativeMatchCanonicalAggregates() {
        val character = burdenedCharacter()
        assertEquals(character.fortitude, character.rollPreset(RollContext.FORTITUDE).bonus)
        assertEquals(character.initiative, character.rollPreset(RollContext.INITIATIVE).bonus)
    }

    @Test
    fun quickRunUsesIntegralDerivedRunValue() {
        val character = burdenedCharacter()
        assertEquals(character.runFull.toInt(), character.rollPreset(RollContext.RUN).bonus)
    }

    @Test
    fun normalizationPreservesNegativeHealthForRulebookDeathAndLastSurvivorMechanics() {
        val character = DublCharacter(
            id = "negative-health",
            attributes = defaultAttributes() + mapOf(
                AttributeId.CONSTITUTION to AttributeValue(base = 3),
                AttributeId.STRENGTH to AttributeValue(base = 2),
            ),
            hpCurrent = -7,
        ).normalized()

        assertEquals(-7, character.hpCurrent)
    }

    @Test
    fun synergyUsesHigherRankPlusRoundedUpHalfOfLowerRank() {
        assertEquals(7, SkillCheckRules.synergyCombinedRank(5, 3))
        assertEquals(7, SkillCheckRules.synergyCombinedRank(3, 5))
        assertEquals(5, SkillCheckRules.synergyCombinedRank(5, 0))
    }

    @Test
    fun assistanceRequiresTrainingAndScalesEveryFourPointsAboveTen() {
        assertNull(SkillCheckRules.assistanceBonus(helperSkillRank = 0, helperRollTotal = 30))
        assertEquals(0, SkillCheckRules.assistanceBonus(helperSkillRank = 1, helperRollTotal = 9))
        assertEquals(1, SkillCheckRules.assistanceBonus(helperSkillRank = 1, helperRollTotal = 10))
        assertEquals(1, SkillCheckRules.assistanceBonus(helperSkillRank = 1, helperRollTotal = 13))
        assertEquals(2, SkillCheckRules.assistanceBonus(helperSkillRank = 1, helperRollTotal = 14))
        assertEquals(3, SkillCheckRules.assistanceBonus(helperSkillRank = 1, helperRollTotal = 18))
    }
}
