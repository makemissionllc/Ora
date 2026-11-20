package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.security.AppLock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import org.mozilla.geckoview.AllowOrDeny
import com.makemission.ora.log.UsageStats
import com.makemission.ora.permissions.SitePermissionDelegate
import com.makemission.ora.permissions.SitePermissionStore
import com.makemission.ora.permissions.SitePrompts
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

/** A pop-up waiting for the card's answer: its address, the page it came from, and whether that page is private. */
data class PendingPopup(val url: String, val fromSite: String, val fromPrivate: Boolean)

/** A GeckoView session for a tab: private mode for a private tab, normal otherwise. */
internal fun newSession(isPrivate: Boolean): GeckoSession =
    if (isPrivate) GeckoSession(GeckoSessionSettings.Builder().usePrivateMode(true).build()) else GeckoSession()

/** One browser tab: a GeckoSession plus the UI state the toolbar and tab grid read from it. */
class BrowserTab(
    val id: Long,
    context: Context,
    initialUrl: String,
    /** Read on each pop-up request: true blocks it, false opens it as a new tab. */
    private val popupsBlocked: () -> Boolean,
    /** A pop-up to open as a tab. The session is the one GeckoView will load the pop-up's page into. */
    private val onPopupAllowed: (String, GeckoSession) -> Unit,
    /** A page handed over a file to save (a download link or a script-started download). */
    private val onDownload: (WebResponse, String, Boolean) -> Unit,
    /** Tablets open tabs with the desktop site, so pages lay out for the larger screen. */
    private val desktopByDefault: Boolean = false,
    /** A private tab runs in GeckoView private mode: no history, cookies or stored logins are kept for it. */
    val isPrivate: Boolean = false,
    /** Session state saved by quick resume. Restores the page's history; null loads the address instead. */
    restoreState: String? = null,
    /** Called with the address and title of a page that finished loading. Private tabs never report. */
    private val onVisited: (String, String) -> Unit = { _, _ -> },
    /** The profile's saved answers to site permission prompts. Private tabs read and write none of it. */
    private val sitePermissions: SitePermissionStore,
    /** True for a tab reopened by quick resume. It is not saved again until its page has loaded (or [RESTORE_GRACE_MS] passed). */
    restoring: Boolean = false,
    /** A reopened background tab waits: it loads when it is first selected, so start-up loads one page, not nine. */
    deferLoad: Boolean = false,
    /** Whether a site may open pop-ups without asking: the domain is checked against the profile's allow list. */
    private val siteAllowedForPopups: (String) -> Boolean = { false },
    /** A pop-up with no tap, when the card must ask. Gets the pop-up's address and the page's domain. */
    private val onPopupAsk: (String, String) -> Unit = { _, _ -> },
    /** Decides a top-level navigation: whether it opens an app instead of loading here. Allow is the default. */
    private val onLinkRequest: (BrowserTab, GeckoSession.NavigationDelegate.LoadRequest) -> AllowOrDeny = { _, _ -> AllowOrDeny.ALLOW },
    /** A session already made for a pop-up. GeckoView loads the pop-up into it, so this tab does not load its address itself. */
    adopt: GeckoSession? = null,
) {
    val session: GeckoSession = adopt ?: newSession(isPrivate)
    /** Page and navigation events go here: a private tab logs nothing, so the log never shows private browsing. */
    private val plog = TabLog(isPrivate)
    private val appContext: Context = context.applicationContext

    /** Reader view for this tab's page: whether it has an article and whether the reader is showing. */
    val reader = com.makemission.ora.extensions.ReaderState { url }

    var url by mutableStateOf(initialUrl)
    /** Name of an app that can open this page ("YouTube"), or null. The address capsule shows an "App" chip while it is set. */
    var appLabel by mutableStateOf<String?>(null)
    // Set when a load was denied to hand it to an app, so the stop that follows isn't reported as a failure.
    private var deniedForApp = false
    /** Set by [BrowserTabs]: the "App" chip calls it to ask again about this page. */
    var onAskApp: () -> Unit = {}
    private val createdAt = System.currentTimeMillis()
    private var restorePending = restoring && initialUrl != BrowserTabs.START_URL && !deferLoad
    private var restoreStateText: String? = restoreState
    /** False while a reopened tab is waiting for its first selection. Its saved address and history stay as saved. */
    var loaded = !(deferLoad && initialUrl != BrowserTabs.START_URL)
        private set

    /**
     * True while a reopened tab is still coming back, so its address and history are not yet the saved ones.
     * Saving at this point would write half-restored state over a good save.
     */
    val isRestoring: Boolean
        get() = restorePending && System.currentTimeMillis() - createdAt < RESTORE_GRACE_MS
    /** Latest session state from GeckoView, kept so quick resume can save this tab's history. */
    var sessionState: String? = null
    var title by mutableStateOf("")
    /** The tab group this tab is in, or null. A tab is in at most one group; private tabs are never grouped. */
    var groupId by mutableStateOf<String?>(null)
    /** When the tab was last opened (milliseconds). A tab untouched for 14 days counts as inactive. */
    var lastActive by mutableLongStateOf(System.currentTimeMillis())
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
    /** True while this tab's page has a video in full screen. The bars hide and the gesture layer shows. */
    var videoFullScreen by mutableStateOf(false)
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

    /** Brings the bars back at once, as a tap on the shrunk address bar does. A later scroll folds them again. */
    fun showChrome() {
        chromeHidden = false
        foldDistance = 0
    }

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
                plog.info("nav", "Back to start page on tab $id")
            }
        }
    }

    /** Lab: things Ora noticed about the site on this tab, shown in the site card. Empty when there is nothing to say. */
    var findings by mutableStateOf<List<com.makemission.ora.labs.SiteFinding>>(emptyList())
        private set
    private val dismissedFindings = HashSet<String>()
    private var redirectHops = 0
    private var findingsSite = ""

    /** Adds a finding unless the same one is already listed or was dismissed for this site on this tab. */
    private fun addFinding(f: com.makemission.ora.labs.SiteFinding) {
        val key = popupSiteOf(url) + "|" + f.id
        if (key in dismissedFindings || findings.any { it.id == f.id }) return
        findings = findings + f
        plog.warn("labs", "Site finding for ${popupSiteOf(url)}: ${f.title}")
    }

    /** Replaces the connection findings (ids starting "sec.") with the current set, so a fixed page loses its warning. */
    private fun setSecurityFindings(now: List<com.makemission.ora.labs.SiteFinding>) {
        val kept = findings.filterNot { it.id.startsWith("sec.") }
        findings = kept
        now.forEach { addFinding(it) }
    }

    /** Replaces the page-pattern findings (ids starting "scan.") with those for the page just read. */
    fun setScanFindings(now: List<com.makemission.ora.labs.SiteFinding>) {
        findings = findings.filterNot { it.id.startsWith("scan.") }
        now.forEach { addFinding(it) }
    }

    /** Dismisses the site card, remembering it for this site on this tab. [goBack] also leaves the page. */
    fun answerFindings(goBack: Boolean) {
        val site = popupSiteOf(url)
        findings.forEach { dismissedFindings += site + "|" + it.id }
        plog.info("labs", "Site card for $site: ${if (goBack) "went back" else "dismissed"}")
        findings = emptyList()
        if (goBack) { if (canGoBack) session.goBack() else session.loadUri("about:blank") }
    }

    /** Lab: connection and blocking facts for the page on this tab, shown by the padlock. Null while the lab is off or nothing has loaded. */
    var siteStats by mutableStateOf<com.makemission.ora.labs.SiteStats?>(null)
        private set
    private val blockedKinds = LinkedHashMap<String, Int>()
    private var lastSecurity: GeckoSession.ProgressDelegate.SecurityInformation? = null

    private fun rebuildSiteStats() {
        val info = lastSecurity
        siteStats = if (!com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.SITE_STATS) || info == null) null
        else com.makemission.ora.labs.SiteStats.from(popupSiteOf(url), info.isSecure, info.isException, info.certificate, blockedKinds.toMap())
    }

    /** Counts what Gecko's content blocking stopped on this page, by kind. */
    private fun countBlocked(event: org.mozilla.geckoview.ContentBlocking.BlockEvent) {
        if (!com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.SITE_STATS)) return
        com.makemission.ora.labs.SiteStats.kindsOf(event.antiTrackingCategory, event.cookieBehaviorCategory, event.safeBrowsingCategory)
            .forEach { blockedKinds[it] = (blockedKinds[it] ?: 0) + 1 }
        rebuildSiteStats()
    }

    /** Looks at the connection of a loaded page and lists what a person would want to know. Runs only for labs that are on. */
    private fun inspectSecurity(info: GeckoSession.ProgressDelegate.SecurityInformation) {
        lastSecurity = info
        rebuildSiteStats()
        val labs = com.makemission.ora.labs.Labs
                val found = mutableListOf<com.makemission.ora.labs.SiteFinding>()
        val page = url
        if (labs.on(com.makemission.ora.labs.LabFeature.INSECURE_PAGE) && page.startsWith("http://") && !info.isSecure) {
            found += com.makemission.ora.labs.SiteFinding("sec.http", "This page is not encrypted", "Others on the network can read or change what you send and see here. Avoid typing passwords.")
        }
        if (labs.on(com.makemission.ora.labs.LabFeature.MIXED_CONTENT) && info.isSecure) {
            if (info.mixedModeActive == GeckoSession.ProgressDelegate.SecurityInformation.CONTENT_LOADED) {
                found += com.makemission.ora.labs.SiteFinding("sec.mixed.active", "Insecure scripts on a secure page", "Part of this page's code came over an unencrypted connection, so the padlock cannot be fully trusted.", leave = true)
            } else if (info.mixedModePassive == GeckoSession.ProgressDelegate.SecurityInformation.CONTENT_LOADED) {
                found += com.makemission.ora.labs.SiteFinding("sec.mixed.passive", "Insecure images or media", "Some pictures or video on this page came over an unencrypted connection.")
            }
        }
        if (labs.on(com.makemission.ora.labs.LabFeature.CERT_CHECKS) && info.isSecure) {
            val cert = info.certificate
            if (info.isException) {
                found += com.makemission.ora.labs.SiteFinding("sec.exception", "Certificate exception in place", "A security exception was allowed for this site earlier, so its certificate is not being fully checked.")
            }
            if (cert != null) {
                if (cert.issuerX500Principal == cert.subjectX500Principal) {
                    found += com.makemission.ora.labs.SiteFinding("sec.selfsigned", "Certificate is self-issued", "No outside authority vouches for this site's identity.", leave = true)
                }
                val days = (cert.notAfter.time - System.currentTimeMillis()) / 86_400_000L
                if (days in 0..14) {
                    found += com.makemission.ora.labs.SiteFinding("sec.expiring", "Certificate expires soon", "The site's security certificate runs out in ${days + 1} day${if (days == 0L) "" else "s"}.")
                }
            }
        }
        setSecurityFindings(found)
    }

    /** Lab: applies or removes the generic browser identity. A desktop-site tab keeps the desktop identity. */
    fun applyGenericIdentity() {
        val generic = com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.GENERIC_USER_AGENT) && !desktopSite
        session.settings.userAgentOverride = if (generic) com.makemission.ora.labs.Labs.GENERIC_USER_AGENT else null
    }

    /** Switches this tab between the mobile and desktop user agent, then reloads so the change applies. */
    fun applyDesktopSite(enabled: Boolean) {
        desktopSite = enabled
        session.settings.userAgentMode = if (enabled) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        applyGenericIdentity()
        session.reload()
    }

    /** Labs that watch a navigation: the lookalike check and the redirect-chain note. Never blocks the load. */
    private fun labsOnRequest(request: GeckoSession.NavigationDelegate.LoadRequest) {
        val labs = com.makemission.ora.labs.Labs
                if (request.target != GeckoSession.NavigationDelegate.TARGET_WINDOW_CURRENT) return
        redirectHops = if (request.isRedirect) redirectHops + 1 else 0
        if (redirectHops == com.makemission.ora.labs.Labs.LONG_REDIRECT_CHAIN) {
            if (labs.on(com.makemission.ora.labs.LabFeature.SCENARIO_LOG)) plog.warn("labs", "Scenario: $redirectHops redirects in a row on tab $id, now heading to ${popupSiteOf(request.uri)}")
            if (labs.on(com.makemission.ora.labs.LabFeature.REDIRECT_WARNING)) addFinding(com.makemission.ora.labs.SiteFinding("redirects", "Sent through several addresses", "This page was reached after $redirectHops redirects in a row, which some tracking and scam links use."))
        }
        if (labs.on(com.makemission.ora.labs.LabFeature.LOOKALIKE_WARNING)) {
            com.makemission.ora.labs.LookalikeCheck.inspect(request.uri)?.let {
                addFinding(com.makemission.ora.labs.SiteFinding("lookalike", "This address may be a lookalike", "${it.host}: ${it.reason}", leave = true))
            }
        }
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
                // A tab still waiting to be loaded keeps its saved address too, or the sidebar lists it as about:blank.
                if (!loaded && url == "about:blank") return
                url?.let { this@BrowserTab.url = it }
                // The lookalike card belongs to one address; leaving it for another site takes the card away.
                if (url != null && findings.isNotEmpty() && popupSiteOf(url) != popupSiteOf(findingsSite)) findings = emptyList()
                findingsSite = url ?: findingsSite
            }

            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                labsOnRequest(request)
                return GeckoResult.fromValue(onLinkRequest(this@BrowserTab, request).also { if (it == AllowOrDeny.DENY) deniedForApp = true })
            }

            override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
                val siteDomain = popupSiteOf(this@BrowserTab.url)
                val target = popupSiteOf(uri)
                val fromTap = PopupTaps.tappedRecently()
                if (popupDecision(fromTap, siteAllowedForPopups(siteDomain), popupsBlocked()) == PopupDecision.ASK) {
                    // Nothing is dropped silently: the card asks, and until it is answered no window opens.
                    plog.info("popup", "Pop-up asked from $siteDomain with no tap: $target")
                    onPopupAsk(uri, siteDomain)
                    return null
                }
                // A tap's pop-up, or one the site may open, becomes a tab. The fresh session goes to GeckoView unopened,
                // so a form POST or a download the page starts there reaches the new tab, not a plain GET of its address.
                plog.info("popup", if (fromTap) "Pop-up opened from a tap: $target" else "Pop-up opened for an allowed site: $target")
                val opened = newSession(isPrivate)
                onPopupAllowed(uri, opened)
                return GeckoResult.fromValue(opened)
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
                val host = popupSiteOf(failed)
                // The connectivity check comes first: with no network the card says so, whatever GeckoView reported.
                val online = ErrorPage.isOnline(appContext)
                val message = ErrorPage.messageFor(error.code, error.category, host, online)
                // The plain title and cause the error page shows, so the log and the page say the same thing.
                val connection = if (online) "online" else "offline"
                if (com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.SCENARIO_LOG) &&
                    error.category == WebRequestError.ERROR_CATEGORY_SECURITY) {
                    plog.warn("labs", "Security check failed for $host: ${message.title}. The page was not opened.")
                }
                plog.error("page", "Could not load $failed: ${message.title} (${message.support}, code ${error.code}, $connection)")
                // One card per cause, in place of GeckoView's blank error page.
                if (failed.isBlank() || failed.startsWith("data:")) return null
                return GeckoResult.fromValue(ErrorPage.uriFor(failed, error, host, online))
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
        session.contentBlockingDelegate = object : org.mozilla.geckoview.ContentBlocking.Delegate {
            override fun onContentBlocked(session: GeckoSession, event: org.mozilla.geckoview.ContentBlocking.BlockEvent) {
                countBlocked(event)
            }
        }
        session.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onSessionStateChange(session: GeckoSession, state: GeckoSession.SessionState) {
                // A tab still waiting to load has an empty session; its saved history must not be replaced by that.
                if (loaded) sessionState = state.toString()
            }

            override fun onPageStart(session: GeckoSession, url: String) {
                loading = true
                progress = 0
                blockedKinds.clear()
                lastSecurity = null
                siteStats = null
                pageRevealed = false
                chromeHidden = false
                lastScrollY = 0
                scrolled = false
                // The new page's colour is sampled once it stops loading; the old one must not linger.
                topColor = null
                // A link menu belongs to the page it was opened on; a new page clears it.
                pressTarget = null
                selection = null
                plog.info("page", "Loading $url")
            }

            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
                inspectSecurity(securityInfo)
            }

            override fun onProgressChange(session: GeckoSession, progressPercent: Int) {
                progress = progressPercent.coerceIn(0, 100)
                if (!pageRevealed && progress >= REVEAL_PROGRESS_PERCENT) {
                    pageRevealed = true
                    plog.info("page", "Showing page at $progress% for ${this@BrowserTab.url}")
                }
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                loading = false
                pageRevealed = true
                restorePending = false
                if (deniedForApp) { deniedForApp = false; return }
                // Progress stays where it is: the bar fades out in place instead of sweeping to full first.
                if (success) {
                    plog.info("page", "Loaded ${this@BrowserTab.url}")
                    if (!isPrivate) onVisited(this@BrowserTab.url, this@BrowserTab.title)
                } else {
                    plog.warn("page", "Stopped before finishing ${this@BrowserTab.url}")
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
                plog.info("page", "Long press on ${link ?: image}")
                pressTarget = PressTarget(linkUri = link, imageUri = image, x = screenX, y = screenY)
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                videoFullScreen = fullScreen
                plog.info("page", if (fullScreen) "Full screen requested: ${this@BrowserTab.url}" else "Left full screen")
            }

            override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
                onDownload(response, this@BrowserTab.url, this@BrowserTab.isPrivate)
            }

            override fun onCrash(session: GeckoSession) {
                plog.error("page", "Renderer crashed for ${this@BrowserTab.url}")
                if (com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.SCENARIO_LOG)) {
                    plog.warn("labs", "Scenario: the page process crashed on tab $id; the tab can be reloaded")
                }
            }
        }
        // Location, camera, microphone and notification requests go to the prompt, or to the saved answer for the site.
        session.permissionDelegate = SitePermissionDelegate(id, isPrivate, sitePermissions)
        // Upload buttons and file inputs open the Android file picker.
        session.promptDelegate = PagePromptDelegate(appContext)
        // Set the user agent before the session opens so the first page already uses it.
        desktopSite = desktopByDefault
        session.settings.userAgentMode = if (desktopByDefault) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        applyGenericIdentity()
        // A pop-up session is opened by GeckoView itself, and its page is loaded by GeckoView, not here.
        if (adopt == null) session.open(GeckoEngine.runtime(context))
        // The start page is drawn natively, so it never goes to GeckoView.
        if (loaded && initialUrl != BrowserTabs.START_URL && adopt == null) loadNow()
        // A waiting tab keeps its saved history text, so saving again before it loads loses nothing.
        else if (!loaded) sessionState = restoreState
    }

    private fun loadNow() {
        val state = restoreStateText
        val restored = state != null && restoreHistory(state)
        if (!restored) {
            // A history that cannot be read must not be saved again, or it would stay broken for good.
            sessionState = null
            restoreStateText = null
            session.loadUri(url)
        }
    }

    /** Loads a tab that was waiting. Does nothing if it already loaded. */
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        restorePending = true
        plog.info("session", "Loading tab $id on first open")
        loadNow()
    }

    /** Puts a saved history back into the session. Returns false if the saved state cannot be read. */
    private fun restoreHistory(state: String): Boolean = try {
        val parsed = GeckoSession.SessionState.fromString(state)
        if (parsed == null) {
            plog.warn("session", "Saved history for $url is not valid. Loading the address instead.")
            false
        } else {
            session.restoreState(parsed)
            plog.info("session", "Restored history for $url")
            true
        }
    } catch (e: Exception) {
        plog.error("session", "Could not restore the history for $url. Loading the address instead.", e)
        false
    }

    fun close() {
        // A prompt still waiting for this tab is released as "not now", so nothing is saved for a closed page.
        SitePrompts.cancelFor(id)
        session.close()
    }
}

