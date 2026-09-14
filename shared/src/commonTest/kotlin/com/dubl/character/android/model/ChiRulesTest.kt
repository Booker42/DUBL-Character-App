package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ChiRulesTest {
    private fun entry(id: String, name: String, requirements: String = "-") = DevelopmentEntry(
        id = id,
        name = name,
        section = "ЦИ",
        category = "ЦИ",
        cost = 0,
        costType = DevelopmentCostType.ABILITY,
        maxRank = 1,
        requirements = requirements,
        benefit = "",
        notes = "",
        tags = listOf("ЦИ"),
        accessId = null,
        abilityOptions = listOf(AbilityOption("ЦИ", 1)),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )

    private val developmentCatalog = DevelopmentCatalog(
        version = "test",
        entries = listOf(
            entry(DevelopmentEffectIds.INTERNAL_CHI, "Внутренняя Ци"),
            entry(DevelopmentEffectIds.MASTER_CHI, "Мастер Ци", "Внутренняя Ци, Воля 5"),
            entry("dragon-school", "Школа Дракона", "Внутренняя Ци"),
        ),
    )

    @Test
    fun techniquesUnlockFromOwnedDevelopmentAndRespectChiCost() {
        val character = DublCharacter(
            id = "chi-rules",
            chiCurrent = 1,
            development = mapOf(DevelopmentEffectIds.INTERNAL_CHI to OwnedDevelopment(1)),
        ).normalized()
        val rules = ChiRules(character, developmentCatalog)
        val basic = ChiTechnique("basic", "Вихрь ударов", "Базовые", 1, "1 ОД", "", "Внутренняя Ци")
        val advanced = ChiTechnique("advanced", "Целительная Ци", "Продвинутые", 2, "2 ОД", "", "Мастер Ци")

        assertTrue(rules.availability(basic).unlocked)
        assertTrue(rules.availability(basic).canUse)
        assertFalse(rules.availability(advanced).unlocked)
        assertFalse(rules.availability(advanced).canUse)
    }

    @Test
    fun schoolTechniqueUnlocksOnlyAfterSchoolIsOwned() {
        val technique = ChiTechnique("dragon", "Рёв дракона", "Школа Дракона", 1, "1 ОД", "", "Школа Дракона")
        val base = DublCharacter(
            id = "school",
            chiCurrent = 3,
            development = mapOf(DevelopmentEffectIds.INTERNAL_CHI to OwnedDevelopment(1)),
        ).normalized()
        assertFalse(ChiRules(base, developmentCatalog).availability(technique).unlocked)

        val trained = base.copy(development = base.development + ("dragon-school" to OwnedDevelopment(1))).normalized()
        assertTrue(ChiRules(trained, developmentCatalog).availability(technique).unlocked)
        assertEquals(1, ChiRules(trained, developmentCatalog).availability(technique).chiCost)
    }
}
