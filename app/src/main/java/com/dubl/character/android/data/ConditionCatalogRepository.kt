package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.ConditionCatalog

class ConditionCatalogRepository(private val context: Context) {
    fun load(): ConditionCatalog {
        val raw = context.assets.open("conditions_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseConditionCatalog(raw)
    }
}
