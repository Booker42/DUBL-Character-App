package com.dubl.character.android.model

import kotlin.test.Test
import kotlin.test.assertEquals

class DevelopmentScreenIndexTest {
    @Test
    fun martialEntriesAreGroupedWithoutReclassifyingAtRenderTime() {
        val sword = entry(
            id = "martial-sword",
            name = "Школа меча",
            section = "Боевые искусства",
            category = "Фехтование",
        )
        val index = DevelopmentScreenIndex(DevelopmentCatalog("test", listOf(sword)))

        val result = index.entries(DevelopmentEntryKind.MARTIAL, query = "")

        assertEquals(listOf("martial-sword"), result.map { it.entry.id })
        assertEquals("Фехтование", result.single().groupName)
    }

    @Test
    fun searchUsesPrecomputedTextAcrossDescriptionAndTags() {
        val sword = entry(
            id = "martial-sword",
            name = "Школа меча",
            section = "Боевые искусства",
            category = "Фехтование",
            benefit = "Ответный удар после парирования",
            tags = listOf("Защита"),
        )
        val index = DevelopmentScreenIndex(DevelopmentCatalog("test", listOf(sword)))

        assertEquals(listOf("martial-sword"), index.entries(DevelopmentEntryKind.MARTIAL, "парирования").map { it.entry.id })
        assertEquals(listOf("martial-sword"), index.entries(DevelopmentEntryKind.MARTIAL, "ЗАЩИТА").map { it.entry.id })
        assertEquals(emptyList(), index.entries(DevelopmentEntryKind.MARTIAL, "алхимия"))
    }

    private fun entry(
        id: String,
        name: String,
        section: String,
        category: String,
        benefit: String = "",
        tags: List<String> = emptyList(),
    ) = DevelopmentEntry(
        id = id,
        name = name,
        section = section,
        category = category,
        cost = 1,
        costType = DevelopmentCostType.XP,
        maxRank = 1,
        requirements = "",
        benefit = benefit,
        notes = "",
        tags = tags,
        accessId = null,
        abilityOptions = emptyList(),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )
}
