package com.dubl.character.desktop.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Desktop-only palette.  The shared Android palette intentionally stays unchanged;
// these values target the darker, higher-contrast desktop reference UI.
internal val DesktopBackground = Color(0xFF080D13)
internal val DesktopSurface = Color(0xFF101821)
internal val DesktopSurfaceRaised = Color(0xFF151F2A)
internal val DesktopSurfaceInset = Color(0xFF0B1219)
internal val DesktopBorder = Color(0xFF263544)
internal val DesktopText = Color(0xFFF6F7F9)
internal val DesktopMuted = Color(0xFF9AA4B2)
internal val DesktopAccent = Color(0xFFF05B70)
internal val DesktopAccentSoft = Color(0xFF3F222C)
internal val DesktopGold = Color(0xFFF4C460)
internal val DesktopHealth = Color(0xFFF05B70)
internal val DesktopStamina = Color(0xFFE8BA60)
internal val DesktopMana = Color(0xFF66A7FF)
internal val DesktopCustomResource = Color(0xFF70C3AE)

@Composable
internal fun DesktopPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DesktopSurface,
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = 0.88f)),
    ) {
        content()
    }
}

@Composable
internal fun DesktopSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: DesktopIconKind? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) DesktopIcon(icon, tint = DesktopMuted, size = 22.dp)
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = DesktopMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        action?.invoke()
    }
}

@Composable
internal fun DesktopHeroPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DesktopSurfaceRaised,
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = 0.90f)),
    ) {
        content()
    }
}

@Composable
internal fun DesktopSmallAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    emphasized: Boolean = false,
) {
    Surface(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(9.dp),
        color = if (emphasized) DesktopAccent else DesktopSurfaceInset,
        border = BorderStroke(1.dp, if (emphasized) DesktopAccent else DesktopBorder.copy(alpha = .88f)),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            maxLines = 1,
            color = when {
                !enabled -> DesktopMuted
                emphasized -> Color.White
                else -> DesktopText
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun DesktopConditionChip(
    label: String,
    modifier: Modifier = Modifier,
    automatic: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val interactive = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Surface(
        modifier = interactive,
        shape = RoundedCornerShape(999.dp),
        color = if (automatic) DesktopAccentSoft.copy(alpha = 0.72f) else DesktopSurfaceInset,
        border = BorderStroke(1.dp, if (automatic) DesktopAccent.copy(alpha = 0.42f) else DesktopBorder.copy(alpha = 0.74f)),
    ) {
        Text(
            if (automatic) "$label · авто" else label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (automatic) DesktopAccent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
internal fun DesktopResourceRow(
    title: String,
    current: Int,
    maximum: Int,
    tint: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    onSecondary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
) {
    val fraction = if (maximum <= 0) 0f else (current.toFloat() / maximum.toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$current / $maximum", color = tint, fontWeight = FontWeight.Bold)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(DesktopSurfaceInset),
        ) {
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction)
                        .height(6.dp)
                        .background(tint.copy(alpha = 0.86f)),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            DesktopSmallAction("−", onMinus, Modifier.width(48.dp))
            DesktopSmallAction("+", onPlus, Modifier.width(48.dp))
            if (onSecondary != null && !secondaryLabel.isNullOrBlank()) {
                TextButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
        }
    }
}

@Composable
internal fun DesktopStatCell(
    title: String,
    value: String,
    meta: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(11.dp),
        color = DesktopSurfaceInset.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = 0.62f)),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = DesktopMuted, style = MaterialTheme.typography.labelLarge)
                    Text(value, color = DesktopAccent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (!secondary.isNullOrBlank()) Text(secondary, color = DesktopMuted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onRoll) { Text("Бросок") }
            }
            Text(meta, color = DesktopMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DesktopSmallAction("−", onMinus, Modifier.width(48.dp))
                DesktopSmallAction("+", onPlus, Modifier.width(48.dp))
            }
        }
    }
}

@Composable
internal fun DesktopMetricCell(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = DesktopAccent,
    onClick: (() -> Unit)? = null,
) {
    val interactive = if (onClick != null) modifier.clickable(onClick = onClick) else modifier
    Surface(
        modifier = interactive,
        shape = RoundedCornerShape(10.dp),
        color = DesktopSurfaceInset.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = 0.54f)),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = DesktopMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(value, color = tint, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DesktopSectionHeader(title = title, action = action)
            content()
        }
    }
}

