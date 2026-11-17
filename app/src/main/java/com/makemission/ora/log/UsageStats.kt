package com.makemission.ora.log

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One counted action: how many times it ran and when it last ran. */
data class UsageEntry(val action: String, val count: Int, val lastUsed: Long)

/**
 * On-device counts of how often each action runs, so quick controls can be designed from real use.
 *
 * Only fixed action names are stored (for example "setting.appearance"). Never URLs, search text,
 * page titles or page content. Nothing is sent off the device. Turning counting off in Settings
 * stops new counts; existing counts stay until the user clears them.
 */
object UsageStats {
    private const val PREFS_NAME = "ora_usage"
    private const val KEY_COUNT = "count."
    private const val KEY_LAST = "last."

    private var prefs: SharedPreferences? = null
    private var enabled = true

    private val _entries = MutableStateFlow<List<UsageEntry>>(emptyList())
    /** Most used first. */
    val entries: StateFlow<List<UsageEntry>> = _entries.asStateFlow()

    /** Loads saved counts. Call once, when the app starts. */
    fun init(context: Context) {
        if (prefs != null) return
        val store = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = store
        _entries.value = store.all.keys
            .filter { it.startsWith(KEY_COUNT) }
            .map { it.removePrefix(KEY_COUNT) }
            .map { action ->
                UsageEntry(action, store.getInt(KEY_COUNT + action, 0), store.getLong(KEY_LAST + action, 0L))
            }
            .sortedByDescending { it.count }
    }

    /** Turned on or off from Settings. */
    fun setEnabled(on: Boolean) {
        enabled = on
    }

    /** Adds one to [action]. [action] must be a fixed name from the code, never user content. */
    fun count(action: String) {
        if (!enabled) return
        val store = prefs ?: return
        val now = System.currentTimeMillis()
        val next = store.getInt(KEY_COUNT + action, 0) + 1
        store.edit().putInt(KEY_COUNT + action, next).putLong(KEY_LAST + action, now).apply()
        _entries.update { list ->
            val others = list.filter { it.action != action }
            (others + UsageEntry(action, next, now)).sortedByDescending { it.count }
        }
    }

    /** Removes every count. Asked for first on the Diagnostics page. */
    fun clear() {
        val store = prefs ?: return
        store.edit().clear().apply()
        _entries.value = emptyList()
    }
}
