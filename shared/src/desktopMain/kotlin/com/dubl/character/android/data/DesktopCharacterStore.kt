package com.dubl.character.android.data

import com.dubl.character.android.model.AppSnapshot
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

class DesktopCharacterStore(
    private val file: Path = defaultDataDirectory().resolve("characters.json"),
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) : CharacterStore {
    override fun load(): AppSnapshot {
        if (!Files.exists(file)) return SnapshotCodec.fresh(idFactory)
        return runCatching {
            SnapshotCodec.decode(Files.readString(file, StandardCharsets.UTF_8), idFactory)
        }.getOrElse { SnapshotCodec.fresh(idFactory) }
    }

    override fun save(snapshot: AppSnapshot) {
        Files.createDirectories(file.parent)
        val temp = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temp, SnapshotCodec.encode(snapshot), StandardCharsets.UTF_8)
        runCatching {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    companion object {
        fun defaultDataDirectory(): Path {
            val xdg = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
            val base = if (xdg != null) Path.of(xdg) else Path.of(System.getProperty("user.home"), ".local", "share")
            return base.resolve("dubl-character")
        }
    }
}
