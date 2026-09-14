package com.dubl.character.android.state

import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.DublCharacter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CharacterSheetSessionTest {
    private fun session(): CharacterSheetSession {
        val character = DublCharacter(
            id = "sheet-test",
            name = "Initial",
            experience = 5000,
            creationExperience = 5000,
            attributes = AttributeId.entries.associateWith { AttributeValue(base = 2) },
            hpCurrent = 1,
            enduranceCurrent = 3,
        )
        return CharacterSheetSession(
            InMemoryCharacterStore(AppSnapshot(listOf(character), character.id)),
        )
    }

    @Test
    fun identityEditMatchesAndroidCreationSemantics() {
        val session = session()

        session.editIdentity(
            name = "",
            concept = "Кузнец",
            experience = 7000,
            size = 6,
            legs = 2,
            manaEnabled = true,
        )

        assertEquals("Новый персонаж", session.active.name)
        assertEquals("Кузнец", session.active.concept)
        assertEquals(7000, session.active.experience)
        assertEquals(7000, session.active.creationExperience)
        assertEquals(6, session.active.size)
        assertEquals(session.active.healthMaximum, session.active.hpCurrent)
    }

    @Test
    fun attributeAndResourceEditsUseSharedNormalization() {
        val session = session()

        session.changeAttribute(AttributeId.CONSTITUTION, 50)
        assertEquals(10, session.active.attributes.getValue(AttributeId.CONSTITUTION).base)
        assertEquals(session.active.healthMaximum, session.active.hpCurrent)

        session.changeHp(-10000)
        session.changeEndurance(-10000)

        assertEquals(0, session.active.hpCurrent)
        assertEquals(0, session.active.enduranceCurrent)
        assertTrue(session.active.healthMaximum >= 0)
    }
}
