package com.makemission.ora.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * The tick that marks the chosen row. It grows in and shrinks out with the shared overlay motion
 * when [visible] changes, so moving a choice from one row to another is seen, not just swapped.
 */
@Composable
fun CheckMark(visible: Boolean, description: String, modifier: Modifier = Modifier, size: Dp = 24.dp) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE),
    ) {
        Icon(Icons.Filled.Check, contentDescription = description, tint = browserColors().Accent, modifier = Modifier.size(size))
    }
}
