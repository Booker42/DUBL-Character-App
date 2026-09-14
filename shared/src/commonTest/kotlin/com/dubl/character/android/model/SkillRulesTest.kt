package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.Test

class SkillRulesTest {
    private fun character(
        vararg values: Pair<AttributeId, Int>,
        skills: Map<String, CharacterSkill> = emptyMap(),
    ): DublCharacter {
        val attrs = defaultAttributes().toMutableMap()
        values.forEach { (id, value) -> attrs[id] = AttributeValue(base = value) }
        return DublCharacter(id = "test", attributes = attrs, skills = skills)
    }

    @Test
    fun multiAttributeSkillOffersAlternativeAttributeCalculations() {
        val state = CharacterSkill(
            id = "athletics",
            definitionId = "athletics",
            rank = 2,
            attributes = listOf(AttributeId.STRENGTH, AttributeId.DEXTERITY),
        )
        val character = character(
            AttributeId.STRENGTH to 3,
            AttributeId.DEXTERITY to 4,
            skills = mapOf("athletics" to state),
        )
        val skill = character.resolveSkill("athletics")!!

        val defaultCalculation = character.skillCalculation(skill)
        assertEquals(5, defaultCalculation.total)
        assertEquals(AttributeId.STRENGTH, defaultCalculation.selectedAttribute)

        val options = character.skillCalculationOptions(skill)
            .map { (attribute, calculation) -> attribute to calculation.total }
        assertEquals(
            listOf(
                AttributeId.STRENGTH to 5,
                AttributeId.DEXTERITY to 6,
            ),
            options,
        )

        assertEquals(6, character.skillCalculation(skill, AttributeId.DEXTERITY).total)
    }


    @Test
    fun rollAttributeOverrideUsesStockDefaultButCanChooseAnyAttribute() {
        val state = CharacterSkill(
            id = "athletics",
            definitionId = "athletics",
            rank = 2,
            attributes = listOf(AttributeId.DEXTERITY),
        )
        val character = character(
            AttributeId.STRENGTH to 3,
            AttributeId.DEXTERITY to 4,
            AttributeId.CHARISMA to 6,
            skills = mapOf("athletics" to state),
        )
        val skill = character.resolveSkill("athletics")!!

        assertEquals(AttributeId.STRENGTH, skill.stockAttribute)
        assertEquals(8, character.skillCalculationForRoll(skill, AttributeId.CHARISMA).total)
        assertEquals(AttributeId.CHARISMA, character.skillCalculationForRoll(skill, AttributeId.CHARISMA).selectedAttribute)
    }

    @Test
    fun untrainedMinusTwoPenaltyIsAppliedAtRankZero() {
        val character = character(AttributeId.DEXTERITY to 4)
        val skill = character.resolveSkill("riding")!!

        assertEquals(2, character.skillCalculation(skill).total)
    }

    @Test
    fun untrainedForbiddenSkillHasNoTotalAtRankZero() {
        val character = character(AttributeId.INTELLIGENCE to 5)
        val skill = character.resolveSkill("engineering_repair")!!
        val result = character.skillCalculation(skill)

        assertNull(result.total)
        assertEquals("Нельзя использовать без обучения", result.unavailableReason)
    }

    @Test
    fun rankCostsMatchDesktopCatalog() {
        assertEquals(listOf(0, 10, 30, 60, 100, 150, 210, 280, 360, 450, 550), SkillCatalog.rankCosts)
        assertEquals(50, SkillCatalog.nextRankCost(4))
        assertNull(SkillCatalog.nextRankCost(10))
    }
}
