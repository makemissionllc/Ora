package com.makemission.ora.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.LocalReducedMotion
import com.makemission.ora.ui.theme.browserColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The small hand-drawn pictures shown on empty pages. */
enum class Doodle { HISTORY, DOWNLOAD, READING, SEARCH }

/**
 * An empty state: a small line drawing that moves gently, and one plain sentence under it. Drawn with the palette
 * (Accent for the moving part, SecondaryText for the outline), so it follows Light, Dark, Pure dark and the Private
 * space. With Remove animations on, the picture holds still. It appears with the page it sits on, so it has no
 * trigger of its own to grow out of; its host list animates in from its origin.
 */
@Composable
fun EmptyDoodleState(doodle: Doodle, text: String, modifier: Modifier = Modifier, action: @Composable (() -> Unit)? = null) {
    val colors = browserColors()
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DoodleCanvas(doodle)
        Text(text, color = colors.SecondaryText, fontSize = 14.sp, textAlign = TextAlign.Center)
        action?.invoke()
    }
}

@Composable
private fun DoodleCanvas(doodle: Doodle) {
    val colors = browserColors()
    val still = LocalReducedMotion.current
    val t = rememberInfiniteTransition(label = "doodle")
    val phase = if (still) 0f else t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(DoodleMs, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    ).value
    val outline = colors.SecondaryText
    val accent = colors.Accent
    Canvas(Modifier.size(72.dp).graphicsLayer { alpha = 0.95f }) {
        val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (doodle) {
            Doodle.HISTORY -> history(phase, outline, accent, stroke)
            Doodle.DOWNLOAD -> download(phase, outline, accent, stroke)
            Doodle.READING -> reading(phase, outline, accent, stroke)
            Doodle.SEARCH -> search(phase, outline, accent, stroke)
        }
    }
}

private const val DoodleMs = 3200

private fun DrawScope.history(p: Float, outline: androidx.compose.ui.graphics.Color, accent: androidx.compose.ui.graphics.Color, s: Stroke) {
    val c = center
    val r = size.minDimension * 0.42f
    drawCircle(outline, r, c, style = s)
    // Minute hand turns once per loop, the hour hand a twelfth of that.
    val a = (p * 2 * PI - PI / 2).toFloat()
    val h = (p * 2 * PI / 12 - PI / 2).toFloat()
    drawLine(accent, c, Offset(c.x + cos(a) * r * 0.72f, c.y + sin(a) * r * 0.72f), s.width, StrokeCap.Round)
    drawLine(outline, c, Offset(c.x + cos(h) * r * 0.45f, c.y + sin(h) * r * 0.45f), s.width, StrokeCap.Round)
    drawCircle(accent, s.width * 0.8f, c)
}

private fun DrawScope.download(p: Float, outline: androidx.compose.ui.graphics.Color, accent: androidx.compose.ui.graphics.Color, s: Stroke) {
    val w = size.width
    val tray = Path().apply {
        moveTo(w * 0.15f, w * 0.62f); lineTo(w * 0.15f, w * 0.82f); lineTo(w * 0.85f, w * 0.82f); lineTo(w * 0.85f, w * 0.62f)
    }
    drawPath(tray, outline, style = s)
    // The arrow drops into the tray, then fades and starts again from the top.
    val e = (p * 2).coerceAtMost(1f)
    val y = w * (0.12f + 0.38f * e)
    val alpha = if (p < 0.5f) 1f else 1f - (p - 0.5f) * 2f
    val col = accent.copy(alpha = alpha.coerceIn(0f, 1f))
    drawLine(col, Offset(w / 2, y), Offset(w / 2, y + w * 0.3f), s.width, StrokeCap.Round)
    drawLine(col, Offset(w / 2 - w * 0.12f, y + w * 0.18f), Offset(w / 2, y + w * 0.3f), s.width, StrokeCap.Round)
    drawLine(col, Offset(w / 2 + w * 0.12f, y + w * 0.18f), Offset(w / 2, y + w * 0.3f), s.width, StrokeCap.Round)
}

private fun DrawScope.reading(p: Float, outline: androidx.compose.ui.graphics.Color, accent: androidx.compose.ui.graphics.Color, s: Stroke) {
    val w = size.width
    drawRoundRect(outline, Offset(w * 0.2f, w * 0.1f), Size(w * 0.6f, w * 0.8f), androidx.compose.ui.geometry.CornerRadius(w * 0.08f), style = s)
    for (i in 0..2) drawLine(outline, Offset(w * 0.32f, w * (0.3f + i * 0.14f)), Offset(w * 0.68f, w * (0.3f + i * 0.14f)), s.width, StrokeCap.Round)
    // The ribbon sways from the top edge.
    val sway = sin(p * 2 * PI).toFloat() * w * 0.025f
    val rib = Path().apply {
        moveTo(w * 0.6f, w * 0.1f); lineTo(w * 0.6f + sway, w * 0.34f); lineTo(w * 0.66f + sway, w * 0.28f)
        lineTo(w * 0.72f, w * 0.34f + 0f); lineTo(w * 0.72f, w * 0.1f)
    }
    drawPath(rib, accent, style = s)
}

private fun DrawScope.search(p: Float, outline: androidx.compose.ui.graphics.Color, accent: androidx.compose.ui.graphics.Color, s: Stroke) {
    val w = size.width
    // The magnifier looks around in a small circle.
    val dx = cos(p * 2 * PI).toFloat() * w * 0.05f
    val dy = sin(p * 2 * PI).toFloat() * w * 0.05f
    val c = Offset(w * 0.44f + dx, w * 0.44f + dy)
    drawCircle(accent, w * 0.24f, c, style = s)
    drawLine(outline, Offset(c.x + w * 0.17f, c.y + w * 0.17f), Offset(w * 0.84f + dx, w * 0.84f + dy), s.width * 1.3f, StrokeCap.Round)
}
