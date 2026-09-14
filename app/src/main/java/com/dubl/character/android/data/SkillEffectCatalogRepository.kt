package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.RollContext
import com.dubl.character.android.model.SkillEffectCatalog
import com.dubl.character.android.model.SkillEffectDefinition
import com.dubl.character.android.model.SkillEffectMode
import org.json.JSONArray
import org.json.JSONObject

class SkillEffectCatalogRepository(private val context: Context) {
    fun load(): SkillEffectCatalog {
        val root = JSONObject(
            context.assets.open("skill_effects_catalog.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        )
        return SkillEffectCatalog(
            version = root.optString("version", "0.5"),
            effects = root.optJSONArray("effects").toEffects(),
        )
    }

    private fun JSONArray?.toEffects(): List<SkillEffectDefinition> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                val item = optJSONObject(index) ?: continue
                add(
                    SkillEffectDefinition(
                        id = item.optString("id"),
                        reviewIndex = item.optInt("reviewIndex", index + 1),
                        sourceName = item.optString("sourceName"),
                        effectText = item.optString("effectText"),
                        targetText = item.optString("targetText"),
                        status = item.optString("status"),
                        plan = item.optString("plan"),
                        mode = item.optString("mode").toEffectMode(),
                        rollContext = item.optString("rollContext").takeIf { it.isNotBlank() }?.let { raw ->
                            RollContext.entries.firstOrNull { it.name == raw }
                        },
                        targetSkills = item.optJSONArray("targetSkills").toStrings(),
                        value = item.optInt("value", 0),
                        perRank = item.optBoolean("perRank", false),
                        toggleLabel = item.optString("toggleLabel", item.optString("sourceName")),
                    )
                )
            }
        }
    }

    private fun String.toEffectMode(): SkillEffectMode = when (this) {
        "auto_bonus" -> SkillEffectMode.AUTO_BONUS
        "toggle_bonus" -> SkillEffectMode.TOGGLE_BONUS
        "toggle_advantage" -> SkillEffectMode.TOGGLE_ADVANTAGE
        "toggle_hindrance" -> SkillEffectMode.TOGGLE_HINDRANCE
        "toggle_rule" -> SkillEffectMode.TOGGLE_RULE
        "alternative" -> SkillEffectMode.ALTERNATIVE
        "preset" -> SkillEffectMode.PRESET
        "reroll" -> SkillEffectMode.REROLL
        "formula" -> SkillEffectMode.FORMULA
        else -> SkillEffectMode.REMINDER
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
