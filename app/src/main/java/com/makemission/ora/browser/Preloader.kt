package com.makemission.ora.browser

import android.content.Context
import android.net.ConnectivityManager
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.settings.PreloadMode
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Gets a head start on the site you are most likely to open next. While you type in the address bar, the
 * top matching site's address is looked up ahead of time, so the page starts loading sooner when you
 * press Go. Android keeps the answer for the whole system, so GeckoView finds it already waiting.
 *
 * It looks up the name, then opens a connection (DNS, TCP and TLS) with a small HEAD request, so the tab that
 * loads the page reuses it. A whole page is fetched only for the top suggestion and only when "Load the top
 * page ahead" is on. Nothing is stored by Ora. It stays out
 * of the way: off when [mode] is Off, on mobile data or with Data Saver on when [mode] is Wi-Fi only, and
 * never for a private tab. Each site is looked up once every few minutes, at most a few at a time.
 */
object Preloader {
    @Volatile var mode: PreloadMode = PreloadMode.WIFI
    /** True when the "Load the top page ahead" setting is on. It still needs [allowedNow] and a non-private tab. */
    @Volatile var loadAhead: Boolean = false
    /** The addresses visited most, best first, kept up to date from the history of the open profile (never a private tab). */
    @Volatile var topSites: List<String> = emptyList()
    private var appContext: Context? = null
    private val warmed = LinkedHashMap<String, Long>()
    private val fetched = LinkedHashMap<String, Long>()
    private val webExecutor: GeckoWebExecutor? by lazy {
        appContext?.let { GeckoWebExecutor(GeckoEngine.runtime(it)) }
    }
    private val executor = ThreadPoolExecutor(
        1, 1, 30, TimeUnit.SECONDS, ArrayBlockingQueue(4),
        { task -> Thread(task, "ora-preload").apply { isDaemon = true } },
        ThreadPoolExecutor.DiscardOldestPolicy(),
    )

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** True when the current connection and the chosen mode allow looking things up ahead of time. */
    fun allowedNow(chosen: PreloadMode = mode): Boolean {
        if (chosen == PreloadMode.OFF) return false
        if (chosen == PreloadMode.ALWAYS) return true
        val cm = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val saverOn = cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        return !cm.isActiveNetworkMetered && !saverOn
    }

    /** Looks up the site behind [target] (a URL or a bare domain) in the background. Does nothing when not allowed. */
    fun warm(target: String, isPrivate: Boolean = false) {
        if (isPrivate || !allowedNow()) return
        val host = hostFor(target) ?: return
        val now = System.currentTimeMillis()
        synchronized(warmed) {
            val last = warmed[host]
            if (last != null && now - last < REPEAT_MS) return
            warmed[host] = now
            while (warmed.size > MAX_REMEMBERED) warmed.remove(warmed.keys.first())
        }
        executor.execute {
            try {
                val started = System.currentTimeMillis()
                InetAddress.getAllByName(host)
                BrowserLog.info("preload", "Looked up ${LogPrivacy.domainOf(host)} ahead of time in ${System.currentTimeMillis() - started} ms")
                connect(host)
            } catch (error: Exception) {
                // Forget it so a later try can run; a failed lookup costs nothing.
                synchronized(warmed) { warmed.remove(host) }
                BrowserLog.warn("preload", "Could not look up ${LogPrivacy.domainOf(host)} ahead of time", error)
            }
        }
    }

    /**
     * Warms the sites you open most, for when the address bar is tapped before anything is typed. [sites] is
     * best first; only the top [TOP_SITES] are used, each looked up and connected to like any other warm-up.
     */
    fun warmTop(sites: List<String>, isPrivate: Boolean = false) {
        if (isPrivate || !allowedNow()) return
        sites.asSequence().mapNotNull { hostFor(it) }.distinct().take(TOP_SITES).forEach { warm(it, isPrivate) }
        BrowserLog.info("preload", "Warmed your most visited sites")
    }

    /**
     * Opens a real connection to [host] through GeckoView's own network stack: a small HEAD request to the
     * site's front door. DNS, the TCP connection and the TLS handshake finish now and are kept for the tab
     * that loads the page, which reuses them. Nothing is stored or shown. Runs on the preload thread.
     */
    private fun connect(host: String) {
        val engine = webExecutor ?: return
        val started = System.currentTimeMillis()
        try {
            val request = WebRequest.Builder("https://$host/").method("HEAD").cacheMode(WebRequest.CACHE_MODE_NO_STORE).build()
            val response = engine.fetch(request, GeckoWebExecutor.FETCH_FLAGS_NONE).poll(CONNECT_TIMEOUT_MS)
            runCatching { response?.body?.close() }
            BrowserLog.info("preload", "Connected to ${LogPrivacy.domainOf(host)} ahead of time in ${System.currentTimeMillis() - started} ms")
        } catch (error: Exception) {
            BrowserLog.warn("preload", "Could not connect to ${LogPrivacy.domainOf(host)} ahead of time", error)
        }
    }

