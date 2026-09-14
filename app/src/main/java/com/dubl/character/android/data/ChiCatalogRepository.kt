package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.ChiCatalog
import com.dubl.character.android.model.ChiSchool
import com.dubl.character.android.model.ChiTechnique
import org.json.JSONArray
import org.json.JSONObject

class ChiCatalogRepository(private val context: Context) {
    fun load(): ChiCatalog {
        val root = JSONObject(
            context.assets.open("chi_catalog.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        )
        return ChiCatalog(
            version = root.optString("version", "0.5"),
            schools = root.optJSONArray("schools").toSchools(),
            techniques = root.optJSONArray("techniques").toTechniques(),
        )
    }

    private fun JSONArray?.toSchools(): List<ChiSchool> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    ChiSchool(
                        id = item.optString("id"),
                        name = item.optString("name", "Школа ЦИ"),
                        requirements = item.optString("requirements", ""),
                        passives = item.optJSONArray("passives").toStrings(),
                    )
                )
            }
        }
    }

    private fun JSONArray?.toTechniques(): List<ChiTechnique> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    ChiTechnique(
                        id = item.optString("id"),
                        name = item.optString("name", "Приём ЦИ"),
                        school = item.optString("school", "Общие приёмы"),
                        chiCost = item.optInt("chiCost", 0).coerceAtLeast(0),
                        action = item.optString("action", ""),
                        effect = item.optString("effect", ""),
                        requirements = item.optString("requirements", "Внутренняя Ци"),
                    )
                )
            }
        }
    }

    private fun JSONArray?.toStrings(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).trim().takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
