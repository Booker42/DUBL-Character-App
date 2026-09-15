package com.dubl.character.android.data

import com.dubl.character.android.model.*

fun main() {
    val skill = CharacterSkill(
        id = "computers",
        definitionId = "computers",
        name = "Домашние компьютеры",
        description = "Локальное описание",
        rank = 3,
        attributes = listOf(AttributeId.INTELLIGENCE, AttributeId.PERCEPTION),
        modifier = 2,
        formulaNote = "Локальная заметка",
        categoryOverride = SkillCategory.KNOWLEDGE,
        untrainedOverride = UntrainedRule.YES_MINUS_2,
        auto6Override = "Локальный 6",
        auto12Override = "Локальный 12",
    )
    val character = DublCharacter(id = "hero", skills = mapOf(skill.id to skill))
    val raw = SnapshotCodec.encode(AppSnapshot(listOf(character), character.id))
    val decoded = SnapshotCodec.decode(raw) { "generated" }.activeCharacter.skills.getValue("computers")

    check(decoded.name == skill.name)
    check(decoded.description == skill.description)
    check(decoded.rank == 3)
    check(decoded.attributes == skill.attributes)
    check(decoded.modifier == 2)
    check(decoded.formulaNote == skill.formulaNote)
    check(decoded.categoryOverride == SkillCategory.KNOWLEDGE) { "category override lost" }
    check(decoded.untrainedOverride == UntrainedRule.YES_MINUS_2)
    check(decoded.auto6Override == "Локальный 6") { "auto6 override lost" }
    check(decoded.auto12Override == "Локальный 12") { "auto12 override lost" }

    println("SKILL_OVERRIDE_PERSISTENCE_OK")
}
