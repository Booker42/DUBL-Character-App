package com.dubl.character.android.data

import android.content.Context
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.DublCharacter
import java.util.UUID

class CharacterRepository(context: Context) : CharacterStore {
    private val preferences = context.getSharedPreferences("dubl_native_state", Context.MODE_PRIVATE)

    override fun load(): AppSnapshot {
        val raw = preferences.getString(KEY_STATE, null) ?: return SnapshotCodec.fresh(::newId)
        return runCatching { SnapshotCodec.decode(raw, ::newId) }
            .getOrElse { SnapshotCodec.fresh(::newId) }
    }

    override fun save(snapshot: AppSnapshot) {
        preferences.edit().putString(KEY_STATE, SnapshotCodec.encode(snapshot)).apply()
    }

    fun newCharacter(): DublCharacter = DublCharacter(id = newId(), name = "Новый персонаж")

    private fun newId(): String = UUID.randomUUID().toString()

    private companion object {
        const val KEY_STATE = "state_v1"
    }
}
