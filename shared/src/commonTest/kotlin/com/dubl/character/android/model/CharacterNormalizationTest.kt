package com.dubl.character.android.model

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class CharacterNormalizationTest {
    @Test
    fun normalizationClampsNewSystemsWithoutDroppingCharacterData() {
        val character = DublCharacter(
            id = "normalize",
            hpCurrent = 999,
            manaCurrent = 999,
            magic = CharacterMagic(
                manaRank = 9,
                power = -3,
                schools = listOf(MagicSchool(name = " ", rank = -2, note = " note ")),
                spells = listOf(
                    KnownSpell(uid = "spell", name = " ", cost = -5, xpOverride = -10),
                    KnownSpell(uid = "spell", name = "duplicate"),
                ),
            ),
            gear = CharacterGear(
                loadAutomatic = false,
                loadManual = -4.0,
                items = listOf(
                    GearItem(uid = "gear", name = " ", quantity = -3, load = -2.0),
                    GearItem(uid = "gear", name = "duplicate"),
                ),
            ),
        )

        val normalized = character.normalized()

        assertEquals(5, normalized.magic.manaRank)
        assertEquals(0, normalized.magic.power)
        assertTrue(normalized.magic.schools.isEmpty())
        assertEquals(1, normalized.magic.spells.size)
        assertEquals("Заклинание", normalized.magic.spells.single().name)
        assertEquals(0, normalized.magic.spells.single().cost)
        assertEquals(0, normalized.magic.spells.single().xpOverride)
        assertEquals(1, normalized.gear.items.size)
        assertEquals("Предмет", normalized.gear.items.single().name)
        assertEquals(1, normalized.gear.items.single().quantity)
        assertEquals(0.0, normalized.gear.items.single().load, 0.001)
        assertEquals(0.0, normalized.gear.loadManual, 0.001)
        assertTrue(normalized.manaEnabled)
        assertTrue(normalized.manaCurrent <= normalized.effectiveManaMaximum)
    }

    @Test
    fun legacyCharacterDefaultsRemainSafe() {
        val character = DublCharacter(id = "legacy").normalized()
        assertTrue(character.magic.schools.isEmpty())
        assertTrue(character.magic.spells.isEmpty())
        assertTrue(character.gear.items.isEmpty())
        assertFalse(character.manaEnabled)
        assertEquals(0, character.manaCurrent)
    }
    @Test
    fun normalizationKeepsPricedAttributesInsideRulebookRange() {
        val character = DublCharacter(
            id = "attributes",
            attributes = defaultAttributes() + mapOf(
                AttributeId.STRENGTH to AttributeValue(base = 99),
                AttributeId.DEXTERITY to AttributeValue(base = -99),
            ),
        ).normalized()

        assertEquals(10, character.attributes.getValue(AttributeId.STRENGTH).base)
        assertEquals(-5, character.attributes.getValue(AttributeId.DEXTERITY).base)
    }

    @Test
    fun legacyBaseManaDevelopmentMigratesIntoMagicWithoutDoubleCounting() {
        val character = DublCharacter(
            id = "legacy-mana",
            development = mapOf(
                MagicEquipmentRules.BASE_MANA_ENTRY_ID to OwnedDevelopment(rank = 3),
            ),
            magic = CharacterMagic(manaRank = 0, power = 4),
        ).normalized()

        assertEquals(3, character.magic.manaRank)
        assertFalse(character.development.containsKey(MagicEquipmentRules.BASE_MANA_ENTRY_ID))
        assertTrue(character.manaEnabled)
    }

    @Test
    fun manualMaximumOverridesAndCustomResourcesAreNormalized() {
        val character = DublCharacter(
            id = "resources",
            healthMaximumOverride = 42,
            enduranceMaximumOverride = 7,
            manaMaximumOverride = 33,
            hpCurrent = 99,
            enduranceCurrent = 99,
            manaCurrent = 99,
            customResources = listOf(
                CustomResource(uid = "chi", name = "  Ци  ", current = 20, maximum = 12),
                CustomResource(uid = "chi", name = "duplicate", current = 1, maximum = 1),
            ),
        ).normalized()

        assertEquals(42, character.healthMaximum)
        assertEquals(7, character.enduranceMaximum)
        assertEquals(33, character.effectiveManaMaximum)
        assertEquals(42, character.hpCurrent)
        assertEquals(7, character.enduranceCurrent)
        assertEquals(33, character.manaCurrent)
        assertEquals(1, character.customResources.size)
        assertEquals("Ци", character.customResources.single().name)
        assertEquals(12, character.customResources.single().current)
    }

    @Test
    fun chiIsDisabledByDefaultAndHasNoPool() {
        val character = DublCharacter(id = "chi-off").normalized()

        assertFalse(character.chiEnabled)
        assertEquals(0, character.chiCurrent)
        assertEquals(0, character.chiMaximum)
    }

    @Test
    fun chiBasePoolUsesWillPlusOneWithMinimumThree() {
        val lowWill = DublCharacter(
            id = "chi-low-will",
            chiEnabled = true,
            attributes = defaultAttributes() + (AttributeId.WILL to AttributeValue(base = 1)),
        ).normalized()
        val trainedWill = DublCharacter(
            id = "chi-trained-will",
            chiEnabled = true,
            attributes = defaultAttributes() + (AttributeId.WILL to AttributeValue(base = 4)),
        ).normalized()

        assertEquals(3, lowWill.chiMaximum)
        assertEquals(5, trainedWill.chiMaximum)
    }

    @Test
    fun chiBonusRanksAddToPoolAndClampCurrentAndRanks() {
        val character = DublCharacter(
            id = "chi-ranked",
            chiEnabled = true,
            chiCurrent = 99,
            chiBonusRanks = 99,
            attributes = defaultAttributes() + (AttributeId.WILL to AttributeValue(base = 4)),
        ).normalized()

        assertEquals(10, character.chiBonusRanks)
        assertEquals(15, character.chiMaximum)
        assertEquals(15, character.chiCurrent)
    }

}
