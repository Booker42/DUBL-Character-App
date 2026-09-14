package com.dubl.character.android.ui.components

internal fun sheetContentOverscrollToConsume(
    availableY: Float,
    fromUserInput: Boolean,
): Float = 0f

internal fun <T> compactGridRows(items: List<T>, columns: Int = 2): List<List<T>> {
    require(columns > 0) { "columns must be positive" }
    return items.chunked(columns)
}
