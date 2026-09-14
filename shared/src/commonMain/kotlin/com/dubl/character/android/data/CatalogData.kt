package com.dubl.character.android.data

import com.dubl.character.android.model.AbilityOption
import com.dubl.character.android.model.ChiCatalog
import com.dubl.character.android.model.ChiSchool
import com.dubl.character.android.model.ChiTechnique
import com.dubl.character.android.model.DevelopmentCatalog
import com.dubl.character.android.model.DevelopmentCostType
import com.dubl.character.android.model.DevelopmentEntry
import com.dubl.character.android.model.GearCatalogEntry
import com.dubl.character.android.model.MagicEquipmentCatalog
import com.dubl.character.android.model.RollContext
import com.dubl.character.android.model.SkillEffectCatalog
import com.dubl.character.android.model.SkillEffectDefinition
import com.dubl.character.android.model.SkillEffectMode
import com.dubl.character.android.model.SpellCatalogEntry


fun parseDevelopmentCatalog(raw: String): DevelopmentCatalog {
    val root = parseRoot(raw)
    val entries = root.array("entries").mapNotNull { value ->
        val item = value.asObject() ?: return@mapNotNull null
        DevelopmentEntry(
            id = item.string("id"),
            name = item.string("name", "Навык"),
            section = item.string("section", "Навыки"),
            category = item.string("category", "Общие"),
            cost = item.int("cost", 0),
            costType = if (item.string("costType", "xp") == "ability") DevelopmentCostType.ABILITY else DevelopmentCostType.XP,
            maxRank = item.int("ranks", 1).coerceAtLeast(1),
            requirements = item.string("requirements"),
            benefit = item.string("benefit"),
            notes = item.string("notes"),
            tags = item.stringList("tags"),
            accessId = item.string("accessId").takeIf(String::isNotBlank),
            abilityOptions = item.array("abilityOptions").mapNotNull option@ { optionValue ->
                val option = optionValue.asObject() ?: return@option null
                AbilityOption(option.string("source", "Источник"), option.int("value", 0).coerceAtLeast(0))
            },
            incomplete = item.bool("incomplete"),
            repeatable = item.bool("repeatable"),
            perfectRoot = item.bool("perfectRoot"),
            mechanicsConflict = item.string("mechanicsConflict"),
            conflictNote = item.string("conflictNote"),
        )
    }
    return DevelopmentCatalog(root.string("version", "desktop"), entries)
}

fun parseChiCatalog(raw: String): ChiCatalog {
    val root = parseRoot(raw)
    val schools = root.array("schools").mapNotNull { value ->
        val item = value.asObject() ?: return@mapNotNull null
        ChiSchool(
            id = item.string("id"),
            name = item.string("name", "Школа ЦИ"),
            requirements = item.string("requirements"),
            passives = item.stringList("passives"),
        )
    }
    val techniques = root.array("techniques").mapNotNull { value ->
        val item = value.asObject() ?: return@mapNotNull null
        ChiTechnique(
            id = item.string("id"),
            name = item.string("name", "Приём ЦИ"),
            school = item.string("school", "Общие приёмы"),
            chiCost = item.int("chiCost", 0).coerceAtLeast(0),
            action = item.string("action"),
            effect = item.string("effect"),
            requirements = item.string("requirements", "Внутренняя Ци"),
        )
    }
    return ChiCatalog(root.string("version", "0.5"), schools, techniques)
}

fun parseMagicEquipmentCatalog(raw: String): MagicEquipmentCatalog {
    val root = parseRoot(raw)
    val spells = root.array("spells").mapNotNull { value ->
        val item = value.asObject() ?: return@mapNotNull null
        SpellCatalogEntry(
            id = item.string("id"),
            name = item.string("name", "Заклинание"),
            school = item.string("school", item.string("category")),
            cost = item.int("cost", 0).coerceAtLeast(0),
            manaText = item.string("manaText", item.int("cost", 0).toString()),
            time = item.string("time"),
            range = item.string("range"),
            area = item.string("area"),
            action = item.string("action"),
            duration = item.string("duration"),
            description = item.string("description"),
            enhancement = item.string("enhancement"),
            incomplete = item.bool("incomplete"),
            conflictNote = item.string("conflictNote"),
        )
    }
    val gear = root.array("gear").mapNotNull { value ->
        val item = value.asObject() ?: return@mapNotNull null
        GearCatalogEntry(
            id = item.string("id"),
            name = item.string("name", "Предмет"),
            category = item.string("category", "Снаряжение"),
            section = item.string("section", "Предметы"),
            fields = item.stringMap("fields"),
            description = item.string("description"),
        )
    }
    return MagicEquipmentCatalog(root.string("version", "desktop"), spells, gear)
}

fun parseSkillEffectCatalog(raw: String): SkillEffectCatalog {
    val root = parseRoot(raw)
    val effects = root.array("effects").mapIndexedNotNull { index, value ->
        val item = value.asObject() ?: return@mapIndexedNotNull null
        val mode = when (item.string("mode")) {
            "auto_bonus" -> SkillEffectMode.AUTO_BONUS
            "toggle_bonus" -> SkillEffectMode.TOGGLE_BONUS
            "toggle_advantage" -> SkillEffectMode.TOGGLE_ADVANTAGE
            "toggle_hindrance" -> SkillEffectMode.TOGGLE_HINDRANCE
            "toggle_rule" -> SkillEffectMode.TOGGLE_RULE
            "alternative" -> SkillEffectMode.ALTERNATIVE
            "preset" -> SkillEffectMode.PRESET
            "reroll" -> SkillEffectMode.REROLL
            "formula" -> SkillEffectMode.FORMULA
            else -> SkillEffectMode.REMINDER
        }
        SkillEffectDefinition(
            id = item.string("id"),
            reviewIndex = item.int("reviewIndex", index + 1),
            sourceName = item.string("sourceName"),
            effectText = item.string("effectText"),
            targetText = item.string("targetText"),
            status = item.string("status"),
            plan = item.string("plan"),
            mode = mode,
            rollContext = item.string("rollContext").takeIf(String::isNotBlank)?.let { rawContext ->
                RollContext.entries.firstOrNull { it.name == rawContext }
            },
            targetSkills = item.stringList("targetSkills"),
            value = item.int("value", 0),
            perRank = item.bool("perRank"),
            toggleLabel = item.string("toggleLabel", item.string("sourceName")),
        )
    }
    return SkillEffectCatalog(root.string("version", "0.5"), effects)
}
