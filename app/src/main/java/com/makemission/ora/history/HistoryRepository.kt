package com.makemission.ora.history

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * One visited address. Each address appears once; a revisit moves it to the top with the new time and adds one
 * to [visits]. Older saves have no count, so they read as one visit.
 */
data class HistoryEntry(val url: String, val title: String, val visitedAt: Long, val visits: Int = 1)

/**
 * How much a page is worth putting first: visits count for more the more recent they are. Every visit is
 * weighted by how fresh the last one was, so a page opened daily this week beats one opened often last year.
 */
fun HistoryEntry.frecency(now: Long = System.currentTimeMillis()): Double {
    val days = ((now - visitedAt).coerceAtLeast(0L)) / DAY_MS.toDouble()
    val recency = when {
        days < 1 -> 4.0
        days < 7 -> 2.0
        days < 30 -> 1.0
        days < 90 -> 0.5
        else -> 0.25
    }
    return kotlin.math.ln(1.0 + visits) * recency
}

private const val DAY_MS = 24L * 60 * 60 * 1000

/** The addresses most worth warming, best first: one per site, ranked by [frecency]. */
fun List<HistoryEntry>.topSites(limit: Int, now: Long = System.currentTimeMillis()): List<HistoryEntry> =
    sortedByDescending { it.frecency(now) }
        .distinctBy { it.url.substringAfter("://").substringBefore('/').lowercase() }
        .take(limit)

/**
 * Keeps the pages a profile has loaded, newest first, as a JSON array in private preferences.
 * Only http and https pages are kept, and the list is capped so it cannot grow without limit.
 */
class HistoryRepository(context: Context, profileId: String = "default") {

    private val prefs = context.applicationContext
        .getSharedPreferences("ora_history", Context.MODE_PRIVATE)

    /** Like bookmarks, the default profile keeps the plain key. Each other profile gets its own list. */
    private val key = if (profileId == "default") "history" else "history_$profileId"

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    /** Puts a finished page at the top of the list. Pages that are not web addresses are skipped. */
    fun record(url: String, title: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) return
        val before = _entries.value.firstOrNull { it.url == url }
        val entry = HistoryEntry(url, title.ifBlank { url }, System.currentTimeMillis(), (before?.visits ?: 0) + 1)
        save((listOf(entry) + _entries.value.filterNot { it.url == url }).take(MAX_ENTRIES))
    }

    fun remove(url: String) {
        save(_entries.value.filterNot { it.url == url })
        BrowserLog.info("history", "Removed one history entry")
    }

    fun clear() {
        val count = _entries.value.size
        save(emptyList())
        BrowserLog.info("history", "Cleared $count history entries")
    }

    private fun save(list: List<HistoryEntry>) {
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().put("url", it.url).put("title", it.title).put("at", it.visitedAt).put("n", it.visits))
        }
        prefs.edit().putString(key, array.toString()).apply()
        _entries.value = list
    }

    private fun load(): List<HistoryEntry> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                HistoryEntry(obj.getString("url"), obj.getString("title"), obj.getLong("at"), obj.optInt("n", 1).coerceAtLeast(1))
            }
        } catch (e: Exception) {
            // Quality of life: a damaged list starts empty instead of stopping the app, and the log says why.
            BrowserLog.error("history", "Could not read saved history", e)
            emptyList()
        }
    }

    companion object {
        /** The most addresses kept. The oldest drop off once the list is full. */
        const val MAX_ENTRIES = 1000
    }
}
