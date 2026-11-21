package com.makemission.ora.devices

import android.content.Context
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A page another device sent to this one. */
data class ReceivedTab(val id: String, val fromName: String, val url: String, val title: String, val at: Long)

/**
 * Pages sent from the person's other devices, kept until they open or dismiss them (`ora_device_inbox`, newest first,
 * at most [MAX_ITEMS]). Only plain web addresses are accepted: a paired device cannot make this one open a
 * `file:`, `javascript:` or app link. Addresses are logged as domains only.
 */
class TabInbox(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_device_inbox", Context.MODE_PRIVATE)

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<ReceivedTab>> = _items.asStateFlow()

    private val _arrived = MutableSharedFlow<ReceivedTab>(extraBufferCapacity = 8)
    /** Emits each page as it arrives, so the browser screen can offer to open it at once. */
    val arrived: SharedFlow<ReceivedTab> = _arrived.asSharedFlow()

    /** Called with a message from [from]. Returns null (a plain "ok") whether or not the page was accepted. */
    fun receive(from: PairedDevice, body: JSONObject): JSONObject? {
        val url = body.optString("url")
        if (!isWebAddress(url)) {
            BrowserLog.warn("devices", "Ignored a page from \"${from.name}\": not a web address")
            return null
        }
        val item = ReceivedTab(UUID.randomUUID().toString(), from.name, url, body.optString("title").take(200).ifBlank { url }, System.currentTimeMillis())
        // The same page sent again moves to the top instead of listing twice.
        save((listOf(item) + _items.value.filterNot { it.url == url && it.fromName == from.name }).take(MAX_ITEMS))
        BrowserLog.info("devices", "Received ${LogPrivacy.domainOf(url)} from \"${from.name}\"")
        _arrived.tryEmit(item)
        return null
    }

    fun remove(id: String) {
        if (_items.value.any { it.id == id }) save(_items.value.filterNot { it.id == id })
    }

    fun clear() {
        if (_items.value.isNotEmpty()) {
            BrowserLog.info("devices", "Cleared ${_items.value.size} received pages")
            save(emptyList())
        }
    }

    private fun save(list: List<ReceivedTab>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("id", it.id).put("from", it.fromName).put("url", it.url).put("title", it.title).put("at", it.at)) }
        prefs.edit().putString("items", array.toString()).apply()
        _items.value = list
    }

    private fun load(): List<ReceivedTab> {
        val raw = prefs.getString("items", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                ReceivedTab(o.getString("id"), o.getString("from"), o.getString("url"), o.optString("title"), o.optLong("at"))
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        const val MAX_ITEMS = 30
        fun isWebAddress(url: String): Boolean =
            url.length <= 8000 && (url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true))
    }
}