@Composable
internal fun KeyValue(label: String, value: String, tint: Color = DesktopAccent) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, color = DesktopMuted, modifier = Modifier.weight(1f))
        Text(value, color = tint, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun RankStepper(rank: Int, min: Int = 0, max: Int, enabled: Boolean = true, onChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(enabled = enabled && rank > min, onClick = { onChange(rank - 1) }) { Text("−") }
        Text(rank.toString(), fontWeight = FontWeight.Bold)
        Button(enabled = enabled && rank < max, onClick = { onChange(rank + 1) }) { Text("+") }
    }
}

@Composable
internal fun EmptyState(text: String) {
    Spacer(Modifier.height(4.dp))
    Text(text, color = DesktopMuted, style = MaterialTheme.typography.bodyMedium)
}

internal fun signed(value: Int): String = if (value >= 0) "+$value" else value.toString()
internal fun formatNumber(value: Double): String = if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')


enum class DesktopIconKind {
    SHEET, SKILLS, DEVELOPMENT, MAGIC, EQUIPMENT, CHARACTERS, SETTINGS,
    PORTRAIT, HEALTH, ENDURANCE, MANA, CHI,
    STRENGTH, CONSTITUTION, DEXTERITY, SPEED, INTELLIGENCE, PERCEPTION, WILL, CHARISMA,
    DEFENSE, REFLEXES, INITIATIVE, FORTITUDE, RUN, SIZE, NOTES, DICE, DETAIL, GENERIC_SKILL,
}

