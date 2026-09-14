package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.AbilityOption
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DevelopmentCostType
import com.dubl.character.android.model.DevelopmentEntry
import org.json.JSONArray
import org.json.JSONObject

class DevelopmentCatalogRepository(private val context: Context) {
    fun load(): DevelopmentCatalog {
        val raw = context.assets.open("development_catalog.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
        val root = JSONObject(raw)
        val entries = root.getJSONArray("entries").toEntryList()
        return DevelopmentCatalog(
            version = root.optString("version", "desktop"),
            entries = entries,
        )
    }

    private fun JSONArray.toEntryList(): List<DevelopmentEntry> = buildList {
        for (index in 0 until length()) {
            val json = getJSONObject(index)
            add(
                DevelopmentEntry(
                    id = json.getString("id"),
                    name = json.optString("name", "Навык"),
                    section = json.optString("section", "Навыки"),
                    category = json.optString("category", "Общие"),
                    cost = json.optInt("cost", 0),
                    costType = if (json.optString("costType", "xp") == "ability") {
                        DevelopmentCostType.ABILITY
                    } else {
                        DevelopmentCostType.XP
                    },
                    maxRank = json.optInt("ranks", 1).coerceAtLeast(1),
                    requirements = json.optString("requirements", ""),
                    benefit = json.optString("benefit", ""),
                    notes = json.optString("notes", ""),
                    tags = json.optJSONArray("tags").toStringList(),
                    accessId = json.optString("accessId", "").takeIf { it.isNotBlank() },
                    abilityOptions = json.optJSONArray("abilityOptions").toAbilityOptions(),
                    incomplete = json.optBoolean("incomplete", false),
                    repeatable = json.optBoolean("repeatable", false),
                    perfectRoot = json.optBoolean("perfectRoot", false),
                    mechanicsConflict = json.optString("mechanicsConflict", ""),
                    conflictNote = json.optString("conflictNote", ""),
                )
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val value = optString(index, "").trim()
                if (value.isNotEmpty()) add(value)
            }
        }
    }

    private fun JSONArray?.toAbilityOptions(): List<AbilityOption> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    AbilityOption(
                        source = item.optString("source", "Источник"),
                        value = item.optInt("value", 0).coerceAtLeast(0),
                    )
                )
            }
        }
    }
}
