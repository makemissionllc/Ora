package com.makemission.ora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.makemission.ora.bookmarks.BookmarksRepository
import com.makemission.ora.browser.BrowserScreen
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OraTheme {
                val settings = remember { SettingsRepository(applicationContext) }
                val bookmarks = remember { BookmarksRepository(applicationContext) }
                BrowserScreen(settings = settings, bookmarks = bookmarks)
            }
        }
    }
}
