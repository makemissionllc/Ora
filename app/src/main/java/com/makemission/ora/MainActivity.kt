package com.makemission.ora

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.makemission.ora.browser.BrowserScreen
import com.makemission.ora.browser.ExternalLinks
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.security.AppLock
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraTheme

class MainActivity : ComponentActivity() {
    private val settings by lazy { SettingsRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserLog.installCrashHandler()
        BrowserLog.info("app", "Started")
        UsageStats.init(applicationContext)
        AppLock.init(settings)
        handleLink(intent)
        enableEdgeToEdge()
        setContent {
            OraTheme {
                val profiles = remember { ProfilesRepository(applicationContext) }
                BrowserScreen(settings = settings, profiles = profiles)
            }
        }
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
        if (uri.scheme != "http" && uri.scheme != "https") return
        BrowserLog.info("app", "Opened a link from another app (${uri.host ?: "unknown host"})")
        ExternalLinks.offer(uri.toString())
    }
}
