package com.dubl.character.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Shared sheet-content interaction hook.
 *
 * Material3 already owns nested scroll for ModalBottomSheet. Consuming residual
 * vertical scroll here makes the sheet fight its own drag gesture and causes the
 * intermittent "jerk" users reported. Keep only keyboard dismissal and let the
 * sheet receive all remaining scroll/fling deltas.
 */
@Composable
fun Modifier.containSheetOverscroll(): Modifier = dismissKeyboardOnPointerDown()
