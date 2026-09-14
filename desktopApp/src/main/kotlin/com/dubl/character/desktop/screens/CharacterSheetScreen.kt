package com.dubl.character.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.CharacterEconomy
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.CharacterSheetResourceId
import com.dubl.character.android.model.CustomResource
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.DevelopmentProgress
import com.dubl.character.android.model.DevelopmentRules
import com.dubl.character.android.model.DevelopmentSheetSectionType
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.RollContext
import com.dubl.character.android.model.ResolvedSkill
import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.model.SheetGroupingRules
import com.dubl.character.android.model.SkillCategory
import com.dubl.character.android.model.resolvedSkills
import com.dubl.character.android.model.skillCalculationForRoll
import com.dubl.character.android.ui.theme.DublCustomResource
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublGold
import com.dubl.character.android.ui.theme.DublHealth
import com.dubl.character.android.ui.theme.DublMana
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.android.ui.theme.DublStamina
import com.dubl.character.desktop.DesktopAppState
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlinx.coroutines.launch

private sealed interface SheetUndo {
    data class Resource(val resource: CharacterSheetResourceId, val delta: Int) : SheetUndo
    data class Attribute(val id: AttributeId, val delta: Int) : SheetUndo
    data class Conditions(val previous: Set<CharacterConditionId>) : SheetUndo
    data class Identity(val previous: DublCharacter) : SheetUndo
}
private data class RecentSheetChange(val text: String, val undo: SheetUndo? = null)
private data class ContextRollRequest(val context: RollContext, val attribute: AttributeId? = null)
private enum class GroupingKind(val title: String) { SKILLS("Умения"), DEVELOPMENT("Навыки") }

