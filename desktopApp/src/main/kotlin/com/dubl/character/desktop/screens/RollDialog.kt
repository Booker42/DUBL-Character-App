package com.dubl.character.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.ResolvedSkill
import com.dubl.character.android.model.RollMode
import com.dubl.character.android.model.RollResult
import com.dubl.character.android.model.RollContext
import com.dubl.character.android.model.SkillEffectCatalog
import com.dubl.character.android.model.SkillEffectRules
import com.dubl.character.android.model.compareRollToTarget
import com.dubl.character.android.model.developmentNormalize
import com.dubl.character.android.model.resolveSkill
import com.dubl.character.android.model.rollCheck
import com.dubl.character.android.model.rollFollowUp
import com.dubl.character.android.model.rollPreset
import com.dubl.character.android.model.skillCalculationForRoll
import com.dubl.character.android.ui.theme.DublMuted

@Composable
fun SkillRollDialog(
    character: DublCharacter,
    skill: ResolvedSkill,
    preferredAttribute: AttributeId?,
    developmentCatalog: DevelopmentCatalog,
    effectCatalog: SkillEffectCatalog,
    onPreferredAttribute: (AttributeId) -> Unit,
    onDismiss: () -> Unit,
) {
    val attributes = skill.attributes.ifEmpty { listOf(skill.stockAttribute) }
    var attribute by remember(skill.id) { mutableStateOf(preferredAttribute?.takeIf { it in attributes } ?: attributes.first()) }
    var mode by remember(skill.id) { mutableStateOf(RollMode.NORMAL) }
    var effectCountText by remember(skill.id) { mutableStateOf("1") }
    var situationalText by remember(skill.id) { mutableStateOf("0") }
    var targetText by remember(skill.id) { mutableStateOf("") }
    var result by remember(skill.id) { mutableStateOf<RollResult?>(null) }
    var selectedEffects by remember(skill.id) { mutableStateOf(emptySet<String>()) }
    var attrMenu by remember { mutableStateOf(false) }

    val effects = SkillEffectRules(character, developmentCatalog, effectCatalog).forSkill(skill)
    val calculation = character.skillCalculationForRoll(skill, attribute)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Бросок: ${skill.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { attrMenu = true }) { Text("Характеристика: ${attribute.title}") }
                DropdownMenu(expanded = attrMenu, onDismissRequest = { attrMenu = false }) {
                    attributes.forEach { option ->
                        DropdownMenuItem(text = { Text(option.title) }, onClick = { attribute = option; attrMenu = false })
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    RollMode.entries.forEach { option ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = mode == option, onClick = { mode = option })
                            Text(option.title)
                        }
                    }
                }
                if (mode != RollMode.NORMAL) {
                    OutlinedTextField(effectCountText, { effectCountText = it.filter(Char::isDigit).take(2) }, label = { Text("Доп. костей") }, singleLine = true)
                }
                OutlinedTextField(situationalText, { situationalText = it.take(4) }, label = { Text("Ситуативная поправка") }, singleLine = true)
                OutlinedTextField(targetText, { targetText = it.filter { c -> c.isDigit() || c == '-' }.take(4) }, label = { Text("СЛ / результат противника") }, singleLine = true)

                if (effects.automaticContributions.isNotEmpty()) {
                    Text("Автоматически: " + effects.automaticContributions.joinToString { "${it.label} ${signed(it.value)}" })
                }
                effects.options.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = option.id in selectedEffects,
                            onCheckedChange = { checked -> selectedEffects = if (checked) selectedEffects + option.id else selectedEffects - option.id },
                        )
                        Column {
                            Text(option.label)
                            Text(option.description, color = DublMuted)
                        }
                    }
                }
                effects.reminders.forEach { reminder ->
                    Text("${reminder.sourceName}: ${reminder.effectText}", color = DublMuted)
                }
                Text(calculation.formulaText(skill), color = DublMuted)
                result?.let { roll ->
                    val chosen = roll.chosenIndices.sorted().joinToString { roll.dice[it].toString() }
                    Text("Кости: ${roll.dice.joinToString()} · выбрано: $chosen")
                    Text("ИТОГ: ${roll.total}")
                    targetText.toIntOrNull()?.let { target ->
                        val comparison = compareRollToTarget(roll.total, target)
                        Text("СЛ $target: ${comparison.outcome} (${signed(comparison.margin)})")
                    }
                    roll.specialResult?.let { Text(it.title) }
                    roll.note?.let { Text(it, color = DublMuted) }
                    roll.followUp?.let { followUp ->
                        OutlinedButton(onClick = { result = rollFollowUp(roll) }) { Text(followUp.buttonTitle) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = calculation.total != null,
                onClick = {
                    val base = calculation.total ?: return@TextButton
                    onPreferredAttribute(attribute)
                    val selected = effects.options.filter { it.id in selectedEffects }
                    val autoBonus = effects.automaticBonus
                    val toggledBonus = selected.sumOf { it.numericBonus }
                    val selectedAdvantage = selected.sumOf { it.advantageDice }
                    val selectedHindrance = selected.sumOf { it.hindranceDice }
                    val manual = effectCountText.toIntOrNull()?.coerceIn(1, 9) ?: 1
                    val manualAdvantage = if (mode == RollMode.ADVANTAGE) manual else 0
                    val manualHindrance = if (mode == RollMode.HINDRANCE) manual else 0
                    val adv = selectedAdvantage + manualAdvantage
                    val hind = selectedHindrance + manualHindrance
                    val effectiveMode = when {
                        adv > 0 && hind > 0 -> return@TextButton
                        adv > 0 -> RollMode.ADVANTAGE
                        hind > 0 -> RollMode.HINDRANCE
                        else -> RollMode.NORMAL
                    }
                    result = rollCheck(
                        mode = effectiveMode,
                        effectCount = when (effectiveMode) { RollMode.ADVANTAGE -> adv; RollMode.HINDRANCE -> hind; RollMode.NORMAL -> 0 },
                        checkBonus = base + autoBonus + toggledBonus,
                        checkBonusLabel = "${skill.name} + ${attribute.title}",
                        situationalBonus = situationalText.toIntOrNull()?.coerceIn(-99, 99) ?: 0,
                    )
                },
            ) { Text(if (result == null) "Бросить" else "Бросить снова") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
fun ContextRollDialog(
    character: DublCharacter,
    context: RollContext,
    developmentCatalog: DevelopmentCatalog,
    effectCatalog: SkillEffectCatalog,
    initialAttribute: AttributeId? = null,
    onDismiss: () -> Unit,
) {
    val allowedSkillIds = when (context) {
        RollContext.ATTACK, RollContext.BREAK_ITEM -> listOf("unarmed", "melee_weapon", "shooting", "throwing")
        RollContext.PARRY, RollContext.DISARM -> listOf("unarmed", "melee_weapon")
        RollContext.FEINT -> listOf("eloquence", "unarmed", "melee_weapon")
        else -> emptyList()
    }
    val skills = allowedSkillIds.mapNotNull(character::resolveSkill)
    var skill by remember(context) { mutableStateOf(skills.firstOrNull()) }
    var attribute by remember(context, skill?.id, initialAttribute) { mutableStateOf<AttributeId?>(initialAttribute) }
    var advantageText by remember(context) { mutableStateOf("0") }
    var hindranceText by remember(context) { mutableStateOf("0") }
    var situationalText by remember(context) { mutableStateOf("0") }
    var targetText by remember(context) { mutableStateOf("") }
    var result by remember(context) { mutableStateOf<RollResult?>(null) }
    var skillMenu by remember { mutableStateOf(false) }
    var attributeMenu by remember { mutableStateOf(false) }

    val attrOptions = when (context) {
        RollContext.ATTRIBUTE -> AttributeId.entries
        RollContext.ATTACK, RollContext.BREAK_ITEM -> when (skill?.id) {
            "shooting" -> listOf(AttributeId.PERCEPTION, AttributeId.DEXTERITY)
            "throwing", "unarmed", "melee_weapon" -> listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH)
            else -> emptyList()
        }
        RollContext.PARRY, RollContext.DISARM -> listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH)
        RollContext.FEINT -> listOf(AttributeId.CHARISMA)
        else -> emptyList()
    }
    val selectedAttribute = initialAttribute?.takeIf { it in attrOptions } ?: attribute?.takeIf { it in attrOptions } ?: attrOptions.firstOrNull()
    val preset = character.rollPreset(context, skill?.id, selectedAttribute)
    val alreadyAppliedLabels = preset.contributions.map { developmentNormalize(it.label) }.toSet()
    val reminders = SkillEffectRules(character, developmentCatalog, effectCatalog)
        .forContext(context)
        .filterNot { developmentNormalize(it.sourceName) in alreadyAppliedLabels }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(context.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (skills.isNotEmpty()) {
                    OutlinedButton(onClick = { skillMenu = true }) { Text("Умение: ${skill?.name ?: "—"}") }
                    DropdownMenu(expanded = skillMenu, onDismissRequest = { skillMenu = false }) {
                        skills.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { skill = option; attribute = null; skillMenu = false }) }
                    }
                }
                if (attrOptions.isNotEmpty()) {
                    OutlinedButton(onClick = { attributeMenu = true }) { Text("Характеристика: ${(selectedAttribute ?: attrOptions.first()).title}") }
                    DropdownMenu(expanded = attributeMenu, onDismissRequest = { attributeMenu = false }) {
                        attrOptions.forEach { option -> DropdownMenuItem(text = { Text(option.title) }, onClick = { attribute = option; attributeMenu = false }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(advantageText, { advantageText = it.filter(Char::isDigit).take(1) }, label = { Text("Преим.") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(hindranceText, { hindranceText = it.filter(Char::isDigit).take(1) }, label = { Text("Помех.") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(situationalText, { situationalText = it.take(4) }, label = { Text("Поправка") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(targetText, { targetText = it.take(4) }, label = { Text("Цель / СЛ") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                preset.contributions.forEach { contribution -> Text("${contribution.label}: ${signed(contribution.value)}", color = DublMuted) }
                reminders.forEach { reminder -> Text("${reminder.sourceName}: ${reminder.effectText}", color = DublMuted) }
                Text(preset.formulaText, color = DublMuted)
                result?.let { roll ->
                    Text("Кости: ${roll.dice.joinToString()} · итог ${roll.total}")
                    targetText.toIntOrNull()?.let { target ->
                        val comparison = compareRollToTarget(roll.total, target)
                        Text("${comparison.outcome} (${signed(comparison.margin)})")
                    }
                    roll.specialResult?.let { Text(it.title) }
                    roll.note?.let { Text(it, color = DublMuted) }
                    roll.followUp?.let { follow -> OutlinedButton(onClick = { result = rollFollowUp(roll) }) { Text(follow.buttonTitle) } }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = preset.available, onClick = {
                val bonus = preset.bonus ?: return@TextButton
                val adv = advantageText.toIntOrNull()?.coerceIn(0, 9) ?: 0
                val hind = hindranceText.toIntOrNull()?.coerceIn(0, 9) ?: 0
                if (adv > 0 && hind > 0) return@TextButton
                val mode = when { adv > 0 -> RollMode.ADVANTAGE; hind > 0 -> RollMode.HINDRANCE; else -> RollMode.NORMAL }
                result = rollCheck(
                    mode = mode,
                    effectCount = if (mode == RollMode.ADVANTAGE) adv else if (mode == RollMode.HINDRANCE) hind else 0,
                    checkBonus = bonus,
                    checkBonusLabel = preset.title,
                    situationalBonus = situationalText.toIntOrNull()?.coerceIn(-99, 99) ?: 0,
                )
            }) { Text(if (result == null) "Бросить" else "Бросить снова") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}
