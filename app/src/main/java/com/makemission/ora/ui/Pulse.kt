package com.makemission.ora.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.makemission.ora.ui.theme.OraMotion

/**
 * Makes a placeholder breathe while [active], so an empty tile reads as "still coming" rather than broken.
 * Flat on purpose (surfaces carry no gradient): only the opacity moves. Stops, and shows full opacity, when
 * [active] is false or the system has animations turned off.
 */
@Composable
fun Modifier.pulse(active: Boolean): Modifier {
    if (!active) return this
    val transition = rememberInfiniteTransition(label = "pulse")
    val a by transition.animateFloat(initialValue = 1f, targetValue = 0.55f, animationSpec = OraMotion.pulse(), label = "pulse-alpha")
    return alpha(a)
}
