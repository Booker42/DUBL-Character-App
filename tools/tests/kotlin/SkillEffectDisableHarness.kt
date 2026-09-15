package com.dubl.character.android.state

import com.dubl.character.android.data.InMemoryCharacterStore
import com.dubl.character.android.data.SnapshotCodec
import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DevelopmentCostType
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.OwnedDevelopment
import com.dubl.character.android.model.RollContext
import com.dubl.character.android.model.SkillEffectCatalog
import com.dubl.character.android.model.SkillEffectDefinition
import com.dubl.character.android.model.SkillEffectMode
import com.dubl.character.android.model.SkillEffectRules
import com.dubl.character.android.model.resolveSkill

fun main() {
    val development = DevelopmentEntry(
        id = "feat_auto",
        name = "Атлетичность",
        section = "Навыки",
        category = "Атлетика",
        cost = 10,
        costType = DevelopmentCostType.XP,
        maxRank = 5,
        requirements = "",
        benefit = "+1 за ранг",
        notes = "",
        tags = emptyList(),
        accessId = null,
        abilityOptions = emptyList(),
        incomplete = false,
        repeatable = false,
        perfectRoot = false,
        mechanicsConflict = "",
        conflictNote = "",
    )
    val developmentCatalog = DevelopmentCatalog("test", listOf(development))
    val effect = SkillEffectDefinition(
        id = "effect_auto",
        reviewIndex = 1,
        sourceName = "Атлетичность",
        effectText = "+1 за ранг",
        targetText = "SKILL · Атлетика",
        status = "test",
        plan = "test",
        mode = SkillEffectMode.AUTO_BONUS,
        rollContext = RollContext.SKILL,
        targetSkills = listOf("Атлетика"),
        value = 1,
        perRank = true,
        toggleLabel = "Атлетичность",
    )
    val effectCatalog = SkillEffectCatalog("test", listOf(effect))
    val character = DublCharacter(
        id = "c",
        development = mapOf("feat_auto" to OwnedDevelopment(rank = 2)),
    )
    val store = InMemoryCharacterStore(AppSnapshot(listOf(character), character.id))
    val session = CharacterSession(store) { "id" }
    val athletics = session.active.resolveSkill("athletics") ?: error("athletics missing")

    check(SkillEffectRules(session.active, developmentCatalog, effectCatalog).forSkill(athletics).automaticBonus == 2)
    session.setSkillEffectEnabled("effect_auto", false)
    check("effect_auto" in session.active.disabledSkillEffectIds)
    check(SkillEffectRules(session.active, developmentCatalog, effectCatalog).forSkill(athletics).automaticBonus == 0)

    val encoded = SnapshotCodec.encode(session.snapshot)
    val decoded = SnapshotCodec.decode(encoded) { "fallback" }
    check("effect_auto" in decoded.activeCharacter.disabledSkillEffectIds)

    session.setSkillEffectEnabled("effect_auto", true)
    check("effect_auto" !in session.active.disabledSkillEffectIds)
    check(SkillEffectRules(session.active, developmentCatalog, effectCatalog).forSkill(athletics).automaticBonus == 2)

    println("SKILL_EFFECT_DISABLE_OK")
}
