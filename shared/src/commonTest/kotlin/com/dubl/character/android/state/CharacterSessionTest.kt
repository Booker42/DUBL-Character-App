package com.dubl.character.android.state

import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.GearCatalogEntry
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.SpellCatalogEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CharacterSessionTest {
    private fun session(): CharacterSession {
        val character = DublCharacter(
            id = "c1",
            experience = 5000,
            attributes = AttributeId.entries.associateWith { AttributeValue(base = 2) },
        )
        var next = 1
        return CharacterSession(
            InMemoryCharacterStore(AppSnapshot(listOf(character), character.id)),
            idFactory = { "generated-${next++}" },
        )
    }

    @Test
    fun skillDevelopmentMagicGearAndRosterMutationsShareOneSession() {
        val session = session()
        val skill = session.addCustomSkill(
            name = "Очень длинное пользовательское умение",
            description = "Описание",
            attributes = listOf(AttributeId.INTELLIGENCE, AttributeId.PERCEPTION),
            untrained = com.dubl.character.android.model.UntrainedRule.NO,
        )
        assertNotNull(skill)
        session.changeSkillRank(skill, 3)
        assertEquals(3, session.active.skills.getValue(skill).rank)

        session.setDevelopmentRank("dev-test", 2, 1)
        assertEquals(2, session.active.development.getValue("dev-test").rank)

        session.setMagicManaRank(2)
        session.setMagicSchoolPower("Разрушение", 5)
        assertEquals(5, session.active.magic.schools.first { it.name == "Разрушение" }.rank)

        val spellAdded = session.addCatalogSpell(
            SpellCatalogEntry("spell-1", "Искра", "Разрушение", 1, "1", "1", "10м", "-", "-", "-", "desc", "", false, ""),
        )
        assertTrue(spellAdded)
        assertEquals(1, session.active.magic.spells.size)

        val gearId = session.addCatalogGear(
            GearCatalogEntry("gear-1", "Молот", "Снаряжение", "Инструменты", mapOf("Вес" to "2"), "Кузнечный молот"),
        )
        assertEquals(gearId, session.active.gear.items.single().uid)

        session.createCharacter()
        assertEquals(2, session.snapshot.characters.size)
        assertEquals("generated-4", session.active.id)
        session.selectCharacter("c1")
        session.deleteActive()
        assertEquals(1, session.snapshot.characters.size)
    }

    @Test
    fun resourceAndCreationMutationsNormalizeThroughCharacterModel() {
        val session = session()
        session.setExperience(7000)
        session.changeAttribute(AttributeId.CONSTITUTION, 50)
        val hpBeforeLethalDamage = session.active.hpCurrent
        session.changeHp(-9999)
        session.setChiEnabled(true)
        session.setChiBonusRanks(4)
        session.restoreChi()

        assertEquals(7000, session.active.creationExperience)
        assertEquals(10, session.active.attributes.getValue(AttributeId.CONSTITUTION).base)
        assertEquals(hpBeforeLethalDamage - 9999, session.active.hpCurrent)
        assertEquals(session.active.chiMaximum, session.active.chiCurrent)
    }
}
