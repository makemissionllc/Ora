package com.makemission.ora.settings

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

/**
 * The searches typed in normal tabs, newest first, so an empty address bar can offer them again.
 * Kept on the device in private preferences, capped, one row per phrase. Private tabs never record.
 */
class RecentSearches(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_recent_searches", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<String>> = _items.asStateFlow()

    fun record(text: String) {
        save(withSearch(_items.value, text))
    }

    fun remove(text: String) {
        save(_items.value.filterNot { it.equals(text, ignoreCase = true) })
        BrowserLog.info("search", "Removed one recent search")
    }

    fun clear() {
        save(emptyList())
        BrowserLog.info("search", "Cleared recent searches")
    }

    private fun save(list: List<String>) {
        _items.value = list
        prefs.edit().putString(KEY, JSONArray(list).toString()).apply()
    }

    private fun load(): List<String> = try {
        val array = JSONArray(prefs.getString(KEY, "[]"))
        (0 until array.length()).map { array.getString(it) }
    } catch (error: org.json.JSONException) {
        BrowserLog.warn("search", "Recent searches could not be read", error)
        emptyList()
    }

    companion object {
        private const val KEY = "items"
        const val MAX = 20

        /** [list] with [text] first, without a repeat (ignoring case), cut to [MAX]. Blank text changes nothing. */
        fun withSearch(list: List<String>, text: String): List<String> {
            val phrase = text.trim()
            if (phrase.isEmpty()) return list
            return (listOf(phrase) + list.filterNot { it.equals(phrase, ignoreCase = true) }).take(MAX)
        }
    }
}
