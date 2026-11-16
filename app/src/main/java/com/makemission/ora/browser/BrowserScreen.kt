package com.makemission.ora.browser

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
import com.makemission.ora.ui.theme.OraMotion
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.layout.onGloballyPositioned
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.makemission.ora.bookmarks.BookmarksRepository
import com.makemission.ora.extensions.ExtensionsRepository
import com.makemission.ora.extensions.ExtensionsScreen
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
import com.makemission.ora.log.BrowserLog
import androidx.compose.foundation.isSystemInDarkTheme
import java.net.URLEncoder
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.StorageController
import android.widget.Toast

/** Screens narrower than this use the phone layout (tab grid); wider ones use the sidebar layout. */
private const val TABLET_MIN_WIDTH_DP = 600

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
    bookmarks: BookmarksRepository,
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
        BrowserContent(settings, bookmarks, modifier)
    }
}

/**
 * Phone: full-screen page, bottom toolbar, tab grid on demand.
 * Tablet: tab/bookmark sidebar on the left, page and bottom toolbar on the right.
 */
@Composable
private fun BrowserContent(
    settings: SettingsRepository,
    bookmarks: BookmarksRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homePage by settings.homePage.collectAsState()
    val bookmarkList by bookmarks.bookmarks.collectAsState()
    val isTablet = LocalConfiguration.current.screenWidthDp >= TABLET_MIN_WIDTH_DP

    val tabs = remember(homePage) { BrowserTabs(context, homePage) }
    var showTabGrid by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showExtensions by remember { mutableStateOf(false) }
    val extensions = remember { ExtensionsRepository(GeckoEngine.runtime(context)) }
    val searchEngines by settings.searchEngines.collectAsState()
    val defaultEngine by settings.defaultSearchEngine.collectAsState()
    /** Search text waiting for the user to pick an engine. Null when the chooser is closed. */
    var pendingSearch by remember { mutableStateOf<String?>(null) }
    val pageView = remember { PageView() }

    val selected = tabs.selected ?: return
    val selectedId = tabs.selectedId

    // Only the selected tab should be active, so background pages stop rendering.
    LaunchedEffect(selectedId) {
        tabs.tabs.forEach { it.session.setActive(it.id == selectedId) }
    }

    val onNewTab: () -> Unit = { tabs.newTab(homePage) }
    /** Tablet only: whether the tab and bookmark sidebar is visible. Saved in settings. */
    val showSidebar by settings.tabletSidebar.collectAsState()
    /** Bookmarks and open tabs, offered as address bar suggestions. */
    val addressCandidates = (
        bookmarkList.map { AddressSuggestion(it.title, displayUrl(it.url), it.url) } +
            tabs.tabs.map { AddressSuggestion(it.title.ifBlank { displayUrl(it.url) }, displayUrl(it.url), it.url) }
        ).distinctBy { it.target }

    /** Loads typed text: searches through the chooser or default engine, or opens a URL. */
    fun submitAddress(input: String) {
        if (isSearchInput(input) && searchEngines.size > 1) {
            pendingSearch = input.trim()
        } else if (isSearchInput(input)) {
            val engine = defaultEngine
            BrowserLog.info("search", "Searching ${engine.label} for \"${input.trim()}\"")
            selected.session.loadUri(engine.urlFor(input.trim()))
        } else {
            selected.session.loadUri(toLoadableUrl(input, defaultEngine.template))
        }
    }

    // Full-screen phone overlay currently open (tablets never use one).
    val phoneOverlay = when {
        isTablet -> PhoneOverlay.NONE
        showTabGrid -> PhoneOverlay.TABS
        showLibrary -> PhoneOverlay.LIBRARY
        showSettings -> PhoneOverlay.SETTINGS
        showExtensions -> PhoneOverlay.EXTENSIONS
        else -> PhoneOverlay.NONE
    }

    // Back closes the open overlay first, then steps back through page history, and only then exits.
    BackHandler(enabled = phoneOverlay != PhoneOverlay.NONE) {
        showTabGrid = false
        showLibrary = false
        showSettings = false
        showExtensions = false
    }
    BackHandler(enabled = phoneOverlay == PhoneOverlay.NONE && selected.canGoBack) {
        selected.session.goBack()
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            // Tablet keeps every control on one top toolbar (design: D-Browse).
            if (isTablet) {
                TabletToolbar(
                    tab = selected,
                    isBookmarked = bookmarkList.any { it.url == selected.url },
                    showSidebar = showSidebar,
                    onToggleSidebar = { settings.setTabletSidebar(!showSidebar) },
                    onSubmit = { input -> submitAddress(input) },
                    candidates = addressCandidates,
                    onBookmark = { bookmarks.toggle(selected.url, selected.title) },
                    onShare = { shareUrl(context, selected.url) },
                    onNewTab = onNewTab
                )
            }
        },
        bottomBar = {
            if (!isTablet) {
            // The toolbar folds down into its bottom edge while an overlay grows out of the tabs button.
            AnimatedVisibility(
                visible = phoneOverlay == PhoneOverlay.NONE,
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = OraMotion.exit())
            ) {
                BrowserToolbar(
                    tab = selected,
                    isBookmarked = bookmarkList.any { it.url == selected.url },
                    onSubmit = { input -> submitAddress(input) },
                    candidates = addressCandidates,
                    onBookmark = { bookmarks.toggle(selected.url, selected.title) },
                    onShare = { shareUrl(context, selected.url) },
                    onTabs = if (isTablet) onNewTab else {
                        {
                            // Refresh the current tab's preview so the grid shows the page as it is now.
                            captureThumbnail(pageView, selected)
                            showTabGrid = true
                        }
                    },
                    onMenu = if (isTablet) null else { { showMenu = true } }
                )
            }
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // The sidebar slides in and out from its left edge, using the shared overlay motion.
            AnimatedVisibility(
                visible = isTablet && showSidebar,
                enter = fadeIn(OraMotion.enter()) + expandHorizontally(expandFrom = Alignment.Start, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = OraMotion.exit())
            ) {
                TabSidebar(
                    tabs = tabs,
                    bookmarks = bookmarkList,
                    onNewTab = onNewTab,
                    onOpenBookmark = { url -> selected.session.loadUri(url) }
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                BrowserPage(tab = selected, pageView = pageView)
                if (showMenu && !isTablet) {
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
                            onClearData = {
                                GeckoEngine.runtime(context).storageController
                                    .clearData(StorageController.ClearFlags.ALL)
                                    .accept(
                                        { Toast.makeText(context, "Browsing data cleared", Toast.LENGTH_SHORT).show() },
                                        { Toast.makeText(context, "Could not clear browsing data", Toast.LENGTH_SHORT).show() }
                                    )
                            }
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
                        PhoneOverlay.TABS -> TabGrid(
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
                        PhoneOverlay.EXTENSIONS -> ExtensionsScreen(
                            repository = extensions,
                            onDone = { showExtensions = false }
                        )
                        PhoneOverlay.NONE -> Unit
                    }
                }
            }
        }
    }
}

