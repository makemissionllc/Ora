package com.makemission.ora.profiles

import android.content.Context
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.ProfileTint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** A profile keeps its own open tabs and bookmarks. History, cookies and logins are shared for now. */
data class Profile(val id: String, val name: String, val tint: ProfileTint = ProfileTint.BASALT)

/**
 * Keeps the list of profiles and which one is active, in private preferences.
 * The first profile is [DEFAULT_ID]; its bookmarks were saved before profiles existed, so they stay put.
 */
class ProfilesRepository(context: Context) {

    // Bookmarks live in the bookmarks file; this one holds the profile list and the active profile.
    private val prefs = context.applicationContext
        .getSharedPreferences("ora_profiles", Context.MODE_PRIVATE)
    private val bookmarkPrefs = context.applicationContext
        .getSharedPreferences("ora_bookmarks", Context.MODE_PRIVATE)

    private val _profiles = MutableStateFlow(load())
    val profiles: StateFlow<List<Profile>> = _profiles.asStateFlow()

    private val _activeId = MutableStateFlow(prefs.getString(KEY_ACTIVE, DEFAULT_ID) ?: DEFAULT_ID)
    val activeId: StateFlow<String> = _activeId.asStateFlow()

    fun select(id: String) {
        if (_profiles.value.none { it.id == id } || id == _activeId.value) return
        prefs.edit().putString(KEY_ACTIVE, id).apply()
        _activeId.value = id
        BrowserLog.info("profiles", "Switched to profile ${_profiles.value.first { it.id == id }.name}")
    }

    /** Adds a profile and makes it active. Blank names are replaced with a numbered one. */
    fun add(name: String, tint: ProfileTint = ProfileTint.BASALT): Profile {
        val clean = name.trim().ifEmpty { "Profile ${_profiles.value.size + 1}" }
        val profile = Profile("p" + System.currentTimeMillis(), clean, tint)
        save(_profiles.value + profile)
        select(profile.id)
        BrowserLog.info("profiles", "Added profile $clean with tint ${tint.label}")
        return profile
    }

    /** Changes a profile's tint. The colours of the whole app follow at once when it is the active profile. */
    fun setTint(id: String, tint: ProfileTint) {
        val current = _profiles.value.firstOrNull { it.id == id } ?: return
        if (current.tint == tint) return
        save(_profiles.value.map { if (it.id == id) it.copy(tint = tint) else it })
        BrowserLog.info("profiles", "Profile ${current.name} tint is now ${tint.label}")
    }

    /** Renames a profile. A blank name is ignored. */
    fun rename(id: String, name: String) {
        val clean = name.trim()
        val current = _profiles.value.firstOrNull { it.id == id } ?: return
        if (clean.isEmpty() || clean == current.name) return
        save(_profiles.value.map { if (it.id == id) it.copy(name = clean) else it })
        BrowserLog.info("profiles", "Renamed profile ${current.name} to $clean")
    }

    /** Removes a profile. The default one cannot be removed, and removing the active one switches to the default. */
    fun remove(id: String) {
        if (id == DEFAULT_ID) return
        save(_profiles.value.filterNot { it.id == id })
        // The profile's bookmarks go with it. Its open tabs are closed by the browser screen.
        bookmarkPrefs.edit().remove("bookmarks_$id").apply()
        if (_activeId.value == id) select(DEFAULT_ID)
        BrowserLog.info("profiles", "Removed profile $id")
    }

    private fun save(list: List<Profile>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("id", it.id).put("name", it.name).put("tint", it.tint.key)) }
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
        _profiles.value = list
    }

    private fun load(): List<Profile> {
        val raw = prefs.getString(KEY_LIST, null)
        val saved = raw?.let { text ->
            val array = JSONArray(text)
            List(array.length()) { i ->
                val obj = array.getJSONObject(i)
                Profile(obj.getString("id"), obj.getString("name"), ProfileTint.fromKey(obj.optString("tint", null)))
            }
        }.orEmpty()
        return if (saved.any { it.id == DEFAULT_ID }) saved else listOf(Profile(DEFAULT_ID, "Personal")) + saved
    }

    companion object {
        const val DEFAULT_ID = "default"
        private const val KEY_LIST = "profiles"
        private const val KEY_ACTIVE = "active"
    }
}
