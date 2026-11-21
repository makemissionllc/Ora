package com.makemission.ora.devices

import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** A page another device was showing recently, offered here as "Continue from <device>". */
data class ContinueOffer(val deviceId: String, val deviceName: String, val url: String, val title: String, val ageMs: Long)

/** What a device answers when asked "what are you showing?". [ageMs] is how long ago the page was last the selected tab. */
data class WhereReply(val deviceId: String, val deviceName: String, val url: String, val title: String, val ageMs: Long)

/** Picks which page, if any, to offer. Pure, so it is tested on the JVM. */
object ContinueLogic {
    /** A page older than this is no longer "where you left off". */
    const val FRESH_MS = 15 * 60_000L

    /**
     * The newest reply that is fresh, is a plain web address, and is not already open here (ignoring a trailing slash
     * and a #fragment). Null when nothing qualifies, so the menu row hides itself instead of offering a page you already have.
     */
    fun pick(replies: List<WhereReply>, openUrls: Collection<String>): ContinueOffer? {
        val open = openUrls.map(::normalize).toSet()
        return replies
            .filter { it.ageMs in 0..FRESH_MS && TabInbox.isWebAddress(it.url) && normalize(it.url) !in open }
            .minByOrNull { it.ageMs }
            ?.let { ContinueOffer(it.deviceId, it.deviceName, it.url, it.title, it.ageMs) }
    }

    fun normalize(url: String): String = url.substringBefore('#').trimEnd('/').lowercase()
}

/**
 * Holds this device's current page (for other devices that ask) and the latest offer from the other devices.
 * Private tabs are never published: [publish] with a private tab clears the page, so nothing from a private tab
 * ever leaves the device.
 */
object ContinueState {
    @Volatile private var url = ""
    @Volatile private var title = ""
    @Volatile private var updatedAt = 0L
    @Volatile var sharing = true

    private val _offer = MutableStateFlow<ContinueOffer?>(null)
    /** The page to offer in the menu, or null. */
    val offer: StateFlow<ContinueOffer?> = _offer.asStateFlow()

    fun publish(pageUrl: String, pageTitle: String, isPrivate: Boolean) {
        if (isPrivate || !TabInbox.isWebAddress(pageUrl)) { if (isPrivate) { url = ""; title = "" }; return }
        if (pageUrl == url && pageTitle == title) return
        url = pageUrl; title = pageTitle; updatedAt = System.currentTimeMillis()
    }

    /** The reply to a "where" question, or null when sharing is off or nothing is open. */
    fun answer(): JSONObject? {
        if (!sharing || url.isEmpty()) return null
        return JSONObject().put("url", url).put("title", title.take(200)).put("age", System.currentTimeMillis() - updatedAt)
    }

    /** Asks every paired device on the network what it is showing and updates [offer]. Blocking: call off the main thread. */
    fun refresh(link: DeviceLink, openUrls: Collection<String>) {
        val replies = link.store.devices.value.filter { link.isOnline(it.id) }.mapNotNull { device ->
            val body = link.ask(device.id, DeviceHub.TYPE_WHERE, JSONObject()) ?: return@mapNotNull null
            if (!body.has("url")) return@mapNotNull null
            WhereReply(device.id, device.name, body.optString("url"), body.optString("title"), body.optLong("age", Long.MAX_VALUE))
        }
        val next = ContinueLogic.pick(replies, openUrls)
        if (next?.url != _offer.value?.url) BrowserLog.info("devices", if (next == null) "No page to continue from another device" else "Can continue a page from \"${next.deviceName}\"")
        _offer.value = next
    }

    fun clearOffer() { _offer.value = null }
}
