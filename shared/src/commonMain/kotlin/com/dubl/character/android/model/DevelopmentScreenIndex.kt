package com.dubl.character.android.model

enum class DevelopmentEntryKind {
    REGULAR,
    SPECIAL,
    MARTIAL,
    CHI,
}

data class IndexedDevelopmentEntry(
    val entry: DevelopmentEntry,
    val kind: DevelopmentEntryKind,
    val groupName: String,
    val searchText: String,
)

/** Stable catalog metadata used by both Compose frontends. */
class DevelopmentScreenIndex(catalog: DevelopmentCatalog) {
    val all: List<IndexedDevelopmentEntry> = catalog.entries.map { entry ->
        val kind = when {
            entry.isMartialArt -> DevelopmentEntryKind.MARTIAL
            entry.isChiDevelopment -> DevelopmentEntryKind.CHI
            entry.isSpecialDevelopment -> DevelopmentEntryKind.SPECIAL
            else -> DevelopmentEntryKind.REGULAR
        }
        val groupName = when (kind) {
            DevelopmentEntryKind.SPECIAL -> when {
                entry.isAbility -> entry.name
                entry.accessId != null -> catalog.byId(entry.accessId)?.name ?: entry.category.ifBlank { entry.name }
                else -> entry.category.ifBlank { entry.name }
            }
            DevelopmentEntryKind.MARTIAL -> entry.category.ifBlank { "Боевые искусства" }
            DevelopmentEntryKind.CHI -> entry.category.ifBlank { "Развитие ЦИ" }
            DevelopmentEntryKind.REGULAR -> entry.category.ifBlank { "Общие" }
        }
        IndexedDevelopmentEntry(
            entry = entry,
            kind = kind,
            groupName = groupName,
            searchText = developmentNormalize(
                listOf(
                    entry.name,
                    groupName,
                    entry.category,
                    entry.section,
                    entry.requirements,
                    entry.benefit,
                    entry.notes,
                    entry.tags.joinToString(" "),
                ).joinToString(" "),
            ),
        )
    }

    private val byId = all.associateBy { it.entry.id }
    private val byKind = all.groupBy { it.kind }

    fun indexed(entryId: String): IndexedDevelopmentEntry? = byId[entryId]

    fun entries(kind: DevelopmentEntryKind, query: String): List<IndexedDevelopmentEntry> {
        val needle = developmentNormalize(query)
        return byKind[kind].orEmpty().filter { needle.isBlank() || needle in it.searchText }
    }
}
