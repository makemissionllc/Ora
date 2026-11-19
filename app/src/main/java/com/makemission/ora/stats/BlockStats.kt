package com.makemission.ora.stats

import android.content.Context
import android.content.SharedPreferences
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozilla.geckoview.ContentBlocking

/** Running totals of what Ora has blocked on this device, shown as tiles on the start page. */
data class BlockCounts(val ads: Long = 0L, val trackers: Long = 0L, val fingerprinting: Long = 0L)

/**
 * Counts blocked ads, trackers and fingerprinting attempts for the start page.
 *
 * Only the three totals are stored, never which site or resource was blocked. Private tabs are not
 * counted, so a private session leaves nothing here. Totals stay on the device, and Settings > Home page
 * clears them.
 */
object BlockStats {
    private const val PREFS_NAME = "ora_block_stats"
    private const val KEY_ADS = "ads"
    private const val KEY_TRACKERS = "trackers"
    private const val KEY_FINGERPRINTING = "fingerprinting"

    /** GeckoView anti-tracking categories that count as trackers: analytics, social, content, strict and email tracking. */
    private val TRACKER_CATEGORIES = ContentBlocking.AntiTracking.ANALYTIC or
        ContentBlocking.AntiTracking.SOCIAL or
        ContentBlocking.AntiTracking.CONTENT or
        ContentBlocking.AntiTracking.STP or
        ContentBlocking.AntiTracking.EMAIL

    private var prefs: SharedPreferences? = null

    private val _counts = MutableStateFlow(BlockCounts())
    /** The totals since the counts were last cleared. */
    val counts: StateFlow<BlockCounts> = _counts.asStateFlow()

    /** Loads the saved totals. Call once, when the app starts. */
    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = store
        _counts.value = BlockCounts(
            ads = store.getLong(KEY_ADS, 0L),
            trackers = store.getLong(KEY_TRACKERS, 0L),
            fingerprinting = store.getLong(KEY_FINGERPRINTING, 0L),
        )
    }

    /** Counts one blocked resource, given its GeckoView anti-tracking category bits. */
    fun record(category: Int) {
        val store = prefs ?: return
        val isAd = category and ContentBlocking.AntiTracking.AD != 0
        val isTracker = category and TRACKER_CATEGORIES != 0
        val isFingerprint = category and ContentBlocking.AntiTracking.FINGERPRINTING != 0
        if (!isAd && !isTracker && !isFingerprint) return
        val current = _counts.value
        val next = BlockCounts(
            ads = current.ads + if (isAd) 1L else 0L,
            trackers = current.trackers + if (isTracker) 1L else 0L,
            fingerprinting = current.fingerprinting + if (isFingerprint) 1L else 0L,
        )
        _counts.value = next
        store.edit()
            .putLong(KEY_ADS, next.ads)
            .putLong(KEY_TRACKERS, next.trackers)
            .putLong(KEY_FINGERPRINTING, next.fingerprinting)
            .apply()
    }

    /** Sets all three totals back to zero. Asked for first in Settings. */
    fun reset() {
        prefs?.edit()?.clear()?.apply()
        _counts.value = BlockCounts()
        BrowserLog.info("stats", "Start page block counts reset")
    }
}
