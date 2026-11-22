package com.makemission.ora.security

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import com.makemission.ora.log.BrowserLog

/** The system fingerprint or face check, used as a faster way to open the PIN lock. */
object BiometricUnlock {
    /** True when the phone has a strong biometric enrolled. */
    fun available(context: Context): Boolean = runCatching {
        context.getSystemService(BiometricManager::class.java)
            ?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }.getOrDefault(false)

    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }

    /** Shows the system prompt. [onSuccess] runs on a pass; a cancel or the Use PIN button just returns to the keypad. */
    fun prompt(context: Context, title: String, onSuccess: () -> Unit) {
        val host = context.activity() ?: return
        runCatching {
            val prompt = BiometricPrompt.Builder(host)
                .setTitle(title)
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .setNegativeButton("Use PIN", host.mainExecutor) { _, _ -> BrowserLog.info("security", "Fingerprint prompt: chose PIN") }
                .build()
            prompt.authenticate(CancellationSignal(), host.mainExecutor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onSuccess()
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    BrowserLog.warn("security", "Fingerprint prompt ended: $errString ($errorCode)")
                }
            })
        }.onFailure { BrowserLog.error("security", "Could not show the fingerprint prompt", it) }
    }
}
