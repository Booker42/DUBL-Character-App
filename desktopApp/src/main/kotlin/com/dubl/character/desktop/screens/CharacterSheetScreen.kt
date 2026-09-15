package com.dubl.character.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.dubl.character.android.model.CustomCondition
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
private data class SheetSkillRollRequest(val skill: ResolvedSkill, val attribute: AttributeId)
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
    val character = state.activeCharacter
    val extras = state.extras
    val economy = CharacterEconomy.breakdown(character, state.developmentCatalog)
    var showIdentity by remember(character.id) { mutableStateOf(false) }
    var showEconomy by remember(character.id) { mutableStateOf(false) }
    var showConditions by remember(character.id) { mutableStateOf(false) }
    var showNotes by remember(character.id) { mutableStateOf(false) }
    var showVisibility by remember(character.id) { mutableStateOf(false) }
    var showHealthControl by remember(character.id) { mutableStateOf(false) }
    var customResource by remember(character.id) { mutableStateOf<CustomResource?>(null) }
    var createResource by remember(character.id) { mutableStateOf(false) }
    var maximumResource by remember(character.id) { mutableStateOf<CharacterSheetResourceId?>(null) }
    var rollRequest by remember(character.id) { mutableStateOf<ContextRollRequest?>(null) }
    var sheetRollAttributeChoice by remember(character.id) { mutableStateOf<ResolvedSkill?>(null) }
    var sheetRollRequest by remember(character.id) { mutableStateOf<SheetSkillRollRequest?>(null) }
    var sheetDevelopmentEntry by remember(character.id) { mutableStateOf<DevelopmentEntry?>(null) }
    var sheetEditingDevelopment by remember(character.id) { mutableStateOf<DevelopmentEntry?>(null) }
    var grouping by remember(character.id) { mutableStateOf<GroupingKind?>(null) }
    var recent by remember(character.id) { mutableStateOf<RecentSheetChange?>(null) }

    fun changeResource(resource: CharacterSheetResourceId, delta: Int) {
        val before = when (resource) {
            CharacterSheetResourceId.HEALTH -> character.hpCurrent
            CharacterSheetResourceId.ENDURANCE -> character.enduranceCurrent
            CharacterSheetResourceId.MANA -> character.manaCurrent
            CharacterSheetResourceId.CHI -> character.chiCurrent
        }
        when (resource) {
            CharacterSheetResourceId.HEALTH -> state.changeHp(delta)
            CharacterSheetResourceId.ENDURANCE -> state.changeEndurance(delta)
            CharacterSheetResourceId.MANA -> state.changeMana(delta)
            CharacterSheetResourceId.CHI -> state.changeChi(delta)
        }
        val after = when (resource) {
            CharacterSheetResourceId.HEALTH -> state.activeCharacter.hpCurrent
            CharacterSheetResourceId.ENDURANCE -> state.activeCharacter.enduranceCurrent
            CharacterSheetResourceId.MANA -> state.activeCharacter.manaCurrent
            CharacterSheetResourceId.CHI -> state.activeCharacter.chiCurrent
        }
        val applied = after - before
        if (applied != 0) recent = RecentSheetChange("${resource.title} ${signed(applied)}", SheetUndo.Resource(resource, applied))
    }

    fun undoRecent() {
        if (recent?.undo != null) state.undoLast()
        recent = null
    }

    val effectiveConditions = buildSet {
        addAll(extras.activeConditions)
        if (character.enduranceCurrent == 0) add(CharacterConditionId.WEAKNESS)
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val compactSheet = maxWidth < 880.dp
        val wideSheet = maxWidth >= 1320.dp
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (recent != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(recent!!.text)
                            if (recent!!.undo != null) TextButton(onClick = ::undoRecent) { Text("Отменить") }
                        }
                    }
                }
            }

            item {
                CharacterHero(
                    state = state,
                    character = character,
                    extras = extras,
                    economy = economy,
                    effectiveConditions = effectiveConditions,
                    compact = compactSheet,
                    onEditIdentity = { showIdentity = true },
                    onEconomy = { showEconomy = true },
                    onConditions = { showConditions = true },
                )
            }

            item {
                CharacterDashboard(
                    state = state,
                    character = character,
                    extras = extras,
                    compact = compactSheet,
                    wide = wideSheet,
                    onResourceDelta = ::changeResource,
                    onResourceVisibility = { showVisibility = true },
                    onHealthControl = { showHealthControl = true },
                    onEditMaximum = { maximumResource = it },
                    onEditCustomResource = { customResource = it },
                    onCreateCustomResource = { createResource = true },
                    onAttributeDelta = { id, delta ->
                        state.changeAttribute(id, delta)
                        recent = RecentSheetChange("${id.title} ${signed(delta)}", SheetUndo.Attribute(id, delta))
                    },
                    onRoll = { context, attribute -> rollRequest = ContextRollRequest(context, attribute) },
                    onSkillRoll = { sheetRollAttributeChoice = it },
                    onNavigateSkills = onNavigateSkills,
                    onGrouping = { grouping = it },
                )
            }

            item {
                SheetSummaries(
                    state = state,
                    character = character,
                    extras = extras,
                    compact = compactSheet,
                    onGrouping = { grouping = it },
                    onDevelopmentDetails = { sheetDevelopmentEntry = it },
                    onNavigateDevelopment = onNavigateDevelopment,
                    onEditNotes = { showNotes = true },
                )
            }
        }
    }

    if (showIdentity) IdentityDialog(state, character, onDismiss = { showIdentity = false }) { previous -> recent = RecentSheetChange("Персонаж изменён", SheetUndo.Identity(previous)) }
    if (showEconomy) EconomyDialog(state, onDismiss = { showEconomy = false })
    if (showConditions) ConditionsDialog(state, onDismiss = { showConditions = false }) { previous -> recent = RecentSheetChange("Состояния изменены", SheetUndo.Conditions(previous)) }
    if (showVisibility) ResourceVisibilityDialog(state, onDismiss = { showVisibility = false })
    if (showNotes) NotesDialog(state, onDismiss = { showNotes = false })
    if (showHealthControl) HealthControlDialog(
        current = state.activeCharacter.hpCurrent,
        maximum = state.activeCharacter.healthMaximum,
        onChange = { delta -> changeResource(CharacterSheetResourceId.HEALTH, delta) },
        onEditMaximum = { showHealthControl = false; maximumResource = CharacterSheetResourceId.HEALTH },
        onDismiss = { showHealthControl = false },
    )
    if (createResource) CustomResourceDialog(state, null, onDismiss = { createResource = false })
    customResource?.let { resource -> CustomResourceDialog(state, resource, onDismiss = { customResource = null }) }
    maximumResource?.let { resource -> MaximumDialog(state, resource, onDismiss = { maximumResource = null }) }
    rollRequest?.let { request -> ContextRollDialog(
        character = state.activeCharacter,
        context = request.context,
        developmentCatalog = state.developmentCatalog,
        effectCatalog = state.skillEffectCatalog,
        initialAttribute = request.attribute,
        onDismiss = { rollRequest = null },
    ) }
    sheetDevelopmentEntry?.let { entry ->
        DevelopmentDetailsDialog(
            state = state,
            entry = entry,
            onOpenEntry = { targetId -> state.developmentCatalog.byId(targetId)?.let { sheetDevelopmentEntry = it } },
            onEditLocal = {
                sheetEditingDevelopment = entry
                sheetDevelopmentEntry = null
            },
            onResetLocal = {
                state.resetDevelopmentOverride(entry.id)
                sheetDevelopmentEntry = null
            },
            onDeleteCustom = {
                state.removeCustomDevelopment(entry.id)
                sheetDevelopmentEntry = null
            },
            hasLocalOverride = character.developmentOverrides.containsKey(entry.id),
            isCustom = character.customDevelopmentEntries.any { it.id == entry.id },
            onDismiss = { sheetDevelopmentEntry = null },
        )
    }
    sheetEditingDevelopment?.let { entry ->
        val isCustom = state.activeCharacter.customDevelopmentEntries.any { it.id == entry.id }
        DevelopmentLocalEditDialog(
            initial = entry,
            title = if (isCustom) "Редактировать свою запись" else "Локальная правка",
            onSave = { updated ->
                if (isCustom) state.updateCustomDevelopment(updated) else state.setDevelopmentOverride(updated)
                sheetEditingDevelopment = null
            },
            onDismiss = { sheetEditingDevelopment = null },
        )
    }
    sheetRollAttributeChoice?.let { skill ->
        SkillAttributeChoiceDialog(
            character = state.activeCharacter,
            skill = skill,
            onConfirm = { attribute ->
                sheetRollAttributeChoice = null
                sheetRollRequest = SheetSkillRollRequest(skill, attribute)
            },
            onDismiss = { sheetRollAttributeChoice = null },
        )
    }
    sheetRollRequest?.let { request -> SkillRollDialog(
        character = state.activeCharacter,
        skill = request.skill,
        preferredAttribute = null,
        initialAttribute = request.attribute,
        developmentCatalog = state.developmentCatalog,
        effectCatalog = state.skillEffectCatalog,
        onPreferredAttribute = {},
        onDismiss = { sheetRollRequest = null },
    ) }
    grouping?.let { kind -> GroupingManagerDialog(state, kind, onDismiss = { grouping = null }) }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CharacterHero(
    state: DesktopAppState,
    character: DublCharacter,
    extras: CharacterSheetExtras,
    economy: com.dubl.character.android.model.CharacterEconomyBreakdown,
    effectiveConditions: Set<CharacterConditionId>,
    compact: Boolean,
    onEditIdentity: () -> Unit,
    onEconomy: () -> Unit,
    onConditions: () -> Unit,
) {
    val activeCustom = extras.customConditions.filter { it.active }
    DesktopHeroPanel(Modifier.fillMaxWidth()) {
        if (compact) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HeroPortrait(state, extras)
                HeroIdentity(character, economy, onEditIdentity, onEconomy)
                HeroConditions(extras, effectiveConditions, activeCustom, onConditions)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top,
            ) {
                HeroPortrait(state, extras)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                    HeroIdentity(character, economy, onEditIdentity, onEconomy)
                    HeroConditions(extras, effectiveConditions, activeCustom, onConditions)
                }
            }
        }
    }
}

