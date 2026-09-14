package com.dubl.character.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.components.DublScreenHeader

@Composable
fun CharactersScreen(controller: CharacterController) {
    var confirmDelete by remember { mutableStateOf(false) }
    val snapshot = controller.snapshot

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        DublScreenHeader(
            title = "Персонажи",
            subtitle = "Выбор активного листа",
            action = {
                Button(onClick = controller::createCharacter) { Text("+ Создать") }
            },
        )

        snapshot.characters.forEach { character ->
            val active = character.id == snapshot.activeCharacterId
            DublCard(Modifier.fillMaxWidth()) {
                Text(
                    character.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${character.concept.ifBlank { "Без концепта" }} • ${character.experience} опыта",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (active) {
                    Text("Активный", color = MaterialTheme.colorScheme.primary)
                } else {
                    OutlinedButton(onClick = { controller.selectCharacter(character.id) }) {
                        Text("Открыть")
                    }
                }
            }
        }

        if (snapshot.characters.size > 1) {
            OutlinedButton(onClick = { confirmDelete = true }) {
                Text("Удалить активного персонажа")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить персонажа?") },
            text = { Text("${controller.active.name} будет удалён с этого устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    controller.deleteActive()
                    confirmDelete = false
                }) { Text("Удалить") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Отмена") } },
        )
    }
}
