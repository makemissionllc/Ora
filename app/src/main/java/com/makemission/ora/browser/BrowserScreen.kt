package com.makemission.ora.browser

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.makemission.ora.ui.theme.OraMotion
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.abs
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.bookmarks.BookmarksRepository
import com.makemission.ora.profiles.ProfileSwitcher
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.extensions.ExtensionsRepository
import com.makemission.ora.extensions.ExtensionsScreen
import com.makemission.ora.settings.BarStyle
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.DarkBrowserColors
import com.makemission.ora.ui.theme.LightBrowserColors
import com.makemission.ora.ui.theme.LocalBrowserColors
import com.makemission.ora.ui.theme.PureDarkBrowserColors
import com.makemission.ora.ui.theme.browserColors
import com.makemission.ora.settings.AppearanceMode
import com.makemission.ora.settings.SettingsScreen
import com.makemission.ora.settings.SearchEngine
import com.makemission.ora.settings.isSearchInput
import com.makemission.ora.downloads.DownloadsRepository
import com.makemission.ora.downloads.DownloadsSheet
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import androidx.compose.foundation.isSystemInDarkTheme
import java.net.URLEncoder
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import android.widget.Toast

/** Screens narrower than this use the phone layout (tab grid); wider ones use the sidebar layout. */
/** Screens at least this wide use the tablet layout. Shared with Settings so each device sees its own options. */
const val TABLET_MIN_WIDTH_DP = 600

/** Full-screen overlays on phones. NONE means the browser page is showing. */
private enum class PhoneOverlay { NONE, TABS, LIBRARY, SETTINGS, EXTENSIONS }

/** Bottom-right corner, where the tabs button sits. Overlays scale out of and into this point. */
private val MORPH_ORIGIN = TransformOrigin(0.85f, 1f)

/** Overlays that slide in from the right edge instead of growing out of the tabs button. */
private val SLIDE_OVERLAYS = setOf(PhoneOverlay.LIBRARY, PhoneOverlay.SETTINGS, PhoneOverlay.EXTENSIONS)

/** Holds the GeckoView so the tab grid can capture the current page as a thumbnail. */
private class PageView {
    var view: GeckoView? = null
}

/**
 * Browser screen. Picks the color palette from the Appearance setting, then draws the browser.
 */
@Composable
fun BrowserScreen(
    settings: SettingsRepository,
    profiles: ProfilesRepository,
    modifier: Modifier = Modifier
) {
    val appearance by settings.appearance.collectAsState()
    val palette = when (appearance) {
        AppearanceMode.PURE_DARK -> PureDarkBrowserColors
        AppearanceMode.DARK -> DarkBrowserColors
        AppearanceMode.LIGHT -> LightBrowserColors
        AppearanceMode.AUTO -> if (isSystemInDarkTheme()) DarkBrowserColors else LightBrowserColors
    }
    // Web pages follow the app's light or dark palette (or the system setting when the toggle is off).
    val websiteTheme by settings.websiteTheme.collectAsState()
    val isDark = palette == DarkBrowserColors || palette == PureDarkBrowserColors
    val runtimeSettings = GeckoEngine.runtime(LocalContext.current).settings
    LaunchedEffect(websiteTheme, isDark) {
        runtimeSettings.preferredColorScheme = when {
            !websiteTheme -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
            isDark -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
            else -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
        }
    }
    CompositionLocalProvider(LocalBrowserColors provides palette) {
        BrowserContent(settings, profiles, modifier)
    }
}

/**
 * Phone: full-screen page, bottom toolbar, tab grid on demand.
 * Tablet: tab/bookmark sidebar on the left, page and bottom toolbar on the right.
 */