/** Shows the selected tab's page. The single GeckoView is re-pointed at whichever session is selected. */
@Composable
private fun BrowserPage(tab: BrowserTab, pageView: PageView) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> GeckoView(ctx).also { pageView.view = it } },
        update = { view -> if (view.session != tab.session) view.setSession(tab.session) }
    )
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
        Column(
            modifier = Modifier.padding(BrowserTokens.ToolbarPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
        ) {
            // Top row: back, forward and the address pill (design: M-Browse).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
            ) {
                ToolbarButton(onClick = { tab.session.goBack() }, enabled = tab.canGoBack, description = "Back") {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                AddressBar(
                    currentUrl = tab.url,
                    loading = tab.loading,
                    onSubmit = onSubmit,
                    candidates = candidates,
                    onReload = { tab.session.reload() },
                    onStop = { tab.session.stop() },
                    modifier = Modifier.weight(1f)
                )
                ToolbarButton(onClick = { tab.session.goForward() }, enabled = tab.canGoForward, description = "Forward") {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
            // Bottom row: share, bookmark, tabs (design: bottom toolbar).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    Icon(Icons.Outlined.Tab, contentDescription = null)
                }
                if (onMenu != null) {
                    ToolbarButton(onClick = onMenu, enabled = true, description = "Menu") {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                }
            }
        }
    }
}

/**
 * Tablet top toolbar (design: D-Browse): sidebar toggle, back, forward, a centered
 * address pill, then share, bookmark and new tab.
 */
@Composable
private fun TabletToolbar(
    tab: BrowserTab,
    isBookmarked: Boolean,
    showSidebar: Boolean,
    onToggleSidebar: () -> Unit,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onNewTab: () -> Unit,
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
            ToolbarButton(onClick = onToggleSidebar, enabled = true, description = if (showSidebar) "Hide sidebar" else "Show sidebar") {
                Icon(Icons.Filled.Menu, contentDescription = null)
            }
            ToolbarButton(onClick = { tab.session.goBack() }, enabled = tab.canGoBack, description = "Back") {
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
                    onReload = { tab.session.reload() },
                    onStop = { tab.session.stop() },
                    modifier = Modifier.widthIn(max = BrowserTokens.TabletAddressMaxWidth)
                )
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
            ToolbarButton(onClick = onNewTab, enabled = true, description = "New tab") {
                Icon(Icons.Filled.Add, contentDescription = null)
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

@Composable
private fun AddressBar(
    currentUrl: String,
    loading: Boolean,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onReload: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(displayUrl(currentUrl))) }
    var focused by remember { mutableStateOf(false) }
    var anchorWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // Show the current page's host while the user is not typing in the field.
    LaunchedEffect(currentUrl, focused) {
        if (!focused) fieldValue = TextFieldValue(displayUrl(currentUrl))
    }

    // What the user has typed, without any autocompleted tail that is still selected.
    val typed = fieldValue.text.substring(0, fieldValue.selection.min.coerceIn(0, fieldValue.text.length))
    val suggestions = if (focused) addressSuggestions(typed, candidates) else emptyList()

    Box(
        modifier = modifier.onGloballyPositioned { anchorWidth = with(density) { it.size.width.toDp() } }
    ) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.AddressBarHeight)
            .shadow(BrowserTokens.AddressBarElevation, BrowserTokens.AddressBarShape),
        shape = BrowserTokens.AddressBarShape,
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
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        if (state.isFocused) {
                            fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(
                    color = browserColors().AddressBarText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(browserColors().Accent),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        onSubmit(fieldValue.text)
                        focused = false
                    }
                )
            )
            // Autocomplete: when typing forward, finish a known domain and select the added part.
            // Deleting never autocompletes, so backspace behaves normally.
            if (loading) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop", tint = browserColors().Accent)
                }
            } else {
                IconButton(onClick = onReload) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = browserColors().AddressBarText)
                }
            }
        }
    }
    // Suggestions drop down under the field. Not focusable, so the keyboard stays open while they show.
    DropdownMenu(
        expanded = focused && suggestions.isNotEmpty(),
        onDismissRequest = { focused = false },
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
                    focused = false
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
    runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url

private fun shareUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
