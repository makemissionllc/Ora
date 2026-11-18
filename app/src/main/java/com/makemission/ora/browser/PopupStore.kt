package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The sites a profile may open pop-up windows from without asking ("Always allow" on the pop-up card).
 * Kept per profile, by domain only. Private tabs never write here.
 */
class PopupStore private constructor(context: Context, profileId: String) {

    private val prefs = context.applicationContext.getSharedPreferences("ora_popups_$profileId", Context.MODE_PRIVATE)
    private val _allowed = MutableStateFlow(prefs.getStringSet(KEY_ALLOWED, emptySet()).orEmpty().toSet())

    /** Domains allowed to open pop-ups without asking, sorted for the Settings list. */
    val allowed: StateFlow<Set<String>> = _allowed.asStateFlow()

    fun isAllowed(domain: String): Boolean = domain in _allowed.value

    fun allow(domain: String) {
        if (domain in _allowed.value || domain == "unknown") return
        _allowed.value = _allowed.value + domain
        save()
        BrowserLog.info("popup", "Always allow pop-ups from $domain")
    }

    fun remove(domain: String) {
        if (domain !in _allowed.value) return
        _allowed.value = _allowed.value - domain
        save()
        BrowserLog.info("popup", "Stopped allowing pop-ups from $domain")
    }

    private fun save() {
        prefs.edit().putStringSet(KEY_ALLOWED, _allowed.value.toSet()).apply()
    }

    companion object {
        private const val KEY_ALLOWED = "allowed_domains"
        private val stores = mutableMapOf<String, PopupStore>()

        /** One store per profile, shared by the browser and Settings so both see the same list. */
        @Synchronized
        fun forProfile(context: Context, profileId: String): PopupStore =
            stores.getOrPut(profileId) { PopupStore(context, profileId) }
    }
}

/** What to do with a window a page asked to open. */
enum class PopupDecision { OPEN_TAB, ASK }

/**
 * Pop-ups that come from a tap on the page open as a tab. Other pop-ups open as a tab when the site is allowed
 * or when "Block pop-up windows" is off; otherwise the user is asked. Nothing is dropped without a card.
 */
internal fun popupDecision(fromTap: Boolean, siteAllowed: Boolean, blockPopups: Boolean): PopupDecision =
    if (fromTap || siteAllowed || !blockPopups) PopupDecision.OPEN_TAB else PopupDecision.ASK

/** Domain of the page that asked, used as the key for "Always allow" and in logs. */
internal fun popupSiteOf(url: String): String = LogPrivacy.domainOf(url)

/** Taps on the page, so a pop-up that follows a tap can be told apart from one the page starts on its own. */
object PopupTaps {
    /** A pop-up counts as a tap's only if it arrives this soon after the finger lifts. */
    const val TAP_WINDOW_MS = 1500L

    @Volatile
    private var lastTapAt = Long.MIN_VALUE

    fun recordTap(now: Long = System.currentTimeMillis()) {
        lastTapAt = now
    }

    fun tappedRecently(now: Long = System.currentTimeMillis()): Boolean =
        lastTapAt != Long.MIN_VALUE && now - lastTapAt in 0..TAP_WINDOW_MS
}
