package com.dubl.character.android.model

data class ConditionDefinition(
    val id: String,
    val name: String,
    val description: String,
)

data class ConditionCatalog(
    val conditions: List<ConditionDefinition>,
) {
    fun summary(condition: CharacterConditionId): String =
        conditions.firstOrNull { it.name == condition.title }?.description
            ?: "Описание состояния отсутствует в каноническом каталоге DUBL 3.69."
}
