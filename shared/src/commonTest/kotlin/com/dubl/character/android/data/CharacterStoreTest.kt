package com.dubl.character.android.data

import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.DublCharacter
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterStoreTest {
    @Test
    fun inMemoryStoreExposesLatestSavedSnapshot() {
        val first = DublCharacter(id = "first", name = "Радана")
        val store: CharacterStore = InMemoryCharacterStore(AppSnapshot(listOf(first), first.id))
        val updated = first.copy(name = "Радана Вольная")

        store.save(AppSnapshot(listOf(updated), updated.id))

        assertEquals(updated, store.load().activeCharacter)
    }
}
