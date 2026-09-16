package com.dubl.character.android.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dubl.character.android.application.CharacterTransferImportResult
import com.dubl.character.android.data.CharacterTransferRejectReason
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.DublCard
import com.dubl.character.android.ui.components.DublScreenHeader
import java.nio.charset.StandardCharsets

private const val TRANSFER_EXTENSION = ".dubl"

@Composable
fun CharactersScreen(controller: CharacterController) {
    var confirmDelete by remember { mutableStateOf(false) }
    var transferStatus by remember { mutableStateOf<String?>(null) }
    val snapshot = controller.snapshot
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            transferStatus = if (writeTransferFile(context, uri, controller.exportActiveCharacter())) {
                "Персонаж экспортирован."
            } else {
                "Не удалось сохранить файл персонажа."
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val raw = readTransferFile(context, uri)
            transferStatus = if (raw == null) {
                "Не удалось прочитать файл персонажа."
            } else {
                importStatus(controller.importCharacter(raw))
            }
        }
    }

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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.weight(1f),
            ) { Text("Импорт") }
            OutlinedButton(
                onClick = {
                    exportLauncher.launch(safeTransferFileName(controller.active.name) + TRANSFER_EXTENSION)
                },
                modifier = Modifier.weight(1f),
            ) { Text("Экспорт") }
        }
        transferStatus?.let { status ->
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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

private fun writeTransferFile(context: Context, uri: Uri, raw: String): Boolean = runCatching {
    val stream = context.contentResolver.openOutputStream(uri) ?: error("Cannot open output stream")
    stream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(raw) }
}.isSuccess

private fun readTransferFile(context: Context, uri: Uri): String? = runCatching {
    val stream = context.contentResolver.openInputStream(uri) ?: error("Cannot open input stream")
    stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
}.getOrNull()

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