@Composable
internal fun DesktopIcon(
    kind: DesktopIconKind,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    size: Dp = 18.dp,
) {
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = (w * 0.10f).coerceAtLeast(1.4f))
        val thin = Stroke(width = (w * 0.075f).coerceAtLeast(1.1f))
        fun line(x1: Float, y1: Float, x2: Float, y2: Float, st: Stroke = stroke) =
            drawLine(tint, start = androidx.compose.ui.geometry.Offset(w * x1, h * y1), end = androidx.compose.ui.geometry.Offset(w * x2, h * y2), strokeWidth = st.width)
        fun circle(x: Float, y: Float, r: Float, st: Stroke = stroke) =
            drawCircle(tint, radius = w * r, center = androidx.compose.ui.geometry.Offset(w * x, h * y), style = st)
        when (kind) {
            DesktopIconKind.HEALTH, DesktopIconKind.CONSTITUTION, DesktopIconKind.FORTITUDE -> {
                val p = Path().apply {
                    moveTo(w * .5f, h * .84f); cubicTo(w * .16f, h * .62f, w * .08f, h * .34f, w * .27f, h * .23f)
                    cubicTo(w * .40f, h * .15f, w * .49f, h * .26f, w * .5f, h * .32f)
                    cubicTo(w * .51f, h * .26f, w * .60f, h * .15f, w * .73f, h * .23f)
                    cubicTo(w * .92f, h * .34f, w * .84f, h * .62f, w * .5f, h * .84f)
                    close()
                }
                drawPath(p, tint, style = stroke)
            }
            DesktopIconKind.ENDURANCE -> {
                val p = Path().apply { moveTo(w*.58f,h*.08f); lineTo(w*.28f,h*.52f); lineTo(w*.49f,h*.52f); lineTo(w*.38f,h*.92f); lineTo(w*.74f,h*.40f); lineTo(w*.53f,h*.40f); close() }
                drawPath(p, tint)
            }
            DesktopIconKind.MANA -> {
                val p = Path().apply { moveTo(w*.5f,h*.08f); cubicTo(w*.30f,h*.38f,w*.20f,h*.51f,w*.20f,h*.67f); cubicTo(w*.20f,h*.86f,w*.35f,h*.94f,w*.5f,h*.94f); cubicTo(w*.65f,h*.94f,w*.80f,h*.86f,w*.80f,h*.67f); cubicTo(w*.80f,h*.51f,w*.70f,h*.38f,w*.5f,h*.08f); close() }
                drawPath(p,tint,style=stroke)
            }
            DesktopIconKind.CHI -> {
                drawArc(color=tint, startAngle=30f, sweepAngle=280f, useCenter=false, topLeft=androidx.compose.ui.geometry.Offset(w*.16f,h*.16f), size=androidx.compose.ui.geometry.Size(w*.68f,h*.68f), style=stroke)
                circle(.52f,.50f,.08f,thin)
            }
            DesktopIconKind.STRENGTH -> {
                line(.18f,.38f,.18f,.68f); line(.30f,.30f,.30f,.76f); line(.30f,.53f,.70f,.53f); line(.70f,.30f,.70f,.76f); line(.82f,.38f,.82f,.68f)
            }
            DesktopIconKind.DEXTERITY -> { line(.18f,.74f,.80f,.22f); line(.28f,.76f,.82f,.42f,thin); line(.18f,.52f,.58f,.18f,thin) }
            DesktopIconKind.SPEED, DesktopIconKind.RUN -> { line(.15f,.34f,.62f,.34f); line(.35f,.54f,.82f,.54f); line(.20f,.74f,.63f,.74f); line(.62f,.34f,.52f,.25f,thin); line(.82f,.54f,.72f,.45f,thin) }
            DesktopIconKind.INTELLIGENCE -> {
                val p=Path().apply { moveTo(w*.12f,h*.24f); quadraticBezierTo(w*.32f,h*.16f,w*.49f,h*.28f); lineTo(w*.49f,h*.80f); quadraticBezierTo(w*.32f,h*.68f,w*.12f,h*.76f); close() }
                drawPath(p,tint,style=thin); val q=Path().apply { moveTo(w*.88f,h*.24f); quadraticBezierTo(w*.68f,h*.16f,w*.51f,h*.28f); lineTo(w*.51f,h*.80f); quadraticBezierTo(w*.68f,h*.68f,w*.88f,h*.76f); close() }; drawPath(q,tint,style=thin)
            }
            DesktopIconKind.PERCEPTION -> {
                val p=Path().apply { moveTo(w*.08f,h*.50f); quadraticBezierTo(w*.28f,h*.22f,w*.50f,h*.22f); quadraticBezierTo(w*.72f,h*.22f,w*.92f,h*.50f); quadraticBezierTo(w*.72f,h*.78f,w*.50f,h*.78f); quadraticBezierTo(w*.28f,h*.78f,w*.08f,h*.50f) }; drawPath(p,tint,style=thin); circle(.5f,.5f,.12f,thin)
            }
            DesktopIconKind.WILL -> { line(.5f,.08f,.5f,.92f,thin); line(.08f,.5f,.92f,.5f,thin); line(.20f,.20f,.80f,.80f,thin); line(.80f,.20f,.20f,.80f,thin); circle(.5f,.5f,.08f,thin) }
            DesktopIconKind.CHARISMA, DesktopIconKind.CHARACTERS -> { circle(.36f,.34f,.12f,thin); circle(.66f,.38f,.10f,thin); drawArc(color=tint, startAngle=200f, sweepAngle=140f, useCenter=false, topLeft=androidx.compose.ui.geometry.Offset(w*.14f,h*.42f), size=androidx.compose.ui.geometry.Size(w*.44f,h*.46f), style=thin); drawArc(color=tint, startAngle=205f, sweepAngle=130f, useCenter=false, topLeft=androidx.compose.ui.geometry.Offset(w*.48f,h*.48f), size=androidx.compose.ui.geometry.Size(w*.40f,h*.38f), style=thin) }
            DesktopIconKind.DEFENSE -> { val p=Path().apply { moveTo(w*.5f,h*.08f); lineTo(w*.82f,h*.20f); lineTo(w*.78f,h*.62f); quadraticBezierTo(w*.68f,h*.82f,w*.5f,h*.92f); quadraticBezierTo(w*.32f,h*.82f,w*.22f,h*.62f); lineTo(w*.18f,h*.20f); close() }; drawPath(p,tint,style=stroke) }
            DesktopIconKind.REFLEXES -> { drawArc(color=tint, startAngle=200f, sweepAngle=250f, useCenter=false, topLeft=androidx.compose.ui.geometry.Offset(w*.16f,h*.16f), size=androidx.compose.ui.geometry.Size(w*.68f,h*.68f), style=stroke); line(.18f,.34f,.18f,.12f,thin); line(.18f,.12f,.38f,.18f,thin) }
            DesktopIconKind.INITIATIVE -> { line(.22f,.12f,.78f,.12f); line(.22f,.88f,.78f,.88f); line(.30f,.16f,.70f,.84f,thin); line(.70f,.16f,.30f,.84f,thin) }
            DesktopIconKind.SIZE -> { circle(.5f,.20f,.10f,thin); line(.5f,.30f,.5f,.68f); line(.30f,.44f,.70f,.44f); line(.5f,.68f,.32f,.92f); line(.5f,.68f,.68f,.92f) }
            DesktopIconKind.DICE, DesktopIconKind.GENERIC_SKILL -> { drawRoundRect(tint, topLeft=androidx.compose.ui.geometry.Offset(w*.14f,h*.14f), size=androidx.compose.ui.geometry.Size(w*.72f,h*.72f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.12f,w*.12f), style=thin); drawCircle(tint,w*.045f,androidx.compose.ui.geometry.Offset(w*.34f,h*.34f)); drawCircle(tint,w*.045f,androidx.compose.ui.geometry.Offset(w*.66f,h*.66f)); drawCircle(tint,w*.045f,androidx.compose.ui.geometry.Offset(w*.50f,h*.50f)) }
            DesktopIconKind.NOTES -> { drawRoundRect(tint, topLeft=androidx.compose.ui.geometry.Offset(w*.20f,h*.10f), size=androidx.compose.ui.geometry.Size(w*.60f,h*.80f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f), style=thin); line(.32f,.38f,.68f,.38f,thin); line(.32f,.53f,.68f,.53f,thin); line(.32f,.68f,.58f,.68f,thin) }
            DesktopIconKind.MAGIC -> { circle(.5f,.5f,.28f,thin); line(.5f,.06f,.5f,.24f,thin); line(.5f,.76f,.5f,.94f,thin); line(.06f,.5f,.24f,.5f,thin); line(.76f,.5f,.94f,.5f,thin) }
            DesktopIconKind.EQUIPMENT -> { drawRoundRect(tint, topLeft=androidx.compose.ui.geometry.Offset(w*.16f,h*.30f), size=androidx.compose.ui.geometry.Size(w*.68f,h*.52f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.08f,w*.08f), style=thin); drawArc(color=tint, startAngle=180f, sweepAngle=180f, useCenter=false, topLeft=androidx.compose.ui.geometry.Offset(w*.34f,h*.12f), size=androidx.compose.ui.geometry.Size(w*.32f,h*.34f), style=thin) }
            DesktopIconKind.DEVELOPMENT -> { circle(.50f,.50f,.12f,thin); circle(.50f,.18f,.07f,thin); circle(.80f,.50f,.07f,thin); circle(.50f,.82f,.07f,thin); circle(.20f,.50f,.07f,thin); line(.50f,.25f,.50f,.38f,thin); line(.62f,.50f,.73f,.50f,thin); line(.50f,.62f,.50f,.75f,thin); line(.27f,.50f,.38f,.50f,thin) }
            DesktopIconKind.SKILLS -> { circle(.5f,.5f,.30f,thin); line(.5f,.20f,.5f,.80f,thin); line(.20f,.5f,.80f,.5f,thin); circle(.5f,.5f,.07f,thin) }
            DesktopIconKind.SHEET -> { drawRoundRect(tint, topLeft=androidx.compose.ui.geometry.Offset(w*.18f,h*.10f), size=androidx.compose.ui.geometry.Size(w*.64f,h*.80f), cornerRadius=androidx.compose.ui.geometry.CornerRadius(w*.06f,w*.06f), style=thin); line(.32f,.32f,.68f,.32f,thin); line(.32f,.48f,.68f,.48f,thin); line(.32f,.64f,.56f,.64f,thin) }
            DesktopIconKind.SETTINGS -> { circle(.5f,.5f,.26f,thin); circle(.5f,.5f,.08f,thin); line(.5f,.06f,.5f,.22f,thin); line(.5f,.78f,.5f,.94f,thin); line(.06f,.5f,.22f,.5f,thin); line(.78f,.5f,.94f,.5f,thin) }
            DesktopIconKind.PORTRAIT -> { val p=Path().apply { moveTo(w*.50f,h*.08f); lineTo(w*.76f,h*.26f); lineTo(w*.68f,h*.76f); lineTo(w*.50f,h*.92f); lineTo(w*.32f,h*.76f); lineTo(w*.24f,h*.26f); close() }; drawPath(p,tint,style=thin) }
            DesktopIconKind.DETAIL -> { line(.35f,.20f,.66f,.50f,thin); line(.66f,.50f,.35f,.80f,thin) }
        }
    }
}

