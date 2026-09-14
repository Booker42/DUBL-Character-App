package com.dubl.character.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.ResolvedSkill
import com.dubl.character.android.model.SkillCatalog
import com.dubl.character.android.model.SkillCategory
import com.dubl.character.android.model.UntrainedRule
import com.dubl.character.android.model.resolvedSkills
import com.dubl.character.android.model.skillCalculation
import com.dubl.character.android.model.skillNextRankCost
import com.dubl.character.android.model.skillXpSpent
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.desktop.DesktopAppState

@Composable
fun SkillsScreen(state: DesktopAppState, modifier: Modifier = Modifier) {
    val character = state.session.active
    var search by remember(character.id) { mutableStateOf("") }
    var category by remember(character.id) { mutableStateOf<SkillCategory?>(null) }
    var learnedOnly by remember(character.id) { mutableStateOf(false) }
    var selected by remember(character.id) { mutableStateOf<ResolvedSkill?>(null) }
    var rollSkill by remember(character.id) { mutableStateOf<ResolvedSkill?>(null) }
    var showHidden by remember(character.id) { mutableStateOf(false) }
    var showCustom by remember(character.id) { mutableStateOf(false) }
    var showSpecialized by remember(character.id) { mutableStateOf(false) }
    var customError by remember(character.id) { mutableStateOf<String?>(null) }

    val skills = character.resolvedSkills().filter { skill ->
        (search.isBlank() || skill.name.contains(search, ignoreCase = true) || skill.description.contains(search, ignoreCase = true)) &&
            (category == null || skill.category == category) &&
            (!learnedOnly || skill.rank > 0)
    }

    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("Умения") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(search, { search = it }, label = { Text("Поиск") }, singleLine = true, modifier = Modifier.weight(1f))
                    Button(onClick = { showSpecialized = true }) { Text("Специализация") }
                    OutlinedButton(onClick = { showCustom = true }) { Text("Своё") }
                    TextButton(onClick = { showHidden = true }) { Text("Скрытые") }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = category == null, onClick = { category = null }, label = { Text("Все") })
                }
                SkillCategory.entries.chunked(3).forEach { rowCategories ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowCategories.forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = if (category == c) null else c }, label = { Text(c.title) })
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = learnedOnly, onCheckedChange = { learnedOnly = it })
                    Text("Только изученные")
                    Text(" · Потрачено XP: ${character.skillXpSpent()}", color = DublMuted)
                }
            }
        }
        skills.groupBy { it.category }.forEach { (skillCategory, categorySkills) ->
            item(key = "skill-category-${skillCategory.name}") {
                Text("${skillCategory.title} · ${categorySkills.size}", color = DublFocus, fontWeight = FontWeight.Bold)
            }
            items(categorySkills, key = { it.id }) { skill ->
                val preferred = state.extras.preferredSkillAttributes[skill.id]?.takeIf { it in skill.attributes } ?: skill.attributes.first()
                val calc = character.skillCalculation(skill, preferred)
                SectionCard(
                    title = skill.name,
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { selected = skill }) { Text("Настроить") }
                            Button(enabled = calc.total != null, onClick = { rollSkill = skill }) { Text("Бросок") }
                        }
                    },
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("ранг ${skill.rank}", fontWeight = FontWeight.SemiBold)
                        Text(calc.total?.let(::signed) ?: "—", color = DublFocus, fontWeight = FontWeight.Bold)
                        character.skillNextRankCost(skill.rank)?.let { Text("след. +$it XP", color = DublMuted) }
                    }
                    if (skill.description.isNotBlank()) Text(skill.description, color = DublMuted)
                    Text(calc.formulaText(skill), color = DublMuted)
                }
            }
        }
    }

    selected?.let { skill -> SkillSettingsDialog(state, skill, onDismiss = { selected = null }, onRoll = { rollSkill = skill }) }
    rollSkill?.let { skill ->
        SkillRollDialog(
            character = state.session.active,
            skill = skill,
            preferredAttribute = state.extras.preferredSkillAttributes[skill.id],
            developmentCatalog = state.developmentCatalog,
            effectCatalog = state.skillEffectCatalog,
            onPreferredAttribute = { attr -> state.updateExtras { setPreferredSkillAttribute(character.id, skill.id, attr) } },
            onDismiss = { rollSkill = null },
        )
    }
    if (showHidden) HiddenSkillsDialog(state, onDismiss = { showHidden = false })
    if (showCustom) CustomSkillDialog(state, onError = { customError = it }, onDismiss = { showCustom = false })
    if (showSpecialized) SpecializedSkillDialog(state, onError = { customError = it }, onDismiss = { showSpecialized = false })

    customError?.let { message ->
        AlertDialog(
            onDismissRequest = { customError = null },
            title = { Text("Не удалось добавить умение") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { customError = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun SkillSettingsDialog(state: DesktopAppState, initial: ResolvedSkill, onDismiss: () -> Unit, onRoll: () -> Unit) {
    val skill = state.session.active.resolvedSkills(includeHidden = true).firstOrNull { it.id == initial.id } ?: initial
    var modifierText by remember(skill.id, skill.modifier) { mutableStateOf(skill.modifier.toString()) }
    var note by remember(skill.id, skill.formulaNote) { mutableStateOf(skill.formulaNote) }
    var attributes by remember(skill.id, skill.attributes) { mutableStateOf(skill.attributes.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(skill.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(skill.description.ifBlank { "Без описания" }, color = DublMuted)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ранг")
                    RankStepper(skill.rank, max = 10) { next -> state.mutate { changeSkillRank(skill.id, next - skill.rank) } }
                }
                Text("Характеристики")
                AttributeId.entries.forEach { attr ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = attr in attributes,
                            onCheckedChange = { checked ->
                                val next = if (checked) attributes + attr else attributes - attr
                                if (next.isNotEmpty()) attributes = next
                            },
                        )
                        Text(attr.title)
                    }
                }
                OutlinedTextField(modifierText, { modifierText = it.take(4) }, label = { Text("Поправка") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Примечание к формуле") })
                Text("Без обучения: ${skill.untrained.label}", color = DublMuted)
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onRoll) { Text("Бросок") }
                TextButton(onClick = {
                    state.mutate {
                        setSkillAttributes(skill.id, attributes.toList())
                        setSkillModifier(skill.id, modifierText.toIntOrNull() ?: 0)
                        setSkillFormulaNote(skill.id, note)
                    }
                    onDismiss()
                }) { Text("Сохранить") }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { state.mutate { hideSkill(skill.id) }; onDismiss() }) { Text("Скрыть") }
                if (skill.isDynamic) TextButton(onClick = { state.mutate { deleteDynamicSkill(skill.id) }; onDismiss() }) { Text("Удалить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun HiddenSkillsDialog(state: DesktopAppState, onDismiss: () -> Unit) {
    val hidden = state.session.active.resolvedSkills(includeHidden = true).filter { it.id in state.session.active.hiddenSkillIds }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Скрытые умения") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (hidden.isEmpty()) EmptyState("Скрытых умений нет.")
                hidden.forEach { skill ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(skill.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { state.mutate { restoreSkill(skill.id) } }) { Text("Вернуть") }
                    }
                }
            }
        },
        confirmButton = { if (hidden.isNotEmpty()) TextButton(onClick = { state.mutate { restoreAllSkills() }; onDismiss() }) { Text("Вернуть все") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun SpecializedSkillDialog(state: DesktopAppState, onError: (String) -> Unit, onDismiss: () -> Unit) {
    var specialization by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(SkillCatalog.templates.first()) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить специализацию") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { expanded = true }) { Text(template.name) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    SkillCatalog.templates.forEach { option -> DropdownMenuItem(text = { Text(option.name) }, onClick = { template = option; expanded = false }) }
                }
                OutlinedTextField(specialization, { specialization = it }, label = { Text("Специализация") })
            }
        },
        confirmButton = { TextButton(enabled = specialization.isNotBlank(), onClick = {
            var added: String? = null
            state.mutate { added = addSpecializedSkill(template.id, specialization) }
            if (added != null) onDismiss() else onError("Введите корректное уникальное название. Такое умение уже может существовать.")
        }) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun CustomSkillDialog(state: DesktopAppState, onError: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var attributes by remember { mutableStateOf(setOf(AttributeId.INTELLIGENCE)) }
    var untrained by remember { mutableStateOf(UntrainedRule.YES) }
    var untrainedMenu by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Своё умение") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") })
                OutlinedTextField(description, { description = it }, label = { Text("Описание") })
                AttributeId.entries.forEach { attr ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = attr in attributes, onCheckedChange = { checked ->
                            val next = if (checked) attributes + attr else attributes - attr
                            if (next.isNotEmpty()) attributes = next
                        })
                        Text(attr.title)
                    }
                }
                OutlinedButton(onClick = { untrainedMenu = true }) { Text("Без обучения: ${untrained.label}") }
                DropdownMenu(expanded = untrainedMenu, onDismissRequest = { untrainedMenu = false }) {
                    UntrainedRule.entries.forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { untrained = option; untrainedMenu = false }) }
                }
            }
        },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = {
            var added: String? = null
            state.mutate { added = addCustomSkill(name, description, attributes.toList(), untrained) }
            if (added != null) onDismiss() else onError("Введите корректное уникальное название. Такое умение уже может существовать.")
        }) { Text("Добавить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
