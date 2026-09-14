package com.dubl.character.android.model

enum class SkillEffectMode {
    AUTO_BONUS,
    TOGGLE_BONUS,
    TOGGLE_ADVANTAGE,
    TOGGLE_HINDRANCE,
    TOGGLE_RULE,
    ALTERNATIVE,
    PRESET,
    REROLL,
    FORMULA,
    REMINDER,
}

data class SkillEffectDefinition(
    val id: String,
    val reviewIndex: Int,
    val sourceName: String,
    val effectText: String,
    val targetText: String,
    val status: String,
    val plan: String,
    val mode: SkillEffectMode,
    val rollContext: RollContext?,
    val targetSkills: List<String>,
    val value: Int,
    val perRank: Boolean,
    val toggleLabel: String,
)

data class SkillEffectCatalog(
    val version: String,
    val effects: List<SkillEffectDefinition>,
)

data class SkillRollEffectOption(
    val id: String,
    val label: String,
    val sourceName: String,
    val description: String,
    val numericBonus: Int = 0,
    val advantageDice: Int = 0,
    val hindranceDice: Int = 0,
)

data class SkillRollEffectResolution(
    val automaticContributions: List<RollContribution> = emptyList(),
    val options: List<SkillRollEffectOption> = emptyList(),
    val reminders: List<SkillEffectDefinition> = emptyList(),
) {
    val automaticBonus: Int get() = automaticContributions.sumOf { it.value }
}

class SkillEffectRules(
    private val character: DublCharacter,
    private val developmentCatalog: DevelopmentCatalog,
    private val effectCatalog: SkillEffectCatalog,
) {
    fun forSkill(skill: ResolvedSkill): SkillRollEffectResolution {
        val relevant = effectCatalog.effects.filter { effect ->
            effect.rollContext == RollContext.SKILL && effect.matchesSkill(skill) && ownedRank(effect.sourceName) > 0
        }
        val automatic = relevant.mapNotNull { effect ->
            if (effect.mode != SkillEffectMode.AUTO_BONUS) return@mapNotNull null
            val value = effect.scaledValue(ownedRank(effect.sourceName))
            if (value == 0) null else RollContribution(effect.sourceName, value)
        }
        val options = relevant.mapNotNull { effect ->
            val rank = ownedRank(effect.sourceName)
            when (effect.mode) {
                SkillEffectMode.TOGGLE_BONUS -> SkillRollEffectOption(
                    id = effect.id,
                    label = effect.toggleLabel,
                    sourceName = effect.sourceName,
                    description = effect.effectText,
                    numericBonus = effect.scaledValue(rank),
                )
                SkillEffectMode.TOGGLE_ADVANTAGE -> SkillRollEffectOption(
                    id = effect.id,
                    label = effect.toggleLabel,
                    sourceName = effect.sourceName,
                    description = effect.effectText,
                    advantageDice = if (effect.perRank) rank.coerceAtLeast(1) else 1,
                )
                SkillEffectMode.TOGGLE_HINDRANCE -> SkillRollEffectOption(
                    id = effect.id,
                    label = effect.toggleLabel,
                    sourceName = effect.sourceName,
                    description = effect.effectText,
                    hindranceDice = if (effect.perRank) rank.coerceAtLeast(1) else 1,
                )
                SkillEffectMode.TOGGLE_RULE -> SkillRollEffectOption(
                    id = effect.id,
                    label = effect.toggleLabel,
                    sourceName = effect.sourceName,
                    description = effect.effectText,
                )
                else -> null
            }
        }
        val reminders = relevant.filter { effect ->
            effect.mode in setOf(
                SkillEffectMode.ALTERNATIVE,
                SkillEffectMode.PRESET,
                SkillEffectMode.REROLL,
                SkillEffectMode.REMINDER,
            )
        }
        return SkillRollEffectResolution(
            automaticContributions = automatic,
            options = options,
            reminders = reminders,
        )
    }

    fun forContext(context: RollContext): List<SkillEffectDefinition> = effectCatalog.effects.filter { effect ->
        effect.rollContext == context && ownedRank(effect.sourceName) > 0
    }

    private fun ownedRank(sourceName: String): Int = developmentCatalog.matchingName(sourceName)
        .maxOfOrNull { entry -> character.development[entry.id]?.rank ?: 0 }
        ?: 0

    private fun SkillEffectDefinition.scaledValue(rank: Int): Int = if (perRank) value * rank else value

    private fun SkillEffectDefinition.matchesSkill(skill: ResolvedSkill): Boolean {
        if (targetSkills.isEmpty()) return false
        val skillName = developmentNormalize(skill.name)
        return targetSkills.any { target ->
            val normalized = developmentNormalize(target)
            when (normalized) {
                "знание" -> skill.category == SkillCategory.KNOWLEDGE && skillName.startsWith("знание")
                "исполнение" -> skillName.startsWith("исполнение")
                "ремесло" -> skillName.startsWith("ремесло")
                else -> skillName == normalized
            }
        }
    }
}
