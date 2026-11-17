package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.notice.InAppNotices
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebRequestError
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
    /** A private tab runs in GeckoView private mode: no history, cookies or stored logins are kept for it. */
    val isPrivate: Boolean = false,
    /** Session state saved by quick resume. Restores the page's history; null loads the address instead. */
    restoreState: String? = null,
    /** Called with the address and title of a page that finished loading. Private tabs never report. */
    private val onVisited: (String, String) -> Unit = { _, _ -> },
) {
    val session: GeckoSession = if (isPrivate) {
        GeckoSession(GeckoSessionSettings.Builder().usePrivateMode(true).build())
    } else {
        GeckoSession()
    }
    private val appContext: Context = context.applicationContext

    var url by mutableStateOf(initialUrl)
    /** Latest session state from GeckoView, kept so quick resume can save this tab's history. */
    var sessionState: String? = null
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
    /** True once the page has scrolled away from its top. The status bar then fades instead of showing a solid strip. */
    var scrolled by mutableStateOf(false)
        private set
    /** Colour of the page's top row, sampled while the page sits at its top. Null until a page has been sampled. */
    var topColor by mutableStateOf<androidx.compose.ui.graphics.Color?>(null)
    /** Scroll distance added up since the last change of direction; decides when the chrome folds. */
    private var foldDistance = 0

    /** Text the user has selected on the page, with its on-screen position. Null when nothing is selected. */
    var selection by mutableStateOf<GeckoSession.SelectionActionDelegate.Selection?>(null)
        private set

    /** Clears the page's selection and its handles; GeckoView then reports the hide, which clears [selection]. */
    fun clearSelection() {
        // An empty text field offers no unselect action; calling it anyway throws.
        selection?.let {
            if (it.isActionAvailable(GeckoSession.SelectionActionDelegate.ACTION_UNSELECT)) it.unselect()
            if (it.isActionAvailable(GeckoSession.SelectionActionDelegate.ACTION_HIDE)) it.hide()
        }
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

            override fun onLoadError(
                session: GeckoSession,
                uri: String?,
                error: WebRequestError
            ): GeckoResult<String>? {
                val failed = uri.orEmpty()
                BrowserLog.error("page", "Could not load $failed: category ${error.category}, code ${error.code}")
                // Show a plain explanation with a Try again button, in place of GeckoView's blank error page.
                if (failed.isBlank() || failed.startsWith("data:")) return null
                return GeckoResult.fromValue(ErrorPage.uriFor(failed, error))
            }
        }
        session.scrollDelegate = object : GeckoSession.ScrollDelegate {
            override fun onScrollChanged(session: GeckoSession, scrollX: Int, scrollY: Int) {
                val delta = scrollY - lastScrollY
                lastScrollY = scrollY
                scrolled = scrollY > 0
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
                // A long-press on an empty text field has no text, but it is still worth a card: it offers Auto-fill.
                val inField = selection.isActionAvailable(GeckoSession.SelectionActionDelegate.ACTION_PASTE)
                if (selection.text.isNullOrBlank() && !inField) return
                this@BrowserTab.selection = selection
            }

            override fun onHideAction(session: GeckoSession, hideReason: Int) {
                this@BrowserTab.selection = null
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onSessionStateChange(session: GeckoSession, state: GeckoSession.SessionState) {
                sessionState = state.toString()
            }

            override fun onPageStart(session: GeckoSession, url: String) {
                loading = true
                progress = 0
                pageRevealed = false
                chromeHidden = false
                lastScrollY = 0
                scrolled = false
                // The new page's colour is sampled once it stops loading; the old one must not linger.
                topColor = null
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
                    if (!isPrivate) onVisited(this@BrowserTab.url, this@BrowserTab.title)
                } else {
                    BrowserLog.warn("page", "Stopped before finishing ${this@BrowserTab.url}")
                    // Quality of life: say so, instead of leaving a half-drawn page with no explanation.
                    InAppNotices.show("Page didn't finish loading. Tap reload to try again.")
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
        if (initialUrl != BrowserTabs.START_URL) {
            val restored = restoreState != null && restoreHistory(restoreState)
            if (!restored) session.loadUri(initialUrl)
        }
    }

    /** Puts a saved history back into the session. Returns false if the saved state cannot be read. */
    private fun restoreHistory(state: String): Boolean = try {
        val parsed = GeckoSession.SessionState.fromString(state)
        if (parsed == null) {
            BrowserLog.warn("session", "Saved history for $url is not valid. Loading the address instead.")
            false
        } else {
            session.restoreState(parsed)
            BrowserLog.info("session", "Restored history for $url")
            true
        }
    } catch (e: Exception) {
        BrowserLog.error("session", "Could not restore the history for $url. Loading the address instead.", e)
        false
    }

    fun close() = session.close()
}

/** Scroll distance in pixels before the bars fold or reappear, so small jitters do not flicker them. */
private const val SCROLL_FOLD_THRESHOLD_PX = 24

/** Load progress at which a page is faded in. Earlier than this the page is still mostly blank. */
const val REVEAL_PROGRESS_PERCENT = 50

/**
 * A tab that was closed, kept for the undo offer and the menu's "Reopen closed tab". [id] is the closed tab's id,
 * [state] its saved back and forward history (null if none was saved), so a reopened tab comes back as it was.
 */
data class ClosedTab(val id: Long, val url: String, val title: String, val state: String?)

/** A link and/or image under a long press, with the press position in page pixels. */
data class PressTarget(val linkUri: String?, val imageUri: String?, val x: Int, val y: Int)

/** Owns every open tab and which one is selected. Survives recomposition; created once by the screen. */
class BrowserTabs(
    private val context: Context,
    private val popupsBlocked: () -> Boolean,
    private val onDownload: (WebResponse, String) -> Unit,
    private val desktopByDefault: Boolean = false,
    /** Tabs saved by quick resume, reopened in place of a single start page. Null starts fresh. */
    private val restore: SessionStore.Saved? = null,
    /** Receives each page a tab finishes loading, for the profile's history. */
    private val onVisited: (String, String) -> Unit = { _, _ -> },
) {
    private var nextId = 0L
    val tabs = mutableStateListOf<BrowserTab>()

    companion object {
        /** Address of the native start page. New tabs open here. */
        const val START_URL = "ora://start"

        /** How many closed tabs "Reopen closed tab" can bring back. Older ones are dropped. */
        const val MAX_CLOSED_HISTORY = 10
    }

    var selectedId by mutableLongStateOf(-1L)
        private set

    init {
        val saved = restore
        if (saved != null && saved.tabs.isNotEmpty()) {
            val reopened = saved.tabs.map { newTab(it.url, select = false, restoreState = it.state) }
            select(reopened[saved.selected.coerceIn(0, reopened.lastIndex)].id)
            BrowserLog.info("session", "Quick resume reopened ${reopened.size} tabs")
        } else {
            newTab(START_URL)
        }
    }

    /**
     * The addresses of the tabs quick resume may keep (private tabs are left out), and the position
     * of the selected tab among them. A private selection falls back to the first tab.
     */
    fun savableSnapshot(): SessionStore.Saved {
        val kept = tabs.filterNot { it.isPrivate }
        val selectedIndex = kept.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        return SessionStore.Saved(kept.map { SessionStore.SavedTab(it.url, it.sessionState) }, selectedIndex)
    }

    val selected: BrowserTab?
        get() = tabs.firstOrNull { it.id == selectedId }

    fun newTab(url: String, select: Boolean = true, isPrivate: Boolean = false, restoreState: String? = null): BrowserTab {
        val tab = BrowserTab(nextId++, context, url, popupsBlocked, { popup -> newTab(popup) }, onDownload, desktopByDefault, isPrivate, restoreState, onVisited)
        tabs.add(tab)
        if (select) select(tab.id)
        // Private tabs log no address, so the log does not record what was browsed privately.
        if (isPrivate) BrowserLog.info("tabs", "Opened private tab ${tab.id} (${tabs.size} open)")
        else BrowserLog.info("tabs", "Opened tab ${tab.id} (${tabs.size} open)")
        return tab
    }

    fun select(id: Long) {
        if (tabs.none { it.id == id }) return
        selectedId = id
    }

    /** The tab closed most recently, kept so the user can undo the close for a few seconds. */
    var recentlyClosed by mutableStateOf<ClosedTab?>(null)
        private set

    /** Closed tabs, most recent last, for "Reopen closed tab". Kept for this run only, not written to disk. */
    private val closedHistory = mutableStateListOf<ClosedTab>()

    /** Title of the tab that "Reopen closed tab" would bring back, or null when nothing was closed. */
    val lastClosedTitle: String?
        get() = closedHistory.lastOrNull()?.title

    /**
     * Reopens the most recently closed tab with its history, and clears the undo offer if it is showing that tab.
     * Returns false when there is nothing to reopen.
     */
    fun reopenLastClosed(): Boolean {
        val closed = closedHistory.removeLastOrNull() ?: return false
        if (recentlyClosed?.id == closed.id) recentlyClosed = null
        newTab(closed.url, restoreState = closed.state)
        BrowserLog.info("tabs", "Reopened closed tab ${closed.id}")
        return true
    }

    fun dismissUndo() {
        recentlyClosed = null
    }

    fun close(id: Long) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        UsageStats.count("tab.close")
        val closing = tabs[index]
        // Start pages have nothing worth bringing back, and a private tab must not come back with its address,
        // so neither is offered for undo or kept for "Reopen closed tab".
        val closed = if (closing.url == START_URL || closing.isPrivate) {
            null
        } else {
            ClosedTab(closing.id, closing.url, closing.title, closing.sessionState)
        }
        recentlyClosed = closed
        if (closed != null) {
            closedHistory.add(closed)
            if (closedHistory.size > MAX_CLOSED_HISTORY) closedHistory.removeAt(0)
        }
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
