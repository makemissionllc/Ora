package com.makemission.ora.downloads

import com.makemission.ora.ui.pressFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** How long the start notice stays on screen before it fades out by itself. */
private const val NOTICE_HOLD_MS = 2600L

/**
 * The small pill that says a download has begun, as Chrome does. It fades out after a few seconds,
 * can be swiped away sideways, and opens the Downloads page when tapped. A new download replaces it.
 * It sits at the bottom edge so it stays clear of the address bar.
 */
@Composable
fun DownloadStartedNotice(
    repository: DownloadsRepository,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    val scope = rememberCoroutineScope()
    var shown by remember { mutableStateOf<DownloadItem?>(null) }
    val visibleState = remember { MutableTransitionState(false) }
    val slide = remember { Animatable(0f) }
    var widthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(repository) {
        repository.started.collect { item ->
            slide.snapTo(0f)
            shown = item
        }
    }
    // Each new download restarts the hold, then the pill fades out on its own.
    LaunchedEffect(shown?.id) {
        if (shown == null) return@LaunchedEffect
        visibleState.targetState = true
        delay(NOTICE_HOLD_MS)
        visibleState.targetState = false
    }

    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) +
            scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) +
            scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)),
    ) {
        shown?.let { item ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = BrowserTokens.DownloadNoticeMaxWidth)
                    .offset { IntOffset(slide.value.roundToInt(), 0) }
                    .onSizeChanged { widthPx = it.width.toFloat() }
                    .pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusLarge))
                    .background(colors.Surface)
                    .clickable {
                        BrowserLog.info("download", "Opened Downloads from the start notice")
                        visibleState.targetState = false
                        onOpen()
                    }
                    .pointerInput(item.id) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    if (abs(slide.value) > widthPx * 0.3f) {
                                        BrowserLog.info("download", "Swiped the start notice away")
                                        visibleState.targetState = false
                                        slide.animateTo(if (slide.value < 0f) -widthPx else widthPx, OraMotion.exit())
                                    } else {
                                        slide.animateTo(0f, OraMotion.enter())
                                    }
                                }
                            },
                            onDragCancel = { scope.launch { slide.animateTo(0f, OraMotion.enter()) } },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                scope.launch { slide.snapTo(slide.value + amount) }
                            },
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = colors.Accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text("Download started", fontSize = 12.sp, color = colors.SecondaryText, maxLines = 1)
                    Text(
                        item.fileName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.AddressBarText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
