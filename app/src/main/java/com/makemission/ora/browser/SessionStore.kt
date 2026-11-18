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
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ora_session", Context.MODE_PRIVATE)

    /** One saved tab: its address, and the GeckoView session state that holds its history (null if none). */
    data class SavedTab(val url: String, val state: String?, val groupId: String? = null, val lastActive: Long? = null)

    /** Saved tabs for one profile, and the index of the tab that was selected. */
    data class Saved(val tabs: List<SavedTab>, val selected: Int)

    /** Replaces the saved tabs for a profile. An empty list removes what was saved. */
    fun save(profileId: String, tabs: List<SavedTab>, selected: Int) {
        if (tabs.isEmpty()) {
            prefs.edit().remove(key(profileId)).apply()
            return
        }
        val array = JSONArray()
        tabs.forEach { tab ->
            array.put(JSONObject().put("url", tab.url).put("state", tab.state ?: JSONObject.NULL).put("group", tab.groupId ?: JSONObject.NULL).put("active", tab.lastActive ?: JSONObject.NULL))
        }
        val json = JSONObject()
            .put("selected", selected)
            .put("tabs", array)
        prefs.edit().putString(key(profileId), json.toString()).apply()
    }

    /** The saved tabs for a profile, or null when nothing is saved or the saved data cannot be read. */
    fun load(profileId: String): Saved? {
        val raw = prefs.getString(key(profileId), null) ?: return null
        return try {
            val json = JSONObject(raw)
            val selected = json.optInt("selected", 0)
            if (json.has("tabs")) {
                val array = json.getJSONArray("tabs")
                val tabs = List(array.length()) {
                    val item = array.getJSONObject(it)
                    SavedTab(item.getString("url"), item.optString("state", "").ifEmpty { null }, item.optString("group", "").ifEmpty { null }, item.optLong("active", 0L).takeIf { it > 0 })
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
        prefs.edit().clear().apply()
    }

    private fun key(profileId: String) = "tabs_$profileId"
}
