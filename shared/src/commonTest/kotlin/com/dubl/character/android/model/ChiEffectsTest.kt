package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class ChiEffectsTest {
    @Test
    fun internalChiActivatesResourceAndMasterProgressionRaisesMaximum() {
        val base = DublCharacter(
            id = "chi",
            attributes = defaultAttributes() + (AttributeId.WILL to AttributeValue(base = 4)),
            development = mapOf(DevelopmentEffectIds.INTERNAL_CHI to OwnedDevelopment(1)),
        )
        assertTrue(base.chiActive)
        assertEquals(5, base.chiMaximum)

        val master = base.copy(
            development = base.development + (DevelopmentEffectIds.MASTER_CHI to OwnedDevelopment(1)),
        )
        assertEquals(7, master.chiMaximum)

        val awakened = master.copy(
            development = master.development + (DevelopmentEffectIds.AWAKENED_CHI to OwnedDevelopment(1)),
        )
        assertEquals(10, awakened.chiMaximum)
    }

    @Test
    fun legacyManualChiToggleStillWorksWithoutInternalChi() {
        val off = DublCharacter(id = "legacy", chiEnabled = false)
        assertFalse(off.chiActive)
        assertEquals(0, off.chiMaximum)
        assertTrue(off.copy(chiEnabled = true).chiActive)
    }

    @Test
    fun safeSchoolPassivesAffectOnlyTheirDerivedValues() {
        val character = DublCharacter(
            id = "schools",
            size = 5,
            attributes = defaultAttributes() + mapOf(
                AttributeId.CONSTITUTION to AttributeValue(base = 4),
                AttributeId.WILL to AttributeValue(base = 4),
                AttributeId.SPEED to AttributeValue(base = 4),
                AttributeId.PERCEPTION to AttributeValue(base = 3),
            ),
            development = mapOf(
                DevelopmentEffectIds.STILL_MOUNTAIN_SCHOOL to OwnedDevelopment(1),
                DevelopmentEffectIds.STORM_LORD_SCHOOL to OwnedDevelopment(1),
            ),
        )

        assertEquals(9, character.fortitude)
        assertEquals(8, character.initiative)
        assertEquals(13.0, character.runFull, 0.001)
        assertEquals(4, character.speed)
    }
}
