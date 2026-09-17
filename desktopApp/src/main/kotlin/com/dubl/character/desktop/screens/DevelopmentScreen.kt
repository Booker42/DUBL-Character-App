package com.dubl.character.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dubl.character.android.model.AbilityOption
import com.dubl.character.android.model.CharacterEconomy
import com.dubl.character.android.model.ChiRules
import com.dubl.character.android.model.DevelopmentAcquisitionChoice
import com.dubl.character.android.model.DevelopmentAcquisitionPlan
import com.dubl.character.android.model.DevelopmentAcquisitionPlanner
import com.dubl.character.android.model.DevelopmentAcquisitionRequest
import com.dubl.character.android.model.DevelopmentAcquisitionStep
import com.dubl.character.android.model.DevelopmentAcquisitionTarget
import com.dubl.character.android.model.DevelopmentCostType
import com.dubl.character.android.model.DevelopmentEffectIds
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.DevelopmentProgress
import com.dubl.character.android.model.DevelopmentRules
import com.dubl.character.android.model.MagicEquipmentRules
import com.dubl.character.android.model.OwnedDevelopment
import com.dubl.character.android.model.RequirementStatus
import com.dubl.character.android.model.developmentNormalize
import com.dubl.character.android.model.developmentRank
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.desktop.DesktopAppState

private enum class DevelopmentTab(val title: String) {
    REGULAR("Обычные"), SPECIAL("Спец. ветки"), MARTIAL("Боевые искусства"), CHI("ЦИ"), OWNED("Взято")
}

private enum class DevelopmentBrowserFilter(val title: String) {
    ALL("Все"), AVAILABLE("Можно взять"), ALMOST("Почти доступно"), PLAN("План")
}

private data class DevelopmentGridSection(
    val title: String,
    val trailing: String? = null,
    val entries: List<DevelopmentEntry>,
)

