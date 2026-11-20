package com.makemission.ora.browser

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Rational
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.Forward10
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Replay10
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Consumer
import androidx.core.app.PictureInPictureModeChangedInfo
import com.makemission.ora.extensions.VideoControlBridge
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.delay

private val SPEEDS = listOf(0.5, 0.75, 1.0, 1.25, 1.5, 2.0)
private const val HIDE_AFTER_MS = 3_500L
private const val HOLD_SPEED = 2.0

/**
 * The full-screen video player's controls, drawn over the page's own. They work like the Samsung Internet player:
 * tap the picture to show or hide the controls, double-tap a side to skip 10 seconds, hold to play at 2x while held,
 * and swipe the left side for brightness and the right side for volume. The controls are a back button and the
 * page's title on top, skip and play in the middle, and the seek bar with lock, speed, fit, mute, rotate and
 * picture-in-picture along the bottom. They hide by themselves while the video plays, and stay up when it is paused
 * or when a screen reader is on.
 */
@Composable
fun VideoPlayerControls(autoPip: Boolean, tablet: Boolean, swipes: Boolean, onExit: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audio = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager }
    val colors = browserColors()
    val state by VideoControlBridge.state.collectAsState()
    var last by remember { mutableStateOf<VideoControlBridge.State?>(null) }
    LaunchedEffect(state) { state?.takeIf { it.hasVideo }?.let { last = it } }
    val video = state?.takeIf { it.hasVideo } ?: last

    var shown by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var cover by remember { mutableStateOf(false) }
    var inPip by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf<Float?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    var hintShown by remember { mutableStateOf(false) }
    var hintTick by remember { mutableIntStateOf(0) }
    var skipTotal by remember { mutableIntStateOf(0) }
    val screenReader = remember(context) { (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager).isTouchExplorationEnabled }
    val show = { shown = true; touches++ }
    val say: (String) -> Unit = { hint = it; hintShown = true; hintTick++ }

    // Controls fold away after a few seconds of play. Paused, ended or with a screen reader on, they stay.
    LaunchedEffect(shown, touches, video?.paused, dragging, locked) {
        if (shown && video?.paused == false && dragging == null && !screenReader) {
            delay(HIDE_AFTER_MS)
            shown = false
        }
    }
    LaunchedEffect(hintTick) {
        if (hintTick > 0) { delay(800); hintShown = false; skipTotal = 0 }
    }
    // A phone turns to landscape for a wide video, and back afterwards. A tablet is left as it is.
    val wide = video != null && video.width > video.height
    DisposableEffect(activity, wide) {
        if (!tablet && wide) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { if (!tablet && wide) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    // Going home while the video plays turns it into a small window instead of stopping it.
    val playing = video?.paused == false
    val pipWide = video?.width?.takeIf { it > 0 } ?: 16
    val pipHigh = video?.height?.takeIf { it > 0 } ?: 9
    DisposableEffect(activity, autoPip, playing, pipWide, pipHigh) {
        if (autoPip && playing) {
            val r = Rational(pipWide, pipHigh).let { r -> if (r.toFloat() > 2.39f) Rational(239, 100) else if (r.toFloat() < 0.42f) Rational(42, 100) else r }
            activity?.setPictureInPictureParams(PictureInPictureParams.Builder().setAspectRatio(r).setAutoEnterEnabled(true).build())
        }
        onDispose {
            if (autoPip && playing) activity?.setPictureInPictureParams(PictureInPictureParams.Builder().setAutoEnterEnabled(false).build())
        }
    }
    DisposableEffect(activity) {
        val listener = Consumer<PictureInPictureModeChangedInfo> { inPip = it.isInPictureInPictureMode }
        (activity as? ComponentActivity)?.addOnPictureInPictureModeChangedListener(listener)
        onDispose {
            (activity as? ComponentActivity)?.removeOnPictureInPictureModeChangedListener(listener)
            // Give the screen its own brightness and the page its normal picture back.
            activity?.window?.let { w -> w.attributes = w.attributes.also { it.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE } }
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    if (inPip) return

    fun skip(seconds: Int) {
        VideoControlBridge.skip(seconds.toDouble())
        skipTotal += seconds
        say(if (skipTotal < 0) "$skipTotal s" else "+$skipTotal s")
    }
    fun togglePlay() {
        if (video?.paused == true || video?.ended == true) VideoControlBridge.play() else VideoControlBridge.pause()
        show()
    }

    var height by remember { mutableFloatStateOf(1f) }
    var width by remember { mutableFloatStateOf(1f) }
    var level by remember { mutableFloatStateOf(0f) }
    var rightSide by remember { mutableStateOf(false) }
    // The touch layer is the parent of every control, so a button gets its own tap first and the picture only gets the rest.
    Box(
        modifier.fillMaxSize()
                .onSizeChanged { height = it.height.toFloat().coerceAtLeast(1f); width = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(locked) {
                    detectTapGestures(
                        onTap = { if (shown) shown = false else show() },
                        onDoubleTap = { p ->
                            if (!locked) { if (p.x < width / 2) skip(-10) else skip(10) }
                        },
                        onPress = {
                            tryAwaitRelease()
                            if (holding) { holding = false; VideoControlBridge.setRate(video?.rate?.takeIf { it > 0 } ?: 1.0) }
                        },
                        onLongPress = {
                            if (!locked && video?.paused == false) {
                                holding = true; VideoControlBridge.setRate(HOLD_SPEED); say("2× speed")
                            }
                        },
                    )
                }
                .pointerInput(swipes, locked) {
                    if (!swipes || locked) return@pointerInput
                    detectVerticalDragGestures(
                        onDragStart = { p ->
                            rightSide = p.x >= width / 2
                            level = if (rightSide) volumeFraction(audio) else currentBrightness(activity)
                            say("${if (rightSide) "Volume" else "Brightness"} ${(level * 100).toInt()}%")
                        },
                        onDragEnd = {}, onDragCancel = {},
                    ) { change, drag ->
                        change.consume()
                        level = (level - drag / height).coerceIn(0f, 1f)
                        if (rightSide) applyVolume(audio, level) else applyBrightness(activity, level)
                        say("${if (rightSide) "Volume" else "Brightness"} ${(level * 100).toInt()}%")
                    }
                }
        
    ) {

        // Top: back, and the page's title.
        AnimatedVisibility(
            visible = shown && !locked, modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit()),
        ) {
            Row(
                Modifier.fillMaxWidth().background(colors.Surface.copy(alpha = 0.92f)).statusBarsPadding().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlayerButton(Icons.AutoMirrored.Outlined.ArrowBack, "Exit full screen") { BrowserLog.info("video", "Exit button tapped"); onExit() }
                Text(
                    video?.title.orEmpty().ifBlank { "Video" }, Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText,
                )
                PlayerButton(Icons.Outlined.ScreenRotation, "Rotate the screen") {
                    val landscape = (activity?.resources?.configuration?.orientation ?: 0) == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                    activity?.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    show()
                }
                PlayerButton(Icons.Outlined.PictureInPictureAlt, "Picture in picture") {
                    try {
                        val w = video?.width?.takeIf { it > 0 } ?: 16
                        val h = video?.height?.takeIf { it > 0 } ?: 9
                        val ratio = Rational(w, h).let { r -> if (r.toFloat() > 2.39f) Rational(239, 100) else if (r.toFloat() < 0.42f) Rational(42, 100) else r }
                        activity?.enterPictureInPictureMode(PictureInPictureParams.Builder().setAspectRatio(ratio).build())
                        BrowserLog.info("video", "Entered picture in picture")
                    } catch (e: Exception) {
                        BrowserLog.error("video", "Could not start picture in picture", e)
                        say("Picture in picture is not available")
                    }
                }
            }
        }

        // Middle: skip back, play or pause, skip forward.
        AnimatedVisibility(
            visible = shown && !locked, modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit()),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                PlayerButton(Icons.Outlined.Replay10, "Back 10 seconds", big = true) { skip(-10); show() }
                val ended = video?.ended == true
                val paused = video?.paused != false
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(colors.Accent).clickable { togglePlay() }
                        .semantics { contentDescription = if (ended) "Replay" else if (paused) "Play" else "Pause" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (ended) Icons.Outlined.Replay else if (paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, null, tint = colors.GridBackground, modifier = Modifier.size(40.dp))
                }
                PlayerButton(Icons.Outlined.Forward10, "Forward 10 seconds", big = true) { skip(10); show() }
            }
        }

        // Bottom: seek bar, then lock, speed, fit, mute.
        AnimatedVisibility(
            visible = shown && !locked && video != null, modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit()),
        ) {
            val v = video ?: return@AnimatedVisibility
            val duration = v.duration.coerceAtLeast(0.0)
            val fraction = if (duration > 0.0) (v.seconds / duration).toFloat().coerceIn(0f, 1f) else 0f
            val position = dragging?.let { it * duration } ?: v.seconds
            Column(Modifier.fillMaxWidth().background(colors.Surface.copy(alpha = 0.92f)).padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatSeconds(position), fontSize = 13.sp, color = colors.AddressBarText, modifier = Modifier.width(52.dp))
                    Slider(
                        value = dragging ?: fraction,
                        onValueChange = { dragging = it; touches++ },
                        onValueChangeFinished = { dragging?.let { VideoControlBridge.seek(it * duration) }; dragging = null },
                        valueRange = 0f..1f,
                        enabled = duration > 0.0,
                        modifier = Modifier.weight(1f).semantics {
                            contentDescription = "Seek bar"
                            stateDescription = "${formatSeconds(position)} of ${formatSeconds(duration)}"
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.AddressBarText, activeTrackColor = colors.AddressBarText,
                            inactiveTrackColor = colors.AddressBarText.copy(alpha = 0.3f),
                        ),
                    )
                    Text(formatSeconds(duration), fontSize = 13.sp, color = colors.AddressBarText, modifier = Modifier.width(52.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlayerButton(Icons.Outlined.Lock, "Lock the controls") { locked = true; shown = true; say("Controls locked") }
                    val rate = v.rate
                    Box(
                        Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(colors.AddressBarBackground)
                            .clickable {
                                val next = SPEEDS.firstOrNull { it > rate + 0.01 } ?: SPEEDS.first()
                                VideoControlBridge.setRate(next); say("Speed ${speedLabel(next)}"); touches++
                            }
                            .padding(horizontal = 18.dp)
                            .semantics { contentDescription = "Playback speed ${speedLabel(rate)}. Tap to change." },
                        contentAlignment = Alignment.Center,
                    ) { Text(speedLabel(rate), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText) }
                    PlayerButton(Icons.Outlined.AspectRatio, if (cover) "Show the whole picture" else "Fill the screen") {
                        cover = !cover; VideoControlBridge.setFit(cover); say(if (cover) "Fill the screen" else "Whole picture"); touches++
                    }
                    PlayerButton(if (v.muted) Icons.Outlined.VolumeOff else Icons.Outlined.VolumeUp, if (v.muted) "Turn sound on" else "Mute") {
                        VideoControlBridge.setMuted(!v.muted); touches++
                    }
                }
            }
        }

        // Locked: only the unlock button, after a tap.
        AnimatedVisibility(
            visible = locked && shown, modifier = Modifier.align(Alignment.CenterStart).padding(start = 24.dp),
            enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit()),
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(colors.Surface.copy(alpha = 0.92f))
                    .clickable { locked = false; show(); say("Controls unlocked") }
                    .semantics { contentDescription = "Unlock the controls" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.LockOpen, null, tint = colors.AddressBarText) }
        }

        // A short note for skips, speed, volume and brightness.
        AnimatedVisibility(
            visible = hintShown, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
            enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit()),
        ) {
            hint?.let {
                Text(
                    it, fontSize = 16.sp, color = colors.AddressBarText,
                    modifier = Modifier.background(colors.Surface, RoundedCornerShape(BrowserTokens.RadiusMedium)).padding(horizontal = 16.dp, vertical = 10.dp)
                        .semantics { contentDescription = it },
                )
            }
        }
    }
}

private fun speedLabel(rate: Double): String = if (rate == rate.toInt().toDouble()) "${rate.toInt()}×" else "${"%.2f".format(rate).trimEnd('0')}×"

/** A 48 dp round icon button with a spoken name. [big] is for the skip buttons beside play. */
@Composable
private fun PlayerButton(icon: ImageVector, label: String, big: Boolean = false, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        Modifier.size(if (big) 56.dp else 48.dp).clip(CircleShape).clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = colors.AddressBarText, modifier = Modifier.size(if (big) 32.dp else 24.dp)) }
}
