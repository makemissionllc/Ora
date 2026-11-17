package com.makemission.ora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.browser.BrowserScreen
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BrowserLog.installCrashHandler()
        BrowserLog.info("app", "Started")
        UsageStats.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            OraTheme {
                val settings = remember { SettingsRepository(applicationContext) }
                val profiles = remember { ProfilesRepository(applicationContext) }
                BrowserScreen(settings = settings, profiles = profiles)
            }
        }
    }
}
