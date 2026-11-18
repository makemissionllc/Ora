package com.makemission.ora.browser

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Settings > Open links in apps. Ask is the default. */
enum class AppLinkMode(val key: String, val label: String) {
    ASK("ask", "Ask"),
    ALWAYS("always", "Always"),
    NEVER("never", "Never");

    companion object {
        fun fromKey(key: String?): AppLinkMode = values().firstOrNull { it.key == key } ?: ASK
    }
}

/** An app that can take a link: what to show on the card, and the intent that opens it. */
class AppMatch(
    val packageName: String?,
    /** The app's name for a web link ("YouTube"), or what handles a scheme ("Phone"). */
    val label: String,
    val intent: Intent,
    val web: Boolean,
    /** For an intent: link, the address the page gave to use when the app is missing. */
    val fallbackUrl: String?,
)

/** A link waiting for Stay in Ora, Always or Open. [key] is what Always remembers: a domain, or a scheme such as "tel:". */
data class PendingAppLink(
    val tabId: Long,
    val url: String,
    val label: String,
    val key: String,
    val web: Boolean,
    val fromPrivate: Boolean,
    val fallbackUrl: String?,
)

/** Finds apps for links and opens them. Nothing here touches the page; callers decide when to use it. */
object AppLinks {
    /** Addresses Ora loads itself or that never leave it. Everything else with a scheme is a link to an app. */
    private val OWN_SCHEMES = setOf(
        "http", "https", "about", "ora", "data", "blob", "javascript", "file", "content",
        "view-source", "resource", "moz-extension", "chrome", "ws", "wss", "",
    )

    fun schemeOf(uri: String): String = uri.substringBefore(':', "").lowercase()

    fun isWeb(uri: String): Boolean = schemeOf(uri).let { it == "http" || it == "https" }

    /** True for tel:, mailto:, market:, intent: and other schemes that only an app can open. */
    fun isAppScheme(uri: String): Boolean = schemeOf(uri) !in OWN_SCHEMES && uri.contains(':')

    /**
     * The first non-browser app that handles this web address. An app that also handles a made-up address
     * (https://example.invalid) takes every link, so it is a browser and is left out. Ora is left out too.
     */
    fun findForWeb(context: Context, uri: String): AppMatch? = try {
        val pm = context.packageManager
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).addCategory(Intent.CATEGORY_BROWSABLE)
        val browsers = pm.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://example.invalid")).addCategory(Intent.CATEGORY_BROWSABLE),
            PackageManager.ResolveInfoFlags.of(0),
        ).map { it.activityInfo.packageName }.toSet()
        pm.queryIntentActivities(view, PackageManager.ResolveInfoFlags.of(0))
            .firstOrNull { it.activityInfo.packageName !in browsers && it.activityInfo.packageName != context.packageName }
            ?.let { AppMatch(it.activityInfo.packageName, it.activityInfo.applicationInfo.loadLabel(pm).toString(), view, web = true, fallbackUrl = null) }
    } catch (e: Exception) {
        BrowserLog.warn("applinks", "Could not look for an app for a link", e)
        null
    }

    /**
     * What opens a link that is not a web address. An intent: link is read with its fallback address, and
     * is cut down to a plain VIEW with no component or selector, so a page cannot aim it at one screen of an app.
     */
    fun findForScheme(context: Context, uri: String): AppMatch? = try {
        val pm = context.packageManager
        if (schemeOf(uri) == "intent") {
            val parsed = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
            val action = parsed.action ?: Intent.ACTION_VIEW
            val data = parsed.data
            if (action != Intent.ACTION_VIEW || data?.scheme == "file" || data?.scheme == "content") null
            else {
                parsed.component = null
                parsed.selector = null
                parsed.addCategory(Intent.CATEGORY_BROWSABLE)
                val pkg = parsed.`package`
                val label = pkg?.let { runCatching { pm.getApplicationInfo(it, 0).loadLabel(pm).toString() }.getOrNull() }
                AppMatch(pkg, label ?: "another app", parsed, web = false, fallbackUrl = parsed.getStringExtra("browser_fallback_url"))
            }
        } else {
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            pm.resolveActivity(view, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
                ?.let {
                    val pkg = it.activityInfo.packageName
                    // "android" is the system chooser, several apps share the scheme: name the kind of app instead.
                    val label = if (pkg == "android") when (schemeOf(uri)) {
                        "tel" -> "Phone"; "mailto" -> "Email"; "sms", "smsto" -> "Messages"; "geo" -> "Maps"; else -> "an app"
                    } else it.activityInfo.applicationInfo.loadLabel(pm).toString()
                    AppMatch(pkg, label, view, web = false, fallbackUrl = null)
                }
        }
    } catch (e: Exception) {
        BrowserLog.warn("applinks", "Could not read a link to an app", e)
        null
    }

    /** Opens the app. A web address is sent with FLAG_ACTIVITY_REQUIRE_NON_BROWSER, so it can never land in a browser. */
    fun open(context: Context, match: AppMatch): Boolean {
        val intent = Intent(match.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (match.web) intent.addFlags(Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            BrowserLog.warn("applinks", "No app could open the link", e)
            false
        } catch (e: SecurityException) {
            BrowserLog.warn("applinks", "Android refused to open the app", e)
            false
        }
    }
}

/**
 * The sites (and schemes such as "tel:") this profile always opens in an app without asking, kept per
 * profile in `ora_app_links_<profileId>`. Private tabs never read or write it.
 */
class AppLinkStore private constructor(context: Context, profileId: String) {

    private val prefs = context.applicationContext.getSharedPreferences("ora_app_links_$profileId", Context.MODE_PRIVATE)
    private val _always = MutableStateFlow(prefs.getStringSet(KEY_ALWAYS, emptySet()).orEmpty().toSet())

    /** Keys that open in an app without asking. */
    val always: StateFlow<Set<String>> = _always.asStateFlow()

    fun isAlways(key: String): Boolean = key in _always.value

    fun add(key: String) {
        if (key in _always.value || key == "unknown") return
        _always.value = _always.value + key
        save()
        BrowserLog.info("applinks", "Always open $key in its app")
    }

    fun remove(key: String) {
        if (key !in _always.value) return
        _always.value = _always.value - key
        save()
        BrowserLog.info("applinks", "Stopped opening $key in its app automatically")
    }

    private fun save() {
        prefs.edit().putStringSet(KEY_ALWAYS, _always.value.toSet()).apply()
    }

    companion object {
        private const val KEY_ALWAYS = "always"
        private val stores = mutableMapOf<String, AppLinkStore>()

        @Synchronized
        fun forProfile(context: Context, profileId: String): AppLinkStore =
            stores.getOrPut(profileId) { AppLinkStore(context, profileId) }
    }
}
