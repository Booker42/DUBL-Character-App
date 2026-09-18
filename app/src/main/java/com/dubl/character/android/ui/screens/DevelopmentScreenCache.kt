package com.dubl.character.android.ui.screens

internal class RetainedPreparationCache<K, V>(
    private val maximumEntries: Int,
) {
    private val values = LinkedHashMap<K, V>()

    @Synchronized
    fun getOrPut(key: K, producer: () -> V): V {
        values.remove(key)?.let { existing ->
            values[key] = existing
            return existing
        }
        return producer().also { value ->
            values[key] = value
            while (values.size > maximumEntries.coerceAtLeast(1)) {
                values.remove(values.keys.first())
            }
        }
    }
}
