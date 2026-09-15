import com.dubl.character.android.model.*

fun main() {
    val unresolved = DevelopmentEntry(
        id = "unfinished-rule",
        name = "Незавершённое правило",
        section = "Навыки",
        category = "Тест",
        cost = 0,
        costType = DevelopmentCostType.XP,
        maxRank = 1,
        requirements = "",
        benefit = "",
        notes = "",
        tags = emptyList(),
        accessId = null,
        abilityOptions = emptyList(),
        incomplete = true,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )
    val catalog = DevelopmentCatalog("test", listOf(unresolved))
    val character = DublCharacter(id = "c1")
    val rules = DevelopmentRules(character, catalog, DevelopmentProgress())
    val availability = rules.availability(unresolved)

    check(!availability.canIncrease)
    check(availability.canForceIncrease) {
        "Incomplete/unresolved rulebook entries must remain user/GM-overridable instead of becoming a hard lock"
    }
    check(availability.reason.contains("не завершена", ignoreCase = true))
    println("DEVELOPMENT_UNRESOLVED_ESCAPE_OK")
}
