package com.dubl.character.android.data

import com.dubl.character.android.model.AppSnapshot
import com.dubl.character.android.model.AttributeId
import com.dubl.character.android.model.AttributeValue
import com.dubl.character.android.model.CharacterGear
import com.dubl.character.android.model.CharacterMagic
import com.dubl.character.android.model.CharacterSkill
import com.dubl.character.android.model.CustomResource
import com.dubl.character.android.model.DublCharacter
import com.dubl.character.android.model.DublRuleset
import com.dubl.character.android.model.RulesetRef
import com.dubl.character.android.model.GearItem
import com.dubl.character.android.model.KnownSpell
import com.dubl.character.android.model.MagicSchool
import com.dubl.character.android.model.OwnedDevelopment
import com.dubl.character.android.model.UntrainedRule
import com.dubl.character.android.model.defaultAttributes

object SnapshotCodec {
    const val SCHEMA = 8

    fun fresh(idFactory: () -> String): AppSnapshot {
        val character = DublCharacter(id = idFactory(), name = "Новый персонаж")
        return AppSnapshot(listOf(character), character.id)
    }

    fun encode(snapshot: AppSnapshot): String = jsonStringify(
        jsonObject(
            "schema" to jsonNumber(SCHEMA),
            "activeCharacterId" to jsonString(snapshot.activeCharacterId),
            "characters" to jsonArray(snapshot.characters.map(::encodeCharacter)),
        ),
    )

    fun decode(raw: String, idFactory: () -> String): AppSnapshot {
        val root = parseRoot(raw)
        val schema = root.int("schema", 1)
        val characters = root.array("characters").mapNotNull { item ->
            item.asObject()?.let { decodeCharacter(it, schema, idFactory) }
        }
        if (characters.isEmpty()) return fresh(idFactory)
        val requested = root.string("activeCharacterId")
        val active = characters.firstOrNull { it.id == requested }?.id ?: characters.first().id
        return AppSnapshot(characters, active)
    }

    private fun encodeCharacter(character: DublCharacter): JsonValue.Obj = jsonObject(
        "id" to jsonString(character.id),
        "ruleset" to jsonObject(
            "id" to jsonString(character.ruleset.id),
            "version" to jsonString(character.ruleset.version),
        ),
        "name" to jsonString(character.name),
        "concept" to jsonString(character.concept),
        "experience" to jsonNumber(character.experience),
        "creationExperience" to jsonNumber(character.creationExperience),
        "creationComplete" to jsonBoolean(character.creationComplete),
        "xpAdjustment" to jsonNumber(character.xpAdjustment),
        "abilityPointsOverride" to character.abilityPointsOverride?.let(::jsonNumber),
        "size" to jsonNumber(character.size),
        "legs" to jsonNumber(character.legs),
        "hpCurrent" to jsonNumber(character.hpCurrent),
        "enduranceCurrent" to jsonNumber(character.enduranceCurrent),
        "manaEnabled" to jsonBoolean(character.manaEnabled),
        "manaCurrent" to jsonNumber(character.manaCurrent),
        "manaMaximum" to jsonNumber(character.manaMaximum),
        "chiEnabled" to jsonBoolean(character.chiEnabled),
        "chiCurrent" to jsonNumber(character.chiCurrent),
        "chiBonusRanks" to jsonNumber(character.chiBonusRanks),
        "healthMaximumOverride" to character.healthMaximumOverride?.let(::jsonNumber),
        "enduranceMaximumOverride" to character.enduranceMaximumOverride?.let(::jsonNumber),
        "manaMaximumOverride" to character.manaMaximumOverride?.let(::jsonNumber),
        "customResources" to jsonArray(character.customResources.map { resource ->
            jsonObject(
                "uid" to jsonString(resource.uid),
                "name" to jsonString(resource.name),
                "current" to jsonNumber(resource.current),
                "maximum" to jsonNumber(resource.maximum),
            )
        }),
        "attributes" to JsonValue.Obj(linkedMapOf<String, JsonValue>().apply {
            character.attributes.forEach { (id, value) ->
                put(id.name, jsonObject("base" to jsonNumber(value.base), "bonus" to jsonNumber(value.bonus)))
            }
        }),
        "skills" to jsonArray(character.skills.values.map(::encodeSkill)),
        "hiddenSkillIds" to jsonArray(character.hiddenSkillIds.map(::jsonString)),
        "development" to jsonArray(character.development.map { (id, owned) ->
            jsonObject(
                "id" to jsonString(id),
                "rank" to jsonNumber(owned.rank),
                "option" to jsonNumber(owned.optionIndex),
            )
        }),
        "magic" to encodeMagic(character.magic),
        "gear" to encodeGear(character.gear),
    )

