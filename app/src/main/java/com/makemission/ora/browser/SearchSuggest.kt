package com.makemission.ora.browser

import com.makemission.ora.log.BrowserLog
import com.makemission.ora.settings.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Query suggestions from the default search engine, the way Google shows them under the address bar as you
 * type. Every engine Ora offers answers in the same OpenSearch shape: `["typed", ["one", "two", ...]]`.
 *
 * The typed text goes to the engine the user chose, so it is off in private tabs and has its own switch in
 * Settings. Nothing is stored on disk and no search text is ever logged; only a count and the engine name are.
 */
object SearchSuggest {
    /** The engine to ask, or null when suggestions are switched off. Set from the Search suggestions setting. */
    @Volatile var engine: SearchEngine? = null

    private const val MAX_SUGGESTIONS = 6
    private const val CACHE_SIZE = 40
    private val cache = object : LinkedHashMap<String, List<String>>(CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>?) = size > CACHE_SIZE
    }

    /** True when [typed] is worth asking the engine about: a search, not an address or a lone character. */
    fun worthAsking(typed: String): Boolean {
        val text = typed.trim()
        if (text.length < 2 || text.contains("://")) return false
        return text.contains(' ') || !text.contains('.')
    }

    /** What the engine thinks you are typing, or nothing when it is off, offline or slow. Call on any thread. */
    suspend fun fetch(typed: String): List<String> {
        val current = engine ?: return emptyList()
        val query = typed.trim()
        if (!worthAsking(query)) return emptyList()
        val key = current.key + "\u0000" + query.lowercase()
        synchronized(cache) { cache[key] }?.let { return it }
        val found = withContext(Dispatchers.IO) {
            try {
                val url = current.suggestTemplate.replace("%s", URLEncoder.encode(query, "UTF-8"))
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connectTimeout = 2_000
                conn.readTimeout = 2_000
                conn.setRequestProperty("Accept", "application/json")
                try {
                    if (conn.responseCode != 200) return@withContext emptyList<String>()
                    val body = conn.inputStream.bufferedReader().use { it.readText() }
                    parse(body, query)
                } finally {
                    conn.disconnect()
                }
            } catch (error: java.io.IOException) {
                BrowserLog.warn("search", "Suggestions from ${current.label} did not arrive: ${error.javaClass.simpleName}")
                emptyList()
            }
        }
        if (found.isNotEmpty()) synchronized(cache) { cache[key] = found }
        return found
    }

    /** The suggestions in an OpenSearch answer, without repeats or the typed text itself. Bad answers give an empty list. */
    internal fun parse(json: String, typed: String): List<String> = try {
        val list = JSONArray(json).optJSONArray(1) ?: JSONArray()
        (0 until list.length())
            .mapNotNull { list.optString(it).trim().takeIf { s -> s.isNotEmpty() } }
            .filterNot { it.equals(typed.trim(), ignoreCase = true) }
            .distinct()
            .take(MAX_SUGGESTIONS)
    } catch (_: org.json.JSONException) {
        emptyList()
    }
}
