package com.makemission.ora.browser

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.launch

/** The settings, for hints. Sheets read it to decide whether to play their one-time cue. */
val LocalHints = androidx.compose.runtime.staticCompositionLocalOf<com.makemission.ora.settings.SettingsRepository?> { null }

/**
 * The one bottom sheet every phone surface uses. It rises on a spring, follows the finger while dragged,
 * pulls up from half height to full height, and settles by flick or distance: down closes it, up expands it.
 * [content] gets a `close` that drops the sheet away with the same motion a swipe-down uses.
 * With [fill] the content gets the sheet's whole height (it scrolls itself); otherwise it wraps and scrolls here.
 */
@Composable
fun OraSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "sheet",
    /** Id of the one-time pull-up cue, or null for none. Plays when the sheet can grow and hints are on. */
    hintId: String? = "sheet-pull-up",
    containerColor: Color = browserColors().Surface,
    fill: Boolean = false,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    val density = LocalDensity.current
    val screenPx = with(density) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val halfPx = screenPx * 0.55f
    val fullPx = screenPx * 0.94f
    val scope = rememberCoroutineScope()
    // 0 = half height, 1 = full height. `drop` is how far the sheet is pushed below its rest (0 = resting, 1 = off screen).
    val expand = remember { Animatable(0f) }
    val drop = remember { Animatable(1f) }
    val colors = browserColors()

    fun close() {
        scope.launch {
            BrowserLog.info(tag, "Sheet closed")
            drop.animateTo(1f, OraMotion.sheetExit())
            onDismiss()
        }
    }
    LaunchedEffect(Unit) {
        BrowserLog.info(tag, "Sheet opened")
        drop.animateTo(0f, OraMotion.sheetSpring())
    }
    BackHandler { close() }

    // One-time nudge: once the sheet has settled, lift it a little and let it fall back, with a caption.
    val hints = LocalHints.current
    val scroll = rememberScrollState()
    var hintText by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    if (hintId != null && hints != null) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(900)
            if (drop.value == 0f && expand.value == 0f && (fill || scroll.maxValue > 0) && hints.shouldShowHint(hintId)) {
                hints.markHintSeen(hintId)
                hintText = true
                expand.animateTo(0.12f, OraMotion.sheetSnap())
                kotlinx.coroutines.delay(350)
                expand.animateTo(0f, OraMotion.sheetSpring())
                kotlinx.coroutines.delay(1600)
                hintText = false
            }
        }
    }

    val travelPx = (fullPx - halfPx).coerceAtLeast(1f)
    val dragState = rememberDraggableState { delta ->
        scope.launch {
            if (delta < 0f && drop.value > 0f) {
                drop.snapTo((drop.value + delta / halfPx).coerceAtLeast(0f))
            } else if (delta < 0f) {
                expand.snapTo((expand.value - delta / travelPx).coerceIn(0f, 1f))
            } else if (expand.value > 0f) {
                expand.snapTo((expand.value - delta / travelPx).coerceIn(0f, 1f))
            } else {
                drop.snapTo((drop.value + delta / halfPx).coerceIn(0f, 1f))
            }
        }
    }

    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnBackPress = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * (1f - drop.value.coerceIn(0f, 1f))))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
            contentAlignment = Alignment.BottomCenter,
        ) {
            val heightPx = halfPx + travelPx * expand.value
            Box(
                modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = drop.value * heightPx }
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(containerColor)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .let { if (fill) it.height(with(density) { heightPx.toDp() }) else it.heightIn(max = with(density) { heightPx.toDp() }) }
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                scope.launch {
                                    when {
                                        velocity > 1400f || (drop.value > 0.25f && velocity > -600f) -> close()
                                        velocity < -900f || expand.value > 0.5f -> {
                                            launch { drop.animateTo(0f, OraMotion.sheetSnap()) }
                                            expand.animateTo(1f, OraMotion.sheetSnap())
                                        }
                                        else -> {
                                            launch { drop.animateTo(0f, OraMotion.sheetSnap()) }
                                            expand.animateTo(0f, OraMotion.sheetSnap())
                                        }
                                    }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(colors.SecondaryText.copy(alpha = 0.4f)))
                }
                Box(Modifier.padding(top = 28.dp).let { if (fill) it.fillMaxSize() else it.verticalScroll(scroll) }) {
                    content { close() }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
            visible = hintText,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = with(density) { heightPx.toDp() } + 10.dp),
            enter = androidx.compose.animation.fadeIn(OraMotion.overlayFadeIn()) + androidx.compose.animation.scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE),
            exit = androidx.compose.animation.fadeOut(OraMotion.overlayFadeOut()) + androidx.compose.animation.scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE),
        ) {
            androidx.compose.material3.Text(
                "Pull up for more",
                fontSize = 13.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = colors.AddressBarText,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.AddressBarBackground).padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
        }
    }
}
