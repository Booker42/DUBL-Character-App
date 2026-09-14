package com.dubl.character.android.ui.layout

import kotlin.test.Test
import kotlin.test.assertEquals

class DublLayoutPolicyTest {
    @Test
    fun compactEndsAt899dp() {
        assertEquals(DublLayoutClass.COMPACT, layoutClassForWidth(0))
        assertEquals(DublLayoutClass.COMPACT, layoutClassForWidth(899))
    }

    @Test
    fun normalCovers900Through1319dp() {
        assertEquals(DublLayoutClass.NORMAL, layoutClassForWidth(900))
        assertEquals(DublLayoutClass.NORMAL, layoutClassForWidth(1319))
    }

    @Test
    fun wideStartsAt1320dp() {
        assertEquals(DublLayoutClass.WIDE, layoutClassForWidth(1320))
        assertEquals(DublLayoutClass.WIDE, layoutClassForWidth(2000))
    }
}
