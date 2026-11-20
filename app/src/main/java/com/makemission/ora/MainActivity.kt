package com.makemission.ora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        setContent {
            val font by settings.font.collectAsState()
            val webFont by settings.webFont.collectAsState()
            val preloadMode by settings.preloadMode.collectAsState()
            androidx.compose.runtime.LaunchedEffect(preloadMode) { com.makemission.ora.browser.Preloader.mode = preloadMode }
            androidx.compose.runtime.LaunchedEffect(font, webFont) {
                com.makemission.ora.extensions.WebFontBridge.apply(if (webFont) font.key else null)
            }
            OraTheme(font = font) {
              androidx.compose.runtime.CompositionLocalProvider(com.makemission.ora.browser.LocalHints provides settings) {
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

    override fun onDestroy() {
        // Says why a window went away, so a double start shows its cause in the log.
        BrowserLog.info("app", "Window destroyed (finishing=$isFinishing, config change=$isChangingConfigurations)")
        super.onDestroy()
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
