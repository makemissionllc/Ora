package com.makemission.ora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.makemission.ora.browser.BrowserScreen
import com.makemission.ora.browser.ExternalLinks
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.security.AppLock
import com.makemission.ora.security.LockScreen
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraTheme

class MainActivity : ComponentActivity() {
    private companion object {
        /** Set once per process by the first onCreate. */
        var processStarted = false
    }

    private val settings by lazy { SettingsRepository(applicationContext) }

    override fun onResume() {
        super.onResume()
        // Follow the system's "Remove animations": with the animation scale at 0, Ora's transitions stop moving.
        val off = android.provider.Settings.Global.getFloat(contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        if (off != com.makemission.ora.ui.theme.OraMotion.reduced) {
            com.makemission.ora.ui.theme.OraMotion.reduced = off
            BrowserLog.info("settings", if (off) "System animations are off: Ora's transitions are reduced" else "System animations are on")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.makemission.ora.browser.FilePicker.register(this)
        com.makemission.ora.browser.ThumbnailStore.init(applicationContext)
        // Startup work runs once per process. If Android recreates the activity (a setting changed, memory
        // pressure) the second onCreate must not install the crash handler again, log "Started" again or
        // lock the app again; it is logged as a recreation so the cause shows in the log.
        if (!processStarted) {
            processStarted = true
            BrowserLog.init(applicationContext)
            BrowserLog.installCrashHandler()
            BrowserLog.info("app", "Started")
            UsageStats.init(applicationContext)
            AppLock.init(settings)
        } else {
            BrowserLog.info("app", "Window recreated (saved state ${if (savedInstanceState != null) "present" else "absent"}); startup not repeated")
        }
        handleLink(intent)
        enableEdgeToEdge()
        // Applies the Smooth scrolling setting now and whenever it changes.
        lifecycleScope.launch { settings.smoothScrolling.collect { applyRefreshRate(it) } }
        setContent {
            val font by settings.font.collectAsState()
            val webFont by settings.webFont.collectAsState()
            val searchSuggestions by settings.searchSuggestions.collectAsState()
            val defaultSearchEngine by settings.defaultSearchEngine.collectAsState()
            androidx.compose.runtime.LaunchedEffect(searchSuggestions, defaultSearchEngine) {
                com.makemission.ora.browser.SearchSuggest.engine = if (searchSuggestions) defaultSearchEngine else null
            }
            val preloadMode by settings.preloadMode.collectAsState()
            val loadAhead by settings.loadAhead.collectAsState()
            androidx.compose.runtime.LaunchedEffect(loadAhead) { com.makemission.ora.browser.Preloader.loadAhead = loadAhead }
            androidx.compose.runtime.LaunchedEffect(preloadMode) { com.makemission.ora.browser.Preloader.mode = preloadMode }
            androidx.compose.runtime.LaunchedEffect(font, webFont) {
                com.makemission.ora.extensions.WebFontBridge.apply(if (webFont) font.key else null)
            }
            OraTheme(font = font) {
              val hapticStrength = settings.hapticStrength.collectAsState()
              val oraHaptics = com.makemission.ora.ui.rememberOraHaptics(hapticStrength)
              androidx.compose.runtime.CompositionLocalProvider(
                  com.makemission.ora.browser.LocalHints provides settings,
                  com.makemission.ora.ui.LocalOraHaptics provides oraHaptics,
              ) {
                val profiles = remember { ProfilesRepository(applicationContext) }
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier.pointerInput(Unit) {
                        // Notes where each press lands, without consuming it, so a dialog can grow out of that point.
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                if (event.type == PointerEventType.Press) event.changes.firstOrNull()?.let { com.makemission.ora.ui.LastPress.position = it.position }
                            }
                        }
                    }
                ) { BrowserScreen(settings = settings, profiles = profiles) }
              }
            }
        }
    }

    /**
     * Android keeps an app on a modest refresh rate (often 60 or 90 Hz) unless it asks for more, which makes
     * scrolling look choppy on a 120 to 165 Hz screen. This asks for the fastest mode that has the screen's
     * current size, or lets the system choose again (mode 0) when the setting is off.
     */
    private fun applyRefreshRate(fastest: Boolean) {
        val display = display ?: return
        val current = display.mode
        val best = if (fastest) {
            display.supportedModes
                .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
                .maxByOrNull { it.refreshRate }
        } else null
        val attrs = window.attributes
        attrs.preferredDisplayModeId = best?.modeId ?: 0
        window.attributes = attrs
        BrowserLog.info("app", if (best != null) "Refresh rate: asked for ${best.refreshRate.toInt()} Hz (screen is at ${current.refreshRate.toInt()} Hz)" else "Refresh rate: left to the system")
    }

    override fun onDestroy() {
        // Says why a window went away, so a double start shows its cause in the log.
        BrowserLog.info("app", "Window destroyed (finishing=$isFinishing, config change=$isChangingConfigurations)")
        super.onDestroy()
    }

    /**
     * Keyboard shortcuts run before any view sees the key, so they work wherever the focus is, even on the web page.
     * The browser screen takes the ones it knows; everything else goes on as usual.
     */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        if (com.makemission.ora.browser.KeyShortcuts.dispatch(event)) return true
        return super.dispatchKeyEvent(event)
    }

    // Lock again whenever Ora leaves the screen, so a phone put down mid-browse is not left open.
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) AppLock.lockIfEnabled(settings)
    }

    // Ora is singleTask, so a link opened from another app arrives here while the browser is running.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    /** Takes a web link from another app and hands it to the browser. Only the host is logged. */
    private fun handleLink(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return
        val uri = intent.data ?: return
        if (uri.scheme == "magnet") {
            val link = com.makemission.ora.torrents.MagnetLink.parse(uri.toString())
            if (link == null) BrowserLog.warn("torrents", "Ignored a magnet link from another app: it has no valid hash")
            else {
                BrowserLog.info("torrents", "A magnet link arrived from another app")
                com.makemission.ora.torrents.TorrentOffers.offer(com.makemission.ora.torrents.TorrentOffer.Magnet(link))
            }
            return
        }
        if (intent.type == "application/x-bittorrent" || uri.path?.endsWith(".torrent", ignoreCase = true) == true) {
            try {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readNBytes(5_000_000) }
                if (bytes != null) com.makemission.ora.torrents.TorrentOffers.offer(
                    com.makemission.ora.torrents.TorrentOffer.File(uri.lastPathSegment ?: "file.torrent", bytes)
                )
            } catch (t: Throwable) {
                BrowserLog.error("torrents", "Could not read the .torrent file from another app", t)
            }
            return
        }
        if (uri.scheme != "http" && uri.scheme != "https") {
            BrowserLog.warn("app", "Ignored a link from another app: ${uri.scheme ?: "no"} links are not opened")
            return
        }
        if (uri.host.isNullOrBlank()) {
            BrowserLog.warn("app", "Ignored a link from another app: it has no site address")
            return
        }
        BrowserLog.info("app", "Opened a link from another app (${uri.host ?: "unknown host"})")
        ExternalLinks.offer(uri.toString())
    }
}
