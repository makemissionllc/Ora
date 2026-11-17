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
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserLog.installCrashHandler()
        BrowserLog.info("app", "Started")
        UsageStats.init(applicationContext)
        handleLink(intent)
        enableEdgeToEdge()
        setContent {
            OraTheme {
                val settings = remember { SettingsRepository(applicationContext) }
                val profiles = remember { ProfilesRepository(applicationContext) }
                BrowserScreen(settings = settings, profiles = profiles)
            }
        }
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
