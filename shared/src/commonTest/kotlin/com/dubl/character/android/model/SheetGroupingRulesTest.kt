package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.Test

class SheetGroupingRulesTest {
    @Test
    fun persistedGroupingKeepsLegacyFormEncodingAndRoundTripsUnicode() {
        val groups = listOf(
            SheetGroup(
                id = "group +/%",
                title = "Бой / защита",
                itemIds = listOf("a+b c"),
                collapsed = true,
            ),
        )

        val encoded = SheetGroupingRules.encode(groups)

        assertEquals(
            "group+%2B%2F%25\u001f%D0%91%D0%BE%D0%B9+%2F+%D0%B7%D0%B0%D1%89%D0%B8%D1%82%D0%B0\u001f1\u001fa%2Bb+c",
            encoded,
        )
        assertEquals(groups, SheetGroupingRules.decode(encoded))
    }

    @Test
    fun movingItemDirectlyToAnotherGroupPreservesEveryOtherItem() {
        val groups = listOf(
            SheetGroup("combat", "Бой", listOf("parry", "block")),
            SheetGroup("travel", "Дорога", listOf("survival")),
        )

        val moved = SheetGroupingRules.moveItem(
            groups = groups,
            itemId = "block",
            targetGroupId = "travel",
            targetIndex = 1,
        )

        assertEquals(listOf("parry"), moved[0].itemIds)
        assertEquals(listOf("survival", "block"), moved[1].itemIds)
    }

    @Test
    fun movingItemWithinSameGroupCanTargetExactPosition() {
        val groups = listOf(SheetGroup("g", "G", listOf("a", "b", "c", "d")))

        val moved = SheetGroupingRules.moveItems(groups, listOf("c"), "g", 1)

        assertEquals(listOf("a", "c", "b", "d"), moved.single().itemIds)
    }

    @Test
    fun movingRootTreePullsDescendantsFromAllGroupsAsOneBlock() {
        val groups = listOf(
            SheetGroup("one", "One", listOf("root", "child-a", "other")),
            SheetGroup("two", "Two", listOf("child-b", "grandchild")),
        )
        val parents = mapOf(
            "root" to null,
            "child-a" to "root",
            "child-b" to "root",
            "grandchild" to "child-b",
            "other" to null,
        )
        val visualOrder = groups.flatMap { it.itemIds }
        val block = SheetGroupingRules.subtreeBlock("root", parents, visualOrder)

        val moved = SheetGroupingRules.moveItems(groups, block, "two", 0)

        assertEquals(listOf("other"), moved[0].itemIds)
        assertEquals(listOf("root", "child-a", "child-b", "grandchild"), moved[1].itemIds)
    }

    @Test
    fun extractingOneChildDoesNotPullItsDescendants() {
        val groups = listOf(
            SheetGroup("tree", "Tree", listOf("root", "child", "grandchild")),
            SheetGroup("custom", "Custom", emptyList()),
        )

        val moved = SheetGroupingRules.moveItems(groups, listOf("child"), "custom", 0)

        assertEquals(listOf("root", "grandchild"), moved[0].itemIds)
        assertEquals(listOf("child"), moved[1].itemIds)
    }

    @Test
    fun hierarchyReconnectsAutomaticallyWhenParentAndChildShareGroup() {
        val ids = listOf("sibling", "child-b", "root", "child-a", "grandchild")
        val parents = mapOf(
            "root" to null,
            "child-a" to "root",
            "child-b" to "root",
            "grandchild" to "child-a",
            "sibling" to null,
        )

        val ordered = SheetGroupingRules.hierarchicalOrder(ids, parents)

        assertEquals(listOf("sibling", "root", "child-b", "child-a", "grandchild"), ordered)
    }

    @Test
    fun groupCanBeDroppedAtExactIndex() {
        val groups = listOf(
            SheetGroup("a", "A"),
            SheetGroup("b", "B"),
            SheetGroup("c", "C"),
        )

        val moved = SheetGroupingRules.moveGroupToIndex(groups, "c", 0)

        assertEquals(listOf("c", "a", "b"), moved.map { it.id })
    }

    @Test
    fun balancedColumnsDoNotShareRowHeight() {
        val items = listOf("short-a", "tall", "short-b", "short-c")
        val columns = SheetGroupingRules.balancedColumns(items) { if (it == "tall") 2 else 1 }

        assertEquals(listOf("short-a", "short-b"), columns.first)
        assertEquals(listOf("tall", "short-c"), columns.second)
    }


    @Test
    fun siblingsCanBeReorderedWhileStayingLinkedToParent() {
        val parents = mapOf("root" to null, "a" to "root", "b" to "root")
        val groups = listOf(SheetGroup("g", "G", listOf("root", "a", "b")))

        val moved = SheetGroupingRules.moveItems(groups, listOf("b"), "g", 1)
        val ordered = SheetGroupingRules.hierarchicalOrder(moved.single().itemIds, parents)

        assertEquals(listOf("root", "b", "a"), ordered)
        assertEquals(1, SheetGroupingRules.localDepth("b", ordered, parents))
        assertEquals(1, SheetGroupingRules.localDepth("a", ordered, parents))
    }


    @Test
    fun hierarchyBlocksKeepParentAndChildrenTogetherForTwoColumnLayout() {
        val ids = listOf("root", "child-a", "child-b", "solo")
        val parents = mapOf("root" to null, "child-a" to "root", "child-b" to "root", "solo" to null)

        val blocks = SheetGroupingRules.hierarchyBlocks(ids, parents)

        assertEquals(listOf(listOf("root", "child-a", "child-b"), listOf("solo")), blocks)
    }

}
