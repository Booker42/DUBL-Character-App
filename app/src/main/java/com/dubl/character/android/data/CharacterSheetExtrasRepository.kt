package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.ConditionLocalDataCodec
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.CharacterSheetResourceId
import com.dubl.character.android.model.SheetGroupingRules

class CharacterSheetExtrasRepository(context: Context) : CharacterExtrasStore {
    private val prefs = context.applicationContext.getSharedPreferences(
        "dubl_character_sheet_extras",
        Context.MODE_PRIVATE,
    )

    override fun load(characterId: String): CharacterSheetExtras {
        val prefix = prefix(characterId)
        val portraitUri = prefs.getString("${prefix}portrait", null)
        val conditions = prefs.getStringSet("${prefix}conditions", emptySet()).orEmpty()
            .mapNotNull { raw -> CharacterConditionId.entries.firstOrNull { it.name == raw } }
            .toSet()
        val hiddenResourceIds = prefs.getStringSet("${prefix}hidden_resources", emptySet()).orEmpty()
            .mapNotNull { raw -> CharacterSheetResourceId.entries.firstOrNull { it.name == raw } }
            .toSet()
        val skillGroups = SheetGroupingRules.decode(prefs.getString("${prefix}skill_groups", null))
        val developmentGroups = SheetGroupingRules.decode(prefs.getString("${prefix}development_groups", null))
        val conditionOverrides = ConditionLocalDataCodec.decodeOverrides(prefs.getString("${prefix}condition_overrides", null))
        val customConditions = ConditionLocalDataCodec.decodeCustom(prefs.getString("${prefix}custom_conditions", null))
        val notes = prefs.getString("${prefix}notes", "").orEmpty()
        val preferredSkillAttributes = prefs
            .getStringSet("${prefix}preferred_skill_attributes", emptySet())
            .orEmpty()
            .mapNotNull { raw ->
                val separatorIndex = raw.lastIndexOf(PREFERRED_ATTRIBUTE_SEPARATOR)
                if (separatorIndex <= 0) return@mapNotNull null
                val skillId = raw.substring(0, separatorIndex)
                val attributeName = raw.substring(separatorIndex + PREFERRED_ATTRIBUTE_SEPARATOR.length)
                val attribute = AttributeId.entries.firstOrNull { it.name == attributeName } ?: return@mapNotNull null
                skillId to attribute
            }
            .toMap()

        return CharacterSheetExtras(
            portraitUri = portraitUri,
            activeConditions = conditions,
            hiddenResourceIds = hiddenResourceIds,
            preferredSkillAttributes = preferredSkillAttributes,
            skillGroups = skillGroups,
            developmentGroups = developmentGroups,
            conditionOverrides = conditionOverrides,
            customConditions = customConditions,
            notes = notes,
        )
    }

    override fun save(characterId: String, extras: CharacterSheetExtras) {
        val prefix = prefix(characterId)
        prefs.edit()
            .putString("${prefix}portrait", extras.portraitUri)
            .putStringSet("${prefix}conditions", extras.activeConditions.map { it.name }.toSet())
            .putStringSet("${prefix}hidden_resources", extras.hiddenResourceIds.map { it.name }.toSet())
            .putString("${prefix}skill_groups", SheetGroupingRules.encode(extras.skillGroups))
            .putString("${prefix}development_groups", SheetGroupingRules.encode(extras.developmentGroups))
            .putString("${prefix}condition_overrides", ConditionLocalDataCodec.encodeOverrides(extras.conditionOverrides))
            .putString("${prefix}custom_conditions", ConditionLocalDataCodec.encodeCustom(extras.customConditions))
            .putString("${prefix}notes", extras.notes)
            .putStringSet(
                "${prefix}preferred_skill_attributes",
                extras.preferredSkillAttributes.map { (skillId, attribute) ->
                    "$skillId$PREFERRED_ATTRIBUTE_SEPARATOR${attribute.name}"
                }.toSet(),
            )
            .remove("${prefix}favorite_skill_ids") // 0.6: all learned skills are shown; favorites are retired.
            .remove("${prefix}favorites") // v4 pre-skill favorites were stats/attributes and are intentionally discarded.
            .apply()
    }

    override fun delete(characterId: String) {
        val prefix = prefix(characterId)
        prefs.edit()
            .remove("${prefix}portrait")
            .remove("${prefix}conditions")
            .remove("${prefix}hidden_resources")
            .remove("${prefix}skill_groups")
            .remove("${prefix}development_groups")
            .remove("${prefix}condition_overrides")
            .remove("${prefix}custom_conditions")
            .remove("${prefix}notes")
            .remove("${prefix}preferred_skill_attributes")
            .remove("${prefix}favorite_skill_ids")
            .remove("${prefix}favorites")
            .apply()
    }

    private fun prefix(characterId: String): String = "character.$characterId."

    private companion object {
        const val PREFERRED_ATTRIBUTE_SEPARATOR = "::"
    }
}
