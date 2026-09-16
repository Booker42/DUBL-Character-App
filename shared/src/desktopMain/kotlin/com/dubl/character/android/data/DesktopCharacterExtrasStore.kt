package com.dubl.character.android.data

import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.CharacterConditionId
import com.dubl.character.android.model.ConditionLocalDataCodec
import com.dubl.character.android.model.CharacterNoteDataCodec
import com.dubl.character.android.model.CharacterSheetExtras
import com.dubl.character.android.model.CharacterSheetResourceId
import com.dubl.character.android.model.SheetGroup
import com.dubl.character.android.model.SheetGroupingRules
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

class DesktopCharacterExtrasStore(
    private val file: Path = DesktopCharacterStore.defaultDataDirectory().resolve("sheet-extras.json"),
) : CharacterExtrasStore {
    private var cache: MutableMap<String, CharacterSheetExtras>? = null

    override fun load(characterId: String): CharacterSheetExtras = ensureLoaded()[characterId] ?: CharacterSheetExtras()

    override fun save(characterId: String, extras: CharacterSheetExtras) {
        val state = ensureLoaded()
        state[characterId] = extras
        persist(state)
    }

    override fun delete(characterId: String) {
        val state = ensureLoaded()
        if (state.remove(characterId) != null) persist(state)
    }

    private fun ensureLoaded(): MutableMap<String, CharacterSheetExtras> {
        cache?.let { return it }
        val loaded = if (!Files.exists(file)) mutableMapOf() else runCatching {
            decode(Files.readString(file, StandardCharsets.UTF_8)).toMutableMap()
        }.getOrDefault(mutableMapOf())
        cache = loaded
        return loaded
    }

    private fun persist(state: Map<String, CharacterSheetExtras>) {
        Files.createDirectories(file.parent)
        val temp = file.resolveSibling("${file.fileName}.tmp")
        Files.writeString(temp, encode(state), StandardCharsets.UTF_8)
        runCatching {
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        }.getOrElse {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun encode(state: Map<String, CharacterSheetExtras>): String = buildString {
        append("{\"version\":1,\"characters\":{")
        state.entries.forEachIndexed { index, (id, extras) ->
            if (index > 0) append(',')
            append(quoted(id)).append(':').append('{')
            append("\"portraitUri\":").append(extras.portraitUri?.let(::quoted) ?: "null")
            append(",\"conditions\":").append(stringArray(extras.activeConditions.map { it.name }))
            append(",\"hiddenResources\":").append(stringArray(extras.hiddenResourceIds.map { it.name }))
            append(",\"preferredAttributes\":{")
            extras.preferredSkillAttributes.entries.forEachIndexed { i, entry ->
                if (i > 0) append(',')
                append(quoted(entry.key)).append(':').append(quoted(entry.value.name))
            }
            append('}')
            append(",\"skillGroups\":").append(quoted(SheetGroupingRules.encode(extras.skillGroups)))
            append(",\"developmentGroups\":").append(quoted(SheetGroupingRules.encode(extras.developmentGroups)))
            append(",\"conditionOverrides\":").append(quoted(ConditionLocalDataCodec.encodeOverrides(extras.conditionOverrides)))
            append(",\"customConditions\":").append(quoted(ConditionLocalDataCodec.encodeCustom(extras.customConditions)))
            append(",\"notes\":").append(quoted(extras.notes))
            append(",\"noteEntries\":").append(quoted(CharacterNoteDataCodec.encode(extras.noteEntries)))
            append('}')
        }
        append("}}")
    }

    private fun decode(raw: String): Map<String, CharacterSheetExtras> {
        // Reuse the stable grouping encoding inside a deliberately small object decoder.
        val root = parseDesktopObject(raw)
        val characters = root.objects["characters"] ?: return emptyMap()
        return characters.objects.mapValues { (_, objectValue) ->
            CharacterSheetExtras(
                portraitUri = objectValue.strings["portraitUri"],
                activeConditions = objectValue.arrays["conditions"].orEmpty().mapNotNull { name -> CharacterConditionId.entries.firstOrNull { it.name == name } }.toSet(),
                hiddenResourceIds = objectValue.arrays["hiddenResources"].orEmpty().mapNotNull { name -> CharacterSheetResourceId.entries.firstOrNull { it.name == name } }.toSet(),
                preferredSkillAttributes = objectValue.objects["preferredAttributes"]?.strings.orEmpty().mapNotNull { (skillId, attrName) ->
                    AttributeId.entries.firstOrNull { it.name == attrName }?.let { skillId to it }
                }.toMap(),
                skillGroups = SheetGroupingRules.decode(objectValue.strings["skillGroups"]),
                developmentGroups = SheetGroupingRules.decode(objectValue.strings["developmentGroups"]),
                conditionOverrides = ConditionLocalDataCodec.decodeOverrides(objectValue.strings["conditionOverrides"]),
                customConditions = ConditionLocalDataCodec.decodeCustom(objectValue.strings["customConditions"]),
                notes = objectValue.strings["notes"].orEmpty(),
                noteEntries = CharacterNoteDataCodec.decode(objectValue.strings["noteEntries"]),
            )
        }
    }

    private data class DesktopObject(
        val strings: MutableMap<String, String?> = linkedMapOf(),
        val arrays: MutableMap<String, List<String>> = linkedMapOf(),
        val objects: MutableMap<String, DesktopObject> = linkedMapOf(),
    )

    private fun parseDesktopObject(raw: String): DesktopObject {
        // This parser only consumes the exact format emitted above and safely falls back to empty state.
        var i = 0
        fun skip() { while (i < raw.length && raw[i].isWhitespace()) i++ }
        fun readString(): String {
            require(raw[i++] == '"')
            val out = StringBuilder()
            while (i < raw.length) {
                val c = raw[i++]
                if (c == '"') return out.toString()
                if (c == '\\') {
                    val e = raw[i++]
                    out.append(when (e) { 'n' -> '\n'; 'r' -> '\r'; 't' -> '\t'; else -> e })
                } else out.append(c)
            }
            error("unterminated")
        }
        fun parseArray(): List<String> {
            require(raw[i++] == '['); skip()
            val values = mutableListOf<String>()
            if (raw.getOrNull(i) == ']') { i++; return values }
            while (true) {
                skip(); values += readString(); skip()
                when (raw[i++]) { ']' -> return values; ',' -> Unit; else -> error("array") }
            }
        }
        lateinit var parseObject: () -> DesktopObject
        parseObject = {
            skip(); require(raw[i++] == '{'); skip()
            val result = DesktopObject()
            if (raw.getOrNull(i) == '}') { i++; result } else {
                while (true) {
                    skip(); val key = readString(); skip(); require(raw[i++] == ':'); skip()
                    when (raw.getOrNull(i)) {
                        '"' -> result.strings[key] = readString()
                        '[' -> result.arrays[key] = parseArray()
                        '{' -> result.objects[key] = parseObject()
                        'n' -> { require(raw.substring(i, (i + 4).coerceAtMost(raw.length)) == "null"); i += 4; result.strings[key] = null }
                        else -> { while (i < raw.length && raw[i] !in charArrayOf(',', '}')) i++ }
                    }
                    skip(); when (raw[i++]) { '}' -> break; ',' -> Unit; else -> error("object") }
                }
                result
            }
        }
        return parseObject()
    }

    private fun stringArray(values: Iterable<String>): String = values.joinToString(prefix = "[", postfix = "]", separator = ",", transform = ::quoted)
    private fun quoted(value: String): String = buildString {
        append('"')
        value.forEach { c -> when (c) { '\\' -> append("\\\\"); '"' -> append("\\\""); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t"); else -> append(c) } }
        append('"')
    }
}
