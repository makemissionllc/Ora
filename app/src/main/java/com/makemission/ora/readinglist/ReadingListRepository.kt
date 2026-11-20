package com.makemission.ora.readinglist

import android.content.Context
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A page saved to read later. [read] turns true once it has been opened or marked. */
data class ReadingItem(val url: String, val title: String, val addedAt: Long, val read: Boolean)

/**
 * The reading list: pages saved to read later, one list per profile (`ora_reading_list`, key by profile id).
 * Private tabs are never added here; the caller checks that before calling [add]. Addresses are logged as
 * domains only.
 */
class ReadingListRepository(context: Context, profileId: String = "default") {
    private val prefs = context.applicationContext.getSharedPreferences("ora_reading_list", Context.MODE_PRIVATE)
    private val key = "items_$profileId"

    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<ReadingItem>> = _items.asStateFlow()

    fun contains(url: String): Boolean = _items.value.any { it.url == url }

    /** Adds the page at the top as unread. Returns false when it is already listed or has no address. */
    fun add(url: String, title: String): Boolean {
        if (url.isBlank() || contains(url)) return false
        save(listOf(ReadingItem(url, title.ifBlank { url }, System.currentTimeMillis(), read = false)) + _items.value)
        BrowserLog.info("readinglist", "Saved ${LogPrivacy.domainOf(url)} to the reading list")
        return true
    }

    fun remove(url: String) {
        if (!contains(url)) return
        save(_items.value.filterNot { it.url == url })
        BrowserLog.info("readinglist", "Removed ${LogPrivacy.domainOf(url)} from the reading list")
    }

    fun setRead(url: String, read: Boolean) {
        if (_items.value.none { it.url == url && it.read != read }) return
        save(_items.value.map { if (it.url == url) it.copy(read = read) else it })
        BrowserLog.info("readinglist", "Marked ${LogPrivacy.domainOf(url)} as ${if (read) "read" else "unread"}")
    }

    /** Removes every read page. Returns how many went. */
    fun clearRead(): Int {
        val gone = _items.value.count { it.read }
        if (gone > 0) {
            save(_items.value.filterNot { it.read })
            BrowserLog.info("readinglist", "Cleared $gone read pages from the reading list")
        }
        return gone
    }

    private fun save(list: List<ReadingItem>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("url", it.url).put("title", it.title).put("addedAt", it.addedAt).put("read", it.read)) }
        prefs.edit().putString(key, array.toString()).apply()
        _items.value = list
    }

    private fun load(): List<ReadingItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                ReadingItem(o.getString("url"), o.optString("title", o.getString("url")), o.optLong("addedAt", 0L), o.optBoolean("read", false))
            }
        }.getOrElse {
            BrowserLog.warn("readinglist", "The reading list could not be read, starting empty", it)
            emptyList()
        }
    }
}
