package com.dubl.character.android.model

enum class CharacterConditionId(val title: String) {
    HELPLESS("Беспомощный"),
    PARALYZED("Парализованный"),
    UNCONSCIOUS("Без сознания"),
    BLINDNESS("Слепота"),
    DAZED("Изумление"),
    DAZZLED("Ослепление"),
    DEAFNESS("Глухота"),
    ENTANGLED("Опутанный"),
    WEAKNESS("Слабость"),
    EXHAUSTION("Истощение"),
    TIRED("Уставший"),
    INVISIBILITY("Невидимость"),
    NAUSEA("Тошнота"),
    PRONE("Сбит с ног"),
    SHOCK("Шок"),
    FEAR("Страх"),
    PANIC("Паника"),
    SICKNESS("Болезненность"),
    STAGGERED("Ошеломление"),
    STUNNED("Оглушение"),
    POISONED("Отравление"),
    INSPIRED("Воодушевление"),
    DEMORALIZED("Деморализация"),
}



data class ConditionLocalOverride(
    val title: String? = null,
    val description: String? = null,
)

data class CustomCondition(
    val id: String,
    val title: String,
    val description: String = "",
    val active: Boolean = false,
)


enum class CharacterSheetResourceId(val title: String) {
    HEALTH("Здоровье"),
    ENDURANCE("Выносливость"),
    MANA("Мана"),
    CHI("ЦИ"),
}

data class CharacterSheetExtras(
    val portraitUri: String? = null,
    val activeConditions: Set<CharacterConditionId> = emptySet(),
    val hiddenResourceIds: Set<CharacterSheetResourceId> = emptySet(),
    val preferredSkillAttributes: Map<String, AttributeId> = emptyMap(),
    val skillGroups: List<SheetGroup> = emptyList(),
    val developmentGroups: List<SheetGroup> = emptyList(),
    val conditionOverrides: Map<CharacterConditionId, ConditionLocalOverride> = emptyMap(),
    val customConditions: List<CustomCondition> = emptyList(),
    val notes: String = "",
)


object ConditionLocalDataCodec {
    private const val RECORD_SEPARATOR = "\u001e"
    private const val FIELD_SEPARATOR = "\u001f"

    fun encodeOverrides(values: Map<CharacterConditionId, ConditionLocalOverride>): String =
        values.entries.sortedBy { it.key.name }.joinToString(RECORD_SEPARATOR) { (condition, override) ->
            listOf(
                condition.name,
                override.title.orEmpty(),
                override.description ?: NULL_TOKEN,
            ).joinToString(FIELD_SEPARATOR) { encodeToken(it) }
        }

    fun decodeOverrides(raw: String?): Map<CharacterConditionId, ConditionLocalOverride> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            if (fields.size < 3) return@mapNotNull null
            val conditionName = decodeToken(fields[0])
            val condition = CharacterConditionId.entries.firstOrNull { it.name == conditionName } ?: return@mapNotNull null
            val title = decodeToken(fields[1]).takeIf(String::isNotBlank)
            val descriptionRaw = decodeToken(fields[2])
            val description = descriptionRaw.takeUnless { it == NULL_TOKEN }
            condition to ConditionLocalOverride(title = title, description = description)
        }.toMap()
    }

    fun encodeCustom(values: List<CustomCondition>): String =
        values.joinToString(RECORD_SEPARATOR) { condition ->
            listOf(
                condition.id,
                condition.title,
                condition.description,
                if (condition.active) "1" else "0",
            ).joinToString(FIELD_SEPARATOR) { encodeToken(it) }
        }

    fun decodeCustom(raw: String?): List<CustomCondition> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(RECORD_SEPARATOR).mapNotNull { record ->
            val fields = record.split(FIELD_SEPARATOR)
            if (fields.size < 4) return@mapNotNull null
            val id = decodeToken(fields[0]).takeIf(String::isNotBlank) ?: return@mapNotNull null
            val title = decodeToken(fields[1]).takeIf(String::isNotBlank) ?: return@mapNotNull null
            CustomCondition(
                id = id,
                title = title,
                description = decodeToken(fields[2]),
                active = decodeToken(fields[3]) == "1",
            )
        }.distinctBy { it.id }
    }

    private fun encodeToken(value: String): String = buildString {
        value.encodeToByteArray().forEach { signedByte ->
            val byte = signedByte.toInt() and 0xff
            when {
                byte == 0x20 -> append('+')
                isSafe(byte) -> append(byte.toChar())
                else -> {
                    append('%')
                    append(HEX[byte ushr 4])
                    append(HEX[byte and 0x0f])
                }
            }
        }
    }

    private fun decodeToken(value: String): String {
        val bytes = mutableListOf<Byte>()
        var index = 0
        while (index < value.length) {
            when (val char = value[index]) {
                '+' -> { bytes += 0x20; index += 1 }
                '%' -> {
                    if (index + 2 >= value.length) return value
                    val high = HEX.indexOf(value[index + 1].uppercaseChar())
                    val low = HEX.indexOf(value[index + 2].uppercaseChar())
                    if (high < 0 || low < 0) return value
                    bytes += ((high shl 4) or low).toByte()
                    index += 3
                }
                else -> {
                    val encoded = char.toString().encodeToByteArray()
                    encoded.forEach { bytes += it }
                    index += 1
                }
            }
        }
        return bytes.toByteArray().decodeToString()
    }

    private fun isSafe(byte: Int): Boolean =
        byte in 'a'.code..'z'.code || byte in 'A'.code..'Z'.code || byte in '0'.code..'9'.code ||
            byte == '-'.code || byte == '_'.code || byte == '.'.code || byte == '*'.code

    private const val HEX = "0123456789ABCDEF"
    private const val NULL_TOKEN = "__DUBL_NULL__"
}
