package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.Test

class DerivedStatsTest {
    @Test
    fun humanDerivedStatsFollowRules() {
        val character = DublCharacter(
            id = "test",
            size = 5,
            attributes = defaultAttributes() + mapOf(
                AttributeId.STRENGTH to AttributeValue(base = 2),
                AttributeId.DEXTERITY to AttributeValue(base = 3),
                AttributeId.CONSTITUTION to AttributeValue(base = 4),
                AttributeId.SPEED to AttributeValue(base = 2),
                AttributeId.PERCEPTION to AttributeValue(base = 1),
                AttributeId.WILL to AttributeValue(base = 2),
            ),
        )

        assertEquals(10, character.defense)
        assertEquals(22, character.healthMaximum)
        assertEquals(5, character.reflexes)
        assertEquals(3, character.initiative)
        assertEquals(6, character.fortitude)
        assertEquals(10.0, character.runFull, 0.0001)
    }

    @Test
    fun sizeAdjustsStrengthAndSpeed() {
        val small = DublCharacter(id = "small", size = 3)
        assertEquals(-2, small.strength)
        assertEquals(2, small.speed)

        val large = DublCharacter(id = "large", size = 7)
        assertEquals(2, large.strength)
        assertEquals(-2, large.speed)
    }

    @Test
    fun abilityPointsUseFullThousands() {
        assertEquals(0, DublCharacter(id = "a", experience = 999).abilityPoints)
        assertEquals(1, DublCharacter(id = "b", experience = 1000).abilityPoints)
        assertEquals(3, DublCharacter(id = "c", experience = 3999).abilityPoints)
    }
}
