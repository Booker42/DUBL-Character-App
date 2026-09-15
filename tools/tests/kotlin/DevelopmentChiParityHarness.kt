import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.model.*
import com.dubl.character.android.state.CharacterSession

private fun entry(
    id: String,
    name: String,
    section: String,
    category: String = section,
    cost: Int = 100,
    costType: DevelopmentCostType = DevelopmentCostType.XP,
    requirements: String = "-",
    tags: List<String> = emptyList(),
    accessId: String? = null,
    abilityOptions: List<AbilityOption> = emptyList(),
): DevelopmentEntry = DevelopmentEntry(
    id = id,
    name = name,
    section = section,
    category = category,
    cost = cost,
    costType = costType,
    maxRank = 1,
    requirements = requirements,
    benefit = "",
    notes = "",
    tags = tags,
    accessId = accessId,
    abilityOptions = abilityOptions,
    incomplete = false,
    repeatable = false,
    perfectRoot = false,
    mechanicsConflict = "",
    conflictNote = "",
)

fun main() {
    val access = entry(
        id = "branch-access",
        name = "Тестовая ветка",
        section = "Ветки способностей",
        cost = 2,
        costType = DevelopmentCostType.ABILITY,
        abilityOptions = listOf(AbilityOption("Источник", 2)),
    )
    val child = entry(
        id = "branch-child",
        name = "Дочерний навык",
        section = "Ветки способностей",
        category = "Тестовая ветка",
        accessId = access.id,
    )
    val style = entry(
        id = "martial-style",
        name = "Тестовый стиль",
        section = "Боевые искусства",
        category = "Тестовый стиль",
        tags = listOf("Боевые искусства", "Боевой стиль"),
    )
    val technique = entry(
        id = "martial-technique",
        name = "Тестовый приём",
        section = "Боевые искусства",
        category = "Тестовый стиль",
        requirements = "Боевые искусства (Тестовый стиль)",
        tags = listOf("Боевые искусства"),
    )
    val internalChi = entry(
        id = DevelopmentEffectIds.INTERNAL_CHI,
        name = "Внутренняя Ци",
        section = "ЦИ",
        cost = 1,
        costType = DevelopmentCostType.ABILITY,
        tags = listOf("ЦИ"),
        abilityOptions = listOf(AbilityOption("ЦИ", 1)),
    )
    val catalog = DevelopmentCatalog("test", listOf(access, child, style, technique, internalChi))

    val attrs = defaultAttributes() + (AttributeId.WILL to AttributeValue(base = 4))
    val character = DublCharacter(id = "c1", experience = 10_000, creationExperience = 10_000, attributes = attrs)
    val session = CharacterSession(
        InMemoryCharacterStore(AppSnapshot(listOf(character), character.id)),
        idFactory = { "unused" },
    )

    fun rules() = DevelopmentRules(session.active, catalog, DevelopmentProgress(session.active.development))

    check(!rules().availability(child).canIncrease)
    session.setDevelopmentRank(access.id, 1, 0)
    check(rules().availability(child).canIncrease)

    val martialBefore = rules().availability(technique)
    check(!martialBefore.canIncrease)
    check(martialBefore.checks.any { it.targetEntryId == style.id })
    session.setDevelopmentRank(style.id, 1)
    check(rules().availability(technique).canIncrease)

    session.setDevelopmentRank(internalChi.id, 1, 0)
    check(session.active.chiActive)
    session.setChiEnabled(false)
    check(session.active.chiActive)

    session.setChiBonusRanks(2)
    check(session.active.chiMaximum == maxOf(3, session.active.will + 1) + 2)
    session.restoreChi()
    val techniqueChi = ChiTechnique("chi-test", "Импульс", "Базовые", 2, "1 ОД", "", "Внутренняя Ци")
    val chiAvailability = ChiRules(session.active, catalog).availability(techniqueChi)
    check(chiAvailability.unlocked)
    check(chiAvailability.canUse)
    val beforeSpend = session.active.chiCurrent
    session.changeChi(-chiAvailability.chiCost)
    check(session.active.chiCurrent == beforeSpend - 2)

    println("DEVELOPMENT_CHI_PARITY_OK")
}
