package com.makemission.ora.settings

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** One page in a saved session. */
data class SessionPage(val url: String, val title: String)

/** A named set of pages saved to open again later, such as "Trip planning". */
data class SavedSession(val name: String, val savedAt: Long, val pages: List<SessionPage>)

/** What opening a session will do: the pages to open and how many were skipped because they are open already. */
data class OpenPlan(val toOpen: List<SessionPage>, val alreadyOpen: Int)

/** Pure helpers for sessions, kept apart from storage so they can be tested. */
object SessionLogic {
    const val MAX_SESSIONS = 20

    /** Pages worth saving from the open tabs: no private tabs, no start page, no duplicates, in tab order. */
    fun pagesToSave(tabs: List<Triple<String, String, Boolean>>): List<SessionPage> =
        tabs.filter { (url, _, isPrivate) -> !isPrivate && url.isNotBlank() && !url.startsWith("ora://") && !url.startsWith("about:") }
            .map { (url, title, _) -> SessionPage(url, title) }
            .distinctBy { it.url }

    /** A name to suggest: the first page's site and the day, such as "example.com, 20 Nov". */
    fun defaultName(pages: List<SessionPage>, now: Long): String {
        val day = SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(now))
        val site = pages.firstOrNull()?.let { siteKeyFor(it.url) }.orEmpty()
        return if (site.isEmpty()) "Session, $day" else "$site, $day"
    }

    /** Opening a session skips pages that are already open, so nothing is opened twice. */
    fun planOpen(session: SavedSession, openUrls: Set<String>): OpenPlan {
        val (open, fresh) = session.pages.partition { it.url in openUrls }
        return OpenPlan(fresh, open.size)
    }

    /** Adds [session], replacing one with the same name (ignoring case), newest first, keeping at most [MAX_SESSIONS]. */
    fun withSession(list: List<SavedSession>, session: SavedSession): List<SavedSession> =
        (listOf(session) + list.filter { !it.name.equals(session.name, ignoreCase = true) }).take(MAX_SESSIONS)
}

/**
 * Named sessions for one profile, kept in that profile's own preferences file. Private tabs are never saved,
 * so nothing about private browsing reaches disk.
 */
class SavedSessions(context: Context, profileId: String) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_saved_sessions_$profileId", Context.MODE_PRIVATE)
    private val _sessions = MutableStateFlow(load())
    val sessions: StateFlow<List<SavedSession>> = _sessions.asStateFlow()

    private fun load(): List<SavedSession> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, "[]") ?: "[]")
        (0 until arr.length()).map { arr.getJSONObject(it) }.map { o ->
            val pages = o.getJSONArray("p")
            SavedSession(o.getString("n"), o.getLong("t"), (0 until pages.length()).map { pages.getJSONObject(it) }.map { SessionPage(it.getString("u"), it.optString("h")) })
        }
    }.getOrElse { BrowserLog.warn("sessions", "Could not read saved sessions", it); emptyList() }

    private fun save(list: List<SavedSession>) {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().put("n", s.name).put("t", s.savedAt).put("p", JSONArray().also { pa -> s.pages.forEach { pa.put(JSONObject().put("u", it.url).put("h", it.title)) } }))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
        _sessions.value = list
    }

    /** Saves [pages] under [name]. Returns false when there is nothing to save. */
    fun add(name: String, pages: List<SessionPage>, now: Long = System.currentTimeMillis()): Boolean {
        if (pages.isEmpty()) return false
        val clean = name.trim().ifEmpty { SessionLogic.defaultName(pages, now) }.take(40)
        save(SessionLogic.withSession(_sessions.value, SavedSession(clean, now, pages)))
        BrowserLog.info("sessions", "Session saved with ${pages.size} pages")
        return true
    }

    fun remove(name: String) {
        save(_sessions.value.filter { it.name != name })
        BrowserLog.info("sessions", "Session removed")
    }

    companion object { private const val KEY = "sessions" }
}
