package com.makemission.ora.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.makemission.ora.ui.theme.OraMotion

/** The first whole number in [s], or null. Used only to decide which way a changing count rolls. */
private fun leadingNumber(s: String): Int? = Regex("\\d+").find(s)?.value?.toIntOrNull()

/**
 * Text whose digits roll when it changes: a bigger number slides in from below and the old one leaves upward, a
 * smaller number does the opposite, so a tab count feels like it moved rather than blinked. Text with no number
 * just fades. Uses the shared enter and exit motion, and a changed width eases instead of jumping.
 */
@Composable
fun RollingText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = text,
        modifier = modifier,
        transitionSpec = {
            val from = leadingNumber(initialState)
            val to = leadingNumber(targetState)
            val up = from != null && to != null && to > from
            val down = from != null && to != null && to < from
            val enter = fadeIn(OraMotion.enter())
            val exit = fadeOut(OraMotion.exit())
            val moved = when {
                up -> (enter + slideInVertically(OraMotion.enter()) { it / 2 }) togetherWith (exit + slideOutVertically(OraMotion.exit()) { -it / 2 })
                down -> (enter + slideInVertically(OraMotion.enter()) { -it / 2 }) togetherWith (exit + slideOutVertically(OraMotion.exit()) { it / 2 })
                else -> enter togetherWith exit
            }
            moved.using(SizeTransform(clip = false))
        },
        label = "rolling-text",
    ) { shown ->
        Text(shown, fontSize = fontSize, fontWeight = fontWeight, color = color)
    }
}
