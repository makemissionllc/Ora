package com.makemission.ora.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween

/** The motion character the user picks in Settings > Appearance. Each one sets the timing and easing of every transition. */
enum class MotionStyle(val key: String, val label: String, val description: String) {
    SMOOTH("smooth", "Smooth", "The default. Quick and even, with a soft landing."),
    ELEGANT("elegant", "Elegant", "Slower, with a long, gentle settle that feels unhurried."),
    FAST("fast", "Fast", "Short and direct, for people who want the page to respond at once."),
    PREMIUM("premium", "Premium", "A confident start and a long, precise finish.");

    companion object {
        fun fromKey(key: String?): MotionStyle = values().firstOrNull { it.key == key } ?: SMOOTH
    }
}

/**
 * Shared motion values. Every transition in the app uses these so the browser moves as one piece.
 * Timing and easing follow the selected [MotionStyle]. Set it with [OraMotion.style] when the setting changes.
 */
object OraMotion {
    /** Current style. Written by SettingsRepository when the user picks one, and read by every transition. */
    @Volatile
    var style: MotionStyle = MotionStyle.SMOOTH

    private val elegantEnter: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val premiumEnter: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    private val premiumExit: Easing = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)

    val ENTER_MS: Int
        get() = when (style) {
            MotionStyle.SMOOTH -> 260
            MotionStyle.ELEGANT -> 380
            MotionStyle.FAST -> 160
            MotionStyle.PREMIUM -> 340
        }

    val EXIT_MS: Int
        get() = when (style) {
            MotionStyle.SMOOTH -> 200
            MotionStyle.ELEGANT -> 280
            MotionStyle.FAST -> 110
            MotionStyle.PREMIUM -> 220
        }

    /** Distance a page slides in from the right edge, as a fraction of its width. */
    const val SLIDE_EXIT_FRACTION = 0.25f

    /** Scale an overlay starts from (or shrinks back to) when it grows out of the tabs button. */
    const val MORPH_SCALE = 0.88f

    /** Wait before the load bar fades in, so a page that finishes quickly never flashes it. */
    const val LOAD_BAR_DELAY_MS = 120

    private fun enterEasing(): Easing = when (style) {
        MotionStyle.SMOOTH -> FastOutSlowInEasing
        MotionStyle.ELEGANT -> elegantEnter
        MotionStyle.FAST -> LinearOutSlowInEasing
        MotionStyle.PREMIUM -> premiumEnter
    }

    private fun exitEasing(): Easing = when (style) {
        MotionStyle.SMOOTH, MotionStyle.FAST -> FastOutLinearInEasing
        MotionStyle.ELEGANT -> FastOutLinearInEasing
        MotionStyle.PREMIUM -> premiumExit
    }

    fun <T> enter(): FiniteAnimationSpec<T> = tween(durationMillis = ENTER_MS, easing = enterEasing())
    fun <T> enterAfterLoadDelay(): FiniteAnimationSpec<T> =
        tween(durationMillis = ENTER_MS, delayMillis = LOAD_BAR_DELAY_MS, easing = enterEasing())
    fun <T> exit(): FiniteAnimationSpec<T> = tween(durationMillis = EXIT_MS, easing = exitEasing())
}