@Composable
private fun BrowserContent(
    settings: SettingsRepository,
    profiles: ProfilesRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isTablet = LocalConfiguration.current.screenWidthDp >= TABLET_MIN_WIDTH_DP
    // Each profile has its own tabs and bookmarks. Switching profile swaps both; other profiles' tabs stay open.
    val profileId by profiles.activeId.collectAsState()
    val profileList by profiles.profiles.collectAsState()
    val bookmarks = remember(profileId) { BookmarksRepository(context, profileId) }
    val bookmarkList by bookmarks.bookmarks.collectAsState()

    val blockAds by settings.blockAds.collectAsState()
    val fadePagesIn by settings.fadePagesIn.collectAsState()
    val secureDns by settings.secureDns.collectAsState()
    val downloads = remember { DownloadsRepository(context) }
    var showDownloads by remember { mutableStateOf(false) }
    val tabsByProfile = remember { HashMap<String, BrowserTabs>() }
    val tabs = remember(profileId) {
        tabsByProfile.getOrPut(profileId) {
            BrowserTabs(
                context,
                { settings.blockPopups.value },
                { response, pageUrl -> downloads.start(response, pageUrl) },
                desktopByDefault = isTablet
            )
        }
    }
    // A web link handed to Ora by another app opens in a new tab and becomes the selected one.
    val incomingLink by ExternalLinks.pending.collectAsState()
    LaunchedEffect(incomingLink) {
        val url = incomingLink ?: return@LaunchedEffect
        ExternalLinks.consume()
        tabs.newTab(url)
    }
    // A removed profile's tabs are closed, so their pages stop running.
    LaunchedEffect(profileList) {
        val ids = profileList.map { it.id }.toSet()
        tabsByProfile.keys.filterNot { it in ids }.forEach { id -> tabsByProfile.remove(id)?.closeAll() }
    }
    // Ad and tracker blocking is a runtime setting: apply it whenever the toggle changes.
    LaunchedEffect(blockAds) {
        GeckoEngine.runtime(context).settings.contentBlocking
            .setEnhancedTrackingProtectionCategory(
                if (blockAds) ContentBlocking.EtpCategory.STRICT else ContentBlocking.EtpCategory.STANDARD
            )
    }
    // Secure DNS is a runtime setting: TRR_MODE_FIRST tries encrypted DNS and falls back to system DNS on failure.
    LaunchedEffect(secureDns) {
        GeckoEngine.runtime(context).settings.setTrustedRecursiveResolverMode(
            if (secureDns) GeckoRuntimeSettings.TRR_MODE_FIRST else GeckoRuntimeSettings.TRR_MODE_OFF
        )
    }
    var showTabGrid by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    LaunchedEffect(showMenu) { if (showMenu) UsageStats.count("menu.open") }
    var showLibrary by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExtensions by remember { mutableStateOf(false) }
    /** Find-in-page bar is open over the page. */
    var showFind by remember { mutableStateOf(false) }
    val extensions = remember { ExtensionsRepository(GeckoEngine.runtime(context)) }
    val searchEngines by settings.searchEngines.collectAsState()
    val defaultEngine by settings.defaultSearchEngine.collectAsState()
    val askEachSearch by settings.askEachSearch.collectAsState()
    /** Search text waiting for the user to pick an engine. Null when the chooser is closed. */
    var pendingSearch by remember { mutableStateOf<String?>(null) }
    val pageView = remember { PageView() }

    val selected = tabs.selected ?: return
    val selectedId = tabs.selectedId

    // Only the selected tab should be active, so background pages stop rendering.
    LaunchedEffect(selectedId) {
        tabs.tabs.forEach { it.session.setActive(it.id == selectedId) }
    }

    val onNewTab: () -> Unit = {
        UsageStats.count("tab.new")
        tabs.newTab(BrowserTabs.START_URL)
    }
    val phoneBar by settings.phoneBarStyle.collectAsState()
    val tabletBar by settings.tabletBarStyle.collectAsState()
    val barStyle = if (isTablet) tabletBar else phoneBar
    /** Opens the tab grid, refreshing the current tab's preview first. */
    val openTabs: () -> Unit = {
        captureThumbnail(pageView, selected)
        showTabGrid = true
    }
    /** Moves to the next (+1) or previous (-1) open tab, wrapping at the ends. */
    val switchTab: (Int) -> Unit = { delta ->
        val list = tabs.tabs
        if (list.size > 1) {
            val index = list.indexOfFirst { it.id == selectedId }
            val next = list[Math.floorMod(index + delta, list.size)]
            tabs.select(next.id)
            BrowserLog.info("tabs", "Swiped to tab ${next.id}")
        }
    }
    // Where the bar sits. Tablet Compact sits at the top like a desktop address bar; phone Compact stays at the bottom.
    val topLayout = barStyle == BarStyle.TOP || (isTablet && barStyle == BarStyle.COMPACT)
    /** Tablet only: whether the tab and bookmark sidebar is visible. Saved in settings. */
    val showSidebar by settings.tabletSidebar.collectAsState()
    /** Bookmarks and open tabs, offered as address bar suggestions. */
    val addressCandidates = (
        bookmarkList.map { AddressSuggestion(it.title, displayUrl(it.url), it.url) } +
            tabs.tabs.map { AddressSuggestion(it.title.ifBlank { displayUrl(it.url) }, displayUrl(it.url), it.url) }
        ).distinctBy { it.target }

    /** Loads typed text: searches through the chooser or default engine, or opens a URL. */
    fun submitAddress(input: String) {
        selected.markStartPageBehind()
        if (isSearchInput(input) && askEachSearch && searchEngines.size > 1) {
            pendingSearch = input.trim()
        } else if (isSearchInput(input)) {
            val engine = defaultEngine
            BrowserLog.info("search", "Searching ${engine.label} for \"${input.trim()}\"")
            selected.session.loadUri(engine.urlFor(input.trim()))
        } else {
            selected.session.loadUri(toLoadableUrl(input, defaultEngine.template))
        }
    }

    // Full-screen phone overlay currently open (tablets never use one). Tablet Settings is a side panel instead.
    val tabletSettingsOpen = isTablet && showSettings
    val tabletTabsOpen = isTablet && showTabGrid
    val phoneOverlay = when {
        showTabGrid && !isTablet -> PhoneOverlay.TABS
        showLibrary -> PhoneOverlay.LIBRARY
        showSettings && !isTablet -> PhoneOverlay.SETTINGS
        showExtensions -> PhoneOverlay.EXTENSIONS
        else -> PhoneOverlay.NONE
    }
    val clearBrowsingData: () -> Unit = {
        GeckoEngine.runtime(context).storageController
            .clearData(StorageController.ClearFlags.ALL)
            .accept(
                { Toast.makeText(context, "Browsing data cleared", Toast.LENGTH_SHORT).show() },
                { Toast.makeText(context, "Could not clear browsing data", Toast.LENGTH_SHORT).show() }
            )
    }

    // Back closes the open overlay first, then steps back through page history, and only then exits.
    BackHandler(enabled = phoneOverlay != PhoneOverlay.NONE || tabletSettingsOpen || tabletTabsOpen) {
        showTabGrid = false
        showLibrary = false
        showSettings = false
        showExtensions = false
    }
    BackHandler(enabled = phoneOverlay == PhoneOverlay.NONE && !tabletSettingsOpen && !tabletTabsOpen && selected.canStepBack) {
        selected.stepBack()
    }

    pendingSearch?.let { query ->
        SearchChooserSheet(
            query = query,
            engines = SearchEngine.entries.filter { it in searchEngines },
            default = defaultEngine,
            onChoose = { engine ->
                pendingSearch = null
                BrowserLog.info("search", "Searching ${engine.label} for \"$query\"")
                selected.session.loadUri(engine.urlFor(query))
            },
            onDismiss = { pendingSearch = null }
        )
    }

    // Bottom-edge bars: tablet keeps them in the Scaffold slot; phone draws them as a floating overlay.
    val bottomBarContent: @Composable () -> Unit = {
        if (!isTablet && barStyle == BarStyle.TOP) {
            // Phone Top: the controls sit at the bottom edge, as in Safari's Top option.
            AnimatedVisibility(
                visible = phoneOverlay == PhoneOverlay.NONE && !selected.chromeHidden,
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = OraMotion.exit())
            ) {
                BottomControlsBar(
                    tab = selected,
                    isBookmarked = bookmarkList.any { it.url == selected.url },
                    onBookmark = { bookmarks.toggle(selected.url, selected.title) },
                    onShare = { shareUrl(context, selected.url) },
                    onTabs = openTabs,
                    onMenu = { showMenu = true },
                    tabCount = tabs.tabs.size
                )
            }
        } else if (!topLayout) {
            // The bar folds down into its bottom edge while an overlay grows out of the tabs button.
            // Phone Compact never leaves while a page is open: scrolling only shrinks it, as in iOS Safari.
            AnimatedVisibility(
                visible = phoneOverlay == PhoneOverlay.NONE && (barStyle == BarStyle.COMPACT || !selected.chromeHidden),
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = OraMotion.exit())
            ) {
                if (barStyle == BarStyle.BOTTOM) {
                    BrowserToolbar(
                        tab = selected,
                        isBookmarked = bookmarkList.any { it.url == selected.url },
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onBookmark = { bookmarks.toggle(selected.url, selected.title) },
                        onShare = { shareUrl(context, selected.url) },
                        onTabs = openTabs,
                        onMenu = { showMenu = true },
                        tabCount = tabs.tabs.size,
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab
                    )
                } else {
                    // Phone Compact: a floating row like iOS Safari's compact bar.
                    PhoneCompactBar(
                        tab = selected,
                        shrunk = selected.chromeHidden,
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onMore = { showMenu = true },
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            // Adaptive shrink: the top bar folds up while the page is scrolled down, and returns on scroll up.
            AnimatedVisibility(
                visible = topLayout && !selected.chromeHidden,
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Top, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = OraMotion.exit())
            ) {
                if (isTablet && barStyle == BarStyle.TOP) {
                    // Tablet Top: the desktop-style toolbar with sidebar toggle, navigation and tools.
                    TabletToolbar(
                        tab = selected,
                        isBookmarked = bookmarkList.any { it.url == selected.url },
                        showSidebarToggle = isTablet,
                        showSidebar = showSidebar,
                        onToggleSidebar = { settings.setTabletSidebar(!showSidebar) },
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onBookmark = { bookmarks.toggle(selected.url, selected.title) },
                        onShare = { shareUrl(context, selected.url) },
                        onNewTab = onNewTab,
                        onDownloads = { showDownloads = true },
                        onMore = { showMenu = true },
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab,
                        onTabs = openTabs,
                        tabCount = tabs.tabs.size
                    )
                } else {
                    // Compact at the top (tablet, and phone Top): only the address bar and the menu.
                    CompactToolbar(
                        tab = selected,
                        showSidebarToggle = false,
                        showSidebar = showSidebar,
                        onToggleSidebar = { settings.setTabletSidebar(!showSidebar) },
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onMore = { showMenu = true },
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab
                    )
                }
            }
        },
        bottomBar = { if (isTablet) bottomBarContent() }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // The sidebar slides in and out from its left edge, using the shared overlay motion.
            AnimatedVisibility(
                visible = isTablet && showSidebar,
                enter = fadeIn(OraMotion.enter()) + expandHorizontally(expandFrom = Alignment.Start, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = OraMotion.exit())
            ) {
                Column(modifier = Modifier.fillMaxHeight()) {
                    ProfileSwitcher(
                        profiles = profileList,
                        activeId = profileId,
                        onSelect = { id -> profiles.select(id) },
                        onAdd = { name -> profiles.add(name) },
                        onRemove = { id -> profiles.remove(id) },
                    )
                    TabSidebar(
                        tabs = tabs,
                        bookmarks = bookmarkList,
                        onNewTab = onNewTab,
                        onOpenBookmark = { url -> selected.session.loadUri(url) }
                    )
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                BrowserPage(
                    tab = selected,
                    pageView = pageView,
                    bookmarks = bookmarkList,
                    blockAds = blockAds,
                    fadePagesIn = fadePagesIn,
                    onSubmit = { input -> submitAddress(input) }
                )
                if (showDownloads) {
                    DownloadsSheet(repository = downloads, onDismiss = { showDownloads = false })
                }
                // Long press on a link or image opens the quick menu at the press point (both layouts).
                selected.pressTarget?.let { target ->
                    key(target) {
                        LinkQuickMenu(
                            target = target,
                            tabs = tabs,
                            onDismiss = { selected.pressTarget = null },
                            onShare = { url -> shareUrl(context, url) }
                        )
                    }
                }
                // Text selected on the page: a card offers search, copy, share-with-highlight and phone or date actions.
                val picked = selected.selection
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = BrowserTokens.SelectionMenuBottomInset)
                ) {
                    SelectionMenuOverlay(visible = picked != null) {
                        picked?.text?.let { text ->
                            SelectionMenu(
                                text = text,
                                onSearch = { query ->
                                    selected.clearSelection()
                                    submitAddress(query)
                                },
                                onCopy = {
                                    picked.copy()
                                    BrowserLog.info("select", "Copied selected text")
                                    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                },
                                onSelectAll = { picked.selectAll() },
                                onShare = { selectedText ->
                                    shareUrl(context, highlightUrl(selected.url, selectedText))
                                    BrowserLog.info("select", "Shared a link with a highlight")
                                },
                                onDismiss = { selected.clearSelection() },
                            )
                        }
                    }
                }
                // Quality of life: a closed tab can be brought back for a few seconds, instead of being lost.
                val closed = tabs.recentlyClosed
                LaunchedEffect(closed?.id) {
                    if (closed != null) {
                        delay(UNDO_CLOSE_MS)
                        tabs.dismissUndo()
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = BrowserTokens.SelectionMenuBottomInset)
                ) {
                    UndoCloseOffer(visible = closed != null, onUndo = { tabs.reopenLastClosed() })
                }
                // Tablet Settings: a panel slides in from the right over the page. Tapping the dimmed page closes it.
                TabletSettingsPanel(
                    visible = isTablet && showSettings,
                    settings = settings,
                    onClose = { showSettings = false },
                    onClearData = clearBrowsingData,
                )
                if (showFind) {
                    // Keyed on the tab, so switching tabs closes the bar and clears its highlights.
                    key(selected.id) {
                        FindBar(
                            session = selected.session,
                            onClose = { showFind = false },
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                        )
                    }
                }
                if (showMenu) {
                    BrowserMenuSheet(
                            isBookmarked = bookmarkList.any { it.url == selected.url },
                        onDismiss = { showMenu = false },
                        onShare = {
                            showMenu = false
                            shareUrl(context, selected.url)
                        },
                        onBookmark = {
                            bookmarks.toggle(selected.url, selected.title)
                            showMenu = false
                        },
                        onNewTab = {
                            showMenu = false
                            onNewTab()
                        },
                        onLibrary = {
                            showMenu = false
                            showLibrary = true
                        },
                        onFind = {
                            showMenu = false
                            showFind = true
                        },
                        desktopSite = selected.desktopSite,
                        onToggleDesktop = { enabled ->
                            selected.applyDesktopSite(enabled)
                            showMenu = false
                        },
                        onSettings = {
                            showMenu = false
                            showSettings = true
                        },
                        onExtensions = {
                            showMenu = false
                            showExtensions = true
                        },
                        onDownloads = {
                            showMenu = false
                            showDownloads = true
                        },
                        onTabs = {
                            showMenu = false
                            openTabs()
                        }
                    )
                }
                // Overlays grow out of the tabs button's corner and shrink back into it.
                AnimatedContent(
                    targetState = phoneOverlay,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val intoSlide = targetState in SLIDE_OVERLAYS
                        val outOfSlide = initialState in SLIDE_OVERLAYS
                        when {
                            // Pages slide in from the right edge and leave the same way.
                            intoSlide -> (slideInHorizontally(OraMotion.enter()) { it } + fadeIn(OraMotion.enter())) togetherWith
                                if (outOfSlide) slideOutHorizontally(OraMotion.exit()) { it } + fadeOut(OraMotion.exit())
                                else scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.exit(), transformOrigin = MORPH_ORIGIN) + fadeOut(OraMotion.exit())
                            // Returning to the page: the slide pages leave to the right.
                            outOfSlide -> (fadeIn(OraMotion.enter())) togetherWith
                                (slideOutHorizontally(OraMotion.exit()) { it } + fadeOut(OraMotion.exit()))
                            // The tab grid grows out of the tabs button and shrinks back into it.
                            else -> (fadeIn(OraMotion.enter()) + scaleIn(
                                initialScale = OraMotion.MORPH_SCALE,
                                animationSpec = OraMotion.enter(),
                                transformOrigin = MORPH_ORIGIN
                            )) togetherWith (fadeOut(OraMotion.exit()) + scaleOut(
                                targetScale = OraMotion.MORPH_SCALE,
                                animationSpec = OraMotion.exit(),
                                transformOrigin = MORPH_ORIGIN
                            ))
                        }
                    },
                    label = "phone-overlay"
                ) { overlay ->
                    when (overlay) {
                        PhoneOverlay.SETTINGS -> SettingsScreen(
                            settings = settings,
                            onDone = { showSettings = false },
                            onClearData = clearBrowsingData
                        )
                        PhoneOverlay.LIBRARY -> BookmarksLibrary(
                            bookmarks = bookmarkList,
                            onOpen = { url ->
                                selected.session.loadUri(url)
                                showLibrary = false
                            },
                            onRemove = { url -> bookmarks.remove(url) },
                            onDone = { showLibrary = false }
                        )
                        PhoneOverlay.TABS -> Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(browserColors().GridBackground)
                                .statusBarsPadding()
                        ) {
                            ProfileSwitcher(
                                profiles = profileList,
                                activeId = profileId,
                                onSelect = { id -> profiles.select(id) },
                                onAdd = { name -> profiles.add(name) },
                                onRemove = { id -> profiles.remove(id) },
                            )
                            TabGrid(
                                tablet = isTablet,
                                tabs = tabs,
                                onSelect = { id ->
                                    tabs.select(id)
                                    showTabGrid = false
                                },
                                onClose = { id -> tabs.close(id) },
                                onNewTab = {
                                    onNewTab()
                                    showTabGrid = false
                                },
                                onDone = { showTabGrid = false }
                            )
                        }
                        PhoneOverlay.EXTENSIONS -> ExtensionsScreen(
                            repository = extensions,
                            onDone = { showExtensions = false }
                        )
                        PhoneOverlay.NONE -> Unit
                    }
                }
            }
        }
        // Phone bars float over the page, as iOS does: the page runs under them and nothing is drawn behind.
        if (!isTablet) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) { bottomBarContent() }
        }
        }
    }
    // Tablet tab overview covers the whole window, toolbar and sidebar included. It grows out of the tabs button.
    AnimatedVisibility(
        visible = tabletTabsOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(OraMotion.enter()) + scaleIn(initialScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.enter(), transformOrigin = MORPH_ORIGIN),
        exit = fadeOut(OraMotion.exit()) + scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.exit(), transformOrigin = MORPH_ORIGIN),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(browserColors().GridBackground)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            ProfileSwitcher(
                profiles = profileList,
                activeId = profileId,
                onSelect = { id -> profiles.select(id) },
                onAdd = { name -> profiles.add(name) },
                onRemove = { id -> profiles.remove(id) },
            )
            TabGrid(
                tablet = true,
                tabs = tabs,
                onSelect = { id ->
                    tabs.select(id)
                    showTabGrid = false
                },
                onClose = { id -> tabs.close(id) },
                onNewTab = {
                    onNewTab()
                    showTabGrid = false
                },
                onDone = { showTabGrid = false }
            )
        }
    }
    }
}

