package regression

import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.model.SheetGroupingRules
import com.dubl.character.android.ui.components.compactGridRows
import com.dubl.character.android.ui.components.sheetContentOverscrollToConsume

fun main() {
    // A downward drag at the top of a scrollable sheet must remain available
    // for ModalBottomSheet so the sheet can be dismissed from its content area.
    check(sheetContentOverscrollToConsume(18f, fromUserInput = true) == 0f)
    // Upward overscroll is still contained so nested content doesn't fight the sheet.
    check(sheetContentOverscrollToConsume(-11f, fromUserInput = true) == -11f)
    check(sheetContentOverscrollToConsume(18f, fromUserInput = false) == 0f)

    val groups = listOf(
        SheetGroup("a", "A", listOf("1", "2")),
        SheetGroup("b", "B", listOf("3", "4")),
        SheetGroup("c", "C", listOf("5")),
    )
    val moved = SheetGroupingRules.moveItem(groups, "2", "b", 1)
    check(moved[0].itemIds == listOf("1"))
    check(moved[1].itemIds == listOf("3", "2", "4"))

    val rows = compactGridRows(listOf("a", "b", "c", "d", "e"), columns = 2)
    check(rows == listOf(listOf("a", "b"), listOf("c", "d"), listOf("e")))

    println("Ui061RegressionHarness: OK")
}
