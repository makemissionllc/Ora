package com.makemission.ora.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.makemission.ora.ui.theme.OraMotion

/**
 * Gives a touchable element a physical response: it sinks to [OraMotion.PRESS_SCALE] while the finger is
 * down, then springs back with a small overshoot when released. Put it before the element's own clickable,
 * so the ripple and the click are unchanged. It only moves the element; it does not consume the touch.
 */
fun Modifier.pressFeedback(): Modifier = composed {
    val scale = remember { Animatable(1f) }
    val haptics = LocalOraHaptics.current
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    haptics.play(Feel.TICK)
                    scale.animateTo(OraMotion.PRESS_SCALE, OraMotion.pressDown())
                    tryAwaitRelease()
                    scale.animateTo(1f, OraMotion.pressRelease())
                }
            )
        }
}
