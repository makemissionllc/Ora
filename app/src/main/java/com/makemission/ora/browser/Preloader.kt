package com.makemission.ora.browser

import android.content.Context
import android.net.ConnectivityManager
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.settings.PreloadMode
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
 * It only ever resolves a name: no page, cookie or content is fetched, and nothing is stored. It stays out
 * of the way: off when [mode] is Off, on mobile data or with Data Saver on when [mode] is Wi-Fi only, and
 * never for a private tab. Each site is looked up once every few minutes, at most a few at a time.
 */
object Preloader {
    @Volatile var mode: PreloadMode = PreloadMode.WIFI
    private var appContext: Context? = null
    private val warmed = LinkedHashMap<String, Long>()
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
            } catch (error: Exception) {
                // Forget it so a later try can run; a failed lookup costs nothing.
                synchronized(warmed) { warmed.remove(host) }
                BrowserLog.warn("preload", "Could not look up ${LogPrivacy.domainOf(host)} ahead of time", error)
            }
        }
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

    private const val REPEAT_MS = 5 * 60 * 1000L
    private const val MAX_REMEMBERED = 64
}
