package com.dubl.character.android.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubl.character.android.data.ChiCatalogRepository
import com.dubl.character.android.data.DevelopmentCatalogRepository
import com.dubl.character.android.model.CharacterEconomy
import com.dubl.character.android.model.CharacterEconomyBreakdown
import com.dubl.character.android.model.ChiRules
import com.dubl.character.android.model.ChiTechnique
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DevelopmentEffectIds
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.DevelopmentProgress
import com.dubl.character.android.model.DevelopmentRules
import com.dubl.character.android.model.MagicEquipmentRules
import com.dubl.character.android.model.RequirementCheck
import com.dubl.character.android.model.RequirementStatus
import com.dubl.character.android.model.developmentNormalize
import com.dubl.character.android.model.developmentRank
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.containSheetOverscroll
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.components.DublScreenHeader
import com.dubl.character.android.ui.components.DublSwitch
import com.dubl.character.android.ui.theme.DublAccent
import com.dubl.character.android.ui.theme.DublDanger
import com.dubl.character.android.ui.theme.DublGold

private enum class DevelopmentTab(val title: String) {
    REGULAR("Обычные"),
    SPECIAL("Спец. ветки"),
    MARTIAL_ARTS("Боевые искусства"),
    CHI("ЦИ"),
    OWNED("Взято"),
}

private data class PendingAbilityPurchase(
    val entry: DevelopmentEntry,
    val optionIndex: Int,
)

private data class PendingRequirementOverride(
    val entry: DevelopmentEntry,
    val optionIndex: Int,
    val failedChecks: List<RequirementCheck>,
)