/** Tablet Settings: a panel that slides in from the right over the page, with a scrim that closes it when tapped. */
@Composable
private fun TabletSettingsPanel(
    visible: Boolean,
    settings: SettingsRepository,
    onClose: () -> Unit,
    onClearData: () -> Unit,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit())) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(browserColors().Scrim)
                .clickable { onClose() }
        )
    }
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(OraMotion.enter()) { it } + fadeIn(OraMotion.enter()),
        exit = slideOutHorizontally(OraMotion.exit()) { it } + fadeOut(OraMotion.exit()),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(BrowserTokens.SettingsPanelFraction)
                    .widthIn(max = BrowserTokens.SettingsPanelMaxWidth)
                    .shadow(BrowserTokens.FloatingBarElevation, RoundedCornerShape(topStart = BrowserTokens.SettingsPanelRadius, bottomStart = BrowserTokens.SettingsPanelRadius))
                    .clip(RoundedCornerShape(topStart = BrowserTokens.SettingsPanelRadius, bottomStart = BrowserTokens.SettingsPanelRadius))
                    .background(browserColors().GridBackground)
            ) {
                SettingsScreen(settings = settings, onDone = onClose, onClearData = onClearData)
            }
        }
    }
}

/** Shows the selected tab's page. The single GeckoView is re-pointed at whichever session is selected. */
@Composable
private fun BrowserPage(
    tab: BrowserTab,
    pageView: PageView,
    bookmarks: List<Bookmark>,
    blockAds: Boolean,
    fadePagesIn: Boolean,
    onSubmit: (String) -> Unit,
) {
    if (tab.url == BrowserTabs.START_URL) {
        StartPage(bookmarks = bookmarks, blockAds = blockAds, onSubmit = onSubmit, modifier = Modifier.fillMaxSize())
        return
    }
    // The page stays transparent while it arrives, then fades in with the shared enter motion.
    // With the setting off it is always fully shown, as before.
    val pageAlpha by animateFloatAsState(
        targetValue = if (!fadePagesIn || tab.pageRevealed) 1f else 0f,
        animationSpec = if (fadePagesIn && tab.pageRevealed) OraMotion.enter<Float>() else OraMotion.exit<Float>(),
        label = "page reveal",
    )
    // Pull to refresh: pulling down from the top of the page reloads it. A small indicator follows the finger
    // and stays while the page reloads. The page itself keeps scrolling as before.
    var pull by remember { mutableStateOf(0.dp) }
    val indicatorVisible = pull > 0.dp || tab.loading
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToReload(
                canPull = { tab.lastScrollY <= PULL_SCROLL_TOLERANCE_PX && !tab.loading },
                onDistance = { pull = it },
                onRefresh = {
                    BrowserLog.info("page", "Pull to refresh: ${tab.url}")
                    tab.session.reload()
                }
            )
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = pageAlpha },
            factory = { ctx -> GeckoView(ctx).also { pageView.view = it } },
            update = { view -> if (view.session != tab.session) view.setSession(tab.session) }
        )
        AnimatedVisibility(
            visible = indicatorVisible,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            enter = fadeIn(OraMotion.enter()) + scaleIn(OraMotion.enter(), initialScale = OraMotion.MORPH_SCALE),
            exit = fadeOut(OraMotion.exit()) + scaleOut(OraMotion.exit(), targetScale = OraMotion.MORPH_SCALE),
        ) {
            Box(
                modifier = Modifier
                    .size(BrowserTokens.PullRefreshIndicatorSize)
                    .graphicsLayer { translationY = (pull.coerceAtMost(BrowserTokens.PullRefreshThreshold) / 2).toPx() }
                    .clip(CircleShape)
                    .background(browserColors().AddressBarBackground)
                    .shadow(BrowserTokens.AddressBarElevation, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (tab.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = browserColors().Accent
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { (pull / BrowserTokens.PullRefreshThreshold).coerceIn(0f, 1f) },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = browserColors().Accent
                    )
                }
            }
        }
    }
}

