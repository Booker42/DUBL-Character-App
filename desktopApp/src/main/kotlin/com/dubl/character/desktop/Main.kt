package com.dubl.character.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.dubl.character.android.ui.layout.DublLayoutClass
import com.dubl.character.android.ui.layout.layoutClassForWidth
import com.dubl.character.android.ui.theme.DublAccentSoft
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.android.ui.theme.DublSurfaceInset
import com.dubl.character.android.ui.theme.DublSurfaceRaised
import com.dubl.character.android.ui.theme.DublTheme
import com.dubl.character.desktop.screens.CharacterSheetScreen
import com.dubl.character.desktop.screens.CharactersScreen
import com.dubl.character.desktop.screens.DevelopmentScreen
import com.dubl.character.desktop.screens.DesktopIcon
import com.dubl.character.desktop.screens.DesktopIconKind
import com.dubl.character.desktop.screens.EquipmentScreen
import com.dubl.character.desktop.screens.MagicScreen
import com.dubl.character.desktop.screens.SkillsScreen

internal enum class DesktopSection(val label: String) {
    SHEET("Лист"),
    SKILLS("Умения"),
    DEVELOPMENT("Навыки"),
    MAGIC("Магия"),
    EQUIPMENT("Снаряжение"),
    CHARACTERS("Персонажи"),
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "DUBL Character 0.2") {
        DublTheme { DesktopApp() }
    }
}

@Composable
private fun DesktopApp() {
    val state = remember { DesktopAppState() }
    var selected by remember { mutableStateOf(DesktopSection.SHEET) }

    BoxWithConstraints(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val layout = layoutClassForWidth(maxWidth.value.toInt())
        if (layout == DublLayoutClass.COMPACT) {
            Column(Modifier.fillMaxSize()) {
                CompactNavigation(selected, { selected = it })
                DesktopContent(state, selected, layout, { selected = it }, Modifier.weight(1f))
            }
        } else {
            Row(Modifier.fillMaxSize()) {
                DesktopRail(state, selected, { selected = it }, Modifier.width(220.dp).fillMaxHeight())
                DesktopContent(state, selected, layout, { selected = it }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DesktopRail(
    state: DesktopAppState,
    selected: DesktopSection,
    onSelected: (DesktopSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var switcherOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier.background(DublSurfaceInset).padding(horizontal = 14.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "DUBL",
            style = MaterialTheme.typography.headlineMedium,
            color = DublFocus,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth().clickable { switcherOpen = true },
            color = DublSurfaceRaised,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.52f)),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(state.activeCharacter.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text("${state.activeCharacter.experience} XP · сменить ▼", color = DublMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        DropdownMenu(expanded = switcherOpen, onDismissRequest = { switcherOpen = false }) {
            state.snapshot.characters.forEach { character ->
                DropdownMenuItem(
                    text = { Text(if (character.id == state.snapshot.activeCharacterId) "✓ ${character.name}" else character.name) },
                    onClick = {
                        state.selectCharacter(character.id)
                        switcherOpen = false
                        onSelected(DesktopSection.SHEET)
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Управление персонажами…") },
                onClick = { switcherOpen = false; onSelected(DesktopSection.CHARACTERS) },
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        DesktopSection.entries.filter { it != DesktopSection.CHARACTERS }.forEach { section ->
            NavigationItem(section, section == selected, { onSelected(section) }, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.weight(1f))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
        NavigationItem(DesktopSection.CHARACTERS,
            DesktopSection.CHARACTERS == selected,
            { onSelected(DesktopSection.CHARACTERS) },
            Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun CompactNavigation(selected: DesktopSection, onSelected: (DesktopSection) -> Unit) {
    Column(Modifier.fillMaxWidth().background(DublSurfaceInset).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DesktopSection.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { section -> NavigationItem(section, section == selected, { onSelected(section) }, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun NavigationItem(section: DesktopSection, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (active) DublAccentSoft else Color.Transparent,
        border = if (active) BorderStroke(1.dp, DublFocus.copy(alpha = 0.32f)) else null,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DesktopIcon(
                kind = section.iconKind,
                tint = if (active) DublFocus else DublMuted,
                size = 18.dp,
            )
            Text(
                section.label,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                color = if (active) MaterialTheme.colorScheme.onSurface else DublMuted,
            )
        }
    }
}

private val DesktopSection.iconKind: DesktopIconKind
    get() = when (this) {
        DesktopSection.SHEET -> DesktopIconKind.SHEET
        DesktopSection.SKILLS -> DesktopIconKind.SKILLS
        DesktopSection.DEVELOPMENT -> DesktopIconKind.DEVELOPMENT
        DesktopSection.MAGIC -> DesktopIconKind.MAGIC
        DesktopSection.EQUIPMENT -> DesktopIconKind.EQUIPMENT
        DesktopSection.CHARACTERS -> DesktopIconKind.CHARACTERS
    }

@Composable
private fun DesktopContent(
    state: DesktopAppState,
    section: DesktopSection,
    layout: DublLayoutClass,
    onNavigate: (DesktopSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
        val pageModifier = Modifier
            .fillMaxSize()
            .padding(
                horizontal = when (layout) {
                    DublLayoutClass.COMPACT -> 16.dp
                    DublLayoutClass.NORMAL -> 24.dp
                    DublLayoutClass.WIDE -> 28.dp
                },
                vertical = if (layout == DublLayoutClass.COMPACT) 14.dp else 22.dp,
            )
        when (section) {
            DesktopSection.SHEET -> CharacterSheetScreen(
                state = state,
                modifier = pageModifier,
                onNavigateSkills = { onNavigate(DesktopSection.SKILLS) },
                onNavigateDevelopment = { onNavigate(DesktopSection.DEVELOPMENT) },
                onNavigateMagic = { onNavigate(DesktopSection.MAGIC) },
                onNavigateEquipment = { onNavigate(DesktopSection.EQUIPMENT) },
            )
            DesktopSection.SKILLS -> SkillsScreen(state, pageModifier)
            DesktopSection.DEVELOPMENT -> DevelopmentScreen(state, pageModifier)
            DesktopSection.MAGIC -> MagicScreen(state, pageModifier)
            DesktopSection.EQUIPMENT -> EquipmentScreen(state, pageModifier)
            DesktopSection.CHARACTERS -> CharactersScreen(state, pageModifier)
        }
    }
}
