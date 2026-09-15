package com.dubl.character.android.model

fun main() {
    val attrs = defaultAttributes().toMutableMap().apply {
        this[AttributeId.INTELLIGENCE] = AttributeValue(base = 5)
    }

    val untrainedCharacter = DublCharacter(id = "untrained-computers", attributes = attrs)
    val untrained = untrainedCharacter.resolveSkill("computers") ?: error("computers skill missing")
    val unavailable = untrainedCharacter.skillCalculation(untrained)
    check(unavailable.total == null) { "UNSPECIFIED rank-0 skill must not execute, got ${unavailable.total}" }
    check(unavailable.unavailableReason == "Правило нетренированного использования не определено в рулбуке") {
        "Unexpected unresolved reason: ${unavailable.unavailableReason}"
    }

    val learnedState = CharacterSkill(id = "computers", definitionId = "computers", rank = 1)
    val learnedCharacter = DublCharacter(
        id = "learned-computers",
        attributes = attrs,
        skills = mapOf("computers" to learnedState),
    )
    val learned = learnedCharacter.resolveSkill("computers") ?: error("learned computers skill missing")
    check(learnedCharacter.skillCalculation(learned).total == 6) {
        "UNSPECIFIED untrained metadata must not block learned skill"
    }

    println("SKILL_UNSPECIFIED_OK")
}