@Composable
fun DevelopmentScreen(state: DesktopAppState, modifier: Modifier = Modifier) {
    val character = state.activeCharacter
    var tab by remember(character.id) { mutableStateOf(DevelopmentTab.REGULAR) }
    var availableOnly by remember(character.id) { mutableStateOf(false) }
    var browserFilter by remember(character.id) { mutableStateOf(DevelopmentBrowserFilter.ALL) }
    var search by remember(character.id) { mutableStateOf("") }
    var selectedEntryId by remember(character.id) { mutableStateOf<String?>(null) }
    var plannedDevelopmentIds by remember(character.id) { mutableStateOf(emptySet<String>()) }
    var acquisitionRequest by remember(character.id) { mutableStateOf<DevelopmentAcquisitionRequest?>(null) }
    var legacyDetailsEntry by remember(character.id) { mutableStateOf<DevelopmentEntry?>(null) }
    var editingDevelopment by remember(character.id) { mutableStateOf<DevelopmentEntry?>(null) }
    var creatingCustomDevelopment by remember(character.id) { mutableStateOf(false) }

    val progress = DevelopmentProgress(character.development)
    val rules = DevelopmentRules(character, state.developmentCatalog, progress)
    val planner = DevelopmentAcquisitionPlanner(character, state.developmentCatalog)
    val chiRules = ChiRules(character, state.developmentCatalog)
    val economy = CharacterEconomy.breakdown(character, state.developmentCatalog)

    fun branchName(entry: DevelopmentEntry): String = when {
        entry.isAbility -> entry.name
        entry.accessId != null -> state.developmentCatalog.byId(entry.accessId)?.name ?: entry.category.ifBlank { entry.name }
        else -> entry.category.ifBlank { entry.name }
    }

    fun prerequisitePlan(entry: DevelopmentEntry): DevelopmentAcquisitionPlan = planner.plan(
        DevelopmentAcquisitionRequest.single(entry.id, includeTarget = false, enforceBudget = false),
    )

    val needle = developmentNormalize(search)
    val source = state.developmentCatalog.entries
        .asSequence()
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
        .filter { entry ->
            if (tab == DevelopmentTab.OWNED || tab == DevelopmentTab.CHI) {
                tab == DevelopmentTab.OWNED || !availableOnly || rules.availability(entry).canIncrease
            } else {
                val availability = rules.availability(entry)
                when (browserFilter) {
                    DevelopmentBrowserFilter.ALL -> true
                    DevelopmentBrowserFilter.AVAILABLE -> availability.canIncrease
                    DevelopmentBrowserFilter.PLAN -> entry.id in plannedDevelopmentIds
                    DevelopmentBrowserFilter.ALMOST -> {
                        if (availability.canIncrease) false else {
                            val missing = prerequisitePlan(entry)
                            missing.unresolvedRequirements.isEmpty() && missing.steps.size == 1
                        }
                    }
                }
            }
        }
        .sortedWith(
            compareBy<DevelopmentEntry>(
                { if (tab == DevelopmentTab.SPECIAL || (tab == DevelopmentTab.OWNED && it.isSpecialDevelopment)) developmentNormalize(branchName(it)) else developmentNormalize(it.category) },
                { if (it.isAbility) 0 else 1 },
                { developmentNormalize(it.name) },
            ),
        )
        .toList()

    val sections: List<DevelopmentGridSection> = when (tab) {
        DevelopmentTab.REGULAR -> source.groupBy { it.category.ifBlank { "Общие" } }
            .map { (name, entries) -> DevelopmentGridSection(name, entries = entries) }
        DevelopmentTab.SPECIAL -> source.groupBy(::branchName)
            .map { (name, entries) -> DevelopmentGridSection(name, "Спец. ветка", entries) }
        DevelopmentTab.MARTIAL -> source.groupBy { it.category.ifBlank { "Боевые искусства" } }
            .map { (name, entries) -> DevelopmentGridSection(name, entries = entries) }
        DevelopmentTab.OWNED -> listOf(
            DevelopmentGridSection("Обычные навыки", entries = source.filter { it.isRegularDevelopment }),
            DevelopmentGridSection("Боевые искусства", entries = source.filter { it.isMartialArt }),
            DevelopmentGridSection("Спец. ветки", entries = source.filter { it.isSpecialDevelopment }),
            DevelopmentGridSection("ЦИ", entries = source.filter { it.isChiDevelopment }),
        ).filter { it.entries.isNotEmpty() }
        DevelopmentTab.CHI -> source.groupBy { it.category.ifBlank { "Развитие ЦИ" } }
            .map { (name, entries) -> DevelopmentGridSection(name, entries = entries) }
    }

    val plannedSummary = if (plannedDevelopmentIds.isNotEmpty()) {
        planner.plan(
            DevelopmentAcquisitionRequest(
                targets = plannedDevelopmentIds.sorted().map { DevelopmentAcquisitionTarget(it) },
                enforceBudget = false,
            ),
        )
    } else null

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionCard("Навыки и развитие") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DevelopmentTab.entries.forEach { target ->
                    FilterChip(
                        selected = tab == target,
                        onClick = {
                            tab = target
                            search = ""
                            availableOnly = false
                            browserFilter = DevelopmentBrowserFilter.ALL
                        },
                        label = { Text(target.title) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text(if (tab == DevelopmentTab.CHI) "Поиск развития или приёма ЦИ" else "Поиск по навыкам, веткам и стилям") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { creatingCustomDevelopment = true }) { Text("Своя запись") }
            }
            if (tab == DevelopmentTab.CHI) {
                FilterChip(
                    selected = availableOnly,
                    onClick = { availableOnly = !availableOnly },
                    label = { Text("Доступно сейчас") },
                )
            } else if (tab != DevelopmentTab.OWNED) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DevelopmentBrowserFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = browserFilter == filter,
                            onClick = { browserFilter = filter },
                            label = { Text(filter.title) },
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Text("Осталось XP: ${economy.remainingXp} / ${economy.totalExperience}", color = if (economy.overspentXp) MaterialTheme.colorScheme.error else DublGold)
                Text("ОС: ${economy.abilityPointsRemaining} свободно из ${economy.abilityPointsBudget}", color = DublFocus)
            }
            Text(
                "Потрачено: характеристики ${economy.attributeXp} · умения ${economy.skillXp} · навыки ${economy.developmentXp} · ЦИ ${economy.chiXp} · магия ${economy.manaXp + economy.magicSchoolXp + economy.spellXp}",
                color = DublMuted,
            )
            plannedSummary?.let { summary ->
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = DublGold.copy(alpha = 0.05f),
                    border = BorderStroke(1.dp, DublGold.copy(alpha = 0.25f)),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text("План персонажа · ${plannedDevelopmentIds.size}", fontWeight = FontWeight.Bold)
                            Text("Осталось добрать: ${summary.xpCost} XP${if (summary.abilityCost > 0) " · ${summary.abilityCost} ОС" else ""}", color = DublMuted)
                        }
                        Button(
                            enabled = summary.steps.isNotEmpty(),
                            onClick = {
                                acquisitionRequest = DevelopmentAcquisitionRequest(
                                    targets = plannedDevelopmentIds.sorted().map { DevelopmentAcquisitionTarget(it) },
                                )
                            },
                        ) { Text("Взять план") }
                    }
                }
            }
        }

        if (tab == DevelopmentTab.CHI) {
            val filteredChiTechniques = state.chiCatalog.techniques
                .filter { technique ->
                    val matches = needle.isBlank() || developmentNormalize(
                        listOf(technique.name, technique.school, technique.action, technique.effect, technique.requirements).joinToString(" "),
                    ).contains(needle)
                    matches && (!availableOnly || chiRules.availability(technique).unlocked)
                }
                .sortedWith(compareBy({ developmentNormalize(it.school) }, { developmentNormalize(it.name) }))
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LazyColumn(Modifier.weight(1f).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { ChiResourceCard(state) }
                    sections.forEach { section ->
                        item(key = "chi-development-header-${section.title}") { DevelopmentGroupHeader(section.title, section.entries.size) }
                        items(section.entries, key = { "chi-development-${it.id}" }) { entry ->
                            DevelopmentCompactCard(
                                entry = entry,
                                rules = rules,
                                progress = progress,
                                planned = entry.id in plannedDevelopmentIds,
                                onSelect = { selectedEntryId = if (selectedEntryId == entry.id) null else entry.id },
                            )
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
                                Button(enabled = availability.canUse, onClick = { state.changeChi(-availability.chiCost) }) { Text("Использовать") }
                            }
                        }
                    }
                }
                selectedEntryId?.let { id ->
                    state.developmentCatalog.byId(id)?.let { entry ->
                        DevelopmentInspector(
                            state = state,
                            entry = entry,
                            rules = rules,
                            planner = planner,
                            planned = entry.id in plannedDevelopmentIds,
                            onTogglePlanned = {
                                plannedDevelopmentIds = if (entry.id in plannedDevelopmentIds) plannedDevelopmentIds - entry.id else plannedDevelopmentIds + entry.id
                            },
                            onOpenEntry = { targetId -> selectedEntryId = targetId },
                            onAcquireRequirements = { acquisitionRequest = DevelopmentAcquisitionRequest.single(entry.id, includeTarget = false) },
                            onAcquireAll = { optionIndex ->
                                acquisitionRequest = DevelopmentAcquisitionRequest.single(entry.id, includeTarget = true, optionIndex = optionIndex)
                            },
                            onManual = { legacyDetailsEntry = entry },
                            onEditLocal = { editingDevelopment = entry },
                            onResetLocal = { state.resetDevelopmentOverride(entry.id) },
                            onDeleteCustom = { state.removeCustomDevelopment(entry.id); selectedEntryId = null },
                            onClose = { selectedEntryId = null },
                        )
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BoxWithConstraints(Modifier.weight(1f).fillMaxSize()) {
                    val columnCount = if (maxWidth >= 840.dp) 2 else 1
                    if (sections.isEmpty()) {
                        EmptyState(if (tab == DevelopmentTab.OWNED) "Пока ничего не взято." else "Ничего не найдено — сбросьте поиск или фильтр.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            sections.forEach { section ->
                                item(
                                    key = "development-header-${section.title}",
                                    span = { GridItemSpan(maxLineSpan) },
                                ) { DevelopmentGroupHeader(section.title, section.entries.size, section.trailing) }
                                gridItems(section.entries, key = { it.id }) { entry ->
                                    DevelopmentCompactCard(
                                        entry = entry,
                                        rules = rules,
                                        progress = progress,
                                        planned = entry.id in plannedDevelopmentIds,
                                        onSelect = { selectedEntryId = if (selectedEntryId == entry.id) null else entry.id },
                                    )
                                }
                            }
                        }
                    }
                }
                selectedEntryId?.let { id ->
                    state.developmentCatalog.byId(id)?.let { entry ->
                        DevelopmentInspector(
                            state = state,
                            entry = entry,
                            rules = rules,
                            planner = planner,
                            planned = entry.id in plannedDevelopmentIds,
                            onTogglePlanned = {
                                plannedDevelopmentIds = if (entry.id in plannedDevelopmentIds) plannedDevelopmentIds - entry.id else plannedDevelopmentIds + entry.id
                            },
                            onOpenEntry = { targetId -> selectedEntryId = targetId },
                            onAcquireRequirements = {
                                acquisitionRequest = DevelopmentAcquisitionRequest.single(entry.id, includeTarget = false)
                            },
                            onAcquireAll = { optionIndex ->
                                acquisitionRequest = DevelopmentAcquisitionRequest.single(
                                    entryId = entry.id,
                                    includeTarget = true,
                                    optionIndex = optionIndex,
                                )
                            },
                            onManual = { legacyDetailsEntry = entry },
                            onEditLocal = { editingDevelopment = entry },
                            onResetLocal = { state.resetDevelopmentOverride(entry.id) },
                            onDeleteCustom = {
                                state.removeCustomDevelopment(entry.id)
                                selectedEntryId = null
                            },
                            onClose = { selectedEntryId = null },
                        )
                    }
                }
            }
        }
    }

    acquisitionRequest?.let { request ->
        DevelopmentAcquisitionPreview(
            state = state,
            request = request,
            onDismiss = { acquisitionRequest = null },
        )
    }

    legacyDetailsEntry?.let { entry ->
        DevelopmentDetailsDialog(
            state = state,
            entry = entry,
            onOpenEntry = { targetId -> state.developmentCatalog.byId(targetId)?.let { selectedEntryId = it.id; legacyDetailsEntry = null } },
            onEditLocal = { editingDevelopment = entry; legacyDetailsEntry = null },
            onResetLocal = { state.resetDevelopmentOverride(entry.id); legacyDetailsEntry = null },
            onDeleteCustom = { state.removeCustomDevelopment(entry.id); legacyDetailsEntry = null; selectedEntryId = null },
            hasLocalOverride = character.developmentOverrides.containsKey(entry.id),
            isCustom = character.customDevelopmentEntries.any { it.id == entry.id },
            onDismiss = { legacyDetailsEntry = null },
        )
    }

    editingDevelopment?.let { entry ->
        val isCustom = character.customDevelopmentEntries.any { it.id == entry.id }
        DevelopmentLocalEditDialog(
            initial = entry,
            title = if (isCustom) "Редактировать свою запись" else "Локальная правка",
            onSave = { updated ->
                if (isCustom) state.updateCustomDevelopment(updated) else state.setDevelopmentOverride(updated)
                editingDevelopment = null
            },
            onDismiss = { editingDevelopment = null },
        )
    }

    if (creatingCustomDevelopment) {
        DevelopmentLocalEditDialog(
            initial = emptyCustomDevelopmentEntry(),
            title = "Своя запись",
            onSave = { updated ->
                state.addCustomDevelopment(updated)
                creatingCustomDevelopment = false
            },
            onDismiss = { creatingCustomDevelopment = false },
        )
    }
}

