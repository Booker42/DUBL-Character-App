package com.dubl.character.android.application

import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.state.CharacterSession

class DevelopmentApplication internal constructor(
    private val session: CharacterSession,
    private val undo: ApplicationUndoManager,
) {
    fun setRank(entryId: String, rank: Int, optionIndex: Int = 0) = session.setDevelopmentRank(entryId, rank, optionIndex)
    fun setOverride(entry: DevelopmentEntry) = session.setDevelopmentOverride(entry)
    fun resetOverride(entryId: String) = session.resetDevelopmentOverride(entryId)
    fun addCustom(entry: DevelopmentEntry): String? = session.addCustomDevelopment(entry)
    fun updateCustom(entry: DevelopmentEntry): Boolean = session.updateCustomDevelopment(entry)
    fun removeCustom(entryId: String) = session.removeCustomDevelopment(entryId)
    fun setChiEnabled(enabled: Boolean) = session.setChiEnabled(enabled)
    fun setChiBonusRanks(rank: Int) = session.setChiBonusRanks(rank)

    fun changeChi(delta: Int) {
        val before = session.active.chiCurrent
        session.changeChi(delta)
        val applied = session.active.chiCurrent - before
        if (applied != 0) undo.record { session.changeChi(-applied) }
    }

    fun restoreChi() = session.restoreChi()
}
