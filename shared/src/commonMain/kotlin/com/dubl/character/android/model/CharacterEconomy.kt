package com.dubl.character.android.model

data class CharacterEconomyBreakdown(
    val totalExperience: Int,
    val creationExperience: Int,
    val attributeXp: Int,
    val skillXp: Int,
    val developmentXp: Int,
    val manaXp: Int,
    val chiXp: Int,
    val magicSchoolXp: Int,
    val spellXp: Int,
    val adjustmentXp: Int,
    val spentXp: Int,
    val remainingXp: Int,
    val recommendedAbilityPoints: Int,
    val abilityPointsBudget: Int,
    val abilityPointsSpent: Int,
    val abilityPointsRemaining: Int,
    val unpricedLearnedSpells: Int,
) {
    val overspentXp: Boolean get() = remainingXp < 0
    val overspentAbilityPoints: Boolean get() = abilityPointsRemaining < 0
}

object CharacterEconomy {
    private val attributeCumulativeCost = mapOf(
        -5 to -200,
        -4 to -170,
        -3 to -135,
        -2 to -95,
        -1 to -50,
        0 to 0,
        1 to 55,
        2 to 115,
        3 to 180,
        4 to 250,
        5 to 325,
        6 to 405,
        7 to 490,
        8 to 580,
        9 to 675,
        10 to 775,
    )

    fun attributeCost(value: Int): Int? = attributeCumulativeCost[value]

    fun nextAttributeCost(currentValue: Int): Int? {
        val current = attributeCost(currentValue) ?: return null
        val next = attributeCost(currentValue + 1) ?: return null
        return next - current
    }

    fun previousAttributeRefund(currentValue: Int): Int? {
        val current = attributeCost(currentValue) ?: return null
        val previous = attributeCost(currentValue - 1) ?: return null
        return current - previous
    }

    fun attributeXp(character: DublCharacter): Int = AttributeId.entries.sumOf { id ->
        attributeCost(character.attributes[id]?.base ?: 0) ?: 0
    }

    fun developmentXp(
        character: DublCharacter,
        catalog: DevelopmentCatalog,
    ): Int = DevelopmentRules(
        character = character,
        catalog = catalog,
        progress = DevelopmentProgress(character.development),
    ).xpSpentOnDevelopment()

    fun abilityPointsSpent(
        character: DublCharacter,
        catalog: DevelopmentCatalog,
    ): Int = DevelopmentRules(
        character = character,
        catalog = catalog,
        progress = DevelopmentProgress(character.development),
    ).abilityPointsSpent()

    fun unpricedLearnedSpells(character: DublCharacter): Int = character.magic.spells.count { spell ->
        spell.learned && spell.xpOverride == null && MagicEquipmentRules.learnXpCost(spell.cost) == null
    }

    fun chiXp(character: DublCharacter): Int = character.chiBonusRanks.coerceIn(0, 10) * 50

    fun breakdown(
        character: DublCharacter,
        catalog: DevelopmentCatalog,
    ): CharacterEconomyBreakdown {
        val attributes = attributeXp(character)
        val skills = character.skillXpSpent()
        val development = developmentXp(character, catalog)
        val mana = MagicEquipmentRules.manaRankXp(character)
        val chi = chiXp(character)
        val magicSchools = MagicEquipmentRules.magicSchoolPowerXp(character)
        val spells = MagicEquipmentRules.learnedSpellXp(character)
        val adjustment = character.xpAdjustment
        val spent = attributes + skills + development + mana + chi + magicSchools + spells + adjustment
        val abilitySpent = abilityPointsSpent(character, catalog)
        val abilityBudget = character.abilityPoints
        return CharacterEconomyBreakdown(
            totalExperience = character.experience,
            creationExperience = character.effectiveCreationExperience,
            attributeXp = attributes,
            skillXp = skills,
            developmentXp = development,
            manaXp = mana,
            chiXp = chi,
            magicSchoolXp = magicSchools,
            spellXp = spells,
            adjustmentXp = adjustment,
            spentXp = spent,
            remainingXp = character.experience - spent,
            recommendedAbilityPoints = character.recommendedAbilityPoints,
            abilityPointsBudget = abilityBudget,
            abilityPointsSpent = abilitySpent,
            abilityPointsRemaining = abilityBudget - abilitySpent,
            unpricedLearnedSpells = unpricedLearnedSpells(character),
        )
    }
}
