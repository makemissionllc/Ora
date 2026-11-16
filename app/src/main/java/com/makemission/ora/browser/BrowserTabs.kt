package com.makemission.ora.browser

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.makemission.ora.log.BrowserLog
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebResponse

/** One browser tab: a GeckoSession plus the UI state the toolbar and tab grid read from it. */
class BrowserTab(
    val id: Long,
    context: Context,
    initialUrl: String,
    /** Read on each pop-up request: true blocks it, false opens it as a new tab. */
    private val popupsBlocked: () -> Boolean,
    private val onPopupAllowed: (String) -> Unit,
    /** A page handed over a file to save (a download link or a script-started download). */
    private val onDownload: (WebResponse, String) -> Unit,
) {
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
    /** Link or image under the last long press. The quick menu shows while this is set. */
    var pressTarget by mutableStateOf<PressTarget?>(null)

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
                // GeckoView reports about:blank when it opens; keep the start page address until a real load.
                if (this@BrowserTab.url == BrowserTabs.START_URL && url == "about:blank") return
                url?.let { this@BrowserTab.url = it }
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                if (popupsBlocked()) {
                    BrowserLog.info("popup", "Blocked pop-up from ${this@BrowserTab.url}: $uri")
                } else {
                    BrowserLog.info("popup", "Opened pop-up as a tab: $uri")
                    onPopupAllowed(uri)
                }
                // Returning null keeps the window from opening; the page gets no window back.
                return null
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

            override fun onContextMenu(
                session: GeckoSession,
                screenX: Int,
                screenY: Int,
                element: GeckoSession.ContentDelegate.ContextElement
            ) {
                val link = element.linkUri?.takeIf { it.isNotBlank() }
                val image = element.srcUri?.takeIf { element.type == GeckoSession.ContentDelegate.ContextElement.TYPE_IMAGE && it.isNotBlank() }
                if (link == null && image == null) return
                BrowserLog.info("page", "Long press on ${link ?: image}")
                pressTarget = PressTarget(linkUri = link, imageUri = image, x = screenX, y = screenY)
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                onDownload(response, this@BrowserTab.url)
            }

            override fun onCrash(session: GeckoSession) {
                BrowserLog.error("page", "Renderer crashed for ${this@BrowserTab.url}")
            }
        }
        session.open(GeckoEngine.runtime(context))
        // The start page is drawn natively, so it never goes to GeckoView.
        if (initialUrl != BrowserTabs.START_URL) session.loadUri(initialUrl)
    }

    fun close() = session.close()
}

/** A link and/or image under a long press, with the press position in page pixels. */
data class PressTarget(val linkUri: String?, val imageUri: String?, val x: Int, val y: Int)

/** Owns every open tab and which one is selected. Survives recomposition; created once by the screen. */
class BrowserTabs(
    private val context: Context,
    private val popupsBlocked: () -> Boolean,
    private val onDownload: (WebResponse, String) -> Unit,
) {
    private var nextId = 0L
    val tabs = mutableStateListOf<BrowserTab>()

    companion object {
        /** Address of the native start page. New tabs open here. */
        const val START_URL = "ora://start"
    }

    var selectedId by mutableLongStateOf(-1L)
        private set

    init {
        newTab(START_URL)
    }

    val selected: BrowserTab?
        get() = tabs.firstOrNull { it.id == selectedId }

    fun newTab(url: String, select: Boolean = true): BrowserTab {
        val tab = BrowserTab(nextId++, context, url, popupsBlocked, { popup -> newTab(popup) }, onDownload)
        tabs.add(tab)
        if (select) select(tab.id)
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
            newTab(START_URL)
        } else if (selectedId == id) {
            select(tabs[minOf(index, tabs.lastIndex)].id)
        }
    }

    fun closeAll() {
        tabs.forEach { it.close() }
        tabs.clear()
    }
}
