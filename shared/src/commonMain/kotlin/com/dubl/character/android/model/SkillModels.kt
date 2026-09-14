package com.dubl.character.android.model

/** UI-only grouping. Categories do not change DUBL rules. */
enum class SkillCategory(val title: String) {
    COMBAT("Бой"),
    PHYSICAL("Физические"),
    FIELD("Полевые"),
    SOCIAL("Социальные"),
    KNOWLEDGE("Знания"),
    TECHNICAL("Технические"),
    CUSTOM("Свои"),
}

enum class UntrainedRule(val label: String, val penalty: Int?, val usable: Boolean) {
    YES("Да", 0, true),
    YES_MINUS_2("Да (−2)", -2, true),
    NO("Нет", null, false),
    UNSPECIFIED("Не указано", 0, true),
}

data class SkillDefinition(
    val id: String,
    val name: String,
    val description: String,
    val category: SkillCategory,
    val defaultAttribute: AttributeId,
    val untrained: UntrainedRule,
    val auto6: String,
    val auto12: String,
    val template: Boolean = false,
)

data class CharacterSkill(
    val id: String,
    val definitionId: String? = null,
    val name: String = "",
    val description: String = "",
    val rank: Int = 0,
    val attributes: List<AttributeId> = emptyList(),
    val modifier: Int = 0,
    val formulaNote: String = "",
    val untrainedOverride: UntrainedRule? = null,
)

data class ResolvedSkill(
    val id: String,
    val definition: SkillDefinition?,
    val state: CharacterSkill,
) {
    val name: String
        get() = state.name.ifBlank { definition?.name.orEmpty() }
    val description: String
        get() = state.description.ifBlank { definition?.description.orEmpty() }
    val category: SkillCategory
        get() = definition?.category ?: SkillCategory.CUSTOM
    val rank: Int get() = state.rank
    val modifier: Int get() = state.modifier
    val formulaNote: String get() = state.formulaNote
    val untrained: UntrainedRule
        get() = state.untrainedOverride ?: definition?.untrained ?: UntrainedRule.YES
    val attributes: List<AttributeId>
        get() = state.attributes.distinct().ifEmpty {
            listOf(definition?.defaultAttribute ?: AttributeId.INTELLIGENCE)
        }
    val isBuiltIn: Boolean
        get() = definition != null && !definition.template && id == definition.id
    val isDynamic: Boolean get() = !isBuiltIn
    val stockAttribute: AttributeId
        get() = definition?.defaultAttribute ?: attributes.first()
}

data class SkillContribution(val label: String, val value: Int)

data class SkillCalculation(
    val total: Int?,
    val contributions: List<SkillContribution>,
    val unavailableReason: String = "",
    val selectedAttribute: AttributeId? = null,
) {
    fun formulaText(skill: ResolvedSkill, showConfiguredOptions: Boolean = true): String = buildString {
        if (showConfiguredOptions && skill.attributes.size > 1) {
            append("Характеристика: ")
            append(selectedAttribute?.title ?: skill.attributes.first().title)
            append('\n').append("Варианты: ")
            append(skill.attributes.joinToString(" / ") { it.title })
        } else {
            append("Характеристика: ")
            append(selectedAttribute?.title ?: skill.attributes.first().title)
        }
        if (unavailableReason.isNotBlank()) {
            append('\n').append(unavailableReason)
        }
        if (contributions.isNotEmpty()) {
            append('\n').append(if (total == null) "Состав: " else "Расчёт: ")
            append(contributions.joinToString("; ") { item ->
                val sign = if (item.value >= 0) "+" else "−"
                "${item.label} $sign${kotlin.math.abs(item.value)}"
            })
            if (total != null) append(" = ${if (total >= 0) "+" else ""}$total")
        }
        if (skill.formulaNote.isNotBlank()) {
            append('\n').append("Особое правило: ").append(skill.formulaNote)
        }
    }
}

object SkillCatalog {
    /** Cumulative XP cost for rank 0..10, copied from the desktop rules catalog. */
    val rankCosts: List<Int> = listOf(0, 10, 30, 60, 100, 150, 210, 280, 360, 450, 550)

