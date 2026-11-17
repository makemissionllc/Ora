package com.makemission.ora.permissions

import android.Manifest
import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * The permissions a site can ask for. [label] is how a prompt names it. [androidPermissions] are the
 * Android permissions Ora must also hold before the site can use the feature.
 */
enum class SitePermission(
    val key: String,
    val label: String,
    val androidPermissions: List<String>,
) {
    LOCATION("location", "your location", listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)),
    CAMERA("camera", "your camera", listOf(Manifest.permission.CAMERA)),
    MICROPHONE("microphone", "your microphone", listOf(Manifest.permission.RECORD_AUDIO)),
    NOTIFICATIONS("notifications", "notifications", listOf(Manifest.permission.POST_NOTIFICATIONS));
}

/**
 * Keeps a profile's Allow and Block answers to site permission prompts, one per site and permission.
 * Stored as one JSON object in private preferences. Each profile has its own. "Not now" is never stored,
 * so the site asks again next time.
 */
class SitePermissionStore(context: Context, profileId: String = "default") {

    private val prefs = context.applicationContext
        .getSharedPreferences("ora_site_permissions", Context.MODE_PRIVATE)

    /** Like history, the default profile keeps the plain key. Each other profile gets its own. */
    private val key = if (profileId == "default") "choices" else "choices_$profileId"

    private val _choices = MutableStateFlow(load())

    /** Saved answers, keyed "origin|permission". True means allowed, false means blocked. */
    val choices: StateFlow<Map<String, Boolean>> = _choices.asStateFlow()

    /** The saved answer for this site and permission: true allowed, false blocked, null when Ora should ask. */
    fun saved(origin: String, permission: SitePermission): Boolean? = _choices.value[slot(origin, permission)]

    fun save(origin: String, permission: SitePermission, allowed: Boolean) {
        write(_choices.value + (slot(origin, permission) to allowed))
        BrowserLog.info("permission", "Saved ${permission.key} as ${if (allowed) "allowed" else "blocked"}")
    }

    /** Forgets every saved answer in this profile. Returns how many were removed. */
    fun clear(): Int {
        val count = _choices.value.size
        write(emptyMap())
        BrowserLog.info("permission", "Cleared $count saved site permission choices")
        return count
    }

    private fun write(map: Map<String, Boolean>) {
        val json = JSONObject()
        map.forEach { (slot, allowed) -> json.put(slot, allowed) }
        prefs.edit().putString(key, json.toString()).apply()
        _choices.value = map
    }

    private fun load(): Map<String, Boolean> {
        val raw = prefs.getString(key, null) ?: return emptyMap()
        return try {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getBoolean(it) }
        } catch (e: Exception) {
            // Quality of life: a damaged list starts empty instead of stopping the app, and the log says why.
            BrowserLog.error("permission", "Could not read saved site permissions", e)
            emptyMap()
        }
    }

    companion object {
        private fun slot(origin: String, permission: SitePermission) = "$origin|${permission.key}"

        /** How many different sites have at least one saved answer. */
        fun countSites(choices: Map<String, Boolean>): Int =
            choices.keys.map { it.substringBefore('|') }.distinct().size
    }
}
