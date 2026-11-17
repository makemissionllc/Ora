package com.makemission.ora.browser

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion

/**
 * Samples the page's top row into one colour for the status bar. Takes evenly spaced pixels across the row
 * and averages them, so a striped or patterned top edge still gives one stable colour. Null if the bitmap
 * cannot be read; the failure is logged.
 */
fun sampleTopRow(bitmap: Bitmap): Color? {
    if (bitmap.width <= 0 || bitmap.height <= 0) return null
    return try {
        val samples = 16
        var red = 0
        var green = 0
        var blue = 0
        for (i in 0 until samples) {
            val x = ((i + 0.5f) * bitmap.width / samples).toInt().coerceIn(0, bitmap.width - 1)
            val pixel = bitmap.getPixel(x, 0)
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
        }
        Color(red / samples, green / samples, blue / samples)
    } catch (e: IllegalStateException) {
        BrowserLog.warn("page", "Could not read the page's top colour", e)
        null
    }
}

/**
 * The status bar's icons read dark on a light strip and light on a dark one. Pass the strip colour; the result
 * is what the system's status-bar appearance should be (true means dark icons).
 */
fun isLightStatusStrip(color: Color): Boolean = color.luminance() > 0.5f

/**
 * Paints the strip behind the status bar. At the top of a page it is one solid colour, the page's own top colour,
 * so the page reads as starting below the bar. Once the page scrolls under the bar, the solid strip fades out and
 * a soft gradient of the same colour takes its place, instead of a hard edge. Both change with the shared motion.
 */
@Composable
fun StatusBarOverlay(
    color: Color,
    scrolled: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val statusTop = WindowInsets.statusBars.asPaddingValues(density).calculateTopPadding()
    val solidAlpha by animateFloatAsState(
        targetValue = if (scrolled) 0f else 1f,
        animationSpec = if (scrolled) OraMotion.exit() else OraMotion.enter(),
        label = "status solid",
    )
    Box(modifier = modifier.fillMaxWidth().height(BrowserTokens.StatusScrollFade)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusTop)
                .graphicsLayer { alpha = solidAlpha }
                .background(color)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.StatusScrollFade)
                .graphicsLayer { alpha = 1f - solidAlpha }
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to color.copy(alpha = 0.96f),
                            0.45f to color.copy(alpha = 0.85f),
                            1f to Color.Transparent,
                        )
                    )
                )
        )
    }
}