    /**
     * Fetches the page behind [target] so it is in Gecko's cache when it is opened. Off unless [loadAhead] is on.
     * Only secure pages without a query string (those can be searches or actions), never in a private tab, and
     * only the first [MAX_PREFETCH_BYTES] are read. Each page is fetched once every few minutes.
     */
    fun prefetch(target: String, isPrivate: Boolean = false) {
        if (isPrivate || !loadAhead || !allowedNow()) return
        val host = hostFor(target) ?: return
        val url = prefetchUrl(target, host) ?: return
        val now = System.currentTimeMillis()
        synchronized(fetched) {
            val last = fetched[url]
            if (last != null && now - last < REPEAT_MS) return
            fetched[url] = now
            while (fetched.size > MAX_REMEMBERED) fetched.remove(fetched.keys.first())
        }
        executor.execute {
            val engine = webExecutor ?: return@execute
            val started = System.currentTimeMillis()
            try {
                val request = WebRequest.Builder(url).method("GET").build()
                val response = engine.fetch(request, GeckoWebExecutor.FETCH_FLAGS_NONE).poll(PREFETCH_TIMEOUT_MS)
                val type = response?.headers?.entries?.firstOrNull { it.key.equals("content-type", true) }?.value.orEmpty()
                if (response == null || response.statusCode !in 200..299 || !type.contains("html", true)) {
                    runCatching { response?.body?.close() }
                    return@execute
                }
                var read = 0
                response.body?.use { body ->
                    val buffer = ByteArray(8192)
                    while (read < MAX_PREFETCH_BYTES) {
                        val n = body.read(buffer)
                        if (n < 0) break
                        read += n
                    }
                }
                BrowserLog.info("preload", "Loaded ${LogPrivacy.domainOf(host)} ahead of time ($read bytes, ${System.currentTimeMillis() - started} ms)")
            } catch (error: Exception) {
                synchronized(fetched) { fetched.remove(url) }
                BrowserLog.warn("preload", "Could not load ${LogPrivacy.domainOf(host)} ahead of time", error)
            }
        }
    }

    /** The https address to fetch for [target], or null when it is not safe to fetch without being asked. */
    internal fun prefetchUrl(target: String, host: String): String? {
        val text = target.trim()
        val withScheme = if (text.contains("://")) text else "https://$text"
        if (!withScheme.startsWith("https://", ignoreCase = true) && !withScheme.startsWith("http://", ignoreCase = true)) return null
        val uri = try { URI(withScheme) } catch (_: Exception) { return null }
        if (uri.rawQuery != null || uri.rawFragment != null || uri.userInfo != null) return null
        val path = uri.rawPath.orEmpty().ifEmpty { "/" }
        // Paths that act on something (logout, delete, confirm, unsubscribe) must never be opened by a guess.
        val lower = path.lowercase()
        if (ACTION_WORDS.any { lower.contains(it) }) return null
        return "https://$host$path"
    }

    /** The public host name in [target], or null for search words, local addresses and anything that is not a site. */
    fun hostFor(target: String): String? {
        val text = target.trim()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val withScheme = if (text.contains("://")) text else "https://$text"
        val host = try { URI(withScheme).host } catch (_: Exception) { null } ?: return null
        val lower = host.lowercase()
        if (!lower.contains('.') || lower.endsWith(".local") || lower.endsWith(".localhost") || lower.endsWith(".onion")) return null
        // Plain IP addresses (a router, a dev server) are not worth warming.
        if (lower.all { it.isDigit() || it == '.' } || lower.contains(':')) return null
        return lower
    }

    private val ACTION_WORDS = listOf("logout", "signout", "sign-out", "delete", "remove", "confirm", "unsubscribe", "checkout", "pay")
    private const val TOP_SITES = 3
    private const val CONNECT_TIMEOUT_MS = 8_000L
    private const val PREFETCH_TIMEOUT_MS = 12_000L
    private const val MAX_PREFETCH_BYTES = 512 * 1024
    private const val REPEAT_MS = 5 * 60 * 1000L
    private const val MAX_REMEMBERED = 64
}