/** Captures the visible page into the tab's thumbnail. Failures keep the previous preview. */
private fun captureThumbnail(pageView: PageView, tab: BrowserTab) {
    pageView.view?.capturePixels()?.accept(
        { bitmap -> if (bitmap != null) tab.thumbnail = bitmap.asImageBitmap() },
        { }
    )
}

@Composable
private fun BrowserToolbar(
    tab: BrowserTab,
    isBookmarked: Boolean,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onTabs: () -> Unit,
    onMenu: (() -> Unit)?,
    onSwipeUp: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    tabCount: Int,
) {
    FloatingBarHolder(insets = WindowInsets.navigationBars.union(WindowInsets.ime)) {
        Column(
            modifier = Modifier.padding(BrowserTokens.ToolbarPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
        ) {
            // Like iOS Safari's bottom bar: the full-width address bar sits above the controls.
            AddressBar(
                currentUrl = tab.url,
                loading = tab.loading,
                onSubmit = onSubmit,
                candidates = candidates,
                onReload = { tab.session.reload() }, onHardReload = { hardReload(tab) }, progress = tab.progress,
                onStop = { tab.session.stop() },
                modifier = Modifier.fillMaxWidth(),
                onSwipeUp = onSwipeUp,
                onSwitchTab = onSwitchTab
            )
            BottomControls(
                tab = tab,
                isBookmarked = isBookmarked,
                onBookmark = onBookmark,
                onShare = onShare,
                onTabs = onTabs,
                onMenu = onMenu,
                tabCount = tabCount
            )
        }
    }
}

/**
 * Holds a phone bottom bar as a floating rounded card: inset from the screen edges, lifted by a shadow,
 * and kept clear of the navigation bar. Nothing is drawn behind it, so the page shows around it.
 */
@Composable
private fun FloatingBarHolder(
    insets: WindowInsets,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(insets)
            .padding(horizontal = BrowserTokens.FloatingBarInset, vertical = BrowserTokens.FloatingBarInset)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = BrowserTokens.FloatingBarShape,
            color = browserColors().ToolbarBackground,
            shadowElevation = BrowserTokens.FloatingBarElevation
        ) {
            content()
        }
    }
}

