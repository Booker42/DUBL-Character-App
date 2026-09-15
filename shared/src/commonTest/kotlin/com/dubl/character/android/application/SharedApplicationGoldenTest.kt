package com.dubl.character.android.application

import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.model.UntrainedRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SharedApplicationGoldenTest {
    private class InMemoryExtrasStore : CharacterExtrasStore {
        private val values = linkedMapOf<String, CharacterSheetExtras>()

        override fun load(characterId: String): CharacterSheetExtras =
            values[characterId] ?: CharacterSheetExtras()

        override fun save(characterId: String, extras: CharacterSheetExtras) {
            values[characterId] = extras
        }

        override fun delete(characterId: String) {
            values.remove(characterId)
        }
    }

    private class DeterministicIds(private var next: Int = 1) {
        fun next(): String = "golden-${next++}"
    }

    private fun application(): DublApplication {
        val ids = DeterministicIds()
        val initial = DublCharacter(id = "character-1", name = "Новый персонаж")
        return DublApplication(
            characterStore = InMemoryCharacterStore(AppSnapshot(listOf(initial), initial.id)),
            extrasStore = InMemoryExtrasStore(),
            idFactory = ids::next,
            customConditionIdFactory = { "golden-condition" },
        )
    }

    @Test
    fun characterProfileAndResourceNormalization() {
        val app = application()

        app.character.setProfile(
            name = "  Радана  ",
            concept = "  странствующий кузнец  ",
            experience = -50,
            size = 42,
            legs = 1,
            manaEnabled = false,
        )

        assertEquals("Радана", app.active.name)
        assertEquals("странствующий кузнец", app.active.concept)
        assertEquals(0, app.active.experience)
        assertEquals(10, app.active.size)
        assertEquals(2, app.active.legs)

        val resourceId = app.character.addCustomResource("  Нитро  ", maximum = 5, current = 99)
        assertEquals("golden-1", resourceId)
        val resource = app.active.customResources.single()
        assertEquals("Нитро", resource.name)
        assertEquals(5, resource.current)
        assertEquals(5, resource.maximum)
    }

    @Test
    fun skillsAndSheetPreferenceShareOneApplicationBoundary() {
        val app = application()

        val skillId = app.skills.addCustom(
            name = "  Тестовая езда  ",
            description = "  Проверка  ",
            attributes = listOf(AttributeId.DEXTERITY, AttributeId.SPEED, AttributeId.DEXTERITY),
            untrained = UntrainedRule.NO,
        )
        assertEquals("golden-1", skillId)

        app.skills.changeRank(skillId!!, 3)
        app.skills.setPreferredAttribute(skillId, AttributeId.SPEED)

        val skill = app.active.skills.getValue(skillId)
        assertEquals(3, skill.rank)
        assertEquals(listOf(AttributeId.DEXTERITY, AttributeId.SPEED), skill.attributes)
        assertEquals(AttributeId.SPEED, app.activeExtras.preferredSkillAttributes[skillId])
    }

    @Test
    fun chiMagicAndEquipmentMutateCanonicalCharacterState() {
        val app = application()

        app.development.setChiEnabled(true)
        app.development.setChiBonusRanks(2)
        assertTrue(app.active.chiEnabled)
        assertEquals(app.active.chiMaximum, app.active.chiCurrent)

        app.magic.setManaRank(1)
        assertTrue(app.magic.addSchool("Разрушение", 2, "golden"))
        val spellId = app.magic.addCustomSpell(
            KnownSpell(uid = "spell-golden", name = "Искра", school = "Разрушение", cost = 2, manaText = "2")
        )
        assertEquals("spell-golden", spellId)
        assertTrue(app.active.magic.spells.single().custom)

        val gearId = app.equipment.addCustom(
            GearItem(uid = "gear-golden", name = "Гоночный шлем", quantity = 0, load = 1.5)
        )
        assertEquals("gear-golden", gearId)
        val gear = app.active.gear.items.single()
        assertTrue(gear.custom)
        assertTrue(gear.quantity >= 1)
    }

    @Test
    fun sheetExtrasAreTypedStateAndIsolatedPerCharacter() {
        val app = application()
        val firstId = app.active.id

        app.sheet.toggleCondition(CharacterConditionId.TIRED)
        app.sheet.setSkillGroups(listOf(SheetGroup("physical", "Физические", listOf("skill-a"))))
        val customId = app.sheet.addCustomCondition("  За рулём  ", "  Особый режим  ", active = true)

        assertEquals("golden-condition", customId)
        assertTrue(CharacterConditionId.TIRED in app.activeExtras.activeConditions)
        assertTrue(app.activeExtras.customConditions.single().active)

        app.character.createCharacter()
        val secondId = app.active.id
        assertEquals("golden-1", secondId)
        assertEquals(CharacterSheetExtras(), app.activeExtras)

        app.character.selectCharacter(firstId)
        assertTrue(CharacterConditionId.TIRED in app.activeExtras.activeConditions)
        assertEquals("physical", app.activeExtras.skillGroups.single().id)
    }

    @Test
    fun sharedUndoRevertsSemanticMutationWithoutTouchingLaterNonUndoableState() {
        val app = application()
        val beforeEndurance = app.active.enduranceCurrent

        app.character.changeEndurance(-1)
        assertTrue(app.canUndo)
        app.sheet.setSkillGroups(listOf(SheetGroup("later", "Позднее", listOf("skill-a"))))

        assertTrue(app.undoLast())
        assertEquals(beforeEndurance, app.active.enduranceCurrent)
        assertEquals("later", app.activeExtras.skillGroups.single().id)
        assertTrue(!app.canUndo)
    }
}
