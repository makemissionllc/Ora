package com.makemission.ora.browser

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import com.makemission.ora.ui.theme.BrowserTokens
import kotlin.math.abs

/**
 * A thin strip along the left edge of the tablet page. A rightward drag that starts here and
 * runs mostly sideways opens the tab sidebar. Only the strip takes the touch, so the rest of the
 * page scrolls and taps as usual. It is not composed while the sidebar is open or the setting is off.
 */
@Composable
fun SidebarEdgeSwipe(onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val distancePx = with(density) { BrowserTokens.EdgeSwipeDistance.toPx() }
    val currentOnOpen by rememberUpdatedState(onOpen)
    Box(
        modifier = modifier
            .padding(start = BrowserTokens.EdgeSwipeInset)
            .width(BrowserTokens.EdgeSwipeWidth)
            .fillMaxHeight()
            .pointerInput(distancePx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var across = 0f
                    var along = 0f
                    var opened = false
                    while (!opened) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val delta = change.positionChange()
                        across += delta.x
                        along += delta.y
                        // Only a mostly sideways drive to the right counts; a scroll or a tap does not.
                        if (across > distancePx && abs(across) > abs(along) * 1.5f) {
                            opened = true
                            currentOnOpen()
                        }
                    }
                }
            }
    )
}