@Composable
private fun HeroPortrait(state: DesktopAppState, extras: CharacterSheetExtras) {
    Column(Modifier.width(156.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        extras.portraitUri?.let { portraitPath ->
            PortraitImage(portraitPath, Modifier.fillMaxWidth().height(156.dp))
        } ?: Surface(
            modifier = Modifier.fillMaxWidth().height(156.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.48f)),
        ) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                DesktopIcon(DesktopIconKind.PORTRAIT, tint = DublMuted, size = 48.dp)
                Spacer(Modifier.height(8.dp))
                Text("Портрет", color = DublMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = {
                val file = pickPortraitFile()
                if (file != null) state.importPortrait(file)?.let { imported -> state.setPortrait(imported) }
            }) { Text(if (extras.portraitUri == null) "Добавить" else "Сменить") }
            if (extras.portraitUri != null) TextButton(onClick = { state.setPortrait(null) }) { Text("Убрать") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroIdentity(
    character: DublCharacter,
    economy: com.dubl.character.android.model.CharacterEconomyBreakdown,
    onEditIdentity: () -> Unit,
    onEconomy: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(character.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(character.concept.ifBlank { "Без концепта" }, color = DublMuted, style = MaterialTheme.typography.bodyLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("XP ${character.experience}", color = DublGold, fontWeight = FontWeight.Bold)
                Text("Осталось ${economy.remainingXp}", color = if (economy.overspentXp) MaterialTheme.colorScheme.error else DublFocus)
                Text("ОС ${economy.abilityPointsRemaining}/${economy.abilityPointsBudget}", color = DublGold)
                Text("Размер ${character.size} · Ног ${character.legs}", color = DublMuted)
                Text(if (character.creationComplete) "Создание завершено" else "Режим создания", color = DublMuted)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopSmallAction("Редактировать", onEditIdentity)
            TextButton(onClick = onEconomy) { Text("Экономика") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroConditions(
    extras: CharacterSheetExtras,
    effectiveConditions: Set<CharacterConditionId>,
    activeCustom: List<CustomCondition>,
    onConditions: () -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (effectiveConditions.isEmpty() && activeCustom.isEmpty()) {
            DesktopConditionChip(label = "Нет активных состояний", onClick = onConditions)
        }
        effectiveConditions.forEach { condition ->
            val local = extras.conditionOverrides[condition]
            DesktopConditionChip(
                label = local?.title ?: condition.title,
                automatic = condition == CharacterConditionId.WEAKNESS && condition !in extras.activeConditions,
                onClick = onConditions,
            )
        }
        activeCustom.forEach { condition ->
            DesktopConditionChip(label = condition.title, onClick = onConditions)
        }
        if (effectiveConditions.isNotEmpty() || activeCustom.isNotEmpty()) {
            DesktopConditionChip(label = "+ Состояние", onClick = onConditions)
        }
    }
}

@Composable
private fun CharacterDashboard(
    state: DesktopAppState,
    character: DublCharacter,
    extras: CharacterSheetExtras,
    compact: Boolean,
    wide: Boolean,
    onResourceDelta: (CharacterSheetResourceId, Int) -> Unit,
    onResourceVisibility: () -> Unit,
    onHealthControl: () -> Unit,
    onEditMaximum: (CharacterSheetResourceId) -> Unit,
    onEditCustomResource: (CustomResource) -> Unit,
    onCreateCustomResource: () -> Unit,
    onAttributeDelta: (AttributeId, Int) -> Unit,
    onRoll: (RollContext, AttributeId?) -> Unit,
    onSkillRoll: (ResolvedSkill) -> Unit,
    onNavigateSkills: () -> Unit,
    onGrouping: (GroupingKind) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ResourcesPanel(
            state, character, extras, compact, onResourceDelta, onResourceVisibility, onHealthControl,
            onEditMaximum, onEditCustomResource, onCreateCustomResource,
        )
        DenseStatsSkillsRow(
            state = state,
            character = character,
            extras = extras,
            compact = compact,
            wide = wide,
            onAttributeDelta = onAttributeDelta,
            onRoll = onRoll,
            onSkillRoll = onSkillRoll,
            onNavigateSkills = onNavigateSkills,
            onGrouping = onGrouping,
        )
    }
}

@Composable
private fun ResourcesPanel(
    state: DesktopAppState,
    character: DublCharacter,
    extras: CharacterSheetExtras,
    compact: Boolean,
    onResourceDelta: (CharacterSheetResourceId, Int) -> Unit,
    onResourceVisibility: () -> Unit,
    onHealthControl: () -> Unit,
    onEditMaximum: (CharacterSheetResourceId) -> Unit,
    onEditCustomResource: (CustomResource) -> Unit,
    onCreateCustomResource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesktopPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DesktopSectionHeader(
                "Ресурсы",
                icon = DesktopIconKind.HEALTH,
                action = { TextButton(onClick = onResourceVisibility) { Text("Видимость") } },
            )

            val tiles = mutableListOf<@Composable (Modifier) -> Unit>()
            if (CharacterSheetResourceId.HEALTH !in extras.hiddenResourceIds) tiles += { tileModifier ->
                DesktopResourceTile(
                    DesktopIconKind.HEALTH, "Здоровье", character.hpCurrent, character.healthMaximum, DublHealth,
                    { onResourceDelta(CharacterSheetResourceId.HEALTH, -1) },
                    { onResourceDelta(CharacterSheetResourceId.HEALTH, 1) },
                    tileModifier,
                    onSecondary = onHealthControl,
                )
            }
            if (CharacterSheetResourceId.ENDURANCE !in extras.hiddenResourceIds) tiles += { tileModifier ->
                DesktopResourceTile(
                    DesktopIconKind.ENDURANCE, "Выносливость", character.enduranceCurrent, character.enduranceMaximum, DublStamina,
                    { onResourceDelta(CharacterSheetResourceId.ENDURANCE, -1) },
                    { onResourceDelta(CharacterSheetResourceId.ENDURANCE, 1) },
                    tileModifier,
                    onSecondary = { onEditMaximum(CharacterSheetResourceId.ENDURANCE) },
                )
            }
            if ((character.manaEnabled || character.effectiveManaMaximum > 0) && CharacterSheetResourceId.MANA !in extras.hiddenResourceIds) tiles += { tileModifier ->
                DesktopResourceTile(
                    DesktopIconKind.MANA, "Мана", character.manaCurrent, character.effectiveManaMaximum, DublMana,
                    { onResourceDelta(CharacterSheetResourceId.MANA, -1) },
                    { onResourceDelta(CharacterSheetResourceId.MANA, 1) },
                    tileModifier,
                    onSecondary = { onEditMaximum(CharacterSheetResourceId.MANA) },
                )
            }
            if (character.chiActive && CharacterSheetResourceId.CHI !in extras.hiddenResourceIds) tiles += { tileModifier ->
                DesktopResourceTile(
                    DesktopIconKind.CHI, "ЦИ", character.chiCurrent, character.chiMaximum, DublFocus,
                    { onResourceDelta(CharacterSheetResourceId.CHI, -1) },
                    { onResourceDelta(CharacterSheetResourceId.CHI, 1) },
                    tileModifier,
                    onSecondary = { state.restoreChi() },
                )
            }

            if (compact) {
                tiles.forEach { tile -> tile(Modifier.fillMaxWidth()) }
            } else {
                tiles.chunked(4).forEach { rowTiles ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                        rowTiles.forEach { tile -> Box(Modifier.weight(1f)) { tile(Modifier.fillMaxWidth()) } }
                        repeat(4 - rowTiles.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            if (character.customResources.isNotEmpty()) {
                character.customResources.chunked(if (compact) 1 else 4).forEach { resources ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.Top) {
                        resources.forEach { resource ->
                            Box(Modifier.weight(1f)) {
                                DesktopResourceTile(
                                    DesktopIconKind.GENERIC_SKILL,
                                    resource.name,
                                    resource.current,
                                    resource.maximum,
                                    DublCustomResource,
                                    { state.changeCustomResource(resource.uid, -1) },
                                    { state.changeCustomResource(resource.uid, 1) },
                                    Modifier.fillMaxWidth(),
                                    onSecondary = { onEditCustomResource(resource) },
                                )
                            }
                        }
                        if (!compact) repeat(4 - resources.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            TextButton(onClick = onCreateCustomResource) { Text("+ Свой ресурс") }
        }
    }
}

@Composable
private fun DenseStatsSkillsRow(
    state: DesktopAppState,
    character: DublCharacter,
    extras: CharacterSheetExtras,
    compact: Boolean,
    wide: Boolean,
    onAttributeDelta: (AttributeId, Int) -> Unit,
    onRoll: (RollContext, AttributeId?) -> Unit,
    onSkillRoll: (ResolvedSkill) -> Unit,
    onNavigateSkills: () -> Unit,
    onGrouping: (GroupingKind) -> Unit,
) {
    val skillRoll: (ResolvedSkill) -> Unit = onSkillRoll
    if (compact) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactCharacteristicsPanel(character, onAttributeDelta, onRoll, Modifier.fillMaxWidth())
            CompactMetricsPanel(character, onRoll, Modifier.fillMaxWidth())
            SheetSkillsPanel(state, character, extras, skillRoll, onNavigateSkills, onGrouping, Modifier.fillMaxWidth())
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            CompactCharacteristicsPanel(
                character, onAttributeDelta, onRoll,
                Modifier.weight(if (wide) 0.28f else 0.30f),
            )
            CompactMetricsPanel(
                character, onRoll,
                Modifier.weight(if (wide) 0.21f else 0.24f),
            )
            SheetSkillsPanel(
                state, character, extras, skillRoll, onNavigateSkills, onGrouping,
                Modifier.weight(if (wide) 0.51f else 0.46f),
            )
        }
    }
}

@Composable
private fun CompactCharacteristicsPanel(
    character: DublCharacter,
    onAttributeDelta: (AttributeId, Int) -> Unit,
    onRoll: (RollContext, AttributeId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = listOf(
        AttributeId.STRENGTH,
        AttributeId.CONSTITUTION,
        AttributeId.DEXTERITY,
        AttributeId.SPEED,
        AttributeId.INTELLIGENCE,
        AttributeId.PERCEPTION,
        AttributeId.WILL,
        AttributeId.CHARISMA,
    )
    DesktopPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopSectionHeader("Характеристики", icon = DesktopIconKind.STRENGTH)
            order.forEach { id ->
                DesktopDenseAttributeRow(
                    icon = attributeIcon(id),
                    title = id.title,
                    value = character.attribute(id).toString(),
                    onRoll = { onRoll(RollContext.ATTRIBUTE, id) },
                    onMinus = { onAttributeDelta(id, -1) },
                    onPlus = { onAttributeDelta(id, 1) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun attributeIcon(id: AttributeId): DesktopIconKind = when (id) {
    AttributeId.STRENGTH -> DesktopIconKind.STRENGTH
    AttributeId.CONSTITUTION -> DesktopIconKind.CONSTITUTION
    AttributeId.DEXTERITY -> DesktopIconKind.DEXTERITY
    AttributeId.SPEED -> DesktopIconKind.SPEED
    AttributeId.INTELLIGENCE -> DesktopIconKind.INTELLIGENCE
    AttributeId.PERCEPTION -> DesktopIconKind.PERCEPTION
    AttributeId.WILL -> DesktopIconKind.WILL
    AttributeId.CHARISMA -> DesktopIconKind.CHARISMA
}

@Composable
private fun CompactMetricsPanel(
    character: DublCharacter,
    onRoll: (RollContext, AttributeId?) -> Unit,
    modifier: Modifier = Modifier,
) {
    data class Metric(val icon: DesktopIconKind, val title: String, val value: String, val context: RollContext? = null)
    val metrics = listOf(
        Metric(DesktopIconKind.DEFENSE, "Защита", character.defense.toString()),
        Metric(DesktopIconKind.REFLEXES, "Рефлексы", character.reflexes.toString(), RollContext.REFLEXES),
        Metric(DesktopIconKind.INITIATIVE, "Инициатива", character.initiative.toString(), RollContext.INITIATIVE),
        Metric(DesktopIconKind.FORTITUDE, "Стойкость", character.fortitude.toString(), RollContext.FORTITUDE),
        Metric(DesktopIconKind.RUN, "Бег", formatNumber(character.runFull)),
        Metric(DesktopIconKind.SIZE, "Размер", character.size.toString()),
    )
    DesktopPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            DesktopSectionHeader("Показатели", icon = DesktopIconKind.INITIATIVE)
            metrics.forEach { metric ->
                DesktopDenseMetricRow(
                    icon = metric.icon,
                    title = metric.title,
                    value = metric.value,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = metric.context?.let { context -> { onRoll(context, null) } },
                )
            }
        }
    }
}

@Composable
private fun SheetSkillsPanel(
    state: DesktopAppState,
    character: DublCharacter,
    extras: CharacterSheetExtras,
    onSkillRoll: (ResolvedSkill) -> Unit,
    onOpenAll: () -> Unit,
    onGrouping: (GroupingKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trained = character.resolvedSkills().filter { it.rank > 0 }
    val defaults = defaultSkillGroups(trained)
    val groups = SheetGroupingRules.normalize(extras.skillGroups, defaults, trained.map { it.id }, "skills:ungrouped")
    LaunchedEffect(groups, extras.skillGroups) {
        if (groups != extras.skillGroups) state.setSkillGroups(groups)
    }
    val byId = trained.associateBy { it.id }
    val ordered = groups.flatMap { group -> group.itemIds.mapNotNull(byId::get) }.distinctBy { it.id }

    DesktopPanel(modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            DesktopSectionHeader(
                "Умения",
                icon = DesktopIconKind.SKILLS,
                action = {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onGrouping(GroupingKind.SKILLS) }) { Text("Группы") }
                        TextButton(onClick = onOpenAll) { Text("Открыть все →") }
                    }
                },
            )
            if (ordered.isEmpty()) {
                EmptyState("Изученные умения появятся здесь.")
            } else {
                ordered.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                        pair.forEach { skill ->
                            val selected = skill.stockAttribute
                            val calc = character.skillCalculationForRoll(skill, selected)
                            DesktopSkillRow(
                                icon = skillIcon(skill.category),
                                title = skill.name,
                                bonus = calc.total?.let(::signed) ?: "—",
                                onRoll = { onSkillRoll(skill) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}


private fun skillIcon(category: SkillCategory): DesktopIconKind = when (category) {
    SkillCategory.COMBAT -> DesktopIconKind.DEFENSE
    SkillCategory.PHYSICAL -> DesktopIconKind.RUN
    SkillCategory.FIELD -> DesktopIconKind.PERCEPTION
    SkillCategory.SOCIAL -> DesktopIconKind.CHARISMA
    SkillCategory.KNOWLEDGE -> DesktopIconKind.INTELLIGENCE
    SkillCategory.TECHNICAL -> DesktopIconKind.EQUIPMENT
    SkillCategory.CUSTOM -> DesktopIconKind.GENERIC_SKILL
}

@Composable
private fun SheetSummaries(
    state: DesktopAppState,
    character: DublCharacter,
    extras: CharacterSheetExtras,
    compact: Boolean,
    onGrouping: (GroupingKind) -> Unit,
    onDevelopmentDetails: (DevelopmentEntry) -> Unit,
    onNavigateDevelopment: () -> Unit,
    onEditNotes: () -> Unit,
) {
    // Keep sheet grouping normalized through the shared extras API even though the summary is deliberately compact.
    val trained = character.resolvedSkills().filter { it.rank > 0 }
    val skillGroups = SheetGroupingRules.normalize(extras.skillGroups, defaultSkillGroups(trained), trained.map { it.id }, "skills:ungrouped")
    LaunchedEffect(skillGroups, extras.skillGroups) {
        if (skillGroups != extras.skillGroups) state.setSkillGroups(skillGroups)
    }

    val rules = DevelopmentRules(character, state.developmentCatalog, DevelopmentProgress(character.development))
    val developmentItems = rules.ownedSheetSections().flatMap { it.items }.distinctBy { it.entry.id }
    val developmentDefaults = defaultDevelopmentGroups(character, state)
    val developmentGroups = SheetGroupingRules.normalize(extras.developmentGroups, developmentDefaults, developmentItems.map { it.entry.id }, "development:ungrouped")
    LaunchedEffect(developmentGroups, extras.developmentGroups) {
        if (developmentGroups != extras.developmentGroups) state.setDevelopmentGroups(developmentGroups)
    }
    val byId = developmentItems.associateBy { it.entry.id }
    val orderedDevelopment = developmentGroups.flatMap { group -> group.itemIds.mapNotNull(byId::get) }.distinctBy { it.entry.id }

    val developmentPanel: @Composable () -> Unit = {
        DesktopPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DesktopSectionHeader(
                    "Навыки и развитие",
                    subtitle = "Профессиональные навыки, особенности и пути развития",
                    icon = DesktopIconKind.DEVELOPMENT,
                    action = {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { onGrouping(GroupingKind.DEVELOPMENT) }) { Text("Группы") }
                            TextButton(onClick = onNavigateDevelopment) { Text("Открыть все →") }
                        }
                    },
                )
                if (orderedDevelopment.isEmpty()) {
                    EmptyState("Взятых навыков и боевых искусств пока нет.")
                } else {
                    orderedDevelopment.take(8).chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            pair.forEach { item ->
                                Surface(
                                    modifier = Modifier.weight(1f).clickable { onDevelopmentDetails(item.entry) },
                                    shape = RoundedCornerShape(9.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .22f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .45f)),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        DesktopIcon(DesktopIconKind.DEVELOPMENT, tint = DublMuted, size = 16.dp)
                                        Text(item.entry.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(rankLabel(item.rank), color = DublFocus, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            repeat(2 - pair.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }

    val notesPanel: @Composable () -> Unit = {
        NotesPanel(extras.notes, onEditNotes, Modifier.fillMaxWidth())
    }

    if (compact) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            developmentPanel()
            notesPanel()
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(.57f)) { developmentPanel() }
            Box(Modifier.weight(.43f)) { notesPanel() }
        }
    }
}


private fun rankLabel(rank: Int): String = when (rank) {
    1 -> "I"
    2 -> "II"
    3 -> "III"
    4 -> "IV"
    5 -> "V"
    else -> rank.toString()
}

@Composable
private fun NotesPanel(
    notes: String,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DesktopPanel(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            DesktopSectionHeader(
                "Заметки",
                icon = DesktopIconKind.NOTES,
                action = { DesktopSmallAction("Изменить", onEdit) },
            )
            Surface(
                modifier = Modifier.fillMaxWidth().heightIn(min = 78.dp),
                shape = RoundedCornerShape(9.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .18f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .42f)),
            ) {
                Row(Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
                    DesktopIcon(DesktopIconKind.NOTES, tint = DublMuted, size = 17.dp)
                    Text(
                        notes.ifBlank { "Заметок пока нет." },
                        color = if (notes.isBlank()) DublMuted else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NotesDialog(state: DesktopAppState, onDismiss: () -> Unit) {
    var notes by remember(state.activeCharacter.id) { mutableStateOf(state.extras.notes) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заметки") },
        text = {
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it.take(12000) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp),
                label = { Text("Заметки персонажа") },
                minLines = 7,
                maxLines = 14,
            )
        },
        confirmButton = {
            Button(onClick = { state.setNotes(notes); onDismiss() }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
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
                        enabled = amount > 0,
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
            state.setIdentity(
                name = name,
                concept = concept,
                size = size.toIntOrNull() ?: 5,
                legs = legs.toIntOrNull() ?: 2,
                manaEnabled = manaEnabled,
            )
            onChanged(before); onDismiss()
        }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun EconomyDialog(state: DesktopAppState, onDismiss: () -> Unit) {
    val character = state.activeCharacter
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
                if (character.creationComplete) OutlinedButton(onClick = { state.reopenCreation() }) { Text("Вернуться в создание") }
                else Button(onClick = { state.completeCreation() }) { Text("Завершить создание") }
            }
        },
        confirmButton = { TextButton(onClick = {
            state.setEconomy(
                total = total.toIntOrNull() ?: 0,
                creation = creation.toIntOrNull() ?: 0,
                adjustment = adjustment.toIntOrNull() ?: 0,
                abilityPointsOverride = abilityOverride.toIntOrNull(),
            )
            onDismiss()
        }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun MaximumDialog(state: DesktopAppState, resource: CharacterSheetResourceId, onDismiss: () -> Unit) {
    val character = state.activeCharacter
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
            when (resource) { CharacterSheetResourceId.HEALTH -> state.setHealthMaximumOverride(value); CharacterSheetResourceId.ENDURANCE -> state.setEnduranceMaximumOverride(value); CharacterSheetResourceId.MANA -> state.setManaMaximumOverride(value); CharacterSheetResourceId.CHI -> Unit }
            onDismiss()
        }) { Text("Сохранить") } },
        dismissButton = { Row { if (resource != CharacterSheetResourceId.CHI) TextButton(onClick = { when (resource) { CharacterSheetResourceId.HEALTH -> state.setHealthMaximumOverride(null); CharacterSheetResourceId.ENDURANCE -> state.setEnduranceMaximumOverride(null); CharacterSheetResourceId.MANA -> state.setManaMaximumOverride(null); CharacterSheetResourceId.CHI -> Unit }; onDismiss() }) { Text("По формуле") }; TextButton(onClick = onDismiss) { Text("Отмена") } } },
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
        confirmButton = { TextButton(enabled = name.isNotBlank(), onClick = { val max = maximum.toIntOrNull()?.coerceAtLeast(0) ?: 0; val cur = current.toIntOrNull()?.coerceIn(0, max) ?: 0; if (resource == null) state.addCustomResource(name, max, cur) else state.updateCustomResource(resource.uid, name, cur, max); onDismiss() }) { Text("Сохранить") } },
        dismissButton = { Row { if (resource != null) TextButton(onClick = { state.removeCustomResource(resource.uid); onDismiss() }) { Text("Удалить") }; TextButton(onClick = onDismiss) { Text("Отмена") } } },
    )
}

@Composable
private fun ConditionsDialog(state: DesktopAppState, onDismiss: () -> Unit, onChanged: (Set<CharacterConditionId>) -> Unit) {
    val previous = state.extras.activeConditions
    var editCondition by remember { mutableStateOf<CharacterConditionId?>(null) }
    var editCustomId by remember { mutableStateOf<String?>(null) }
    var createCustom by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Состояния") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(CharacterConditionId.entries, key = { it.name }) { condition ->
                    val local = state.extras.conditionOverrides[condition]
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            condition in state.extras.activeConditions,
                            { state.toggleCondition(condition) },
                        )
                        Column(Modifier.weight(1f)) {
                            Text(local?.title ?: condition.title, fontWeight = FontWeight.Bold)
                            Text(local?.description ?: state.conditionCatalog.summary(condition), color = DublMuted)
                            if (local != null) Text("локальная правка", color = DublGold, fontSize = 11.sp)
                        }
                        TextButton(onClick = { editCondition = condition }) { Text("Правка") }
                    }
                }
                if (state.extras.customConditions.isNotEmpty()) {
                    item { Text("Свои состояния", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
                    items(state.extras.customConditions, key = { it.id }) { condition ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = condition.active,
                                onCheckedChange = { checked -> state.setCustomConditionActive(condition.id, checked) },
                            )
                            Column(Modifier.weight(1f)) {
                                Text(condition.title, fontWeight = FontWeight.Bold)
                                if (condition.description.isNotBlank()) Text(condition.description, color = DublMuted)
                            }
                            TextButton(onClick = { editCustomId = condition.id }) { Text("Изменить") }
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { createCustom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Добавить своё состояние")
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onChanged(previous); onDismiss() }) { Text("Готово") } },
    )

    editCondition?.let { condition ->
        val local = state.extras.conditionOverrides[condition]
        ConditionOverrideDialog(
            canonicalTitle = condition.title,
            canonicalDescription = state.conditionCatalog.summary(condition),
            title = local?.title ?: condition.title,
            description = local?.description ?: state.conditionCatalog.summary(condition),
            onSave = { title, description ->
                state.setConditionOverride(condition, title, description)
                editCondition = null
            },
            onReset = {
                state.resetConditionOverride(condition)
                editCondition = null
            },
            onDismiss = { editCondition = null },
        )
    }

    if (createCustom) {
        CustomConditionDialog(
            condition = null,
            onSave = { title, description, active ->
                state.addCustomCondition(title, description, active)
                createCustom = false
            },
            onDelete = {},
            onDismiss = { createCustom = false },
        )
    }

    editCustomId?.let { id ->
        state.extras.customConditions.firstOrNull { it.id == id }?.let { condition ->
            CustomConditionDialog(
                condition = condition,
                onSave = { title, description, active ->
                    state.updateCustomCondition(id, title, description, active)
                    editCustomId = null
                },
                onDelete = {
                    state.removeCustomCondition(id)
                    editCustomId = null
                },
                onDismiss = { editCustomId = null },
            )
        }
    }
}

@Composable
private fun ConditionOverrideDialog(
    canonicalTitle: String,
    canonicalDescription: String,
    title: String,
    description: String,
    onSave: (String, String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    var localTitle by remember(title) { mutableStateOf(title) }
    var localDescription by remember(description) { mutableStateOf(description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Локальная правка состояния") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Канон остаётся неизменным. Эта трактовка хранится только у персонажа.", color = DublMuted)
                OutlinedTextField(localTitle, { localTitle = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(localDescription, { localDescription = it }, label = { Text("Описание / трактовка") })
                Text("Рулбук: $canonicalTitle", color = DublMuted, fontSize = 11.sp)
                if (canonicalDescription.isNotBlank()) Text(canonicalDescription, color = DublMuted, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(enabled = localTitle.isNotBlank(), onClick = { onSave(localTitle, localDescription) }) { Text("Сохранить") } },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text("К рулбуку") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun CustomConditionDialog(
    condition: CustomCondition?,
    onSave: (String, String, Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(condition?.id) { mutableStateOf(condition?.title.orEmpty()) }
    var description by remember(condition?.id) { mutableStateOf(condition?.description.orEmpty()) }
    var active by remember(condition?.id) { mutableStateOf(condition?.active ?: false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (condition == null) "Добавить своё состояние" else "Изменить своё состояние") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Описание / домашнее правило") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(active, { active = it })
                    Text("Активно")
                }
            }
        },
        confirmButton = { TextButton(enabled = title.isNotBlank(), onClick = { onSave(title, description, active) }) { Text("Сохранить") } },
        dismissButton = {
            Row {
                if (condition != null) TextButton(onClick = onDelete) { Text("Удалить") }
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        },
    )
}

@Composable
private fun ResourceVisibilityDialog(state: DesktopAppState, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Видимость ресурсов") },
        text = { Column { CharacterSheetResourceId.entries.forEach { resource -> Row(verticalAlignment = Alignment.CenterVertically) { val hidden = resource in state.extras.hiddenResourceIds; Checkbox(!hidden, { visible -> state.setResourceHidden(resource, !visible) }); Text(resource.title) } } } },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
    )
}

@Composable
private fun GroupingManagerDialog(state: DesktopAppState, kind: GroupingKind, onDismiss: () -> Unit) {
    val character = state.activeCharacter
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
        if (kind == GroupingKind.SKILLS) state.setSkillGroups(next) else state.setDevelopmentGroups(next)
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
private fun PortraitImage(path: String, modifier: Modifier = Modifier.fillMaxWidth().height(176.dp)) {
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
            modifier = modifier,
        )
    }
}

private fun pickPortraitFile(): Path? {
    val dialog = FileDialog(null as Frame?, "Выбрать портрет", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}
