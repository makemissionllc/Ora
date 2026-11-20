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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.makemission.ora.extensions.VideoControlBridge
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.delay
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
            // The screen stays awake while a video plays full screen.
            view.keepScreenOn = true
            // The picture runs into the notch area instead of leaving black bars beside it.
            window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
            BrowserLog.info("page", "Video full screen: system bars hidden")
        }
        onDispose {
            if (active && window != null) {
                view.keepScreenOn = false
                window.attributes = window.attributes.also { it.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT }
                WindowCompat.getInsetsController(window, view).show(WindowInsetsCompat.Type.systemBars())
                BrowserLog.info("page", "Video full screen ended: system bars shown")
            }
        }
    }
}

internal data class Readout(val label: String, val percent: Int)

internal const val SKIP_SECONDS = 10
internal const val SKIP_HINT_MS = 700L

internal fun readoutFor(brightness: Boolean, level: Float) =
    Readout(label = if (brightness) "Brightness" else "Volume", percent = (level * 100).roundToInt())

internal fun currentBrightness(activity: Activity?): Float {
    val window = activity?.window ?: return 0.5f
    val own = window.attributes.screenBrightness
    if (own >= 0f) return own
    // No brightness set by the app yet: start from the system's current level.
    val system = Settings.System.getInt(activity.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
    return (system / 255f).coerceIn(0f, 1f)
}

internal fun applyBrightness(activity: Activity?, level: Float) {
    val window = activity?.window ?: return
    // Keep a little light so the screen never goes fully dark while the video plays.
    window.attributes = window.attributes.also { it.screenBrightness = level.coerceAtLeast(0.01f) }
}

internal fun volumeFraction(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

internal fun applyVolume(audio: AudioManager, level: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    try {
        // Flag 0 keeps the system volume panel from popping up over the video.
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (level * max).roundToInt(), 0)
    } catch (e: SecurityException) {
        BrowserLog.error("video", "Could not change the volume: ${e.message}")
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

internal fun formatSeconds(value: Double): String {
    val total = value.roundToInt().coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
