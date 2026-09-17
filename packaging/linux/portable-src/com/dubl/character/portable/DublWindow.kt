package com.dubl.character.portable

import com.dubl.character.android.data.CharacterExtrasStore
import com.dubl.character.android.model.*
import com.dubl.character.android.state.CharacterExtrasSession
import com.dubl.character.android.state.CharacterSession
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridLayout
import java.awt.Image
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JRadioButton
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder

private enum class Section(val label: String) {
    SHEET("Лист"),
    SKILLS("Умения"),
    DEVELOPMENT("Навыки"),
    MAGIC("Магия"),
    EQUIPMENT("Снаряжение"),
    CHARACTERS("Персонажи"),
}

private enum class DevelopmentFilter(val title: String) {
    ALL("Все"), REGULAR("Обычные"), SPECIAL("Спец. ветки"), MARTIAL("Боевые искусства"), CHI("ЦИ"), OWNED("Взято"),
}

private enum class GroupingKind(val title: String) {
    SKILLS("Умения"),
    DEVELOPMENT("Навыки"),
}

private data class RecentChange(
    val message: String,
    val character: DublCharacter,
    val extras: CharacterSheetExtras,
)

class DublWindow(
    private val session: CharacterSession,
    private val extras: CharacterExtrasSession,
    private val extrasStore: CharacterExtrasStore,
    private val catalogs: CatalogBundle,
    private val dataDir: Path,
) : JFrame("FURY — DUBL 3.69") {
    private val root = JPanel(BorderLayout())
    private val rail = JPanel()
    private val contentHost = JPanel(BorderLayout())
    private var selected = Section.SHEET
    private var skillSearch = ""
    private var skillCategoryFilter: SkillCategory? = null
    private var skillTrainedOnly = false
    private var developmentSearch = ""
    private var developmentFilter = DevelopmentFilter.REGULAR
    private var developmentAvailableOnly = false
    private var spellSearch = ""
    private var spellSchoolFilter: String? = null
    private var hideUnlearnedMagicSchools = true
    private var gearSearch = ""
    private var recentChange: RecentChange? = null

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        minimumSize = Dimension(720, 520)
        preferredSize = Dimension(1180, 800)
        background = com.dubl.character.portable.background
        root.background = com.dubl.character.portable.background
        root.add(buildRail(), BorderLayout.WEST)
        root.add(contentHost, BorderLayout.CENTER)
        contentPane = root
        showSection(selected)
        pack()
        val screen = Toolkit.getDefaultToolkit().screenSize
        val targetWidth = minOf(preferredSize.width, (screen.width - 24).coerceAtLeast(minimumSize.width))
        val targetHeight = minOf(preferredSize.height, (screen.height - 24).coerceAtLeast(minimumSize.height))
        setSize(targetWidth, targetHeight)
        setLocationRelativeTo(null)
    }

    private fun buildRail(): Component {
        rail.background = surfaceInset
        rail.layout = BoxLayout(rail, BoxLayout.Y_AXIS)
        rail.border = EmptyBorder(18, 14, 18, 14)
        rail.preferredSize = Dimension(190, 0)
        rail.add(JLabel("FURY").apply {
            foreground = focus; font = font.deriveFont(Font.BOLD, 25f); alignmentX = Component.LEFT_ALIGNMENT
            border = EmptyBorder(2, 10, 6, 4)
        })
        rail.add(JLabel(session.active.name).apply {
            foreground = muted; alignmentX = Component.LEFT_ALIGNMENT; border = EmptyBorder(0, 10, 16, 4)
        })
        Section.entries.forEach { section ->
            val button = JButton(section.label).apply {
                alignmentX = Component.LEFT_ALIGNMENT; maximumSize = Dimension(Int.MAX_VALUE, 44)
                horizontalAlignment = JLabel.LEFT; isFocusPainted = false; border = EmptyBorder(10, 13, 10, 13)
                putClientProperty("section", section)
                addActionListener { selected = section; refreshRail(); showSection(section) }
            }
            rail.add(button); rail.add(Box.createVerticalStrut(6))
        }
        rail.add(Box.createVerticalGlue())
        rail.add(JLabel("Desktop $APP_VERSION").apply { foreground = muted; border = EmptyBorder(8, 10, 4, 4); alignmentX = Component.LEFT_ALIGNMENT })
        refreshRail()
        return rail
    }

    private fun refreshRail() {
        rail.components.filterIsInstance<JButton>().forEach { button ->
            val section = button.getClientProperty("section") as Section
            button.background = if (section == selected) accentSoft else surfaceInset
            button.foreground = if (section == selected) textColor else muted
            button.border = if (section == selected) BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(focus.darker(), 1, true), EmptyBorder(9, 12, 9, 12),
            ) else EmptyBorder(10, 13, 10, 13)
        }
        rail.components.filterIsInstance<JLabel>().getOrNull(1)?.text = session.active.name
    }

    private fun showSection(section: Section) {
        contentHost.removeAll(); contentHost.background = background
        val content = when (section) {
            Section.SHEET -> buildCharacterSheet()
            Section.SKILLS -> buildSkills()
            Section.DEVELOPMENT -> buildDevelopment()
            Section.MAGIC -> buildMagic()
            Section.EQUIPMENT -> buildEquipment()
            Section.CHARACTERS -> buildCharacters()
        }
        contentHost.add(scroll(content), BorderLayout.CENTER)
        contentHost.revalidate(); contentHost.repaint()
    }

    fun smokeNavigateAllSections() {
        Section.entries.forEach { section ->
            selected = section
            refreshRail()
            showSection(section)
            validate()
            contentHost.doLayout()
            check(contentHost.componentCount == 1) { "Section ${section.name} did not render" }
        }
    }

    private fun mutate(message: String = "Изменение сохранено", action: CharacterSession.() -> Unit) {
        val beforeCharacter = session.active
        val beforeExtras = extras.load(beforeCharacter.id)
        session.action()
        if (session.active.id == beforeCharacter.id && session.active != beforeCharacter) {
            recentChange = RecentChange(message, beforeCharacter, beforeExtras)
        }
        refreshRail(); showSection(selected)
    }

    private fun updateExtras(message: String = "Настройки листа изменены", action: CharacterExtrasSession.() -> Unit) {
        val beforeCharacter = session.active
        val beforeExtras = extras.load(beforeCharacter.id)
        extras.action()
        if (extras.load(beforeCharacter.id) != beforeExtras) {
            recentChange = RecentChange(message, beforeCharacter, beforeExtras)
        }
        showSection(selected)
    }

    private fun undoRecentChange() {
        val change = recentChange ?: return
        if (session.active.id != change.character.id) return
        session.updateActive { change.character }
        extras.update(change.character.id) { change.extras }
        recentChange = null
        refreshRail(); showSection(selected)
    }

    // region Character sheet
    private fun buildCharacterSheet(): JPanel {
        val character = session.active
        val sheetExtras = extras.load(character.id)
        val economy = CharacterEconomy.breakdown(character, catalogs.development)
        return pagePanel().apply {
            add(pageHeader("Лист", character.name))
            recentChange?.let { change ->
                add(card {
                    add(JPanel(BorderLayout(8, 0)).apply {
                        isOpaque = false
                        add(JLabel(change.message).apply { foreground = focus; font = font.deriveFont(Font.BOLD, 13f) }, BorderLayout.CENTER)
                        add(actionButton("Отменить") { undoRecentChange() }, BorderLayout.EAST)
                    })
                })
                verticalGap(10)
            }
            add(card {
                val row = JPanel(BorderLayout(12, 0)).apply {
                    isOpaque = false
                    portraitPreview(sheetExtras.portraitUri)?.let { add(it, BorderLayout.WEST) }
                    add(JPanel().apply {
                        isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        add(JLabel(character.name).apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 21f) })
                        add(JLabel(character.concept.ifBlank { "Без концепта" }).apply { foreground = muted })
                        sheetExtras.portraitUri?.let { add(JLabel("Портрет: ${Path.of(it).fileName}").apply { foreground = muted }) }
                    }, BorderLayout.CENTER)
                    add(JPanel(FlowLayout(FlowLayout.RIGHT, 6, 0)).apply {
                        isOpaque = false
                        add(actionButton("Портрет") { choosePortrait() })
                        add(actionButton("Редактировать") { showIdentityDialog() })
                    }, BorderLayout.EAST)
                }
                add(row); verticalGap(10)
                val stats = JPanel(GridLayout(1, 4, 8, 0)).apply { isOpaque = false }
                stats.add(statTile("Опыт", character.experience.toString(), gold))
                stats.add(statTile("Осталось XP", economy.remainingXp.toString(), if (economy.overspentXp) health else gold))
                stats.add(statTile("ОС", "${economy.abilityPointsRemaining}/${economy.abilityPointsBudget}", gold))
                stats.add(statTile("Размер", "${character.size} · ног ${character.legs}", focus))
                add(stats); verticalGap(8)
                add(actionButton("Экономика и создание") { showEconomyDialog() }.apply { alignmentX = Component.LEFT_ALIGNMENT })
            })
            verticalGap(12)
            add(resourcesCard(character, sheetExtras))
            verticalGap(12)
            add(attributesCard(character))
            verticalGap(12)
            add(conditionsCard(character, sheetExtras))
            verticalGap(12)
            add(derivedStatsCard(character))
            verticalGap(12)
            add(quickChecksCard(character))
            verticalGap(12)
            add(ownedSkillGroupsCard(character, sheetExtras))
            verticalGap(12)
            add(ownedDevelopmentGroupsCard(character, sheetExtras))
            verticalGap(12)
            add(sheetSummaryCard(character))
            add(Box.createVerticalGlue())
        }
    }

    private fun resourcesCard(character: DublCharacter, sheetExtras: CharacterSheetExtras) = card {
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel("Ресурсы").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.WEST)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
                isOpaque = false
                add(actionButton("Настроить") { showResourceVisibilityDialog() })
                add(actionButton("+ Свой ресурс") { showCustomResourceDialog(null) })
            }, BorderLayout.EAST)
        }
        add(header); verticalGap(10)
        if (CharacterSheetResourceId.HEALTH !in sheetExtras.hiddenResourceIds) add(resourceTile("Здоровье", character.hpCurrent, character.healthMaximum, health, { mutate { changeHp(-1) } }, { mutate { changeHp(1) } }, { showHealthControlDialog() }, "Управл."))
        if (CharacterSheetResourceId.ENDURANCE !in sheetExtras.hiddenResourceIds) { verticalGap(7); add(resourceTile("Выносливость", character.enduranceCurrent, character.enduranceMaximum, stamina, { mutate { changeEndurance(-1) } }, { mutate { changeEndurance(1) } }, { showMaximumDialog(CharacterSheetResourceId.ENDURANCE) })) }
        if (character.manaEnabled && CharacterSheetResourceId.MANA !in sheetExtras.hiddenResourceIds) { verticalGap(7); add(resourceTile("Мана", character.manaCurrent, character.effectiveManaMaximum, mana, { mutate { changeMana(-1) } }, { mutate { changeMana(1) } }, { showMaximumDialog(CharacterSheetResourceId.MANA) })) }
        if (character.chiActive && CharacterSheetResourceId.CHI !in sheetExtras.hiddenResourceIds) { verticalGap(7); add(resourceTile("ЦИ", character.chiCurrent, character.chiMaximum, chiColor, { mutate { changeChi(-1) } }, { mutate { changeChi(1) } }, { mutate { restoreChi() } }, "Восст.")) }
        character.customResources.forEach { resource ->
            verticalGap(7)
            add(JPanel(BorderLayout(8, 0)).apply {
                background = surfaceRaised; border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1, true), EmptyBorder(9, 11, 9, 11))
                add(JLabel("${resource.name}   ${resource.current} / ${resource.maximum}").apply { foreground = gold; font = font.deriveFont(Font.BOLD, 15f) }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(smallButton("−") { mutate { changeCustomResource(resource.uid, -1) } })
                    add(smallButton("+") { mutate { changeCustomResource(resource.uid, 1) } })
                    add(smallButton("Изм.") { showCustomResourceDialog(resource) })
                    add(dangerButton("×") { if (confirm("Удалить ресурс «${resource.name}»?")) mutate { removeCustomResource(resource.uid) } })
                }, BorderLayout.EAST)
            })
        }
    }

    private fun resourceTile(label: String, current: Int, maximum: Int, tint: java.awt.Color, minus: () -> Unit, plus: () -> Unit, third: () -> Unit, thirdLabel: String = "Макс.") = JPanel(BorderLayout(10, 0)).apply {
        background = surfaceRaised; border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1, true), EmptyBorder(9, 11, 9, 11))
        add(JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(JLabel(label).apply { foreground = muted }); add(JLabel("$current / $maximum").apply { foreground = tint; font = font.deriveFont(Font.BOLD, 16f) }) }, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply { isOpaque = false; add(smallButton("−", minus)); add(smallButton("+", plus)); add(smallButton(thirdLabel, third)) }, BorderLayout.EAST)
    }

    private fun attributesCard(character: DublCharacter) = card {
        heading("Характеристики"); verticalGap(10)
        val grid = JPanel(GridLayout(0, 2, 8, 8)).apply { isOpaque = false }
        AttributeId.entries.forEach { attribute ->
            val base = character.attributes.getValue(attribute).base
            val effective = character.attribute(attribute)
            grid.add(JPanel(BorderLayout(8, 5)).apply {
                background = surfaceRaised; border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1, true), EmptyBorder(8, 10, 8, 10))
                add(JPanel().apply { isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(JLabel(attribute.title).apply { foreground = muted }); add(JLabel(if (effective == base) "$effective" else "$effective · база $base").apply { foreground = focus; font = font.deriveFont(Font.BOLD, 17f) }) }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(smallButton("−") { mutate { changeAttribute(attribute, -1) } })
                    add(smallButton("+") { mutate { changeAttribute(attribute, 1) } })
                    add(smallButton("XP") { showAttributeInfoDialog(attribute) })
                }, BorderLayout.EAST)
            })
        }
        add(grid)
    }

    private fun conditionsCard(character: DublCharacter, sheetExtras: CharacterSheetExtras) = card {
        val effective = buildSet {
            addAll(sheetExtras.activeConditions)
            if (character.enduranceCurrent == 0) add(CharacterConditionId.WEAKNESS)
        }
        val row = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel("Состояния").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.WEST)
            add(actionButton("Изменить") { showConditionsDialog() }, BorderLayout.EAST)
        }
        add(row); verticalGap(8)
        if (effective.isEmpty()) mutedLabel("Активных состояний нет.")
        else effective.sortedBy { it.title }.forEach { condition ->
            val automatic = condition == CharacterConditionId.WEAKNESS && character.enduranceCurrent == 0
            add(JLabel("• ${condition.title}${if (automatic) " · автоматически" else ""}").apply {
                foreground = if (automatic) gold else focus
                toolTipText = condition.rulesSummary
            })
        }
    }

    private fun derivedStatsCard(character: DublCharacter) = card {
        heading("Показатели"); verticalGap(9)
        val grid = JPanel(GridLayout(0, 3, 8, 8)).apply { isOpaque = false }
        grid.add(statInfoTile("Защита", character.defense.toString(), { showStatInfoDialog("DEFENSE") }))
        grid.add(statInfoTile("Рефлексы", signed(character.reflexes), { showStatInfoDialog("REFLEXES") }) { showContextRollDialog(RollContext.REFLEXES) })
        grid.add(statInfoTile("Инициатива", signed(character.initiative), { showStatInfoDialog("INITIATIVE") }) { showContextRollDialog(RollContext.INITIATIVE) })
        grid.add(statInfoTile("Стойкость", signed(character.fortitude), { showStatInfoDialog("FORTITUDE") }) { showContextRollDialog(RollContext.FORTITUDE) })
        grid.add(statInfoTile("Бег", "${formatNumber(character.runFull)} м", { showStatInfoDialog("RUN") }))
        grid.add(statInfoTile("Размер", character.size.toString(), { showStatInfoDialog("SIZE") }))
        add(grid)
    }

    private fun statInfoTile(label: String, value: String, details: () -> Unit, roll: (() -> Unit)? = null) = JPanel(BorderLayout(4, 4)).apply {
        background = surfaceRaised
        border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1, true), EmptyBorder(8, 10, 8, 10))
        add(JPanel().apply {
            isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JLabel(label).apply { foreground = muted })
            add(JLabel(value).apply { foreground = focus; font = font.deriveFont(Font.BOLD, 17f) })
        }, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
            isOpaque = false
            add(smallButton("Формула", details))
            roll?.let { add(smallButton("Бросок", it)) }
        }, BorderLayout.EAST)
    }

    private fun quickChecksCard(character: DublCharacter) = card {
        heading("Боевые и быстрые проверки"); verticalGap(5)
        mutedLabel("Те же RollRules и производные показатели, что Android; цель/СЛ и ситуационные модификаторы вводятся вручную.")
        verticalGap(8)
        val contexts = listOf(
            RollContext.DODGE,
            RollContext.ATTACK,
            RollContext.PARRY,
            RollContext.FEINT,
            RollContext.GRAPPLE,
            RollContext.DISARM,
            RollContext.TRIP,
            RollContext.PUSH,
            RollContext.KNOCKDOWN,
            RollContext.BREAK_ITEM,
        )
        val grid = JPanel(GridLayout(0, 2, 7, 7)).apply { isOpaque = false }
        contexts.forEach { context -> grid.add(actionButton(context.title) { showContextRollDialog(context) }) }
        add(grid)
        verticalGap(8)
        add(actionButton("Проверка характеристики") { showContextRollDialog(RollContext.ATTRIBUTE) }.apply { alignmentX = Component.LEFT_ALIGNMENT })
        val trained = character.resolvedSkills().filter { it.rank > 0 }.take(12)
        if (trained.isNotEmpty()) {
            verticalGap(8); mutedLabel("Изученные умения")
            add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
                isOpaque = false
                trained.forEach { skill -> add(smallButton(skill.name) { showSkillRollDialog(skill) }) }
            })
        }
    }

    private fun ownedSkillGroupsCard(character: DublCharacter, sheetExtras: CharacterSheetExtras) = card {
        val trained = character.resolvedSkills().filter { it.rank > 0 }
        val defaults = SkillCategory.entries.mapNotNull { category ->
            trained.filter { it.category == category }.map { it.id }.takeIf { it.isNotEmpty() }
                ?.let { SheetGroup("skills:${category.name}", category.title, it) }
        }
        val groups = SheetGroupingRules.normalize(
            saved = sheetExtras.skillGroups,
            defaults = defaults,
            validItemIds = trained.map { it.id },
            ungroupedId = "skills:ungrouped",
        )
        val byId = trained.associateBy { it.id }
        add(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel("Умения · ${trained.size}").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.WEST)
            if (trained.isNotEmpty()) add(actionButton("Группы") { showGroupingManager(GroupingKind.SKILLS) }, BorderLayout.EAST)
        })
        verticalGap(7)
        if (trained.isEmpty()) {
            mutedLabel("Здесь появятся все умения с рангом 1 и выше.")
            return@card
        }
        if (groups != sheetExtras.skillGroups) extras.setSkillGroups(character.id, groups)
        groups.forEach { group ->
            val items = group.itemIds.mapNotNull(byId::get)
            add(groupHeader(group, items.size) {
                updateExtras("Группа умений ${if (group.collapsed) "развёрнута" else "свёрнута"}") {
                    setSkillGroups(character.id, SheetGroupingRules.toggleCollapsed(groups, group.id))
                }
            })
            if (!group.collapsed) {
                items.forEach { skill ->
                    val preferred = sheetExtras.preferredSkillAttributes[skill.id]?.takeIf { it in skill.attributes } ?: skill.stockAttribute
                    val calc = character.skillCalculationForRoll(skill, preferred)
                    add(JPanel(BorderLayout(8, 0)).apply {
                        isOpaque = false; border = EmptyBorder(4, 12, 4, 0)
                        add(wrappingLabel("${skill.name} · ранг ${skill.rank} · ${preferred.shortTitle} ${calc.total?.let(::signed) ?: "—"}", if (calc.total == null) muted else textColor), BorderLayout.CENTER)
                        add(smallButton("Бросок") { showSkillRollDialog(skill) }, BorderLayout.EAST)
                    })
                }
            }
        }
    }

    private fun ownedDevelopmentGroupsCard(character: DublCharacter, sheetExtras: CharacterSheetExtras) = card {
        val rules = DevelopmentRules(character, catalogs.development, DevelopmentProgress(character.development))
        val sections = rules.ownedSheetSections()
        val items = sections.flatMap { it.items }.distinctBy { it.entry.id }
        val byId = items.associateBy { it.entry.id }
        val parentById = items.associate { it.entry.id to it.parentId }
        val defaults = sections.map { section ->
            SheetGroup(
                id = "development:${section.type.name}",
                title = when (section.type) {
                    DevelopmentSheetSectionType.REGULAR -> "Обычные навыки"
                    DevelopmentSheetSectionType.SPECIAL -> "Спец. навыки"
                    DevelopmentSheetSectionType.MARTIAL_ARTS -> "Боевые искусства"
                    DevelopmentSheetSectionType.CHI -> "ЦИ"
                },
                itemIds = section.items.map { it.entry.id },
            )
        }
        val groups = SheetGroupingRules.normalize(
            saved = sheetExtras.developmentGroups,
            defaults = defaults,
            validItemIds = items.map { it.entry.id },
            ungroupedId = "development:ungrouped",
        )
        add(JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel("Взятые навыки · ${items.size}").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.WEST)
            if (items.isNotEmpty()) add(actionButton("Группы") { showGroupingManager(GroupingKind.DEVELOPMENT) }, BorderLayout.EAST)
        })
        verticalGap(7)
        if (items.isEmpty()) {
            mutedLabel("Взятых навыков, боевых искусств и ЦИ пока нет.")
            return@card
        }
        if (groups != sheetExtras.developmentGroups) extras.setDevelopmentGroups(character.id, groups)
        groups.forEach { group ->
            val ordered = SheetGroupingRules.hierarchicalOrder(group.itemIds, parentById)
            val groupItems = ordered.mapNotNull(byId::get)
            add(groupHeader(group, groupItems.size) {
                updateExtras("Группа навыков ${if (group.collapsed) "развёрнута" else "свёрнута"}") {
                    setDevelopmentGroups(character.id, SheetGroupingRules.toggleCollapsed(groups, group.id))
                }
            })
            if (!group.collapsed) {
                groupItems.forEach { item ->
                    val depth = SheetGroupingRules.localDepth(item.entry.id, group.itemIds, parentById)
                    val invalid = rules.requirements(item.entry).any { it.status != RequirementStatus.OK }
                    add(JPanel(BorderLayout(8, 0)).apply {
                        isOpaque = false; border = EmptyBorder(4, 12 + depth * 18, 4, 0)
                        val prefix = if (depth > 0) "↳ " else ""
                        add(wrappingLabel("$prefix${item.entry.name} · ранг ${item.rank}${if (invalid) " · требования!" else ""}", if (invalid) health else textColor), BorderLayout.CENTER)
                        add(smallButton("Описание") { info(item.entry.name, buildString {
                            append(item.entry.benefit.ifBlank { "Описание отсутствует." })
                            if (item.entry.requirements.isNotBlank()) append("\n\nТребования: ${item.entry.requirements}")
                            if (item.entry.notes.isNotBlank()) append("\n\n${item.entry.notes}")
                        }) }, BorderLayout.EAST)
                    })
                }
            }
        }
    }

    private fun groupHeader(group: SheetGroup, count: Int, onToggle: () -> Unit) = JPanel(BorderLayout(8, 0)).apply {
        isOpaque = false; border = EmptyBorder(8, 0, 3, 0)
        add(JLabel("${group.title} · $count").apply { foreground = focus; font = font.deriveFont(Font.BOLD, 14f) }, BorderLayout.WEST)
        add(smallButton(if (group.collapsed) "Развернуть" else "Свернуть", onToggle), BorderLayout.EAST)
    }

    private fun wrappingLabel(value: String, tint: java.awt.Color): JTextArea = JTextArea(value, 1, 34).apply {
        isEditable = false; isOpaque = false; lineWrap = true; wrapStyleWord = true; foreground = tint
        border = null
        minimumSize = Dimension(120, 20)
    }

    private fun sheetSummaryCard(character: DublCharacter) = card {
        val learned = character.resolvedSkills().filter { it.rank > 0 }
        val development = DevelopmentRules(character, catalogs.development, DevelopmentProgress(character.development)).ownedSheetSections().sumOf { it.items.size }
        heading("На листе"); verticalGap(7)
        mutedLabel("Изучено умений: ${learned.size} · Взято навыков/веток/приёмов: $development · Заклинаний: ${character.magic.spells.size} · Предметов: ${character.gear.items.size}")
        verticalGap(7)
        add(JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply {
            isOpaque = false
            add(actionButton("Умения") { selected = Section.SKILLS; refreshRail(); showSection(selected) })
            add(actionButton("Навыки") { selected = Section.DEVELOPMENT; refreshRail(); showSection(selected) })
            add(actionButton("Магия") { selected = Section.MAGIC; refreshRail(); showSection(selected) })
            add(actionButton("Снаряжение") { selected = Section.EQUIPMENT; refreshRail(); showSection(selected) })
            add(actionButton("Группы умений") { showGroupingManager(GroupingKind.SKILLS) })
            add(actionButton("Группы навыков") { showGroupingManager(GroupingKind.DEVELOPMENT) })
        })
    }
    // endregion

    // region Skills
    private fun buildSkills(): JPanel {
        val character = session.active
        val all = character.resolvedSkills().filter { skill ->
            (skillSearch.isBlank() || skill.name.contains(skillSearch, true) || skill.description.contains(skillSearch, true)) &&
                (skillCategoryFilter == null || skill.category == skillCategoryFilter) &&
                (!skillTrainedOnly || skill.rank > 0)
        }
        return pagePanel().apply {
            add(pageHeader("Умения", "${character.name} · XP в умениях ${character.skillXpSpent()}"))
            add(card {
                val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { isOpaque = false }
                val search = JTextField(skillSearch, 20)
                val categoryOptions = arrayOf("Все категории") + SkillCategory.entries.map { it.title }.toTypedArray()
                val category = JComboBox(categoryOptions).apply {
                    selectedIndex = skillCategoryFilter?.let { SkillCategory.entries.indexOf(it) + 1 } ?: 0
                }
                val trainedOnly = JCheckBox("Только изученные", skillTrainedOnly).apply { background = surface; foreground = textColor }
                row.add(search); row.add(category); row.add(trainedOnly)
                row.add(actionButton("Применить") {
                    skillSearch = search.text.trim()
                    skillCategoryFilter = if (category.selectedIndex <= 0) null else SkillCategory.entries[category.selectedIndex - 1]
                    skillTrainedOnly = trainedOnly.isSelected
                    showSection(selected)
                })
                row.add(actionButton("Сброс") {
                    skillSearch = ""; skillCategoryFilter = null; skillTrainedOnly = false; showSection(selected)
                })
                row.add(actionButton("+ Специализация") { showSpecializedSkillDialog() })
                row.add(actionButton("+ Своё умение") { showCustomSkillDialog() })
                row.add(actionButton("Скрытые (${character.hiddenSkillIds.size})") { showHiddenSkillsDialog() })
                add(row)
            }); verticalGap(10)
            all.forEach { skill -> add(skillCard(skill)); verticalGap(7) }
            if (all.isEmpty()) add(card { mutedLabel("Ничего не найдено.") })
            add(Box.createVerticalGlue())
        }
    }

    private fun skillCard(skill: ResolvedSkill) = card {
        val calculation = session.active.skillCalculation(skill)
        val top = JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            add(JPanel().apply {
                isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JLabel(skill.name).apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 16f) })
                add(JLabel("${skill.category.title} · ранг ${skill.rank} · XP ${session.active.skillXpCostForRank(skill.rank)}").apply { foreground = muted })
            }, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                isOpaque = false
                add(smallButton("−") { mutate { changeSkillRank(skill.id, -1) } })
                add(smallButton("+") { mutate { changeSkillRank(skill.id, 1) } })
                add(actionButton("Подробнее") { showSkillDetails(skill) })
                add(actionButton("Настроить") { showSkillSettingsDialog(skill) })
                add(actionButton("Бросок") { showSkillRollDialog(skill) })
                add(actionButton("Скрыть") { mutate { hideSkill(skill.id) } })
                if (skill.isDynamic) add(dangerButton("Удалить") { if (confirm("Удалить умение «${skill.name}»?")) mutate { deleteDynamicSkill(skill.id) } })
            }, BorderLayout.EAST)
        }
        add(top); verticalGap(6)
        mutedLabel(skill.description.ifBlank { "Без описания" })
        verticalGap(4)
        mutedLabel(calculation.formulaText(skill))
    }
    // endregion

    // region Development / Chi
    private fun buildDevelopment(): JPanel {
        val character = session.active
        val rules = DevelopmentRules(character, catalogs.development, DevelopmentProgress(character.development))
        val economy = CharacterEconomy.breakdown(character, catalogs.development)
        val filtered = catalogs.development.entries.asSequence()
            .filter { entry -> !entry.incomplete || (developmentFilter == DevelopmentFilter.OWNED && (character.development[entry.id]?.rank ?: 0) > 0) }
            .filterNot { it.id == MagicEquipmentRules.BASE_MANA_ENTRY_ID }
            .filter { entry ->
                when (developmentFilter) {
                    DevelopmentFilter.ALL -> true
                    DevelopmentFilter.REGULAR -> entry.isRegularDevelopment
                    DevelopmentFilter.SPECIAL -> entry.isSpecialDevelopment
                    DevelopmentFilter.MARTIAL -> entry.isMartialArt
                    DevelopmentFilter.CHI -> entry.isChiDevelopment
                    DevelopmentFilter.OWNED -> (character.development[entry.id]?.rank ?: 0) > 0
                }
            }
            .filter { entry ->
                developmentSearch.isBlank() || listOf(
                    entry.name,
                    developmentBranchName(entry),
                    entry.category,
                    entry.section,
                    entry.requirements,
                    entry.benefit,
                    entry.notes,
                    entry.tags.joinToString(" "),
                ).joinToString(" ").contains(developmentSearch, true)
            }
            .filter { entry -> developmentFilter == DevelopmentFilter.OWNED || !developmentAvailableOnly || rules.availability(entry).canIncrease }
            .sortedWith(compareBy<DevelopmentEntry>({ it.category.lowercase() }, { if (it.isAbility) 0 else 1 }, { it.name.lowercase() }))
            .toList()
        return pagePanel().apply {
            add(pageHeader("Навыки", "${character.name} · XP ${economy.developmentXp} · ОС ${economy.abilityPointsSpent}/${economy.abilityPointsBudget}"))
            add(card {
                val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { isOpaque = false }
                val search = JTextField(developmentSearch, 20)
                val filter = JComboBox(DevelopmentFilter.entries.toTypedArray()).apply { selectedItem = developmentFilter; renderer = titleRenderer<DevelopmentFilter> { it.title } }
                val availableOnly = JCheckBox("Только доступные", developmentAvailableOnly).apply { background = surface; foreground = textColor }
                row.add(search); row.add(filter); row.add(availableOnly)
                row.add(actionButton("Применить") {
                    developmentSearch = search.text.trim()
                    developmentFilter = filter.selectedItem as DevelopmentFilter
                    developmentAvailableOnly = availableOnly.isSelected
                    showSection(selected)
                })
                row.add(actionButton("Сброс") { developmentSearch = ""; developmentFilter = DevelopmentFilter.REGULAR; developmentAvailableOnly = false; showSection(selected) })
                add(row)
            }); verticalGap(10)
            if (developmentFilter == DevelopmentFilter.CHI) { add(chiResourceCard(character)); verticalGap(10); add(chiTechniquesCard(character)); verticalGap(10) }
            add(card { heading("Каталог · ${filtered.size}"); mutedLabel("Показываются первые 160 совпадений. Используй поиск, чтобы быстро добраться до нужной ветки.") })
            verticalGap(8)
            var remaining = 160
            developmentGroupsFor(filtered).forEach { (groupTitle, groupEntries) ->
                if (remaining <= 0) return@forEach
                val visible = groupEntries.take(remaining)
                if (visible.isEmpty()) return@forEach
                add(card {
                    add(JPanel(BorderLayout(8, 0)).apply {
                        isOpaque = false
                        add(JLabel(groupTitle).apply { foreground = focus; font = font.deriveFont(Font.BOLD, 16f) }, BorderLayout.WEST)
                        add(JLabel("${groupEntries.size}").apply { foreground = muted }, BorderLayout.EAST)
                    })
                })
                verticalGap(6)
                visible.forEach { entry -> add(developmentCard(entry, rules)); verticalGap(7) }
                remaining -= visible.size
                verticalGap(3)
            }
            add(Box.createVerticalGlue())
        }
    }

    private fun developmentBranchName(entry: DevelopmentEntry): String = when {
        entry.isAbility -> entry.name
        entry.accessId != null -> catalogs.development.byId(entry.accessId)?.name ?: entry.category.ifBlank { entry.name }
        else -> entry.category.ifBlank { entry.name }
    }

    private fun developmentGroupsFor(entries: List<DevelopmentEntry>): List<Pair<String, List<DevelopmentEntry>>> {
        fun sorted(values: List<DevelopmentEntry>) = values.sortedWith(
            compareBy<DevelopmentEntry>({ if (it.isAbility) 0 else 1 }, { it.name.lowercase() }),
        )
        return when (developmentFilter) {
            DevelopmentFilter.REGULAR -> entries.groupBy { it.category.ifBlank { "Общие" } }.map { (title, values) -> title to sorted(values) }
            DevelopmentFilter.SPECIAL -> entries.groupBy(::developmentBranchName).map { (title, values) -> "$title · Спец. ветка" to sorted(values) }
            DevelopmentFilter.MARTIAL -> entries.groupBy { it.category.ifBlank { "Боевые искусства" } }.map { (title, values) -> title to sorted(values) }
            DevelopmentFilter.CHI -> entries.groupBy { it.category.ifBlank { "ЦИ" } }.map { (title, values) -> title to sorted(values) }
            DevelopmentFilter.OWNED -> buildList {
                entries.filter { it.isRegularDevelopment }.takeIf { it.isNotEmpty() }?.let { add("Обычные навыки" to sorted(it)) }
                entries.filter { it.isMartialArt }.takeIf { it.isNotEmpty() }?.let { add("Боевые искусства" to sorted(it)) }
                entries.filter { it.isChiDevelopment }.takeIf { it.isNotEmpty() }?.let { add("ЦИ" to sorted(it)) }
                entries.filter { it.isSpecialDevelopment }.groupBy(::developmentBranchName).forEach { (branch, values) ->
                    add("$branch · Спец. ветка" to sorted(values))
                }
            }
            DevelopmentFilter.ALL -> entries.groupBy { it.section.ifBlank { it.category.ifBlank { "Развитие" } } }.map { (title, values) -> title to sorted(values) }
        }
    }

    private fun developmentCard(entry: DevelopmentEntry, rules: DevelopmentRules) = card {
        val owned = session.active.development[entry.id] ?: OwnedDevelopment()
        val availability = rules.availability(entry, owned.optionIndex)
        val top = JPanel(BorderLayout(8, 0)).apply {
            isOpaque = false
            add(JPanel().apply {
                isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JLabel(entry.name).apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 16f) })
                val costLabel = if (entry.isAbility) "${availability.abilityCost} ОС" else "${entry.cost} XP/ранг"
                add(JLabel("${entry.section} · ${entry.category} · ранг ${owned.rank}/${entry.maxRank} · $costLabel").apply { foreground = muted })
            }, BorderLayout.CENTER)
            add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                isOpaque = false
                add(smallButton("−") { mutate { setDevelopmentRank(entry.id, (owned.rank - 1).coerceAtLeast(0), owned.optionIndex) } })
                add(smallButton("+") { increaseDevelopment(entry, availability, owned) })
                add(actionButton("Подробнее") { showDevelopmentDetails(entry, availability, owned) })
                if (entry.abilityOptions.size > 1) add(actionButton("Источник") { chooseDevelopmentOption(entry, owned) })
            }, BorderLayout.EAST)
        }
        add(top); verticalGap(5)
        if (entry.requirements.isNotBlank()) mutedLabel("Требования: ${entry.requirements}")
        if (entry.benefit.isNotBlank()) { verticalGap(3); mutedLabel(entry.benefit) }
        val failed = availability.checks.filter { it.status != RequirementStatus.OK }
        if (failed.isNotEmpty()) { verticalGap(3); mutedLabel("Проверка: ${failed.joinToString("; ") { it.text }}") }
    }

    private fun chiResourceCard(character: DublCharacter) = card {
        heading("ЦИ"); verticalGap(8)
        mutedLabel("Запас ${character.chiCurrent}/${character.chiMaximum} · бонусных рангов ${character.chiBonusRanks} · ${if (character.chiActive) "активно" else "неактивно"}")
        verticalGap(7)
        add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
            isOpaque = false
            add(actionButton(if (character.chiEnabled) "Выключить ручной ЦИ" else "Включить ЦИ") { mutate { setChiEnabled(!character.chiEnabled) } })
            add(smallButton("Ранг −") { mutate { setChiBonusRanks(character.chiBonusRanks - 1) } })
            add(smallButton("Ранг +") { mutate { setChiBonusRanks(character.chiBonusRanks + 1) } })
            add(actionButton("Восстановить") { mutate { restoreChi() } })
        })
    }

    private fun chiTechniquesCard(character: DublCharacter) = card {
        heading("Приёмы ЦИ · ${catalogs.chi.techniques.size}"); verticalGap(7)
        val rules = ChiRules(character, catalogs.development)
        catalogs.chi.techniques.filter { technique ->
            val availability = rules.availability(technique)
            (developmentSearch.isBlank() || listOf(technique.name, technique.school, technique.action, technique.effect, technique.requirements).joinToString(" ").contains(developmentSearch, true)) &&
                (!developmentAvailableOnly || availability.unlocked)
        }.take(80).forEach { technique ->
            val a = rules.availability(technique)
            add(JPanel(BorderLayout(7, 0)).apply {
                isOpaque = false; border = EmptyBorder(5, 0, 5, 0)
                add(JLabel("${technique.name} · ${technique.school} · ЦИ ${technique.chiCost}").apply { foreground = if (a.canUse) chiColor else muted }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(actionButton("Описание") { info(technique.name, "${technique.action}\n\n${technique.effect}\n\nТребования: ${technique.requirements}\n${a.reason}") })
                    if (a.canUse) add(actionButton(if (technique.chiCost > 0) "Использовать · −${technique.chiCost} ЦИ" else "Использовать") {
                        if (technique.chiCost > 0) mutate("Использован приём ЦИ: ${technique.name}") { changeChi(-technique.chiCost) }
                        else info(technique.name, "Приём не требует затрат ЦИ.")
                    })
                }, BorderLayout.EAST)
            })
        }
    }
    // endregion

    // region Magic
    private fun buildMagic(): JPanel {
        val character = session.active
        val magic = character.magic
        return pagePanel().apply {
            add(pageHeader("Магия", "${character.name} · мана ${character.manaCurrent}/${character.effectiveManaMaximum}"))
            add(card {
                heading("Мана"); verticalGap(7)
                mutedLabel("Ранг маны ${magic.manaRank}/5 · восстановление ${MagicEquipmentRules.manaRecoveryPerRound(character)}/раунд")
                mutedLabel("XP: мана ${MagicEquipmentRules.manaRankXp(character)} · школы ${MagicEquipmentRules.magicSchoolPowerXp(character)} · заклинания ${MagicEquipmentRules.learnedSpellXp(character)}")
                add(JPanel(FlowLayout(FlowLayout.LEFT, 5, 0)).apply {
                    isOpaque = false
                    add(smallButton("Ранг −") { mutate { setMagicManaRank(magic.manaRank - 1) } })
                    add(smallButton("Ранг +") { mutate { setMagicManaRank(magic.manaRank + 1) } })
                    add(smallButton("Мана −") { mutate { changeMana(-1) } }); add(smallButton("Мана +") { mutate { changeMana(1) } })
                })
            }); verticalGap(10)
            add(card {
                heading("Школы магии"); verticalGap(7)
                val hide = JCheckBox("Скрыть неизученные школы", hideUnlearnedMagicSchools).apply {
                    background = surface; foreground = textColor
                    addActionListener { hideUnlearnedMagicSchools = isSelected; showSection(selected) }
                }
                add(hide); verticalGap(6)
                val visibleSchools = MagicEquipmentRules.visibleMagicSchools(character, hideUnlearnedMagicSchools)
                if (visibleSchools.isEmpty()) mutedLabel("Изученных школ пока нет. Отключи фильтр, чтобы выбрать школу.")
                visibleSchools.forEach { school ->
                    val power = MagicEquipmentRules.schoolPower(character, school)
                    val ownedIndex = character.magic.schools.indexOfFirst { MagicSchoolCatalog.canonicalizeOrNull(it.name) == school }
                    add(JPanel(BorderLayout(6, 0)).apply {
                        isOpaque = false; border = EmptyBorder(3, 0, 3, 0)
                        add(JLabel("$school · сила $power").apply { foreground = if (power > 0) focus else muted }, BorderLayout.CENTER)
                        add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                            isOpaque = false
                            add(smallButton("−") { mutate { setMagicSchoolPower(school, (power - 1).coerceAtLeast(0)) } })
                            add(smallButton("+") { mutate { setMagicSchoolPower(school, power + 1) } })
                            add(smallButton("Изм.") { showSchoolDialog(school) })
                            if (ownedIndex >= 0) add(dangerButton("Удалить") {
                                if (confirm("Удалить школу «$school»?")) mutate("Школа магии удалена: $school") { removeMagicSchool(ownedIndex) }
                            })
                        }, BorderLayout.EAST)
                    })
                }
            }); verticalGap(10)
            add(card {
                heading("Фильтр книги"); verticalGap(7)
                val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { isOpaque = false }
                val search = JTextField(spellSearch, 22)
                val schoolOptions = arrayOf("Все школы") + MagicSchoolCatalog.schools.toTypedArray()
                val school = JComboBox(schoolOptions).apply { selectedIndex = spellSchoolFilter?.let { MagicSchoolCatalog.schools.indexOf(it) + 1 } ?: 0 }
                row.add(search); row.add(school)
                row.add(actionButton("Применить") {
                    spellSearch = search.text.trim()
                    spellSchoolFilter = if (school.selectedIndex <= 0) null else MagicSchoolCatalog.schools[school.selectedIndex - 1]
                    showSection(selected)
                })
                row.add(actionButton("Сброс") { spellSearch = ""; spellSchoolFilter = null; showSection(selected) })
                add(row)
            }); verticalGap(10)
            add(spellbookCard(character)); verticalGap(10)
            add(spellCatalogCard(character)); add(Box.createVerticalGlue())
        }
    }

    private fun spellbookCard(character: DublCharacter) = card {
        val spells = character.magic.spells.filter { spell ->
            (spellSearch.isBlank() || listOf(spell.name, spell.school, spell.description, spell.action).joinToString(" ").contains(spellSearch, true)) &&
                (spellSchoolFilter == null || spellSchoolFilter in MagicSchoolCatalog.parseSchools(spell.school))
        }
        val xp = MagicEquipmentRules.learnedSpellXp(character)
        val unpriced = CharacterEconomy.unpricedLearnedSpells(character)
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(JLabel("Книга заклинаний · ${spells.size} · XP $xp${if (unpriced > 0) " · без цены $unpriced" else ""}").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.WEST)
            add(actionButton("+ Своё заклинание") { showSpellDialog(null) }, BorderLayout.EAST)
        }
        add(header); verticalGap(7)
        if (spells.isEmpty()) mutedLabel("Заклинаний пока нет.")
        spells.forEach { spell ->
            val usability = MagicEquipmentRules.spellUsability(character, spell)
            add(JPanel(BorderLayout(7, 0)).apply {
                isOpaque = false; border = EmptyBorder(5, 0, 5, 0)
                add(JLabel("${spell.name} · ${spell.school.ifBlank { "без школы" }} · мана ${spell.cost}${if (!usability.usable) " · недоступно" else ""}").apply { foreground = if (usability.usable) focus else muted }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(actionButton("Описание") { info(spell.name, spellDetails(spell, usability)) })
                    add(actionButton("Изм.") { showSpellDialog(spell) })
                    add(dangerButton("×") { if (confirm("Удалить заклинание «${spell.name}»?")) mutate { removeSpell(spell.uid) } })
                }, BorderLayout.EAST)
            })
        }
    }

    private fun spellCatalogCard(character: DublCharacter) = card {
        heading("Каталог заклинаний"); verticalGap(7)
        mutedLabel("Поиск и фильтр школы применяются одновременно к книге и каталогу.")
        verticalGap(7)
        val owned = character.magic.spells.mapNotNull { it.catalogId }.toSet()
        catalogs.magicEquipment.spells.asSequence()
            .filter { spellSearch.isBlank() || it.name.contains(spellSearch, true) || it.school.contains(spellSearch, true) || it.description.contains(spellSearch, true) }
            .filter { spellSchoolFilter == null || spellSchoolFilter in MagicSchoolCatalog.parseSchools(it.school) }
            .filterNot { it.id in owned }.take(60).forEach { spell ->
            val usability = MagicEquipmentRules.spellUsability(character, spell)
            add(JPanel(BorderLayout(7, 0)).apply {
                isOpaque = false; border = EmptyBorder(4, 0, 4, 0)
                add(JLabel("${spell.name} · ${spell.school} · ${spell.cost} маны").apply { foreground = if (usability.usable) textColor else muted }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(actionButton("Описание") { info(spell.name, catalogSpellDetails(spell, usability)) })
                    add(actionButton("Выучить") { mutate { addCatalogSpell(spell) } })
                }, BorderLayout.EAST)
            })
        }
    }
    // endregion

    // region Equipment
    private fun buildEquipment(): JPanel {
        val character = session.active
        val load = MagicEquipmentRules.equipmentLoad(character)
        val capacity = MagicEquipmentRules.equipmentCapacity(character)
        val burden = MagicEquipmentRules.burden(character)
        return pagePanel().apply {
            add(pageHeader("Снаряжение", "${character.name} · нагрузка ${formatNumber(load)} / $capacity · ${burden.title}"))
            add(card {
                heading("Нагрузка"); verticalGap(7)
                val automatic = JCheckBox("Считать автоматически", character.gear.loadAutomatic)
                automatic.addActionListener { mutate { setGearLoadAutomatic(automatic.isSelected) } }
                add(automatic)
                if (!character.gear.loadAutomatic) {
                    verticalGap(5)
                    add(actionButton("Ручная нагрузка: ${formatNumber(character.gear.loadManual)}") { showManualLoadDialog() }.apply { alignmentX = Component.LEFT_ALIGNMENT })
                }
                mutedLabel("Штраф: ${signed(burden.penalty)}")
            }); verticalGap(10)
            add(inventoryCard(character)); verticalGap(10)
            add(gearCatalogCard()); add(Box.createVerticalGlue())
        }
    }

    private fun inventoryCard(character: DublCharacter) = card {
        val visibleItems = character.gear.items.filter { item ->
            gearSearch.isBlank() || listOf(
                item.name,
                item.category,
                item.section,
                item.description,
                item.fields.values.joinToString(" "),
            ).joinToString(" ").contains(gearSearch, ignoreCase = true)
        }.sortedBy { it.name.lowercase() }
        val header = JPanel(BorderLayout()).apply {
            isOpaque = false
            val totalQuantity = character.gear.items.sumOf { it.quantity }
            val visibleQuantity = visibleItems.sumOf { it.quantity }
            val count = if (gearSearch.isBlank()) "$totalQuantity шт. · ${character.gear.items.size} позиций" else "$visibleQuantity/$totalQuantity шт. · ${visibleItems.size}/${character.gear.items.size} позиций"
            add(JLabel("Инвентарь · $count").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.WEST)
            add(actionButton("+ Свой предмет") { showGearDialog(null) }, BorderLayout.EAST)
        }
        add(header); verticalGap(7)
        when {
            character.gear.items.isEmpty() -> mutedLabel("Инвентарь пуст.")
            visibleItems.isEmpty() -> mutedLabel("По запросу «$gearSearch» ничего не найдено.")
        }
        visibleItems.forEach { item ->
            add(JPanel(BorderLayout(7, 0)).apply {
                isOpaque = false; border = EmptyBorder(5, 0, 5, 0)
                add(JLabel("${item.name} ×${item.quantity} · вес ${formatNumber(item.load)}${if (!item.carried) " · не несётся" else ""}").apply { foreground = if (item.carried) textColor else muted }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(smallButton("−") { mutate { updateGearItem(item.uid) { it.copy(quantity = (it.quantity - 1).coerceAtLeast(1)) } } })
                    add(smallButton("+") { mutate { updateGearItem(item.uid) { it.copy(quantity = it.quantity + 1) } } })
                    add(actionButton(if (item.carried) "Снять" else "Нести") { mutate { updateGearItem(item.uid) { it.copy(carried = !it.carried) } } })
                    add(actionButton("Описание") { info(item.name, gearDetails(item)) })
                    add(actionButton("Изм.") { showGearDialog(item) })
                    add(dangerButton("×") { if (confirm("Удалить «${item.name}»?")) mutate { removeGearItem(item.uid) } })
                }, BorderLayout.EAST)
            })
        }
    }

    private fun gearDetails(item: GearItem): String = buildString {
        append("Категория: ${item.category}\n")
        append("Раздел: ${item.section}\n")
        append("Количество: ${item.quantity}\n")
        append("Вес за единицу: ${formatNumber(item.load)}\n")
        append("Несётся: ${if (item.carried) "да" else "нет"}")
        if (item.description.isNotBlank()) append("\n\n${item.description}")
        if (item.fields.isNotEmpty()) {
            append("\n\n")
            append(item.fields.entries.joinToString("\n") { "${it.key}: ${it.value}" })
        }
    }

    private fun gearCatalogCard() = card {
        heading("Каталог снаряжения"); verticalGap(7)
        val row = JPanel(FlowLayout(FlowLayout.LEFT, 6, 0)).apply { isOpaque = false }
        val search = JTextField(gearSearch, 24)
        row.add(search); row.add(actionButton("Найти") { gearSearch = search.text.trim(); showSection(selected) }); row.add(actionButton("Сброс") { gearSearch = ""; showSection(selected) })
        add(row); verticalGap(7)
        catalogs.magicEquipment.gear.asSequence().filter { gearSearch.isBlank() || it.name.contains(gearSearch, true) || it.category.contains(gearSearch, true) || it.section.contains(gearSearch, true) }.take(80).forEach { entry ->
            add(JPanel(BorderLayout(7, 0)).apply {
                isOpaque = false; border = EmptyBorder(4, 0, 4, 0)
                add(JLabel("${entry.name} · ${entry.category} · вес ${formatNumber(MagicEquipmentRules.catalogGearLoad(entry))}").apply { foreground = textColor }, BorderLayout.CENTER)
                add(JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0)).apply {
                    isOpaque = false
                    add(actionButton("Описание") { info(entry.name, buildString { append(entry.description); if (entry.fields.isNotEmpty()) append("\n\n").append(entry.fields.entries.joinToString("\n") { "${it.key}: ${it.value}" }) }) })
                    add(actionButton("Добавить") { mutate { addCatalogGear(entry) } })
                }, BorderLayout.EAST)
            })
        }
    }
    // endregion

    // region Characters
    private fun buildCharacters(): JPanel = pagePanel().apply {
        add(pageHeader("Персонажи", "${session.snapshot.characters.size} персонажей"))
        add(card {
            add(actionButton("+ Новый персонаж") { mutate { createCharacter() } }.apply { alignmentX = Component.LEFT_ALIGNMENT })
        }); verticalGap(9)
        session.snapshot.characters.forEach { character ->
            add(card {
                val active = character.id == session.snapshot.activeCharacterId
                val top = JPanel(BorderLayout(8, 0)).apply {
                    isOpaque = false
                    add(JPanel().apply {
                        isOpaque = false; layout = BoxLayout(this, BoxLayout.Y_AXIS)
                        add(JLabel("${if (active) "● " else ""}${character.name}").apply { foreground = if (active) focus else textColor; font = font.deriveFont(Font.BOLD, 17f) })
                        add(JLabel("${character.concept.ifBlank { "Без концепта" }} · XP ${character.experience}").apply { foreground = muted })
                    }, BorderLayout.CENTER)
                    add(JPanel(FlowLayout(FlowLayout.RIGHT, 5, 0)).apply {
                        isOpaque = false
                        if (!active) add(actionButton("Открыть") { mutate { selectCharacter(character.id) } })
                        add(dangerButton("Удалить") {
                            if (session.snapshot.characters.size <= 1) info("Персонажи", "Нельзя удалить последнего персонажа.")
                            else if (confirm("Удалить «${character.name}»?")) {
                                if (!active) session.selectCharacter(character.id)
                                extras.delete(character.id)
                                session.deleteActive(); refreshRail(); showSection(selected)
                            }
                        })
                    }, BorderLayout.EAST)
                }
                add(top)
            }); verticalGap(8)
        }
        add(Box.createVerticalGlue())
    }
    // endregion

    // region dialogs
    private fun showIdentityDialog() {
        val c = session.active
        val name = JTextField(c.name, 22); val concept = JTextField(c.concept, 22); val exp = JTextField(c.experience.toString(), 10); val size = JTextField(c.size.toString(), 5); val legs = JTextField(c.legs.toString(), 5)
        val manaEnabled = JCheckBox("Мана включена", c.manaEnabled).apply { background = surface; foreground = textColor }
        val form = JPanel().apply {
            background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(formPanel("Имя" to name, "Концепт" to concept, "Общий опыт" to exp, "Размер 1–10" to size, "Количество ног" to legs))
            add(manaEnabled)
        }
        if (dialog(form, "Редактировать персонажа")) {
            val nextExperience = exp.text.toIntOrNull()?.coerceAtLeast(0) ?: c.experience
            mutate("Данные персонажа изменены") {
                updateActive { it.copy(
                    name = name.text.trim().ifBlank { "Новый персонаж" },
                    concept = concept.text.trim(),
                    size = size.text.toIntOrNull()?.coerceIn(1, 10) ?: it.size,
                    legs = legs.text.toIntOrNull()?.coerceAtLeast(2) ?: it.legs,
                    manaEnabled = manaEnabled.isSelected,
                    manaCurrent = if (manaEnabled.isSelected) it.manaCurrent else 0,
                ) }
                setExperience(nextExperience)
            }
        }
    }

    private fun showEconomyDialog() {
        val c = session.active
        val creation = JTextField(c.effectiveCreationExperience.toString(), 10)
        val adjustment = JTextField(c.xpAdjustment.toString(), 10)
        val ability = JTextField(c.abilityPointsOverride?.toString().orEmpty(), 10)
        val breakdown = CharacterEconomy.breakdown(c, catalogs.development)
        val panel = JPanel().apply {
            background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); border = EmptyBorder(8,8,8,8)
            add(formPanel("Стартовый опыт" to creation, "XP adjustment" to adjustment, "ОС override (пусто = формула)" to ability))
            add(JLabel("Потрачено XP: ${breakdown.spentXp}; осталось: ${breakdown.remainingXp}; ОС: ${breakdown.abilityPointsSpent}/${breakdown.abilityPointsBudget}").apply { foreground = muted })
        }
        val options = arrayOf("Сохранить", if (c.creationComplete) "Вернуть создание" else "Завершить создание", "Отмена")
        val result = JOptionPane.showOptionDialog(this, panel, "Экономика персонажа", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0])
        if (result == 0) mutate {
            setCreationExperience(creation.text.toIntOrNull() ?: c.effectiveCreationExperience)
            setXpAdjustment(adjustment.text.toIntOrNull() ?: c.xpAdjustment)
            setAbilityPointsOverride(ability.text.trim().takeIf { it.isNotEmpty() }?.toIntOrNull())
        } else if (result == 1) mutate { if (c.creationComplete) reopenCreation() else completeCreation() }
    }

    private fun showHealthControlDialog() {
        val c = session.active
        val amount = JTextField("1", 8)
        val panel = JPanel().apply {
            background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); border = EmptyBorder(8, 8, 8, 8)
            add(JLabel("Здоровье: ${c.hpCurrent} / ${c.healthMaximum}").apply { foreground = health; font = font.deriveFont(Font.BOLD, 20f) })
            verticalGap(8)
            add(formPanel("Количество" to amount))
        }
        val options = arrayOf("Получить урон", "Лечение", "Восстановить всё здоровье", "Изменить максимум", "Отмена")
        when (JOptionPane.showOptionDialog(this, panel, "Здоровье", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options.last())) {
            0 -> amount.text.toIntOrNull()?.coerceAtLeast(0)?.takeIf { it > 0 }?.let { value -> mutate("Получен урон: $value") { changeHp(-value) } }
            1 -> amount.text.toIntOrNull()?.coerceAtLeast(0)?.takeIf { it > 0 }?.let { value -> mutate("Лечение: $value") { changeHp(value) } }
            2 -> mutate("Здоровье полностью восстановлено") { changeHp(active.healthMaximum - active.hpCurrent) }
            3 -> showMaximumDialog(CharacterSheetResourceId.HEALTH)
        }
    }

    private fun showMaximumDialog(resource: CharacterSheetResourceId) {
        val c = session.active
        val current = when (resource) { CharacterSheetResourceId.HEALTH -> c.healthMaximumOverride ?: c.healthMaximum; CharacterSheetResourceId.ENDURANCE -> c.enduranceMaximumOverride ?: c.enduranceMaximum; CharacterSheetResourceId.MANA -> c.manaMaximumOverride ?: c.effectiveManaMaximum; CharacterSheetResourceId.CHI -> c.chiMaximum }
        if (resource == CharacterSheetResourceId.CHI) { mutate { restoreChi() }; return }
        val input = JTextField(current.toString(), 10)
        val options = arrayOf("Сохранить", "По формуле", "Отмена")
        val result = JOptionPane.showOptionDialog(this, formPanel("Максимум ${resource.title.lowercase()}" to input), "${resource.title}: максимум", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0])
        when (result) {
            0 -> mutate { val value = input.text.toIntOrNull()?.coerceAtLeast(0) ?: 0; when (resource) { CharacterSheetResourceId.HEALTH -> setHealthMaximumOverride(value); CharacterSheetResourceId.ENDURANCE -> setEnduranceMaximumOverride(value); CharacterSheetResourceId.MANA -> setManaMaximumOverride(value); CharacterSheetResourceId.CHI -> Unit } }
            1 -> mutate { when (resource) { CharacterSheetResourceId.HEALTH -> setHealthMaximumOverride(null); CharacterSheetResourceId.ENDURANCE -> setEnduranceMaximumOverride(null); CharacterSheetResourceId.MANA -> setManaMaximumOverride(null); CharacterSheetResourceId.CHI -> Unit } }
        }
    }

    private fun showCustomResourceDialog(resource: CustomResource?) {
        val name = JTextField(resource?.name.orEmpty(), 18); val current = JTextField((resource?.current ?: 0).toString(), 8); val maximum = JTextField((resource?.maximum ?: 1).toString(), 8)
        if (dialog(formPanel("Название" to name, "Текущее" to current, "Максимум" to maximum), if (resource == null) "Новый ресурс" else "Изменить ресурс")) {
            val max = maximum.text.toIntOrNull()?.coerceAtLeast(0) ?: 0; val cur = current.text.toIntOrNull()?.coerceIn(0, max) ?: 0
            if (resource == null) mutate { addCustomResource(name.text, max, cur) } else mutate { updateCustomResource(resource.uid, name.text, cur, max) }
        }
    }

    private fun showConditionsDialog() {
        val c = session.active
        val current = extras.load(c.id)
        val automaticWeakness = c.enduranceCurrent == 0
        val box = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS) }
        val checks = linkedMapOf<CharacterConditionId, JCheckBox>()
        CharacterConditionId.entries.forEach { condition ->
            val automatic = condition == CharacterConditionId.WEAKNESS && automaticWeakness
            val check = JCheckBox(
                condition.title + if (automatic) " · автоматически при 0 Выносливости" else "",
                condition in current.activeConditions || automatic,
            ).apply {
                toolTipText = condition.rulesSummary
                background = surface
                foreground = if (automatic) gold else textColor
                isEnabled = !automatic
            }
            checks[condition] = check
            box.add(JPanel(BorderLayout(6, 0)).apply {
                background = surface
                add(check, BorderLayout.CENTER)
                add(smallButton("Описание") { info(condition.title, condition.rulesSummary) }, BorderLayout.EAST)
                maximumSize = Dimension(Int.MAX_VALUE, 42)
            })
        }
        val pane = JScrollPane(box).apply { preferredSize = Dimension(560, 440) }
        if (dialog(pane, "Состояния")) {
            val selectedConditions = checks.filter { (condition, check) ->
                check.isSelected && !(condition == CharacterConditionId.WEAKNESS && automaticWeakness)
            }.keys
            updateExtras("Состояния изменены") { update(c.id) { it.copy(activeConditions = selectedConditions) } }
        }
    }

    private fun showResourceVisibilityDialog() {
        val c = session.active; val current = extras.load(c.id)
        val available = buildList { add(CharacterSheetResourceId.HEALTH); add(CharacterSheetResourceId.ENDURANCE); if (c.manaEnabled) add(CharacterSheetResourceId.MANA); if (c.chiActive) add(CharacterSheetResourceId.CHI) }
        val checks = available.associateWith { id -> JCheckBox(id.title, id !in current.hiddenResourceIds).apply { background = surface; foreground = textColor } }
        val panel = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); checks.values.forEach(::add) }
        if (dialog(panel, "Показывать ресурсы")) {
            val hidden = available.filter { checks.getValue(it).isSelected.not() }.toSet()
            updateExtras("Видимость ресурсов изменена") { update(c.id) { it.copy(hiddenResourceIds = hidden) } }
        }
    }

    private fun portraitPreview(uri: String?): JLabel? {
        val path = uri?.let { runCatching { Path.of(it) }.getOrNull() } ?: return null
        if (!Files.isRegularFile(path)) return null
        return runCatching {
            val source = ImageIcon(path.toString())
            val scaled = source.image.getScaledInstance(96, 96, Image.SCALE_SMOOTH)
            JLabel(ImageIcon(scaled)).apply {
                preferredSize = Dimension(104, 104)
                border = EmptyBorder(4, 0, 4, 8)
            }
        }.getOrNull()
    }

    private fun showAttributeInfoDialog(id: AttributeId) {
        val character = session.active
        val base = character.attributes.getValue(id).base
        val total = character.attribute(id)
        val modifier = total - base
        val nextCost = CharacterEconomy.nextAttributeCost(base)
        val refund = CharacterEconomy.previousAttributeRefund(base)
        val text = buildString {
            append("Текущее значение: $total\nБаза: $base")
            if (modifier != 0) append("\nМодификация: ${signed(modifier)}")
            append("\n\n")
            append(nextCost?.let { "Следующий ранг: $it XP" } ?: "Максимум по таблице")
            refund?.let { append("\nСнижение: возврат $it XP") }
        }
        info(id.title, text)
    }

    private fun showStatInfoDialog(stat: String) {
        val character = session.active
        val (title, value, formula, breakdown, note) = when (stat) {
            "DEFENSE" -> listOf(
                "Защита", character.defense.toString(), "10 − Размер + Скорость + Ловкость",
                "10 − ${character.size} + ${character.speed} + ${character.dexterity}\nИтог: ${character.defense}", "",
            )
            "REFLEXES" -> listOf(
                "Рефлексы", signed(character.reflexes), "Скорость + Ловкость",
                "${character.speed} + ${character.dexterity}\nИтог: ${signed(character.reflexes)}", "",
            )
            "INITIATIVE" -> listOf(
                "Инициатива", signed(character.initiative), "Скорость + Восприятие",
                "${character.speed} + ${character.perception}\nИтог: ${signed(character.initiative)}", "",
            )
            "FORTITUDE" -> listOf(
                "Стойкость", signed(character.fortitude), "Телосложение + Воля",
                "${character.constitution} + ${character.will}\nИтог: ${signed(character.fortitude)}", "",
            )
            "RUN" -> {
                val multiplier = runMultiplier(character)
                listOf(
                    "Бег", "${formatNumber(character.runFull)} м", "Базовый бег + Скорость × множитель размера/ног",
                    "Базовый бег: ${formatNumber(character.runBase)} м\nСкорость: ${character.speed}\nМножитель: ${formatNumber(multiplier)}\nИтог: ${formatNumber(character.runBase)} + ${character.speed} × ${formatNumber(multiplier)} = ${formatNumber(character.runFull)} м",
                    "Множитель зависит от Размера (${character.size}) и количества ног (${character.legs}).",
                )
            }
            else -> listOf(
                "Размер", character.size.toString(), "Задаётся напрямую",
                "Размер: ${character.size}\nМодификатор Силы: ${signed(character.size - 5)}\nМодификатор Скорости: ${signed(5 - character.size)}",
                "Размер влияет на Силу, Скорость, Защиту, здоровье и Бег.",
            )
        }
        val detailText = "$value\n\nФормула\n$formula\n\n$breakdown${if (note.isNotBlank()) "\n\n$note" else ""}"
        val panel = JPanel().apply {
            background = surface
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(JTextArea(detailText, 9, 46).apply {
                isEditable = false; lineWrap = true; wrapStyleWord = true
                background = surface; foreground = textColor; border = EmptyBorder(4, 4, 8, 4)
            })
        }
        when (stat) {
            "FORTITUDE" -> {
                val options = arrayOf("Бросить Стойкость", "Закрыть")
                if (JOptionPane.showOptionDialog(this, panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]) == 0) {
                    showContextRollDialog(RollContext.FORTITUDE)
                }
            }
            "SIZE" -> {
                val size = JTextField(character.size.toString(), 8)
                panel.add(JLabel("Быстрое редактирование").apply { foreground = gold; font = font.deriveFont(Font.BOLD, 13f) })
                panel.add(formPanel("Размер 1–10" to size))
                val options = arrayOf("Сохранить размер", "Закрыть")
                if (JOptionPane.showOptionDialog(this, panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]) == 0) {
                    val newSize = size.text.toIntOrNull()?.coerceIn(1, 10) ?: character.size
                    mutate("Размер изменён") { updateActive { it.copy(size = newSize) } }
                }
            }
            "RUN" -> {
                val legs = JTextField(character.legs.toString(), 8)
                panel.add(JLabel("Быстрое редактирование").apply { foreground = gold; font = font.deriveFont(Font.BOLD, 13f) })
                panel.add(formPanel("Количество ног" to legs))
                val options = arrayOf("Сохранить", "Закрыть")
                if (JOptionPane.showOptionDialog(this, panel, title, JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]) == 0) {
                    val newLegs = legs.text.toIntOrNull()?.coerceAtLeast(2) ?: character.legs
                    mutate("Количество ног изменено") { updateActive { it.copy(legs = newLegs) } }
                }
            }
            else -> JOptionPane.showMessageDialog(this, panel, title, JOptionPane.INFORMATION_MESSAGE)
        }
    }

    private fun runMultiplier(character: DublCharacter): Double = if (character.legs >= 3) {
        when (character.size.coerceIn(1, 10)) {
            1 -> 0.5; 2 -> 1.0; 3 -> 1.5; 4, 5, 6 -> 2.0; 7 -> 3.0; 8 -> 4.0; 9 -> 5.0; else -> 6.0
        }
    } else {
        when (character.size.coerceIn(1, 10)) {
            1 -> 0.125; 2 -> 0.25; 3 -> 0.5; 4, 5 -> 1.0; 6, 7 -> 1.5; 8 -> 2.0; 9 -> 3.0; else -> 4.0
        }
    }

    private fun choosePortrait() {
        val chooser = JFileChooser()
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return
        val source = chooser.selectedFile.toPath(); val ext = source.fileName.toString().substringAfterLast('.', "img")
        val dir = dataDir.resolve("portraits"); Files.createDirectories(dir)
        val target = dir.resolve("${session.active.id}.$ext")
        runCatching { Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING) }.onSuccess { updateExtras { setPortrait(session.active.id, target.toString()) } }.onFailure { error("Не удалось сохранить портрет: ${it.message}") }
    }

    private fun showSpecializedSkillDialog() {
        val templates = SkillCatalog.templates
        val combo = JComboBox(templates.toTypedArray()).apply { renderer = titleRenderer<SkillDefinition> { it.name } }
        val specialization = JTextField("", 20)
        if (dialog(formPanel("Тип" to combo, "Специализация" to specialization), "Специализированное умение")) {
            val template = combo.selectedItem as SkillDefinition
            val result = session.addSpecializedSkill(template.id, specialization.text)
            if (result == null) info("Умения", "Не удалось добавить: пустое или уже существующее название.") else showSection(selected)
        }
    }

    private fun showCustomSkillDialog() {
        val name = JTextField("", 20); val description = JTextArea("", 4, 26).apply { lineWrap = true; wrapStyleWord = true }
        val attrs = AttributeId.entries.associateWith { JCheckBox(it.title, it == AttributeId.INTELLIGENCE).apply { background = surface; foreground = textColor } }
        val untrained = JComboBox(UntrainedRule.entries.toTypedArray()).apply { renderer = titleRenderer<UntrainedRule> { it.label } }
        val panel = JPanel().apply {
            background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(formPanel("Название" to name, "Без обучения" to untrained)); add(JLabel("Характеристики").apply { foreground = textColor }); attrs.values.forEach(::add); add(JLabel("Описание").apply { foreground = textColor }); add(JScrollPane(description))
        }
        if (dialog(panel, "Своё умение")) {
            val selectedAttrs = attrs.filterValues { it.isSelected }.keys.toList()
            val id = session.addCustomSkill(name.text, description.text, selectedAttrs, untrained.selectedItem as UntrainedRule)
            if (id == null) info("Умения", "Нужно уникальное название и хотя бы одна характеристика.") else showSection(selected)
        }
    }

    private fun showSkillDetails(skill: ResolvedSkill) {
        val character = session.active
        val nextCost = SkillCatalog.nextRankCost(skill.rank)
        val calculations = character.skillCalculationOptions(skill)
        val text = buildString {
            append(skill.description.ifBlank { "Без описания" })
            append("\n\nРанг: ${skill.rank}/10")
            append("\nПотрачено XP: ${SkillCatalog.costForRank(skill.rank)}")
            append("\n")
            append(nextCost?.let { "Следующий ранг ${skill.rank + 1}: +$it XP" } ?: "Достигнут максимальный ранг")
            append("\n\nДопустимые характеристики: ${skill.attributes.joinToString(" / ") { it.title }}")
            calculations.forEach { (attribute, calculation) ->
                append("\n\n${attribute.title}: ${calculation.total?.let(::signed) ?: "—"}")
                if (calculation.contributions.isNotEmpty()) {
                    append("\n")
                    append(calculation.contributions.joinToString(" + ") { "${it.label} ${signed(it.value)}" })
                }
                if (calculation.unavailableReason.isNotBlank()) append("\n${calculation.unavailableReason}")
            }
            append("\n\nБез обучения: ${skill.untrained.label}")
            skill.definition?.let { definition ->
                append("\nАвтоуспех 6: ${definition.auto6}")
                append(" · Автоуспех 12: ${definition.auto12}")
            }
            if (skill.formulaNote.isNotBlank()) append("\n\nОсобое правило / условный бонус:\n${skill.formulaNote}")
        }
        info(skill.name, text)
    }

    private fun showSkillSettingsDialog(skill: ResolvedSkill) {
        val attrs = AttributeId.entries.associateWith { JCheckBox(it.title, it in skill.attributes).apply { background = surface; foreground = textColor } }
        val modifier = JTextField(skill.modifier.toString(), 8); val note = JTextArea(skill.formulaNote, 4, 28).apply { lineWrap = true; wrapStyleWord = true }
        val panel = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(formPanel("Модификатор" to modifier)); add(JLabel("Характеристики").apply { foreground = textColor }); attrs.values.forEach(::add); add(JLabel("Особое правило / заметка формулы").apply { foreground = textColor }); add(JScrollPane(note)) }
        if (dialog(panel, "Настроить ${skill.name}")) mutate {
            val selectedAttrs = attrs.filterValues { it.isSelected }.keys.toList()
            if (selectedAttrs.isNotEmpty()) setSkillAttributes(skill.id, selectedAttrs)
            setSkillModifier(skill.id, modifier.text.toIntOrNull() ?: 0); setSkillFormulaNote(skill.id, note.text)
        }
    }

    private fun showHiddenSkillsDialog() {
        val hidden = session.active.resolvedSkills(includeHidden = true).filter { it.id in session.active.hiddenSkillIds }
        if (hidden.isEmpty()) { info("Скрытые умения", "Скрытых умений нет."); return }
        val panel = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); hidden.forEach { skill -> add(JPanel(BorderLayout()).apply { isOpaque = false; add(JLabel(skill.name).apply { foreground = textColor }, BorderLayout.CENTER); add(actionButton("Вернуть") { session.restoreSkill(skill.id); showSection(selected) }, BorderLayout.EAST) }) } }
        val options = arrayOf("Вернуть все", "Закрыть")
        if (JOptionPane.showOptionDialog(this, JScrollPane(panel).apply { preferredSize = Dimension(520, 380) }, "Скрытые умения", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]) == 0) mutate { restoreAllSkills() }
    }

    private fun showSkillRollDialog(skill: ResolvedSkill) {
        val character = session.active
        val savedPreferred = extras.load(character.id).preferredSkillAttributes[skill.id]
        val attrs = skill.attributes.ifEmpty { listOf(skill.stockAttribute) }.toTypedArray()
        val attr = JComboBox(attrs).apply {
            selectedItem = savedPreferred?.takeIf { it in attrs } ?: attrs.first()
            renderer = titleRenderer<AttributeId> { it.title }
        }
        val mode = JComboBox(RollMode.entries.toTypedArray()).apply { renderer = titleRenderer<RollMode> { it.title } }
        val extra = JSpinner(SpinnerNumberModel(1, 1, 20, 1))
        val situational = JSpinner(SpinnerNumberModel(0, -99, 99, 1))
        val target = JTextField("", 8)

        val effects = SkillEffectRules(character, catalogs.development, catalogs.skillEffects).forSkill(skill)
        val effectChecks = effects.options.associateWith { option ->
            JCheckBox(
                option.label + when {
                    option.numericBonus != 0 -> " (${signed(option.numericBonus)})"
                    option.advantageDice > 0 -> " (+${option.advantageDice} преимущество)"
                    option.hindranceDice > 0 -> " (+${option.hindranceDice} помеха)"
                    else -> ""
                },
                false,
            ).apply {
                background = surface
                foreground = textColor
                toolTipText = option.description
            }
        }

        val panel = JPanel().apply {
            background = surface
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(formPanel(
                "Характеристика" to attr,
                "Режим" to mode,
                "Преимуществ / помех" to extra,
                "Ситуативный бонус" to situational,
                "СЛ / результат противника" to target,
            ))
            if (effects.automaticContributions.isNotEmpty()) {
                add(JLabel("Автоматические эффекты").apply { foreground = focus; font = font.deriveFont(Font.BOLD, 13f) })
                effects.automaticContributions.forEach { contribution ->
                    add(JLabel("• ${contribution.label}: ${signed(contribution.value)}").apply { foreground = muted })
                }
            }
            if (effectChecks.isNotEmpty()) {
                verticalGap(7)
                add(JLabel("Ситуационные эффекты навыков").apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 13f) })
                effectChecks.values.forEach(::add)
            }
            if (effects.reminders.isNotEmpty()) {
                verticalGap(7)
                add(JLabel("Связанные правила").apply { foreground = gold; font = font.deriveFont(Font.BOLD, 13f) })
                effects.reminders.forEach { effect ->
                    add(JLabel("• ${effect.sourceName}: ${effect.effectText}").apply { foreground = muted; toolTipText = effect.effectText })
                }
            }
        }
        if (!dialog(JScrollPane(panel).apply { preferredSize = Dimension(620, 520) }, "Бросок: ${skill.name}")) return

        val chosenAttr = attr.selectedItem as AttributeId
        updateExtras("Характеристика броска сохранена") { setPreferredSkillAttribute(character.id, skill.id, chosenAttr) }
        val calculation = session.active.skillCalculationForRoll(skill, chosenAttr)
        val baseBonus = calculation.total
        if (baseBonus == null) { info("Бросок", calculation.unavailableReason); return }
        val selectedEffects = effectChecks.filterValues { it.isSelected }.keys
        val effectNumericBonus = selectedEffects.sumOf { it.numericBonus }
        val selectedAdvantage = selectedEffects.sumOf { it.advantageDice }
        val selectedHindrance = selectedEffects.sumOf { it.hindranceDice }
        val manualCount = extra.value as Int
        val selectedMode = mode.selectedItem as RollMode
        val manualAdvantage = if (selectedMode == RollMode.ADVANTAGE) manualCount else 0
        val manualHindrance = if (selectedMode == RollMode.HINDRANCE) manualCount else 0
        val totalAdvantage = manualAdvantage + selectedAdvantage
        val totalHindrance = manualHindrance + selectedHindrance
        if (totalAdvantage > 0 && totalHindrance > 0) {
            info("Бросок", "Преимущества и помехи не смешиваются автоматически. Оставь только один тип эффекта.")
            return
        }
        val effectiveMode = when {
            totalAdvantage > 0 -> RollMode.ADVANTAGE
            totalHindrance > 0 -> RollMode.HINDRANCE
            else -> RollMode.NORMAL
        }
        val effectCount = when (effectiveMode) {
            RollMode.ADVANTAGE -> totalAdvantage
            RollMode.HINDRANCE -> totalHindrance
            RollMode.NORMAL -> 0
        }
        val effectiveBonus = baseBonus + effects.automaticBonus + effectNumericBonus
        var result = rollCheck(
            effectiveMode,
            effectCount,
            effectiveBonus,
            "${skill.name} + ${chosenAttr.title}",
            situational.value as Int,
        )
        while (true) {
            val chosen = result.chosenIndices.sorted().joinToString { index -> "${result.dice[index]}" }
            val targetValue = target.text.toIntOrNull()
            val comparison = targetValue?.let { compareRollToTarget(result.total, it) }
            val text = buildString {
                append("Кости: ${result.dice.joinToString()}\nВыбрано: $chosen\nБонус проверки: ${signed(result.checkBonus)}\nСитуативный: ${signed(result.situationalBonus)}\n\nИТОГ: ${result.total}")
                comparison?.let { append("\nСЛ ${it.target}: ${it.outcome} (${signed(it.margin)})") }
                result.specialResult?.let { append("\n${it.title}") }
                result.note?.let { append("\n\n$it") }
            }
            if (result.followUp == null) { info("Результат броска", text); return }
            val options = arrayOf(result.followUp!!.buttonTitle, "Закрыть")
            val choice = JOptionPane.showOptionDialog(this, JTextArea(text, 12, 46).apply { isEditable = false; lineWrap = true; wrapStyleWord = true }, "Результат броска", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0])
            if (choice != 0) return
            result = rollFollowUp(result)
        }
    }

    private fun showContextRollDialog(
        context: RollContext,
        skillId: String? = null,
        attribute: AttributeId? = null,
    ) {
        val character = session.active
        val allowedSkillIds = when (context) {
            RollContext.ATTACK, RollContext.BREAK_ITEM -> listOf("unarmed", "melee_weapon", "shooting", "throwing")
            RollContext.PARRY, RollContext.DISARM -> listOf("unarmed", "melee_weapon")
            RollContext.FEINT -> listOf("eloquence", "unarmed", "melee_weapon")
            else -> emptyList()
        }
        val skillOptions = allowedSkillIds.mapNotNull(character::resolveSkill)
        val skillCombo = JComboBox(skillOptions.toTypedArray()).apply {
            renderer = titleRenderer<ResolvedSkill> { it.name }
            skillId?.let { requested -> skillOptions.firstOrNull { it.id == requested }?.let { selectedItem = it } }
        }
        fun attributesFor(skillId: String?): List<AttributeId> = when (context) {
            RollContext.ATTACK, RollContext.BREAK_ITEM -> when (skillId) {
                "shooting" -> listOf(AttributeId.PERCEPTION, AttributeId.DEXTERITY)
                "throwing" -> listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH)
                "unarmed", "melee_weapon" -> listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH)
                else -> emptyList()
            }
            RollContext.PARRY, RollContext.DISARM -> listOf(AttributeId.DEXTERITY, AttributeId.STRENGTH)
            RollContext.FEINT -> listOf(AttributeId.CHARISMA)
            else -> emptyList()
        }
        val attributeCombo = JComboBox<AttributeId>().apply { renderer = titleRenderer<AttributeId> { it.title } }
        fun refreshAttributes() {
            val options = if (context == RollContext.ATTRIBUTE) AttributeId.entries else attributesFor((skillCombo.selectedItem as? ResolvedSkill)?.id)
            attributeCombo.removeAllItems()
            options.forEach(attributeCombo::addItem)
            attribute?.takeIf { it in options }?.let { attributeCombo.selectedItem = it }
        }
        refreshAttributes()
        skillCombo.addActionListener { refreshAttributes() }
        val advantage = JSpinner(SpinnerNumberModel(0, 0, 9, 1))
        val hindrance = JSpinner(SpinnerNumberModel(0, 0, 9, 1))
        val situational = JTextField("0", 7)
        val target = JTextField("", 7)
        val beforePreset = character.rollPreset(context, (skillCombo.selectedItem as? ResolvedSkill)?.id, attributeCombo.selectedItem as? AttributeId)
        val reminders = SkillEffectRules(character, catalogs.development, catalogs.skillEffects).forContext(context)
        val panel = JPanel().apply {
            background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS)
            if (skillOptions.isNotEmpty()) add(formPanel("Умение" to skillCombo))
            if (attributeCombo.itemCount > 0) add(formPanel("Характеристика" to attributeCombo))
            add(formPanel("Преимущество" to advantage, "Помеха" to hindrance, "Ситуационная поправка" to situational, "Цель / СЛ" to target))
            add(JLabel(beforePreset.formulaText).apply { foreground = muted; border = EmptyBorder(7, 8, 5, 8) })
            if (reminders.isNotEmpty()) {
                add(JTextArea(reminders.joinToString("\n") { "${it.sourceName}: ${it.effectText}" }, 5, 42).apply {
                    isEditable = false; lineWrap = true; wrapStyleWord = true; background = surfaceRaised; foreground = gold; border = EmptyBorder(7, 8, 7, 8)
                })
            }
        }
        if (!dialog(panel, context.title)) return
        val selectedSkill = skillCombo.selectedItem as? ResolvedSkill
        val selectedAttribute = attributeCombo.selectedItem as? AttributeId
        val preset = character.rollPreset(context, selectedSkill?.id, selectedAttribute)
        val bonus = preset.bonus ?: run { error(preset.unavailableReason.ifBlank { "Проверка недоступна" }); return }
        val advantageCount = advantage.value as Int
        val hindranceCount = hindrance.value as Int
        val mode = when {
            advantageCount > 0 -> RollMode.ADVANTAGE
            hindranceCount > 0 -> RollMode.HINDRANCE
            else -> RollMode.NORMAL
        }
        val effectCount = when (mode) {
            RollMode.ADVANTAGE -> advantageCount
            RollMode.HINDRANCE -> hindranceCount
            RollMode.NORMAL -> 0
        }
        var result = rollCheck(
            mode = mode,
            effectCount = effectCount,
            checkBonus = bonus,
            checkBonusLabel = preset.title,
            situationalBonus = situational.text.toIntOrNull()?.coerceIn(-99, 99) ?: 0,
        )
        while (true) {
            val chosen = result.chosenIndices.sorted().joinToString { index -> "${result.dice[index]}" }
            val targetValue = target.text.toIntOrNull()?.coerceIn(-999, 999)
            val comparison = targetValue?.let { compareRollToTarget(result.total, it) }
            val output = buildString {
                append("Кости: ${result.dice.joinToString()}\nВыбрано: $chosen\n${preset.formulaText}\nБонус проверки: ${signed(result.checkBonus)}\nСитуативный: ${signed(result.situationalBonus)}\n\nИТОГ: ${result.total}")
                comparison?.let { append("\nСЛ ${it.target}: ${it.outcome} (${signed(it.margin)})") }
                result.specialResult?.let { append("\n${it.title}") }
                result.note?.let { append("\n\n$it") }
            }
            if (result.followUp == null) { info("Результат броска", output); return }
            val options = arrayOf(result.followUp!!.buttonTitle, "Закрыть")
            val choice = JOptionPane.showOptionDialog(this, JTextArea(output, 12, 46).apply { isEditable = false; lineWrap = true; wrapStyleWord = true }, "Результат броска", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0])
            if (choice != 0) return
            result = rollFollowUp(result)
        }
    }

    private fun showDevelopmentDetails(entry: DevelopmentEntry, availability: DevelopmentAvailability, owned: OwnedDevelopment) {
        val text = buildString {
            append("Раздел: ${entry.section}\nКатегория: ${entry.category}\nРанг: ${owned.rank}/${entry.maxRank}")
            append(if (entry.isAbility) "\nСтоимость: ${availability.abilityCost} ОС" else "\nСтоимость: ${entry.cost} XP за ранг")
            if (entry.requirements.isNotBlank()) append("\n\nТребования:\n${entry.requirements}")
            if (entry.benefit.isNotBlank()) append("\n\nЭффект:\n${entry.benefit}")
            if (entry.notes.isNotBlank()) append("\n\nПримечания:\n${entry.notes}")
            if (entry.tags.isNotEmpty()) append("\n\nТеги: ${entry.tags.joinToString(", ")}")
            if (availability.checks.isNotEmpty()) {
                append("\n\nПроверка требований:\n")
                append(availability.checks.joinToString("\n") { check -> "• ${check.text} · ${check.status}" })
            }
            if (entry.abilityOptions.isNotEmpty()) {
                append("\n\nИсточники способности:\n")
                append(entry.abilityOptions.joinToString("\n") { option -> "• ${option.source}: ${option.value} ОС" })
            }
        }
        info(entry.name, text)
    }

    private fun increaseDevelopment(entry: DevelopmentEntry, availability: DevelopmentAvailability, owned: OwnedDevelopment) {
        if (owned.rank >= entry.maxRank) return
        if (!availability.canIncrease) {
            if (!availability.canForceIncrease || !confirm("Требования не выполнены или требуют ручной проверки. Форсировать получение «${entry.name}»?\n\n${availability.checks.filter { it.status != RequirementStatus.OK }.joinToString("\n") { it.text }}")) return
        }
        var optionIndex = owned.optionIndex
        if (entry.abilityOptions.size > 1) {
            val combo = JComboBox(entry.abilityOptions.toTypedArray()).apply { selectedIndex = optionIndex.coerceIn(0, entry.abilityOptions.lastIndex); renderer = titleRenderer<AbilityOption> { "${it.source}: ${it.value} ОС" } }
            if (!dialog(formPanel("Источник" to combo), "Источник способности")) return
            optionIndex = combo.selectedIndex
        }
        mutate { setDevelopmentRank(entry.id, owned.rank + 1, optionIndex) }
    }

    private fun chooseDevelopmentOption(entry: DevelopmentEntry, owned: OwnedDevelopment) {
        val combo = JComboBox(entry.abilityOptions.toTypedArray()).apply { selectedIndex = owned.optionIndex.coerceIn(0, entry.abilityOptions.lastIndex); renderer = titleRenderer<AbilityOption> { "${it.source}: ${it.value} ОС" } }
        if (dialog(formPanel("Источник" to combo), entry.name)) mutate { setDevelopmentRank(entry.id, owned.rank.coerceAtLeast(1), combo.selectedIndex) }
    }

    private fun showGroupingManager(kind: GroupingKind) {
        val character = session.active
        val sheetExtras = extras.load(character.id)
        val skillItems = character.resolvedSkills().filter { it.rank > 0 }
        val developmentItems = DevelopmentRules(character, catalogs.development, DevelopmentProgress(character.development))
            .ownedSheetSections().flatMap { it.items }.distinctBy { it.entry.id }
        val itemLabels: Map<String, String> = when (kind) {
            GroupingKind.SKILLS -> skillItems.associate { it.id to it.name }
            GroupingKind.DEVELOPMENT -> developmentItems.associate { it.entry.id to it.entry.name }
        }
        val parentById: Map<String, String?> = when (kind) {
            GroupingKind.SKILLS -> emptyMap()
            GroupingKind.DEVELOPMENT -> developmentItems.associate { it.entry.id to it.parentId }
        }
        val treeRootIds: Set<String> = if (kind == GroupingKind.DEVELOPMENT) {
            val parentIds = parentById.values.filterNotNull().toSet()
            developmentItems.asSequence()
                .filter { item -> item.entry.id in parentIds && item.parentId == null && (item.entry.isSpecialDevelopment || item.entry.isMartialArt) }
                .map { it.entry.id }
                .toSet()
        } else emptySet()
        val defaults = when (kind) {
            GroupingKind.SKILLS -> SkillCategory.entries.mapNotNull { category ->
                skillItems.filter { it.category == category }.map { it.id }.takeIf { it.isNotEmpty() }
                    ?.let { SheetGroup("skills:${category.name}", category.title, it) }
            }
            GroupingKind.DEVELOPMENT -> DevelopmentRules(character, catalogs.development, DevelopmentProgress(character.development))
                .ownedSheetSections().map { section ->
                    SheetGroup(
                        "development:${section.type.name}",
                        when (section.type) {
                            DevelopmentSheetSectionType.REGULAR -> "Обычные навыки"
                            DevelopmentSheetSectionType.SPECIAL -> "Спец. навыки"
                            DevelopmentSheetSectionType.MARTIAL_ARTS -> "Боевые искусства"
                            DevelopmentSheetSectionType.CHI -> "ЦИ"
                        },
                        section.items.map { it.entry.id },
                    )
                }
        }
        val ungroupedId = if (kind == GroupingKind.SKILLS) "skills:ungrouped" else "development:ungrouped"
        var groups = SheetGroupingRules.normalize(
            if (kind == GroupingKind.SKILLS) sheetExtras.skillGroups else sheetExtras.developmentGroups,
            defaults,
            itemLabels.keys.toList(),
            ungroupedId,
        )

        fun persist() {
            if (kind == GroupingKind.SKILLS) extras.setSkillGroups(character.id, groups) else extras.setDevelopmentGroups(character.id, groups)
        }
        fun chooseGroup(title: String, includeUngrouped: Boolean = true): SheetGroup? {
            val available = groups.filter { includeUngrouped || it.id != ungroupedId }
            if (available.isEmpty()) return null
            val combo = JComboBox(available.toTypedArray()).apply { renderer = titleRenderer<SheetGroup> { it.title } }
            return if (dialog(formPanel("Группа" to combo), title)) combo.selectedItem as SheetGroup else null
        }
        fun chooseItem(title: String): String? {
            val ids = groups.flatMap { it.itemIds }.distinct().toTypedArray()
            if (ids.isEmpty()) return null
            val combo = JComboBox(ids).apply { renderer = titleRenderer<String> { itemLabels[it] ?: it } }
            return if (dialog(formPanel("Элемент" to combo), title)) combo.selectedItem as String else null
        }

        while (true) {
            val summary = buildString {
                groups.forEachIndexed { index, group ->
                    append("${index + 1}. ${group.title} (${group.itemIds.size})\n")
                    group.itemIds.take(8).forEach { id ->
                        val depth = SheetGroupingRules.localDepth(id, group.itemIds, parentById)
                        append("   ${"  ".repeat(depth)}• ${itemLabels[id] ?: id}\n")
                    }
                    if (group.itemIds.size > 8) append("   … ещё ${group.itemIds.size - 8}\n")
                }
            }
            val options = arrayOf("+ Группа", "Переименовать", "Переместить", "Элемент ↑", "Элемент ↓", "Группа ↑", "Группа ↓", "Удалить", "Закрыть")
            val choice = JOptionPane.showOptionDialog(
                this,
                JTextArea(summary, 24, 58).apply { isEditable = false; background = surface; foreground = textColor; caretPosition = 0 },
                "Группы · ${kind.title}",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options.last(),
            )
            when (choice) {
                0 -> {
                    val name = JOptionPane.showInputDialog(this, "Название новой группы:")?.trim().orEmpty()
                    if (name.isNotBlank()) groups = SheetGroupingRules.addGroup(groups, "custom:${UUID.randomUUID()}", name)
                }
                1 -> chooseGroup("Переименовать группу", includeUngrouped = false)?.let { group ->
                    val name = JOptionPane.showInputDialog(this, "Новое название:", group.title)?.trim().orEmpty()
                    if (name.isNotBlank()) groups = SheetGroupingRules.renameGroup(groups, group.id, name)
                }
                2 -> {
                    val itemId = chooseItem("Переместить элемент") ?: continue
                    val target = chooseGroup("Куда переместить?") ?: continue
                    val targetIndex = target.itemIds.size
                    val movingIds = if (itemId in treeRootIds) {
                        SheetGroupingRules.subtreeBlock(itemId, parentById, groups.flatMap { it.itemIds })
                    } else listOf(itemId)
                    groups = SheetGroupingRules.moveItems(groups, movingIds, target.id, targetIndex)
                    if (kind == GroupingKind.DEVELOPMENT) groups = groups.map { groupItem ->
                        groupItem.copy(itemIds = SheetGroupingRules.hierarchicalOrder(groupItem.itemIds, parentById))
                    }
                }
                3, 4 -> {
                    val itemId = chooseItem(if (choice == 3) "Поднять элемент" else "Опустить элемент") ?: continue
                    val group = groups.firstOrNull { itemId in it.itemIds } ?: continue
                    val movingIds = if (itemId in treeRootIds) {
                        SheetGroupingRules.subtreeBlock(itemId, parentById, group.itemIds)
                    } else listOf(itemId)
                    val firstIndex = movingIds.mapNotNull(group.itemIds::indexOf).filter { index -> index >= 0 }.minOrNull() ?: continue
                    val maxTarget = (group.itemIds.size - movingIds.size).coerceAtLeast(0)
                    val targetIndex = (firstIndex + if (choice == 3) -1 else 1).coerceIn(0, maxTarget)
                    groups = SheetGroupingRules.moveItems(groups, movingIds, group.id, targetIndex)
                    if (kind == GroupingKind.DEVELOPMENT) groups = groups.map { groupItem ->
                        groupItem.copy(itemIds = SheetGroupingRules.hierarchicalOrder(groupItem.itemIds, parentById))
                    }
                }
                5 -> chooseGroup("Поднять группу")?.let { groups = SheetGroupingRules.moveGroup(groups, it.id, -1) }
                6 -> chooseGroup("Опустить группу")?.let { groups = SheetGroupingRules.moveGroup(groups, it.id, 1) }
                7 -> chooseGroup("Удалить группу", includeUngrouped = false)?.let { groups = SheetGroupingRules.deleteGroup(groups, it.id, ungroupedId) }
                else -> {
                    persist()
                    recentChange = RecentChange("Группировка листа изменена", character, sheetExtras)
                    showSection(selected)
                    return
                }
            }
            persist()
        }
    }

    private fun showSchoolDialog(school: String) {
        val existingIndex = session.active.magic.schools.indexOfFirst { MagicSchoolCatalog.canonicalizeOrNull(it.name) == school }
        val existing = session.active.magic.schools.getOrNull(existingIndex) ?: MagicSchool(school, 0, "")
        val rank = JTextField(existing.rank.toString(), 6); val note = JTextArea(existing.note, 4, 28).apply { lineWrap = true; wrapStyleWord = true }
        val panel = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(formPanel("Сила" to rank)); add(JLabel("Заметка").apply { foreground = textColor }); add(JScrollPane(note)) }
        if (dialog(panel, school)) {
            val value = rank.text.toIntOrNull()?.coerceAtLeast(0) ?: 0
            if (existingIndex >= 0) { session.updateMagicSchool(existingIndex, school, value, note.text) } else if (value > 0) { session.addMagicSchool(school, value, note.text) }
            showSection(selected)
        }
    }

    private fun showSpellDialog(spell: KnownSpell?) {
        val name = JTextField(spell?.name.orEmpty(), 20); val school = JTextField(spell?.school.orEmpty(), 18)
        val cost = JTextField((spell?.cost ?: 0).toString(), 5); val manaText = JTextField(spell?.manaText.orEmpty(), 12); val time = JTextField(spell?.time.orEmpty(), 12); val range = JTextField(spell?.range.orEmpty(), 12); val area = JTextField(spell?.area.orEmpty(), 12); val action = JTextField(spell?.action.orEmpty(), 12); val duration = JTextField(spell?.duration.orEmpty(), 12)
        val description = JTextArea(spell?.description.orEmpty(), 5, 30).apply { lineWrap = true; wrapStyleWord = true }; val enhancement = JTextArea(spell?.enhancement.orEmpty(), 4, 30).apply { lineWrap = true; wrapStyleWord = true }; val learned = JCheckBox("Изучено", spell?.learned ?: true).apply { background = surface; foreground = textColor }; val xp = JTextField(spell?.xpOverride?.toString().orEmpty(), 6)
        val panel = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(formPanel("Название" to name, "Школа" to school, "Стоимость маны" to cost, "Текст маны" to manaText, "Время" to time, "Дальность" to range, "Область" to area, "Действие" to action, "Длительность" to duration, "XP override" to xp)); add(learned); add(JLabel("Описание").apply { foreground = textColor }); add(JScrollPane(description)); add(JLabel("Усиление").apply { foreground = textColor }); add(JScrollPane(enhancement)) }
        if (!dialog(JScrollPane(panel).apply { preferredSize = Dimension(620, 650) }, if (spell == null) "Своё заклинание" else "Изменить заклинание")) return
        val built = KnownSpell(uid = spell?.uid.orEmpty(), catalogId = spell?.catalogId, name = name.text.trim().ifBlank { "Заклинание" }, school = school.text.trim(), cost = cost.text.toIntOrNull()?.coerceAtLeast(0) ?: 0, manaText = manaText.text, time = time.text, range = range.text, area = area.text, action = action.text, duration = duration.text, description = description.text, enhancement = enhancement.text, learned = learned.isSelected, xpOverride = xp.text.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()?.coerceAtLeast(0), incomplete = spell?.incomplete ?: false, conflictNote = spell?.conflictNote.orEmpty(), custom = spell?.custom ?: true)
        if (spell == null) session.addCustomSpell(built) else session.updateSpell(spell.uid) { built.copy(uid = spell.uid) }
        showSection(selected)
    }

    private fun showManualLoadDialog() {
        val input = JTextField(formatNumber(session.active.gear.loadManual), 10)
        if (dialog(formPanel("Ручная нагрузка" to input), "Нагрузка")) mutate { setGearManualLoad(input.text.replace(',', '.').toDoubleOrNull() ?: 0.0) }
    }

    private fun showGearDialog(item: GearItem?) {
        val name = JTextField(item?.name.orEmpty(), 20); val quantity = JTextField((item?.quantity ?: 1).toString(), 6); val load = JTextField(formatNumber(item?.load ?: 0.0), 8); val category = JTextField(item?.category ?: "Снаряжение", 16); val section = JTextField(item?.section ?: "Предметы", 16); val carried = JCheckBox("Несётся", item?.carried ?: true).apply { background = surface; foreground = textColor }; val description = JTextArea(item?.description.orEmpty(), 5, 30).apply { lineWrap = true; wrapStyleWord = true }
        val panel = JPanel().apply { background = surface; layout = BoxLayout(this, BoxLayout.Y_AXIS); add(formPanel("Название" to name, "Количество" to quantity, "Вес одной вещи" to load, "Категория" to category, "Раздел" to section)); add(carried); add(JLabel("Описание").apply { foreground = textColor }); add(JScrollPane(description)) }
        if (!dialog(panel, if (item == null) "Свой предмет" else "Изменить предмет")) return
        val built = GearItem(uid = item?.uid.orEmpty(), catalogId = item?.catalogId, name = name.text.trim().ifBlank { "Предмет" }, quantity = quantity.text.toIntOrNull()?.coerceAtLeast(1) ?: 1, load = load.text.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0, carried = carried.isSelected, description = description.text, category = category.text.trim().ifBlank { "Снаряжение" }, section = section.text.trim().ifBlank { "Предметы" }, fields = item?.fields ?: emptyMap(), custom = item?.custom ?: true)
        if (item == null) session.addCustomGear(built) else session.updateGearItem(item.uid) { built.copy(uid = item.uid) }
        showSection(selected)
    }
    // endregion

    private fun catalogSpellDetails(spell: SpellCatalogEntry, usability: SpellUsability): String = buildString {
        append("Школа: ${spell.school}\nМана: ${spell.cost}\nВремя: ${spell.time}\nДальность: ${spell.range}\nОбласть: ${spell.area}\nПроверка: ${spell.action}\nДлительность: ${spell.duration}")
        if (spell.description.isNotBlank()) append("\n\n${spell.description}")
        if (spell.enhancement.isNotBlank()) append("\n\nУсиление: ${spell.enhancement}")
        if (spell.conflictNote.isNotBlank()) append("\n\nПримечание: ${spell.conflictNote}")
        if (!usability.usable) append("\n\nНедоступно: нужна сила школы ${usability.requiredPower}")
    }

    private fun spellDetails(spell: KnownSpell, usability: SpellUsability): String = buildString {
        append("Школа: ${spell.school}\nМана: ${spell.cost}\nВремя: ${spell.time}\nДальность: ${spell.range}\nОбласть: ${spell.area}\nДлительность: ${spell.duration}\n\n${spell.description}")
        if (spell.enhancement.isNotBlank()) append("\n\nУсиление: ${spell.enhancement}")
        if (!usability.usable) append("\n\nНедоступно: нужна сила школы ${usability.requiredPower}")
    }

    private fun formPanel(vararg fields: Pair<String, Component>) = JPanel(GridLayout(0, 2, 8, 8)).apply {
        background = surface; border = EmptyBorder(8, 8, 8, 8)
        fields.forEach { (label, component) -> add(formLabel(label)); add(component) }
    }

    private fun dialog(component: Component, title: String): Boolean = JOptionPane.showConfirmDialog(this, component, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION
    private fun confirm(message: String): Boolean = JOptionPane.showConfirmDialog(this, message, "FURY", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION
    private fun info(title: String, message: String) = JOptionPane.showMessageDialog(this, JTextArea(message, 12, 48).apply { isEditable = false; lineWrap = true; wrapStyleWord = true; background = surface; foreground = textColor }, title, JOptionPane.INFORMATION_MESSAGE)
    private fun error(message: String) = JOptionPane.showMessageDialog(this, message, "FURY", JOptionPane.ERROR_MESSAGE)
}

private fun <T> titleRenderer(label: (T) -> String) = object : javax.swing.DefaultListCellRenderer() {
    @Suppress("UNCHECKED_CAST")
    override fun getListCellRendererComponent(list: javax.swing.JList<*>?, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also { if (value != null) (it as JLabel).text = label(value as T) }
    }
}
