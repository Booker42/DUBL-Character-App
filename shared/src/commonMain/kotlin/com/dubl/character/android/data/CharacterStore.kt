package com.dubl.character.android.data

import com.dubl.character.android.model.AppSnapshot

/** Storage boundary shared by platform-specific persistence implementations. */
interface CharacterStore {
    fun load(): AppSnapshot
    fun save(snapshot: AppSnapshot)
}

/** Volatile store used by desktop until a native file format is deliberately introduced. */
class InMemoryCharacterStore(initial: AppSnapshot) : CharacterStore {
    private var snapshot = initial

    override fun load(): AppSnapshot = snapshot

    override fun save(snapshot: AppSnapshot) {
        this.snapshot = snapshot
    }
}