@Composable
private fun DevelopmentCompactCard(
    entry: DevelopmentEntry,
    rules: DevelopmentRules,
    progress: DevelopmentProgress,
    planned: Boolean,
    onSelect: () -> Unit,
) {
    val rank = progress.rank(entry.id)
    val availability = rules.availability(entry)
    val hasManual = availability.checks.any { it.status == RequirementStatus.MANUAL }
    val accent = when {
        rank > 0 -> DublGold
        availability.canIncrease -> DublFocus
        else -> MaterialTheme.colorScheme.outline
    }
    Surface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 112.dp).clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = if (rank > 0 || availability.canIncrease || planned) 0.45f else 0.22f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(entry.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$rank/${entry.maxRank}", color = DublFocus, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (entry.isAbility) "${availability.abilityCost} ОС" else "${entry.cost} XP", color = DublMuted)
                Text(
                    when {
                        planned -> "★ В плане"
                        rank > 0 && availability.checks.any { it.status != RequirementStatus.OK } -> "⚠ Требования"
                        rank > 0 -> "✓ Взято"
                        availability.canIncrease -> "✓ Доступно"
                        hasManual -> "? Проверить"
                        else -> "Не хватает требований"
                    },
                    color = when {
                        planned -> DublGold
                        availability.canIncrease || rank > 0 -> DublFocus
                        else -> DublMuted
                    },
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (entry.benefit.isNotBlank()) {
                Text(entry.benefit, color = DublMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DevelopmentInspector(
    state: DesktopAppState,
    entry: DevelopmentEntry,
    rules: DevelopmentRules,
    planner: DevelopmentAcquisitionPlanner,
    planned: Boolean,
    onTogglePlanned: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onAcquireRequirements: () -> Unit,
    onAcquireAll: (Int) -> Unit,
    onManual: () -> Unit,
    onEditLocal: () -> Unit,
    onResetLocal: () -> Unit,
    onDeleteCustom: () -> Unit,
    onClose: () -> Unit,
) {
    val character = state.activeCharacter
    val rank = character.developmentRank(entry.id)
    var optionIndex by remember(entry.id, rank) {
        mutableStateOf(if (rank > 0) character.development[entry.id]?.optionIndex ?: 0 else 0)
    }
    val availability = rules.availability(entry, optionIndex)
    val missing = planner.plan(DevelopmentAcquisitionRequest.single(entry.id, includeTarget = false, enforceBudget = false))
    val unlocks = planner.unlocks(entry.id)
    val hasLocalOverride = character.developmentOverrides.containsKey(entry.id)
    val isCustom = character.customDevelopmentEntries.any { it.id == entry.id }

    Surface(
        modifier = Modifier.widthIn(min = 340.dp, max = 410.dp).fillMaxSize(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${entry.category.ifBlank { entry.section }} · ранг $rank/${entry.maxRank}", color = DublMuted)
                }
                TextButton(onClick = onClose) { Text("×") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = planned, onClick = onTogglePlanned, label = { Text(if (planned) "★ В плане" else "☆ В план") })
                Text(if (entry.isAbility) "${availability.abilityCost} ОС" else "${entry.cost} XP / ранг", color = DublGold)
            }
            if (entry.benefit.isNotBlank()) {
                Text("Что даёт", fontWeight = FontWeight.Bold)
                Text(entry.benefit)
            }
            if (entry.notes.isNotBlank()) Text(entry.notes, color = DublMuted)

            Text("Требования", fontWeight = FontWeight.Bold)
            availability.checks.forEach { check ->
                val tint = when (check.status) {
                    RequirementStatus.OK -> DublFocus
                    RequirementStatus.MANUAL -> DublGold
                    RequirementStatus.FAIL -> MaterialTheme.colorScheme.error
                }
                check.targetEntryId?.let { id ->
                    TextButton(onClick = { onOpenEntry(id) }) { Text("${if (check.status == RequirementStatus.OK) "✓" else "✕"} ${check.text} →", color = tint) }
                } ?: Text("${if (check.status == RequirementStatus.OK) "✓" else if (check.status == RequirementStatus.MANUAL) "?" else "✕"} ${check.text}", color = tint)
            }

            if (missing.steps.isNotEmpty() || missing.unresolvedRequirements.isNotEmpty()) {
                Text("Что нужно сделать", fontWeight = FontWeight.Bold)
                missing.steps.forEach { step -> Text("• ${developmentAcquisitionStepText(step)}", color = DublMuted) }
                missing.unresolvedRequirements.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                Text("Требования: ${missing.xpCost} XP${if (missing.abilityCost > 0) " · ${missing.abilityCost} ОС" else ""}", color = DublGold)
            } else {
                Text("Все требования уже выполнены", color = DublFocus)
            }

            if (unlocks.isNotEmpty()) {
                Text("Открывает", fontWeight = FontWeight.Bold)
                unlocks.take(10).forEach { target ->
                    TextButton(onClick = { onOpenEntry(target.id) }) { Text("→ ${target.name}") }
                }
                if (unlocks.size > 10) Text("И ещё ${unlocks.size - 10}", color = DublMuted)
            }

            if (entry.isAbility && entry.abilityOptions.isNotEmpty()) {
                Text("Источник способности", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.abilityOptions.forEachIndexed { index, option ->
                        FilterChip(
                            selected = optionIndex == index,
                            enabled = rank == 0,
                            onClick = { optionIndex = index },
                            label = { Text("${option.source} · ${option.value} ОС") },
                        )
                    }
                }
                Text("Доступ к ветке: ${rules.abilityCost(entry, optionIndex)} ОС · доступно ${rules.abilityPointsAvailable()} ОС", color = DublGold)
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    enabled = missing.steps.isNotEmpty(),
                    onClick = onAcquireRequirements,
                    modifier = Modifier.weight(1f),
                ) { Text("Добрать требования") }
                Button(
                    enabled = rank < entry.maxRank,
                    onClick = { onAcquireAll(optionIndex) },
                    modifier = Modifier.weight(1f),
                ) { Text("Добрать и взять") }
            }
            if (rank > 0) {
                OutlinedButton(
                    onClick = { state.setDevelopmentRank(entry.id, rank - 1, state.activeCharacter.development[entry.id]?.optionIndex ?: 0) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (rank == 1) "Убрать" else "− ранг") }
            }
            TextButton(onClick = onManual) { Text("Ручное управление / принудительное добавление") }
            OutlinedButton(onClick = onEditLocal, modifier = Modifier.fillMaxWidth()) { Text("Локальная правка") }
            when {
                isCustom -> TextButton(onClick = onDeleteCustom) { Text("Удалить свою запись") }
                hasLocalOverride -> TextButton(onClick = onResetLocal) { Text("Сбросить к рулбуку") }
            }
        }
    }
}

private fun developmentAcquisitionStepText(step: DevelopmentAcquisitionStep): String = when (step) {
    is DevelopmentAcquisitionStep.Attribute -> "${step.label}: ${step.fromValue} → ${step.toValue} · ${step.xpCost} XP"
    is DevelopmentAcquisitionStep.Skill -> "${step.label}: ${step.fromRank} → ${step.toRank} · ${step.xpCost} XP"
    is DevelopmentAcquisitionStep.Development -> "${step.label}: ${step.fromRank} → ${step.toRank}" +
        when {
            step.abilityCost > 0 -> " · ${step.abilityCost} ОС"
            step.xpCost > 0 -> " · ${step.xpCost} XP"
            else -> ""
        }
}

@Composable
private fun DevelopmentAcquisitionPreview(
    state: DesktopAppState,
    request: DevelopmentAcquisitionRequest,
    onDismiss: () -> Unit,
) {
    var choiceSelections by remember(request) { mutableStateOf(request.choiceSelections) }
    val resolvedRequest = request.copy(choiceSelections = choiceSelections)
    val plan = DevelopmentAcquisitionPlanner(state.activeCharacter, state.developmentCatalog).plan(resolvedRequest)

    FuryDialog(
        onDismissRequest = onDismiss,
        title = { Text("План развития") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                plan.choices.forEach { choice: DevelopmentAcquisitionChoice ->
                    Text("Выберите путь: ${choice.label}", fontWeight = FontWeight.SemiBold)
                    choice.options.forEachIndexed { index, option ->
                        OutlinedButton(
                            onClick = { choiceSelections = choiceSelections + (choice.id to index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${if (choice.selectedIndex == index) "✓ " else ""}${option.label} · ${option.xpCost} XP${if (option.abilityCost > 0) " · ${option.abilityCost} ОС" else ""}")
                        }
                    }
                }
                if (plan.steps.isEmpty()) Text("Все выбранные требования уже выполнены.")
                plan.steps.forEach { step -> Text("• ${developmentAcquisitionStepText(step)}") }
                if (plan.unresolvedRequirements.isNotEmpty()) {
                    Text("Нельзя определить автоматически", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    plan.unresolvedRequirements.forEach { Text("• $it", color = MaterialTheme.colorScheme.error) }
                }
                Text("Итого: ${plan.xpCost} XP${if (plan.abilityCost > 0) " · ${plan.abilityCost} ОС" else ""}", color = DublGold, fontWeight = FontWeight.Bold)
                Text("После покупки: ${plan.xpRemainingAfter} XP · ${plan.abilityRemainingAfter} ОС", color = DublMuted)
                if (!plan.canAfford) {
                    Text("Недостаточно XP или очков способностей для автоматической покупки.", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = plan.canApply,
                onClick = {
                    state.acquireDevelopment(resolvedRequest)
                    onDismiss()
                },
            ) { Text("Применить · ${plan.xpCost} XP${if (plan.abilityCost > 0) " + ${plan.abilityCost} ОС" else ""}") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
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
    onEditLocal: () -> Unit,
    onResetLocal: () -> Unit,
    onDeleteCustom: () -> Unit,
    hasLocalOverride: Boolean,
    isCustom: Boolean,
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

    FuryDialog(
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
                            next <= owned.rank -> state.setDevelopmentRank(entry.id, next, optionIndex)
                            availability.canIncrease && entry.isAbility -> pendingAbilityPurchase = true
                            availability.canIncrease -> state.setDevelopmentRank(entry.id, next, optionIndex)
                            availability.canForceIncrease -> pendingRequirementOverride = true
                        }
                    }
                    if (!availability.canIncrease && availability.canForceIncrease && owned.rank < entry.maxRank) {
                        Text("Можно взять принудительно", color = DublGold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = onEditLocal) { Text("Локальная правка") }
                    when {
                        isCustom -> TextButton(onClick = onDeleteCustom) { Text("Удалить свою запись") }
                        hasLocalOverride -> TextButton(onClick = onResetLocal) { Text("Сбросить к рулбуку") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )

    if (pendingRequirementOverride) {
        val failedChecks = availability.checks.filter { it.status != RequirementStatus.OK }
        FuryDialog(
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
                    state.setDevelopmentRank(entry.id, owned.rank + 1, optionIndex)
                    pendingRequirementOverride = false
                }) { Text(if (entry.isAbility) "Открыть всё равно" else "Добавить всё равно") }
            },
            dismissButton = { TextButton(onClick = { pendingRequirementOverride = false }) { Text("Отмена") } },
        )
    }

    if (pendingAbilityPurchase) {
        val cost = rules.abilityCost(entry, optionIndex)
        FuryDialog(
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
                    state.setDevelopmentRank(entry.id, owned.rank + 1, optionIndex)
                    pendingAbilityPurchase = false
                }) { Text("Открыть · $cost ОС") }
            },
            dismissButton = { TextButton(onClick = { pendingAbilityPurchase = false }) { Text("Отмена") } },
        )
    }
}

private fun emptyCustomDevelopmentEntry(): DevelopmentEntry = DevelopmentEntry(
    id = "",
    name = "",
    section = "Свои",
    category = "Домашние правила",
    cost = 0,
    costType = DevelopmentCostType.XP,
    maxRank = 1,
    requirements = "",
    benefit = "",
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

@Composable
internal fun DevelopmentLocalEditDialog(
    initial: DevelopmentEntry,
    title: String,
    onSave: (DevelopmentEntry) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var section by remember(initial.id) { mutableStateOf(initial.section) }
    var category by remember(initial.id) { mutableStateOf(initial.category) }
    var cost by remember(initial.id) { mutableStateOf(initial.cost.toString()) }
    var maxRank by remember(initial.id) { mutableStateOf(initial.maxRank.toString()) }
    var requirements by remember(initial.id) { mutableStateOf(initial.requirements) }
    var benefit by remember(initial.id) { mutableStateOf(initial.benefit) }
    var notes by remember(initial.id) { mutableStateOf(initial.notes) }
    var tags by remember(initial.id) { mutableStateOf(initial.tags.joinToString(", ")) }
    var accessId by remember(initial.id) { mutableStateOf(initial.accessId.orEmpty()) }
    var abilityOptions by remember(initial.id) { mutableStateOf(initial.abilityOptions.joinToString("; ") { "${it.source}=${it.value}" }) }
    var mechanicsConflict by remember(initial.id) { mutableStateOf(initial.mechanicsConflict) }
    var conflictNote by remember(initial.id) { mutableStateOf(initial.conflictNote) }
    var costType by remember(initial.id) { mutableStateOf(initial.costType) }
    var incomplete by remember(initial.id) { mutableStateOf(initial.incomplete) }
    var repeatable by remember(initial.id) { mutableStateOf(initial.repeatable) }
    var perfectRoot by remember(initial.id) { mutableStateOf(initial.perfectRoot) }

    FuryDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(section, { section = it }, label = { Text("Раздел") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(category, { category = it }, label = { Text("Категория") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = costType == DevelopmentCostType.XP, onClick = { costType = DevelopmentCostType.XP }, label = { Text("XP") })
                    FilterChip(selected = costType == DevelopmentCostType.ABILITY, onClick = { costType = DevelopmentCostType.ABILITY }, label = { Text("ОС") })
                }
                OutlinedTextField(cost, { cost = it.filter(Char::isDigit) }, label = { Text("Стоимость") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(maxRank, { maxRank = it.filter(Char::isDigit) }, label = { Text("Макс. ранг") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(requirements, { requirements = it }, label = { Text("Требования") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(benefit, { benefit = it }, label = { Text("Эффект") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(notes, { notes = it }, label = { Text("Особое / заметки") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(tags, { tags = it }, label = { Text("Теги через запятую") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(accessId, { accessId = it }, label = { Text("ID родительской ветки") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(abilityOptions, { abilityOptions = it }, label = { Text("Варианты ОС: источник=цена; …") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(mechanicsConflict, { mechanicsConflict = it }, label = { Text("Механический конфликт") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(conflictNote, { conflictNote = it }, label = { Text("Комментарий конфликта") }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Правило неполное / спорное")
                    Switch(checked = incomplete, onCheckedChange = { incomplete = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Повторяемая запись")
                    Switch(checked = repeatable, onCheckedChange = { repeatable = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Корень совершенной ветки")
                    Switch(checked = perfectRoot, onCheckedChange = { perfectRoot = it })
                }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        initial.copy(
                            name = name,
                            section = section,
                            category = category,
                            cost = cost.toIntOrNull() ?: 0,
                            costType = costType,
                            maxRank = maxRank.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            requirements = requirements,
                            benefit = benefit,
                            notes = notes,
                            tags = tags.split(',').map(String::trim).filter(String::isNotBlank),
                            accessId = accessId.trim().takeIf(String::isNotBlank),
                            abilityOptions = abilityOptions.split(';').mapNotNull { raw ->
                                val parts = raw.split('=', limit = 2)
                                val source = parts.getOrNull(0)?.trim().orEmpty()
                                val value = parts.getOrNull(1)?.trim()?.toIntOrNull()
                                if (source.isBlank() || value == null) null else AbilityOption(source, value)
                            },
                            incomplete = incomplete,
                            repeatable = repeatable,
                            perfectRoot = perfectRoot,
                            mechanicsConflict = mechanicsConflict,
                            conflictNote = conflictNote,
                        ),
                    )
                },
            ) { Text("Сохранить локально") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
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
                onCheckedChange = { enabled -> state.setChiEnabled(enabled) },
                enabled = !automaticAccess,
            )
            if (character.chiActive) {
                OutlinedButton(
                    enabled = character.chiCurrent > 0,
                    onClick = { state.changeChi(-1) },
                ) { Text("−1") }
                Button(
                    enabled = character.chiCurrent < character.chiMaximum,
                    onClick = { state.changeChi(1) },
                ) { Text("+1") }
                TextButton(
                    enabled = character.chiCurrent < character.chiMaximum,
                    onClick = { state.restoreChi() },
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
                RankStepper(character.chiBonusRanks, max = 10) { state.setChiBonusRanks(it) }
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
                Button(enabled = availability.canUse, onClick = { state.changeChi(-availability.chiCost) }) { Text("Использовать") }
            }
        }
    }
}