@Composable
fun FeatsScreen(controller: CharacterController) {
    val character = controller.active
    val context = LocalContext.current
    val catalog = remember(context.applicationContext) {
        DevelopmentCatalogRepository(context.applicationContext).load()
    }
    val chiCatalog = remember(context.applicationContext) {
        ChiCatalogRepository(context.applicationContext).load()
    }
    val progress = DevelopmentProgress(character.development)
    var query by remember(character.id) { mutableStateOf("") }
    var tab by remember(character.id) { mutableStateOf(DevelopmentTab.REGULAR) }
    var availableOnly by remember(character.id) { mutableStateOf(false) }
    var selectedEntryId by remember(character.id) { mutableStateOf<String?>(null) }
    var pendingAbilityPurchase by remember(character.id) { mutableStateOf<PendingAbilityPurchase?>(null) }
    var pendingRequirementOverride by remember(character.id) { mutableStateOf<PendingRequirementOverride?>(null) }

    val rules = remember(character, progress, catalog) {
        DevelopmentRules(character, catalog, progress)
    }
    val economy = remember(character, catalog) { CharacterEconomy.breakdown(character, catalog) }
    val chiRules = remember(character, catalog) { ChiRules(character, catalog) }

    fun increase(entry: DevelopmentEntry, optionIndex: Int) {
        val availability = rules.availability(entry, optionIndex)
        when {
            availability.canIncrease -> {
                if (entry.isAbility) {
                    pendingAbilityPurchase = PendingAbilityPurchase(entry, optionIndex)
                } else {
                    controller.setDevelopmentRank(entry.id, availability.currentRank + 1, optionIndex)
                }
            }
            availability.canForceIncrease -> {
                pendingRequirementOverride = PendingRequirementOverride(
                    entry = entry,
                    optionIndex = optionIndex,
                    failedChecks = availability.checks.filter { it.status != RequirementStatus.OK },
                )
            }
        }
    }

    fun decrease(entry: DevelopmentEntry) {
        val current = progress.rank(entry.id)
        if (current <= 0) return
        controller.setDevelopmentRank(entry.id, current - 1, progress.optionIndex(entry.id))
    }

    fun branchName(entry: DevelopmentEntry): String = when {
        entry.isAbility -> entry.name
        entry.accessId != null -> catalog.byId(entry.accessId)?.name ?: entry.category.ifBlank { entry.name }
        else -> entry.category.ifBlank { entry.name }
    }

    val filteredEntries = remember(query, tab, availableOnly, character, progress, catalog) {
        val localRules = DevelopmentRules(character, catalog, progress)
        val needle = developmentNormalize(query)
        catalog.entries
            .asSequence()
            .filter { entry -> !entry.incomplete || (tab == DevelopmentTab.OWNED && progress.rank(entry.id) > 0) }
            .filterNot { it.id == MagicEquipmentRules.BASE_MANA_ENTRY_ID }
            .filter { entry ->
                when (tab) {
                    DevelopmentTab.REGULAR -> entry.isRegularDevelopment
                    DevelopmentTab.SPECIAL -> entry.isSpecialDevelopment
                    DevelopmentTab.MARTIAL_ARTS -> entry.isMartialArt
                    DevelopmentTab.CHI -> entry.isChiDevelopment
                    DevelopmentTab.OWNED -> progress.rank(entry.id) > 0
                }
            }
            .filter { entry ->
                if (needle.isBlank()) true else developmentNormalize(
                    listOf(
                        entry.name,
                        branchName(entry),
                        entry.category,
                        entry.section,
                        entry.requirements,
                        entry.benefit,
                        entry.notes,
                        entry.tags.joinToString(" "),
                    ).joinToString(" ")
                ).contains(needle)
            }
            .filter { entry ->
                tab == DevelopmentTab.OWNED || !availableOnly || localRules.availability(entry).canIncrease
            }
            .sortedWith(
                compareBy<DevelopmentEntry>(
                    { if (tab == DevelopmentTab.SPECIAL || (tab == DevelopmentTab.OWNED && it.isSpecialDevelopment)) developmentNormalize(branchName(it)) else developmentNormalize(it.category) },
                    { if (it.isAbility) 0 else 1 },
                    { developmentNormalize(it.name) },
                )
            )
            .toList()
    }
    val filteredChiTechniques = remember(query, availableOnly, character, chiCatalog, catalog) {
        val needle = developmentNormalize(query)
        chiCatalog.techniques.filter { technique ->
            val matches = needle.isBlank() || developmentNormalize(
                listOf(technique.name, technique.school, technique.action, technique.effect, technique.requirements).joinToString(" ")
            ).contains(needle)
            matches && (!availableOnly || chiRules.availability(technique).unlocked)
        }.sortedWith(compareBy<ChiTechnique>({ developmentNormalize(it.school) }, { developmentNormalize(it.name) }))
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { Spacer(Modifier.height(8.dp)) }
        item {
            DublScreenHeader(
                title = "Навыки",
                subtitle = "Развитие, боевые искусства, ЦИ и спец. ветки",
            )
        }

        item {
            DevelopmentBudgetCard(economy)
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(DevelopmentTab.entries) { target ->
                    FilterChip(
                        selected = tab == target,
                        onClick = {
                            tab = target
                            query = ""
                            availableOnly = false
                        },
                        label = { Text(target.title) },
                    )
                }
            }
        }

        if (tab == DevelopmentTab.SPECIAL) {
            item {
                DublCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Как работают спец. ветки",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Если у ветки есть доступ за ОС, сначала откройте его. Затем навыки этой ветки покупаются отдельно за XP и проверяют свои требования.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (tab == DevelopmentTab.CHI) {
            val automaticAccess = character.chiAutomaticAccess
            val progressionBonus = character.chiProgressionBonus
            item {
                ChiDevelopmentCard(
                    enabled = character.chiActive,
                    automaticAccess = automaticAccess,
                    current = character.chiCurrent,
                    maximum = character.chiMaximum,
                    baseMaximum = character.chiBaseMaximum,
                    bonusRanks = character.chiBonusRanks,
                    progressionBonus = progressionBonus,
                    onToggle = controller::setChiEnabled,
                    onChangeCurrent = controller::changeChi,
                    onChangeBonusRanks = { delta -> controller.setChiBonusRanks(character.chiBonusRanks + delta) },
                    onRestore = controller::restoreChi,
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Поиск развития или приёма ЦИ") },
                    singleLine = true,
                )
            }
            item {
                FilterChip(
                    selected = availableOnly,
                    onClick = { availableOnly = !availableOnly },
                    label = { Text("Доступно сейчас") },
                )
            }
            item {
                Text(
                    "Развитие: ${filteredEntries.size} · Приёмы: ${filteredChiTechniques.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            filteredEntries.groupBy { it.category.ifBlank { "Развитие ЦИ" } }.forEach { (category, entries) ->
                item(key = "chi-development-header-$category") {
                    DevelopmentGroupHeader(category, entries.size)
                }
                items(entries, key = { "chi-development-${it.id}" }) { entry ->
                    DevelopmentRow(
                        entry = entry,
                        rules = rules,
                        progress = progress,
                        onClick = { selectedEntryId = entry.id },
                    )
                }
            }
            filteredChiTechniques.groupBy { it.school }.forEach { (school, techniques) ->
                item(key = "chi-technique-header-$school") {
                    DevelopmentGroupHeader(school, techniques.size, trailing = "Приёмы")
                }
                items(techniques, key = { "chi-technique-${it.id}" }) { technique ->
                    ChiTechniqueCard(
                        technique = technique,
                        availability = chiRules.availability(technique),
                        onUse = { controller.changeChi(-technique.chiCost) },
                    )
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            when (tab) {
                                DevelopmentTab.REGULAR -> "Поиск обычного навыка"
                                DevelopmentTab.SPECIAL -> "Поиск спец. ветки или навыка"
                                DevelopmentTab.MARTIAL_ARTS -> "Поиск стиля или приёма"
                                DevelopmentTab.CHI -> ""
                                DevelopmentTab.OWNED -> "Поиск среди взятых"
                            }
                        )
                    },
                    singleLine = true,
                )
            }

            if (tab != DevelopmentTab.OWNED) {
                item {
                    FilterChip(
                        selected = availableOnly,
                        onClick = { availableOnly = !availableOnly },
                        label = { Text("Доступно сейчас") },
                    )
                }
            }

            item {
                Text(
                    when (tab) {
                        DevelopmentTab.REGULAR -> "Обычных навыков: ${filteredEntries.size}"
                        DevelopmentTab.SPECIAL -> "Записей спец. веток: ${filteredEntries.size}"
                        DevelopmentTab.MARTIAL_ARTS -> "Стилей и приёмов: ${filteredEntries.size}"
                        DevelopmentTab.CHI -> ""
                        DevelopmentTab.OWNED -> "Взято: ${filteredEntries.size}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (filteredEntries.isEmpty()) {
                item {
                    DublCard(Modifier.fillMaxWidth()) {
                        Text(
                            if (tab == DevelopmentTab.OWNED) "Пока ничего не взято" else "Ничего не найдено",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (tab == DevelopmentTab.OWNED) {
                                "Полученные навыки, боевые искусства и открытые спец. ветки появятся здесь вместе с описаниями."
                            } else {
                                "Сбросьте поиск или фильтр доступности."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (query.isNotBlank() || availableOnly) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    query = ""
                                    availableOnly = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { Text("Сбросить фильтры") }
                        }
                    }
                }
            } else when (tab) {
                DevelopmentTab.REGULAR -> {
                val grouped = filteredEntries.groupBy { it.category.ifBlank { "Общие" } }
                grouped.forEach { (category, entries) ->
                    item(key = "regular-header-$category") {
                        DevelopmentGroupHeader(category, entries.size)
                    }
                    items(entries, key = { it.id }) { entry ->
                        DevelopmentRow(
                            entry = entry,
                            rules = rules,
                            progress = progress,
                            onClick = { selectedEntryId = entry.id },
                        )
                    }
                }
            }

                DevelopmentTab.SPECIAL -> {
                val grouped = filteredEntries.groupBy(::branchName)
                grouped.forEach { (branch, branchEntries) ->
                    val access = branchEntries.firstOrNull { it.isAbility }
                        ?: catalog.entries.firstOrNull { it.isAbility && developmentNormalize(it.name) == developmentNormalize(branch) }
                    item(key = "special-header-$branch") {
                        SpecialBranchHeader(
                            title = branch,
                            entriesCount = branchEntries.count { !it.isAbility },
                            access = access,
                            progress = progress,
                            rules = rules,
                        )
                    }
                    items(branchEntries.sortedWith(compareBy<DevelopmentEntry>({ if (it.isAbility) 0 else 1 }, { developmentNormalize(it.name) })), key = { it.id }) { entry ->
                        DevelopmentRow(
                            entry = entry,
                            rules = rules,
                            progress = progress,
                            onClick = { selectedEntryId = entry.id },
                        )
                    }
                }
            }

                DevelopmentTab.MARTIAL_ARTS -> {
                    val grouped = filteredEntries.groupBy { it.category.ifBlank { "Боевые искусства" } }
                    grouped.forEach { (category, entries) ->
                        item(key = "martial-header-$category") {
                            DevelopmentGroupHeader(category, entries.size)
                        }
                        items(entries, key = { it.id }) { entry ->
                            DevelopmentRow(
                                entry = entry,
                                rules = rules,
                                progress = progress,
                                onClick = { selectedEntryId = entry.id },
                            )
                        }
                    }
                }

                DevelopmentTab.CHI -> Unit

                DevelopmentTab.OWNED -> {
                    val regularOwned = filteredEntries.filter { it.isRegularDevelopment }
                    val martialOwned = filteredEntries.filter { it.isMartialArt }
                    val specialOwned = filteredEntries.filter { it.isSpecialDevelopment }
                    val chiOwned = filteredEntries.filter { it.isChiDevelopment }

                if (regularOwned.isNotEmpty()) {
                    item(key = "owned-regular-header") {
                        DevelopmentGroupHeader("Обычные навыки", regularOwned.size)
                    }
                    items(regularOwned, key = { "owned-${it.id}" }) { entry ->
                        OwnedDevelopmentRow(
                            entry = entry,
                            progress = progress,
                            rules = rules,
                            onClick = { selectedEntryId = entry.id },
                        )
                    }
                }

                if (martialOwned.isNotEmpty()) {
                    item(key = "owned-martial-header") {
                        DevelopmentGroupHeader("Боевые искусства", martialOwned.size)
                    }
                    items(martialOwned, key = { "owned-${it.id}" }) { entry ->
                        OwnedDevelopmentRow(
                            entry = entry,
                            progress = progress,
                            rules = rules,
                            onClick = { selectedEntryId = entry.id },
                        )
                    }
                }

                if (chiOwned.isNotEmpty()) {
                    item(key = "owned-chi-header") {
                        DevelopmentGroupHeader("ЦИ", chiOwned.size)
                    }
                    items(chiOwned, key = { "owned-${it.id}" }) { entry ->
                        OwnedDevelopmentRow(
                            entry = entry,
                            progress = progress,
                            rules = rules,
                            onClick = { selectedEntryId = entry.id },
                        )
                    }
                }

                specialOwned.groupBy(::branchName).forEach { (branch, entries) ->
                    item(key = "owned-special-header-$branch") {
                        DevelopmentGroupHeader(branch, entries.size, trailing = "Спец. ветка")
                    }
                    items(entries.sortedWith(compareBy<DevelopmentEntry>({ if (it.isAbility) 0 else 1 }, { developmentNormalize(it.name) })), key = { "owned-${it.id}" }) { entry ->
                        OwnedDevelopmentRow(
                            entry = entry,
                            progress = progress,
                            rules = rules,
                            onClick = { selectedEntryId = entry.id },
                        )
                    }
                }
            }
        }
        }
    }

    selectedEntryId?.let { id ->
        catalog.byId(id)?.let { entry ->
            DevelopmentDetailSheet(
                entry = entry,
                catalog = catalog,
                progress = progress,
                rules = rules,
                onOpenEntry = { targetId -> selectedEntryId = targetId },
                onIncrease = { optionIndex -> increase(entry, optionIndex) },
                onDecrease = { decrease(entry) },
                onDismiss = { selectedEntryId = null },
            )
        } ?: run { selectedEntryId = null }
    }

    pendingRequirementOverride?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingRequirementOverride = null },
            title = { Text("Требования не выполнены") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        if (pending.entry.isAbility) {
                            "Открыть «${pending.entry.name}» несмотря на невыполненные требования?"
                        } else {
                            "Добавить «${pending.entry.name}» несмотря на невыполненные требования?"
                        },
                    )
                    pending.failedChecks.forEach { check ->
                        Text(
                            "• ${check.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when (check.status) {
                                RequirementStatus.FAIL -> DublDanger
                                RequirementStatus.MANUAL -> DublGold
                                RequirementStatus.OK -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                    Text(
                        "После добавления запись останется помеченной «⚠ Требования», пока условия реально не будут выполнены.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (pending.entry.isAbility) {
                            "Будет потрачено ${rules.abilityCost(pending.entry, pending.optionIndex)} ОС."
                        } else {
                            "Будет учтено ${pending.entry.cost} XP за следующий ранг."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = DublGold,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = progress.rank(pending.entry.id)
                        controller.setDevelopmentRank(
                            pending.entry.id,
                            current + 1,
                            pending.optionIndex,
                        )
                        pendingRequirementOverride = null
                    },
                ) {
                    Text(if (pending.entry.isAbility) "Открыть всё равно" else "Добавить всё равно")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRequirementOverride = null }) { Text("Отмена") }
            },
        )
    }

    pendingAbilityPurchase?.let { pending ->
        val cost = rules.abilityCost(pending.entry, pending.optionIndex)
        AlertDialog(
            onDismissRequest = { pendingAbilityPurchase = null },
            title = { Text("Открыть спец. ветку?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(pending.entry.name, fontWeight = FontWeight.Bold)
                    pending.entry.abilityOptions.getOrNull(pending.optionIndex)?.let { option ->
                        Text("Источник: ${option.source}")
                    }
                    Text("Будет потрачено $cost ОС. Навыки внутри ветки покупаются отдельно за XP.")
                    if (character.creationComplete) {
                        Text(
                            "Создание уже завершено. Книга описывает покупку способностей за ОС при создании; продолжайте только по решению мастера.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DublGold,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = progress.rank(pending.entry.id)
                        controller.setDevelopmentRank(pending.entry.id, current + 1, pending.optionIndex)
                        pendingAbilityPurchase = null
                    },
                ) { Text("Открыть · $cost ОС") }
            },
            dismissButton = {
                TextButton(onClick = { pendingAbilityPurchase = null }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun DevelopmentBudgetCard(economy: CharacterEconomyBreakdown) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = DublGold.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, DublGold.copy(alpha = 0.24f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Опыт",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${economy.spentXp} XP потрачено",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (economy.overspentXp) DublDanger else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "${economy.remainingXp} осталось из ${economy.totalExperience}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (economy.overspentXp) DublDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        "Очки способностей",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${economy.abilityPointsSpent} / ${economy.abilityPointsBudget} ОС",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (economy.overspentAbilityPoints) DublDanger else DublGold,
                    )
                    Text(
                        "${economy.abilityPointsRemaining} доступно",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (economy.overspentAbilityPoints) DublDanger else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(
                "Характеристики ${economy.attributeXp} · Умения ${economy.skillXp} · Навыки ${economy.developmentXp} · ЦИ ${economy.chiXp} · Магия ${economy.manaXp + economy.magicSchoolXp + economy.spellXp}" +
                    if (economy.adjustmentXp != 0) " · Поправка ${economy.adjustmentXp}" else "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ChiDevelopmentCard(
    enabled: Boolean,
    automaticAccess: Boolean,
    current: Int,
    maximum: Int,
    baseMaximum: Int,
    bonusRanks: Int,
    progressionBonus: Int,
    onToggle: (Boolean) -> Unit,
    onChangeCurrent: (Int) -> Unit,
    onChangeBonusRanks: (Int) -> Unit,
    onRestore: () -> Unit,
) {
    DublCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("ЦИ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Внутренняя энергия для боевых приёмов",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DublSwitch(
                checked = enabled,
                onCheckedChange = if (automaticAccess) null else onToggle,
                enabled = !automaticAccess,
            )
        }

        if (automaticAccess) {
            Text(
                "Ресурс открыт способностью «Внутренняя ЦИ» и остаётся активным, пока способность изучена.",
                style = MaterialTheme.typography.bodySmall,
                color = DublAccent,
            )
        }

        if (!enabled) {
            Text(
                "Включите ЦИ, если персонаж освоил доступ к этому ресурсу. Сам переключатель не расходует XP и не выдаёт способности автоматически.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@DublCard
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DublAccent.copy(alpha = 0.055f),
            border = BorderStroke(1.dp, DublAccent.copy(alpha = 0.24f)),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text("Текущий запас", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$current / $maximum", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(onClick = { onChangeCurrent(-1) }, enabled = current > 0) { Text("−1") }
                        OutlinedButton(onClick = { onChangeCurrent(1) }, enabled = current < maximum) { Text("+1") }
                    }
                }
                OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth(), enabled = current < maximum) {
                    Text("Восстановить полностью")
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Дополнительный запас ЦИ", fontWeight = FontWeight.SemiBold)
                Text(
                    "$bonusRanks / 10 рангов · ${CharacterEconomy.CHI_BONUS_RANK_XP} XP за ранг",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { onChangeBonusRanks(-1) }, enabled = bonusRanks > 0) { Text("−") }
                OutlinedButton(onClick = { onChangeBonusRanks(1) }, enabled = bonusRanks < 10) { Text("+") }
            }
        }

        Text(
            "Максимум: база $baseMaximum + купленный запас $bonusRanks + развитие $progressionBonus = $maximum.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "По правилам запас полностью восстанавливается после 15 минут медитации/лёгкой активности или после 8 часов отдыха.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ChiTechniqueCard(
    technique: ChiTechnique,
    availability: com.dubl.character.android.model.ChiTechniqueAvailability,
    onUse: () -> Unit,
) {
    val unlocked = availability.unlocked
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        color = if (unlocked) DublAccent.copy(alpha = 0.035f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, if (unlocked) DublAccent.copy(alpha = 0.20f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(technique.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(technique.action, "${technique.chiCost} ЦИ").filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (unlocked) DublAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (unlocked) "Открыт" else "Закрыт",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (unlocked) DublAccent else DublGold,
                )
            }
            if (technique.effect.isNotBlank()) {
                Text(technique.effect, style = MaterialTheme.typography.bodySmall)
            }
            if (!unlocked) {
                Text(
                    "Требуется: ${technique.requirements}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DublGold,
                )
            } else if (!availability.canUse && availability.reason.isNotBlank()) {
                Text(availability.reason, style = MaterialTheme.typography.bodySmall, color = DublDanger)
            }
            OutlinedButton(
                onClick = onUse,
                enabled = availability.canUse,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (technique.chiCost > 0) "Использовать · ${technique.chiCost} ЦИ" else "Использовать · без затрат ЦИ")
            }
        }
    }
}

@Composable
private fun DevelopmentGroupHeader(
    title: String,
    count: Int,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 11.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            trailing?.let { "$it · $count" } ?: count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun SpecialBranchHeader(
    title: String,
    entriesCount: Int,
    access: DevelopmentEntry?,
    progress: DevelopmentProgress,
    rules: DevelopmentRules,
) {
    val accessRank = access?.let { progress.rank(it.id) } ?: 0
    val accessLabel = when {
        access == null -> "XP-ветка"
        accessRank > 0 -> "Доступ открыт"
        access.abilityOptions.isNotEmpty() -> {
            val costs = access.abilityOptions.map { it.value }.distinct().sorted()
            val value = if (costs.size == 1) costs.first().toString() else "${costs.first()}–${costs.last()}"
            "$value ОС для доступа"
        }
        else -> "${rules.abilityCost(access, 0)} ОС для доступа"
    }
    val accent = when {
        access == null -> DublAccent
        accessRank > 0 -> DublGold
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 11.dp, bottom = 2.dp),
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    accessLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "$entriesCount XP-навык${if (entriesCount == 1) "" else "ов"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OwnedDevelopmentRow(
    entry: DevelopmentEntry,
    progress: DevelopmentProgress,
    rules: DevelopmentRules,
    onClick: () -> Unit,
) {
    val rank = progress.rank(entry.id)
    val ownedAvailability = remember(entry.id, rules) { rules.availability(entry) }
    val invalidOwned = ownedAvailability.checks.any { it.status != RequirementStatus.OK }
    val accent = when {
        invalidOwned -> DublDanger
        entry.isSpecialDevelopment -> DublGold
        else -> DublAccent
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.035f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        when {
                            entry.isAbility -> "Открытая спец. ветка"
                            entry.isSpecialDevelopment -> "Спец. навык · ${entry.category}"
                            else -> "Обычный навык · ${entry.category.ifBlank { "Общие" }}"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(10.dp))
                DevelopmentStatusPill(
                    status = when {
                        invalidOwned -> "⚠ Требования"
                        entry.maxRank > 1 -> "Ранг $rank/${entry.maxRank}"
                        else -> "Взято"
                    },
                    accent = accent,
                )
            }
            if (invalidOwned) {
                Text(
                    "Текущие требования не выполнены",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DublDanger,
                )
            }
            val description = entry.benefit.ifBlank { entry.notes }
            if (description.isNotBlank()) {
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DevelopmentRow(
    entry: DevelopmentEntry,
    rules: DevelopmentRules,
    progress: DevelopmentProgress,
    onClick: () -> Unit,
) {
    val availability = remember(entry.id, rules) { rules.availability(entry) }
    val rank = progress.rank(entry.id)
    val owned = rank > 0
    val invalidOwned = owned && availability.checks.any { it.status != RequirementStatus.OK }
    val accent = when {
        invalidOwned -> DublDanger
        owned -> DublGold
        availability.canIncrease -> DublAccent
        else -> MaterialTheme.colorScheme.outline
    }
    val status = when {
        invalidOwned -> "⚠ Требования"
        owned && entry.isAbility -> "✓ Открыта"
        owned -> "✓ $rank/${entry.maxRank}"
        availability.canIncrease && entry.isAbility -> "Открыть"
        availability.canIncrease -> "Доступно"
        availability.checks.any { it.status == RequirementStatus.MANUAL } -> "Проверить"
        else -> "Закрыто"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (owned || availability.canIncrease) accent.copy(alpha = 0.035f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accent.copy(alpha = if (owned || availability.canIncrease) 0.42f else 0.20f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier
                    .width(3.dp)
                    .height(38.dp),
                shape = RoundedCornerShape(999.dp),
                color = accent.copy(alpha = if (owned || availability.canIncrease) 0.9f else 0.34f),
            ) {}
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (owned || availability.canIncrease) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    buildString {
                        append(
                            when {
                                entry.isAbility -> "Доступ к спец. ветке"
                                entry.isSpecialDevelopment -> "Спец. навык"
                                else -> "Навык"
                            }
                        )
                        append(" · ")
                        if (entry.isAbility) {
                            if (entry.abilityOptions.isNotEmpty()) {
                                val values = entry.abilityOptions.map { it.value }.distinct().sorted()
                                append(values.joinToString("–"))
                                append(" ОС")
                            } else {
                                append(entry.cost).append(" ОС")
                            }
                        } else {
                            append(entry.cost).append(" опыта")
                        }
                        if (entry.maxRank > 1) append(" · до ${entry.maxRank} ранга")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry.isSpecialDevelopment && !entry.isAbility) {
                    Text(
                        "Ветка: ${entry.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            DevelopmentStatusPill(status = status, accent = accent)
        }
    }
}


@Composable
private fun DevelopmentStatusPill(
    status: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.26f)),
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            color = accent,
            maxLines = 1,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DevelopmentDetailSheet(
    entry: DevelopmentEntry,
    catalog: DevelopmentCatalog,
    progress: DevelopmentProgress,
    rules: DevelopmentRules,
    onOpenEntry: (String) -> Unit,
    onIncrease: (Int) -> Unit,
    onDecrease: () -> Unit,
    onDismiss: () -> Unit,
) {
    val currentRank = progress.rank(entry.id)
    var optionIndex by remember(entry.id, currentRank) {
        mutableStateOf(
            if (currentRank > 0) progress.optionIndex(entry.id)
            else 0
        )
    }
    val availability = rules.availability(entry, optionIndex)
    val children = catalog.childrenOf(entry.id)
    val ownedInvalid = currentRank > 0 && availability.checks.any { it.status != RequirementStatus.OK }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 690.dp)
                .containSheetOverscroll()
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 18.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${entry.section} · ${entry.category}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (currentRank > 0) {
                    Text(
                        "Ранг $currentRank/${entry.maxRank}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (ownedInvalid) DublDanger else DublGold,
                    )
                }
            }

            if (entry.tags.isNotEmpty()) {
                Text(
                    entry.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = DublGold,
                )
            }

            if (entry.benefit.isNotBlank()) {
                DetailBlock("Эффект", entry.benefit)
            }
            if (entry.notes.isNotBlank()) {
                DetailBlock("Особое", entry.notes)
            }
            if (entry.conflictNote.isNotBlank()) {
                DetailBlock("Расхождение книги", entry.conflictNote)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Text("Требования", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            availability.checks.forEach { check ->
                RequirementRow(check = check, onOpenEntry = onOpenEntry)
            }

            if (ownedInvalid) {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = DublDanger.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, DublDanger.copy(alpha = 0.35f)),
                ) {
                    Text(
                        "Запись уже получена, но текущие требования не выполнены. Она не удаляется автоматически и останется помеченной, пока условия не будут соблюдены.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = DublDanger,
                    )
                }
            }

            if (children.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                Text(
                    "Открывает ${children.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                children.take(8).forEach { child ->
                    TextButton(onClick = { onOpenEntry(child.id) }) {
                        Text(child.name, modifier = Modifier.fillMaxWidth())
                    }
                }
                if (children.size > 8) {
                    Text(
                        "И ещё ${children.size - 8} записей в ветке",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

            if (entry.isAbility && entry.abilityOptions.isNotEmpty()) {
                Text("Источник способности", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(entry.abilityOptions.indices.toList()) { index ->
                        val option = entry.abilityOptions[index]
                        FilterChip(
                            selected = optionIndex == index,
                            enabled = currentRank == 0,
                            onClick = { optionIndex = index },
                            label = { Text("${option.source} · ${option.value} ОС") },
                        )
                    }
                }
            }

            Text(
                if (entry.isAbility) {
                    val cost = rules.abilityCost(entry, optionIndex)
                    "Доступ к ветке: $cost ОС · доступно ${rules.abilityPointsAvailable()} ОС"
                } else {
                    "Стоимость следующего ранга: ${entry.cost} опыта"
                },
                style = MaterialTheme.typography.labelLarge,
                color = DublGold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (currentRank > 0) {
                    OutlinedButton(
                        onClick = onDecrease,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            when {
                                entry.isAbility -> "Закрыть доступ"
                                currentRank == 1 -> "Убрать"
                                else -> "− ранг"
                            }
                        )
                    }
                }
                Button(
                    onClick = { onIncrease(optionIndex) },
                    enabled = availability.canIncrease || availability.canForceIncrease,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            availability.canIncrease && entry.isAbility -> "Открыть · ${availability.abilityCost} ОС"
                            availability.canIncrease && currentRank > 0 -> "+ ранг · ${entry.cost} XP"
                            availability.canIncrease -> "Получить · ${entry.cost} XP"
                            availability.canForceIncrease && entry.isAbility -> "Открыть всё равно"
                            availability.canForceIncrease -> "Добавить всё равно"
                            else -> availability.reason
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RequirementRow(
    check: RequirementCheck,
    onOpenEntry: (String) -> Unit,
) {
    val color = when (check.status) {
        RequirementStatus.OK -> Color(0xFF71A492)
        RequirementStatus.FAIL -> DublDanger
        RequirementStatus.MANUAL -> DublGold
    }
    val prefix = when (check.status) {
        RequirementStatus.OK -> "✓"
        RequirementStatus.FAIL -> "✕"
        RequirementStatus.MANUAL -> "?"
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = check.targetEntryId != null) {
                check.targetEntryId?.let(onOpenEntry)
            },
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.055f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(prefix, color = color, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text(
                check.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
            )
            if (check.targetEntryId != null) {
                Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DetailBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(body, style = MaterialTheme.typography.bodyMedium)
    }
}
