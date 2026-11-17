package com.makemission.ora.browser

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlin.math.roundToInt

/**
 * Hides the status and navigation bars while a page's video is full screen, and shows them again afterwards.
 * Only the video full-screen state changes the bars; the status bar's own colour and fade stay as they are.
 */
@Composable
fun VideoFullScreenEffect(active: Boolean) {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity()
    DisposableEffect(active, activity) {
        val window = activity?.window
        if (active && window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            BrowserLog.info("page", "Video full screen: system bars hidden")
        }
        onDispose {
            if (active && window != null) {
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
                BrowserLog.info("page", "Video full screen ended: system bars shown")
            }
        }
    }
}

/**
 * Brightness and volume swipes for a full-screen video. Swiping up or down on the left half changes the screen
 * brightness, and on the right half the media volume. A readout shows the level while the finger is down.
 */
@Composable
fun VideoGestureLayer(modifier: Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var readout by remember { mutableStateOf<Readout?>(null) }
    // The last readout stays in place while it fades out, so the text does not blank during the exit.
    var lastShown by remember { mutableStateOf<Readout?>(null) }
    LaunchedEffect(readout) { readout?.let { lastShown = it } }

    DisposableEffect(activity) {
        onDispose {
            // Give the screen back its own brightness when the video leaves full screen.
            activity?.window?.let { window ->
                window.attributes = window.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
            }
        }
    }

    Box(modifier = modifier) {
        Row(modifier = Modifier.fillMaxSize()) {
            GestureHalf(
                modifier = Modifier.weight(1f),
                brightness = true,
                activity = activity,
                audio = audio,
                onReadout = { readout = it },
            )
            GestureHalf(
                modifier = Modifier.weight(1f),
                brightness = false,
                activity = activity,
                audio = audio,
                onReadout = { readout = it },
            )
        }
        AnimatedVisibility(
            visible = readout != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(OraMotion.enter()),
            exit = fadeOut(OraMotion.exit()),
        ) {
            lastShown?.let { ReadoutPill(it) }
        }
    }
}

@Composable
private fun GestureHalf(
    modifier: Modifier,
    brightness: Boolean,
    activity: Activity?,
    audio: AudioManager,
    onReadout: (Readout?) -> Unit,
) {
    var height by remember { mutableFloatStateOf(1f) }
    var level by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(brightness) {
                detectVerticalDragGestures(
                    onDragStart = {
                        level = if (brightness) currentBrightness(activity) else volumeFraction(audio)
                        onReadout(readoutFor(brightness, level))
                    },
                    onDragEnd = { onReadout(null) },
                    onDragCancel = { onReadout(null) },
                ) { change, dragAmount ->
                    change.consume()
                    // Swiping up (negative drag) raises the level; a full swipe across the half covers the whole range.
                    level = (level - dragAmount / height).coerceIn(0f, 1f)
                    if (brightness) applyBrightness(activity, level) else applyVolume(audio, level)
                    onReadout(readoutFor(brightness, level))
                }
            }
    )
}

@Composable
private fun ReadoutPill(readout: Readout) {
    Text(
        text = "${readout.label} ${readout.percent}%",
        fontSize = 16.sp,
        color = browserColors().AddressBarText,
        modifier = Modifier
            .background(browserColors().Surface, RoundedCornerShape(BrowserTokens.RadiusMedium))
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

private data class Readout(val label: String, val percent: Int)

private fun readoutFor(brightness: Boolean, level: Float) =
    Readout(label = if (brightness) "Brightness" else "Volume", percent = (level * 100).roundToInt())

private fun currentBrightness(activity: Activity?): Float {
    val window = activity?.window ?: return 0.5f
    val own = window.attributes.screenBrightness
    if (own >= 0f) return own
    // No brightness set by the app yet: start from the system's current level.
    val system = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
    return (system / 255f).coerceIn(0f, 1f)
}

private fun applyBrightness(activity: Activity?, level: Float) {
    val window = activity?.window ?: return
    // Keep a little light so the screen never goes fully dark while the video plays.
    window.attributes = window.attributes.also { it.screenBrightness = level.coerceAtLeast(0.01f) }
}

private fun volumeFraction(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

private fun applyVolume(audio: AudioManager, level: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    try {
        // Flag 0 keeps the system volume panel from popping up over the video.
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).roundToInt(), 0)
    } catch (e: SecurityException) {
        BrowserLog.error("video", "Could not change the volume: ${e.message}")
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
