package com.dubl.character.desktop.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.dubl.character.desktop.DesktopAppState
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublMuted

@Composable
fun CharactersScreen(state: DesktopAppState, modifier: Modifier = Modifier) {
    var confirmDelete by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Персонажи", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("${state.snapshot.characters.size} персонаж(а/ей)", color = DublMuted)
                }
                Button(onClick = { state.createCharacter() }) { Text("+ Новый персонаж") }
            }
        }
        items(state.snapshot.characters, key = { it.id }) { character ->
            val active = character.id == state.snapshot.activeCharacterId
            SectionCard(
                title = character.name,
                modifier = Modifier.clickable { state.selectCharacter(character.id) },
                action = {
                    if (active && state.snapshot.characters.size > 1) {
                        TextButton(onClick = { confirmDelete = true }) { Text("Удалить") }
                    } else if (!active) {
                        OutlinedButton(onClick = { state.selectCharacter(character.id) }) { Text("Открыть") }
                    }
                },
            ) {
                Text(character.concept.ifBlank { "Без концепта" }, color = DublMuted)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("XP ${character.experience}", color = DublFocus)
                    Text(if (active) "Активный" else "", color = DublFocus, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить персонажа?") },
            text = { Text("${state.activeCharacter.name} будет удалён вместе с desktop-настройками листа. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    state.deleteActive()
                    confirmDelete = false
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}
