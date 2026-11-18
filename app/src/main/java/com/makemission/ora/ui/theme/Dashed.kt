package com.makemission.ora.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A dashed outline. In Ora a dashed line means private or temporary and nothing else (AGENTS.md rule 28):
 * the private address capsule, the private new-tab button and the private tab favicon outline.
 */
fun Modifier.dashedOutline(shape: Shape, color: Color, width: Dp = 1.5.dp, dash: Dp = 6.dp, gap: Dp = 4.dp): Modifier =
    drawWithContent {
        drawContent()
        val stroke = Stroke(
            width = width.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash.toPx(), gap.toPx())),
        )
        // Inset by half the stroke so the dashes sit inside the shape's edge instead of being clipped.
        val half = width.toPx() / 2f
        val outline = shape.createOutline(Size(size.width - half * 2, size.height - half * 2), layoutDirection, this)
        translate(half, half) {
            drawOutline(outline, color, style = stroke)
        }
    }
