package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.log.BrowserLog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Remembers each profile's open tabs so quick resume can reopen them on the next launch. For each tab
 * it keeps the address and the page's session state (its back and forward history), plus which tab
 * was selected. Private tabs are never kept. Stored in private preferences.
 */
class SessionStore(private val storage: Storage) {

    constructor(context: Context) : this(PrefsStorage(context.applicationContext))

    /** Where the saved text lives. Preferences in the app; a map in tests. */
    interface Storage {
        fun get(key: String): String?
        fun put(key: String, value: String)
        fun remove(key: String)
        fun clear()
    }

    private class PrefsStorage(context: Context) : Storage {
        private val prefs = context.getSharedPreferences("ora_session", Context.MODE_PRIVATE)
        override fun get(key: String): String? = prefs.getString(key, null)
        override fun put(key: String, value: String) = prefs.edit().putString(key, value).apply()
        override fun remove(key: String) = prefs.edit().remove(key).apply()
        override fun clear() = prefs.edit().clear().apply()
    }

    /** One saved tab: its address, and the GeckoView session state that holds its history (null if none). */
    data class SavedTab(val url: String, val state: String?)

    /** Saved tabs for one profile, and the index of the tab that was selected. */
    data class Saved(val tabs: List<SavedTab>, val selected: Int)

    /**
     * Replaces the saved tabs for a profile. An empty list never overwrites a save: an empty state means
     * "nothing was ready to save", not "the user has no tabs" (closing the last tab leaves a start page).
     * Returns false when nothing was written.
     */
    fun save(profileId: String, tabs: List<SavedTab>, selected: Int): Boolean {
        if (tabs.isEmpty()) {
            BrowserLog.warn("session", "Kept the earlier saved tabs: there were no tabs to save")
            return false
        }
        val array = JSONArray()
        tabs.forEach { tab ->
            array.put(JSONObject().put("url", tab.url).put("state", tab.state ?: JSONObject.NULL))
        }
        val json = JSONObject()
            .put("selected", selected)
            .put("tabs", array)
        storage.put(key(profileId), json.toString())
        return true
    }

    /** The saved tabs for a profile, or null when nothing is saved or the saved data cannot be read. */
    fun load(profileId: String): Saved? {
        val raw = storage.get(key(profileId)) ?: return null
        return try {
            val json = JSONObject(raw)
            val selected = json.optInt("selected", 0)
            if (json.has("tabs")) {
                val array = json.getJSONArray("tabs")
                val tabs = List(array.length()) {
                    val item = array.getJSONObject(it)
                    SavedTab(item.getString("url"), item.optString("state", "").ifEmpty { null })
                }
                Saved(tabs, selected)
            } else {
                // Saved by an earlier version: addresses only, no history to restore.
                val array = json.getJSONArray("urls")
                Saved(List(array.length()) { SavedTab(array.getString(it), null) }, selected)
            }
        } catch (e: JSONException) {
            BrowserLog.error("session", "Could not read the saved tabs for profile $profileId", e)
            null
        }
    }

    /** Forgets every saved tab, for all profiles. Used when quick resume is turned off. */
    fun clear() {
        storage.clear()
    }

    private fun key(profileId: String) = "tabs_$profileId"
}
