package com.dubl.character.android.data

import com.dubl.character.android.model.*

private fun entry(id: String, name: String, cost: Int) = DevelopmentEntry(
    id = id,
    name = name,
    section = "Навыки",
    category = "Тест",
    cost = cost,
    costType = DevelopmentCostType.XP,
    maxRank = 2,
    requirements = "Воля 2",
    benefit = "Эффект $name",
    notes = "Заметка",
    tags = listOf("tag"),
    accessId = null,
    abilityOptions = emptyList(),
    incomplete = false,
    repeatable = true,
    perfectRoot = false,
    mechanicsConflict = "",
    conflictNote = "",
)

fun main() {
    val override = entry("feat-test", "Локальная версия", 45)
    val custom = entry("custom-development-42", "Своя способность", 17)
    val character = DublCharacter(
        id = "hero",
        developmentOverrides = mapOf(override.id to override),
        customDevelopmentEntries = listOf(custom),
    )
    val raw = SnapshotCodec.encode(AppSnapshot(listOf(character), character.id))
    val decoded = SnapshotCodec.decode(raw) { "generated" }.activeCharacter

    val restoredOverride = decoded.developmentOverrides[override.id] ?: error("override lost")
    check(restoredOverride == override) { "override changed: $restoredOverride" }
    check(decoded.customDevelopmentEntries == listOf(custom)) { "custom development lost" }
    check(SnapshotCodec.SCHEMA >= 10)

    println("DEVELOPMENT_OVERRIDE_PERSISTENCE_OK")
}
