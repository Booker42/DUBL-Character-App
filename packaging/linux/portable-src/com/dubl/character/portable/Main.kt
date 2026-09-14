package com.dubl.character.portable

import com.dubl.character.android.data.DesktopCharacterExtrasStore
import com.dubl.character.android.data.DesktopCharacterStore
import com.dubl.character.android.model.*
import com.dubl.character.android.state.CharacterExtrasSession
import com.dubl.character.android.state.CharacterSession
import com.dubl.character.desktop.data.DesktopCatalogLoader
import java.util.UUID
import javax.swing.SwingUtilities

const val APP_VERSION = "0.2.0"

fun main(args: Array<String>) {
    val dataDir = DesktopCharacterStore.defaultDataDirectory()
    val store = DesktopCharacterStore(dataDir.resolve("characters.json")) { UUID.randomUUID().toString() }
    val extrasStore = DesktopCharacterExtrasStore(dataDir.resolve("sheet-extras.json"))
    val session = CharacterSession(store = store, idFactory = { UUID.randomUUID().toString() })
    val extras = CharacterExtrasSession(extrasStore)
    val catalogs = DesktopCatalogLoader()
    val bundle = CatalogBundle(
        development = catalogs.loadDevelopment(),
        chi = catalogs.loadChi(),
        magicEquipment = catalogs.loadMagicEquipment(),
        skillEffects = catalogs.loadSkillEffects(),
    )
    session.syncCatalogGearLoads(bundle.magicEquipment.gear)

    when {
        args.contains("--version") -> {
            println("DUBL Character $APP_VERSION")
            return
        }
        args.contains("--smoke-test") -> {
            val originalId = session.active.id
            val before = session.active

            session.changeAttribute(AttributeId.CONSTITUTION, 1)
            session.changeSkillRank("athletics", 1)
            val customResourceId = session.addCustomResource("Smoke resource", maximum = 5, current = 2)
            check(customResourceId != null)
            session.changeCustomResource(customResourceId, 1)

            val development = bundle.development.entries.first { it.isRegularDevelopment && !it.incomplete }
            session.setDevelopmentRank(development.id, 1)

            session.setChiEnabled(true)
            session.setChiBonusRanks(1)
            session.restoreChi()

            session.setMagicManaRank(1)
            session.setMagicSchoolPower("Боевая магия", 1)
            val spell = bundle.magicEquipment.spells.first { entry ->
                session.active.magic.spells.none { it.catalogId == entry.id || it.name.equals(entry.name, ignoreCase = true) }
            }
            check(session.addCatalogSpell(spell))

            val gear = bundle.magicEquipment.gear.first { entry -> session.active.gear.items.none { it.catalogId == entry.id } }
            session.addCatalogGear(gear)

            check(session.active.resolveSkill("athletics")!!.rank >= 1)
            check(session.active.constitution >= before.constitution)
            check(session.active.customResources.any { it.uid == customResourceId && it.current == 3 })
            check(session.active.development[development.id]?.rank == 1)
            check(session.active.chiActive && session.active.chiCurrent == session.active.chiMaximum)
            check(session.active.magic.spells.any { it.catalogId == spell.id })
            check(session.active.gear.items.any { it.catalogId == gear.id })

            session.createCharacter()
            check(session.snapshot.characters.size >= 2)
            session.selectCharacter(originalId)
            check(session.active.id == originalId)

            val reloaded = store.load()
            val persisted = reloaded.characters.first { it.id == originalId }
            check(persisted.customResources.any { it.uid == customResourceId })
            check(persisted.magic.spells.any { it.catalogId == spell.id })
            check(persisted.gear.items.any { it.catalogId == gear.id })

            check(bundle.development.entries.size == 796)
            check(bundle.magicEquipment.gear.size == 260)
            check(bundle.magicEquipment.spells.size == 265)
            check(bundle.chi.techniques.size == 68)
            println("DUBL_SMOKE_OK hp=${persisted.healthMaximum} skills=${persisted.skills.size} chars=${reloaded.characters.size} domains=sheet,skills,development,chi,magic,equipment,roster,persistence")
            return
        }
        args.contains("--gui-smoke") -> {
            SwingUtilities.invokeAndWait {
                installDarkDefaults()
                val window = DublWindow(session, extras, extrasStore, bundle, dataDir)
                window.isVisible = true
                window.smokeNavigateAllSections()
                window.dispose()
            }
            println("DUBL_GUI_SMOKE_OK sections=6")
            return
        }
    }

    SwingUtilities.invokeLater {
        installDarkDefaults()
        DublWindow(session, extras, extrasStore, bundle, dataDir).isVisible = true
    }
}

data class CatalogBundle(
    val development: DevelopmentCatalog,
    val chi: ChiCatalog,
    val magicEquipment: MagicEquipmentCatalog,
    val skillEffects: SkillEffectCatalog,
)
