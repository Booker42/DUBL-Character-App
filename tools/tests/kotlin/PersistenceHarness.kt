import com.dubl.character.android.data.*
import com.dubl.character.android.model.*
import java.nio.file.Files

fun main() {
    val dir = Files.createTempDirectory("dubl-persist-harness")
    val stateFile = dir.resolve("characters.json")
    var ids = 0
    val store = DesktopCharacterStore(stateFile) { "gen-${++ids}" }
    val initial = DublCharacter(
        id = "hero",
        name = "Радана",
        concept = "Кузнец",
        experience = 7000,
        skills = mapOf("custom" to CharacterSkill("custom", name = "Кузнечное дело", rank = 4, attributes = listOf(AttributeId.INTELLIGENCE))),
        hiddenSkillIds = setOf("stealth"),
        development = mapOf("feat" to OwnedDevelopment(2, 1)),
        magic = CharacterMagic(2, 0, listOf(MagicSchool("Разрушение", 4)), listOf(KnownSpell("spell", name = "Искра", school = "Разрушение", cost = 1))),
        gear = CharacterGear(items = listOf(GearItem("hammer", name = "Молот", quantity = 2, load = 1.5))),
        customResources = listOf(CustomResource("heat", "Жар", 2, 5)),
    ).normalized()
    val snapshot = AppSnapshot(listOf(initial), initial.id)
    store.save(snapshot)
    val raw = Files.readString(stateFile)
    check(raw.contains("\"schema\":7"))
    val loaded = store.load()
    check(loaded.activeCharacter.name == "Радана")
    check(loaded.activeCharacter.skills.getValue("custom").rank == 4)
    check(loaded.activeCharacter.magic.spells.single().name == "Искра")
    check(loaded.activeCharacter.gear.items.single().quantity == 2)
    check(loaded.activeCharacter.customResources.single().name == "Жар")

    val extrasFile = dir.resolve("extras.json")
    val extrasStore = DesktopCharacterExtrasStore(extrasFile)
    val extras = CharacterSheetExtras(
        portraitUri = "/tmp/portrait.png",
        activeConditions = setOf(CharacterConditionId.TIRED),
        hiddenResourceIds = setOf(CharacterSheetResourceId.MANA),
        preferredSkillAttributes = mapOf("custom" to AttributeId.INTELLIGENCE),
        skillGroups = listOf(SheetGroup("g", "Работа", listOf("custom"))),
    )
    extrasStore.save("hero", extras)
    val loadedExtras = DesktopCharacterExtrasStore(extrasFile).load("hero")
    check(loadedExtras == extras)
    println("PersistenceHarness OK")
}
