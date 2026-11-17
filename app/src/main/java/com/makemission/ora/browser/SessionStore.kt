package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.log.BrowserLog
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Remembers each profile's open tabs (their addresses and which one was selected) so quick resume
 * can reopen them on the next launch. Private tabs are never kept. Stored in private preferences.
 */
class SessionStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("ora_session", Context.MODE_PRIVATE)

    /** Saved tab addresses for one profile, and the index of the tab that was selected. */
    data class Saved(val urls: List<String>, val selected: Int)

    /** Replaces the saved tabs for a profile. An empty list removes what was saved. */
    fun save(profileId: String, urls: List<String>, selected: Int) {
        if (urls.isEmpty()) {
            prefs.edit().remove(key(profileId)).apply()
            return
        }
        val json = JSONObject()
            .put("selected", selected)
            .put("urls", JSONArray(urls))
        prefs.edit().putString(key(profileId), json.toString()).apply()
    }

    /** The saved tabs for a profile, or null when nothing is saved or the saved data cannot be read. */
    fun load(profileId: String): Saved? {
        val raw = prefs.getString(key(profileId), null) ?: return null
        return try {
            val json = JSONObject(raw)
            val array = json.getJSONArray("urls")
            Saved(List(array.length()) { array.getString(it) }, json.optInt("selected", 0))
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