/**
 * Control row for the phone Top layout: the address bar sits at the top, and back, forward,
 * share, bookmark, tabs and menu sit at the bottom edge, as in Safari's Top option.
 */
@Composable
private fun BottomControlsBar(
    tab: BrowserTab,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    tabCount: Int,
) {
    FloatingBarHolder(insets = WindowInsets.navigationBars) {
        BottomControls(
            tab = tab,
            isBookmarked = isBookmarked,
            onBookmark = onBookmark,
            onShare = onShare,
            onTabs = onTabs,
            onMenu = onMenu,
            tabCount = tabCount,
            modifier = Modifier.padding(BrowserTokens.ToolbarPadding)
        )
    }
}

/** Back, forward, share, bookmark, tabs and menu, spread evenly across the row. */
@Composable
private fun BottomControls(
    tab: BrowserTab,
    isBookmarked: Boolean,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onTabs: () -> Unit,
    onMenu: (() -> Unit)?,
    tabCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(onClick = { tab.stepBack() }, enabled = tab.canStepBack, description = "Back") {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
        }
        ToolbarButton(onClick = { tab.session.goForward() }, enabled = tab.canGoForward, description = "Forward") {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
        }
        ToolbarButton(onClick = onShare, enabled = tab.url.isNotBlank(), description = "Share") {
            Icon(Icons.Filled.Share, contentDescription = null)
        }
        ToolbarButton(onClick = onBookmark, enabled = tab.url.isNotBlank(), description = "Bookmark") {
            Icon(
                if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                contentDescription = null
            )
        }
        ToolbarButton(onClick = onTabs, enabled = true, description = "Tabs") {
            TabsIcon(tabCount)
        }
        if (onMenu != null) {
            ToolbarButton(onClick = onMenu, enabled = true, description = "Menu") {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
        }
    }
}

