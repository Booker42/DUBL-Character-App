package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.GearCatalogEntry
import com.dubl.character.android.model.MagicEquipmentCatalog
import com.dubl.character.android.model.SpellCatalogEntry
import org.json.JSONObject

class MagicEquipmentCatalogRepository(private val context: Context) {
    fun load(): MagicEquipmentCatalog {
        val raw = context.assets.open("magic_equipment_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        val spellsJson = root.optJSONArray("spells")
        val gearJson = root.optJSONArray("gear")

        val spells = buildList {
            if (spellsJson != null) {
                for (index in 0 until spellsJson.length()) {
                    val json = spellsJson.optJSONObject(index) ?: continue
                    add(
                        SpellCatalogEntry(
                            id = json.optString("id"),
                            name = json.optString("name", "Заклинание"),
                            school = json.optString("school", json.optString("category", "")),
                            cost = json.optInt("cost", 0).coerceAtLeast(0),
                            manaText = json.optString("manaText", json.optInt("cost", 0).toString()),
                            time = json.optString("time", ""),
                            range = json.optString("range", ""),
                            area = json.optString("area", ""),
                            action = json.optString("action", ""),
                            duration = json.optString("duration", ""),
                            description = json.optString("description", ""),
                            enhancement = json.optString("enhancement", ""),
                            incomplete = json.optBoolean("incomplete", false),
                            conflictNote = json.optString("conflictNote", ""),
                        )
                    )
                }
            }
        }

        val gear = buildList {
            if (gearJson != null) {
                for (index in 0 until gearJson.length()) {
                    val json = gearJson.optJSONObject(index) ?: continue
                    val fieldsJson = json.optJSONObject("fields")
                    val fields = linkedMapOf<String, String>()
                    if (fieldsJson != null) {
                        val keys = fieldsJson.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            fields[key] = fieldsJson.optString(key, "")
                        }
                    }
                    add(
                        GearCatalogEntry(
                            id = json.optString("id"),
                            name = json.optString("name", "Предмет"),
                            category = json.optString("category", "Снаряжение"),
                            section = json.optString("section", "Предметы"),
                            fields = fields,
                            description = json.optString("description", ""),
                        )
                    )
                }
            }
        }

        return MagicEquipmentCatalog(
            version = root.optString("version", "desktop"),
            spells = spells,
            gear = gear,
        )
    }
}
