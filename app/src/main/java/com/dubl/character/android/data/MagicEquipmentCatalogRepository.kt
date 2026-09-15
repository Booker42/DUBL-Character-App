package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.MagicEquipmentCatalog

class MagicEquipmentCatalogRepository(private val context: Context) {
    fun load(): MagicEquipmentCatalog {
        val raw = context.assets.open("magic_equipment_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        return parseMagicEquipmentCatalog(raw)
    }
}
