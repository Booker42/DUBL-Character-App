package com.dubl.character.android.ui.components

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager

/**
 * Clears text-field focus as soon as the user touches outside the currently
 * focused editor. Child text fields receive the same pointer event afterwards
 * and can immediately request focus again, so tapping another field still works.
 */
@Composable
fun Modifier.dismissKeyboardOnPointerDown(): Modifier {
    val focusManager = LocalFocusManager.current
    return pointerInput(focusManager) {
        awaitEachGesture {
            awaitFirstDown(
                requireUnconsumed = false,
                pass = PointerEventPass.Initial,
            )
            focusManager.clearFocus(force = true)
        }
    }
}