/** Scroll distance in pixels before the bars fold or reappear, so small jitters do not flicker them. */
private const val SCROLL_FOLD_THRESHOLD_PX = 24

/** How long a reopened tab may take to load before quick resume stops waiting for it and saves anyway. */
const val RESTORE_GRACE_MS = 20_000L

/** Load progress at which a page is faded in. Earlier than this the page is still mostly blank. */
const val REVEAL_PROGRESS_PERCENT = 50

/**
 * A tab that was closed, kept for the undo offer and the menu's "Reopen closed tab". [id] is the closed tab's id,
 * [state] its saved back and forward history (null if none was saved), so a reopened tab comes back as it was.
 */
data class ClosedTab(val id: Long, val url: String, val title: String, val state: String?)

/** A link and/or image under a long press, with the press position in page pixels. */
data class PressTarget(val linkUri: String?, val imageUri: String?, val x: Int, val y: Int)

/** The tabs in one space (normal or private), in their order. Address-bar swipes move only within it. */
internal fun <T> tabsInSpace(items: List<T>, isPrivate: (T) -> Boolean, currentPrivate: Boolean): List<T> =
    items.filter { isPrivate(it) == currentPrivate }

/** Owns every open tab and which one is selected. Survives recomposition; created once by the screen. */
class BrowserTabs(
    private val context: Context,
    /** The three callbacks are replaced by [rebind] when a new window takes over these tabs, so none points at a destroyed window. */
    var popupsBlocked: () -> Boolean,
    var onDownload: (WebResponse, String, Boolean) -> Unit,
    private val desktopByDefault: Boolean = false,
    /** Tabs saved by quick resume, reopened in place of a single start page. Null starts fresh. */
    private val restore: SessionStore.Saved? = null,
    /** Receives each page a tab finishes loading, for the profile's history. */
    var onVisited: (String, String) -> Unit = { _, _ -> },
    /** The profile's saved site permission answers, handed to each tab. */
    private val sitePermissions: SitePermissionStore,
    /** Where this profile's tab groups are kept. Null keeps none. */
    private val groupStore: TabGroupStore? = null,
) {
    private var nextId = 0L
    /** Private tabs have their own ids, counting down from -2 (-1 means "none selected"), so they never share the normal numbering. */
    private var nextPrivateId = -2L
    val tabs = mutableStateListOf<BrowserTab>()
    /** This profile's tab groups, in order. Declared before the first tabs so restored tabs can find theirs. */
    val groups = mutableStateListOf<TabGroup>().also { list -> groupStore?.let { list.addAll(it.load()) } }

    companion object {
        /** Address of the native start page. New tabs open here. */
        const val START_URL = "ora://start"

        /** How many closed tabs "Reopen closed tab" can bring back. Older ones are dropped. */
        const val MAX_CLOSED_HISTORY = 10

        /** Most pop-ups kept waiting for a choice at once. */
        const val MAX_POPUP_QUEUE = 20
    }

    var selectedId by mutableLongStateOf(-1L)
        private set

    /** A new window takes over these tabs: its callbacks replace the destroyed window's. */
    fun rebind(
        popupsBlocked: () -> Boolean,
        onDownload: (WebResponse, String, Boolean) -> Unit,
        onVisited: (String, String) -> Unit,
    ) {
        this.popupsBlocked = popupsBlocked
        this.onDownload = onDownload
        this.onVisited = onVisited
    }

    // Declared before init: the first tab can load, and ask about an app link, while init runs.
    /** Settings > Open links in apps, read on each link. Replaced when a new window takes over. */
    var appLinkMode: () -> AppLinkMode = { AppLinkMode.ASK }

    /** A link the card is asking about. Null when no card is showing. */
    var pendingAppLink by mutableStateOf<PendingAppLink?>(null)

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    init {
        val saved = restore
        if (saved != null && saved.tabs.isNotEmpty()) {
            val reopened = saved.tabs.map { saved -> newTab(saved.url, select = false, restoreState = saved.state, restoring = true, deferLoad = true).also { tab -> tab.groupId = saved.groupId?.takeIf { gid -> groups.any { it.id == gid } }; saved.lastActive?.let { tab.lastActive = it } } }
            select(reopened[saved.selected.coerceIn(0, reopened.lastIndex)].id)
            BrowserLog.info("session", "Quick resume reopened ${reopened.size} tabs")
            // Quality of life: say what came back, so a restored session is never a surprise.
            if (reopened.size > 1) InAppNotices.show("Reopened ${reopened.size} tabs. Each loads when you open it")
        } else {
            newTab(START_URL)
        }
    }

    /**
     * The addresses of the tabs quick resume may keep (private tabs are left out), and the position
     * of the selected tab among them. A private selection falls back to the first tab.
     * Null while any reopened tab is still loading: saving then would overwrite a good save with half-restored state.
     */
    fun savableSnapshot(): SessionStore.Saved? {
        if (tabs.any { it.isRestoring }) return null
        val kept = tabs.filterNot { it.isPrivate || it.url.isBlank() }
        if (kept.isEmpty()) return null
        val selectedIndex = kept.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
        return SessionStore.Saved(kept.map { SessionStore.SavedTab(it.url, it.sessionState, it.groupId, if (it.id == selectedId) System.currentTimeMillis() else it.lastActive) }, selectedIndex)
    }

    val selected: BrowserTab?
        get() = tabs.firstOrNull { it.id == selectedId }

    /** The profile's allow list for pop-ups. Null while the screen has not attached it, so nothing is allowed then. */
    var popupStore: PopupStore? = null

    /** A pop-up with no tap that the card is asking about. Null when no card is showing. */
    /** Pop-ups the page opened without a tap, oldest first. The card shows the first one and "N more" for the rest. */
    private val popupQueue = mutableStateListOf<PendingPopup>()

    val pendingPopup: PendingPopup?
        get() = popupQueue.firstOrNull()

    /** How many more pop-ups wait behind [pendingPopup]. */
    val morePopups: Int
        get() = (popupQueue.size - 1).coerceAtLeast(0)

    /** Adds a pop-up to the queue. A burst beyond [MAX_POPUP_QUEUE] is dropped, and the log says so. */
    fun askPopup(popup: PendingPopup) {
        if (popupQueue.size >= MAX_POPUP_QUEUE) {
            BrowserLog.warn("popup", "Pop-up queue full: dropped a pop-up from ${popup.fromSite}")
            return
        }
        popupQueue.add(popup)
    }

    /** Takes [popup] off the queue once the user has answered it. A stale answer for another pop-up does nothing. */
    fun answerPopup(popup: PendingPopup): Boolean {
        if (popupQueue.firstOrNull() != popup) return false
        popupQueue.removeAt(0)
        return true
    }


    /** The profile's "always open in the app" list. Null while the screen has not attached it. */
    var appLinkStore: AppLinkStore? = null


    /**
     * Called for every top-level navigation. Only a link the user caused, to a different site, can go to an app;
     * redirects, page scripts and subframes never do. Returns Deny when Ora should not load the address itself.
     */
    internal fun appLinkDecision(tab: BrowserTab, request: GeckoSession.NavigationDelegate.LoadRequest): AllowOrDeny {
        val uri = request.uri
        val userCaused = request.hasUserGesture || request.isDirectNavigation
        val private = tab.isPrivate
        // Private tabs always ask and never remember; they also log nothing about the link.
        val mode = if (private) AppLinkMode.ASK else appLinkMode()
        fun log(message: String) { if (!private) BrowserLog.info("applinks", message) }

        if (AppLinks.isWeb(uri)) {
            if (!userCaused || request.isRedirect) return AllowOrDeny.ALLOW
            val domain = LogPrivacy.domainOf(uri)
            if (domain == LogPrivacy.domainOf(tab.url)) return AllowOrDeny.ALLOW
            val match = AppLinks.findForWeb(context, uri)
            tab.appLabel = match?.label
            if (match == null || mode == AppLinkMode.NEVER) return AllowOrDeny.ALLOW
            val remembered = !private && appLinkStore?.isAlways(domain) == true
            if (mode == AppLinkMode.ALWAYS || remembered) {
                if (AppLinks.open(context, match)) {
                    log("Opened in app (${match.packageName}) for $domain")
                    tab.appLabel = null
                    return AllowOrDeny.DENY
                }
                log("Stayed in Ora: the app for $domain would not open")
                return AllowOrDeny.ALLOW
            }
            pendingAppLink = PendingAppLink(tab.id, uri, match.label, domain, web = true, fromPrivate = private, fallbackUrl = null)
            return AllowOrDeny.ALLOW
        }

        if (!AppLinks.isAppScheme(uri)) return AllowOrDeny.ALLOW
        val scheme = AppLinks.schemeOf(uri)
        // A page script cannot start an app: only a tap or a typed address.
        if (!userCaused) {
            log("Ignored a $scheme: link a page started on its own")
            return AllowOrDeny.DENY
        }
        val match = AppLinks.findForScheme(context, uri)
        if (match == null) {
            log("No app for a $scheme: link")
            InAppNotices.show("No app on this phone can open that.")
            return AllowOrDeny.DENY
        }
        val key = "$scheme:"
        val remembered = !private && appLinkStore?.isAlways(key) == true
        when {
            mode == AppLinkMode.NEVER -> {
                log("Stayed in Ora: opening apps is off, for a $scheme: link")
                match.fallbackUrl?.let { fallback -> mainHandler.post { tab.session.loadUri(fallback) } }
                    ?: InAppNotices.show("Open links in apps is set to Never in Settings.")
            }
            mode == AppLinkMode.ALWAYS || remembered -> openOrFallback(tab, match, key, private)
            else -> pendingAppLink = PendingAppLink(tab.id, uri, match.label, key, web = false, fromPrivate = private, fallbackUrl = match.fallbackUrl)
        }
        return AllowOrDeny.DENY
    }

    private fun openOrFallback(tab: BrowserTab, match: AppMatch, key: String, private: Boolean) {
        if (AppLinks.open(context, match)) {
            if (!private) BrowserLog.info("applinks", "Opened in app (${match.packageName ?: key}) for $key")
        } else {
            if (!private) BrowserLog.warn("applinks", "Stayed in Ora: no app opened for $key")
            match.fallbackUrl?.let { fallback -> mainHandler.post { tab.session.loadUri(fallback) } }
                ?: InAppNotices.show("No app on this phone can open that.")
        }
    }

    /** The user chose on the card. [open] false is Stay in Ora; [remember] saves the site or scheme for next time. */
    fun answerAppLink(link: PendingAppLink, open: Boolean, remember: Boolean = false) {
        if (pendingAppLink != link) return
        pendingAppLink = null
        val tab = tabs.firstOrNull { it.id == link.tabId }
        if (!open) {
            if (!link.fromPrivate) BrowserLog.info("applinks", "Stayed in Ora for ${link.key}")
            if (!link.web) link.fallbackUrl?.let { fallback -> tab?.session?.loadUri(fallback) }
            return
        }
        if (remember && !link.fromPrivate) appLinkStore?.add(link.key)
        val match = if (link.web) AppLinks.findForWeb(context, link.url) else AppLinks.findForScheme(context, link.url)
        if (match == null) {
            InAppNotices.show("That app isn't available any more.")
            return
        }
        if (tab != null) openOrFallback(tab, match, link.key, link.fromPrivate)
        else AppLinks.open(context, match)
    }

    /** The "App" chip: asks again about the page this tab shows. */
    fun askAboutApp(tab: BrowserTab) {
        val match = AppLinks.findForWeb(context, tab.url) ?: run { tab.appLabel = null; return }
        pendingAppLink = PendingAppLink(tab.id, tab.url, match.label, LogPrivacy.domainOf(tab.url), web = true, fromPrivate = tab.isPrivate, fallbackUrl = null)
    }

    fun newTab(url: String, select: Boolean = true, isPrivate: Boolean = false, restoreState: String? = null, restoring: Boolean = false, deferLoad: Boolean = false, adopt: GeckoSession? = null): BrowserTab {
        // A private tab is never created while the app is locked: it could not be shown, and nothing may load behind the lock.
        if (isPrivate && AppLock.locked) {
            BrowserLog.warn("tabs", "Did not open a private tab: the app is locked")
            AppLock.requestUnlock()
            InAppNotices.show("Unlock Ora to open a private tab")
            return selected ?: tabs.first()
        }
        val tab = BrowserTab(
            if (isPrivate) nextPrivateId-- else nextId++, context, url, { popupsBlocked() },
            { popup, opened -> newTab(popup, isPrivate = isPrivate, adopt = opened) },
            { response, page, priv -> onDownload(response, page, priv) },
            desktopByDefault, isPrivate, restoreState, { page, title -> onVisited(page, title) }, sitePermissions, restoring, deferLoad,
            siteAllowedForPopups = { domain -> popupStore?.isAllowed(domain) == true },
            onPopupAsk = { popup, fromSite -> askPopup(PendingPopup(popup, fromSite, isPrivate)) },
            onLinkRequest = { t, request -> appLinkDecision(t, request) },
            adopt = adopt,
        )
        tab.onAskApp = { askAboutApp(tab) }
        tabs.add(tab)
        if (select) select(tab.id)
        // Private tabs log no address, so the log does not record what was browsed privately.
        if (isPrivate) BrowserLog.info("tabs", "Opened private tab ${-tab.id - 1} (${tabs.count { it.isPrivate }} private open)")
        else BrowserLog.info("tabs", "Opened tab ${tab.id} (${tabs.count { !it.isPrivate }} open)")
        return tab
    }

    fun select(id: Long) {
        val tab = tabs.firstOrNull { it.id == id } ?: return
        tab.lastActive = System.currentTimeMillis()
        tab.ensureLoaded()
        selectedId = id
    }

    /** The tab closed most recently, kept so the user can undo the close for a few seconds. */
    var recentlyClosed by mutableStateOf<ClosedTab?>(null)
        private set

    /** How many tabs the offer in [recentlyClosed] brings back: 1 for a single close, more for a group. */
    var recentlyClosedCount by mutableStateOf(1)
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

    /** Brings back every tab the undo offer covers, then clears the offer. */
    fun undoRecentClose() {
        repeat(recentlyClosedCount) { reopenLastClosed() }
        recentlyClosed = null
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
        recentlyClosedCount = 1
        if (closed != null) {
            closedHistory.add(closed)
            if (closedHistory.size > MAX_CLOSED_HISTORY) closedHistory.removeAt(0)
        }
        tabs.removeAt(index).close()
        if (closing.isPrivate) BrowserLog.info("tabs", "Closed private tab ${-id - 1} (${tabs.count { it.isPrivate }} private open)")
        else BrowserLog.info("tabs", "Closed tab $id (${tabs.count { !it.isPrivate }} open)")
        if (tabs.isEmpty()) {
            newTab(START_URL)
        } else if (selectedId == id) {
            select(tabs[minOf(index, tabs.lastIndex)].id)
        }
    }

    /**
     * Closes every tab and opens one start page. Each page that could come back is kept for "Reopen closed tab"
     * (the last [MAX_CLOSED_HISTORY]); start pages and private tabs are not, as with a single close.
     */
    fun closeAllToStart() {
        val count = tabs.size
        UsageStats.count("tab.close_all")
        tabs.forEach { tab ->
            if (tab.url != START_URL && !tab.isPrivate) {
                closedHistory.add(ClosedTab(tab.id, tab.url, tab.title, tab.sessionState))
                if (closedHistory.size > MAX_CLOSED_HISTORY) closedHistory.removeAt(0)
            }
            tab.close()
        }
        tabs.clear()
        recentlyClosed = null
        BrowserLog.info("tabs", "Closed all $count tabs")
        newTab(START_URL)
    }

    /** Closes every private tab. A normal tab is selected afterwards, and a start page is opened if none is left. */
    fun closePrivate() {
        val doomed = tabs.filter { it.isPrivate }
        if (doomed.isEmpty()) return
        UsageStats.count("tab.close_private")
        doomed.forEach { it.close() }
        tabs.removeAll(doomed)
        BrowserLog.info("tabs", "Closed ${doomed.size} private tabs")
        if (tabs.isEmpty()) newTab(START_URL)
        else if (tabs.none { it.id == selectedId }) select(tabs.last().id)
    }

    // ---- Tab groups ----

    private fun persistGroups() {
        groups.forEachIndexed { i, g -> if (g.order != i) groups[i] = g.copy(order = i) }
        groupStore?.save(groups.toList())
    }

    /** True for a tab not opened in 14 days. The open tab is never inactive. */
    fun isInactive(tab: BrowserTab): Boolean =
        !tab.isPrivate && tab.id != selectedId && System.currentTimeMillis() - tab.lastActive > INACTIVE_AFTER_MS

    fun tabsIn(groupId: String): List<BrowserTab> = tabs.filter { it.groupId == groupId }

    /** Adds an empty group. A blank name becomes "Group N". */
    fun addGroup(name: String, color: GroupColor): TabGroup {
        val clean = name.trim().ifEmpty { "Group ${groups.size + 1}" }
        val group = TabGroup("g" + System.currentTimeMillis(), clean, color, collapsed = false, order = groups.size)
        groups.add(group)
        persistGroups()
        UsageStats.count("group.new")
        BrowserLog.info("groups", "Added group $clean (${color.label})")
        return group
    }

    fun updateGroup(id: String, name: String, color: GroupColor) {
        val i = groups.indexOfFirst { it.id == id }
        if (i < 0) return
        groups[i] = groups[i].copy(name = name.trim().ifEmpty { groups[i].name }, color = color)
        persistGroups()
        BrowserLog.info("groups", "Edited group ${groups[i].name} (${color.label})")
    }

    fun toggleCollapsed(id: String) {
        val i = groups.indexOfFirst { it.id == id }
        if (i < 0) return
        groups[i] = groups[i].copy(collapsed = !groups[i].collapsed)
        persistGroups()
    }

    /** Puts a tab in a group, or takes it out with a null id. Private tabs cannot be grouped. */
    fun moveToGroup(tabId: Long, groupId: String?) {
        val tab = tabs.firstOrNull { it.id == tabId } ?: return
        if (tab.isPrivate || (groupId != null && groups.none { it.id == groupId })) return
        tab.groupId = groupId
        BrowserLog.info("groups", if (groupId == null) "Took tab $tabId out of its group" else "Added tab $tabId to a group")
    }

    /** Deletes the group and keeps its tabs, now not in a group. */
    fun ungroup(id: String) {
        tabsIn(id).forEach { it.groupId = null }
        groups.removeAll { it.id == id }
        persistGroups()
        BrowserLog.info("groups", "Ungrouped a group")
    }

    /** Deletes the group and closes its tabs. */
    fun closeGroup(id: String) {
        val doomed = tabsIn(id).map { it.id }
        val keptBefore = closedHistory.size
        doomed.forEach { close(it) }
        // One offer for the whole group: "N tabs closed", and Undo brings back the ones that were kept.
        val kept = closedHistory.size - keptBefore
        recentlyClosed = if (kept > 0) closedHistory.last() else null
        recentlyClosedCount = maxOf(kept, 1)
        groups.removeAll { it.id == id }
        persistGroups()
        BrowserLog.info("groups", "Closed a group and its ${doomed.size} tabs")
    }

    fun closeAll() {
        tabs.forEach { it.close() }
        tabs.clear()
    }
}

/** A tab not opened for this long moves into the collapsed Inactive tabs row (14 days). */
const val INACTIVE_AFTER_MS = 14L * 24 * 60 * 60 * 1000
/** [BrowserLog] for one tab. Private tabs are silent except for crashes, which say only that a private tab crashed. */
private class TabLog(private val silent: Boolean) {
    fun info(tag: String, message: String) { if (!silent) BrowserLog.info(tag, message) }
    fun warn(tag: String, message: String, error: Throwable? = null) { if (!silent) BrowserLog.warn(tag, message, error) }
    fun error(tag: String, message: String, error: Throwable? = null) {
        if (!silent) BrowserLog.error(tag, message, error) else BrowserLog.error(tag, "A private tab hit an error")
    }
}

/**
 * The open tabs of every profile, kept for the life of the process. A recreated window (Android rebuilds it for some
 * system changes) picks these up instead of opening every tab a second time.
 */
object TabRegistry {
    val byProfile = HashMap<String, BrowserTabs>()
}
