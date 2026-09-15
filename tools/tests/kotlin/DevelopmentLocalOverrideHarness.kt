import com.dubl.character.android.model.*

private fun canonicalEntry() = DevelopmentEntry(
    id = "feat-test",
    name = "Канонический навык",
    section = "Навыки",
    category = "Тест",
    cost = 20,
    costType = DevelopmentCostType.XP,
    maxRank = 1,
    requirements = "Сила 2",
    benefit = "Канонический эффект",
    notes = "",
    tags = listOf("canonical"),
    accessId = null,
    abilityOptions = emptyList(),
    incomplete = false,
    repeatable = false,
    perfectRoot = false,
    mechanicsConflict = "",
    conflictNote = "",
)

fun main() {
    val canonical = DevelopmentCatalog("3.69", listOf(canonicalEntry()))
    val overriddenEntry = canonicalEntry().copy(
        name = "Локальная трактовка",
        cost = 35,
        maxRank = 3,
        requirements = "",
        benefit = "Игрок решил иначе",
        tags = listOf("local"),
        incomplete = false,
    )
    val custom = DevelopmentEntry(
        id = "custom-development-1",
        name = "Своя способность",
        section = "Свои",
        category = "Домашние правила",
        cost = 15,
        costType = DevelopmentCostType.XP,
        maxRank = 2,
        requirements = "",
        benefit = "Пользовательская механика",
        notes = "",
        tags = listOf("custom"),
        accessId = null,
        abilityOptions = emptyList(),
        incomplete = false,
        repeatable = true,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )

    val character = DublCharacter(
        id = "hero",
        developmentOverrides = mapOf("feat-test" to overriddenEntry),
        customDevelopmentEntries = listOf(custom),
    )
    val effective = character.effectiveDevelopmentCatalog(canonical)

    val overridden = effective.byId("feat-test") ?: error("override entry missing")
    check(overridden.name == "Локальная трактовка")
    check(overridden.cost == 35)
    check(overridden.maxRank == 3)
    check(overridden.requirements.isBlank())
    check(overridden.benefit == "Игрок решил иначе")
    check(overridden.tags == listOf("local"))
    check(effective.byId("custom-development-1")?.name == "Своя способность")

    val reset = character.copy(developmentOverrides = emptyMap()).effectiveDevelopmentCatalog(canonical)
    check(reset.byId("feat-test")?.name == "Канонический навык")
    check(reset.byId("feat-test")?.cost == 20)
    check(reset.byId("custom-development-1") != null)

    println("DEVELOPMENT_LOCAL_OVERRIDE_OK")
}
