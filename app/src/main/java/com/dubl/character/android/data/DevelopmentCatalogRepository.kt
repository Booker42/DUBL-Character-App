package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.DevelopmentCatalog

class DevelopmentCatalogRepository(private val context: Context) {
    fun load(): DevelopmentCatalog {
        val raw = context.assets.open("development_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseDevelopmentCatalog(raw)
    }
}
