package com.dubl.character.android.application

import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.data.CharacterTransferRejectReason
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.DevelopmentCostType
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.model.SkillCatalog
import com.dubl.character.android.model.CharacterSheetResourceId
import com.dubl.character.android.model.UntrainedRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertIs

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

    private class GoldenFixture {
        private val ids = DeterministicIds()
        private val initial = DublCharacter(id = "character-1", name = "Новый персонаж")
        val characterStore = InMemoryCharacterStore(AppSnapshot(listOf(initial), initial.id))
        val extrasStore = InMemoryExtrasStore()

        fun application(): DublApplication = DublApplication(
            characterStore = characterStore,
            extrasStore = extrasStore,
            idFactory = ids::next,
            customConditionIdFactory = { "golden-condition" },
        )
    }

    private fun application(): DublApplication = GoldenFixture().application()

    private fun developmentEntry(
        id: String = "placeholder",
        name: String = "Гоночная подготовка",
        maxRank: Int = 3,
    ): DevelopmentEntry = DevelopmentEntry(
        id = id,
        name = name,
        section = "Дополнение",
        category = "Гонки",
        cost = 75,
        costType = DevelopmentCostType.XP,
        maxRank = maxRank,
        requirements = "",
        benefit = "Тестовый эффект",
        notes = "",
        tags = listOf("Опциональный модуль"),
        accessId = null,
        abilityOptions = emptyList(),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )

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
    @Test
    fun creationEconomyAndCompletionStayCanonical() {
        val app = application()

        app.character.changeAttribute(AttributeId.CONSTITUTION, 2)
        app.character.setEconomy(
            total = 3_500,
            creation = 9_999,
            adjustment = -2_000_000,
            abilityPointsOverride = -4,
        )

        assertEquals(3_500, app.active.experience)
        assertEquals(3_500, app.active.creationExperience)
        assertEquals(-1_000_000, app.active.xpAdjustment)
        assertEquals(0, app.active.abilityPoints)

        app.character.setAbilityPointsOverride(null)
        assertEquals(3, app.active.abilityPoints)
        app.character.completeCreation()
        assertTrue(app.active.creationComplete)
        assertEquals(app.active.healthMaximum, app.active.hpCurrent)

        app.character.setExperience(1_500)
        assertEquals(1_500, app.active.creationExperience)
        assertEquals(1, app.active.abilityPoints)
    }

    @Test
    fun skillLifecycleCoversOverridesHideRestoreAndDelete() {
        val app = application()
        val builtIn = SkillCatalog.builtIns.first()

        app.skills.changeRank(builtIn.id, 20)
        app.skills.setModifier(builtIn.id, 150)
        app.skills.setNameOverride(builtIn.id, "  Локальное   имя  ")
        assertEquals(10, app.active.skills.getValue(builtIn.id).rank)
        assertEquals(99, app.active.skills.getValue(builtIn.id).modifier)
        assertEquals("Локальное имя", app.active.skills.getValue(builtIn.id).name)

        app.skills.hide(builtIn.id)
        assertTrue(builtIn.id in app.active.hiddenSkillIds)
        app.skills.restore(builtIn.id)
        assertFalse(builtIn.id in app.active.hiddenSkillIds)
        app.skills.resetDefinitionOverrides(builtIn.id)
        assertEquals("", app.active.skills.getValue(builtIn.id).name)

        val dynamicId = app.skills.addCustom(
            name = "Управление болидом",
            description = "Гоночная специализация",
            attributes = listOf(AttributeId.DEXTERITY),
            untrained = UntrainedRule.NO,
        )!!
        app.skills.deleteDynamic(dynamicId)
        assertNull(app.active.skills[dynamicId])
    }

    @Test
    fun specializedSkillLifecyclePreservesTemplateIdentity() {
        val app = application()
        val template = SkillCatalog.templates.first()

        val id = app.skills.addSpecialized(template.id, "  Гонки   ")!!
        val state = app.active.skills.getValue(id)
        assertEquals(template.id, state.definitionId)
        assertTrue(state.name.contains("Гонки"))
        assertEquals(listOf(template.defaultAttribute), state.attributes)

        app.skills.changeRank(id, 2)
        assertEquals(2, app.active.skills.getValue(id).rank)
        app.skills.deleteDynamic(id)
        assertNull(app.active.skills[id])
    }

    @Test
    fun customDevelopmentLifecyclePersistsOwnershipAndRemoval() {
        val app = application()

        val id = app.development.addCustom(developmentEntry(name = "  Гоночная   подготовка  "))!!
        assertEquals("custom-development-golden-1", id)
        val created = app.active.customDevelopmentEntries.single()
        assertEquals(id, created.id)
        assertEquals("Гоночная подготовка", created.name)

        app.development.setRank(id, 2)
        assertEquals(2, app.active.development.getValue(id).rank)

        assertTrue(app.development.updateCustom(created.copy(name = "Продвинутая подготовка", maxRank = 5)))
        assertEquals("Продвинутая подготовка", app.active.customDevelopmentEntries.single().name)

        app.development.removeCustom(id)
        assertTrue(app.active.customDevelopmentEntries.isEmpty())
        assertFalse(id in app.active.development)
    }

    @Test
    fun chiSpendRestoreAndUndoStayShared() {
        val app = application()
        app.development.setChiEnabled(true)
        app.development.setChiBonusRanks(2)
        val maximum = app.active.chiMaximum

        app.development.changeChi(-2)
        assertEquals((maximum - 2).coerceAtLeast(0), app.active.chiCurrent)
        assertTrue(app.canUndo)
        assertTrue(app.undoLast())
        assertEquals(maximum, app.active.chiCurrent)

        app.development.changeChi(-3)
        app.development.restoreChi()
        assertEquals(maximum, app.active.chiCurrent)
    }

    @Test
    fun magicLifecycleCoversSchoolSpellLearningAndManaUndo() {
        val app = application()
        app.magic.setManaRank(2)
        assertTrue(app.magic.addSchool("Разрушение", 3, "golden"))
        val maximum = app.active.effectiveManaMaximum
        assertTrue(maximum > 0)
        assertEquals(maximum, app.active.manaCurrent)

        app.magic.changeMana(-2)
        assertEquals((maximum - 2).coerceAtLeast(0), app.active.manaCurrent)
        assertTrue(app.undoLast())
        assertEquals(maximum, app.active.manaCurrent)

        val spellId = app.magic.addCustomSpell(
            KnownSpell(uid = "spell-race", name = "Форсаж", school = "Разрушение", cost = 1, manaText = "1")
        )
        app.magic.setSpellLearned(spellId, false)
        assertFalse(app.active.magic.spells.single { it.uid == spellId }.learned)
        app.magic.removeSpell(spellId)
        assertTrue(app.active.magic.spells.none { it.uid == spellId })

        assertTrue(app.magic.updateSchool(0, "Разрушение", 4, "updated"))
        assertEquals(4, app.active.magic.schools.single().rank)
        app.magic.removeSchool(0)
        assertTrue(app.active.magic.schools.isEmpty())
    }

    @Test
    fun equipmentLifecycleCoversQuantityCarriedLoadAndRemoval() {
        val app = application()
        val id = app.equipment.addCustom(
            GearItem(uid = "gear-race", name = "Гоночный комплект", quantity = 0, load = 2.5)
        )
        assertEquals("gear-race", id)
        assertEquals(1, app.active.gear.items.single().quantity)

        app.equipment.setItemQuantity(id, 4)
        app.equipment.setItemCarried(id, false)
        assertEquals(4, app.active.gear.items.single().quantity)
        assertFalse(app.active.gear.items.single().carried)

        app.equipment.setLoadAutomatic(false)
        app.equipment.setManualLoad(-10.0)
        assertFalse(app.active.gear.loadAutomatic)
        assertEquals(0.0, app.active.gear.loadManual)

        app.equipment.removeItem(id)
        assertTrue(app.active.gear.items.isEmpty())
    }

    @Test
    fun customResourceLifecycleClampsUpdatesSpendsAndRemoves() {
        val app = application()
        val id = app.character.addCustomResource(" Нитро ", maximum = 4, current = 3)!!

        app.character.changeCustomResource(id, -99)
        assertEquals(0, app.active.customResources.single().current)
        app.character.updateCustomResource(id, "  Супер   нитро ", current = 99, maximum = 2)
        assertEquals("Супер нитро", app.active.customResources.single().name)
        assertEquals(2, app.active.customResources.single().current)
        assertEquals(2, app.active.customResources.single().maximum)
        app.character.changeCustomResource(id, 99)
        assertEquals(2, app.active.customResources.single().current)

        app.character.removeCustomResource(id)
        assertTrue(app.active.customResources.isEmpty())
    }

    @Test
    fun conditionGroupingAndPreferencesSurviveApplicationRestart() {
        val fixture = GoldenFixture()
        var app = fixture.application()
        val skillId = app.skills.addCustom(
            name = "Трасса",
            description = "",
            attributes = listOf(AttributeId.PERCEPTION, AttributeId.SPEED),
            untrained = UntrainedRule.YES,
        )!!
        app.sheet.toggleCondition(CharacterConditionId.INSPIRED)
        app.sheet.setResourceHidden(CharacterSheetResourceId.MANA, true)
        app.sheet.setPreferredSkillAttribute(skillId, AttributeId.SPEED)
        app.sheet.setSkillGroups(listOf(SheetGroup("race", "Гонки", listOf(skillId))))
        app.sheet.setDevelopmentGroups(listOf(SheetGroup("dev", "Развитие", listOf("entry-a"))))

        val snapshotBeforeRestart = app.snapshot
        app = fixture.application()

        assertEquals(snapshotBeforeRestart, app.snapshot)
        assertTrue(CharacterConditionId.INSPIRED in app.activeExtras.activeConditions)
        assertTrue(CharacterSheetResourceId.MANA in app.activeExtras.hiddenResourceIds)
        assertEquals(AttributeId.SPEED, app.activeExtras.preferredSkillAttributes[skillId])
        assertEquals("race", app.activeExtras.skillGroups.single().id)
        assertEquals("dev", app.activeExtras.developmentGroups.single().id)
    }

    @Test
    fun characterRosterDeletionRemovesExtrasAndKeepsValidActiveCharacter() {
        val fixture = GoldenFixture()
        val app = fixture.application()
        val firstId = app.active.id

        app.character.createCharacter()
        val secondId = app.active.id
        app.sheet.toggleCondition(CharacterConditionId.TIRED)
        assertTrue(CharacterConditionId.TIRED in fixture.extrasStore.load(secondId).activeConditions)

        app.character.createCharacter()
        val thirdId = app.active.id
        app.character.selectCharacter(secondId)
        app.character.deleteActive()

        assertEquals(setOf(firstId, thirdId), app.snapshot.characters.map { it.id }.toSet())
        assertFalse(app.snapshot.characters.any { it.id == secondId })
        assertTrue(app.snapshot.characters.any { it.id == app.snapshot.activeCharacterId })
        assertEquals(CharacterSheetExtras(), fixture.extrasStore.load(secondId))
    }


    @Test
    fun characterTransferCreatesFreshActiveCopyAndPreservesPortableExtras() {
        val fixture = GoldenFixture()
        val app = fixture.application()
        val sourceId = app.active.id
        app.character.setName("Радана")
        app.sheet.setPortrait("content://portrait/source")
        app.sheet.toggleCondition(CharacterConditionId.TIRED)
        app.sheet.setResourceHidden(CharacterSheetResourceId.MANA, true)
        app.sheet.setSkillGroups(listOf(SheetGroup("craft", "Ремесло", listOf("smithing"))))
        app.sheet.setNotes("Дорожная кузница")

        val raw = app.transfer.exportActive()
        val firstImport = assertIs<CharacterTransferImportResult.Imported>(app.transfer.importCharacter(raw))

        assertEquals("golden-1", firstImport.characterId)
        assertEquals("Радана", firstImport.name)
        assertEquals(2, app.snapshot.characters.size)
        assertEquals(firstImport.characterId, app.snapshot.activeCharacterId)
        assertTrue(firstImport.characterId != sourceId)
        assertEquals("Радана", app.active.name)
        assertTrue(CharacterConditionId.TIRED in app.activeExtras.activeConditions)
        assertTrue(CharacterSheetResourceId.MANA in app.activeExtras.hiddenResourceIds)
        assertEquals("craft", app.activeExtras.skillGroups.single().id)
        assertEquals("Дорожная кузница", app.activeExtras.notes)
        assertNull(app.activeExtras.portraitUri)

        val secondImport = assertIs<CharacterTransferImportResult.Imported>(app.transfer.importCharacter(raw))
        assertEquals("golden-2", secondImport.characterId)
        assertEquals(3, app.snapshot.characters.size)
        assertEquals(secondImport.characterId, app.snapshot.activeCharacterId)
    }

    @Test
    fun rejectedCharacterTransferDoesNotMutateApplicationState() {
        val app = application()
        val before = app.snapshot
        val beforeExtras = app.activeExtras

        val result = assertIs<CharacterTransferImportResult.Rejected>(
            app.transfer.importCharacter("not a dubl file"),
        )

        assertEquals(CharacterTransferRejectReason.INVALID_FILE, result.reason)
        assertEquals(before, app.snapshot)
        assertEquals(beforeExtras, app.activeExtras)
    }

}
