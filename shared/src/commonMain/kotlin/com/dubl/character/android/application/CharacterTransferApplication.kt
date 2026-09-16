package com.dubl.character.android.application

import com.dubl.character.android.data.CharacterTransferCodec
import com.dubl.character.android.data.CharacterTransferDecodeResult
import com.dubl.character.android.data.CharacterTransferRejectReason
import com.dubl.character.android.state.CharacterExtrasSession
import com.dubl.character.android.state.CharacterSession

sealed interface CharacterTransferImportResult {
    data class Imported(
        val characterId: String,
        val name: String,
    ) : CharacterTransferImportResult

    data class Rejected(
        val reason: CharacterTransferRejectReason,
    ) : CharacterTransferImportResult
}

class CharacterTransferApplication internal constructor(
    private val characters: CharacterSession,
    private val extras: CharacterExtrasSession,
) {
    fun exportActive(): String = CharacterTransferCodec.encode(
        character = characters.active,
        extras = extras.load(characters.active.id),
    )

    fun importCharacter(raw: String): CharacterTransferImportResult {
        val decoded = CharacterTransferCodec.decode(raw, characters::newIdForTransfer)
        if (decoded is CharacterTransferDecodeResult.Failure) {
            return CharacterTransferImportResult.Rejected(decoded.reason)
        }
        decoded as CharacterTransferDecodeResult.Success

        val importedId = characters.importTransferredCharacter(decoded.payload.character)
        extras.replace(importedId, decoded.payload.extras)
        return CharacterTransferImportResult.Imported(
            characterId = importedId,
            name = characters.active.name,
        )
    }
}
