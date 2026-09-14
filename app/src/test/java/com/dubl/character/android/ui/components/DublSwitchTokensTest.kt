package com.dubl.character.android.ui.components

import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DublSwitchTokensTest {
    @Test
    fun uncheckedThumbStaysVisuallyDistinctFromTrack() {
        assertNotEquals(DublSwitchTokens.uncheckedThumb, DublSwitchTokens.uncheckedTrack)
        assertTrue(DublSwitchTokens.uncheckedThumb.alpha >= 0.9f)
    }
}
