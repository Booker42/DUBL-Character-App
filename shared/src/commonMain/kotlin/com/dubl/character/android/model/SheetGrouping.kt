package com.dubl.character.android.model

data class SheetGroup(
    val id: String,
    val title: String,
    val itemIds: List<String> = emptyList(),
    val collapsed: Boolean = false,
)

object SheetGroupingRules {
    private const val GROUP_SEPARATOR = "\u001e"
    private const val FIELD_SEPARATOR = "\u001f"
    private const val ITEM_SEPARATOR = "\u001d"

    fun normalize(
        saved: List<SheetGroup>,
        defaults: List<SheetGroup>,
        validItemIds: List<String>,
        ungroupedId: String,
        ungroupedTitle: String = "Без группы",
    ): List<SheetGroup> {
        val valid = validItemIds.toSet()
        if (valid.isEmpty()) {
            return saved.map { it.copy(itemIds = emptyList()) }
        }

        if (saved.isEmpty()) {
            val seen = mutableSetOf<String>()
            val result = defaults.mapNotNull { group ->
                val items = group.itemIds.filter { it in valid && seen.add(it) }
                group.copy(itemIds = items).takeIf { items.isNotEmpty() }
            }.toMutableList()
            val missing = validItemIds.filter { seen.add(it) }
            if (missing.isNotEmpty()) result += SheetGroup(ungroupedId, ungroupedTitle, missing)
            return result
        }

        val seen = mutableSetOf<String>()
        val result = saved.map { group ->
            group.copy(itemIds = group.itemIds.filter { it in valid && seen.add(it) })
        }.toMutableList()
        val missing = validItemIds.filter { seen.add(it) }
        if (missing.isNotEmpty()) {
            val ungroupedIndex = result.indexOfFirst { it.id == ungroupedId }
            if (ungroupedIndex >= 0) {
                val group = result[ungroupedIndex]
                result[ungroupedIndex] = group.copy(itemIds = group.itemIds + missing)
            } else {
                result += SheetGroup(ungroupedId, ungroupedTitle, missing)
            }
        }
        return result
    }

    fun moveItems(
        groups: List<SheetGroup>,
        itemIds: List<String>,
        targetGroupId: String,
        targetIndex: Int,
    ): List<SheetGroup> {
        if (groups.none { it.id == targetGroupId }) return groups
        val existing = groups.flatMap { it.itemIds }.toSet()
        val moving = itemIds.distinct().filter { it in existing }
        if (moving.isEmpty()) return groups
        val movingSet = moving.toSet()
        val without = groups.map { group ->
            group.copy(itemIds = group.itemIds.filterNot { it in movingSet })
        }.toMutableList()
        val groupIndex = without.indexOfFirst { it.id == targetGroupId }
        val target = without[groupIndex]
        val insertion = targetIndex.coerceIn(0, target.itemIds.size)
        val items = target.itemIds.toMutableList().apply { addAll(insertion, moving) }
        without[groupIndex] = target.copy(itemIds = items)
        return without
    }

    fun subtreeBlock(
        rootId: String,
        parentById: Map<String, String?>,
        itemOrder: List<String>,
    ): List<String> {
        fun belongsToRoot(itemId: String): Boolean {
            var current: String? = itemId
            val visited = mutableSetOf<String>()
            while (current != null && visited.add(current)) {
                if (current == rootId) return true
                current = parentById[current]
            }
            return false
        }
        return itemOrder.distinct().filter(::belongsToRoot)
    }

    fun hierarchicalOrder(
        itemIds: List<String>,
        parentById: Map<String, String?>,
    ): List<String> {
        val distinct = itemIds.distinct()
        val present = distinct.toSet()
        val children = distinct.groupBy { id -> parentById[id]?.takeIf { it in present } }
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()

        fun append(id: String) {
            if (!visited.add(id)) return
            result += id
            children[id].orEmpty().forEach(::append)
        }

        children[null].orEmpty().forEach(::append)
        distinct.filterNot { it in visited }.forEach(::append)
        return result
    }

