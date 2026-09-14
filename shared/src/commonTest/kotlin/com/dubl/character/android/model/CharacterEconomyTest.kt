package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class CharacterEconomyTest {
    private val feat = DevelopmentEntry(
        id = "feat",
        name = "Навык",
        section = "Навыки",
        category = "Общие",
        cost = 40,
        costType = DevelopmentCostType.XP,
        maxRank = 3,
        requirements = "-",
        benefit = "",
        notes = "",
        tags = emptyList(),
        accessId = null,
        abilityOptions = emptyList(),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )
    private val ability = feat.copy(
        id = "ability",
        name = "Способность",
        cost = 0,
        costType = DevelopmentCostType.ABILITY,
        maxRank = 1,
        abilityOptions = listOf(AbilityOption("Мастерство", 2)),
    )
    private val catalog = DevelopmentCatalog("test", listOf(feat, ability))

    @Test
    fun attributeTableMatchesRulebookCumulativeCosts() {
        assertEquals(-200, CharacterEconomy.attributeCost(-5))
        assertEquals(-50, CharacterEconomy.attributeCost(-1))
        assertEquals(0, CharacterEconomy.attributeCost(0))
        assertEquals(55, CharacterEconomy.attributeCost(1))
        assertEquals(325, CharacterEconomy.attributeCost(5))
        assertEquals(775, CharacterEconomy.attributeCost(10))
        assertEquals(55, CharacterEconomy.nextAttributeCost(0))
        assertEquals(100, CharacterEconomy.nextAttributeCost(9))
    }

    @Test
    fun fullBuildUsesOneSharedXpBudget() {
        val skills = mapOf(
            "athletics" to CharacterSkill(id = "athletics", definitionId = "athletics", rank = 2),
        )
        val character = DublCharacter(
            id = "economy",
            experience = 2000,
            creationExperience = 2000,
            attributes = defaultAttributes() + mapOf(
                AttributeId.STRENGTH to AttributeValue(base = 2), // 115
                AttributeId.DEXTERITY to AttributeValue(base = 1), // 55
            ),
            skills = skills, // 30
            development = mapOf(
                feat.id to OwnedDevelopment(rank = 2), // 80
                ability.id to OwnedDevelopment(rank = 1), // OS, no XP
            ),
            magic = CharacterMagic(
                manaRank = 2, // 200
                power = 5,
                spells = listOf(
                    KnownSpell(uid = "a", cost = 3, learned = true), // 10
                    KnownSpell(uid = "b", cost = 9, learned = true), // 30
                    KnownSpell(uid = "c", cost = 4, learned = false),
                ),
            ),
            xpAdjustment = -20,
        ).normalized()

        val economy = CharacterEconomy.breakdown(character, catalog)
        assertEquals(170, economy.attributeXp)
        assertEquals(30, economy.skillXp)
        assertEquals(80, economy.developmentXp)
        assertEquals(200, economy.manaXp)
        assertEquals(40, economy.spellXp)
        assertEquals(-20, economy.adjustmentXp)
        assertEquals(500, economy.spentXp)
        assertEquals(1500, economy.remainingXp)
        assertEquals(2, economy.recommendedAbilityPoints)
        assertEquals(2, economy.abilityPointsSpent)
        assertEquals(0, economy.abilityPointsRemaining)
    }

    @Test
    fun progressionXpDoesNotIncreaseCreationAbilityPointRecommendation() {
        val character = DublCharacter(
            id = "progression",
            experience = 3500,
            creationExperience = 2000,
            creationComplete = true,
        )
        assertEquals(2, character.recommendedAbilityPoints)
        assertEquals(2, character.abilityPoints)
    }

    @Test
    fun abilityPointOverrideIsExplicitAndCanExceedRecommendation() {
        val character = DublCharacter(
            id = "override",
            experience = 1000,
            creationExperience = 1000,
            creationComplete = true,
            abilityPointsOverride = 4,
        )
        assertEquals(1, character.recommendedAbilityPoints)
        assertEquals(4, character.abilityPoints)
    }

    @Test
    fun unpricedSpellIsReportedInsteadOfSilentlyPretendingItCostsZero() {
        val character = DublCharacter(
            id = "spell",
            magic = CharacterMagic(
                spells = listOf(KnownSpell(uid = "x", cost = 25, learned = true)),
            ),
        )
        val economy = CharacterEconomy.breakdown(character, catalog)
        assertEquals(1, economy.unpricedLearnedSpells)
        assertEquals(0, economy.spellXp)
    }

    @Test
    fun negativeAttributesFollowLiteralTableAndAreVisibleInBudget() {
        val character = DublCharacter(
            id = "negative",
            experience = 100,
            attributes = defaultAttributes() + (AttributeId.STRENGTH to AttributeValue(base = -1)),
        )
        val economy = CharacterEconomy.breakdown(character, catalog)
        assertEquals(-50, economy.attributeXp)
        assertEquals(150, economy.remainingXp)
        assertFalse(economy.overspentXp)
    }

    @Test
    fun overspendingIsRepresentedInsteadOfClampedAway() {
        val character = DublCharacter(
            id = "overspent",
            experience = 10,
            development = mapOf(feat.id to OwnedDevelopment(rank = 1)),
        )
        val economy = CharacterEconomy.breakdown(character, catalog)
        assertEquals(-30, economy.remainingXp)
        assertTrue(economy.overspentXp)
    }
    @Test
    fun magicSchoolPowerXpIsPartOfTotalSpentExperience() {
        val character = DublCharacter(
            id = "magic-school-xp",
            experience = 1000,
            magic = CharacterMagic(
                schools = listOf(
                    MagicSchool("Разрушение", 4),
                    MagicSchool("Ограждение", 2),
                ),
            ),
        )
        val economy = CharacterEconomy.breakdown(character, catalog)
        assertEquals(150, economy.magicSchoolXp)
        assertEquals(150, economy.spentXp)
        assertEquals(850, economy.remainingXp)
    }

    @Test
    fun chiBonusRanksCostFiftyXpEach() {
        val character = DublCharacter(
            id = "chi-xp",
            experience = 1000,
            chiEnabled = true,
            chiBonusRanks = 3,
        ).normalized()

        val economy = CharacterEconomy.breakdown(character, catalog)
        assertEquals(150, economy.chiXp)
        assertEquals(150, economy.spentXp)
        assertEquals(850, economy.remainingXp)
    }

}
