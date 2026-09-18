package com.dubl.character.android.ui.development

/**
 * Manual expansion is deliberately independent from search. Search may reveal
 * matching groups temporarily without mutating the state restored afterwards.
 */
data class DevelopmentGroupVisibility(
    val manuallyExpandedIds: Set<String> = emptySet(),
) {
    fun toggle(groupId: String): DevelopmentGroupVisibility = copy(
        manuallyExpandedIds = if (groupId in manuallyExpandedIds) {
            manuallyExpandedIds - groupId
        } else {
            manuallyExpandedIds + groupId
        },
    )

    fun isExpanded(groupId: String, searchActive: Boolean): Boolean =
        searchActive || groupId in manuallyExpandedIds
}
