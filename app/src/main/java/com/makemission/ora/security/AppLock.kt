package com.makemission.ora.security

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** PIN lock for the whole app. Locks on start and whenever Ora leaves the screen. */
object AppLock {
    const val MIN_PIN_LENGTH = 4
    private const val FREE_ATTEMPTS = 5
    private const val BASE_LOCKOUT_MS = 30_000L
    private const val MAX_LOCKOUT_MS = 15 * 60_000L

    enum class Result { OK, WRONG, LOCKED_OUT }

    /** True while the lock screen should cover the browser. */
    var locked by mutableStateOf(false)
        private set

    /** Starts locked when a PIN is set. Call once from onCreate. */
    fun init(settings: SettingsRepository) {
        locked = settings.appLockRecord.value != null
    }

    fun lockIfEnabled(settings: SettingsRepository) {
        if (settings.appLockRecord.value != null && !locked) {
            locked = true
            BrowserLog.info("security", "App locked")
        }
    }

    fun lockoutRemainingMs(settings: SettingsRepository): Long =
        (settings.appLockLockedUntil() - System.currentTimeMillis()).coerceAtLeast(0)

    /** Checks [pin]. After five wrong tries in a row, further tries wait 30 s, doubling each time up to 15 min. */
    suspend fun tryUnlock(settings: SettingsRepository, pin: String): Result {
        val record = settings.appLockRecord.value ?: run { locked = false; return Result.OK }
        if (lockoutRemainingMs(settings) > 0) return Result.LOCKED_OUT
        val ok = withContext(Dispatchers.Default) { PinHasher.verify(pin, record) }
        if (ok) {
            settings.setAppLockFailures(0, 0)
            locked = false
            UsageStats.count("security.unlock")
            BrowserLog.info("security", "App unlocked")
            return Result.OK
        }
        val fails = settings.appLockFailures() + 1
        var until = 0L
        if (fails >= FREE_ATTEMPTS) {
            val steps = (fails - FREE_ATTEMPTS).coerceAtMost(5)
            until = System.currentTimeMillis() + (BASE_LOCKOUT_MS shl steps).coerceAtMost(MAX_LOCKOUT_MS)
        }
        settings.setAppLockFailures(fails, until)
        BrowserLog.warn("security", "Wrong PIN ($fails in a row)${if (until > 0) ", waiting before the next try" else ""}")
        return if (until > 0) Result.LOCKED_OUT else Result.WRONG
    }

    suspend fun setPin(settings: SettingsRepository, pin: String) {
        val record = withContext(Dispatchers.Default) { PinHasher.create(pin) }
        settings.setAppLockRecord(record)
        settings.setAppLockFailures(0, 0)
    }

    suspend fun verifyOnly(settings: SettingsRepository, pin: String): Boolean {
        val record = settings.appLockRecord.value ?: return true
        return withContext(Dispatchers.Default) { PinHasher.verify(pin, record) }
    }

    fun turnOff(settings: SettingsRepository) {
        settings.setAppLockRecord(null)
        locked = false
    }
}
