package com.dubl.character.android.model

enum class AttributeId(val title: String, val shortTitle: String) {
    STRENGTH("Сила", "СИЛ"),
    DEXTERITY("Ловкость", "ЛОВ"),
    CONSTITUTION("Телосложение", "ТЕЛ"),
    SPEED("Скорость", "СКР"),
    INTELLIGENCE("Интеллект", "ИНТ"),
    PERCEPTION("Восприятие", "ВОС"),
    WILL("Воля", "ВОЛ"),
    CHARISMA("Харизма", "ХАР"),
}

data class AttributeValue(
    val base: Int = 0,
    val bonus: Int = 0,
) {
    val total: Int get() = base + bonus
}

data class CustomResource(
    val uid: String,
    val name: String = "Ресурс",
    val current: Int = 0,
    val maximum: Int = 0,
)

data class DublCharacter(
    val id: String,
    val name: String = "Новый персонаж",
    val concept: String = "",
    val experience: Int = 0,
    val creationExperience: Int = 0,
    val creationComplete: Boolean = false,
    val xpAdjustment: Int = 0,
    val abilityPointsOverride: Int? = null,
    val size: Int = 5,
    val legs: Int = 2,
    val attributes: Map<AttributeId, AttributeValue> = defaultAttributes(),
    val hpCurrent: Int = 0,
    val enduranceCurrent: Int = 3,
    val manaEnabled: Boolean = false,
    val manaCurrent: Int = 0,
    val manaMaximum: Int = 0,
    val chiEnabled: Boolean = false,
    val chiCurrent: Int = 0,
    val chiBonusRanks: Int = 0,
    val healthMaximumOverride: Int? = null,
    val enduranceMaximumOverride: Int? = null,
    val manaMaximumOverride: Int? = null,
    val customResources: List<CustomResource> = emptyList(),
    val skills: Map<String, CharacterSkill> = emptyMap(),
    val hiddenSkillIds: Set<String> = emptySet(),
    val development: Map<String, OwnedDevelopment> = emptyMap(),
    val magic: CharacterMagic = CharacterMagic(),
    val gear: CharacterGear = CharacterGear(),
) {
    fun attributeRaw(id: AttributeId): Int = attributes[id]?.total ?: 0

    fun attribute(id: AttributeId): Int = when (id) {
        AttributeId.STRENGTH -> attributeRaw(id) + (size - 5)
        AttributeId.SPEED -> attributeRaw(id) + (5 - size)
        else -> attributeRaw(id)
    }

    val strength: Int get() = attribute(AttributeId.STRENGTH)
    val dexterity: Int get() = attribute(AttributeId.DEXTERITY)
    val constitution: Int get() = attribute(AttributeId.CONSTITUTION)
    val speed: Int get() = attribute(AttributeId.SPEED)
    val perception: Int get() = attribute(AttributeId.PERCEPTION)
    val will: Int get() = attribute(AttributeId.WILL)

    val equipmentLoadPenalty: Int get() = MagicEquipmentRules.burden(this).penalty
    val defense: Int get() = 10 - size + speed + dexterity + equipmentLoadPenalty
    val incredibleHealthBonus: Int
        get() = developmentRank(DevelopmentEffectIds.INCREDIBLE_HEALTH) * incredibleHealthPerRank(size)
    val calculatedHealthMaximum: Int get() = (constitution * size + strength + incredibleHealthBonus).coerceAtLeast(0)
    val healthMaximum: Int get() = healthMaximumOverride ?: calculatedHealthMaximum
    val enduranceMaximum: Int
        get() = enduranceMaximumOverride ?: (3 + developmentRank(DevelopmentEffectIds.ENDURING))
    val reflexes: Int
        get() = speed + dexterity + equipmentLoadPenalty + developmentRank(DevelopmentEffectIds.QUICK_REFLEXES)
    val initiative: Int
        get() = speed + perception + developmentRank(DevelopmentEffectIds.IMPROVED_INITIATIVE) +
            developmentRank(DevelopmentEffectIds.STORM_LORD_SCHOOL)
    val fortitude: Int
        get() = constitution + will + developmentRank(DevelopmentEffectIds.STALWART) +
            developmentRank(DevelopmentEffectIds.STILL_MOUNTAIN_SCHOOL)
    val effectiveCreationExperience: Int
        get() = when {
            creationExperience > 0 -> creationExperience.coerceAtMost(experience.coerceAtLeast(0))
            !creationComplete -> experience.coerceAtLeast(0)
            else -> 0
        }
    val recommendedAbilityPoints: Int get() = effectiveCreationExperience / 1000
    val abilityPoints: Int get() = abilityPointsOverride ?: recommendedAbilityPoints
    val effectiveManaMaximum: Int
        get() = manaMaximumOverride ?: if (magic.manaRank > 0) MagicEquipmentRules.manaMaximum(this) else manaMaximum.coerceAtLeast(0)
    val chiActive: Boolean
        get() = chiEnabled || developmentRank(DevelopmentEffectIds.INTERNAL_CHI) > 0
    val chiMaximum: Int
        get() = if (chiActive) {
            maxOf(3, will + 1) +
                chiBonusRanks.coerceIn(0, 10) +
                developmentRank(DevelopmentEffectIds.MASTER_CHI) * 2 +
                developmentRank(DevelopmentEffectIds.AWAKENED_CHI) * 3
        } else 0

    val runBase: Double
        get() = if (legs >= 3) {
            when (size.coerceIn(1, 10)) {
                1 -> 2.0
                2 -> 4.0
                3 -> 4.0
                4 -> 8.0
                5 -> 12.0
                6 -> 20.0
                7 -> 32.0
                8 -> 40.0
                9 -> 56.0
                else -> 80.0
            }
        } else {
            when (size.coerceIn(1, 10)) {
                1 -> 1.0
                2 -> 2.0
                3 -> 4.0
                4 -> 6.0
                5 -> 8.0
                6 -> 12.0
                7 -> 18.0
                8 -> 24.0
                9 -> 32.0
                else -> 60.0
            }
        }

    val runFull: Double
        get() {
            val multiplier = if (legs >= 3) {
                when (size.coerceIn(1, 10)) {
                    1 -> 0.5
                    2 -> 1.0
                    3 -> 1.5
                    4, 5, 6 -> 2.0
                    7 -> 3.0
                    8 -> 4.0
                    9 -> 5.0
                    else -> 6.0
                }
            } else {
                when (size.coerceIn(1, 10)) {
                    1 -> 0.125
                    2 -> 0.25
                    3 -> 0.5
                    4, 5 -> 1.0
                    6, 7 -> 1.5
                    8 -> 2.0
                    9 -> 3.0
                    else -> 4.0
                }
            }
            val runSpeed = speed + developmentRank(DevelopmentEffectIds.STORM_LORD_SCHOOL)
            return (runBase + runSpeed * multiplier + equipmentLoadPenalty + developmentRank(DevelopmentEffectIds.RUNNER)).coerceAtLeast(0.0)
        }

    fun normalized(): DublCharacter {
        val normalizedSkills = skills.mapValues { (_, skill) ->
            skill.copy(
                rank = skill.rank.coerceIn(0, 10),
                attributes = skill.attributes.distinct(),
            )
        }
        val normalizedAttributes = attributes.mapValues { (_, value) ->
            value.copy(base = value.base.coerceIn(-5, 10))
        }
        val legacyManaRank = development[MagicEquipmentRules.BASE_MANA_ENTRY_ID]?.rank?.coerceIn(0, 5) ?: 0
        val normalizedManaRank = maxOf(magic.manaRank.coerceIn(0, 5), legacyManaRank)
        val normalizedSchools = magic.schools
            .mapNotNull { school ->
                val cleanName = MagicSchoolCatalog.canonicalizeOrNull(school.name) ?: school.name.trim().ifBlank { return@mapNotNull null }
                MagicSchool(cleanName, school.rank.coerceAtLeast(0), school.note.trim())
            }
            .groupBy { MagicSchoolCatalog.canonicalizeOrNull(it.name) ?: it.name.lowercase() }
            .map { (_, schools) -> schools.maxBy { it.rank } }
            .sortedWith(compareBy<MagicSchool> { MagicSchoolCatalog.sortIndex(it.name) }.thenBy { it.name.lowercase() })
        val normalizedMagic = magic.copy(
            manaRank = normalizedManaRank,
            power = if (normalizedSchools.isEmpty()) magic.power.coerceAtLeast(0) else 0,
            schools = normalizedSchools,
            spells = magic.spells.map { spell ->
                spell.copy(
                    name = spell.name.trim().ifBlank { "Заклинание" },
                    school = spell.school.trim(),
                    cost = spell.cost.coerceAtLeast(0),
                    learned = spell.learned,
                    xpOverride = spell.xpOverride?.coerceAtLeast(0),
                )
            }.distinctBy { it.uid },
        )
        val normalizedGear = gear.copy(
            loadManual = gear.loadManual.coerceAtLeast(0.0),
            items = gear.items.map { item ->
                item.copy(
                    name = item.name.trim().ifBlank { "Предмет" },
                    quantity = item.quantity.coerceAtLeast(1),
                    load = item.load.coerceAtLeast(0.0),
                )
            }.distinctBy { it.uid },
        )
        val normalizedExperience = experience.coerceAtLeast(0)
        val normalizedCreationExperience = creationExperience.coerceAtLeast(0).coerceAtMost(normalizedExperience)
        val normalizedCustomResources = customResources.mapNotNull { resource ->
            val cleanName = resource.name.trim().replace(Regex("\\s+"), " ")
            if (resource.uid.isBlank() || cleanName.isBlank()) return@mapNotNull null
            val max = resource.maximum.coerceAtLeast(0)
            resource.copy(name = cleanName, maximum = max, current = resource.current.coerceIn(0, max))
        }.distinctBy { it.uid }
        val clamped = copy(
            experience = normalizedExperience,
            creationExperience = normalizedCreationExperience,
            xpAdjustment = xpAdjustment.coerceIn(-1_000_000, 1_000_000),
            abilityPointsOverride = abilityPointsOverride?.coerceAtLeast(0),
            size = size.coerceIn(1, 10),
            attributes = normalizedAttributes,
            legs = legs.coerceAtLeast(2),
            healthMaximumOverride = healthMaximumOverride?.coerceAtLeast(0),
            enduranceMaximumOverride = enduranceMaximumOverride?.coerceAtLeast(0),
            manaMaximumOverride = manaMaximumOverride?.coerceAtLeast(0),
            manaMaximum = manaMaximum.coerceAtLeast(0),
            chiBonusRanks = chiBonusRanks.coerceIn(0, 10),
            customResources = normalizedCustomResources,
            skills = normalizedSkills,
            hiddenSkillIds = hiddenSkillIds.filterTo(linkedSetOf()) { id ->
                SkillCatalog.builtIns.any { it.id == id } || normalizedSkills.containsKey(id)
            },
            development = development.mapNotNull { (id, owned) ->
                val rank = owned.rank.coerceAtLeast(0)
                if (rank == 0 || id.isBlank() || id == MagicEquipmentRules.BASE_MANA_ENTRY_ID) null else id to owned.copy(
                    rank = rank,
                    optionIndex = owned.optionIndex.coerceAtLeast(0),
                )
            }.toMap(linkedMapOf()),
            magic = normalizedMagic,
            gear = normalizedGear,
        )
        val maxMana = clamped.effectiveManaMaximum
        val maxChi = clamped.chiMaximum
        return clamped.copy(
            hpCurrent = clamped.hpCurrent.coerceIn(0, clamped.healthMaximum),
            enduranceCurrent = clamped.enduranceCurrent.coerceIn(0, clamped.enduranceMaximum),
            manaCurrent = clamped.manaCurrent.coerceIn(0, maxMana),
            manaEnabled = clamped.manaEnabled || clamped.magic.manaRank > 0 || clamped.manaMaximumOverride != null,
            manaMaximum = clamped.manaMaximum.coerceAtLeast(0),
            chiCurrent = clamped.chiCurrent.coerceIn(0, maxChi),
        )
    }
}

data class AppSnapshot(
    val characters: List<DublCharacter>,
    val activeCharacterId: String,
) {
    val activeCharacter: DublCharacter
        get() = characters.firstOrNull { it.id == activeCharacterId }
            ?: characters.first()
}

fun defaultAttributes(): Map<AttributeId, AttributeValue> =
    AttributeId.entries.associateWith { AttributeValue() }
