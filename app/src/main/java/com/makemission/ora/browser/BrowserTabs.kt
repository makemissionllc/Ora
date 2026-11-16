package com.makemission.ora.browser

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.makemission.ora.log.BrowserLog
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings

/** One browser tab: a GeckoSession plus the UI state the toolbar and tab grid read from it. */
class BrowserTab(val id: Long, context: Context, initialUrl: String) {
    val session: GeckoSession = GeckoSession()

    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf("")
    /** Last captured picture of the page, shown as the tab's preview in the tab grid. */
    var thumbnail by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    var loading by mutableStateOf(false)
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var desktopSite by mutableStateOf(false)
        private set

    /** Switches this tab between the mobile and desktop user agent, then reloads so the change applies. */
    fun applyDesktopSite(enabled: Boolean) {
        desktopSite = enabled
        session.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.reload()
    }

    init {
        session.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
                hasUserGesture: Boolean
            ) {
                url?.let { this@BrowserTab.url = it }
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                this@BrowserTab.canGoBack = canGoBack
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                this@BrowserTab.canGoForward = canGoForward
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                loading = true
                BrowserLog.info("page", "Loading $url")
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                loading = false
                if (success) {
                    BrowserLog.info("page", "Loaded ${this@BrowserTab.url}")
                } else {
                    BrowserLog.warn("page", "Stopped before finishing ${this@BrowserTab.url}")
                }
            }
        }
        session.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                this@BrowserTab.title = title.orEmpty()
            }

            override fun onCrash(session: GeckoSession) {
                BrowserLog.error("page", "Renderer crashed for ${this@BrowserTab.url}")
            }
        }
        session.open(GeckoEngine.runtime(context))
        session.loadUri(initialUrl)
    }

    fun close() = session.close()
}

/** Owns every open tab and which one is selected. Survives recomposition; created once by the screen. */
class BrowserTabs(private val context: Context, private val homePage: String) {
    private var nextId = 0L
    val tabs = mutableStateListOf<BrowserTab>()

    var selectedId by mutableLongStateOf(-1L)
        private set

    init {
        newTab(homePage)
    }

    val selected: BrowserTab?
        get() = tabs.firstOrNull { it.id == selectedId }

    fun newTab(url: String): BrowserTab {
        val tab = BrowserTab(nextId++, context, url)
        tabs.add(tab)
        select(tab.id)
        BrowserLog.info("tabs", "Opened tab ${tab.id} (${tabs.size} open)")
        return tab
    }

    fun select(id: Long) {
        if (tabs.none { it.id == id }) return
        selectedId = id
    }

    fun close(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        tabs.removeAt(index).close()
        BrowserLog.info("tabs", "Closed tab $id (${tabs.size} open)")
        if (tabs.isEmpty()) {
            newTab(homePage)
        } else if (selectedId == id) {
            select(tabs[minOf(index, tabs.lastIndex)].id)
        }
    }

    fun closeAll() {
        tabs.forEach { it.close() }
        tabs.clear()
    }
}
