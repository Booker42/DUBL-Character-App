package com.dubl.character.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dubl.character.android.data.CharacterRepository
import com.dubl.character.android.data.CharacterSheetExtrasRepository
import com.dubl.character.android.state.CharacterController
import com.dubl.character.android.ui.components.dismissKeyboardOnPointerDown
import com.dubl.character.android.ui.screens.CharactersScreen
import com.dubl.character.android.ui.screens.EquipmentScreen
import com.dubl.character.android.ui.screens.FeatsScreen
import com.dubl.character.android.ui.screens.MagicScreen
import com.dubl.character.android.ui.screens.OverviewScreen
import com.dubl.character.android.ui.screens.SkillsScreen
import com.dubl.character.android.ui.theme.DublAccentSoft
import com.dubl.character.android.ui.theme.DublFocus
import com.dubl.character.android.ui.theme.DublMuted
import com.dubl.character.android.ui.theme.DublSurfaceInset

private enum class AppSection(val label: String) {
    OVERVIEW("Лист"),
    SKILLS("Умения"),
    FEATS("Навыки"),
    MAGIC("Магия"),
    INVENTORY("Вещи"),
    MORE("Ещё"),
}

@Composable
fun DublApp() {
    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val controller = remember(appContext) {
        CharacterController(CharacterRepository(appContext), CharacterSheetExtrasRepository(appContext))
    }
    var selected by rememberSaveable { mutableStateOf(AppSection.OVERVIEW) }

    Scaffold(
        modifier = Modifier.dismissKeyboardOnPointerDown(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            DublBottomBar(
                selected = selected,
                onSelected = { selected = it },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (selected) {
                AppSection.OVERVIEW -> OverviewScreen(controller)
                AppSection.SKILLS -> SkillsScreen(controller)
                AppSection.FEATS -> FeatsScreen(controller)
                AppSection.MAGIC -> MagicScreen(controller)
                AppSection.INVENTORY -> EquipmentScreen(controller)
                AppSection.MORE -> CharactersScreen(controller)
            }
        }
    }
}

@Composable
private fun DublBottomBar(
    selected: AppSection,
    onSelected: (AppSection) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DublSurfaceInset)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f),
            thickness = 1.dp,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = DublSurfaceInset,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .padding(horizontal = 4.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppSection.entries.forEach { section ->
                    val active = selected == section
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .clickable { onSelected(section) },
                        shape = RoundedCornerShape(13.dp),
                        color = if (active) DublAccentSoft.copy(alpha = 0.92f) else DublSurfaceInset,
                        border = if (active) {
                            BorderStroke(1.dp, DublFocus.copy(alpha = 0.28f))
                        } else {
                            null
                        },
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            DublNavIcon(
                                section = section,
                                color = if (active) DublFocus else DublMuted,
                            )
                            Text(
                                text = section.label,
                                fontSize = 9.5.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                color = if (active) MaterialTheme.colorScheme.onSurface else DublMuted,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun DublNavIcon(
    section: AppSection,
    color: Color,
) {
    Canvas(modifier = Modifier.size(30.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.1.dp.toPx()
        val cap = StrokeCap.Round

        when (section) {
            AppSection.OVERVIEW -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.22f, h * 0.14f),
                    size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.72f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = Stroke(width = stroke),
                )
                drawLine(color, Offset(w * 0.34f, h * 0.40f), Offset(w * 0.66f, h * 0.40f), stroke, cap)
                drawLine(color, Offset(w * 0.34f, h * 0.58f), Offset(w * 0.60f, h * 0.58f), stroke, cap)
            }
            AppSection.SKILLS -> {
                listOf(0.28f, 0.50f, 0.72f).forEach { y ->
                    drawCircle(color, radius = w * 0.055f, center = Offset(w * 0.24f, h * y))
                    drawLine(color, Offset(w * 0.38f, h * y), Offset(w * 0.78f, h * y), stroke, cap)
                }
            }
            AppSection.FEATS -> {
                val top = Offset(w * 0.50f, h * 0.22f)
                val left = Offset(w * 0.28f, h * 0.68f)
                val right = Offset(w * 0.72f, h * 0.68f)
                drawLine(color, top, Offset(w * 0.50f, h * 0.47f), stroke, cap)
                drawLine(color, Offset(w * 0.50f, h * 0.47f), left, stroke, cap)
                drawLine(color, Offset(w * 0.50f, h * 0.47f), right, stroke, cap)
                drawCircle(color, radius = w * 0.075f, center = top)
                drawCircle(color, radius = w * 0.075f, center = left)
                drawCircle(color, radius = w * 0.075f, center = right)
            }
            AppSection.MAGIC -> {
                drawLine(color, Offset(w * 0.50f, h * 0.14f), Offset(w * 0.50f, h * 0.86f), stroke, cap)
                drawLine(color, Offset(w * 0.14f, h * 0.50f), Offset(w * 0.86f, h * 0.50f), stroke, cap)
                drawLine(color, Offset(w * 0.28f, h * 0.28f), Offset(w * 0.72f, h * 0.72f), stroke * 0.8f, cap)
                drawLine(color, Offset(w * 0.72f, h * 0.28f), Offset(w * 0.28f, h * 0.72f), stroke * 0.8f, cap)
                drawCircle(color, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.50f))
            }
            AppSection.INVENTORY -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.28f),
                    size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
                    style = Stroke(width = stroke),
                )
                drawLine(color, Offset(w * 0.32f, h * 0.28f), Offset(w * 0.40f, h * 0.16f), stroke, cap)
                drawLine(color, Offset(w * 0.40f, h * 0.16f), Offset(w * 0.60f, h * 0.16f), stroke, cap)
                drawLine(color, Offset(w * 0.60f, h * 0.16f), Offset(w * 0.68f, h * 0.28f), stroke, cap)
                drawLine(color, Offset(w * 0.18f, h * 0.47f), Offset(w * 0.82f, h * 0.47f), stroke, cap)
            }
            AppSection.MORE -> {
                listOf(0.30f, 0.50f, 0.70f).forEach { x ->
                    drawCircle(color, radius = w * 0.07f, center = Offset(w * x, h * 0.50f))
                }
            }
        }
    }
}
