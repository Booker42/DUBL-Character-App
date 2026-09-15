package com.dubl.character.desktop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dubl.character.android.data.DesktopCharacterExtrasStore
import com.dubl.character.android.data.DesktopCharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.effectiveDevelopmentCatalog
import com.dubl.character.android.state.CharacterExtrasSession
import com.dubl.character.android.state.CharacterSession
import com.dubl.character.desktop.data.DesktopCatalogLoader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

class DesktopAppState {
    private val characterStore = DesktopCharacterStore()
    private val extrasStore = DesktopCharacterExtrasStore()
    private val session = CharacterSession(characterStore) { UUID.randomUUID().toString() }
    val extrasSession = CharacterExtrasSession(extrasStore)
    private val catalogLoader = DesktopCatalogLoader()

    val conditionCatalog = catalogLoader.loadConditions()
    private val canonicalDevelopmentCatalog = catalogLoader.loadDevelopment()
    val developmentCatalog get() = activeCharacter.effectiveDevelopmentCatalog(canonicalDevelopmentCatalog)
    val chiCatalog = catalogLoader.loadChi()
    val magicEquipmentCatalog = catalogLoader.loadMagicEquipment()
    val skillEffectCatalog = catalogLoader.loadSkillEffects()

    var snapshot: AppSnapshot by mutableStateOf(session.snapshot)
        private set
    var extras: CharacterSheetExtras by mutableStateOf(extrasSession.load(session.active.id))
        private set

    val activeCharacter: DublCharacter get() = snapshot.activeCharacter

    init {
        // Android repairs old catalog gear weights on load. Desktop does the same once.
        session.syncCatalogGearLoads(magicEquipmentCatalog.gear)
        refresh()
    }

    fun refresh() {
        snapshot = session.snapshot
        extras = extrasSession.load(session.active.id)
    }

    fun mutate(action: CharacterSession.() -> Unit) {
        session.action()
        refresh()
    }

    fun updateExtras(action: CharacterExtrasSession.() -> Unit) {
        extrasSession.action()
        extras = extrasSession.load(session.active.id)
    }

    fun createCharacter() = mutate { createCharacter() }

    fun selectCharacter(id: String) = mutate { selectCharacter(id) }

    fun deleteActive() {
        val id = session.active.id
        extrasSession.delete(id)
        mutate { deleteActive() }
    }

    fun importPortrait(source: Path): String? = runCatching {
        val ext = source.fileName.toString().substringAfterLast('.', "png").take(8)
        val portraits = DesktopCharacterStore.defaultDataDirectory().resolve("portraits")
        Files.createDirectories(portraits)
        val target = portraits.resolve("${session.active.id}-${UUID.randomUUID()}.$ext")
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
        target.toAbsolutePath().toString()
    }.getOrNull()
}
