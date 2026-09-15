package com.dubl.character.android.data

import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.ConditionLocalOverride
import com.dubl.character.android.model.CustomCondition
import java.nio.file.Files

fun main() {
    val dir = Files.createTempDirectory("dubl-condition-overrides")
    val file = dir.resolve("sheet-extras.json")
    val store = DesktopCharacterExtrasStore(file)
    val expected = CharacterSheetExtras(
        activeConditions = setOf(CharacterConditionId.TIRED),
        conditionOverrides = mapOf(
            CharacterConditionId.TIRED to ConditionLocalOverride(
                title = "Утомлён",
                description = "Локальная трактовка",
            ),
        ),
        customConditions = listOf(
            CustomCondition("custom-1", "Горит синим", "Домашнее состояние", active = true),
        ),
    )
    store.save("hero", expected)

    val reloaded = DesktopCharacterExtrasStore(file).load("hero")
    check(reloaded.activeConditions == expected.activeConditions)
    check(reloaded.conditionOverrides == expected.conditionOverrides)
    check(reloaded.customConditions == expected.customConditions)
    println("CONDITION_OVERRIDE_PERSISTENCE_OK")
}
