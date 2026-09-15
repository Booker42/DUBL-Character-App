package com.dubl.character.android.application

import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.SkillCategory
import com.dubl.character.android.model.UntrainedRule
import com.dubl.character.android.state.CharacterExtrasSession
import com.dubl.character.android.state.CharacterSession

class SkillsApplication internal constructor(
    private val session: CharacterSession,
    private val extras: CharacterExtrasSession,
) {
    fun changeRank(skillId: String, delta: Int) = session.changeSkillRank(skillId, delta)
    fun setAttributes(skillId: String, attributes: List<AttributeId>) = session.setSkillAttributes(skillId, attributes)
    fun setModifier(skillId: String, modifier: Int) = session.setSkillModifier(skillId, modifier)
    fun setFormulaNote(skillId: String, note: String) = session.setSkillFormulaNote(skillId, note)
    fun setNameOverride(skillId: String, name: String) = session.setSkillNameOverride(skillId, name)
    fun setDescriptionOverride(skillId: String, description: String) = session.setSkillDescriptionOverride(skillId, description)
    fun setCategoryOverride(skillId: String, category: SkillCategory?) = session.setSkillCategoryOverride(skillId, category)
    fun setUntrainedOverride(skillId: String, rule: UntrainedRule?) = session.setSkillUntrainedOverride(skillId, rule)
    fun setAutoOverrides(skillId: String, auto6: String?, auto12: String?) = session.setSkillAutoOverrides(skillId, auto6, auto12)
    fun resetDefinitionOverrides(skillId: String) = session.resetSkillDefinitionOverrides(skillId)
    fun hide(skillId: String) = session.hideSkill(skillId)
    fun restore(skillId: String) = session.restoreSkill(skillId)
    fun restoreAll() = session.restoreAllSkills()
    fun setEffectEnabled(effectId: String, enabled: Boolean) = session.setSkillEffectEnabled(effectId, enabled)

    fun addSpecialized(templateId: String, specialization: String): String? =
        session.addSpecializedSkill(templateId, specialization)

    fun addCustom(name: String, description: String, attributes: List<AttributeId>, untrained: UntrainedRule): String? =
        session.addCustomSkill(name, description, attributes, untrained)

    fun deleteDynamic(skillId: String) = session.deleteDynamicSkill(skillId)

    fun setPreferredAttribute(skillId: String, attribute: AttributeId?) =
        extras.setPreferredSkillAttribute(session.active.id, skillId, attribute)
}
