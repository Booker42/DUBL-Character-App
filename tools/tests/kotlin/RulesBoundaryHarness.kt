import com.dubl.character.android.model.*

fun main() {
    val attrs = AttributeId.entries.associateWith { AttributeValue(base = 0) }.toMutableMap().apply {
        this[AttributeId.WILL] = AttributeValue(base = 4)
    }
    val character = DublCharacter(
        id = "boundary",
        size = 1,
        attributes = attrs,
        chiBonusRanks = 3,
        development = mapOf(
            DevelopmentEffectIds.INTERNAL_CHI to OwnedDevelopment(rank = 1),
            DevelopmentEffectIds.MASTER_CHI to OwnedDevelopment(rank = 2),
            DevelopmentEffectIds.AWAKENED_CHI to OwnedDevelopment(rank = 1),
            DevelopmentEffectIds.QUICK_REFLEXES to OwnedDevelopment(rank = 2),
            DevelopmentEffectIds.IMPROVED_INITIATIVE to OwnedDevelopment(rank = 1),
            DevelopmentEffectIds.STORM_LORD_SCHOOL to OwnedDevelopment(rank = 1),
            DevelopmentEffectIds.STALWART to OwnedDevelopment(rank = 2),
            DevelopmentEffectIds.STILL_MOUNTAIN_SCHOOL to OwnedDevelopment(rank = 1),
            DevelopmentEffectIds.RUNNER to OwnedDevelopment(rank = 3),
        ),
    )
    check(character.strengthSizeModifier == -4)
    check(character.speedSizeModifier == 4)
    check(character.runMultiplier == 0.125)
    check(character.quickReflexesBonus == 2)
    check(character.improvedInitiativeBonus == 1)
    check(character.stormLordBonus == 1)
    check(character.stalwartBonus == 2)
    check(character.stillMountainBonus == 1)
    check(character.runStormSpeedBonus == 1)
    check(character.runRunnerBonus == 3)
    check(character.chiAutomaticAccess)
    check(character.chiActive)
    check(character.chiBaseMaximum == 5)
    check(character.chiProgressionBonus == 7)
    check(character.chiMaximum == 15)
    check(CharacterEconomy.CHI_BONUS_RANK_XP == 50)
    check(CharacterEconomy.chiXp(character) == 150)

    check(RollContext.ATTACK.allowedSkillIds() == listOf("unarmed", "melee_weapon", "shooting", "throwing"))
    check(RollContext.PARRY.allowedSkillIds() == listOf("unarmed", "melee_weapon"))
    check(RollContext.FEINT.allowedSkillIds() == listOf("eloquence", "unarmed", "melee_weapon"))
    check(RollContext.ATTACK.allowedAttributes("shooting") == listOf(AttributeId.PERCEPTION, AttributeId.DEXTERITY))
    check(RollContext.ATTACK.allowedAttributes("throwing") == listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH))
    check(RollContext.PARRY.allowedAttributes("melee_weapon") == listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH))
    check(RollContext.FEINT.allowedAttributes("eloquence") == listOf(AttributeId.CHARISMA))
    check(RollContext.DODGE.allowedAttributes(null).isEmpty())

    val options = listOf(
        SkillRollEffectOption("a", "A", "A", "", numericBonus = 2),
        SkillRollEffectOption("b", "B", "B", "", numericBonus = -1, advantageDice = 2),
        SkillRollEffectOption("c", "C", "C", "", hindranceDice = 3),
    )
    val totals = options.selectedTotals(setOf("a", "b"))
    check(totals.numericBonus == 1)
    check(totals.advantageDice == 2)
    check(totals.hindranceDice == 0)

    check(MagicEquipmentRules.magicSchoolRankXp(0) == 0)
    check(MagicEquipmentRules.magicSchoolRankXp(4) == 100)
    check(MagicEquipmentRules.magicSchoolRankXp(-4) == 0)

    println("RULES_BOUNDARY_OK")
}
