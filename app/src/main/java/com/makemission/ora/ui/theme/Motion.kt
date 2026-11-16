package com.makemission.ora.ui.theme

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween

/**
 * Shared motion values. Every transition in the app uses these so the browser moves as one piece.
 * Enter uses fast-out-slow-in (settles softly), exit uses fast-out-linear-in (leaves quickly).
 */
object OraMotion {
    const val ENTER_MS = 260
    const val EXIT_MS = 200

    /** Distance a page slides in from the right edge, as a fraction of its width. */
    const val SLIDE_EXIT_FRACTION = 0.25f

    /** Scale an overlay starts from (or shrinks back to) when it grows out of the tabs button. */
    const val MORPH_SCALE = 0.88f

    /** Wait before the load bar fades in, so a page that finishes quickly never flashes it. */
    const val LOAD_BAR_DELAY_MS = 120

    fun <T> enter(): FiniteAnimationSpec<T> = tween(durationMillis = ENTER_MS, easing = FastOutSlowInEasing)
    fun <T> enterAfterLoadDelay(): FiniteAnimationSpec<T> =
        tween(durationMillis = ENTER_MS, delayMillis = LOAD_BAR_DELAY_MS, easing = FastOutSlowInEasing)
    fun <T> exit(): FiniteAnimationSpec<T> = tween(durationMillis = EXIT_MS, easing = FastOutLinearInEasing)
}