@Composable
fun CharacterSheetScreen(
    state: DesktopAppState,
    modifier: Modifier = Modifier,
    onNavigateSkills: () -> Unit = {},
    onNavigateDevelopment: () -> Unit = {},
    onNavigateMagic: () -> Unit = {},
    onNavigateEquipment: () -> Unit = {},
) {
    val character = state.session.active
    val extras = state.extras
    val economy = CharacterEconomy.breakdown(character, state.developmentCatalog)
    var showIdentity by remember(character.id) { mutableStateOf(false) }
    var showEconomy by remember(character.id) { mutableStateOf(false) }
    var showConditions by remember(character.id) { mutableStateOf(false) }
    var showVisibility by remember(character.id) { mutableStateOf(false) }
    var showHealthControl by remember(character.id) { mutableStateOf(false) }
    var customResource by remember(character.id) { mutableStateOf<CustomResource?>(null) }
    var createResource by remember(character.id) { mutableStateOf(false) }
    var maximumResource by remember(character.id) { mutableStateOf<CharacterSheetResourceId?>(null) }
    var rollRequest by remember(character.id) { mutableStateOf<ContextRollRequest?>(null) }
    var sheetRollSkill by remember(character.id) { mutableStateOf<ResolvedSkill?>(null) }
    var sheetDevelopmentEntry by remember(character.id) { mutableStateOf<DevelopmentEntry?>(null) }
    var grouping by remember(character.id) { mutableStateOf<GroupingKind?>(null) }
    var recent by remember(character.id) { mutableStateOf<RecentSheetChange?>(null) }

    fun changeResource(resource: CharacterSheetResourceId, delta: Int) {
        val before = when (resource) {
            CharacterSheetResourceId.HEALTH -> character.hpCurrent
            CharacterSheetResourceId.ENDURANCE -> character.enduranceCurrent
            CharacterSheetResourceId.MANA -> character.manaCurrent
            CharacterSheetResourceId.CHI -> character.chiCurrent
        }
        state.mutate {
            when (resource) {
                CharacterSheetResourceId.HEALTH -> changeHp(delta)
                CharacterSheetResourceId.ENDURANCE -> changeEndurance(delta)
                CharacterSheetResourceId.MANA -> changeMana(delta)
                CharacterSheetResourceId.CHI -> changeChi(delta)
            }
        }
        val after = when (resource) {
            CharacterSheetResourceId.HEALTH -> state.session.active.hpCurrent
            CharacterSheetResourceId.ENDURANCE -> state.session.active.enduranceCurrent
            CharacterSheetResourceId.MANA -> state.session.active.manaCurrent
            CharacterSheetResourceId.CHI -> state.session.active.chiCurrent
        }
        val applied = after - before
        if (applied != 0) recent = RecentSheetChange("${resource.title} ${signed(applied)}", SheetUndo.Resource(resource, applied))
    }

    fun undoRecent() {
        when (val undo = recent?.undo) {
            is SheetUndo.Resource -> state.mutate {
                when (undo.resource) {
                    CharacterSheetResourceId.HEALTH -> changeHp(-undo.delta)
                    CharacterSheetResourceId.ENDURANCE -> changeEndurance(-undo.delta)
                    CharacterSheetResourceId.MANA -> changeMana(-undo.delta)
                    CharacterSheetResourceId.CHI -> changeChi(-undo.delta)
                }
            }
            is SheetUndo.Attribute -> state.mutate { changeAttribute(undo.id, -undo.delta) }
            is SheetUndo.Conditions -> state.updateExtras {
                val current = load(character.id).activeConditions
                current.filterNot { it in undo.previous }.forEach { toggleCondition(character.id, it) }
                undo.previous.filterNot { it in current }.forEach { toggleCondition(character.id, it) }
            }
            is SheetUndo.Identity -> state.mutate { updateActive { undo.previous } }
            null -> Unit
        }
        recent = null
    }

    val effectiveConditions = buildSet {
        addAll(extras.activeConditions)
        if (character.enduranceCurrent == 0) add(CharacterConditionId.WEAKNESS)
    }

    LazyColumn(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            if (recent != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(recent!!.text)
                        if (recent!!.undo != null) TextButton(onClick = ::undoRecent) { Text("Отменить") }
                    }
                }
            }
        }
        item {
            SectionCard(
                title = character.name,
                action = { OutlinedButton(onClick = { showIdentity = true }) { Text("Редактировать") } },
            ) {
                Text(character.concept.ifBlank { "Без концепта" }, color = DublMuted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("XP ${character.experience}", color = DublGold, fontWeight = FontWeight.Bold)
                    Text("Осталось ${economy.remainingXp}", color = if (economy.overspentXp) MaterialTheme.colorScheme.error else DublFocus)
                    Text("ОС ${economy.abilityPointsRemaining}/${economy.abilityPointsBudget}", color = DublFocus)
                    TextButton(onClick = { showEconomy = true }) { Text("Экономика") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Размер ${character.size}")
                    Text("Ног ${character.legs}")
                    Text(if (character.creationComplete) "Создание завершено" else "Режим создания", color = DublMuted)
                }
                extras.portraitUri?.let { portraitPath ->
                    PortraitImage(portraitPath)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val file = pickPortraitFile()
                        if (file != null) state.importPortrait(file)?.let { imported -> state.updateExtras { setPortrait(character.id, imported) } }
                    }) { Text(if (extras.portraitUri == null) "Добавить портрет" else "Сменить портрет") }
                    extras.portraitUri?.let {
                        Text(it.substringAfterLast('/'), color = DublMuted)
                        TextButton(onClick = { state.updateExtras { setPortrait(character.id, null) } }) { Text("Убрать") }
                    }
                }
            }
        }

        item {
            SectionCard("Ресурсы", action = { TextButton(onClick = { showVisibility = true }) { Text("Видимость") } }) {
                if (CharacterSheetResourceId.HEALTH !in extras.hiddenResourceIds) ResourceRow("Здоровье", character.hpCurrent, character.healthMaximum, DublHealth, { changeResource(CharacterSheetResourceId.HEALTH, -1) }, { changeResource(CharacterSheetResourceId.HEALTH, 1) }, { showHealthControl = true }, "Контроль")
                if (CharacterSheetResourceId.ENDURANCE !in extras.hiddenResourceIds) ResourceRow("Выносливость", character.enduranceCurrent, character.enduranceMaximum, DublStamina, { changeResource(CharacterSheetResourceId.ENDURANCE, -1) }, { changeResource(CharacterSheetResourceId.ENDURANCE, 1) }, { maximumResource = CharacterSheetResourceId.ENDURANCE })
                if ((character.manaEnabled || character.effectiveManaMaximum > 0) && CharacterSheetResourceId.MANA !in extras.hiddenResourceIds) ResourceRow("Мана", character.manaCurrent, character.effectiveManaMaximum, DublMana, { changeResource(CharacterSheetResourceId.MANA, -1) }, { changeResource(CharacterSheetResourceId.MANA, 1) }, { maximumResource = CharacterSheetResourceId.MANA })
                if (character.chiActive && CharacterSheetResourceId.CHI !in extras.hiddenResourceIds) ResourceRow("ЦИ", character.chiCurrent, character.chiMaximum, DublFocus, { changeResource(CharacterSheetResourceId.CHI, -1) }, { changeResource(CharacterSheetResourceId.CHI, 1) }, { state.mutate { restoreChi() } }, "Полностью")
                character.customResources.forEach { resource ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(resource.name, fontWeight = FontWeight.SemiBold)
                            Text("${resource.current} / ${resource.maximum}", color = DublCustomResource)
                        }
                        OutlinedButton(onClick = { state.mutate { changeCustomResource(resource.uid, -1) } }) { Text("−") }
                        Button(onClick = { state.mutate { changeCustomResource(resource.uid, 1) } }) { Text("+") }
                        TextButton(onClick = { customResource = resource }) { Text("Изменить") }
                    }
                }
                Button(onClick = { createResource = true }) { Text("+ Свой ресурс") }
            }
        }

        item {
            SectionCard("Состояния", action = { OutlinedButton(onClick = { showConditions = true }) { Text("Изменить") } }) {
                if (effectiveConditions.isEmpty()) EmptyState("Активных состояний нет.")
                effectiveConditions.forEach { condition ->
                    Text("• ${condition.title}${if (condition == CharacterConditionId.WEAKNESS && condition !in extras.activeConditions) " · автоматически" else ""}")
                    Text(condition.rulesSummary, color = DublMuted)
                }
            }
        }

        item {
            SectionCard("Характеристики") {
                AttributeId.entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { id ->
                            val raw = character.attributes.getValue(id).base
                            val effective = character.attribute(id)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            ) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(id.title, color = DublMuted)
                                    Text(effective.toString(), color = DublFocus, fontWeight = FontWeight.Bold)
                                    if (raw != effective) Text("База $raw", color = DublMuted)
                                    Text("↑ ${CharacterEconomy.nextAttributeCost(raw)?.let { "$it XP" } ?: "макс."} · ↓ ${CharacterEconomy.previousAttributeRefund(raw)?.let { "$it XP" } ?: "мин."}", color = DublMuted)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { state.mutate { changeAttribute(id, -1) }; recent = RecentSheetChange("${id.title} −1", SheetUndo.Attribute(id, -1)) }, modifier = Modifier.weight(1f)) { Text("−") }
                                        Button(onClick = { state.mutate { changeAttribute(id, 1) }; recent = RecentSheetChange("${id.title} +1", SheetUndo.Attribute(id, 1)) }, modifier = Modifier.weight(1f)) { Text("+") }
                                        TextButton(onClick = { rollRequest = ContextRollRequest(RollContext.ATTRIBUTE, id) }) { Text("Бросок") }
                                    }
                                }
                            }
                        }
                        if (pair.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            SectionCard("Показатели") {
                KeyValue("Защита", character.defense.toString())
                KeyValue("Рефлексы", signed(character.reflexes))
                KeyValue("Инициатива", signed(character.initiative))
                KeyValue("Стойкость", signed(character.fortitude))
                KeyValue("Бег", "${formatNumber(character.runFull)} м")
                KeyValue("Нагрузка", "${formatNumber(character.equipmentLoadPenalty.toDouble())} штраф")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(RollContext.FORTITUDE, RollContext.REFLEXES, RollContext.INITIATIVE, RollContext.DODGE, RollContext.ATTACK, RollContext.PARRY).forEach { context ->
                        OutlinedButton(onClick = { rollRequest = ContextRollRequest(context) }) { Text(context.title) }
                    }
                }
            }
        }

        item {
            val trained = character.resolvedSkills().filter { it.rank > 0 }
            val defaults = defaultSkillGroups(trained)
            val groups = SheetGroupingRules.normalize(extras.skillGroups, defaults, trained.map { it.id }, "skills:ungrouped")
            LaunchedEffect(groups, extras.skillGroups) {
                if (groups != extras.skillGroups) state.updateExtras { setSkillGroups(character.id, groups) }
            }
            val byId = trained.associateBy { it.id }
            SectionCard("Умения · ${trained.size}", action = { TextButton(onClick = { grouping = GroupingKind.SKILLS }) { Text("Группы") } }) {
                if (trained.isEmpty()) EmptyState("Изученные умения появятся здесь.")
                groups.forEach { group ->
                    GroupHeader(group, group.itemIds.size) { state.updateExtras { setSkillGroups(character.id, SheetGroupingRules.toggleCollapsed(groups, group.id)) } }
                    if (!group.collapsed) group.itemIds.mapNotNull(byId::get).forEach { skill ->
                        val preferred = extras.preferredSkillAttributes[skill.id]?.takeIf { it in skill.attributes } ?: skill.attributes.first()
                        val calc = character.skillCalculationForRoll(skill, preferred)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${skill.name} · ${skill.rank} · ${preferred.shortTitle} ${calc.total?.let(::signed) ?: "—"}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { sheetRollSkill = skill }) { Text("Бросок") }
                        }
                    }
                }
            }
        }

        item {
            val rules = DevelopmentRules(character, state.developmentCatalog, DevelopmentProgress(character.development))
            val sections = rules.ownedSheetSections()
            val items = sections.flatMap { it.items }.distinctBy { it.entry.id }
            val parentById = items.associate { it.entry.id to it.parentId }
            val defaults = defaultDevelopmentGroups(character, state)
            val groups = SheetGroupingRules.normalize(extras.developmentGroups, defaults, items.map { it.entry.id }, "development:ungrouped")
            LaunchedEffect(groups, extras.developmentGroups) {
                if (groups != extras.developmentGroups) state.updateExtras { setDevelopmentGroups(character.id, groups) }
            }
            val byId = items.associateBy { it.entry.id }
            SectionCard("Взятые навыки · ${items.size}", action = { TextButton(onClick = { grouping = GroupingKind.DEVELOPMENT }) { Text("Группы") } }) {
                if (items.isEmpty()) EmptyState("Взятых навыков и боевых искусств пока нет.")
                groups.forEach { group ->
                    GroupHeader(group, group.itemIds.size) { state.updateExtras { setDevelopmentGroups(character.id, SheetGroupingRules.toggleCollapsed(groups, group.id)) } }
                    if (!group.collapsed) SheetGroupingRules.hierarchicalOrder(group.itemIds, parentById).mapNotNull(byId::get).forEach { item ->
                        val depth = SheetGroupingRules.localDepth(item.entry.id, group.itemIds, parentById)
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("${"↳ ".repeat(depth.coerceAtMost(3))}${item.entry.name} · ранг ${item.rank}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { sheetDevelopmentEntry = item.entry }) { Text("Подробнее") }
                        }
                    }
                }
            }
        }

        item {
            SectionCard("На листе") {
                Text("Заклинаний: ${character.magic.spells.size} · Предметов: ${character.gear.items.size}", color = DublMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onNavigateSkills) { Text("Умения") }
                    OutlinedButton(onClick = onNavigateDevelopment) { Text("Навыки") }
                    OutlinedButton(onClick = onNavigateMagic) { Text("Магия") }
                    OutlinedButton(onClick = onNavigateEquipment) { Text("Снаряжение") }
                }
            }
        }
    }

    if (showIdentity) IdentityDialog(state, character, onDismiss = { showIdentity = false }) { previous -> recent = RecentSheetChange("Персонаж изменён", SheetUndo.Identity(previous)) }
    if (showEconomy) EconomyDialog(state, onDismiss = { showEconomy = false })
    if (showConditions) ConditionsDialog(state, onDismiss = { showConditions = false }) { previous -> recent = RecentSheetChange("Состояния изменены", SheetUndo.Conditions(previous)) }
    if (showVisibility) ResourceVisibilityDialog(state, onDismiss = { showVisibility = false })
    if (showHealthControl) HealthControlDialog(
        current = state.session.active.hpCurrent,
        maximum = state.session.active.healthMaximum,
        onChange = { delta -> changeResource(CharacterSheetResourceId.HEALTH, delta) },
        onEditMaximum = { showHealthControl = false; maximumResource = CharacterSheetResourceId.HEALTH },
        onDismiss = { showHealthControl = false },
    )
    if (createResource) CustomResourceDialog(state, null, onDismiss = { createResource = false })
    customResource?.let { resource -> CustomResourceDialog(state, resource, onDismiss = { customResource = null }) }
    maximumResource?.let { resource -> MaximumDialog(state, resource, onDismiss = { maximumResource = null }) }
    rollRequest?.let { request -> ContextRollDialog(
        character = state.session.active,
        context = request.context,
        developmentCatalog = state.developmentCatalog,
        effectCatalog = state.skillEffectCatalog,
        initialAttribute = request.attribute,
        onDismiss = { rollRequest = null },
    ) }
    sheetDevelopmentEntry?.let { entry -> DevelopmentDetailsDialog(state, entry, onDismiss = { sheetDevelopmentEntry = null }) }
    sheetRollSkill?.let { skill -> SkillRollDialog(
        character = state.session.active,
        skill = skill,
        preferredAttribute = state.extras.preferredSkillAttributes[skill.id],
        developmentCatalog = state.developmentCatalog,
        effectCatalog = state.skillEffectCatalog,
        onPreferredAttribute = { attribute -> state.updateExtras { setPreferredSkillAttribute(state.session.active.id, skill.id, attribute) } },
        onDismiss = { sheetRollSkill = null },
    ) }
    grouping?.let { kind -> GroupingManagerDialog(state, kind, onDismiss = { grouping = null }) }
}