/**
 * Tablet top toolbar (design: D-Browse): sidebar toggle, back, forward, a centered
 * address pill, then share, bookmark and new tab.
 */
/** The Tabs icon with the number of open tabs on top, so the count is visible without opening the overview. */
@Composable
private fun TabsIcon(tabCount: Int) {
    // A new tab makes the icon swell and settle, so the user sees the tab was added even with the grid closed.
    val bump = remember { Animatable(1f) }
    var previous by remember { mutableIntStateOf(tabCount) }
    LaunchedEffect(tabCount) {
        if (tabCount > previous) {
            bump.snapTo(TAB_BUMP_SCALE)
            bump.animateTo(1f, OraMotion.enter<Float>())
        }
        previous = tabCount
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.graphicsLayer { scaleX = bump.value; scaleY = bump.value }
    ) {
        Icon(Icons.Outlined.Tab, contentDescription = null)
        Text(
            text = tabCount.toString(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = browserColors().Accent
        )
    }
}

@Composable
private fun TabletToolbar(
    tab: BrowserTab,
    isBookmarked: Boolean,
    showSidebarToggle: Boolean,
    showSidebar: Boolean,
    onToggleSidebar: () -> Unit,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onNewTab: () -> Unit,
    onDownloads: () -> Unit,
    onMore: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    onTabs: () -> Unit,
    tabCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(browserColors().ToolbarBackground)
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BrowserTokens.ToolbarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
        ) {
            if (showSidebarToggle) {
                ToolbarButton(onClick = onToggleSidebar, enabled = true, description = if (showSidebar) "Hide sidebar" else "Show sidebar") {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                }
            }
            ToolbarButton(onClick = { tab.stepBack() }, enabled = tab.canStepBack, description = "Back") {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
            ToolbarButton(onClick = { tab.session.goForward() }, enabled = tab.canGoForward, description = "Forward") {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AddressBar(
                    currentUrl = tab.url,
                    loading = tab.loading,
                    onSubmit = onSubmit,
                    candidates = candidates,
                    onReload = { tab.session.reload() }, onHardReload = { hardReload(tab) }, progress = tab.progress,
                    onStop = { tab.session.stop() },
                    modifier = Modifier.widthIn(max = BrowserTokens.TabletAddressMaxWidth),
                    onSwipeUp = onSwipeUp,
                    onSwitchTab = onSwitchTab
                )
            }
            ToolbarButton(onClick = onShare, enabled = tab.url.isNotBlank(), description = "Share") {
                Icon(Icons.Filled.Share, contentDescription = null)
            }
            ToolbarButton(onClick = onDownloads, enabled = true, description = "Downloads") {
                Icon(Icons.Filled.Download, contentDescription = null)
            }
            ToolbarButton(onClick = onBookmark, enabled = tab.url.isNotBlank(), description = "Bookmark") {
                Icon(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = null
                )
            }
            ToolbarButton(onClick = onTabs, enabled = true, description = "Tabs") {
                TabsIcon(tabCount)
            }
            ToolbarButton(onClick = onNewTab, enabled = true, description = "New tab") {
                Icon(Icons.Filled.Add, contentDescription = null)
            }
            ToolbarButton(onClick = onMore, enabled = true, description = "More") {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.HairlineHeight)
                .background(browserColors().ToolbarBorder)
        )
    }
}

/** Compact layout: the address bar with a menu button. Tabs open from the menu or a swipe up. */
@Composable
private fun CompactToolbar(
    tab: BrowserTab,
    showSidebarToggle: Boolean,
    showSidebar: Boolean,
    onToggleSidebar: () -> Unit,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onMore: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwitchTab: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(browserColors().ToolbarBackground)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.HairlineHeight)
                .background(browserColors().ToolbarBorder)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(BrowserTokens.ToolbarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
        ) {
            if (showSidebarToggle) {
                ToolbarButton(onClick = onToggleSidebar, enabled = true, description = if (showSidebar) "Hide sidebar" else "Show sidebar") {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                }
            }
            AddressBar(
                currentUrl = tab.url,
                loading = tab.loading,
                onSubmit = onSubmit,
                candidates = candidates,
                onReload = { tab.session.reload() }, onHardReload = { hardReload(tab) }, progress = tab.progress,
                onStop = { tab.session.stop() },
                modifier = Modifier.weight(1f),
                onSwipeUp = onSwipeUp,
                onSwitchTab = onSwitchTab
            )
            ToolbarButton(onClick = onMore, enabled = true, description = "More") {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
        }
    }
}

/**
 * Phone Compact bar, modeled on iOS Safari: a floating row with a round back button, the address
 * pill in the middle, and a round menu button. No full-width strip sits behind it.
 */
