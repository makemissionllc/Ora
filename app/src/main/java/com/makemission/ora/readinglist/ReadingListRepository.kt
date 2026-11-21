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
data class ReadingItem(val url: String, val title: String, val addedAt: Long, val read: Boolean, /** When [read] was last changed, so linked devices can tell which change is newer. */ val readAt: Long = 0L)

/**
 * The reading list: pages saved to read later, one list per profile (`ora_reading_list`, key by profile id).
 * Private tabs are never added here; the caller checks that before calling [add]. Addresses are logged as
 * domains only.
 */
class ReadingListRepository(context: Context, profileId: String = "default") {
    private val prefs = context.applicationContext.getSharedPreferences("ora_reading_list", Context.MODE_PRIVATE)
    private val key = "items_$profileId"
    private val removedKey = "removed_$profileId"

    private var removed: Map<String, Long> = loadRemoved()

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
        removed = removed + (url to System.currentTimeMillis())
        save(_items.value.filterNot { it.url == url })
        BrowserLog.info("readinglist", "Removed ${LogPrivacy.domainOf(url)} from the reading list")
    }

    fun setRead(url: String, read: Boolean) {
        if (_items.value.none { it.url == url && it.read != read }) return
        save(_items.value.map { if (it.url == url) it.copy(read = read, readAt = System.currentTimeMillis()) else it })
        BrowserLog.info("readinglist", "Marked ${LogPrivacy.domainOf(url)} as ${if (read) "read" else "unread"}")
    }

    /** Removes every read page. Returns how many went. */
    fun clearRead(): Int {
        val gone = _items.value.count { it.read }
        if (gone > 0) {
            val now = System.currentTimeMillis()
            removed = removed + _items.value.filter { it.read }.associate { it.url to now }
            save(_items.value.filterNot { it.read })
            BrowserLog.info("readinglist", "Cleared $gone read pages from the reading list")
        }
        return gone
    }

    /** The list and the removals, as sent to a linked device. */
    fun state(): ReadingState = ReadingState(_items.value, removed)

    /**
     * Merges another device's list into this one ([ReadingMerge]). Returns how many pages are new here, so the
     * caller can say "Synced 3 pages". Nothing is written when the merge changes nothing.
     */
    fun mergeFrom(other: ReadingState): Int {
        val before = _items.value
        val merged = ReadingMerge.merge(state(), other)
        val added = merged.items.count { m -> before.none { it.url == m.url } }
        if (merged.items != before || merged.removed != removed) {
            removed = merged.removed
            save(merged.items)
            BrowserLog.info("readinglist", "Merged the reading list from another device: $added new, ${before.count { b -> merged.items.none { it.url == b.url } }} removed")
        }
        return added
    }

    private fun removedJson(): String {
        val o = JSONObject()
        removed.forEach { (url, time) -> o.put(url, time) }
        return o.toString()
    }

    private fun loadRemoved(): Map<String, Long> {
        val raw = prefs.getString(removedKey, null) ?: return emptyMap()
        return runCatching { val o = JSONObject(raw); o.keys().asSequence().associateWith { o.getLong(it) } }.getOrDefault(emptyMap())
    }

    private fun save(list: List<ReadingItem>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("url", it.url).put("title", it.title).put("addedAt", it.addedAt).put("read", it.read).put("readAt", it.readAt)) }
        prefs.edit().putString(key, array.toString()).putString(removedKey, removedJson()).apply()
        _items.value = list
    }

    companion object {
        private val shared = HashMap<String, ReadingListRepository>()

        /** One repository per profile for the whole app, so the page, the menu and the device sync all see the same list. */
        @Synchronized
        fun shared(context: Context, profileId: String): ReadingListRepository =
            shared.getOrPut(profileId) { ReadingListRepository(context, profileId) }
    }

    private fun load(): List<ReadingItem> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                ReadingItem(o.getString("url"), o.optString("title", o.getString("url")), o.optLong("addedAt", 0L), o.optBoolean("read", false), o.optLong("readAt", 0L))
            }
        }.getOrElse {
            BrowserLog.warn("readinglist", "The reading list could not be read, starting empty", it)
            emptyList()
        }
    }
}
