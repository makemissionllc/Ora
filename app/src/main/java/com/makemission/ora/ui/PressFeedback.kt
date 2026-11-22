package com.makemission.ora.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.PointerEventPass
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.makemission.ora.ui.theme.OraMotion

/**
 * Gives a touchable element a physical response: it sinks to [OraMotion.PRESS_SCALE] while the finger is
 * down (a little less on wide rows, and not at all on full-screen areas), then springs back with a small overshoot when released. Put it before the element's own clickable,
 * so the ripple and the click are unchanged. It only moves the element; it does not consume the touch.
 */
fun Modifier.pressFeedback(): Modifier = composed {
    // 0 is at rest and 1 is fully pressed; the size of the element decides how far that sinks.
    val pressed = remember { Animatable(0f) }
    val haptics = LocalOraHaptics.current
    val scope = rememberCoroutineScope()
    this
        .graphicsLayer {
            val dp = density
            val wide = size.width / dp > OraMotion.ROW_PRESS_MIN_WIDTH_DP
            val huge = wide && size.height / dp > OraMotion.PRESS_STILL_MIN_DP && size.width / dp > OraMotion.PRESS_STILL_MIN_DP
            val target = when {
                huge -> 1f
                wide -> OraMotion.ROW_PRESS_SCALE
                else -> OraMotion.PRESS_SCALE
            }
            val value = 1f - pressed.value * (1f - target)
            scaleX = value
            scaleY = value
        }
        .pointerInput(Unit) {
            // Watch on the Initial pass and never consume, so a clickable inside or beside still gets the touch.
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                haptics.play(Feel.TICK)
                scope.launch { pressed.animateTo(1f, OraMotion.pressDown()) }
                do {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                } while (event.changes.any { it.pressed })
                scope.launch { pressed.animateTo(0f, OraMotion.pressRelease()) }
            }
        }
}
