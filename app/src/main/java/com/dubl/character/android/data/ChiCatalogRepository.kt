package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.ChiCatalog

class ChiCatalogRepository(private val context: Context) {
    fun load(): ChiCatalog {
        val raw = context.assets.open("chi_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseChiCatalog(raw)
    }
}