    fun hierarchyBlocks(
        itemIds: List<String>,
        parentById: Map<String, String?>,
    ): List<List<String>> {
        val ordered = hierarchicalOrder(itemIds, parentById)
        val present = ordered.toSet()
        val roots = ordered.filter { id -> parentById[id]?.takeIf { it in present } == null }
        return roots.map { root -> subtreeBlock(root, parentById, ordered) }
    }

    fun localDepth(
        itemId: String,
        groupItemIds: Collection<String>,
        parentById: Map<String, String?>,
    ): Int {
        val present = groupItemIds.toSet()
        var depth = 0
        var current = parentById[itemId]
        val visited = mutableSetOf(itemId)
        while (current != null && current in present && visited.add(current)) {
            depth += 1
            current = parentById[current]
        }
        return depth
    }

    fun moveGroupToIndex(groups: List<SheetGroup>, id: String, targetIndex: Int): List<SheetGroup> {
        val index = groups.indexOfFirst { it.id == id }
        if (index < 0) return groups
        val result = groups.toMutableList()
        val moving = result.removeAt(index)
        result.add(targetIndex.coerceIn(0, result.size), moving)
        return result
    }

    fun <T> balancedColumns(
        items: List<T>,
        weight: (T) -> Int = { 1 },
    ): Pair<List<T>, List<T>> {
        val left = mutableListOf<T>()
        val right = mutableListOf<T>()
        var leftWeight = 0
        var rightWeight = 0
        items.forEach { item ->
            val itemWeight = weight(item).coerceAtLeast(1)
            val toLeft = when {
                leftWeight < rightWeight -> true
                rightWeight < leftWeight -> false
                else -> left.size <= right.size
            }
            if (toLeft) {
                left += item
                leftWeight += itemWeight
            } else {
                right += item
                rightWeight += itemWeight
            }
        }
        return left to right
    }

    fun moveItem(
        groups: List<SheetGroup>,
        itemId: String,
        targetGroupId: String,
        targetIndex: Int,
    ): List<SheetGroup> = moveItems(groups, listOf(itemId), targetGroupId, targetIndex)

    /** Move one visual slot. Crossing a group edge moves into the adjacent group. */
    fun moveItemByStep(groups: List<SheetGroup>, itemId: String, direction: Int): List<SheetGroup> {
        if (direction == 0) return groups
        val groupIndex = groups.indexOfFirst { itemId in it.itemIds }
        if (groupIndex < 0) return groups
        val group = groups[groupIndex]
        val itemIndex = group.itemIds.indexOf(itemId)

        return if (direction < 0) {
            when {
                itemIndex > 0 -> moveItem(groups, itemId, group.id, itemIndex - 1)
                groupIndex > 0 -> {
                    val previous = groups[groupIndex - 1]
                    moveItem(groups, itemId, previous.id, previous.itemIds.size)
                }
                else -> groups
            }
        } else {
            when {
                itemIndex < group.itemIds.lastIndex -> moveItem(groups, itemId, group.id, itemIndex + 1)
                groupIndex < groups.lastIndex -> {
                    val next = groups[groupIndex + 1]
                    moveItem(groups, itemId, next.id, 0)
                }
                else -> groups
            }
        }
    }

    fun addGroup(groups: List<SheetGroup>, id: String, title: String): List<SheetGroup> {
        val normalizedTitle = title.trim().ifBlank { "Новая группа" }
        if (groups.any { it.id == id }) return groups
        return groups + SheetGroup(id = id, title = normalizedTitle)
    }

    fun renameGroup(groups: List<SheetGroup>, id: String, title: String): List<SheetGroup> =
        groups.map { group ->
            if (group.id == id) group.copy(title = title.trim().ifBlank { group.title }) else group
        }

    fun toggleCollapsed(groups: List<SheetGroup>, id: String): List<SheetGroup> =
        groups.map { group -> if (group.id == id) group.copy(collapsed = !group.collapsed) else group }