@Composable
private fun PhoneCompactBar(
    tab: BrowserTab,
    /** True once the page has scrolled down: the round buttons fold away and the pill gets shorter. */
    shrunk: Boolean,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onMore: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwitchTab: (Int) -> Unit,
) {
    // 0 = full bar, 1 = shrunk. Shrinking uses the exit easing, growing back the enter easing.
    val shrink by animateFloatAsState(
        targetValue = if (shrunk) 1f else 0f,
        animationSpec = if (shrunk) OraMotion.exit<Float>() else OraMotion.enter<Float>(),
        label = "compactShrink"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            .padding(
                start = BrowserTokens.PhoneCompactPadding,
                end = BrowserTokens.PhoneCompactPadding,
                top = BrowserTokens.PhoneCompactPadding,
                bottom = lerp(BrowserTokens.PhoneCompactFloatBottom, BrowserTokens.PhoneCompactShrunkBottom, shrink)
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(lerp(BrowserTokens.PhoneCompactGap, 0.dp, shrink))
    ) {
        // Each round button sits in a box that narrows to nothing, so it folds away instead of vanishing.
        Box(modifier = Modifier.width(lerp(BrowserTokens.PhoneCompactCircle, 0.dp, shrink)).clipToBounds()) {
            FloatingCircleButton(
                onClick = { tab.stepBack() },
                enabled = tab.canStepBack && shrink < 0.5f,
                description = "Back",
                // Quality of life: a long press on Back jumps straight to the start page.
                onLongClick = {
                    BrowserLog.info("nav", "Long-press Back: opened start page from ${tab.url}")
                    // The start page is native, so set the address directly; GeckoView cannot load ora://.
                    tab.url = BrowserTabs.START_URL
                }
            ) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null)
            }
        }
        // The middle slot takes the free width and centres the pill. Shrinking narrows the pill around the
        // centre, as Safari does, instead of letting it widen into the space the round buttons leave.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            AddressBar(
                currentUrl = tab.url,
                loading = tab.loading,
                onSubmit = onSubmit,
                candidates = candidates,
                onReload = { tab.session.reload() }, onHardReload = { hardReload(tab) }, progress = tab.progress,
                onStop = { tab.session.stop() },
                modifier = Modifier.fillMaxWidth(1f + (BrowserTokens.PhoneCompactShrunkWidthFraction - 1f) * shrink),
                height = lerp(BrowserTokens.AddressBarHeight, BrowserTokens.PhoneCompactShrunkHeight, shrink),
                onSwipeUp = onSwipeUp,
                onSwitchTab = onSwitchTab,
                shape = BrowserTokens.PhoneCompactPill,
                centered = true
            )
        }
        Box(modifier = Modifier.width(lerp(BrowserTokens.PhoneCompactCircle, 0.dp, shrink)).clipToBounds()) {
            FloatingCircleButton(onClick = onMore, enabled = shrink < 0.5f, description = "More") {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
        }
    }
}

/** A round, filled button that floats beside the address pill (phone Compact bar). */
@Composable
private fun FloatingCircleButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    onLongClick: (() -> Unit)? = null,
    icon: @Composable () -> Unit,
) {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .size(BrowserTokens.PhoneCompactCircle)
            .shadow(BrowserTokens.PhoneCompactElevation, CircleShape)
            .clip(CircleShape)
            .background(colors.AddressBarBackground)
            .combinedClickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = description,
                onClick = onClick,
                onLongClickLabel = if (onLongClick != null) "$description: start page" else null,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) colors.Accent else colors.DisabledIcon
        ) {
            icon()
        }
    }
}

/**
 * Address bar gestures that never consume the touch, so text selection keeps working:
 * a swipe up calls [onSwipeUp]; a sideways swipe calls [onSwitchTab] with +1 (left) or -1 (right).
 */
private fun Modifier.addressSwipes(onSwipeUp: () -> Unit, onSwitchTab: (Int) -> Unit): Modifier =
    pointerInput(onSwipeUp, onSwitchTab) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var dx = 0f
            var dy = 0f
            var released = false
            while (!released) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                val move = change.positionChange()
                dx += move.x
                dy += move.y
                if (!change.pressed) released = true
            }
            val threshold = 96f
            if (abs(dy) > abs(dx) && dy < -threshold) {
                BrowserLog.info("tabs", "Swipe up on address bar: all tabs")
                onSwipeUp()
            } else if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                onSwitchTab(if (dx < 0) 1 else -1)
            }
        }
    }

/** Reloads the page ignoring cached copies. Logged so reloads show on the Diagnostics page. */
private fun hardReload(tab: BrowserTab) {
    BrowserLog.info("nav", "Reload without cache on tab ${tab.id}")
    tab.session.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE)
}