    val definitions: List<SkillDefinition> = listOf(
        skill("athletics", "Атлетика", "Прыжки, лазание, плавание", SkillCategory.PHYSICAL, AttributeId.STRENGTH, UntrainedRule.YES, "Да", "Нет"),
        skill("barter", "Бартер", "Торговля", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.YES, "Нет", "Нет"),
        skill("awareness", "Внимательность", "Слух, зрение", SkillCategory.FIELD, AttributeId.PERCEPTION, UntrainedRule.YES, "Да", "Нет"),
        skill("riding", "Верховая езда", "Держаться в седле", SkillCategory.PHYSICAL, AttributeId.DEXTERITY, UntrainedRule.YES_MINUS_2, "Нет", "Нет"),
        skill("lockpicking", "Взлом", "Взламывание замков и схожих устройств", SkillCategory.TECHNICAL, AttributeId.DEXTERITY, UntrainedRule.YES, "Да", "Да"),
        skill("driving", "Вождение", "Управление колесным транспортом", SkillCategory.TECHNICAL, AttributeId.DEXTERITY, UntrainedRule.YES_MINUS_2, "Нет", "Нет"),
        skill("survival", "Выживание", "Охота, ориентирование в дикой местности", SkillCategory.FIELD, AttributeId.PERCEPTION, UntrainedRule.YES_MINUS_2, "Нет", "Нет"),
        skill("animal_handling", "Дрессировка", "Обучение животных и управление ими.", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.NO, "Да", "Нет"),
        skill("intimidation", "Запугивание", "Устрашение", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.YES, "Нет", "Нет"),
        skill("knowledge_academic", "Знание (Академическое)", "Общие академические сведения.", SkillCategory.KNOWLEDGE, AttributeId.INTELLIGENCE, UntrainedRule.YES, "Нет", "Нет"),
        skill("knowledge_template", "Знание (Любое)", "Практические знания выбранной специальности.", SkillCategory.KNOWLEDGE, AttributeId.INTELLIGENCE, UntrainedRule.NO, "Нет", "Нет", true),
        skill("engineering_repair", "Инженерное дело (Ремонт)", "Ремонт механизмов и конструкций.", SkillCategory.TECHNICAL, AttributeId.INTELLIGENCE, UntrainedRule.NO, "Да", "Нет"),
        skill("performance_vocal", "Исполнение (Вокал)", "Пение", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.YES_MINUS_2, "Нет", "Нет"),
        skill("performance_template", "Исполнение (Любое)", "Игра на музыкальных инструментах, ораторское искусство, танцы", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.NO, "Нет", "Нет", true),
        skill("leadership", "Лидерство", "Управление подчиненными, командование", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.YES, "Нет", "Нет"),
        skill("eloquence", "Красноречие", "Обман, дипломатия", SkillCategory.SOCIAL, AttributeId.CHARISMA, UntrainedRule.YES, "Нет", "Нет"),
        skill("medicine", "Медицина", "Оказание медицинской помощи", SkillCategory.KNOWLEDGE, AttributeId.INTELLIGENCE, UntrainedRule.YES, "Нет", "Нет"),
        skill("piloting", "Пилотирование", "Управление летательными аппаратами", SkillCategory.TECHNICAL, AttributeId.DEXTERITY, UntrainedRule.NO, "Нет", "Нет"),
        skill("search", "Поиск", "Поиск предметов, следов и информации.", SkillCategory.FIELD, AttributeId.PERCEPTION, UntrainedRule.YES, "Да", "Да"),
        skill("profession_template", "Профессия", "Практические знания выбранной специальности.", SkillCategory.KNOWLEDGE, AttributeId.INTELLIGENCE, UntrainedRule.NO, "Да", "Нет", true),
        skill("craft_template", "Ремесло", "Создание предметов", SkillCategory.TECHNICAL, AttributeId.INTELLIGENCE, UntrainedRule.NO, "Да", "Да", true),
        skill("stealth", "Скрытность", "Незаметность, бесшумность", SkillCategory.FIELD, AttributeId.DEXTERITY, UntrainedRule.YES, "Нет", "Нет"),
        skill("shooting", "Стрельба", "Меткость и точность стрельбы", SkillCategory.COMBAT, AttributeId.PERCEPTION, UntrainedRule.YES, "Нет", "Нет"),
        skill("melee_weapon", "Холодное оружие", "Опытность владения холодным оружием, точность атак", SkillCategory.COMBAT, AttributeId.DEXTERITY, UntrainedRule.YES, "Нет", "Нет"),
        skill("throwing", "Метание", "Меткость и точность метания", SkillCategory.COMBAT, AttributeId.DEXTERITY, UntrainedRule.YES, "Нет", "Нет"),
        skill("unarmed", "Рукопашный бой", "Кулачные бои, борьба", SkillCategory.COMBAT, AttributeId.DEXTERITY, UntrainedRule.YES, "Нет", "Нет"),
        skill("computers", "Компьютеры", "Системное администрирование, программирование и поиск информации.", SkillCategory.TECHNICAL, AttributeId.INTELLIGENCE, UntrainedRule.UNSPECIFIED, "Не указано в базовой таблице", "Не указано в базовой таблице"),
    )

    val builtIns: List<SkillDefinition> = definitions.filterNot { it.template }
    val templates: List<SkillDefinition> = definitions.filter { it.template }
    private val byId = definitions.associateBy { it.id }

    fun definition(id: String?): SkillDefinition? = id?.let(byId::get)

    fun costForRank(rank: Int): Int = rankCosts[rank.coerceIn(0, 10)]