    fun moveGroup(groups: List<SheetGroup>, id: String, direction: Int): List<SheetGroup> {
        val index = groups.indexOfFirst { it.id == id }
        if (index < 0) return groups
        val target = (index + direction.sign()).coerceIn(0, groups.lastIndex)
        if (target == index) return groups
        return groups.toMutableList().apply {
            val group = removeAt(index)
            add(target, group)
        }
    }

    fun deleteGroup(
        groups: List<SheetGroup>,
        id: String,
        ungroupedId: String,
        ungroupedTitle: String = "Без группы",
    ): List<SheetGroup> {
        val removed = groups.firstOrNull { it.id == id } ?: return groups
        if (id == ungroupedId) return groups
        val result = groups.filterNot { it.id == id }.toMutableList()
        if (removed.itemIds.isEmpty()) return result
        val index = result.indexOfFirst { it.id == ungroupedId }
        if (index >= 0) {
            val target = result[index]
            result[index] = target.copy(itemIds = (target.itemIds + removed.itemIds).distinct())
        } else {
            result += SheetGroup(ungroupedId, ungroupedTitle, removed.itemIds)
        }
        return result
    }

    fun encode(groups: List<SheetGroup>): String = groups.joinToString(GROUP_SEPARATOR) { group ->
        listOf(
            encodeToken(group.id),
            encodeToken(group.title),
            if (group.collapsed) "1" else "0",
            group.itemIds.joinToString(ITEM_SEPARATOR) { encodeToken(it) },
        ).joinToString(FIELD_SEPARATOR)
    }

    fun decode(raw: String?): List<SheetGroup> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(GROUP_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            if (fields.size < 4) return@mapNotNull null
            val id = decodeToken(fields[0]).takeIf { it.isNotBlank() } ?: return@mapNotNull null
            SheetGroup(
                id = id,
                title = decodeToken(fields[1]).ifBlank { "Группа" },
                collapsed = fields[2] == "1",
                itemIds = fields[3]
                    .split(ITEM_SEPARATOR)
                    .filter { it.isNotBlank() }
                    .map(::decodeToken)
                    .filter { it.isNotBlank() }
                    .distinct(),
            )
        }
    }

    private fun Int.sign(): Int = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }

    private fun encodeToken(value: String): String = formEncode(value)
    private fun decodeToken(value: String): String = runCatching { formDecode(value) }.getOrDefault(value)
}

private const val HEX = "0123456789ABCDEF"

private fun isFormSafe(byte: Int): Boolean =
    byte in 'a'.code..'z'.code ||
        byte in 'A'.code..'Z'.code ||
        byte in '0'.code..'9'.code ||
        byte == '-'.code || byte == '_'.code || byte == '.'.code || byte == '*'.code

private fun formEncode(value: String): String = buildString {
    value.encodeToByteArray().forEach { signedByte ->
        val byte = signedByte.toInt() and 0xff
        when {
            byte == 0x20 -> append('+')
            isFormSafe(byte) -> append(byte.toChar())
            else -> {
                append('%')
                append(HEX[byte ushr 4])
                append(HEX[byte and 0x0f])
            }
        }
    }
}

private fun formDecode(value: String): String {
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < value.length) {
        when (value[index]) {
            '+' -> {
                bytes += 0x20
                index += 1
            }
            '%' -> {
                require(index + 2 < value.length) { "Incomplete percent escape" }
                val high = value[index + 1].digitToIntOrNull(16) ?: error("Invalid percent escape")
                val low = value[index + 2].digitToIntOrNull(16) ?: error("Invalid percent escape")
                bytes += ((high shl 4) or low).toByte()
                index += 3
            }
            else -> {
                val nextEscape = value.indexOfAny(charArrayOf('+', '%'), startIndex = index)
                    .takeIf { it >= 0 }
                    ?: value.length
                bytes += value.substring(index, nextEscape).encodeToByteArray().toList()
                index = nextEscape
            }
        }
    }
    return bytes.toByteArray().decodeToString(throwOnInvalidSequence = true)
}
