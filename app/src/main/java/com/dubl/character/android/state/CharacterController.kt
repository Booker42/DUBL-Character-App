package com.dubl.character.android.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dubl.character.android.data.CharacterRepository
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.GearCatalogEntry
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.SpellCatalogEntry
import com.dubl.character.android.model.UntrainedRule
import java.util.UUID

/**
 * Android observable adapter over the platform-independent CharacterSession.
 *
 * All game mutations live in shared commonMain. This class only mirrors the latest
 * snapshot into Compose state so existing Android screens keep their public API.
 */
class CharacterController(repository: CharacterRepository) {
    private val session = CharacterSession(repository) { UUID.randomUUID().toString() }

    var snapshot: AppSnapshot by mutableStateOf(session.snapshot)
        private set

    val active: DublCharacter get() = snapshot.activeCharacter

    private inline fun <T> sync(block: () -> T): T {
        val result = block()
        snapshot = session.snapshot
        return result
    }

    fun updateActive(transform: (DublCharacter) -> DublCharacter) = sync { session.updateActive(transform) }
    fun changeAttribute(id: AttributeId, delta: Int) = sync { session.changeAttribute(id, delta) }
    fun setExperience(total: Int) = sync { session.setExperience(total) }
    fun setCreationExperience(value: Int) = sync { session.setCreationExperience(value) }
    fun setXpAdjustment(value: Int) = sync { session.setXpAdjustment(value) }
    fun setAbilityPointsOverride(value: Int?) = sync { session.setAbilityPointsOverride(value) }
    fun completeCreation() = sync { session.completeCreation() }
    fun reopenCreation() = sync { session.reopenCreation() }
    fun changeHp(delta: Int) = sync { session.changeHp(delta) }
    fun changeEndurance(delta: Int) = sync { session.changeEndurance(delta) }
    fun changeMana(delta: Int) = sync { session.changeMana(delta) }
    fun changeChi(delta: Int) = sync { session.changeChi(delta) }
    fun setChiEnabled(enabled: Boolean) = sync { session.setChiEnabled(enabled) }
    fun setChiBonusRanks(rank: Int) = sync { session.setChiBonusRanks(rank) }
    fun restoreChi() = sync { session.restoreChi() }
    fun setHealthMaximumOverride(value: Int?) = sync { session.setHealthMaximumOverride(value) }
    fun setEnduranceMaximumOverride(value: Int?) = sync { session.setEnduranceMaximumOverride(value) }
    fun setManaMaximumOverride(value: Int?) = sync { session.setManaMaximumOverride(value) }

    fun addCustomResource(name: String, maximum: Int, current: Int = maximum): String? =
        sync { session.addCustomResource(name, maximum, current) }

    fun updateCustomResource(uid: String, name: String, current: Int, maximum: Int) =
        sync { session.updateCustomResource(uid, name, current, maximum) }

    fun changeCustomResource(uid: String, delta: Int) = sync { session.changeCustomResource(uid, delta) }
    fun removeCustomResource(uid: String) = sync { session.removeCustomResource(uid) }
    fun changeSkillRank(skillId: String, delta: Int) = sync { session.changeSkillRank(skillId, delta) }
    fun setSkillAttributes(skillId: String, attributes: List<AttributeId>) = sync { session.setSkillAttributes(skillId, attributes) }
    fun setSkillModifier(skillId: String, modifier: Int) = sync { session.setSkillModifier(skillId, modifier) }
    fun setSkillFormulaNote(skillId: String, note: String) = sync { session.setSkillFormulaNote(skillId, note) }
    fun hideSkill(skillId: String) = sync { session.hideSkill(skillId) }
    fun restoreSkill(skillId: String) = sync { session.restoreSkill(skillId) }
    fun restoreAllSkills() = sync { session.restoreAllSkills() }
    fun setDevelopmentRank(entryId: String, rank: Int, optionIndex: Int = 0) =
        sync { session.setDevelopmentRank(entryId, rank, optionIndex) }

    fun setMagicManaRank(rank: Int) = sync { session.setMagicManaRank(rank) }

    @Deprecated("0.3.1 uses per-school magic power")
    fun setMagicPower(power: Int) = sync { session.setMagicPower(power) }

    fun setMagicSchoolPower(name: String, power: Int) = sync { session.setMagicSchoolPower(name, power) }
    fun addMagicSchool(name: String, rank: Int, note: String): Boolean = sync { session.addMagicSchool(name, rank, note) }
    fun updateMagicSchool(index: Int, name: String, rank: Int, note: String): Boolean =
        sync { session.updateMagicSchool(index, name, rank, note) }
    fun removeMagicSchool(index: Int) = sync { session.removeMagicSchool(index) }
    fun addCatalogSpell(entry: SpellCatalogEntry): Boolean = sync { session.addCatalogSpell(entry) }
    fun addCustomSpell(spell: KnownSpell): String = sync { session.addCustomSpell(spell) }
    fun updateSpell(uid: String, transform: (KnownSpell) -> KnownSpell) = sync { session.updateSpell(uid, transform) }
    fun removeSpell(uid: String) = sync { session.removeSpell(uid) }
    fun setGearLoadAutomatic(enabled: Boolean) = sync { session.setGearLoadAutomatic(enabled) }
    fun setGearManualLoad(value: Double) = sync { session.setGearManualLoad(value) }
    fun syncCatalogGearLoads(entries: List<GearCatalogEntry>) = sync { session.syncCatalogGearLoads(entries) }
    fun addCatalogGear(entry: GearCatalogEntry): String = sync { session.addCatalogGear(entry) }
    fun addCustomGear(item: GearItem): String = sync { session.addCustomGear(item) }
    fun updateGearItem(uid: String, transform: (GearItem) -> GearItem) = sync { session.updateGearItem(uid, transform) }
    fun removeGearItem(uid: String) = sync { session.removeGearItem(uid) }

    fun addSpecializedSkill(templateId: String, specialization: String): String? =
        sync { session.addSpecializedSkill(templateId, specialization) }

    fun addCustomSkill(
        name: String,
        description: String,
        attributes: List<AttributeId>,
        untrained: UntrainedRule,
    ): String? = sync { session.addCustomSkill(name, description, attributes, untrained) }

    fun deleteDynamicSkill(skillId: String) = sync { session.deleteDynamicSkill(skillId) }
    fun createCharacter() = sync { session.createCharacter() }
    fun selectCharacter(id: String) = sync { session.selectCharacter(id) }
    fun deleteActive() = sync { session.deleteActive() }
}
