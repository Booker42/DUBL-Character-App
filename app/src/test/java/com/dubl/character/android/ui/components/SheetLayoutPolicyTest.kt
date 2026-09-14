package com.dubl.character.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetLayoutPolicyTest {
    @Test
    fun compactGridUsesTwoColumnsAndKeepsOddTail() {
        assertEquals(
            listOf(listOf("a", "b"), listOf("c", "d"), listOf("e")),
            compactGridRows(listOf("a", "b", "c", "d", "e"), columns = 2),
        )
    }
}
