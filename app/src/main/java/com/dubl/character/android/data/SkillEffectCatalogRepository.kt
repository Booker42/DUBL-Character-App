package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.SkillEffectCatalog

class SkillEffectCatalogRepository(private val context: Context) {
    fun load(): SkillEffectCatalog {
        val raw = context.assets.open("skill_effects_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseSkillEffectCatalog(raw)
    }
}
