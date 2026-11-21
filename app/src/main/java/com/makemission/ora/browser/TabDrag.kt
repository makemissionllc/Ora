package com.makemission.ora.browser

import androidx.compose.ui.composed
import android.content.ClipData
import android.content.ClipDescription
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

/** Marks a drag as one of Ora's own tabs, so other apps' drags are ignored. */
private const val TAB_DRAG_LABEL = "ora-tab"

/**
 * Lets a tab be picked up with a long press and moved onto a group. A long press that is released
 * without moving still calls [onLongPress], so "Add to group" keeps working. Drags stay inside Ora.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tabDragSource(tabId: Long, accent: Color, onLongPress: (() -> Unit)?): Modifier = composed {
    val haptics = com.makemission.ora.ui.LocalOraHaptics.current
    dragAndDropSource(
        drawDragDecoration = {
            drawRoundRect(accent.copy(alpha = 0.85f), size = this.size, cornerRadius = CornerRadius(12.dp.toPx()))
        },
        block = {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            // Wait for the hold: a lift or a move past the slop before the timeout is a tap or a scroll.
            val held = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                var travelledEarly = 0f
                while (true) {
                    val change = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull false
                    if (!change.pressed) return@withTimeoutOrNull false
                    travelledEarly += change.positionChange().getDistance()
                    if (travelledEarly > viewConfiguration.touchSlop) return@withTimeoutOrNull false
                }
                @Suppress("UNREACHABLE_CODE") true
            }
            if (held != null) return@awaitEachGesture
            // Held long enough: the card is lifted.
            haptics.play(com.makemission.ora.ui.Feel.LONG_PRESS)
            val press = down
            // Held in place and let go: the old long-press action. Moving after the hold: a drag.
            val slop = viewConfiguration.touchSlop
            var travelled = 0f
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Main).changes.firstOrNull { it.id == press.id } ?: return@awaitEachGesture
                if (!change.pressed) { onLongPress?.invoke(); return@awaitEachGesture }
                travelled += change.positionChange().getDistance()
                if (travelled > slop) {
                    startTransfer(DragAndDropTransferData(ClipData.newPlainText(TAB_DRAG_LABEL, tabId.toString())))
                    return@awaitEachGesture
                }
            }
        }
        },
    )
}

/** Accepts a tab dropped on it. [onHover] says whether a tab is over it now, for highlighting. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tabDropTarget(onHover: (Boolean) -> Unit, onDrop: (Long) -> Unit): Modifier = composed {
    val haptics = com.makemission.ora.ui.LocalOraHaptics.current
    dragAndDropTarget(
        shouldStartDragAndDrop = { it.toAndroidDragEvent().clipDescription?.label == TAB_DRAG_LABEL },
        target = object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) { haptics.play(com.makemission.ora.ui.Feel.THRESHOLD); onHover(true) }
            override fun onExited(event: DragAndDropEvent) = onHover(false)
            override fun onEnded(event: DragAndDropEvent) = onHover(false)
            override fun onDrop(event: DragAndDropEvent): Boolean {
                onHover(false)
                val id = event.toAndroidDragEvent().clipData?.getItemAt(0)?.text?.toString()?.toLongOrNull() ?: return false
                haptics.play(com.makemission.ora.ui.Feel.CONFIRM)
                onDrop(id)
                return true
            }
        },
    )
}
