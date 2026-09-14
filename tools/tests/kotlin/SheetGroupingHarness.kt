import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.model.SheetGroupingRules

fun main() {
    val defaults = listOf(
        SheetGroup("combat", "Бой", listOf("a", "b")),
        SheetGroup("social", "Социальные", listOf("c")),
    )
    val initial = SheetGroupingRules.normalize(emptyList(), defaults, listOf("a", "b", "c"), "ungrouped")
    check(initial.map { it.itemIds } == listOf(listOf("a", "b"), listOf("c")))

    val moved = SheetGroupingRules.moveItemByStep(initial, "b", 1)
    check(moved[0].itemIds == listOf("a"))
    check(moved[1].itemIds == listOf("b", "c"))

    val collapsed = SheetGroupingRules.toggleCollapsed(moved, "social")
    check(collapsed[1].collapsed)

    val withGroup = SheetGroupingRules.addGroup(collapsed, "user-1", " Мой бой ")
    check(withGroup.last().title == "Мой бой")
    val movedCustom = SheetGroupingRules.moveItem(withGroup, "a", "user-1", 0)
    check(movedCustom.last().itemIds == listOf("a"))

    val encoded = SheetGroupingRules.encode(movedCustom)
    check(SheetGroupingRules.decode(encoded) == movedCustom)

    val normalized = SheetGroupingRules.normalize(
        movedCustom,
        defaults,
        listOf("a", "b", "c", "new"),
        "ungrouped",
    )
    check(normalized.last().id == "ungrouped")
    check(normalized.last().itemIds == listOf("new"))

    val deleted = SheetGroupingRules.deleteGroup(normalized, "user-1", "ungrouped")
    check(deleted.none { it.id == "user-1" })
    check(deleted.last().itemIds.contains("a"))

    println("SheetGroupingHarness: OK")
}
