package com.dubl.character.android.model

data class RulesetRef(
    val id: String,
    val version: String,
)

object DublRuleset {
    const val ID = "dubl"
    const val VERSION = "3.69"
    val reference = RulesetRef(ID, VERSION)
}
