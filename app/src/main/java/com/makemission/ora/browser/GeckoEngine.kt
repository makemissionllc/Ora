package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.extensions.ReaderBridge
import com.makemission.ora.extensions.VideoControlBridge
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.passwords.PasswordStore
import com.makemission.ora.passwords.PasswordStorageDelegate
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File
import java.io.IOException

/**
 * GeckoView needs exactly one [GeckoRuntime] per process, so it is shared here
 * and every browser session is opened against it.
 */
object GeckoEngine {
    private var runtime: GeckoRuntime? = null
    private var restInstalled = false

    /**
     * The helpers that are only used on request (Reader view, the full-screen video bar, the scam-pattern lab)
     * are installed shortly after start-up instead of during it, so the first page does not wait for them.
     */
    fun installRest(context: Context) {
        if (restInstalled) return
        restInstalled = true
        val rt = runtime(context)
        VideoControlBridge.install(rt)
        ReaderBridge.install(rt)
        com.makemission.ora.extensions.SiteScanBridge.install(rt)
        BrowserLog.info("app", "Reader, video and scan helpers installed after start-up")
    }

    fun runtime(context: Context): GeckoRuntime =
        runtime ?: createRuntime(context.applicationContext).also {
            runtime = it
            // Saved logins: GeckoView asks this delegate for logins to fill and hands back submitted ones.
            PasswordStore.init(context.applicationContext)
            it.setAutocompleteStorageDelegate(PasswordStorageDelegate)
            // Helpers that change what a page looks like are installed first, so the first page already has them.
            com.makemission.ora.extensions.WebFontBridge.install(it)
            com.makemission.ora.extensions.ZapperBridge.install(it)
            com.makemission.ora.extensions.SiteTweaksBridge.install(it)
            BrowserLog.info("app", "GeckoView ready ${android.os.SystemClock.elapsedRealtime() - android.os.Process.getStartElapsedRealtime()} ms after the process started")
        }

    /**
     * Starts GeckoView with Gecko's own pop-up blocker switched off. Gecko drops a window.open() that no tap
     * caused before GeckoView reports it, so the pop-up card never got a chance to show. With the blocker off,
     * every pop-up reaches the navigation delegate in [BrowserTab], which opens it, asks, or blocks it.
     *
     * GeckoView sets preferences at start-up only through a config file, so the setting is written to app
     * storage and passed as the config file path. If the file cannot be written, the runtime starts with its
     * default settings, the no-tap card will not appear, and the failure is logged.
     */
    private fun createRuntime(context: Context): GeckoRuntime {
        Preloader.init(context)
        val config = writePopupConfig(context) ?: return GeckoRuntime.getDefault(context)
        val settings = GeckoRuntimeSettings.Builder().configFilePath(config.absolutePath).build()
        return GeckoRuntime.create(context, settings)
    }

    private fun writePopupConfig(context: Context): File? =
        try {
            File(context.filesDir, "gecko-config.yaml").also { it.writeText(configText(context)) }
        } catch (error: IOException) {
            BrowserLog.warn("popup", "Could not write the GeckoView config, so no-tap pop-ups may be dropped", error)
            null
        }

    /**
     * The start-up preferences. When preloading is allowed at start-up, Gecko's own speculative connections and
     * DNS prefetch are switched on too, so it opens connections to sites it has learned you visit. Changing the
     * Preloading setting takes full effect the next time Ora starts.
     */
    private fun configText(context: Context): String {
        val mode = com.makemission.ora.settings.SettingsRepository(context).preloadMode.value
        Preloader.mode = mode
        val ahead = Preloader.allowedNow(mode)
        BrowserLog.info("preload", "Preloading ${mode.label}; Gecko speculative connections ${if (ahead) "on" else "off"}")
        val extra = (if (ahead) PRELOAD_PREFS else "") + labPrefs(context) + strongPrivatePrefs(context)
        return "prefs:\n  dom.disable_open_during_load: false\n$extra" + "env: {}\nargs: []\n"
    }

    /**
     * Labs that Gecko only reads at start-up. Reduced referrer trims every referrer to the site's origin (2) and sends
     * one to another site only when the base domains match (1). The choice was saved before this runs, so a change takes
     * effect the next time Ora starts.
     */
    private fun labPrefs(context: Context): String {
        val on = com.makemission.ora.settings.SettingsRepository(context).labs.value
        val referrer = com.makemission.ora.labs.LabFeature.REDUCED_REFERRER in on
        BrowserLog.info("labs", "Start-up labs: reduced referrer ${if (referrer) "on" else "off"}")
        return if (referrer) "  network.http.referer.XOriginPolicy: 1\n  network.http.referer.trimmingPolicy: 2\n  network.http.referer.XOriginTrimmingPolicy: 2\n" else ""
    }

    /**
     * Strongest private tabs, start-up part: Gecko's full fingerprint resistance for private windows only
     * (pbmode). Sites then see English (US) as the language, UTC as the time zone, a rounded window size and
     * no real hardware details. Normal tabs are not touched. Takes effect the next time Ora starts.
     */
    private fun strongPrivatePrefs(context: Context): String {
        val on = com.makemission.ora.settings.SettingsRepository(context).strongPrivateTabs.value
        BrowserLog.info("privacy", "Start-up: private-tab fingerprint resistance ${if (on) "on" else "off"}")
        return if (on) "  privacy.resistFingerprinting.pbmode: true\n  privacy.spoof_english: 2\n  privacy.resistFingerprinting.letterboxing: false\n" else ""
    }

    private const val PRELOAD_PREFS = "  network.predictor.enabled: true\n" +
        "  network.http.speculative-parallel-limit: 6\n" +
        "  network.dns.disablePrefetch: false\n" +
        "  network.dns.disablePrefetchFromHTTPS: false\n" +
        "  network.early-hints.enabled: true\n" +
        "  network.early-hints.preconnect.enabled: true\n" +
        "  network.dnsCacheEntries: 800\n"
}