@Composable
private fun AddressBar(
    currentUrl: String,
    loading: Boolean,
    /** Load progress from 0 to 100, drawn as the load bar while the page loads. */
    progress: Int = 100,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onReload: () -> Unit,
    /** Long press on reload: reload past the cache. */
    onHardReload: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
    /** Height of the pill. The phone Compact bar lowers it while the page is scrolled. */
    height: Dp = BrowserTokens.AddressBarHeight,
    onSwipeUp: () -> Unit = {},
    onSwitchTab: (Int) -> Unit = {},
    shape: androidx.compose.ui.graphics.Shape = BrowserTokens.AddressBarShape,
    /** Centres the URL in the pill (phone Compact bar). */
    centered: Boolean = false,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(displayUrl(currentUrl))) }
    var focused by remember { mutableStateOf(false) }
    // Set when the field gains focus so the whole address is selected; cleared once the user edits.
    var selectAllPending by remember { mutableStateOf(false) }
    // A tap outside the suggestions closes them only. It must not drop focus, or the clear button and typed text reset.
    var suggestionsHidden by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    // Quality of life: once a search or address is submitted, the keyboard closes instead of staying up.
    val finishEditing = {
        view.hideKeyboard()
        keyboard?.hide()
        focusManager.clearFocus()
        focused = false
    }
    var anchorWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // Show the current page's host while the user is not typing in the field.
    LaunchedEffect(currentUrl, focused) {
        if (!focused) fieldValue = TextFieldValue(displayUrl(currentUrl))
    }

    // What the user has typed, without any autocompleted tail that is still selected.
    val typed = fieldValue.text.substring(0, fieldValue.selection.min.coerceIn(0, fieldValue.text.length))
    val suggestions = if (focused && !suggestionsHidden) addressSuggestions(typed, candidates) else emptyList()

    Box(
        modifier = modifier.addressSwipes(onSwipeUp, onSwitchTab).onGloballyPositioned { anchorWidth = with(density) { it.size.width.toDp() } }
    ) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .shadow(BrowserTokens.AddressBarElevation, shape),
        shape = shape,
        color = browserColors().AddressBarBackground
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = BrowserTokens.AddressBarInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentUrl.startsWith("https://")) {
                Spacer(Modifier.width(BrowserTokens.AddressBarTextStart))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Secure connection",
                    modifier = Modifier.size(BrowserTokens.LockIconSize),
                    tint = browserColors().SecondaryText
                )
            }
            Spacer(Modifier.width(BrowserTokens.AddressBarTextStart))
            BasicTextField(
                value = fieldValue,
                onValueChange = { next ->
                    // Right after focus the input method or a tap can collapse the selection (to the start, or
                    // to the tapped spot), so typing landed mid-URL and Backspace did nothing. While the
                    // select-all is pending, ignore any collapse that does not change the text.
                    if (selectAllPending && next.text == fieldValue.text && next.selection.collapsed) {
                        return@BasicTextField
                    }
                    selectAllPending = false
                    suggestionsHidden = false
                    val typing = next.text.length > fieldValue.text.length
                    val completion = if (typing) autocompleteFor(next.text, candidates) else null
                    fieldValue = if (completion != null) {
                        TextFieldValue(completion, TextRange(next.text.length, completion.length))
                    } else {
                        next
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        if (state.isFocused) {
                            suggestionsHidden = false
                            selectAllPending = true
                            fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(
                    color = browserColors().AddressBarText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start
                ),
                cursorBrush = SolidColor(browserColors().Accent),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        finishEditing()
                        onSubmit(fieldValue.text)
                    }
                )
            )
            // Autocomplete: when typing forward, finish a known domain and select the added part.
            // Deleting never autocompletes, so backspace behaves normally.
            // Quality of life: one tap clears the whole address while editing, and keeps the keyboard up.
            if (focused && fieldValue.text.isNotEmpty()) {
                // Not focusable, so tapping it keeps the field focused and the URL does not refill it.
                IconButton(
                    modifier = Modifier.focusProperties { canFocus = false },
                    onClick = {
                        BrowserLog.info("search", "Address cleared")
                        selectAllPending = false
                        fieldValue = TextFieldValue("")
                    }
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear address", tint = browserColors().SecondaryText)
                }
            }
            if (loading) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop", tint = browserColors().Accent)
                }
            } else {
                // Quality of life: a long press reloads past the cache, for pages that look stale.
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .combinedClickable(onClick = onReload, onLongClick = onHardReload, onClickLabel = "Reload", onLongClickLabel = "Reload without cache"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = browserColors().AddressBarText)
                }
            }
        }
    }
    // Load bar: a thin line along the bottom edge fills as the page loads and fades out when it is done.
    // The fill only moves forward: a new load snaps back to empty rather than sweeping backwards.
    // It fades in after a short delay, so pages that finish quickly never flash it.
    val loadFraction = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        val target = progress / 100f
        if (target < loadFraction.value) loadFraction.snapTo(target)
        else loadFraction.animateTo(target, OraMotion.enter())
    }
    Box(
        modifier = Modifier.matchParentSize().clip(shape),
        contentAlignment = Alignment.BottomStart
    ) {
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(OraMotion.enterAfterLoadDelay()),
            exit = fadeOut(OraMotion.exit())
        ) {
            LinearProgressIndicator(
                progress = { loadFraction.value },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrowserTokens.LoadBarHeight),
                color = browserColors().Accent,
                trackColor = Color.Transparent
            )
        }
    }
    // Suggestions drop down under the field. Not focusable, so the keyboard stays open while they show.
    DropdownMenu(
        expanded = focused && suggestions.isNotEmpty(),
        onDismissRequest = { suggestionsHidden = true },
        modifier = Modifier.width(anchorWidth),
        properties = PopupProperties(focusable = false)
    ) {
        suggestions.forEach { suggestion ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            suggestion.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 15.sp,
                            color = browserColors().AddressBarText
                        )
                        Text(
                            suggestion.subtitle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            color = browserColors().SecondaryText
                        )
                    }
                },
                onClick = {
                    finishEditing()
                    BrowserLog.info("search", "Suggestion chosen: ${suggestion.target}")
                    onSubmit(suggestion.target)
                }
            )
        }
    }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(BrowserTokens.ToolbarButtonSize)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) browserColors().Accent else browserColors().DisabledIcon
        ) {
            icon()
        }
    }
}

/** Turns typed text into a URL: full URLs as-is, bare domains get https, anything else is searched. */
internal fun toLoadableUrl(input: String, searchTemplate: String): String {
    val text = input.trim()
    return when {
        text.isEmpty() -> ""
        Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*").matches(text) -> text
        !text.contains(' ') && text.contains('.') -> "https://$text"
        else -> searchTemplate.replace("%s", URLEncoder.encode(text, "UTF-8"))
    }
}

/** The part of a URL shown in the address bar when not editing, e.g. "mozilla.org". */
internal fun displayUrl(url: String): String =
    if (url == BrowserTabs.START_URL) "Start page" else runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url

private fun shareUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, null))
}

/** GeckoView can report a scroll of a pixel or two at the very top; that still counts as the top for a pull. */
private const val PULL_SCROLL_TOLERANCE_PX = 4

/** "Tab closed · Undo" card shown after a tab is closed. Grows in and shrinks back, like the other overlays. */
@Composable
private fun UndoCloseOffer(visible: Boolean, onUndo: () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(OraMotion.enter()) + scaleIn(OraMotion.enter(), initialScale = OraMotion.MORPH_SCALE),
        exit = fadeOut(OraMotion.exit()) + scaleOut(OraMotion.exit(), targetScale = OraMotion.MORPH_SCALE),
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                .background(browserColors().Surface)
                .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Tab closed", color = browserColors().AddressBarText, fontSize = 15.sp)
            TextButton(onClick = {
                BrowserLog.info("tabs", "Undo close from the tab grid")
                onUndo()
            }) {
                Text("Undo", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Scale the tabs icon jumps to when a tab opens, before settling back to normal. */
private const val TAB_BUMP_SCALE = 1.25f

/** How long the "Tab closed · Undo" offer stays up. */
private const val UNDO_CLOSE_MS = 5000L
