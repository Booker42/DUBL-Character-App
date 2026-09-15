import com.dubl.character.android.model.*
import com.dubl.character.android.data.DesktopCharacterExtrasStore
import com.dubl.character.android.data.DesktopCharacterStore
import java.nio.file.Files

fun main() {
    val root = Files.createTempDirectory("dubl-store-test")
    val first = DublCharacter(
        id = "hero",
        name = "Радана",
        concept = "Кузнец",
        experience = 3210,
        creationExperience = 2000,
        creationComplete = true,
        attributes = defaultAttributes() + (AttributeId.STRENGTH to AttributeValue(4, 1)),
        hpCurrent = 15,
        enduranceCurrent = 2,
        manaEnabled = true,
        manaCurrent = 7,
        chiEnabled = true,
        chiCurrent = 3,
        customResources = listOf(CustomResource("rage", "Ярость", 2, 5)),
        skills = mapOf("athletics" to CharacterSkill("athletics", "athletics", rank = 3, attributes = listOf(AttributeId.STRENGTH, AttributeId.DEXTERITY), modifier = 1)),
        hiddenSkillIds = setOf("barter"),
        development = mapOf("feat" to OwnedDevelopment(2, 1)),
        magic = CharacterMagic(2, 0, listOf(MagicSchool("Боевая магия", 4)), listOf(KnownSpell("s1", name="Искра", school="Боевая магия", cost=2))),
        gear = CharacterGear(true, 0.0, listOf(GearItem("g1", name="Молот", quantity=2, load=1.5, fields=mapOf("Материал" to "Сталь")))),
    ).normalized()
    check(first.ruleset == DublRuleset.reference)
    val snapshot = AppSnapshot(listOf(first), first.id)
    val store = DesktopCharacterStore(root.resolve("characters.json")) { "fresh-id" }
    store.save(snapshot)
    val raw = Files.readString(root.resolve("characters.json"))
    check(raw.contains("\"schema\":11"))
    check(raw.contains("\"ruleset\":{\"id\":\"dubl\",\"version\":\"3.69\"}"))
    val loaded = store.load()
    check(loaded == snapshot)
    check(loaded.activeCharacter.ruleset == DublRuleset.reference)

    val legacyRaw = """{"schema":7,"activeCharacterId":"legacy","characters":[{"id":"legacy","name":"Legacy"}]}"""
    val migrated = com.dubl.character.android.data.SnapshotCodec.decode(legacyRaw) { "fallback" }
    check(migrated.activeCharacter.ruleset == DublRuleset.reference)

    val extras = CharacterSheetExtras(
        portraitUri = "/tmp/portrait.png",
        activeConditions = setOf(CharacterConditionId.TIRED, CharacterConditionId.PRONE),
        hiddenResourceIds = setOf(CharacterSheetResourceId.MANA),
        preferredSkillAttributes = mapOf("athletics" to AttributeId.DEXTERITY),
        skillGroups = listOf(SheetGroup("s", "Боевые", listOf("athletics"))),
        developmentGroups = listOf(SheetGroup("d", "Ветки", listOf("feat"))),
        notes = "Проверить ремни на повозке.\nКупить уголь.",
    )
    val extrasStore = DesktopCharacterExtrasStore(root.resolve("extras"))
    extrasStore.save(first.id, extras)
    check(extrasStore.load(first.id) == extras)
    extrasStore.delete(first.id)
    check(extrasStore.load(first.id) == CharacterSheetExtras())
    println("DESKTOP_PERSISTENCE_OK")
}
