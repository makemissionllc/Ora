package com.makemission.ora.settings

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/** How one site is changed: dark colours, text that fills the width, and a font ("" keeps the site's own; "serif", "sans" or "mono"). */
data class SiteTweak(val dark: Boolean = false, val wide: Boolean = false, val font: String = "") {
    val isDefault get() = !dark && !wide && font.isEmpty()
    val count get() = listOf(dark, wide, font.isNotEmpty()).count { it }

    companion object { val FONTS = listOf("" to "Own", "serif" to "Serif", "sans" to "Sans", "mono" to "Mono") }
}

/** Pure helpers for the tweak map, kept apart from storage so they can be tested. */
object TweakLogic {
    /** Sets [tweak] for [site]; a tweak with nothing on removes the site, so the list never holds empty rows. */
    fun with(map: Map<String, SiteTweak>, site: String, tweak: SiteTweak): Map<String, SiteTweak> {
        if (site.isEmpty()) return map
        val clean = tweak.copy(font = tweak.font.takeIf { it in SiteTweak.FONTS.map { f -> f.first } } ?: "")
        return if (clean.isDefault) map - site else map + (site to clean)
    }

    fun summary(map: Map<String, SiteTweak>): List<Pair<String, SiteTweak>> = map.toList().sortedBy { it.first }
}

/**
 * Tweaks kept per site (host without "www."), saved in private preferences. Private tabs never write here.
 * One shared store, so the browser screen and the helper extension see the same list.
 */
class SiteTweaks(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_site_tweaks", Context.MODE_PRIVATE)
    private val _tweaks = MutableStateFlow(load())
    val tweaks: StateFlow<Map<String, SiteTweak>> = _tweaks.asStateFlow()

    private fun load(): Map<String, SiteTweak> = runCatching {
        val o = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        o.keys().asSequence().associateWith { k -> o.getJSONObject(k).let { SiteTweak(it.optBoolean("d"), it.optBoolean("w"), it.optString("f")) } }
    }.getOrElse { BrowserLog.warn("tweaks", "Could not read site tweaks", it); emptyMap() }

    private fun save(map: Map<String, SiteTweak>) {
        val o = JSONObject()
        map.forEach { (k, v) -> o.put(k, JSONObject().put("d", v.dark).put("w", v.wide).put("f", v.font)) }
        prefs.edit().putString(KEY, o.toString()).apply()
        _tweaks.value = map
    }

    fun set(site: String, tweak: SiteTweak) {
        save(TweakLogic.with(_tweaks.value, site, tweak))
        BrowserLog.info("tweaks", "Site tweaks changed: ${tweak.count} on")
    }

    fun reset(site: String) = set(site, SiteTweak())

    companion object {
        private const val KEY = "tweaks"
        @Volatile private var instance: SiteTweaks? = null
        fun get(context: Context): SiteTweaks = instance ?: synchronized(this) { instance ?: SiteTweaks(context).also { instance = it } }
    }
}
