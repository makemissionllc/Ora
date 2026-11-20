package com.makemission.ora.settings

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/** Pure helpers for the element zapper's rules (site name to the CSS selectors hidden on it), kept apart so they can be tested. */
object ZapLogic {
    const val MAX_PER_SITE = 60

    /** Adds [selector] to [host], ignoring blanks and repeats, and keeping the newest [MAX_PER_SITE]. */
    fun add(rules: Map<String, List<String>>, host: String, selector: String): Map<String, List<String>> {
        val h = host.trim().lowercase()
        val s = selector.trim()
        if (h.isEmpty() || s.isEmpty()) return rules
        val current = rules[h].orEmpty()
        if (s in current) return rules
        return rules + (h to (current + s).takeLast(MAX_PER_SITE))
    }

    /** Removes every rule for [host]. */
    fun clear(rules: Map<String, List<String>>, host: String): Map<String, List<String>> = rules - host.trim().lowercase()

    /** Sites with rules, most hidden first, then by name. */
    fun summary(rules: Map<String, List<String>>): List<Pair<String, Int>> =
        rules.filterValues { it.isNotEmpty() }.map { it.key to it.value.size }.sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
}

/** The hide rules for one profile, saved in `ora_zapper_<profileId>`. Private tabs never reach this store. */
class ElementZapper(context: Context, profileId: String) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_zapper_$profileId", Context.MODE_PRIVATE)
    private val _rules = MutableStateFlow(load())
    val rules: StateFlow<Map<String, List<String>>> = _rules.asStateFlow()

    private fun load(): Map<String, List<String>> = runCatching {
        val o = JSONObject(prefs.getString(KEY, "{}") ?: "{}")
        o.keys().asSequence().associateWith { k -> o.getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) } } }
    }.getOrElse { BrowserLog.warn("zapper", "Could not read hidden elements", it); emptyMap() }

    private fun save(map: Map<String, List<String>>) {
        val o = JSONObject()
        map.forEach { (h, l) -> o.put(h, JSONArray(l)) }
        prefs.edit().putString(KEY, o.toString()).apply()
        _rules.value = map
    }

    fun add(host: String, selector: String) {
        save(ZapLogic.add(_rules.value, host, selector))
        BrowserLog.info("zapper", "An element was hidden on a site")
    }

    fun clear(host: String) {
        save(ZapLogic.clear(_rules.value, host))
        BrowserLog.info("zapper", "Hidden elements were shown again on a site")
    }

    companion object { private const val KEY = "rules" }
}