    private fun encodeSkill(skill: CharacterSkill): JsonValue.Obj = jsonObject(
        "id" to jsonString(skill.id),
        "definitionId" to skill.definitionId?.let(::jsonString),
        "name" to jsonString(skill.name),
        "description" to jsonString(skill.description),
        "rank" to jsonNumber(skill.rank),
        "attributes" to jsonArray(skill.attributes.map { jsonString(it.name) }),
        "modifier" to jsonNumber(skill.modifier),
        "formulaNote" to jsonString(skill.formulaNote),
        "untrainedOverride" to skill.untrainedOverride?.name?.let(::jsonString),
    )

    private fun encodeMagic(magic: CharacterMagic): JsonValue.Obj = jsonObject(
        "manaRank" to jsonNumber(magic.manaRank),
        "power" to jsonNumber(magic.power),
        "schools" to jsonArray(magic.schools.map { school ->
            jsonObject(
                "name" to jsonString(school.name),
                "rank" to jsonNumber(school.rank),
                "note" to jsonString(school.note),
            )
        }),
        "spells" to jsonArray(magic.spells.map { spell ->
            jsonObject(
                "uid" to jsonString(spell.uid),
                "catalogId" to spell.catalogId?.let(::jsonString),
                "name" to jsonString(spell.name),
                "school" to jsonString(spell.school),
                "cost" to jsonNumber(spell.cost),
                "manaText" to jsonString(spell.manaText),
                "time" to jsonString(spell.time),
                "range" to jsonString(spell.range),
                "area" to jsonString(spell.area),
                "action" to jsonString(spell.action),
                "duration" to jsonString(spell.duration),
                "description" to jsonString(spell.description),
                "enhancement" to jsonString(spell.enhancement),
                "learned" to jsonBoolean(spell.learned),
                "xpOverride" to spell.xpOverride?.let(::jsonNumber),
                "incomplete" to jsonBoolean(spell.incomplete),
                "conflictNote" to jsonString(spell.conflictNote),
                "custom" to jsonBoolean(spell.custom),
            )
        }),
    )

    private fun encodeGear(gear: CharacterGear): JsonValue.Obj = jsonObject(
        "loadAutomatic" to jsonBoolean(gear.loadAutomatic),
        "loadManual" to jsonNumber(gear.loadManual),
        "items" to jsonArray(gear.items.map { item ->
            jsonObject(
                "uid" to jsonString(item.uid),
                "catalogId" to item.catalogId?.let(::jsonString),
                "name" to jsonString(item.name),
                "quantity" to jsonNumber(item.quantity),
                "load" to jsonNumber(item.load),
                "carried" to jsonBoolean(item.carried),
                "description" to jsonString(item.description),
                "category" to jsonString(item.category),
                "section" to jsonString(item.section),
                "custom" to jsonBoolean(item.custom),
                "fields" to JsonValue.Obj(item.fields.mapValues { jsonString(it.value) }),
            )
        }),
    )

    private fun decodeCharacter(root: JsonValue.Obj, schema: Int, idFactory: () -> String): DublCharacter {
        val attributes = defaultAttributes().toMutableMap()
        val jsonAttributes = root.objectValue("attributes")
        AttributeId.entries.forEach { id ->
            jsonAttributes?.objectValue(id.name)?.let { value ->
                attributes[id] = AttributeValue(value.int("base", 0), value.int("bonus", 0))
            }
        }

        val skills = linkedMapOf<String, CharacterSkill>()
        root.array("skills").forEach { item ->
            item.asObject()?.let(::decodeSkill)?.let { skills[it.id] = it }
        }
        val hidden = root.array("hiddenSkillIds").mapNotNull { it.asString()?.takeIf(String::isNotBlank) }.toCollection(linkedSetOf())
        val development = linkedMapOf<String, OwnedDevelopment>()
        root.array("development").forEach { value ->
            val item = value.asObject() ?: return@forEach
            val id = item.string("id").trim()
            val rank = item.int("rank", 0)
            if (id.isNotBlank() && rank > 0) development[id] = OwnedDevelopment(rank, item.int("option", 0).coerceAtLeast(0))
        }
        val customResources = root.array("customResources").mapNotNull { value ->
            val item = value.asObject() ?: return@mapNotNull null
            CustomResource(
                uid = item.string("uid").ifBlank(idFactory),
                name = item.string("name", "Ресурс"),
                current = item.int("current", 0),
                maximum = item.int("maximum", 0),
            )
        }
        val experience = root.int("experience", 0).coerceAtLeast(0)
        val creationExperience = if (root.has("creationExperience")) root.int("creationExperience", experience).coerceAtLeast(0) else experience
        val creationComplete = if (root.has("creationComplete")) root.bool("creationComplete", false) else schema < 5

        val rulesetRoot = root.objectValue("ruleset")
        val ruleset = if (rulesetRoot == null) {
            DublRuleset.reference
        } else {
            RulesetRef(
                id = rulesetRoot.string("id", DublRuleset.ID).ifBlank { DublRuleset.ID },
                version = rulesetRoot.string("version", DublRuleset.VERSION).ifBlank { DublRuleset.VERSION },
            )
        }

        return DublCharacter(
            id = root.string("id").ifBlank(idFactory),
            ruleset = ruleset,
            name = root.string("name", "Новый персонаж"),
            concept = root.string("concept"),
            experience = experience,
            creationExperience = creationExperience,
            creationComplete = creationComplete,
            xpAdjustment = root.int("xpAdjustment", 0),
            abilityPointsOverride = root.intOrNull("abilityPointsOverride")?.coerceAtLeast(0),
            size = root.int("size", 5),
            legs = root.int("legs", 2),
            attributes = attributes,
            hpCurrent = root.int("hpCurrent", 0),
            enduranceCurrent = root.int("enduranceCurrent", 3),
            manaEnabled = root.bool("manaEnabled", false),
            manaCurrent = root.int("manaCurrent", 0),
            manaMaximum = root.int("manaMaximum", 0),
            chiEnabled = root.bool("chiEnabled", false),
            chiCurrent = root.int("chiCurrent", 0),
            chiBonusRanks = root.int("chiBonusRanks", 0),
            healthMaximumOverride = root.intOrNull("healthMaximumOverride"),
            enduranceMaximumOverride = root.intOrNull("enduranceMaximumOverride"),
            manaMaximumOverride = root.intOrNull("manaMaximumOverride"),
            customResources = customResources,
            skills = skills,
            hiddenSkillIds = hidden,
            development = development,
            magic = decodeMagic(root.objectValue("magic"), idFactory),
            gear = decodeGear(root.objectValue("gear"), idFactory),
        ).normalized()
    }

