package com.makemission.ora.settings

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** The key a site's text size is stored under: its host without a leading "www.", lower-case. Empty when [url] has no host. */
fun siteKeyFor(url: String): String {
    val host = url.substringAfter("://", "").substringBefore('/').substringBefore('?').substringBefore('#')
        .substringAfter('@').substringBefore(':').lowercase()
    return host.removePrefix("www.")
}

/**
 * Text sizes that differ from the global one, kept per site so a long article can be larger without
 * changing every other page. Saved as a JSON object of host to percent in private preferences.
 * A private tab never reads or writes here: nothing about private browsing reaches disk.
 */
class SiteTextSizes(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_site_text_sizes", Context.MODE_PRIVATE)
    private val _sizes = MutableStateFlow(load())
    /** Percent by site key. A site not listed uses the global size. */
    val sizes: StateFlow<Map<String, Int>> = _sizes.asStateFlow()

    private fun load(): Map<String, Int> = runCatching {
        val json = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        json.keys().asSequence().associateWith { json.getInt(it).coerceIn(SettingsRepository.TEXT_SIZE_MIN, SettingsRepository.TEXT_SIZE_MAX) }
    }.getOrDefault(emptyMap())

    private fun save(map: Map<String, Int>) {
        prefs.edit().putString(KEY, JSONObject(map as Map<*, *>).toString()).apply()
        _sizes.value = map
    }

    /** The size to use for [site]: its own, or [global]. */
    fun effective(site: String, global: Int): Int = _sizes.value[site] ?: global

    fun set(site: String, percent: Int) {
        if (site.isEmpty()) return
        val value = percent.coerceIn(SettingsRepository.TEXT_SIZE_MIN, SettingsRepository.TEXT_SIZE_MAX)
        save(_sizes.value + (site to value))
        BrowserLog.info("settings", "Site text size set to $value%")
    }

    fun clear(site: String) {
        if (site !in _sizes.value) return
        save(_sizes.value - site)
        BrowserLog.info("settings", "Site text size cleared")
    }

    fun clearAll() {
        val n = _sizes.value.size
        save(emptyMap())
        BrowserLog.info("settings", "Cleared $n site text sizes")
    }

    companion object {
        private const val KEY = "sizes"
        @Volatile private var instance: SiteTextSizes? = null
        /** One shared store, so the browser screen and Settings see the same sizes. */
        fun get(context: Context): SiteTextSizes = instance ?: synchronized(this) { instance ?: SiteTextSizes(context).also { instance = it } }
    }
}
