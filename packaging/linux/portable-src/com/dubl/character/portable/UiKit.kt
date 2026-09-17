package com.dubl.character.portable

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.UIManager
import javax.swing.border.EmptyBorder

val background = Color(0x0E, 0x10, 0x14)
val surface = Color(0x19, 0x1C, 0x23)
val surfaceInset = Color(0x13, 0x16, 0x1B)
val surfaceRaised = Color(0x1D, 0x20, 0x28)
val borderColor = Color(0x30, 0x34, 0x3E)
val textColor = Color(0xEE, 0xE9, 0xE1)
val muted = Color(0xA1, 0xA5, 0xB0)
val accent = Color(0xA8, 0x39, 0x48)
val accentSoft = Color(0x39, 0x25, 0x2E)
val focus = Color(0xD4, 0x86, 0x8F)
val gold = Color(0xD0, 0xAE, 0x7E)
val health = Color(0xB6, 0x58, 0x66)
val stamina = Color(0xB4, 0x9B, 0x68)
val mana = Color(0x80, 0x95, 0xC7)
val chiColor = Color(0x71, 0xA4, 0x92)

fun installDarkDefaults() {
    UIManager.put("Panel.background", background)
    UIManager.put("Label.foreground", textColor)
    UIManager.put("Button.background", surfaceRaised)
    UIManager.put("Button.foreground", textColor)
    UIManager.put("Button.focus", accentSoft)
    UIManager.put("CheckBox.background", surface)
    UIManager.put("CheckBox.foreground", textColor)
    UIManager.put("TextField.background", surfaceInset)
    UIManager.put("TextField.foreground", textColor)
    UIManager.put("TextField.caretForeground", textColor)
    UIManager.put("TextArea.background", surfaceInset)
    UIManager.put("TextArea.foreground", textColor)
    UIManager.put("TextArea.caretForeground", textColor)
    UIManager.put("ComboBox.background", surfaceRaised)
    UIManager.put("ComboBox.foreground", textColor)
    UIManager.put("ScrollPane.background", background)
    UIManager.put("Viewport.background", background)
    UIManager.put("OptionPane.background", surface)
    UIManager.put("OptionPane.messageForeground", textColor)
}

fun pagePanel() = JPanel().apply {
    background = com.dubl.character.portable.background
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = EmptyBorder(24, 28, 30, 28)
}

fun pageHeader(title: String, subtitle: String) = JPanel().apply {
    isOpaque = false
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    alignmentX = Component.LEFT_ALIGNMENT
    maximumSize = Dimension(Int.MAX_VALUE, 84)
    add(JLabel(title).apply { foreground = textColor; font = font.deriveFont(Font.BOLD, 25f) })
    add(Box.createVerticalStrut(4))
    add(JLabel(subtitle).apply { foreground = muted; font = font.deriveFont(14f) })
    add(Box.createVerticalStrut(16))
}

fun card(content: JPanel.() -> Unit) = JPanel().apply {
    background = surface
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
    border = BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(borderColor, 1, true),
        EmptyBorder(16, 16, 16, 16),
    )
    alignmentX = Component.LEFT_ALIGNMENT
    maximumSize = Dimension(Int.MAX_VALUE, 20000)
    content()
}.also { it.putClientProperty("dubl.card", true) }

fun JPanel.heading(value: String, size: Float = 17f) {
    add(JLabel(value).apply { foreground = textColor; font = font.deriveFont(Font.BOLD, size); alignmentX = Component.LEFT_ALIGNMENT })
}

fun JPanel.mutedLabel(value: String, size: Float = 13f) {
    val rows = ((value.length + 89) / 90).coerceIn(1, 6)
    add(JTextArea(value).apply {
        isEditable = false
        isOpaque = false
        lineWrap = true
        wrapStyleWord = true
        foreground = muted
        font = font.deriveFont(size)
        border = null
        alignmentX = Component.LEFT_ALIGNMENT
        minimumSize = Dimension(120, rows * 18)
        preferredSize = Dimension(320, rows * 18)
        maximumSize = Dimension(Int.MAX_VALUE, rows * 20 + 4)
    })
}

