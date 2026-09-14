package com.dubl.character.android.model

data class ChiSchool(
    val id: String,
    val name: String,
    val requirements: String,
    val passives: List<String>,
)

data class ChiTechnique(
    val id: String,
    val name: String,
    val school: String,
    val chiCost: Int,
    val action: String,
    val effect: String,
    val requirements: String,
)

data class ChiCatalog(
    val version: String,
    val schools: List<ChiSchool>,
    val techniques: List<ChiTechnique>,
)

data class ChiTechniqueAvailability(
    val technique: ChiTechnique,
    val checks: List<RequirementCheck>,
    val unlocked: Boolean,
    val canUse: Boolean,
    val chiCost: Int,
    val reason: String,
)

class ChiRules(
    private val character: DublCharacter,
    developmentCatalog: DevelopmentCatalog,
) {
    private val developmentRules = DevelopmentRules(
        character = character,
        catalog = developmentCatalog,
        progress = DevelopmentProgress(character.development),
    )

    fun availability(technique: ChiTechnique): ChiTechniqueAvailability {
        val pseudo = DevelopmentEntry(
            id = "chi-technique:${technique.id}",
            name = technique.name,
            section = "ЦИ",
            category = technique.school,
            cost = 0,
            costType = DevelopmentCostType.XP,
            maxRank = 1,
            requirements = technique.requirements,
            benefit = technique.effect,
            notes = technique.action,
            tags = listOf("ЦИ", "Приём ЦИ"),
            accessId = null,
            abilityOptions = emptyList(),
            incomplete = false,
            repeatable = false,
            perfectRoot = false,
            mechanicsConflict = "",
            conflictNote = "",
        )
        val checks = developmentRules.requirements(pseudo)
        val unlocked = checks.all { it.status == RequirementStatus.OK }
        val enoughChi = technique.chiCost <= character.chiCurrent
        val reason = when {
            !character.chiActive -> "Запас ЦИ недоступен"
            !unlocked -> checks.filter { it.status != RequirementStatus.OK }.joinToString("; ") { it.text }
            !enoughChi -> "Недостаточно ЦИ: нужно ${technique.chiCost}"
            else -> ""
        }
        return ChiTechniqueAvailability(
            technique = technique,
            checks = checks,
            unlocked = unlocked,
            canUse = character.chiActive && unlocked && enoughChi,
            chiCost = technique.chiCost,
            reason = reason,
        )
    }
}
