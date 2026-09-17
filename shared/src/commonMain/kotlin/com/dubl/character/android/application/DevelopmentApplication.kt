package com.dubl.character.android.application

import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DevelopmentAcquisitionPlanner
import com.dubl.character.android.model.DevelopmentAcquisitionRequest
import com.dubl.character.android.model.DevelopmentAcquisitionResult
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

    fun acquire(
        catalog: DevelopmentCatalog,
        request: DevelopmentAcquisitionRequest,
    ): DevelopmentAcquisitionResult {
        val before = session.active
        val plan = DevelopmentAcquisitionPlanner(before, catalog).plan(request)
        if (!plan.canApply) {
            val message = when {
                plan.unresolvedRequirements.isNotEmpty() -> "Есть требования, которые нельзя выполнить автоматически"
                !plan.canAfford -> "Недостаточно XP или очков способностей"
                plan.steps.isEmpty() -> "Все выбранные требования уже выполнены"
                else -> "План нельзя применить"
            }
            return DevelopmentAcquisitionResult(false, plan, message)
        }
        session.updateActive { plan.projectedCharacter }
        val after = session.active
        if (after != before) {
            undo.record { session.updateActive { before } }
            return DevelopmentAcquisitionResult(true, plan)
        }
        return DevelopmentAcquisitionResult(false, plan, "Изменения не потребовались")
    }

    fun changeChi(delta: Int) {
        val before = session.active.chiCurrent
        session.changeChi(delta)
        val applied = session.active.chiCurrent - before
        if (applied != 0) undo.record { session.changeChi(-applied) }
    }

    fun restoreChi() = session.restoreChi()
}
