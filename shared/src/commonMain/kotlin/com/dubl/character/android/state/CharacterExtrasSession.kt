package com.dubl.character.android.state

import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.model.*

internal class CharacterExtrasSession(
    private val store: CharacterExtrasStore,
    private val customConditionIdFactory: () -> String = { "" },
) {
    fun load(characterId: String): CharacterSheetExtras = store.load(characterId)

    internal fun update(characterId: String, transform: (CharacterSheetExtras) -> CharacterSheetExtras): CharacterSheetExtras {
        val next = transform(store.load(characterId))
        store.save(characterId, next)
        return next
    }

    fun setPortrait(characterId: String, uri: String?) = update(characterId) { it.copy(portraitUri = uri?.trim()?.takeIf(String::isNotBlank)) }

    fun toggleCondition(characterId: String, condition: CharacterConditionId) = update(characterId) { extras ->
        val next = extras.activeConditions.toMutableSet()
        if (!next.add(condition)) next.remove(condition)
        extras.copy(activeConditions = next)
    }

    fun setResourceHidden(characterId: String, resource: CharacterSheetResourceId, hidden: Boolean) = update(characterId) { extras ->
        extras.copy(hiddenResourceIds = if (hidden) extras.hiddenResourceIds + resource else extras.hiddenResourceIds - resource)
    }

    fun setPreferredSkillAttribute(characterId: String, skillId: String, attribute: AttributeId?) = update(characterId) { extras ->
        extras.copy(preferredSkillAttributes = if (attribute == null) extras.preferredSkillAttributes - skillId else extras.preferredSkillAttributes + (skillId to attribute))
    }

    fun setSkillGroups(characterId: String, groups: List<SheetGroup>) = update(characterId) { it.copy(skillGroups = groups) }
    fun setDevelopmentGroups(characterId: String, groups: List<SheetGroup>) = update(characterId) { it.copy(developmentGroups = groups) }

    fun setConditionOverride(
        characterId: String,
        condition: CharacterConditionId,
        title: String?,
        description: String?,
    ) = update(characterId) { extras ->
        val cleanTitle = title?.trim()?.takeIf(String::isNotBlank)
        val cleanDescription = description?.trim()
        val override = ConditionLocalOverride(
            title = cleanTitle?.takeUnless { it == condition.title },
            description = cleanDescription,
        )
        val next = if (override.title == null && override.description == null) {
            extras.conditionOverrides - condition
        } else {
            extras.conditionOverrides + (condition to override)
        }
        extras.copy(conditionOverrides = next)
    }

    fun resetConditionOverride(characterId: String, condition: CharacterConditionId) = update(characterId) { extras ->
        extras.copy(conditionOverrides = extras.conditionOverrides - condition)
    }

    fun addCustomCondition(characterId: String, title: String, description: String = ""): String? {
        val cleanTitle = title.trim().takeIf(String::isNotBlank) ?: return null
        var createdId: String? = null
        update(characterId) { extras ->
            val existing = extras.customConditions.mapTo(linkedSetOf()) { it.id }
            val requested = customConditionIdFactory().trim().takeIf { it.isNotBlank() && it !in existing }
            val id = requested ?: generateSequence(1) { it + 1 }
                .map { "custom-condition-$it" }
                .first { it !in existing }
            createdId = id
            extras.copy(
                customConditions = extras.customConditions + CustomCondition(
                    id = id,
                    title = cleanTitle,
                    description = description.trim(),
                ),
            )
        }
        return createdId
    }

    fun updateCustomCondition(characterId: String, id: String, title: String, description: String) = update(characterId) { extras ->
        val cleanTitle = title.trim().takeIf(String::isNotBlank) ?: return@update extras
        extras.copy(
            customConditions = extras.customConditions.map { condition ->
                if (condition.id == id) condition.copy(title = cleanTitle, description = description.trim()) else condition
            },
        )
    }

    fun setCustomConditionActive(characterId: String, id: String, active: Boolean) = update(characterId) { extras ->
        extras.copy(
            customConditions = extras.customConditions.map { condition ->
                if (condition.id == id) condition.copy(active = active) else condition
            },
        )
    }

    fun removeCustomCondition(characterId: String, id: String) = update(characterId) { extras ->
        extras.copy(customConditions = extras.customConditions.filterNot { it.id == id })
    }

    fun delete(characterId: String) = store.delete(characterId)
}