    private fun decodeSkill(root: JsonValue.Obj): CharacterSkill? {
        val id = root.string("id").ifBlank { return null }
        val attrs = root.array("attributes").mapNotNull { value ->
            val raw = value.asString() ?: return@mapNotNull null
            AttributeId.entries.firstOrNull { it.name == raw }
        }.distinct()
        val untrained = root.string("untrainedOverride").takeIf(String::isNotBlank)?.let { raw ->
            UntrainedRule.entries.firstOrNull { it.name == raw }
        }
        return CharacterSkill(
            id = id,
            definitionId = root.string("definitionId").takeIf(String::isNotBlank),
            name = root.string("name"),
            description = root.string("description"),
            rank = root.int("rank", 0),
            attributes = attrs,
            modifier = root.int("modifier", 0),
            formulaNote = root.string("formulaNote"),
            untrainedOverride = untrained,
        )
    }

    private fun decodeMagic(root: JsonValue.Obj?, idFactory: () -> String): CharacterMagic {
        if (root == null) return CharacterMagic()
        val schools = root.array("schools").mapNotNull { value ->
            val item = value.asObject() ?: return@mapNotNull null
            MagicSchool(item.string("name", "Школа"), item.int("rank", item.int("level", 0)), item.string("note"))
        }
        val spells = root.array("spells").mapNotNull { value ->
            val item = value.asObject() ?: return@mapNotNull null
            KnownSpell(
                uid = item.string("uid").ifBlank(idFactory),
                catalogId = item.string("catalogId").takeIf(String::isNotBlank),
                name = item.string("name", "Заклинание"),
                school = item.string("school"),
                cost = item.int("cost", 0),
                manaText = item.string("manaText"),
                time = item.string("time"),
                range = item.string("range"),
                area = item.string("area"),
                action = item.string("action"),
                duration = item.string("duration"),
                description = item.string("description"),
                enhancement = item.string("enhancement"),
                learned = item.bool("learned", true),
                xpOverride = item.intOrNull("xpOverride"),
                incomplete = item.bool("incomplete", false),
                conflictNote = item.string("conflictNote"),
                custom = item.bool("custom", false),
            )
        }
        return CharacterMagic(root.int("manaRank", 0), root.int("power", 0), schools, spells)
    }

    private fun decodeGear(root: JsonValue.Obj?, idFactory: () -> String): CharacterGear {
        if (root == null) return CharacterGear()
        val items = root.array("items").mapNotNull { value ->
            val item = value.asObject() ?: return@mapNotNull null
            GearItem(
                uid = item.string("uid").ifBlank(idFactory),
                catalogId = item.string("catalogId").takeIf(String::isNotBlank),
                name = item.string("name", "Предмет"),
                quantity = item.int("quantity", item.int("qty", 1)),
                load = item.double("load", 0.0),
                carried = item.bool("carried", true),
                description = item.string("description"),
                category = item.string("category", "Снаряжение"),
                section = item.string("section", "Предметы"),
                fields = item.stringMap("fields"),
                custom = item.bool("custom", false),
            )
        }
        return CharacterGear(root.bool("loadAutomatic", true), root.double("loadManual", 0.0), items)
    }
}

private fun JsonValue.Obj.intOrNull(key: String): Int? {
    if (!has(key) || isNull(key)) return null
    return when (val value = values[key]) {
        is JsonValue.Num -> value.raw.toDoubleOrNull()?.toInt()
        is JsonValue.Str -> value.value.toIntOrNull()
        else -> null
    }
}
