package com.dubl.character.android.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetInteractionPolicyTest {
    @Test
    fun downwardUserOverscrollIsReleasedToParentSheet() {
        assertEquals(0f, sheetContentOverscrollToConsume(18f, fromUserInput = true), 0f)
        assertEquals(0f, sheetContentOverscrollToConsume(-11f, fromUserInput = true), 0f)
    }

    @Test
    fun nonUserScrollIsNotIntercepted() {
        assertEquals(0f, sheetContentOverscrollToConsume(18f, fromUserInput = false), 0f)
    }
}