fun html(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>")

fun JPanel.verticalGap(px: Int) { add(Box.createVerticalStrut(px)) }

fun statTile(label: String, value: String, tint: Color = focus) = JPanel(BorderLayout()).apply {
    background = surfaceRaised
    border = BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(borderColor, 1, true), EmptyBorder(9, 11, 9, 11))
    add(JLabel(label).apply { foreground = muted }, BorderLayout.NORTH)
    add(JLabel(value).apply { foreground = tint; font = font.deriveFont(Font.BOLD, 17f) }, BorderLayout.CENTER)
}

fun actionButton(label: String, action: () -> Unit) = JButton(label).apply {
    isFocusPainted = false
    margin = Insets(6, 12, 6, 12)
    addActionListener { action() }
}

fun smallButton(label: String, action: () -> Unit) = JButton(label).apply {
    isFocusPainted = false
    margin = Insets(3, 8, 3, 8)
    addActionListener { action() }
}

fun dangerButton(label: String, action: () -> Unit) = actionButton(label, action).apply {
    background = Color(0x45, 0x23, 0x2B)
    foreground = Color(0xF0, 0xB0, 0xB7)
}

fun formLabel(value: String) = JLabel(value).apply { foreground = textColor }

fun textField(value: String, columns: Int = 18) = JTextField(value, columns)
fun textArea(value: String, rows: Int = 5, columns: Int = 32) = JTextArea(value, rows, columns).apply {
    lineWrap = true; wrapStyleWord = true; border = EmptyBorder(6, 7, 6, 7)
}
fun checkBox(label: String, selected: Boolean, action: ((Boolean) -> Unit)? = null) = JCheckBox(label, selected).apply {
    action?.let { listener -> addActionListener { listener(isSelected) } }
}

fun scroll(content: Component): JScrollPane = JScrollPane(content).apply {
    border = null
    viewport.background = background
    horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    verticalScrollBar.unitIncrement = 18
}

fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

fun rowPanel(gap: Int = 8, content: JPanel.() -> Unit) = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT, gap, 4)).apply {
    isOpaque = false
    alignmentX = Component.LEFT_ALIGNMENT
    maximumSize = Dimension(Int.MAX_VALUE, 64)
    content()
}

fun gridPanel(columns: Int, hgap: Int = 8, vgap: Int = 8, content: JPanel.() -> Unit) = JPanel(java.awt.GridLayout(0, columns, hgap, vgap)).apply {
    isOpaque = false
    alignmentX = Component.LEFT_ALIGNMENT
    maximumSize = Dimension(Int.MAX_VALUE, 10000)
    content()
}

fun JPanel.cardGap() { add(Box.createVerticalStrut(12)) }

fun infoLabel(value: String, tint: Color = muted) = JLabel(value).apply { foreground = tint }

fun confirm(parent: Component, message: String, title: String = "FURY"): Boolean =
    javax.swing.JOptionPane.showConfirmDialog(parent, message, title, javax.swing.JOptionPane.YES_NO_OPTION) == javax.swing.JOptionPane.YES_OPTION

fun message(parent: Component, message: String, title: String = "FURY") {
    javax.swing.JOptionPane.showMessageDialog(parent, message, title, javax.swing.JOptionPane.INFORMATION_MESSAGE)
}

fun prompt(parent: Component, message: String, initial: String = ""): String? =
    javax.swing.JOptionPane.showInputDialog(parent, message, initial)?.toString()

fun promptInt(parent: Component, message: String, initial: Int): Int? = prompt(parent, message, initial.toString())?.trim()?.toIntOrNull()
fun promptDouble(parent: Component, message: String, initial: Double): Double? = prompt(parent, message, initial.toString().replace(".0", ""))?.trim()?.replace(',', '.')?.toDoubleOrNull()
