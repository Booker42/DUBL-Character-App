package com.dubl.character.android.data

import com.dubl.character.android.model.CharacterSheetExtras

interface CharacterExtrasStore {
    fun load(characterId: String): CharacterSheetExtras
    fun save(characterId: String, extras: CharacterSheetExtras)
    fun delete(characterId: String) = Unit
}
