package com.makemission.ora.security

import com.makemission.ora.ui.pressFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.text.font.FontWeight
import com.makemission.ora.ui.theme.Mineral
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Covers the browser while the app lock is engaged. Fades and scales with the shared overlay motion. */
@Composable
fun LockScreen(
    settings: SettingsRepository,
    visible: Boolean,
    title: String = "Ora is locked",
    leaveLabel: String? = null,
    onLeave: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE)
    ) {
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var waitMs by remember { mutableStateOf(AppLock.lockoutRemainingMs(settings)) }
        val scope = rememberCoroutineScope()

        // Count the wait down once a second so the button and message come back by themselves.
        LaunchedEffect(waitMs > 0) {
            while (waitMs > 0) {
                delay(1000)
                waitMs = AppLock.lockoutRemainingMs(settings)
            }
        }

        fun submit() {
            if (pin.isEmpty() || waitMs > 0) return
            val attempt = pin
            scope.launch {
                when (AppLock.tryUnlock(settings, attempt)) {
                    AppLock.Result.OK -> { pin = ""; error = null }
                    AppLock.Result.WRONG -> { pin = ""; error = "Wrong PIN. Try again." }
                    AppLock.Result.LOCKED_OUT -> { pin = ""; error = null }
                }
                waitMs = AppLock.lockoutRemainingMs(settings)
            }
        }

        val colors = browserColors()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.GridBackground)
                // Swallow taps so nothing underneath can be reached while locked.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            OraMark()
            Spacer(Modifier.height(22.dp))
            Text("Enter your PIN", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 15.sp, color = colors.SecondaryText)
            Spacer(Modifier.height(28.dp))
            PinDots(filled = pin.length)
            Spacer(Modifier.height(14.dp))
            val message = when {
                waitMs > 0 -> "Too many wrong tries. Try again in ${(waitMs + 999) / 1000} s."
                else -> error
            }
            // Keeps its height so the keypad does not jump when a message appears.
            Text(message ?: " ", fontSize = 13.sp, color = if (message != null) Mineral.Ember else Color.Transparent)
            if (leaveLabel != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onLeave) {
                    Text(leaveLabel, color = colors.SecondaryText)
                }
            }
            Spacer(Modifier.weight(1f))
            val enabled = waitMs <= 0
            Column(
                modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("123", "456", "789").forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { d ->
                            PinKey(enabled = enabled, onClick = { if (pin.length < 12) { pin += d; error = null } }) {
                                Text(d.toString(), fontSize = 28.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Unlock sits where the design leaves a gap: PINs here can be longer than four digits.
                    PinKey(
                        enabled = enabled && pin.length >= AppLock.MIN_PIN_LENGTH,
                        fill = colors.Accent,
                        onClick = { submit() },
                        description = "Unlock",
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = colors.GridBackground, modifier = Modifier.size(28.dp))
                    }
                    PinKey(enabled = enabled, onClick = { if (pin.length < 12) { pin += "0"; error = null } }) {
                        Text("0", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                    }
                    // Quality of life: a long press on delete clears the whole PIN.
                    PinKey(
                        enabled = pin.isNotEmpty(),
                        fill = Color.Transparent,
                        onClick = { pin = pin.dropLast(1); error = null },
                        onLongClick = { pin = ""; error = null },
                        description = "Delete, hold to clear",
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}

/** The Ora mark: a chalk ring on Basalt, as on the launcher icon. */
@Composable
private fun OraMark() {
    val chalk = Mineral.Chalk
    Canvas(
        modifier = Modifier
            .size(68.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Mineral.Basalt)
            .border(1.dp, Mineral.Seam, RoundedCornerShape(20.dp))
    ) {
        val k = size.width / 108f
        val path = Path().apply {
            fillType = PathFillType.EvenOdd
            addOval(Rect(Offset(54f * k, 56f * k), 32f * k))
            addOval(Rect(Offset(59.5f * k, 49.5f * k), 13f * k))
        }
        drawPath(path, chalk)
    }
}

/** At least four dots: filled for each digit typed, a ring for the digits still needed. */
@Composable
private fun PinDots(filled: Int) {
    val colors = browserColors()
    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(maxOf(filled, AppLock.MIN_PIN_LENGTH)) { i ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .then(
                        if (i < filled) Modifier.background(colors.Accent)
                        else Modifier.border(2.dp, colors.SecondaryText.copy(alpha = 0.45f), CircleShape)
                    )
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinKey(
    enabled: Boolean,
    onClick: () -> Unit,
    fill: Color = browserColors().ToolbarBackground,
    onLongClick: (() -> Unit)? = null,
    description: String? = null,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(76.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(fill)
            .combinedClickable(enabled = enabled, onClick = onClick, onLongClick = onLongClick, onLongClickLabel = description),
        contentAlignment = Alignment.Center
    ) { content() }
}
