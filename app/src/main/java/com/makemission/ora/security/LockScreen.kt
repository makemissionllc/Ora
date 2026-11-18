package com.makemission.ora.security

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Covers the browser while the app lock is engaged. Fades and scales with the shared overlay motion. */
@Composable
fun LockScreen(settings: SettingsRepository) {
    AnimatedVisibility(
        visible = AppLock.locked,
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(browserColors().GridBackground)
                // Swallow taps so nothing underneath can be reached while locked.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Ora is locked", fontSize = 22.sp, color = browserColors().AddressBarText)
            Spacer(Modifier.height(8.dp))
            Text("Enter your PIN to continue", fontSize = 14.sp, color = browserColors().SecondaryText)
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(12); error = null },
                modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth(),
                singleLine = true,
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                enabled = waitMs <= 0
            )
            Spacer(Modifier.height(12.dp))
            val message = when {
                waitMs > 0 -> "Too many wrong tries. Try again in ${(waitMs + 999) / 1000} s."
                else -> error
            }
            if (message != null) {
                Text(message, fontSize = 13.sp, color = browserColors().Accent)
                Spacer(Modifier.height(12.dp))
            }
            Button(onClick = { submit() }, enabled = pin.isNotEmpty() && waitMs <= 0) { Text("Unlock") }
        }
    }
}