@Composable
private fun HealthControlDialog(
    current: Int,
    maximum: Int,
    onChange: (Int) -> Unit,
    onEditMaximum: () -> Unit,
    onDismiss: () -> Unit,
) {
    var amountText by remember(current, maximum) { mutableStateOf("1") }
    val amount = amountText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Здоровье · $current / $maximum") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Количество урона / лечения") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = amount > 0 && current > 0,
                        onClick = { onChange(-amount); onDismiss() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Получить урон") }
                    OutlinedButton(
                        enabled = amount > 0 && current < maximum,
                        onClick = { onChange(amount); onDismiss() },
                        modifier = Modifier.weight(1f),
                    ) { Text("Лечение") }
                }
                TextButton(
                    enabled = current < maximum,
                    onClick = { onChange(maximum - current); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Восстановить всё здоровье") }
                TextButton(onClick = onEditMaximum, modifier = Modifier.fillMaxWidth()) { Text("Изменить максимум здоровья") }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun ResourceRow(title: String, current: Int, maximum: Int, tint: Color, minus: () -> Unit, plus: () -> Unit, third: () -> Unit, thirdLabel: String = "Макс.") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text("$current / $maximum", color = tint, fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = minus) { Text("−") }
        Button(onClick = plus) { Text("+") }
        TextButton(onClick = third) { Text(thirdLabel) }
    }
}

