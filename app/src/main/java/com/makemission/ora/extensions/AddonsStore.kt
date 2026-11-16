package com.makemission.ora.extensions

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** One add-on from addons.mozilla.org (Firefox Add-ons), ready to install. */
data class StoreAddon(
    val id: Long,
    val name: String,
    val summary: String,
    val version: String,
    val users: Long,
    val xpiUrl: String,
)

/**
 * Client for the public Firefox Add-ons (AMO) search API. Only sends the search text;
 * no user identifiers or cookies are included in the request.
 */
object AddonsStore {

    private const val SEARCH = "https://addons.mozilla.org/api/v5/addons/search/"

    /** Searches Android extensions. Throws on network or parse failure. */
    suspend fun search(query: String): List<StoreAddon> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL("$SEARCH?q=$encoded&app=android&type=extension&lang=en-US&page_size=25")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Ora-Browser")
        }
        try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                throw IllegalStateException("Store returned ${connection.responseCode}")
            }
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            parse(body)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * AMO returns some text fields as a locale map, e.g. {"en-US": "uBlock Origin"}, even with
     * lang set. Prefer en-US, then fall back to the first value, then to a plain string.
     */
    private fun localized(item: JSONObject, key: String): String {
        val raw = item.opt(key)
        return when (raw) {
            is JSONObject -> raw.optString("en-US").ifBlank {
                raw.keys().asSequence().firstOrNull()?.let { raw.optString(it) }.orEmpty()
            }
            is String -> raw
            else -> ""
        }
    }

    internal fun parse(body: String): List<StoreAddon> {
        val results = JSONObject(body).optJSONArray("results") ?: return emptyList()
        return buildList {
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val version = item.optJSONObject("current_version") ?: continue
                val file = version.optJSONObject("file") ?: continue
                val xpi = file.optString("url").takeIf { it.isNotBlank() } ?: continue
                add(
                    StoreAddon(
                        id = item.optLong("id"),
                        name = localized(item, "name").ifBlank { "Extension" },
                        summary = localized(item, "summary"),
                        version = version.optString("version"),
                        users = item.optLong("average_daily_users"),
                        xpiUrl = xpi,
                    )
                )
            }
        }
    }
}
