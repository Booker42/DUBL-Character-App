package com.dubl.character.android.model

fun main() {
    val a = DevelopmentEntry(
        id = "feat_a", name = "Технарь", section = "Навыки", category = "Компьютеры",
        cost = 10, costType = DevelopmentCostType.XP, maxRank = 5, requirements = "", benefit = "+1",
        notes = "", tags = emptyList(), accessId = null, abilityOptions = emptyList(), incomplete = false,
        repeatable = false, perfectRoot = false, mechanicsConflict = "", conflictNote = "",
    )
    val b = a.copy(id = "feat_b", category = "Киберпанк")
    val catalog = DevelopmentCatalog("test", listOf(a, b))
    val effect = SkillEffectDefinition(
        id = "effect_b",
        reviewIndex = 1,
        sourceName = "Технарь",
        effectText = "+1",
        targetText = "SKILL · Компьютеры",
        status = "test",
        plan = "test",
        mode = SkillEffectMode.AUTO_BONUS,
        rollContext = RollContext.SKILL,
        targetSkills = listOf("Компьютеры"),
        value = 1,
        perRank = true,
        toggleLabel = "Технарь",
        developmentId = "feat_b",
    )
    val effects = SkillEffectCatalog("test", listOf(effect))
    val character = DublCharacter(
        id = "c",
        development = mapOf("feat_a" to OwnedDevelopment(rank = 3)),
    )
    val computers = character.resolveSkill("computers") ?: error("computers missing")
    check(SkillEffectRules(character, catalog, effects).forSkill(computers).automaticBonus == 0) {
        "effect bound to feat_b must not inherit feat_a rank just because names match"
    }
    println("SKILL_EFFECT_STABLE_ID_OK")
}
