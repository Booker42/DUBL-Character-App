package com.dubl.character.android.ui.layout

enum class DublLayoutClass {
    COMPACT,
    NORMAL,
    WIDE,
}

const val COMPACT_MAX_WIDTH_DP = 899
const val WIDE_MIN_WIDTH_DP = 1320

fun layoutClassForWidth(widthDp: Int): DublLayoutClass = when {
    widthDp <= COMPACT_MAX_WIDTH_DP -> DublLayoutClass.COMPACT
    widthDp >= WIDE_MIN_WIDTH_DP -> DublLayoutClass.WIDE
    else -> DublLayoutClass.NORMAL
}
