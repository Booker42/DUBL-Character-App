package com.dubl.character.desktop.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.dubl.character.android.model.CharacterEconomy
import com.dubl.character.android.model.ChiRules
import com.dubl.character.android.model.DevelopmentEffectIds
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.DevelopmentProgress
import com.dubl.character.android.model.DevelopmentRules
import com.dubl.character.android.model.OwnedDevelopment
import com.dubl.character.android.model.MagicEquipmentRules
import com.dubl.character.android.model.developmentNormalize
import com.dubl.character.android.model.developmentRank
import com.dubl.character.android.model.RequirementStatus
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.desktop.DesktopAppState

private enum class DevelopmentTab(val title: String) {
    REGULAR("Обычные"), SPECIAL("Спец. ветки"), MARTIAL("Боевые искусства"), CHI("ЦИ"), OWNED("Взято")
}

@Composable
fun DevelopmentScreen(state: DesktopAppState, modifier: Modifier = Modifier) {
    val character = state.activeCharacter
    var tab by remember(character.id) { mutableStateOf(DevelopmentTab.REGULAR) }
    var search by remember(character.id) { mutableStateOf("") }
    var availableOnly by remember(character.id) { mutableStateOf(false) }
    var selected by remember(character.id) { mutableStateOf<DevelopmentEntry?>(null) }
    val progress = DevelopmentProgress(character.development)
    val rules = DevelopmentRules(character, state.developmentCatalog, progress)
    val chiRules = ChiRules(character, state.developmentCatalog)
    val economy = CharacterEconomy.breakdown(character, state.developmentCatalog)

    fun branchName(entry: DevelopmentEntry): String = when {
        entry.isAbility -> entry.name
        entry.accessId != null -> state.developmentCatalog.byId(entry.accessId)?.name ?: entry.category.ifBlank { entry.name }
        else -> entry.category.ifBlank { entry.name }
    }

    val needle = developmentNormalize(search)
    val source = state.developmentCatalog.entries
        .asSequence()
        .filter { entry -> !entry.incomplete || (tab == DevelopmentTab.OWNED && progress.rank(entry.id) > 0) }
        .filterNot { it.id == MagicEquipmentRules.BASE_MANA_ENTRY_ID }
        .filter { entry ->
            when (tab) {
                DevelopmentTab.REGULAR -> entry.isRegularDevelopment
                DevelopmentTab.SPECIAL -> entry.isSpecialDevelopment
                DevelopmentTab.MARTIAL -> entry.isMartialArt
                DevelopmentTab.CHI -> entry.isChiDevelopment
                DevelopmentTab.OWNED -> progress.rank(entry.id) > 0
            }
        }
        .filter { entry ->
            needle.isBlank() || developmentNormalize(
                listOf(
                    entry.name, branchName(entry), entry.category, entry.section, entry.requirements,
                    entry.benefit, entry.notes, entry.tags.joinToString(" "),
                ).joinToString(" "),
            ).contains(needle)
        }
        .filter { entry -> tab == DevelopmentTab.OWNED || !availableOnly || rules.availability(entry).canIncrease }
        .sortedWith(
            compareBy<DevelopmentEntry>(
                { if (tab == DevelopmentTab.SPECIAL || (tab == DevelopmentTab.OWNED && it.isSpecialDevelopment)) developmentNormalize(branchName(it)) else developmentNormalize(it.category) },
                { if (it.isAbility) 0 else 1 },
                { developmentNormalize(it.name) },
            ),
        )
        .toList()

    val filteredChiTechniques = state.chiCatalog.techniques
        .filter { technique ->
            val matches = needle.isBlank() || developmentNormalize(
                listOf(technique.name, technique.school, technique.action, technique.effect, technique.requirements).joinToString(" "),
            ).contains(needle)
            matches && (!availableOnly || chiRules.availability(technique).unlocked)
        }
        .sortedWith(compareBy({ developmentNormalize(it.school) }, { developmentNormalize(it.name) }))

    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("Навыки и развитие") {
                DevelopmentTab.entries.chunked(3).forEach { tabRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tabRow.forEach { item ->
                            FilterChip(
                                selected = tab == item,
                                onClick = { tab = item; search = ""; availableOnly = false },
                                label = { Text(item.title) },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    search,
                    { search = it },
                    label = { Text(if (tab == DevelopmentTab.CHI) "Поиск развития или приёма ЦИ" else "Поиск по навыкам, веткам и стилям") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (tab != DevelopmentTab.OWNED) {
                    FilterChip(selected = availableOnly, onClick = { availableOnly = !availableOnly }, label = { Text("Доступно сейчас") })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("XP: ${economy.spentXp} потрачено · ${economy.remainingXp} осталось из ${economy.totalExperience}", color = DublGold)
                    Text("ОС: ${economy.abilityPointsSpent} / ${economy.abilityPointsBudget} (${economy.abilityPointsRemaining} свободно)", color = DublFocus)
                }
                Text(
                    "Характеристики ${economy.attributeXp} · Умения ${economy.skillXp} · Навыки ${economy.developmentXp} · ЦИ ${economy.chiXp} · Магия ${economy.manaXp + economy.magicSchoolXp + economy.spellXp}" +
                        if (economy.adjustmentXp != 0) " · Поправка ${economy.adjustmentXp}" else "",
                    color = DublMuted,
                )
                if (tab == DevelopmentTab.SPECIAL) {
                    Text("Сначала открывается доступ ветки за ОС, затем её дочерние навыки покупаются за XP.", color = DublMuted)
                }
            }
        }

        if (tab == DevelopmentTab.CHI) {
            item { ChiResourceCard(state) }
            source.groupBy { it.category.ifBlank { "Развитие ЦИ" } }.forEach { (category, entries) ->
                item(key = "chi-development-header-$category") { DevelopmentGroupHeader(category, entries.size) }
                items(entries, key = { "chi-development-${it.id}" }) { entry ->
                    DevelopmentEntryCard(state, entry, rules, onDetails = { selected = entry })
                }
            }
            filteredChiTechniques.groupBy { it.school }.forEach { (school, techniques) ->
                item(key = "chi-technique-header-$school") { DevelopmentGroupHeader(school, techniques.size, "Приёмы") }
                items(techniques, key = { "chi-technique-${it.id}" }) { technique ->
                    val availability = chiRules.availability(technique)
                    SectionCard(technique.name) {
                        Text("${technique.chiCost} ЦИ · ${technique.action}", color = DublMuted)
                        Text(technique.effect, color = DublMuted)
                        if (availability.reason.isNotBlank()) Text(availability.reason, color = MaterialTheme.colorScheme.error)
                        Button(enabled = availability.canUse, onClick = { state.mutate { changeChi(-availability.chiCost) } }) { Text("Использовать") }
                    }
                }
            }
        } else if (source.isEmpty()) {
            item { EmptyState(if (tab == DevelopmentTab.OWNED) "Пока ничего не взято." else "Ничего не найдено — сбросьте поиск или фильтр доступности.") }
        } else {
            when (tab) {
                DevelopmentTab.REGULAR -> source.groupBy { it.category.ifBlank { "Общие" } }.forEach { (category, entries) ->
                    item(key = "regular-header-$category") { DevelopmentGroupHeader(category, entries.size) }
                    items(entries, key = { it.id }) { entry -> DevelopmentEntryCard(state, entry, rules) { selected = entry } }
                }
                DevelopmentTab.SPECIAL -> source.groupBy(::branchName).forEach { (branch, entries) ->
                    item(key = "special-header-$branch") { DevelopmentGroupHeader(branch, entries.size, "Спец. ветка") }
                    items(entries.sortedWith(compareBy<DevelopmentEntry>({ if (it.isAbility) 0 else 1 }, { developmentNormalize(it.name) })), key = { it.id }) { entry ->
                        DevelopmentEntryCard(state, entry, rules) { selected = entry }
                    }
                }
                DevelopmentTab.MARTIAL -> source.groupBy { it.category.ifBlank { "Боевые искусства" } }.forEach { (category, entries) ->
                    item(key = "martial-header-$category") { DevelopmentGroupHeader(category, entries.size) }
                    items(entries, key = { it.id }) { entry -> DevelopmentEntryCard(state, entry, rules) { selected = entry } }
                }
                DevelopmentTab.OWNED -> {
                    val regular = source.filter { it.isRegularDevelopment }
                    val special = source.filter { it.isSpecialDevelopment }
                    val martial = source.filter { it.isMartialArt }
                    val chi = source.filter { it.isChiDevelopment }
                    if (regular.isNotEmpty()) {
                        item { DevelopmentGroupHeader("Обычные навыки", regular.size) }
                        items(regular, key = { "owned-${it.id}" }) { entry -> DevelopmentEntryCard(state, entry, rules) { selected = entry } }
                    }
                    special.groupBy(::branchName).forEach { (branch, entries) ->
                        item(key = "owned-special-header-$branch") { DevelopmentGroupHeader(branch, entries.size, "Спец. ветка") }
                        items(entries, key = { "owned-${it.id}" }) { entry -> DevelopmentEntryCard(state, entry, rules) { selected = entry } }
                    }
                    if (martial.isNotEmpty()) {
                        item { DevelopmentGroupHeader("Боевые искусства", martial.size) }
                        items(martial, key = { "owned-${it.id}" }) { entry -> DevelopmentEntryCard(state, entry, rules) { selected = entry } }
                    }
                    if (chi.isNotEmpty()) {
                        item { DevelopmentGroupHeader("ЦИ", chi.size) }
                        items(chi, key = { "owned-${it.id}" }) { entry -> DevelopmentEntryCard(state, entry, rules) { selected = entry } }
                    }
                }
                DevelopmentTab.CHI -> Unit
            }
        }
    }

    selected?.let { entry ->
        DevelopmentDetailsDialog(
            state = state,
            entry = entry,
            onOpenEntry = { targetId -> state.developmentCatalog.byId(targetId)?.let { selected = it } },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun DevelopmentGroupHeader(title: String, count: Int, trailing: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = DublFocus, fontWeight = FontWeight.Bold)
        Text(listOfNotNull(trailing, count.toString()).joinToString(" · "), color = DublMuted)
    }
}

@Composable
private fun DevelopmentEntryCard(
    state: DesktopAppState,
    entry: DevelopmentEntry,
    rules: DevelopmentRules,
    onDetails: () -> Unit,
) {
    val owned = state.activeCharacter.development[entry.id] ?: OwnedDevelopment()
    val availability = rules.availability(entry, owned.optionIndex)
    val hasRequirementIssue = availability.checks.any { it.status != RequirementStatus.OK }
    val manualRequirement = availability.checks.any { it.status == RequirementStatus.MANUAL }
    SectionCard(title = entry.name, action = { OutlinedButton(onClick = onDetails) { Text("Подробнее") } }) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${entry.category.ifBlank { entry.section }} · ${if (entry.isAbility) "${availability.abilityCost} ОС" else "${entry.cost} XP"}", color = DublMuted)
            Text("Ранг ${owned.rank}/${entry.maxRank}", color = DublFocus, fontWeight = FontWeight.Bold)
            if (hasRequirementIssue) {
                Text(
                    if (manualRequirement) "Требуется ручная проверка" else "Требования не выполнены",
                    color = if (manualRequirement) DublGold else MaterialTheme.colorScheme.error,
                )
            }
        }
        if (entry.benefit.isNotBlank()) Text(entry.benefit, color = DublMuted)
        if (entry.accessId != null) Text("Ветка: ${state.developmentCatalog.byId(entry.accessId)?.name ?: entry.accessId}", color = DublMuted)
    }
}


@Composable
internal fun DevelopmentDetailsDialog(
    state: DesktopAppState,
    entry: DevelopmentEntry,
    onOpenEntry: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val character = state.activeCharacter
    val owned = character.development[entry.id] ?: OwnedDevelopment()
    val rules = DevelopmentRules(character, state.developmentCatalog, DevelopmentProgress(character.development))
    var optionIndex by remember(entry.id, owned.optionIndex) { mutableStateOf(owned.optionIndex) }
    val availability = rules.availability(entry, optionIndex)
    val children = state.developmentCatalog.childrenOf(entry.id)
    var pendingRequirementOverride by remember(entry.id) { mutableStateOf(false) }
    var pendingAbilityPurchase by remember(entry.id) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(entry.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KeyValue("Раздел", entry.section)
                KeyValue("Категория", entry.category)
                KeyValue("Стоимость", if (entry.isAbility) "${rules.abilityCost(entry, optionIndex)} ОС" else "${entry.cost} XP / ранг")
                KeyValue("Ранг", "${owned.rank}/${entry.maxRank}")
                if (entry.abilityOptions.isNotEmpty()) {
                    Text("Источник ОС / вариант", fontWeight = FontWeight.SemiBold)
                    entry.abilityOptions.forEachIndexed { index, option ->
                        OutlinedButton(
                            enabled = owned.rank == 0,
                            onClick = { optionIndex = index },
                        ) { Text("${if (index == optionIndex) "✓ " else ""}${option.source}: ${option.value} ОС") }
                    }
                }
                if (entry.requirements.isNotBlank()) Text("Требования: ${entry.requirements}")
                availability.checks.forEach { check ->
                    val tint = when (check.status) {
                        RequirementStatus.OK -> DublMuted
                        RequirementStatus.MANUAL -> DublGold
                        RequirementStatus.FAIL -> MaterialTheme.colorScheme.error
                    }
                    check.targetEntryId?.let { targetId ->
                        TextButton(onClick = { onOpenEntry(targetId) }) {
                            Text("${check.status}: ${check.text} →", color = tint)
                        }
                    } ?: Text("${check.status}: ${check.text}", color = tint)
                }
                if (children.isNotEmpty()) {
                    Text("Открывает ${children.size}", fontWeight = FontWeight.SemiBold)
                    children.take(8).forEach { child ->
                        TextButton(onClick = { onOpenEntry(child.id) }) {
                            Text(child.name)
                        }
                    }
                    if (children.size > 8) Text("И ещё ${children.size - 8} записей в ветке", color = DublMuted)
                }
                if (entry.benefit.isNotBlank()) Text(entry.benefit)
                if (entry.notes.isNotBlank()) Text(entry.notes, color = DublMuted)
                if (entry.mechanicsConflict.isNotBlank()) Text(entry.mechanicsConflict, color = MaterialTheme.colorScheme.error)
                if (entry.conflictNote.isNotBlank()) Text(entry.conflictNote, color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RankStepper(owned.rank, max = entry.maxRank.coerceAtLeast(1)) { next ->
                        when {
                            next <= owned.rank -> state.mutate { setDevelopmentRank(entry.id, next, optionIndex) }
                            availability.canIncrease && entry.isAbility -> pendingAbilityPurchase = true
                            availability.canIncrease -> state.mutate { setDevelopmentRank(entry.id, next, optionIndex) }
                            availability.canForceIncrease -> pendingRequirementOverride = true
                        }
                    }
                    if (!availability.canIncrease && availability.canForceIncrease && owned.rank < entry.maxRank) {
                        Text("Можно взять принудительно", color = DublGold)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )

    if (pendingRequirementOverride) {
        val failedChecks = availability.checks.filter { it.status != RequirementStatus.OK }
        AlertDialog(
            onDismissRequest = { pendingRequirementOverride = false },
            title = { Text("Требования не выполнены") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(if (entry.isAbility) "Открыть «${entry.name}» несмотря на требования?" else "Добавить «${entry.name}» несмотря на требования?")
                    failedChecks.forEach { check -> Text("• ${check.text}", color = if (check.status == RequirementStatus.FAIL) MaterialTheme.colorScheme.error else DublGold) }
                    Text(
                        if (entry.isAbility) "Будет потрачено ${rules.abilityCost(entry, optionIndex)} ОС."
                        else "Будет учтено ${entry.cost} XP за следующий ранг.",
                        color = DublGold,
                    )
                    Text("Запись останется помеченной требованиями, пока условия не будут выполнены.", color = DublMuted)
                }
            },
            confirmButton = {
                Button(onClick = {
                    state.mutate { setDevelopmentRank(entry.id, owned.rank + 1, optionIndex) }
                    pendingRequirementOverride = false
                }) { Text(if (entry.isAbility) "Открыть всё равно" else "Добавить всё равно") }
            },
            dismissButton = { TextButton(onClick = { pendingRequirementOverride = false }) { Text("Отмена") } },
        )
    }

    if (pendingAbilityPurchase) {
        val cost = rules.abilityCost(entry, optionIndex)
        AlertDialog(
            onDismissRequest = { pendingAbilityPurchase = false },
            title = { Text("Открыть спец. ветку?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(entry.name, fontWeight = FontWeight.Bold)
                    entry.abilityOptions.getOrNull(optionIndex)?.let { Text("Источник: ${it.source}") }
                    Text("Будет потрачено $cost ОС. Навыки внутри ветки покупаются отдельно за XP.")
                    if (character.creationComplete) Text("Создание уже завершено; продолжайте по решению мастера.", color = DublGold)
                }
            },
            confirmButton = {
                Button(onClick = {
                    state.mutate { setDevelopmentRank(entry.id, owned.rank + 1, optionIndex) }
                    pendingAbilityPurchase = false
                }) { Text("Открыть · $cost ОС") }
            },
            dismissButton = { TextButton(onClick = { pendingAbilityPurchase = false }) { Text("Отмена") } },
        )
    }
}

@Composable
private fun ChiResourceCard(state: DesktopAppState) {
    val character = state.activeCharacter
    val automaticAccess = character.chiAutomaticAccess
    val progressionBonus = character.chiProgressionBonus
    val baseMaximum = character.chiBaseMaximum

    SectionCard("Ресурс ЦИ") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(if (character.chiActive) "${character.chiCurrent} / ${character.chiMaximum}" else "ЦИ выключено", color = DublFocus, fontWeight = FontWeight.Bold)
                Text("Доп. ранги: ${character.chiBonusRanks} / 10 · ${CharacterEconomy.CHI_BONUS_RANK_XP} XP за ранг", color = DublMuted)
            }
            Switch(
                checked = character.chiActive,
                onCheckedChange = { enabled -> state.mutate { setChiEnabled(enabled) } },
                enabled = !automaticAccess,
            )
            if (character.chiActive) {
                OutlinedButton(
                    enabled = character.chiCurrent > 0,
                    onClick = { state.mutate { changeChi(-1) } },
                ) { Text("−1") }
                Button(
                    enabled = character.chiCurrent < character.chiMaximum,
                    onClick = { state.mutate { changeChi(1) } },
                ) { Text("+1") }
                TextButton(
                    enabled = character.chiCurrent < character.chiMaximum,
                    onClick = { state.mutate { restoreChi() } },
                ) { Text("Восстановить") }
            }
        }
        if (automaticAccess) {
            Text("Ресурс открыт способностью «Внутренняя ЦИ» и остаётся активным, пока способность изучена.", color = DublFocus)
        }
        if (!character.chiActive) {
            Text("Включение ресурса само по себе не расходует XP и не выдаёт способности автоматически.", color = DublMuted)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Бонусные ранги")
                RankStepper(character.chiBonusRanks, max = 10) { state.mutate { setChiBonusRanks(it) } }
            }
            Text(
                "Максимум: база $baseMaximum + купленный запас ${character.chiBonusRanks} + развитие $progressionBonus = ${character.chiMaximum}.",
                color = DublMuted,
            )
            Text(
                "Запас полностью восстанавливается после 15 минут медитации/лёгкой активности или после 8 часов отдыха.",
                color = DublMuted,
            )
        }
    }
}

@Composable
private fun ChiTechniquesCard(state: DesktopAppState) {
    val character = state.activeCharacter
    val rules = ChiRules(character, state.developmentCatalog)
    SectionCard("Приёмы ЦИ") {
        if (state.chiCatalog.techniques.isEmpty()) EmptyState("Приёмов ЦИ в каталоге нет.")
        state.chiCatalog.techniques.forEach { technique ->
            val availability = rules.availability(technique)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${technique.name} · ${technique.school}", fontWeight = FontWeight.SemiBold)
                    Text("${technique.chiCost} ЦИ · ${technique.action}", color = DublMuted)
                    Text(technique.effect, color = DublMuted)
                    if (availability.reason.isNotBlank()) Text(availability.reason, color = MaterialTheme.colorScheme.error)
                }
                Button(enabled = availability.canUse, onClick = { state.mutate { changeChi(-availability.chiCost) } }) { Text("Использовать") }
            }
        }
    }
}
