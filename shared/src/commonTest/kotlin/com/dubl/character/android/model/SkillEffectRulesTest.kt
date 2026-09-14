package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.Test

class SkillEffectRulesTest {
    private fun development(id: String, name: String, maxRank: Int = 3) = DevelopmentEntry(
        id = id, name = name, section = "Навыки", category = "Тест", cost = 10,
        costType = DevelopmentCostType.XP, maxRank = maxRank, requirements = "-", benefit = "",
        notes = "", tags = emptyList(), accessId = null, abilityOptions = emptyList(), incomplete = false,
        repeatable = false, perfectRoot = false, mechanicsConflict = "", conflictNote = "",
    )

    @Test
    fun autoAndSituationalEffectsResolveFromOwnedDevelopmentRanks() {
        val athletics = development("athletic", "Атлетичность", 2)
        val diplomat = development("diplomat", "Дипломат", 3)
        val catalog = DevelopmentCatalog("test", listOf(athletics, diplomat))
        val character = DublCharacter(
            id = "effects",
            skills = mapOf(
                "athletics" to CharacterSkill(id = "athletics", definitionId = "athletics", rank = 4),
                "eloquence" to CharacterSkill(id = "eloquence", definitionId = "eloquence", rank = 4),
            ),
            development = mapOf(
                athletics.id to OwnedDevelopment(2),
                diplomat.id to OwnedDevelopment(3),
            ),
        )
        val effects = SkillEffectCatalog(
            "test",
            listOf(
                SkillEffectDefinition("e1", 4, "Атлетичность", "", "", "0.5 · EXISTING", "", SkillEffectMode.AUTO_BONUS, RollContext.SKILL, listOf("Атлетика"), 1, true, "Атлетичность"),
                SkillEffectDefinition("e2", 74, "Дипломат", "", "", "0.5 · EXISTING", "", SkillEffectMode.TOGGLE_BONUS, RollContext.SKILL, listOf("Красноречие"), 1, true, "Дипломат — пытаюсь договориться"),
            ),
        )
        val rules = SkillEffectRules(character, catalog, effects)

        val athleticsResult = rules.forSkill(character.resolveSkill("athletics")!!)
        assertEquals(2, athleticsResult.automaticBonus)
        assertTrue(athleticsResult.automaticContributions.any { it.label == "Атлетичность" && it.value == 2 })

        val eloquenceResult = rules.forSkill(character.resolveSkill("eloquence")!!)
        assertEquals(0, eloquenceResult.automaticBonus)
        assertEquals(1, eloquenceResult.options.size)
        assertEquals(3, eloquenceResult.options.single().numericBonus)
        assertEquals("Дипломат — пытаюсь договориться", eloquenceResult.options.single().label)
    }

    @Test
    fun advantageToggleAddsDiceEffectNotNumericBonus() {
        val perk = development("sweet", "Сладкие речи")
        val catalog = DevelopmentCatalog("test", listOf(perk))
        val character = DublCharacter(
            id = "adv",
            skills = mapOf("eloquence" to CharacterSkill(id = "eloquence", definitionId = "eloquence", rank = 4)),
            development = mapOf(perk.id to OwnedDevelopment(1)),
        )
        val effectCatalog = SkillEffectCatalog(
            "test",
            listOf(SkillEffectDefinition("e", 77, "Сладкие речи", "", "", "0.5 · EXISTING", "", SkillEffectMode.TOGGLE_ADVANTAGE, RollContext.SKILL, listOf("Красноречие"), 0, false, "Вру")),
        )
        val option = SkillEffectRules(character, catalog, effectCatalog).forSkill(character.resolveSkill("eloquence")!!).options.single()
        assertEquals(1, option.advantageDice)
        assertEquals(0, option.numericBonus)
    }
}
