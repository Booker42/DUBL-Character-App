package com.dubl.character.android.application

import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.CharacterSheetResourceId
import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.state.CharacterExtrasSession

class SheetApplication internal constructor(
    private val extras: CharacterExtrasSession,
    private val activeCharacterId: () -> String,
    private val undo: ApplicationUndoManager,
) {
    val current: CharacterSheetExtras get() = extras.load(activeCharacterId())

    fun setPortrait(uri: String?) = extras.setPortrait(activeCharacterId(), uri)

    fun toggleCondition(condition: CharacterConditionId) {
        val characterId = activeCharacterId()
        val before = extras.load(characterId).activeConditions
        extras.toggleCondition(characterId, condition)
        if (extras.load(characterId).activeConditions != before) {
            undo.record { extras.update(characterId) { it.copy(activeConditions = before) } }
        }
    }

    fun setConditions(conditions: Set<CharacterConditionId>) {
        val characterId = activeCharacterId()
        val before = extras.load(characterId).activeConditions
        extras.update(characterId) { it.copy(activeConditions = conditions) }
        if (extras.load(characterId).activeConditions != before) {
            undo.record { extras.update(characterId) { it.copy(activeConditions = before) } }
        }
    }

    fun setResourceHidden(resource: CharacterSheetResourceId, hidden: Boolean) =
        extras.setResourceHidden(activeCharacterId(), resource, hidden)

    fun setPreferredSkillAttribute(skillId: String, attribute: AttributeId?) =
        extras.setPreferredSkillAttribute(activeCharacterId(), skillId, attribute)

    fun setSkillGroups(groups: List<SheetGroup>) = extras.setSkillGroups(activeCharacterId(), groups)
    fun setDevelopmentGroups(groups: List<SheetGroup>) = extras.setDevelopmentGroups(activeCharacterId(), groups)
    fun setNotes(notes: String) = extras.setNotes(activeCharacterId(), notes)
    fun addNote(title: String, body: String = ""): String? = extras.addNote(activeCharacterId(), title, body)
    fun updateNote(id: String, title: String, body: String) = extras.updateNote(activeCharacterId(), id, title, body)
    fun removeNote(id: String) = extras.removeNote(activeCharacterId(), id)

    fun setConditionOverride(condition: CharacterConditionId, title: String?, description: String?) =
        extras.setConditionOverride(activeCharacterId(), condition, title, description)

    fun resetConditionOverride(condition: CharacterConditionId) = extras.resetConditionOverride(activeCharacterId(), condition)

    fun addCustomCondition(title: String, description: String = "", active: Boolean = false): String? {
        val id = extras.addCustomCondition(activeCharacterId(), title, description) ?: return null
        if (active) extras.setCustomConditionActive(activeCharacterId(), id, true)
        return id
    }

    fun updateCustomCondition(id: String, title: String, description: String, active: Boolean? = null) {
        extras.updateCustomCondition(activeCharacterId(), id, title, description)
        if (active != null) extras.setCustomConditionActive(activeCharacterId(), id, active)
    }

    fun setCustomConditionActive(id: String, active: Boolean) = extras.setCustomConditionActive(activeCharacterId(), id, active)
    fun removeCustomCondition(id: String) = extras.removeCustomCondition(activeCharacterId(), id)
}
