package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.DevelopmentCatalog

class DevelopmentCatalogRepository(private val context: Context) {
    fun load(): DevelopmentCatalog {
        val regular = context.assets.open("development_regular_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val special = context.assets.open("development_special_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val roots = context.assets.open("development_ability_roots_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val martial = context.assets.open("development_martial_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val chi = context.assets.open("development_chi_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val magic = context.assets.open("development_magic_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val bootstrap = context.assets.open("development_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        return mergeDevelopmentCatalogs(regular, special, roots, martial, chi, magic, bootstrap)
    }
}
