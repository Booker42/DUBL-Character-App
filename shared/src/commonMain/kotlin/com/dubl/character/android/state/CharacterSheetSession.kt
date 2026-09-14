package com.dubl.character.android.state

import com.dubl.character.android.data.CharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DublCharacter

/**
 * Platform-independent editing session for the character-sheet workflow.
 *
 * The mutation semantics intentionally mirror the existing Android CharacterController
 * for fields exposed by the main character sheet. Domain normalization remains owned by
 * DublCharacter.normalized(), so desktop does not duplicate clamping or derived formulas.
 */
class CharacterSheetSession(private val store: CharacterStore) {
    var snapshot: AppSnapshot = store.load()
        private set

    val active: DublCharacter
        get() = snapshot.activeCharacter

    fun updateActive(transform: (DublCharacter) -> DublCharacter) {
        val activeId = snapshot.activeCharacterId
        val updatedCharacters = snapshot.characters.map { character ->
            if (character.id == activeId) transform(character).normalized() else character
        }
        persist(snapshot.copy(characters = updatedCharacters))
    }

    fun editIdentity(
        name: String,
        concept: String,
        experience: Int,
        size: Int,
        legs: Int,
        manaEnabled: Boolean,
    ) = updateActive { current ->
        val cleanExperience = experience.coerceAtLeast(0)
        var updated = current.copy(
            name = name.ifBlank { "Новый персонаж" },
            concept = concept,
            experience = cleanExperience,
            creationExperience = if (current.creationComplete) {
                current.creationExperience.coerceAtMost(cleanExperience)
            } else {
                cleanExperience
            },
            size = size,
            legs = legs,
            manaEnabled = manaEnabled,
        )
        if (!current.creationComplete) {
            updated = updated.copy(hpCurrent = updated.healthMaximum)
        }
        updated
    }

    fun setName(value: String) = updateActive { character ->
        character.copy(name = value.ifBlank { "Новый персонаж" })
    }

    fun setExperience(total: Int) = updateActive { character ->
        val clean = total.coerceAtLeast(0)
        character.copy(
            experience = clean,
            creationExperience = if (character.creationComplete) {
                character.creationExperience.coerceAtMost(clean)
            } else {
                clean
            },
        )
    }

    fun changeAttribute(id: AttributeId, delta: Int) = updateActive { character ->
        val current = character.attributes.getValue(id)
        val nextBase = (current.base + delta).coerceIn(-5, 10)
        val updated = character.copy(
            attributes = character.attributes + (id to current.copy(base = nextBase)),
        )
        if (character.creationComplete) updated else updated.copy(hpCurrent = updated.healthMaximum)
    }

    fun changeHp(delta: Int) = updateActive { character ->
        character.copy(hpCurrent = character.hpCurrent + delta)
    }

    fun changeEndurance(delta: Int) = updateActive { character ->
        character.copy(enduranceCurrent = character.enduranceCurrent + delta)
    }

    fun changeMana(delta: Int) = updateActive { character ->
        character.copy(manaCurrent = character.manaCurrent + delta)
    }

    fun setHealthMaximumOverride(value: Int?) = updateActive { character ->
        character.copy(healthMaximumOverride = value?.coerceAtLeast(0))
    }

    fun setEnduranceMaximumOverride(value: Int?) = updateActive { character ->
        character.copy(enduranceMaximumOverride = value?.coerceAtLeast(0))
    }

    fun setManaMaximumOverride(value: Int?) = updateActive { character ->
        character.copy(
            manaMaximumOverride = value?.coerceAtLeast(0),
            manaEnabled = character.manaEnabled || value != null || character.magic.manaRank > 0,
        )
    }

    private fun persist(updated: AppSnapshot) {
        snapshot = updated
        store.save(updated)
    }
}
