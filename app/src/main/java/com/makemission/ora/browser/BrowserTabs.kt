package com.makemission.ora.browser

import android.content.Context
import android.widget.Toast
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
    /** Tablets open tabs with the desktop site, so pages lay out for the larger screen. */
    private val desktopByDefault: Boolean = false,
) {
    val session: GeckoSession = GeckoSession()
    private val appContext: Context = context.applicationContext

    var url by mutableStateOf(initialUrl)
    var title by mutableStateOf("")
    /** Last captured picture of the page, shown as the tab's preview in the tab grid. */
    var thumbnail by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    var loading by mutableStateOf(false)
    /** Load progress from 0 to 100 while a page loads; the address bar shows it as a thin bar. */
    var progress by mutableStateOf(100)
        private set
    /**
     * False while a page is still arriving, so the view can stay transparent and fade in once it is
     * [REVEAL_PROGRESS_PERCENT] loaded. True once it is shown or nothing is loading.
     */
    var pageRevealed by mutableStateOf(true)
        private set
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)
    var desktopSite by mutableStateOf(false)
        private set
    /** Link or image under the last long press. The quick menu shows while this is set. */
    var pressTarget by mutableStateOf<PressTarget?>(null)
    /** True while the bars are folded away so the page gets the room. Scrolling down folds them; scrolling up or reaching the top shows them. */
    var chromeHidden by mutableStateOf(false)
        private set
    var lastScrollY = 0
    /** Scroll distance added up since the last change of direction; decides when the chrome folds. */
    private var foldDistance = 0

    /** Text the user has selected on the page, with its on-screen position. Null when nothing is selected. */
    var selection by mutableStateOf<GeckoSession.SelectionActionDelegate.Selection?>(null)
        private set

    /** Clears the page's selection and its handles; GeckoView then reports the hide, which clears [selection]. */
    fun clearSelection() {
        selection?.unselect()
        selection?.hide()
        selection = null
    }

    /** True when a page was opened from the native start page and the tab has not returned to it yet. */
    var startPageBehind by mutableStateOf(false)
        private set

    /** Back can step through GeckoView history, or return to the native start page once that history runs out. */
    val canStepBack: Boolean
        get() = canGoBack || startPageBehind

    /** Records that the start page is behind the current page, so Back can return to it. */
    fun markStartPageBehind() {
        if (url == BrowserTabs.START_URL) startPageBehind = true
    }

    /** One step back: GeckoView history first, then the native start page. */
    fun stepBack() {
        when {
            canGoBack -> session.goBack()
            startPageBehind -> {
                startPageBehind = false
                url = BrowserTabs.START_URL
                BrowserLog.info("nav", "Back to start page on tab $id")
            }
        }
    }

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
        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                val delta = scrollY - lastScrollY
                lastScrollY = scrollY
                // GeckoView reports small steps (1 to 2 px per event), so add them up in one direction
                // and fold once the total passes the threshold. A change of direction starts over.
                foldDistance = if (delta == 0 || (foldDistance >= 0) == (delta >= 0)) foldDistance + delta else delta
                when {
                    scrollY <= 0 -> {
                        chromeHidden = false
                        foldDistance = 0
                    }
                    foldDistance > SCROLL_FOLD_THRESHOLD_PX -> chromeHidden = true
                    foldDistance < -SCROLL_FOLD_THRESHOLD_PX -> chromeHidden = false
                }
            }
        }
        // Replaces GeckoView's built-in selection bar: the page's text selection drives Ora's own card instead.
        session.selectionActionDelegate = object : GeckoSession.SelectionActionDelegate {
            override fun onShowActionRequest(session: GeckoSession, selection: GeckoSession.SelectionActionDelegate.Selection) {
                if (selection.text.isNullOrBlank()) return
                this@BrowserTab.selection = selection
            }

            override fun onHideAction(session: GeckoSession, hideReason: Int) {
                this@BrowserTab.selection = null
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                loading = true
                progress = 0
                pageRevealed = false
                chromeHidden = false
                lastScrollY = 0
                // A link menu belongs to the page it was opened on; a new page clears it.
                pressTarget = null
                selection = null
                BrowserLog.info("page", "Loading $url")
            }

            override fun onProgressChange(session: GeckoSession, progressPercent: Int) {
                progress = progressPercent.coerceIn(0, 100)
                if (!pageRevealed && progress >= REVEAL_PROGRESS_PERCENT) {
                    pageRevealed = true
                    BrowserLog.info("page", "Showing page at $progress% for ${this@BrowserTab.url}")
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                loading = false
                pageRevealed = true
                // Progress stays where it is: the bar fades out in place instead of sweeping to full first.
                if (success) {
                    BrowserLog.info("page", "Loaded ${this@BrowserTab.url}")
                } else {
                    BrowserLog.warn("page", "Stopped before finishing ${this@BrowserTab.url}")
                    // Quality of life: say so, instead of leaving a half-drawn page with no explanation.
                    Toast.makeText(appContext, "Page didn't finish loading. Tap reload to try again.", Toast.LENGTH_SHORT).show()
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
        // Set the user agent before the session opens so the first page already uses it.
        desktopSite = desktopByDefault
        session.settings.userAgentMode = if (desktopByDefault) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        session.open(GeckoEngine.runtime(context))
        // The start page is drawn natively, so it never goes to GeckoView.
        if (initialUrl != BrowserTabs.START_URL) session.loadUri(initialUrl)
    }

    fun close() = session.close()
}

/** Scroll distance in pixels before the bars fold or reappear, so small jitters do not flicker them. */
private const val SCROLL_FOLD_THRESHOLD_PX = 24

/** Load progress at which a page is faded in. Earlier than this the page is still mostly blank. */
const val REVEAL_PROGRESS_PERCENT = 50

/** A tab that was closed, kept for the undo offer. [id] is the closed tab's id, so each close can be told apart. */
data class ClosedTab(val id: Long, val url: String)

/** A link and/or image under a long press, with the press position in page pixels. */
data class PressTarget(val linkUri: String?, val imageUri: String?, val x: Int, val y: Int)

/** Owns every open tab and which one is selected. Survives recomposition; created once by the screen. */
class BrowserTabs(
    private val context: Context,
    private val popupsBlocked: () -> Boolean,
    private val onDownload: (WebResponse, String) -> Unit,
    private val desktopByDefault: Boolean = false,
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
        val tab = BrowserTab(nextId++, context, url, popupsBlocked, { popup -> newTab(popup) }, onDownload, desktopByDefault)
        tabs.add(tab)
        if (select) select(tab.id)
        BrowserLog.info("tabs", "Opened tab ${tab.id} (${tabs.size} open)")
        return tab
    }

    fun select(id: Long) {
        if (tabs.none { it.id == id }) return
        selectedId = id
    }

    /** The tab closed most recently, kept so the user can undo the close for a few seconds. */
    var recentlyClosed by mutableStateOf<ClosedTab?>(null)
        private set

    /** Reopens the most recently closed tab's page and clears the undo offer. */
    fun reopenLastClosed() {
        val closed = recentlyClosed ?: return
        recentlyClosed = null
        newTab(closed.url)
        BrowserLog.info("tabs", "Reopened closed tab ${closed.id}")
    }

    fun dismissUndo() {
        recentlyClosed = null
    }

    fun close(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        val closing = tabs[index]
        // Start pages have nothing worth bringing back, so they are not offered for undo.
        recentlyClosed = if (closing.url == START_URL) null else ClosedTab(closing.id, closing.url)
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
