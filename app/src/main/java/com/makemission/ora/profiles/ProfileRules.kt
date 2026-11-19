package com.makemission.ora.profiles

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * "Always open this site in profile X": one rule per site, kept in `ora_profile_rules` (not per profile, since a
 * rule names the profile). A rule for a site also covers its subdomains. Private tabs never follow a rule.
 */
class ProfileRules(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("ora_profile_rules", Context.MODE_PRIVATE)

    private val _rules = MutableStateFlow(load())
    /** Site (without "www.") to profile id. */
    val rules: StateFlow<Map<String, String>> = _rules.asStateFlow()

    fun set(site: String, profileId: String) {
        val host = hostOf(site) ?: return
        _rules.value = _rules.value + (host to profileId)
        save()
        BrowserLog.info("profiles", "Rule added: $host opens in its own profile")
    }

    fun remove(site: String) {
        if (site !in _rules.value) return
        _rules.value = _rules.value - site
        save()
        BrowserLog.info("profiles", "Rule removed: $site")
    }

    /** The profile id a rule names for this address, or null. */
    fun profileFor(url: String): String? = match(_rules.value, url)

    /** Forgets rules that name a profile that was removed. */
    fun dropMissing(existing: Set<String>) {
        val kept = _rules.value.filterValues { it in existing }
        if (kept.size != _rules.value.size) { _rules.value = kept; save() }
    }

    private fun save() = prefs.edit().putString("rules", JSONObject(_rules.value as Map<*, *>).toString()).apply()

    private fun load(): Map<String, String> = runCatching {
        val o = JSONObject(prefs.getString("rules", "{}") ?: "{}")
        o.keys().asSequence().associateWith { o.getString(it) }
    }.getOrDefault(emptyMap())

    companion object {
        /** The host of an address or a bare site name, lower-case and without "www.". Null when there is none. */
        fun hostOf(input: String): String? {
            val text = input.trim().lowercase().removePrefix("https://").removePrefix("http://")
            val host = text.substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':').removePrefix("www.")
            return host.takeIf { it.contains('.') && !it.contains(' ') && it.length > 3 }
        }

        /** The profile for [url]: the rule for its host, else for the nearest parent domain. */
        fun match(rules: Map<String, String>, url: String): String? {
            var host = hostOf(url) ?: return null
            while (true) {
                rules[host]?.let { return it }
                host = host.substringAfter('.', "")
                if (!host.contains('.')) return null
            }
        }
    }
}