@Composable
internal fun DesktopTinyButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier = modifier.size(28.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = DesktopSurfaceInset,
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = .88f)),
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else DesktopMuted, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun DesktopIconButton(
    kind: DesktopIconKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Surface(
        modifier = modifier.size(28.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(7.dp),
        color = DesktopSurfaceInset,
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = .88f)),
    ) { Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { DesktopIcon(kind, tint = tint, size = 14.dp) } }
}

@Composable
internal fun DesktopDenseAttributeRow(
    icon: DesktopIconKind,
    title: String,
    value: String,
    onRoll: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, RoundedCornerShape(8.dp), DesktopSurfaceInset.copy(alpha=.72f), border=BorderStroke(1.dp,DesktopBorder.copy(alpha=.68f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=9.dp, vertical=3.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            DesktopIcon(icon, tint=DesktopAccent, size=20.dp)
            Text(title, modifier=Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
            Text(value, fontWeight=FontWeight.Bold, style=MaterialTheme.typography.titleMedium)
            DesktopIconButton(DesktopIconKind.DICE, onRoll)
            DesktopTinyButton("−", onMinus)
            DesktopTinyButton("+", onPlus)
        }
    }
}

@Composable
internal fun DesktopDenseMetricRow(
    icon: DesktopIconKind,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val interactive = if (onClick != null) modifier.clickable(onClick=onClick) else modifier
    Surface(interactive, RoundedCornerShape(8.dp), DesktopSurfaceInset.copy(alpha=.72f), border=BorderStroke(1.dp,DesktopBorder.copy(alpha=.68f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=10.dp, vertical=5.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            val iconTint = when (icon) {
                DesktopIconKind.FORTITUDE, DesktopIconKind.RUN -> DesktopAccent
                else -> DesktopMuted
            }
            DesktopIcon(icon, tint=iconTint, size=20.dp)
            Text(title, modifier=Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
            Text(value, fontWeight=FontWeight.Bold, style=MaterialTheme.typography.titleMedium)
            if (onClick != null) DesktopIcon(DesktopIconKind.DETAIL, tint=DesktopMuted, size=12.dp)
        }
    }
}

@Composable
internal fun DesktopHeroAttributeCell(
    icon: DesktopIconKind,
    title: String,
    value: String,
    onRoll: () -> Unit,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DesktopSurfaceInset.copy(alpha = .76f),
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = .62f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            DesktopIcon(icon, tint = DesktopAccent, size = 18.dp)
            Text(
                title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(value, color = DesktopText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DesktopIconButton(DesktopIconKind.DICE, onRoll, tint = DesktopAccent)
            DesktopTinyButton("−", onMinus)
            DesktopTinyButton("+", onPlus)
        }
    }
}

@Composable
internal fun DesktopHeroMetricCell(
    icon: DesktopIconKind,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    tint: Color = DesktopMuted,
    onRoll: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DesktopSurfaceInset.copy(alpha = .70f),
        border = BorderStroke(1.dp, DesktopBorder.copy(alpha = .56f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            DesktopIcon(icon, tint = tint, size = 18.dp)
            Text(
                title,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(value, color = DesktopText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (onRoll != null) DesktopIconButton(DesktopIconKind.DICE, onRoll, tint = DesktopAccent)
        }
    }
}

@Composable
internal fun DesktopSkillRow(
    icon: DesktopIconKind,
    title: String,
    rank: Int,
    bonus: String,
    onRoll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier, RoundedCornerShape(8.dp), DesktopSurfaceInset.copy(alpha=.72f), border=BorderStroke(1.dp,DesktopBorder.copy(alpha=.68f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal=11.dp, vertical=6.dp), verticalAlignment=Alignment.CenterVertically, horizontalArrangement=Arrangement.spacedBy(9.dp)) {
            DesktopIcon(icon, tint=DesktopMuted, size=20.dp)
            Text(title, modifier=Modifier.weight(1f), maxLines=1, overflow=TextOverflow.Ellipsis)
            Text("Ранг $rank", color=DesktopMuted, style=MaterialTheme.typography.labelMedium, fontWeight=FontWeight.SemiBold)
            Text(bonus, color=DesktopAccent, fontWeight=FontWeight.Bold)
            DesktopIconButton(DesktopIconKind.DICE, onRoll)
        }
    }
}

@Composable
internal fun DesktopResourceTile(
    icon: DesktopIconKind,
    title: String,
    current: Int,
    maximum: Int,
    tint: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    onSecondary: (() -> Unit)? = null,
) {
    val fraction = if (maximum <= 0) 0f else (current.toFloat()/maximum.toFloat()).coerceIn(0f,1f)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopIcon(icon, tint = tint, size = 18.dp)
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$current / $maximum", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = DesktopText)
            if (onSecondary != null) {
                Text(
                    "⋯",
                    color = DesktopMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onSecondary).padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DesktopTinyButton("−", onMinus, Modifier.size(26.dp))
            Box(Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(999.dp)).background(Color(0xFF222D39))) {
                if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).height(5.dp).background(tint.copy(alpha=.94f)))
            }
            DesktopTinyButton("+", onPlus, Modifier.size(26.dp))
        }
    }
}
