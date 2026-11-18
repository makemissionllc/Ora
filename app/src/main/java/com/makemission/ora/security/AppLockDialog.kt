package com.makemission.ora.security

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.launch

/** Turn the app lock on (choose and confirm a PIN) or off (confirm the current PIN). */
@Composable
fun AppLockDialog(settings: SettingsRepository, turningOff: Boolean, onDismiss: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val digits: (String) -> String = { it.filter(Char::isDigit).take(12) }
    val keyboard = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (turningOff) "Turn off app lock?" else "Set a PIN") },
        text = {
            Column {
                Text(
                    if (turningOff) "Enter your PIN to turn the lock off."
                    else "Ora asks for this PIN when you open it and whenever you come back to it. Only a salted hash of it is kept on this device.",
                    color = browserColors().SecondaryText
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = digits(it); error = null },
                    singleLine = true,
                    label = { Text(if (turningOff) "Current PIN" else "New PIN (${AppLock.MIN_PIN_LENGTH}+ digits)") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = keyboard
                )
                if (!turningOff) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = digits(it); error = null },
                        singleLine = true,
                        label = { Text("Confirm PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = keyboard
                    )
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = browserColors().Accent)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    if (turningOff) {
                        if (AppLock.verifyOnly(settings, pin)) {
                            AppLock.turnOff(settings)
                            BrowserLog.info("security", "App lock turned off")
                            InAppNotices.show("App lock is off")
                            onDismiss()
                        } else {
                            BrowserLog.warn("security", "Wrong PIN while turning the app lock off")
                            error = "Wrong PIN"
                        }
                    } else if (pin.length < AppLock.MIN_PIN_LENGTH) {
                        error = "Use at least ${AppLock.MIN_PIN_LENGTH} digits"
                    } else if (pin != confirm) {
                        error = "The two PINs don't match"
                    } else {
                        AppLock.setPin(settings, pin)
                        BrowserLog.info("security", "App lock turned on")
                        InAppNotices.show("App lock is on")
                        onDismiss()
                    }
                }
            }) {
                Text(if (turningOff) "Turn off" else "Turn on", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = browserColors().SecondaryText) }
        }
    )
}
