package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.DevelopmentCatalog

class DevelopmentCatalogRepository(context: Context) {
    private val appContext = context.applicationContext

    fun load(): DevelopmentCatalog {
        cached?.let { return it }
        return synchronized(lock) {
            cached?.let { return@synchronized it }
            loadUncached().also { cached = it }
        }
    }

    private fun loadUncached(): DevelopmentCatalog {
        val regular = appContext.assets.open("development_regular_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val special = appContext.assets.open("development_special_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val roots = appContext.assets.open("development_ability_roots_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val martial = appContext.assets.open("development_martial_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val chi = appContext.assets.open("development_chi_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val magic = appContext.assets.open("development_magic_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        val bootstrap = appContext.assets.open("development_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { parseDevelopmentCatalog(it.readText()) }
        return mergeDevelopmentCatalogs(regular, special, roots, martial, chi, magic, bootstrap)
    }

    private companion object {
        private val lock = Any()
        @Volatile
        private var cached: DevelopmentCatalog? = null
    }
}
