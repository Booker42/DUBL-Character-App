package com.dubl.character.android.application

import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.SpellCatalogEntry
import com.dubl.character.android.state.CharacterSession

class MagicApplication internal constructor(
    private val session: CharacterSession,
    private val undo: ApplicationUndoManager,
) {
    fun changeMana(delta: Int) {
        val before = session.active.manaCurrent
        session.changeMana(delta)
        val applied = session.active.manaCurrent - before
        if (applied != 0) undo.record { session.changeMana(-applied) }
    }

    fun setManaRank(rank: Int) = session.setMagicManaRank(rank)
    @Deprecated("0.3.1 uses per-school magic power")
    fun setLegacyPower(power: Int) = session.setMagicPower(power)
    fun setSchoolPower(name: String, power: Int) = session.setMagicSchoolPower(name, power)
    fun addSchool(name: String, rank: Int, note: String): Boolean = session.addMagicSchool(name, rank, note)
    fun updateSchool(index: Int, name: String, rank: Int, note: String): Boolean = session.updateMagicSchool(index, name, rank, note)
    fun removeSchool(index: Int) = session.removeMagicSchool(index)
    fun addCatalogSpell(entry: SpellCatalogEntry): Boolean = session.addCatalogSpell(entry)
    fun addCustomSpell(spell: KnownSpell): String = session.addCustomSpell(spell)
    fun updateSpell(spell: KnownSpell) = session.replaceSpell(spell)
    fun setSpellLearned(uid: String, learned: Boolean) = session.setSpellLearned(uid, learned)
    fun removeSpell(uid: String) = session.removeSpell(uid)
}
