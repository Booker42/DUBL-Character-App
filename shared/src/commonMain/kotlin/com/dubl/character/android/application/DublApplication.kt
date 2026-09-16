package com.dubl.character.android.application

import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.data.CharacterStore
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.state.CharacterExtrasSession
import com.dubl.character.android.state.CharacterSession

/**
 * The single public state-changing boundary for DUBL application behavior.
 *
 * Platform adapters may observe state and call capability methods, but they never
 * receive the raw sessions or arbitrary character/extras transforms.
 */
class DublApplication(
    characterStore: CharacterStore,
    extrasStore: CharacterExtrasStore,
    idFactory: () -> String,
    customConditionIdFactory: () -> String = { "" },
) {
    private val characterSession = CharacterSession(characterStore, idFactory)
    private val extrasSession = CharacterExtrasSession(extrasStore, customConditionIdFactory)
    private val undoManager = ApplicationUndoManager()

    val character = CharacterApplication(characterSession, extrasSession, undoManager)
    val skills = SkillsApplication(characterSession, extrasSession)
    val development = DevelopmentApplication(characterSession, undoManager)
    val magic = MagicApplication(characterSession, undoManager)
    val equipment = EquipmentApplication(characterSession)
    val sheet = SheetApplication(extrasSession, { characterSession.active.id }, undoManager)
    val transfer = CharacterTransferApplication(characterSession, extrasSession)

    val snapshot: AppSnapshot get() = characterSession.snapshot
    val active: DublCharacter get() = characterSession.active
    val activeExtras: CharacterSheetExtras get() = sheet.current
    val canUndo: Boolean get() = undoManager.canUndo

    fun undoLast(): Boolean = undoManager.undoLast()
}