@Composable
private fun GroupHeader(group: SheetGroup, count: Int, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("${group.title} · $count", color = DublFocus, fontWeight = FontWeight.Bold)
        TextButton(onClick = onToggle) { Text(if (group.collapsed) "Развернуть" else "Свернуть") }
    }
}

@Composable
private fun IdentityDialog(state: DesktopAppState, character: DublCharacter, onDismiss: () -> Unit, onChanged: (DublCharacter) -> Unit) {
    var name by remember(character.id) { mutableStateOf(character.name) }
    var concept by remember(character.id) { mutableStateOf(character.concept) }
    var size by remember(character.id) { mutableStateOf(character.size.toString()) }
    var legs by remember(character.id) { mutableStateOf(character.legs.toString()) }
    var manaEnabled by remember(character.id) { mutableStateOf(character.manaEnabled) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Персонаж") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Имя") })
                OutlinedTextField(concept, { concept = it }, label = { Text("Концепт") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(size, { size = it.filter(Char::isDigit).take(2) }, label = { Text("Размер") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(legs, { legs = it.filter(Char::isDigit).take(2) }, label = { Text("Ноги") }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Switch(manaEnabled, { manaEnabled = it }); Text("Использовать ману") }
            }
        },
        confirmButton = { TextButton(onClick = {
            val before = character
            state.mutate { updateActive { it.copy(name = name.trim().ifBlank { "Новый персонаж" }, concept = concept.trim(), size = size.toIntOrNull()?.coerceIn(1, 10) ?: 5, legs = legs.toIntOrNull()?.coerceAtLeast(2) ?: 2, manaEnabled = manaEnabled) } }
            onChanged(before); onDismiss()
        }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun EconomyDialog(state: DesktopAppState, onDismiss: () -> Unit) {
    val character = state.session.active
    val economy = CharacterEconomy.breakdown(character, state.developmentCatalog)
    var total by remember(character.id) { mutableStateOf(character.experience.toString()) }
    var creation by remember(character.id) { mutableStateOf(character.effectiveCreationExperience.toString()) }
    var adjustment by remember(character.id) { mutableStateOf(character.xpAdjustment.toString()) }
    var abilityOverride by remember(character.id) { mutableStateOf(character.abilityPointsOverride?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Опыт и создание") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(total, { total = it.filter(Char::isDigit).take(8) }, label = { Text("Общий XP") })
                OutlinedTextField(creation, { creation = it.filter(Char::isDigit).take(8) }, label = { Text("Стартовый XP") })
                OutlinedTextField(adjustment, { adjustment = it.take(9) }, label = { Text("XP adjustment") })
                OutlinedTextField(abilityOverride, { abilityOverride = it.filter(Char::isDigit).take(5) }, label = { Text("ОС override (пусто = авто)") })
                Text("Характеристики ${economy.attributeXp} · Умения ${economy.skillXp} · Навыки ${economy.developmentXp} · Мана ${economy.manaXp} · ЦИ ${economy.chiXp} · Школы ${economy.magicSchoolXp} · Заклинания ${economy.spellXp}", color = DublMuted)
                Text("Потрачено ${economy.spentXp} · осталось ${economy.remainingXp}", color = if (economy.overspentXp) MaterialTheme.colorScheme.error else DublFocus)
                Text("ОС: ${economy.abilityPointsSpent}/${economy.abilityPointsBudget} · осталось ${economy.abilityPointsRemaining}", color = if (economy.overspentAbilityPoints) MaterialTheme.colorScheme.error else DublFocus)
                if (economy.unpricedLearnedSpells > 0) Text("Заклинаний без цены XP: ${economy.unpricedLearnedSpells}", color = DublGold)
                if (character.creationComplete) OutlinedButton(onClick = { state.mutate { reopenCreation() } }) { Text("Вернуться в создание") }
                else Button(onClick = { state.mutate { completeCreation() } }) { Text("Завершить создание") }
            }
        },
        confirmButton = { TextButton(onClick = {
            state.mutate {
                setExperience(total.toIntOrNull() ?: 0)
                setCreationExperience(creation.toIntOrNull() ?: 0)
                setXpAdjustment(adjustment.toIntOrNull() ?: 0)
                setAbilityPointsOverride(abilityOverride.toIntOrNull())
            }
            onDismiss()
        }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun MaximumDialog(state: DesktopAppState, resource: CharacterSheetResourceId, onDismiss: () -> Unit) {
    val character = state.session.active
    val current = when (resource) {
        CharacterSheetResourceId.HEALTH -> character.healthMaximumOverride ?: character.healthMaximum
        CharacterSheetResourceId.ENDURANCE -> character.enduranceMaximumOverride ?: character.enduranceMaximum
        CharacterSheetResourceId.MANA -> character.manaMaximumOverride ?: character.effectiveManaMaximum
        CharacterSheetResourceId.CHI -> character.chiMaximum
    }
    var text by remember(resource) { mutableStateOf(current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Максимум: ${resource.title}") },
        text = { OutlinedTextField(text, { text = it.filter(Char::isDigit).take(5) }, label = { Text("Ручной максимум") }) },
        confirmButton = { TextButton(enabled = resource != CharacterSheetResourceId.CHI, onClick = {
            val value = text.toIntOrNull()?.coerceAtLeast(0) ?: 0
            state.mutate { when (resource) { CharacterSheetResourceId.HEALTH -> setHealthMaximumOverride(value); CharacterSheetResourceId.ENDURANCE -> setEnduranceMaximumOverride(value); CharacterSheetResourceId.MANA -> setManaMaximumOverride(value); CharacterSheetResourceId.CHI -> Unit } }
            onDismiss()
        }) { Text("Сохранить") } },
        dismissButton = { Row { if (resource != CharacterSheetResourceId.CHI) TextButton(onClick = { state.mutate { when (resource) { CharacterSheetResourceId.HEALTH -> setHealthMaximumOverride(null); CharacterSheetResourceId.ENDURANCE -> setEnduranceMaximumOverride(null); CharacterSheetResourceId.MANA -> setManaMaximumOverride(null); CharacterSheetResourceId.CHI -> Unit } }; onDismiss() }) { Text("По формуле") }; TextButton(onClick = onDismiss) { Text("Отмена") } } },
    )
}

@Composable
private fun CustomResourceDialog(state: DesktopAppState, resource: CustomResource?, onDismiss: () -> Unit) {
    var name by remember(resource?.uid) { mutableStateOf(resource?.name.orEmpty()) }
    var current by remember(resource?.uid) { mutableStateOf((resource?.current ?: 0).toString()) }
    var maximum by remember(resource?.uid) { mutableStateOf((resource?.maximum ?: 1).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (resource == null) "Новый ресурс" else resource.name) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(7.dp)) { OutlinedTextField(name, { name = it }, label = { Text("Название") }); OutlinedTextField(current, { current = it.filter(Char::isDigit) }, label = { Text("Текущее") }); OutlinedTextField(maximum, { maximum = it.filter(Char::isDigit) }, label = { Text("Максимум") }) } },
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { val max = maximum.toIntOrNull()?.coerceAtLeast(0) ?: 0; val cur = current.toIntOrNull()?.coerceIn(0, max) ?: 0; state.mutate { if (resource == null) addCustomResource(name, max, cur) else updateCustomResource(resource.uid, name, cur, max) }; onDismiss() }) { Text("Сохранить") } },
        dismissButton = { Row { if (resource != null) TextButton(onClick = { state.mutate { removeCustomResource(resource.uid) }; onDismiss() }) { Text("Удалить") }; TextButton(onClick = onDismiss) { Text("Отмена") } } },
    )
}

@Composable
private fun ConditionsDialog(state: DesktopAppState, onDismiss: () -> Unit, onChanged: (Set<CharacterConditionId>) -> Unit) {
    val previous = state.extras.activeConditions
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Состояния") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { CharacterConditionId.entries.forEach { condition -> Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(condition in state.extras.activeConditions, { state.updateExtras { toggleCondition(state.session.active.id, condition) } }); Column { Text(condition.title); Text(condition.rulesSummary, color = DublMuted) } } } } },
        confirmButton = { TextButton(onClick = { onChanged(previous); onDismiss() }) { Text("Готово") } },
    )
}

@Composable
private fun ResourceVisibilityDialog(state: DesktopAppState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видимость ресурсов") },
        text = { Column { CharacterSheetResourceId.entries.forEach { resource -> Row(verticalAlignment = Alignment.CenterVertically) { val hidden = resource in state.extras.hiddenResourceIds; Checkbox(!hidden, { visible -> state.updateExtras { setResourceHidden(state.session.active.id, resource, !visible) } }); Text(resource.title) } } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

@Composable
private fun GroupingManagerDialog(state: DesktopAppState, kind: GroupingKind, onDismiss: () -> Unit) {
    val character = state.session.active
    val skillItems = character.resolvedSkills().filter { it.rank > 0 }
    val developmentRules = DevelopmentRules(character, state.developmentCatalog, DevelopmentProgress(character.development))
    val developmentItems = developmentRules.ownedSheetSections().flatMap { it.items }.distinctBy { it.entry.id }
    val labels = if (kind == GroupingKind.SKILLS) skillItems.associate { it.id to it.name } else developmentItems.associate { it.entry.id to it.entry.name }
    val parentById = if (kind == GroupingKind.DEVELOPMENT) developmentItems.associate { it.entry.id to it.parentId } else emptyMap()
    val defaults = if (kind == GroupingKind.SKILLS) defaultSkillGroups(skillItems) else defaultDevelopmentGroups(character, state)
    val ungroupedId = if (kind == GroupingKind.SKILLS) "skills:ungrouped" else "development:ungrouped"
    var groups by remember(character.id, kind) {
        mutableStateOf(
            SheetGroupingRules.normalize(
                if (kind == GroupingKind.SKILLS) state.extras.skillGroups else state.extras.developmentGroups,
                defaults,
                labels.keys.toList(),
                ungroupedId,
            ),
        )
    }
    var newName by remember { mutableStateOf("") }
    var editingGroupId by remember { mutableStateOf<String?>(null) }
    var editingGroupName by remember { mutableStateOf("") }
    var hoveredGroupId by remember { mutableStateOf<String?>(null) }
    val groupBounds = remember(character.id, kind) { mutableMapOf<String, Rect>() }
    val itemBounds = remember(character.id, kind) { mutableMapOf<String, Rect>() }
    val listState = rememberLazyListState()
    val dragScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var listBounds by remember(character.id, kind) { mutableStateOf(Rect.Zero) }

    fun persist(next: List<SheetGroup>) {
        groups = next
        state.updateExtras {
            if (kind == GroupingKind.SKILLS) setSkillGroups(character.id, next)
            else setDevelopmentGroups(character.id, next)
        }
    }

    val visualGroups = groups.map { group ->
        if (kind == GroupingKind.DEVELOPMENT) group.copy(itemIds = SheetGroupingRules.hierarchicalOrder(group.itemIds, parentById)) else group
    }
    val allVisualIds = visualGroups.flatMap { it.itemIds }
    val treeRootIds = parentById.keys.filterTo(mutableSetOf()) { id -> parentById[id] == null && parentById.values.any { it == id } }

    fun autoScroll(windowY: Float) {
        if (listBounds == Rect.Zero) return
        val edgePx = with(density) { 54.dp.toPx() }
        val delta = when {
            windowY < listBounds.top + edgePx -> -30f
            windowY > listBounds.bottom - edgePx -> 30f
            else -> 0f
        }
        if (delta != 0f) dragScope.launch { listState.scrollBy(delta) }
    }

    fun groupAt(windowY: Float, excludingId: String? = null): String? {
        val candidates = groupBounds.entries.filter { it.key != excludingId }
        val direct = candidates.firstOrNull { (_, bounds) -> windowY in bounds.top..bounds.bottom }
        return direct?.key ?: candidates.minByOrNull { (_, bounds) ->
            kotlin.math.abs(windowY - ((bounds.top + bounds.bottom) / 2f))
        }?.key
    }

    fun moveBlockAt(block: List<String>, windowY: Float) {
        val moving = block.toSet()
        val targetGroupId = groupAt(windowY) ?: return
        val targetGroup = visualGroups.firstOrNull { it.id == targetGroupId } ?: return
        val remaining = targetGroup.itemIds.filterNot { it in moving }
        val targetItemId = remaining.filter(itemBounds::containsKey).minByOrNull { itemId ->
            val bounds = itemBounds[itemId] ?: return@minByOrNull Float.MAX_VALUE
            kotlin.math.abs(windowY - ((bounds.top + bounds.bottom) / 2f))
        }
        val targetIndex = if (targetItemId == null) {
            remaining.size
        } else {
            val bounds = itemBounds[targetItemId]
            val base = remaining.indexOf(targetItemId).coerceAtLeast(0)
            if (bounds != null && windowY > (bounds.top + bounds.bottom) / 2f) base + 1 else base
        }
        var next = SheetGroupingRules.moveItems(visualGroups, block, targetGroupId, targetIndex)
        if (kind == GroupingKind.DEVELOPMENT) {
            next = next.map { it.copy(itemIds = SheetGroupingRules.hierarchicalOrder(it.itemIds, parentById)) }
        }
        persist(next)
    }

    fun moveGroupAt(groupId: String, windowY: Float) {
        val targetGroupId = groupAt(windowY, excludingId = groupId) ?: return
        val remaining = visualGroups.filterNot { it.id == groupId }
        val targetIndex = remaining.indexOfFirst { it.id == targetGroupId }.takeIf { it >= 0 } ?: return
        val bounds = groupBounds[targetGroupId]
        val insertion = if (bounds != null && windowY > (bounds.top + bounds.bottom) / 2f) targetIndex + 1 else targetIndex
        persist(SheetGroupingRules.moveGroupToIndex(visualGroups, groupId, insertion))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Группы · ${kind.title}") },
        text = {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .onGloballyPositioned { listBounds = it.boundsInWindow() },
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "Зажмите ≡ и тяните. Корень ветки переносит всё дерево; дочерний навык можно перенести отдельно.",
                            modifier = Modifier.padding(10.dp),
                            color = DublMuted,
                        )
                    }
                }

                items(visualGroups, key = { it.id }) { group ->
                    var groupDragOffset by remember(group.id) { mutableStateOf(0f) }
                    var groupDragging by remember(group.id) { mutableStateOf(false) }
                    var groupPointerY by remember(group.id) { mutableStateOf(0f) }
                    val dropTarget = hoveredGroupId == group.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (groupDragging) 8f else 0f)
                            .graphicsLayer { translationY = groupDragOffset }
                            .onGloballyPositioned { groupBounds[group.id] = it.boundsInWindow() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (dropTarget) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                        border = BorderStroke(if (dropTarget) 2.dp else 1.dp, if (dropTarget) DublFocus else MaterialTheme.colorScheme.outline),
                    ) {
                        Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    modifier = Modifier.pointerInput(group.id) {
                                        var accumulated = 0f
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { local ->
                                                accumulated = 0f
                                                groupDragOffset = 0f
                                                groupDragging = true
                                                groupPointerY = (groupBounds[group.id]?.top ?: 0f) + local.y
                                                autoScroll(groupPointerY)
                                                hoveredGroupId = groupAt(groupPointerY, group.id)
                                            },
                                            onDragCancel = {
                                                groupDragOffset = 0f
                                                groupDragging = false
                                                hoveredGroupId = null
                                            },
                                            onDragEnd = {
                                                groupDragOffset = 0f
                                                groupDragging = false
                                                moveGroupAt(group.id, groupPointerY)
                                                hoveredGroupId = null
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                accumulated += amount.y
                                                groupDragOffset = accumulated
                                                groupPointerY += amount.y
                                                autoScroll(groupPointerY)
                                                hoveredGroupId = groupAt(groupPointerY, group.id)
                                            },
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                ) {
                                    Text("≡", modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), fontSize = 21.sp, color = DublFocus, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(group.title, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${group.itemIds.size} элементов", color = DublMuted)
                                }
                                TextButton(onClick = { persist(SheetGroupingRules.toggleCollapsed(visualGroups, group.id)) }) {
                                    Text(if (group.collapsed) "▸" else "▾")
                                }
                                TextButton(onClick = { editingGroupId = group.id; editingGroupName = group.title }) { Text("✎") }
                                if (group.id != ungroupedId) {
                                    TextButton(onClick = { persist(SheetGroupingRules.deleteGroup(visualGroups, group.id, ungroupedId)) }) { Text("×") }
                                }
                            }

                            if (editingGroupId == group.id) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(editingGroupName, { editingGroupName = it }, label = { Text("Название") }, modifier = Modifier.weight(1f), singleLine = true)
                                    Button(onClick = {
                                        persist(SheetGroupingRules.renameGroup(visualGroups, group.id, editingGroupName))
                                        editingGroupId = null
                                    }) { Text("OK") }
                                }
                            }

                            if (!group.collapsed) {
                                if (group.itemIds.isEmpty()) {
                                    Text("Пустая группа — перетащите сюда элемент.", color = DublMuted)
                                }
                                group.itemIds.forEach { itemId ->
                                    val depth = SheetGroupingRules.localDepth(itemId, group.itemIds, parentById)
                                    val movesTree = itemId in treeRootIds
                                    val block = if (movesTree) SheetGroupingRules.subtreeBlock(itemId, parentById, allVisualIds) else listOf(itemId)
                                    DraggableGroupingItem(
                                        itemId = itemId,
                                        label = labels[itemId] ?: itemId,
                                        depth = depth,
                                        treeSize = block.size,
                                        movesTree = movesTree,
                                        onBounds = { itemBounds[itemId] = it },
                                        onDragWindowY = { y -> autoScroll(y); hoveredGroupId = groupAt(y) },
                                        onDropWindowY = { y -> moveBlockAt(block, y); hoveredGroupId = null },
                                        onDragCancel = { hoveredGroupId = null },
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newName, { newName = it }, label = { Text("Новая группа") }, modifier = Modifier.weight(1f), singleLine = true)
                        Button(
                            enabled = newName.isNotBlank(),
                            onClick = {
                                persist(SheetGroupingRules.addGroup(groups, "custom:${UUID.randomUUID()}", newName))
                                newName = ""
                            },
                        ) { Text("Добавить") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

@Composable
private fun DraggableGroupingItem(
    itemId: String,
    label: String,
    depth: Int,
    treeSize: Int,
    movesTree: Boolean,
    onBounds: (Rect) -> Unit,
    onDragWindowY: (Float) -> Unit,
    onDropWindowY: (Float) -> Unit,
    onDragCancel: () -> Unit,
) {
    var dragOffset by remember(itemId) { mutableStateOf(0f) }
    var dragging by remember(itemId) { mutableStateOf(false) }
    var bounds by remember(itemId) { mutableStateOf(Rect.Zero) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth.coerceAtMost(3) * 12).dp)
            .zIndex(if (dragging) 5f else 0f)
            .graphicsLayer { translationY = dragOffset }
            .onGloballyPositioned { coordinates -> bounds = coordinates.boundsInWindow(); onBounds(bounds) }
            .pointerInput(itemId, movesTree, treeSize) {
                var accumulated = 0f
                var pointerY = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = { local ->
                        accumulated = 0f
                        dragOffset = 0f
                        dragging = true
                        pointerY = bounds.top + local.y
                        onDragWindowY(pointerY)
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        dragging = false
                        onDragCancel()
                    },
                    onDragEnd = {
                        dragOffset = 0f
                        dragging = false
                        onDropWindowY(pointerY)
                    },
                    onDrag = { change, amount ->
                        change.consume()
                        accumulated += amount.y
                        dragOffset = accumulated
                        pointerY += amount.y
                        onDragWindowY(pointerY)
                    },
                )
            },
        shape = RoundedCornerShape(9.dp),
        color = if (dragging) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (dragging) DublFocus else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        Row(Modifier.padding(horizontal = 9.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("≡", color = DublFocus, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            if (depth > 0) {
                Text("↳", color = DublFocus)
                Spacer(Modifier.width(5.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                when {
                    movesTree -> Text("Корень дерева · переносит $treeSize элементов", color = DublGold)
                    depth > 0 -> Text("Дочерний навык · переносится отдельно", color = DublMuted)
                }
            }
            Text("тянуть", color = DublMuted)
        }
    }
}

private fun defaultSkillGroups(skills: List<com.dubl.character.android.model.ResolvedSkill>): List<SheetGroup> = SkillCategory.entries.mapNotNull { category ->
    skills.filter { it.category == category }.map { it.id }.takeIf { it.isNotEmpty() }?.let { SheetGroup("skills:${category.name}", category.title, it) }
}

private fun defaultDevelopmentGroups(character: DublCharacter, state: DesktopAppState): List<SheetGroup> = DevelopmentRules(character, state.developmentCatalog, DevelopmentProgress(character.development)).ownedSheetSections().map { section ->
    SheetGroup(
        "development:${section.type.name}",
        when (section.type) {
            DevelopmentSheetSectionType.REGULAR -> "Обычные навыки"
            DevelopmentSheetSectionType.SPECIAL -> "Спец. навыки"
            DevelopmentSheetSectionType.MARTIAL_ARTS -> "Боевые искусства"
            DevelopmentSheetSectionType.CHI -> "ЦИ"
        },
        section.items.map { it.entry.id },
    )
}


@Composable
private fun PortraitImage(path: String) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, path) {
        value = runCatching {
            File(path).inputStream().buffered().use(::loadImageBitmap)
        }.getOrNull()
    }
    bitmap?.let { image ->
        Image(
            bitmap = image,
            contentDescription = "Портрет персонажа",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(176.dp),
        )
    }
}

private fun pickPortraitFile(): Path? {
    val dialog = FileDialog(null as Frame?, "Выбрать портрет", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}