    fun nextRankCost(rank: Int): Int? {
        val current = rank.coerceIn(0, 10)
        if (current >= 10) return null
        return rankCosts[current + 1] - rankCosts[current]
    }

    private fun skill(
        id: String,
        name: String,
        description: String,
        category: SkillCategory,
        attribute: AttributeId,
        untrained: UntrainedRule,
        auto6: String,
        auto12: String,
        template: Boolean = false,
    ) = SkillDefinition(id, name, description, category, attribute, untrained, auto6, auto12, template)
}

fun DublCharacter.resolvedSkills(includeHidden: Boolean = false): List<ResolvedSkill> {
    val builtIns = SkillCatalog.builtIns.map { definition ->
        val state = skills[definition.id] ?: CharacterSkill(
            id = definition.id,
            definitionId = definition.id,
        )
        ResolvedSkill(definition.id, definition, state)
    }
    val dynamic = skills.values
        .filter { state -> SkillCatalog.builtIns.none { it.id == state.id } }
        .map { state -> ResolvedSkill(state.id, SkillCatalog.definition(state.definitionId), state) }

    return (builtIns + dynamic)
        .filter { includeHidden || it.id !in hiddenSkillIds }
        .sortedWith(compareBy<ResolvedSkill>({ it.category.ordinal }, { it.name.lowercase() }))
}

fun DublCharacter.resolveSkill(skillId: String): ResolvedSkill? {
    val builtIn = SkillCatalog.builtIns.firstOrNull { it.id == skillId }
    if (builtIn != null) {
        return ResolvedSkill(
            id = builtIn.id,
            definition = builtIn,
            state = skills[builtIn.id] ?: CharacterSkill(id = builtIn.id, definitionId = builtIn.id),
        )
    }
    val state = skills[skillId] ?: return null
    return ResolvedSkill(skillId, SkillCatalog.definition(state.definitionId), state)
}

fun DublCharacter.skillCalculation(
    skill: ResolvedSkill,
    attribute: AttributeId? = null,
): SkillCalculation {
    val selectedAttribute = attribute
        ?.takeIf { it in skill.attributes }
        ?: skill.attributes.first()
    return skillCalculationWithSelectedAttribute(skill, selectedAttribute)
}

/**
 * Per-roll override used by the character sheet. DUBL checks can call for a
 * different characteristic than the skill's configured/default one, so this
 * path intentionally accepts any core characteristic for this single roll.
 */
fun DublCharacter.skillCalculationForRoll(
    skill: ResolvedSkill,
    attribute: AttributeId,
): SkillCalculation = skillCalculationWithSelectedAttribute(skill, attribute)

private fun DublCharacter.skillCalculationWithSelectedAttribute(
    skill: ResolvedSkill,
    selectedAttribute: AttributeId,
): SkillCalculation {
    val contributions = mutableListOf<SkillContribution>()
    var total = attribute(selectedAttribute)
    contributions += SkillContribution(selectedAttribute.title, total)

    total += skill.rank
    contributions += SkillContribution("Ранг", skill.rank)

    if (skill.modifier != 0) {
        total += skill.modifier
        contributions += SkillContribution("Поправка умения", skill.modifier)
    }

    if (skill.rank == 0) {
        if (!skill.untrained.usable) {
            return SkillCalculation(
                total = null,
                contributions = contributions,
                unavailableReason = "Нельзя использовать без обучения",
                selectedAttribute = selectedAttribute,
            )
        }
        val penalty = skill.untrained.penalty ?: 0
        if (penalty != 0) {
            total += penalty
            contributions += SkillContribution("Без обучения", penalty)
        }
    }

    return SkillCalculation(
        total = total,
        contributions = contributions,
        selectedAttribute = selectedAttribute,
    )
}

fun DublCharacter.skillCalculationOptions(skill: ResolvedSkill): List<Pair<AttributeId, SkillCalculation>> =
    skill.attributes.map { attribute ->
        attribute to skillCalculation(skill, attribute)
    }

fun DublCharacter.skillXpSpent(): Int = resolvedSkills(includeHidden = true)
    .sumOf { skillXpCostForRank(it.rank) }

fun DublCharacter.skillXpCostForRank(rank: Int): Int {
    val normalized = rank.coerceIn(0, 10)
    val base = SkillCatalog.costForRank(normalized)
    if (!creationComplete || developmentRank(DevelopmentEffectIds.SELF_TAUGHT) <= 0) return base
    val discountedPart = SkillCatalog.costForRank(normalized.coerceAtMost(2))
    return base - discountedPart / 2
}

fun DublCharacter.skillNextRankCost(currentRank: Int): Int? {
    val current = currentRank.coerceIn(0, 10)
    if (current >= 10) return null
    return skillXpCostForRank(current + 1) - skillXpCostForRank(current)
}
