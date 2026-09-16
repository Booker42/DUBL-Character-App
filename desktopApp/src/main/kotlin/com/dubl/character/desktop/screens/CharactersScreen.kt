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
import com.dubl.character.android.application.CharacterTransferImportResult
import com.dubl.character.android.data.CharacterTransferRejectReason
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.desktop.DesktopAppState
import java.awt.FileDialog
import java.awt.Frame
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Composable
fun CharactersScreen(state: DesktopAppState, modifier: Modifier = Modifier) {
    var confirmDelete by remember { mutableStateOf(false) }
    var transferStatus by remember { mutableStateOf<String?>(null) }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        val file = pickTransferFile()
                        if (file != null) {
                            val raw = runCatching { Files.readString(file, StandardCharsets.UTF_8) }.getOrNull()
                            transferStatus = if (raw == null) {
                                "Не удалось прочитать файл персонажа."
                            } else {
                                importStatus(state.importCharacter(raw))
                            }
                        }
                    }) { Text("Импорт") }
                    OutlinedButton(onClick = {
                        val file = saveTransferFile(state.activeCharacter.name)
                        if (file != null) {
                            transferStatus = if (runCatching {
                                    Files.writeString(file, state.exportActiveCharacter(), StandardCharsets.UTF_8)
                                }.isSuccess
                            ) {
                                "Персонаж экспортирован."
                            } else {
                                "Не удалось сохранить файл персонажа."
                            }
                        }
                    }) { Text("Экспорт") }
                    Button(onClick = { state.createCharacter() }) { Text("+ Новый персонаж") }
                }
            }
        }
        transferStatus?.let { status ->
            item {
                Text(
                    status,
                    color = DublMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
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

private fun pickTransferFile(): Path? {
    val dialog = FileDialog(null as Frame?, "Импорт персонажа DUBL", FileDialog.LOAD)
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return Path.of(dialog.directory, file)
}

private fun saveTransferFile(characterName: String): Path? {
    val dialog = FileDialog(null as Frame?, "Экспорт персонажа DUBL", FileDialog.SAVE)
    dialog.file = "${safeTransferFileName(characterName)}.dubl"
    dialog.isVisible = true
    val file = dialog.file ?: return null
    val chosen = Path.of(dialog.directory, file)
    return if (chosen.fileName.toString().endsWith(".dubl", ignoreCase = true)) {
        chosen
    } else {
        chosen.resolveSibling("${chosen.fileName}.dubl")
    }
}

private fun importStatus(result: CharacterTransferImportResult): String = when (result) {
    is CharacterTransferImportResult.Imported -> "Импортирован персонаж: ${result.name}."
    is CharacterTransferImportResult.Rejected -> when (result.reason) {
        CharacterTransferRejectReason.INVALID_FILE -> "Это не поддерживаемый файл персонажа DUBL."
        CharacterTransferRejectReason.UNSUPPORTED_FORMAT_VERSION -> "Версия файла персонажа пока не поддерживается."
        CharacterTransferRejectReason.UNSUPPORTED_RULESET -> "Этот файл создан для другого рулбука или версии правил."
    }
}

private fun safeTransferFileName(name: String): String = name
    .trim()
    .ifBlank { "character" }
    .replace(Regex("[\\\\/:*?\"<>|]+"), "_")
    .take(80)
