package com.dubl.character.android.state

import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.model.*

class CharacterExtrasSession(private val store: CharacterExtrasStore) {
    fun load(characterId: String): CharacterSheetExtras = store.load(characterId)

    fun update(characterId: String, transform: (CharacterSheetExtras) -> CharacterSheetExtras): CharacterSheetExtras {
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

    fun delete(characterId: String) = store.delete(characterId)
}
