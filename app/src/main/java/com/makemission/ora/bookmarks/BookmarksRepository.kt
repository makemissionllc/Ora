package com.makemission.ora.bookmarks

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val url: String, val title: String)

/** Stores bookmarks as a JSON array in private preferences. */
class BookmarksRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ora_bookmarks", Context.MODE_PRIVATE)

    private val _bookmarks = MutableStateFlow(load())
    val bookmarks: StateFlow<List<Bookmark>> = _bookmarks.asStateFlow()

    fun isBookmarked(url: String): Boolean = _bookmarks.value.any { it.url == url }

    /** Adds the page if it is not bookmarked yet, otherwise removes it. */
    fun toggle(url: String, title: String) {
        val current = _bookmarks.value
        val updated = if (current.any { it.url == url }) {
            current.filterNot { it.url == url }
        } else {
            listOf(Bookmark(url, title.ifBlank { url })) + current
        }
        save(updated)
    }

    fun remove(url: String) = save(_bookmarks.value.filterNot { it.url == url })

    private fun save(list: List<Bookmark>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("url", it.url).put("title", it.title)) }
        prefs.edit().putString(KEY, array.toString()).apply()
        _bookmarks.value = list
    }

    private fun load(): List<Bookmark> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            Bookmark(obj.getString("url"), obj.getString("title"))
        }
    }

    private companion object {
        const val KEY = "bookmarks"
    }
}
