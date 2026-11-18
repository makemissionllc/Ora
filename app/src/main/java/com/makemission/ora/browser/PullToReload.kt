package com.makemission.ora.browser

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import com.makemission.ora.log.UsageStats
import com.makemission.ora.ui.theme.BrowserTokens

/**
 * Pull to refresh for pages. Pulling down while the page is at its top reports how far the finger has
 * moved, and reloads the page when released past [BrowserTokens.PullRefreshThreshold].
 *
 * It only watches the touch in the initial pass and never consumes it, so the page still scrolls normally.
 */
fun Modifier.pullToReload(
    canPull: () -> Boolean,
    onDistance: (Dp) -> Unit,
    onRefresh: () -> Unit,
    /** Called once per pull, the first time the finger reaches the threshold (a haptic tick on the phone and tablet). */
    onThreshold: () -> Unit = {},
): Modifier = pointerInput(canPull) {
    val threshold = BrowserTokens.PullRefreshThreshold.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        // Only a gesture that starts at the top of the page can become a pull.
        val pulling = canPull()
        var distance = 0f
        var downward = 0f
        var reached = false
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                if (pulling && distance >= threshold) {
                    UsageStats.gesture("pull_to_refresh", "registered", null, "pull=${distance.toInt()}")
                    onRefresh()
                } else if (pulling && distance > 24f) {
                    UsageStats.gesture("pull_to_refresh", "ignored", "released_early", "pull=${distance.toInt()} need=${threshold.toInt()}")
                } else if (!pulling && downward > threshold) {
                    // A downward drag that began below the top of the page is a scroll, not a refresh.
                    UsageStats.gesture("pull_to_refresh", "ignored", "not_at_top", "dy=${downward.toInt()}")
                }
                onDistance(Dp.Hairline)
                break
            }
            if (pulling) {
                distance = (distance + change.positionChange().y).coerceAtLeast(0f)
                if (!reached && distance >= threshold) {
                    reached = true
                    onThreshold()
                }
                onDistance(distance.toDp())
            } else {
                downward += change.positionChange().y
            }
        }
    }
}
