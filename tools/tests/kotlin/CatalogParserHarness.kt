import com.dubl.character.android.data.*
import java.io.File

fun main(args: Array<String>) {
    val root = File(args.single())
    val development = mergeDevelopmentCatalogs(
        parseDevelopmentCatalog(File(root, "development_regular_catalog.json").readText()),
        parseDevelopmentCatalog(File(root, "development_special_catalog.json").readText()),
        parseDevelopmentCatalog(File(root, "development_ability_roots_catalog.json").readText()),
        parseDevelopmentCatalog(File(root, "development_martial_catalog.json").readText()),
        parseDevelopmentCatalog(File(root, "development_chi_catalog.json").readText()),
        parseDevelopmentCatalog(File(root, "development_magic_catalog.json").readText()),
        parseDevelopmentCatalog(File(root, "development_catalog.json").readText()),
    )
    val chi = parseChiCatalog(File(root, "chi_catalog.json").readText())
    val magic = parseMagicEquipmentCatalog(File(root, "magic_equipment_catalog.json").readText())
    val effects = parseSkillEffectCatalog(File(root, "skill_effects_catalog.json").readText())
    check(development.entries.size == 796) { development.entries.size }
    check(chi.schools.size == 9)
    check(chi.techniques.size == 68)
    check(magic.spells.size == 265)
    check(magic.gear.size == 260)
    check(effects.effects.size == 283)
    check(development.entries.first().name == "Плавание")
    check(magic.spells.first().name == "Агония")
    println("CatalogParserHarness OK")
}
