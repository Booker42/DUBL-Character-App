package com.dubl.character.android.ui.development

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DevelopmentGroupVisibilityTest {
    @Test
    fun groupsStartCollapsed() {
        val visibility = DevelopmentGroupVisibility()

        assertFalse(visibility.isExpanded("martial:swords", searchActive = false))
    }

    @Test
    fun severalGroupsCanRemainExpanded() {
        val visibility = DevelopmentGroupVisibility()
            .toggle("regular:craft")
            .toggle("martial:swords")

        assertTrue(visibility.isExpanded("regular:craft", searchActive = false))
        assertTrue(visibility.isExpanded("martial:swords", searchActive = false))
    }

    @Test
    fun activeSearchTemporarilyExpandsMatchingGroups() {
        val visibility = DevelopmentGroupVisibility()

        assertTrue(visibility.isExpanded("martial:swords", searchActive = true))
        assertFalse(visibility.isExpanded("martial:swords", searchActive = false))
    }

    @Test
    fun clearingSearchRestoresManualExpansionState() {
        val visibility = DevelopmentGroupVisibility()
            .toggle("special:assassin")

        assertTrue(visibility.isExpanded("martial:swords", searchActive = true))
        assertTrue(visibility.isExpanded("special:assassin", searchActive = false))
        assertFalse(visibility.isExpanded("martial:swords", searchActive = false))
    }
}
