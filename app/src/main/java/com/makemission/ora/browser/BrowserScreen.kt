package com.makemission.ora.browser

import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.animation.core.Animatable
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Color
import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.core.view.WindowCompat
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.TextButton
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.combinedClickable
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import android.view.View
import androidx.compose.ui.viewinterop.AndroidView
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.bookmarks.BookmarksRepository
import com.makemission.ora.history.HistoryRepository
import com.makemission.ora.profiles.Profile
import com.makemission.ora.profiles.ProfileSwitcher
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.extensions.ExtensionsRepository
import com.makemission.ora.extensions.ExtensionsScreen
import com.makemission.ora.settings.AddressSwipeEffect
import com.makemission.ora.settings.AddressSwipeTarget
import com.makemission.ora.settings.BarStyle
import com.makemission.ora.security.AppLock
import com.makemission.ora.security.LockScreen
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.DarkBrowserColors
import com.makemission.ora.ui.theme.LightBrowserColors
import com.makemission.ora.ui.theme.LocalBrowserColors
import com.makemission.ora.ui.theme.ProfileTint
import com.makemission.ora.ui.theme.browserColors
import com.makemission.ora.ui.theme.PaletteStyle
import com.makemission.ora.ui.theme.materialBrowserColors
import com.makemission.ora.ui.theme.oraColorScheme
import com.makemission.ora.ui.theme.PureDarkBrowserColors
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.PrivateSpace
import com.makemission.ora.ui.theme.dashedOutline
import com.makemission.ora.ui.theme.privateBrowserColors
import androidx.compose.runtime.SideEffect
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
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.permissions.SitePermissionStore
import com.makemission.ora.permissions.SitePromptOverlay
import com.makemission.ora.permissions.SitePrompts
import com.makemission.ora.ui.pressFeedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
    /** The bottom space last given to the GeckoView, so it is only set again when the floating row changes height. */
    var appliedClip = 0
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
    val paletteStyle by settings.paletteStyle.collectAsState()
    val original = when (appearance) {
        AppearanceMode.PURE_DARK -> PureDarkBrowserColors
        AppearanceMode.DARK -> DarkBrowserColors
        AppearanceMode.LIGHT -> LightBrowserColors
        AppearanceMode.AUTO -> if (isSystemInDarkTheme()) DarkBrowserColors else LightBrowserColors
    }
    val isDark = original == DarkBrowserColors || original == PureDarkBrowserColors
    // Material controls use the Material scheme for the same light or dark mode as the chosen appearance.
    val context = LocalContext.current
    val materialScheme = remember(isDark) { oraColorScheme(context, isDark) }
    // The active profile's tint recolours Ora's own surfaces in Dark (never web pages).
    val profileList by profiles.profiles.collectAsState()
    val activeProfileId by profiles.activeId.collectAsState()
    val activeTint = profileList.firstOrNull { it.id == activeProfileId }?.tint ?: ProfileTint.BASALT
    val tinted = browserColors(original, activeTint)
    // The private space overrides the tint with Obsidian while a private tab is selected.
    val spaced = if (PrivateSpace.active) privateBrowserColors(tinted) else tinted
    val palette = if (paletteStyle == PaletteStyle.MATERIAL) {
        materialBrowserColors(materialScheme, spaced)
    } else {
        spaced
    }
    // Web pages follow the app's light or dark palette (or the system setting when the toggle is off).
    val websiteTheme by settings.websiteTheme.collectAsState()
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
        // Drawn inside the palette so the lock follows Light, Dark and Pure dark like everything else.
        val wholeApp by settings.appLockWholeApp.collectAsState()
        LockScreen(settings, visible = AppLock.locked && wholeApp)
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
    // History is per profile as well. One list per profile is shared by its tabs and its views, so they never disagree.
    val historyByProfile = remember { HashMap<String, HistoryRepository>() }
    fun historyFor(id: String): HistoryRepository = historyByProfile.getOrPut(id) { HistoryRepository(context, id) }
    val history = remember(profileId) { historyFor(profileId) }
    val historyList by history.entries.collectAsState()
    val saveHistory by settings.saveHistory.collectAsState()
    // Site permission answers are per profile too, the same as history.
    val sitePermissionStores = remember { HashMap<String, SitePermissionStore>() }
    fun sitePermissionsFor(id: String): SitePermissionStore =
        sitePermissionStores.getOrPut(id) { SitePermissionStore(context, id) }

    val blockAds by settings.blockAds.collectAsState()
    val videoGestures by settings.videoGestures.collectAsState()
    val videoBackExits by settings.videoBackExits.collectAsState()
    val cookieBehavior by settings.cookieBehavior.collectAsState()
    val fadePagesIn by settings.fadePagesIn.collectAsState()
    val secureDns by settings.secureDns.collectAsState()
    val httpsOnly by settings.httpsOnly.collectAsState()
    LaunchedEffect(httpsOnly) {
        GeckoEngine.runtime(context).settings.setAllowInsecureConnections(
            if (httpsOnly) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL
        )
    }
    val globalPrivacyControl by settings.globalPrivacyControl.collectAsState()
    val fingerprintingProtection by settings.fingerprintingProtection.collectAsState()
    val downloads = remember { DownloadsRepository(context) }
    var showDownloads by remember { mutableStateOf(false) }
    val tabsByProfile = remember { HashMap<String, BrowserTabs>() }
    val sessionStore = remember { SessionStore(context) }
    val quickResume by settings.quickResume.collectAsState()
    val tabs = remember(profileId) {
        tabsByProfile.getOrPut(profileId) {
            BrowserTabs(
                context,
                { settings.blockPopups.value },
                { response, pageUrl -> downloads.start(response, pageUrl) },
                desktopByDefault = isTablet,
                // Quick resume: a profile's tabs reopen from the last time they were saved, when the setting is on.
                restore = if (settings.quickResume.value) sessionStore.load(profileId) else null,
                // Finished pages are listed in this profile's history, when saving is on.
                onVisited = { url, title -> if (settings.saveHistory.value) historyFor(profileId).record(url, title) },
                sitePermissions = sitePermissionsFor(profileId),
                groupStore = TabGroupStore(context, profileId),
            )
        }
    }
    // Quick resume: when the app leaves the screen, every profile open this session has its tabs saved.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && settings.quickResume.value) {
                tabsByProfile.forEach { (id, profileTabs) ->
                    val saved = profileTabs.savableSnapshot()
                    sessionStore.save(id, saved.tabs, saved.selected)
                }
                BrowserLog.info("session", "Saved tabs for ${tabsByProfile.size} profile(s)")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // With quick resume off, nothing is kept on disk: forget any saved tabs.
    LaunchedEffect(quickResume) {
        if (!quickResume) sessionStore.clear()
    }
    // A web link handed to Ora by another app opens in a new tab and becomes the selected one.
    val incomingLink by ExternalLinks.pending.collectAsState()
    LaunchedEffect(incomingLink) {
        val url = incomingLink ?: return@LaunchedEffect
        ExternalLinks.consume()
        BrowserLog.info("page", "Opened link from another app: $url")
        tabs.newTab(url)
        InAppNotices.show("Link opened from another app, in a new tab")
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
    // Cookie policy is a runtime setting: GeckoView enforces it for every session.
    LaunchedEffect(cookieBehavior) {
        GeckoEngine.runtime(context).settings.contentBlocking.setCookieBehavior(cookieBehavior)
    }
    // Secure DNS is a runtime setting: TRR_MODE_FIRST tries encrypted DNS and falls back to system DNS on failure.
    LaunchedEffect(secureDns) {
        GeckoEngine.runtime(context).settings.setTrustedRecursiveResolverMode(
            if (secureDns) GeckoRuntimeSettings.TRR_MODE_FIRST else GeckoRuntimeSettings.TRR_MODE_OFF
        )
    }
    // Global Privacy Control and fingerprinting protection are runtime settings applied to every session.
    LaunchedEffect(globalPrivacyControl) {
        GeckoEngine.runtime(context).settings.setGlobalPrivacyControl(globalPrivacyControl)
    }
    LaunchedEffect(fingerprintingProtection) {
        GeckoEngine.runtime(context).settings.setFingerprintingProtection(fingerprintingProtection)
    }
    // Password autofill is a runtime setting: GeckoView fills login forms from saved logins when it is on.
    val autofillPasswords by settings.autofillPasswords.collectAsState()
    LaunchedEffect(autofillPasswords) {
        GeckoEngine.runtime(context).settings.setLoginAutofillEnabled(autofillPasswords)
    }
    // Lab: the device font on web pages. Turning web fonts off makes pages use the browser's default font.
    val deviceFontWeb by settings.deviceFontWeb.collectAsState()
    LaunchedEffect(deviceFontWeb) {
        GeckoEngine.runtime(context).settings.setWebFontsEnabled(!deviceFontWeb)
    }
    // Text size on web pages. GeckoView scales page text by this factor once automatic adjustment is off.
    val textSize by settings.textSizePercent.collectAsState()
    LaunchedEffect(textSize) {
        val engine = GeckoEngine.runtime(context).settings
        engine.setAutomaticFontSizeAdjustment(false)
        engine.setFontSizeFactor(textSize / 100f)
        BrowserLog.info("page", "Page text size set to $textSize%")
    }
    var showTabGrid by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    /** Tablet: each tap on ⋯ while the panel is open adds one, which slides it closed. */
    var menuCloseRequests by remember { mutableIntStateOf(0) }
    /** The tablet ⋯ button opens the menu, or closes it when it is already open. */
    val toggleMenu: () -> Unit = { if (showMenu) menuCloseRequests++ else showMenu = true }
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

    val appLockSet = settings.appLockRecord.collectAsState().value != null
    val selected = tabs.selected ?: return
    SideEffect { PrivateSpace.active = selected.isPrivate }
    val selectedId = tabs.selectedId
    // One place for the bookmark state and toggle, shared by the phone and tablet bars and the menu.
    val selectedBookmarked = bookmarkList.any { it.url == selected.url }
    val toggleSelectedBookmark = { bookmarks.toggle(selected.url, selected.title) }

    // Only the selected tab should be active, so background pages stop rendering.
    LaunchedEffect(selectedId) {
        tabs.tabs.forEach { it.session.setActive(it.id == selectedId) }
    }

    /** A private tab: GeckoView keeps no history, cookies or thumbnails for it. */
    val onNewPrivateTab: () -> Unit = {
        UsageStats.count("tab.new.private")
        tabs.newTab(BrowserTabs.START_URL, isPrivate = true)
    }
    val onNewTab: () -> Unit = {
        UsageStats.count("tab.new")
        tabs.newTab(BrowserTabs.START_URL)
    }
    /** Brings back the last closed tab, or says so when there is none. */
    val onReopenClosed: () -> Unit = {
        UsageStats.count("tab.reopen")
        if (!tabs.reopenLastClosed()) InAppNotices.show("No closed tabs to reopen")
    }
    val phoneBar by settings.phoneBarStyle.collectAsState()
    val tabletBar by settings.tabletBarStyle.collectAsState()
    val barStyle = if (isTablet) tabletBar else phoneBar
    /** Opens the tab grid, refreshing the current tab's preview first. */
    val openTabs: () -> Unit = {
        captureThumbnail(pageView, selected)
        showTabGrid = true
    }
    // A sideways swipe on the address bar moves through open tabs, the tab's pages or profiles, as the Settings choice
    // says, and the page moves with the chosen effect instead of cutting over. A swipe with nowhere to go bounces.
    val swipeMotion = rememberAddressSwipeMotion()
    val swipeScope = rememberCoroutineScope()
    val swipeTarget by settings.addressSwipeTarget.collectAsState()
    val swipeEffect by settings.addressSwipeEffect.collectAsState()
    /** Moves by +1 (a swipe to the left, next) or -1 (a swipe to the right, previous), wrapping at the ends. */
    val switchTab: (Int) -> Unit = { delta ->
        swipeScope.launch {
            when (swipeTarget) {
                AddressSwipeTarget.TABS -> {
                    val list = tabs.tabs
                    val canSwitch = list.size > 1
                    val index = list.indexOfFirst { it.id == selectedId }
                    val next = if (canSwitch) list[Math.floorMod(index + delta, list.size)] else null
                    UsageStats.gesture("address_swipe.tabs", if (canSwitch) "moved" else "bounced", if (canSwitch) null else "only_one_tab")
                    swipeMotion.play(delta, swipeEffect, canSwitch) {
                        next?.let {
                            tabs.select(it.id)
                            BrowserLog.info("tabs", "Swiped to tab ${it.id}")
                        }
                    }
                }
                AddressSwipeTarget.PAGES -> {
                    val canSwitch = if (delta > 0) selected.canGoForward else selected.canStepBack
                    UsageStats.gesture("address_swipe.pages", if (canSwitch) "moved" else "bounced", if (canSwitch) null else if (delta > 0) "no_forward_page" else "no_back_page")
                    swipeMotion.play(delta, swipeEffect, canSwitch) {
                        if (delta > 0) selected.session.goForward() else selected.stepBack()
                        BrowserLog.info("tabs", "Swiped to the ${if (delta > 0) "next" else "previous"} page in tab ${selected.id}")
                    }
                }
                AddressSwipeTarget.PROFILES -> {
                    val list = profileList
                    val canSwitch = list.size > 1
                    UsageStats.gesture("address_swipe.profiles", if (canSwitch) "moved" else "bounced", if (canSwitch) null else "only_one_profile")
                    val index = list.indexOfFirst { it.id == profiles.activeId.value }
                    val next = if (canSwitch) list[Math.floorMod(index + delta, list.size)] else null
                    swipeMotion.play(delta, swipeEffect, canSwitch) {
                        next?.let { profiles.select(it.id) }
                    }
                }
            }
        }
    }
    // Where the bar sits. Tablet Compact sits at the top like a desktop address bar; phone Compact stays at the bottom.
    val topLayout = barStyle == BarStyle.TOP
    /** Tablet Compact floats over the top of the page, the way the phone Compact bar floats at the bottom. */
    val tabletCompact = isTablet && barStyle == BarStyle.COMPACT
    /** Phone Compact: the floating row sits at the bottom, with the page running under the status bar and the row. */
    val phoneCompact = !isTablet && barStyle == BarStyle.COMPACT
    /** Phone Compact: the height of the status bar, the space the page starts below at the top of the page. */
    val phoneZoneHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    /** Tablet only: whether the tab and bookmark sidebar is visible. Saved in settings. */
    val showSidebar by settings.tabletSidebar.collectAsState()
    val edgeSwipeSidebar by settings.edgeSwipeSidebar.collectAsState()
    val density = LocalDensity.current
    /** Height of the top bar, measured so the tablet side panel starts below it. */
    var topBarHeight by remember { mutableStateOf(0.dp) }
    // Tablet Compact floats over the page, so the sidebar reserves this much room at its top.
    var compactRowHeight by remember { mutableStateOf(0.dp) }
    // Phone Compact: the floating row's measured height in pixels. GeckoView is told it, so the page ends above the row.
    var bottomBarPx by remember { mutableIntStateOf(0) }
    /**
     * The colour behind the status bar: the start page's grey on the start page, otherwise the page's own top colour
     * once it has loaded, and the theme background until one is known. It eases between colours with the shared enter motion.
     */
    val stripColor = if (selected.url == BrowserTabs.START_URL) {
        browserColors().GridBackground
    } else {
        selected.topColor ?: MaterialTheme.colorScheme.background
    }
    val barColor by animateColorAsState(stripColor, OraMotion.enter(), label = "status strip")
    // Dark icons on a light strip, light icons on a dark one. Set on the window so the system bar follows the page.
    val statusWindow = (context as? Activity)?.window
    LaunchedEffect(stripColor, statusWindow) {
        statusWindow?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars =
                isLightStatusStrip(stripColor)
        }
    }
    // Read the page's top row once it has loaded and is not scrolled; a scrolled page no longer shows its header colour.
    LaunchedEffect(selected.id, selected.loading, selected.scrolled) {
        if (selected.url != BrowserTabs.START_URL && !selected.loading && !selected.scrolled) {
            pageView.view?.capturePixels()?.accept(
                { bitmap -> if (bitmap != null) selected.topColor = sampleTopRow(bitmap) },
                { }
            )
        }
    }
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

    // Full-screen overlay currently open. Settings is full screen on both form factors; the tablet tab overview covers the window.
    val tabletTabsOpen = isTablet && showTabGrid
    val phoneOverlay = when {
        showTabGrid && !isTablet -> PhoneOverlay.TABS
        showLibrary -> PhoneOverlay.LIBRARY
        showSettings -> PhoneOverlay.SETTINGS
        showExtensions -> PhoneOverlay.EXTENSIONS
        else -> PhoneOverlay.NONE
    }
    // Hidden while a full-screen panel (Settings, Library, Extensions, the tab overview) or the downloads sheet is open,
    // so the browser bars never sit over a panel's own header.
    // A full-screen video hides the bars too, so only the video is on screen.
    val toolbarHidden = phoneOverlay != PhoneOverlay.NONE || showDownloads || tabletTabsOpen || selected.videoFullScreen
    VideoFullScreenEffect(active = selected.videoFullScreen)
    // Back leaves a full-screen video when the setting is on. The other Back handlers below stand down then,
    // so this one is the one that runs.
    val backExitsVideo = selected.videoFullScreen && videoBackExits
    BackHandler(enabled = backExitsVideo) {
        BrowserLog.info("page", "Back leaves full screen")
        selected.session.exitFullScreen()
    }
    // Tablet Compact: the space under the floating row takes the page's top colour and the page starts below it.
    // Phone Compact does the same with the status bar's height. Once the page scrolls, that space folds away
    // with a short move, and the content runs up under the status bar fade.
    val zoneFraction by animateFloatAsState(
        targetValue = if ((tabletCompact || phoneCompact) && !selected.scrolled) 1f else 0f,
        animationSpec = if (selected.scrolled) OraMotion.exit<Float>() else OraMotion.enter<Float>(),
        label = "compact top zone",
    )
    val clearBrowsingData: () -> Unit = {
        GeckoEngine.runtime(context).storageController
            .clearData(StorageController.ClearFlags.ALL)
            .accept(
                { InAppNotices.show("Browsing data cleared") },
                { InAppNotices.show("Could not clear browsing data") }
            )
    }

    // Back closes the open overlay first, then steps back through page history, and only then exits.
    // Android's own permissions (location, camera, microphone, notifications) that a page needs before it can use them.
    val androidPermissionRequest by SitePrompts.androidRequest.collectAsState()
    val androidPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        SitePrompts.finishAndroid(results)
    }
    LaunchedEffect(androidPermissionRequest) {
        androidPermissionRequest?.let { androidPermissionLauncher.launch(it.permissions) }
    }

    BackHandler(enabled = (phoneOverlay != PhoneOverlay.NONE || tabletTabsOpen) && !backExitsVideo) {
        showTabGrid = false
        showLibrary = false
        showSettings = false
        showExtensions = false
    }
    BackHandler(enabled = phoneOverlay == PhoneOverlay.NONE && !tabletTabsOpen && selected.canStepBack && !backExitsVideo) {
        selected.stepBack()
    }
    // Quality of life: on tablet, Back closes an open sidebar before it steps back through history.
    // Back closes find in page first; the capsule returns in its slot.
    BackHandler(enabled = showFind && phoneOverlay == PhoneOverlay.NONE && !tabletTabsOpen && !backExitsVideo) { showFind = false }
    BackHandler(enabled = isTablet && showSidebar && phoneOverlay == PhoneOverlay.NONE && !tabletTabsOpen && !backExitsVideo) {
        settings.setTabletSidebar(false)
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
                visible = !toolbarHidden && !selected.chromeHidden,
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = OraMotion.exit())
            ) {
                BottomControlsBar(
                    tab = selected,
                    isBookmarked = selectedBookmarked,
                    onBookmark = toggleSelectedBookmark,
                    onShare = { shareUrl(context, selected.url) },
                    onTabs = openTabs,
                    onMenu = { showMenu = true },
                    tabCount = tabs.tabs.size
                )
            }
        } else if (!topLayout && !tabletCompact) {
            // The bar folds down into its bottom edge while an overlay grows out of the tabs button.
            // Phone Compact never leaves while a page is open: scrolling only shrinks it, as in iOS Safari.
            AnimatedVisibility(
                visible = !toolbarHidden && (barStyle == BarStyle.COMPACT || !selected.chromeHidden),
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Bottom, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Bottom, animationSpec = OraMotion.exit())
            ) {
                if (barStyle == BarStyle.BOTTOM) {
                    BrowserToolbar(
                        tab = selected,
                        isBookmarked = selectedBookmarked,
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onBookmark = toggleSelectedBookmark,
                        onShare = { shareUrl(context, selected.url) },
                        onTabs = openTabs,
                        onMenu = { showMenu = true },
                        tabCount = tabs.tabs.size,
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab
                    )
                } else {
                    // Phone Compact: a floating row like iOS Safari's compact bar.
                    FloatingCompactRow(
                        tab = selected,
                        shrunk = selected.chromeHidden,
                        top = false,
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

    // Tablet: the floating row starts past the sidebar (300 dp + 12 dp gap), because the sidebar holds its own buttons.
    val sidebarInset by animateDpAsState(
        targetValue = if (isTablet && showSidebar) BrowserTokens.SidebarWidth else 0.dp,
        animationSpec = OraMotion.enter(),
        label = "sidebar inset",
    )
    val tabletCompactRow: @Composable () -> Unit = {
        FloatingCompactRow(
            tab = selected,
            shrunk = selected.chromeHidden,
            top = true,
            onSubmit = { input -> submitAddress(input) },
            candidates = addressCandidates,
            onMore = toggleMenu,
            onSwipeUp = openTabs,
            onSwitchTab = switchTab,
            onTabs = openTabs,
            onToggleSidebar = { settings.setTabletSidebar(!showSidebar) },
            showSidebar = showSidebar,
            pillMaxWidth = BrowserTokens.TabletCompactPillMaxWidth,
            startInset = sidebarInset,
            findBar = if (showFind) {
                { key(selected.id) { FindBar(session = selected.session, onClose = { showFind = false }, embedded = true) } }
            } else null,
        )
    }
    // In-app messages (formerly toasts) show as a snackbar. A new message replaces the one on screen, so a
    // burst of feedback never queues up behind a stale line.
    val notices = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        InAppNotices.messages.collect { message ->
            notices.currentSnackbarData?.dismiss()
            notices.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Tablet Compact floats over the browser grid colour, so no strip shows behind the row or beside the sidebar.
        containerColor = if (tabletCompact) Color.Transparent else MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = notices) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(BrowserTokens.RadiusMedium),
                    containerColor = browserColors().Surface,
                    contentColor = browserColors().AddressBarText,
                )
            }
        },
        topBar = {
            Column(modifier = Modifier.fillMaxWidth().onSizeChanged { topBarHeight = with(density) { it.height.toDp() } }) {
            // Adaptive shrink: the top bar folds up while the page is scrolled down, and returns on scroll up.
            AnimatedVisibility(
                visible = topLayout && !selected.chromeHidden && !toolbarHidden,
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Top, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = OraMotion.exit())
            ) {
                if (isTablet && barStyle == BarStyle.TOP) {
                    // Tablet Top: the desktop-style toolbar with sidebar toggle, navigation and tools.
                    TabletToolbar(
                        tab = selected,
                        isBookmarked = selectedBookmarked,
                        showSidebarToggle = isTablet,
                        showSidebar = showSidebar,
                        onToggleSidebar = { settings.setTabletSidebar(!showSidebar) },
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onBookmark = toggleSelectedBookmark,
                        onShare = { shareUrl(context, selected.url) },
                        onNewTab = onNewTab,
                        onDownloads = { showDownloads = true },
                        onMore = toggleMenu,
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab,
                        onTabs = openTabs,
                        tabCount = tabs.tabs.size
                    )
                } else {
                    // Phone Top: the address bar and the menu.
                    CompactToolbar(
                        tab = selected,
                        onSubmit = { input -> submitAddress(input) },
                        candidates = addressCandidates,
                        onMore = { showMenu = true },
                        onSwipeUp = openTabs,
                        onSwitchTab = switchTab
                    )
                }
            }
            }
        },
        bottomBar = { if (isTablet) bottomBarContent() }
    ) { innerPadding ->
        // Quality of life: with a hardware keyboard, Ctrl+Tab moves to the next open tab, and Escape closes the sidebar.
        Box(modifier = Modifier.fillMaxSize().onPreviewKeyEvent { e ->
            if (e.isCtrlPressed && e.key == Key.Tab && e.type == KeyEventType.KeyDown && tabs.tabs.size > 1) {
                val i = tabs.tabs.indexOfFirst { it.id == tabs.selectedId }
                val next = tabs.tabs[(i + 1) % tabs.tabs.size]
                BrowserLog.info("tabs", "Ctrl+Tab to tab ${next.id}")
                tabs.select(next.id)
                true
            } else if (isTablet && showSidebar && e.key == Key.Escape && e.type == KeyEventType.KeyUp) {
                BrowserLog.info("ui", "Sidebar closed with Escape")
                settings.setTabletSidebar(false)
                true
            } else false
        }) {
        Row(modifier = Modifier.fillMaxSize().then(if (isTablet && showSidebar) Modifier.background(browserColors().GridBackground) else Modifier).padding(innerPadding)) {
            // The sidebar slides in and out from its left edge, using the shared overlay motion.
            AnimatedVisibility(
                visible = isTablet && showSidebar,
                // It also scales from the hamburger (top left) and shrinks back into it.
                enter = fadeIn(OraMotion.overlayFadeIn()) +
                    scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0f, 0f)) +
                    expandHorizontally(expandFrom = Alignment.Start, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.overlayFadeOut()) +
                    scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0f, 0f)) +
                    shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = OraMotion.exit())
            ) {
                // The whole column is sidebar-coloured and fixed to the sidebar width, so no white
                // window background shows behind the profile chips or beside the list.
                // It starts below the status bar, so its rounded top corners are not hidden under the bar.
                Column(
                    modifier = Modifier
                        .width(BrowserTokens.SidebarWidth)
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        // The grid colour fills the rounded corners, so they show the browser colour, not the white window.
                        .background(browserColors().GridBackground)
                        .clip(BrowserTokens.SidebarShape)
                        .background(browserColors().SidebarBackground)
                ) {
                    if (tabletCompact) {
                        // The sidebar holds its own buttons, so the floating row never draws over it.
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = BrowserTokens.PhoneCompactPadding, bottom = BrowserTokens.PhoneCompactPadding),
                            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.PhoneCompactGap),
                        ) {
                            FloatingCircleButton(onClick = { settings.setTabletSidebar(false) }, enabled = true, description = "Hide sidebar") {
                                Icon(Icons.Filled.Menu, contentDescription = null)
                            }
                            FloatingCircleButton(
                                onClick = { selected.stepBack() },
                                enabled = selected.canStepBack,
                                description = "Back",
                                onLongClick = {
                                    BrowserLog.info("nav", "Long-press Back: opened start page from ${selected.url}")
                                    selected.url = BrowserTabs.START_URL
                                },
                            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) }
                            FloatingCircleButton(
                                onClick = { selected.session.goForward() },
                                enabled = selected.canGoForward,
                                description = "Forward",
                            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) }
                        }
                    }
                    // Chips scroll sideways when they are wider than the sidebar, so "+ New profile" stays reachable.
                    ProfileSwitcher(
                        profiles = profileList,
                        activeId = profileId,
                        onSelect = { id -> profiles.select(id) },
                        onAdd = { name, tint -> profiles.add(name, tint) },
                        onEdit = { id, name, tint -> profiles.rename(id, name); profiles.setTint(id, tint) },
                        onRemove = { id -> profiles.remove(id) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // The list takes the height left under the profile chips, so its bottom is not cut off.
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        TabSidebar(
                            tabs = tabs,
                            bookmarks = bookmarkList,
                            history = historyList,
                            historyOff = !saveHistory,
                            onNewTab = onNewTab,
                            onOpenBookmark = { url -> selected.session.loadUri(url) },
                            onOpenHistory = { url -> selected.session.loadUri(url) },
                            onRemoveHistory = { url -> history.remove(url) },
                            onClearHistory = { history.clear() },
                            onNewPrivateTab = onNewPrivateTab,
                            onLockPrivate = if (appLockSet) ({ AppLock.lockIfEnabled(settings) }) else null,
                        )
                    }
                }
            }
            // With the sidebar open the page sits in a rounded card, inset 12 dp from the sidebar, right and bottom.
            val cardInset by animateDpAsState(if (isTablet && showSidebar) BrowserTokens.PanelInset else 0.dp, OraMotion.enter(), label = "page card inset")
            val cardShape = RoundedCornerShape(if (isTablet && showSidebar) BrowserTokens.PageCardRadius else 0.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .padding(start = cardInset, end = cardInset, bottom = cardInset)
                    .clip(cardShape)
                    .addressSwipeEffect(swipeMotion, swipeEffect)
            ) {
                BrowserPage(
                    tab = selected,
                    pageView = pageView,
                    bookmarks = bookmarkList,
                    blockAds = blockAds,
                    fadePagesIn = fadePagesIn,
                    androidAutofill = autofillPasswords,
                    // Phone Compact and Bottom let the page run up under the status bar; push it below, as the fade ends there.
                    belowStatusBar = !isTablet && !topLayout,
                    // Tablet Compact floats over the page; the start page keeps its content clear of the row.
                    topInset = if (tabletCompact) compactRowHeight else 0.dp,
                    // Tablet Compact: the space under the floating row takes the page's top colour and the page moves down to meet it.
                    zoneHeight = when {
                        tabletCompact -> compactRowHeight
                        phoneCompact -> phoneZoneHeight
                        else -> 0.dp
                    },
                    zoneFraction = zoneFraction,
                    zoneColor = barColor,
                    // Phone Compact: the page ends above the floating row, which GeckoView is told about (0 elsewhere).
                    bottomClipPx = if (phoneCompact) bottomBarPx else 0,
                    onSubmit = { input -> submitAddress(input) }
                )
                // Brightness and volume swipes sit over the page while its video is full screen.
                if (selected.videoFullScreen && videoGestures) {
                    VideoGestureLayer(modifier = Modifier.fillMaxSize())
                }
                // Seek bar along the bottom of a full-screen video, drawn above the gesture halves.
                if (selected.videoFullScreen) {
                    VideoSeekBar(modifier = Modifier.align(Alignment.BottomCenter))
                }
                if (showDownloads) {
                    DownloadsSheet(repository = downloads, onDismiss = { showDownloads = false })
                }
                // Long press on a link or image opens the quick menu at the press point (both layouts).
                selected.pressTarget?.let { target ->
                    key(target) {
                        LinkQuickMenu(
                            target = target,
                            tabs = tabs,
                            // The page sits lower by the tablet's top zone, so the press point is moved down by the same amount.
                            pageTop = (if (tabletCompact) compactRowHeight else phoneZoneHeight) * zoneFraction,
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
                        // Quality of life: the card rides above the keyboard, so Auto-fill is reachable in a focused field.
                        .imePadding()
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
                                    InAppNotices.show("Copied")
                                },
                                onSelectAll = { picked.selectAll() },
                                onShare = { selectedText ->
                                    shareUrl(context, highlightUrl(selected.url, selectedText))
                                    BrowserLog.info("select", "Shared a link with a highlight")
                                },
                                onDismiss = { selected.clearSelection() },
                                onAutofill = if (autofillPasswords && picked.isActionAvailable(GeckoSession.SelectionActionDelegate.ACTION_PASTE)) {
                                    { requestAutofill(context, pageView.view); selected.clearSelection() }
                                } else null,
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
                // Back closes the reader first, so it never leaves the page behind it.
                androidx.activity.compose.BackHandler(enabled = selected.reader.open) { selected.reader.close() }
                key(selected.id) {
                    ReaderBar(
                        reader = selected.reader,
                        settings = settings,
                        compact = !isTablet,
                        modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = if (isTablet) 28.dp else BrowserTokens.SelectionMenuBottomInset),
                    )
                }
                if (showFind && !tabletCompact) {
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
                        isTablet = isTablet,
                        // The tablet panel starts below whichever bar is on top: the toolbar (Top) or the floating row (Compact).
                        topInset = if (tabletCompact) compactRowHeight else topBarHeight,
                        closeRequests = menuCloseRequests,
                        isBookmarked = selectedBookmarked,
                        onDismiss = { showMenu = false },
                        // The menu closes itself before each action runs, so actions only do their work.
                        onShare = { shareUrl(context, selected.url) },
                        onBookmark = toggleSelectedBookmark,
                        onNewTab = { onNewTab() },
                        onNewPrivateTab = { onNewPrivateTab() },
                        onLibrary = { showLibrary = true },
                        onFind = { showFind = true },
                        readerOpen = selected.reader.open,
                        readerHint = when {
                            selected.reader.open -> "Showing ${selected.reader.site}"
                            selected.reader.readable -> "${selected.reader.minutes} min read, without the clutter"
                            else -> "No article found on this page"
                        },
                        onReader = {
                            if (selected.reader.open) {
                                selected.reader.close()
                            } else if (!selected.reader.readable) {
                                InAppNotices.show("This page has no article to read")
                            } else {
                                UsageStats.count("menu.reader")
                                selected.reader.show(
                                    com.makemission.ora.extensions.ReaderLook(
                                        settings.readerSizeStep.value, settings.readerSerif.value, settings.readerPage.value,
                                    ),
                                )
                            }
                        },
                        closedTabTitle = tabs.lastClosedTitle,
                        onReopenClosed = onReopenClosed,
                        onAddToHome = {
                            val url = selected.url
                            if (url == BrowserTabs.START_URL || url.isBlank() || selected.isPrivate) {
                                InAppNotices.show(if (selected.isPrivate) "Private tabs can't be added to the home screen" else "Open a page first")
                            } else {
                                HomeShortcut.add(context, url, selected.title)
                            }
                        },
                        onCopyAddress = {
                            val url = selected.url
                            if (url == BrowserTabs.START_URL || url.isBlank()) {
                                InAppNotices.show("Nothing to copy on the start page")
                            } else {
                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Page address", url))
                                UsageStats.count("menu.copy_address")
                                BrowserLog.info("page", "Page address copied from the menu")
                                InAppNotices.show("Page address copied")
                            }
                        },
                        desktopSite = selected.desktopSite,
                        onToggleDesktop = { enabled -> selected.applyDesktopSite(enabled) },
                        onSettings = { showSettings = true },
                        onExtensions = { showExtensions = true },
                        onDownloads = { showDownloads = true },
                        onTabs = { openTabs() }
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
                                else scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleOut(), transformOrigin = MORPH_ORIGIN) + fadeOut(OraMotion.overlayFadeOut())
                            // Returning to the page: the slide pages leave to the right.
                            outOfSlide -> (fadeIn(OraMotion.enter())) togetherWith
                                (slideOutHorizontally(OraMotion.exit()) { it } + fadeOut(OraMotion.exit()))
                            // The tab grid grows out of the tabs button and shrinks back into it.
                            else -> (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(
                                initialScale = OraMotion.MORPH_SCALE,
                                animationSpec = OraMotion.overlayScaleIn(),
                                transformOrigin = MORPH_ORIGIN
                            )) togetherWith (fadeOut(OraMotion.overlayFadeOut()) + scaleOut(
                                targetScale = OraMotion.MORPH_SCALE,
                                animationSpec = OraMotion.overlayScaleOut(),
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
                            onClearData = clearBrowsingData,
                            sitePermissions = sitePermissionsFor(profileId),
                        )
                        PhoneOverlay.LIBRARY -> LibraryScreen(
                            bookmarks = bookmarkList,
                            history = historyList,
                            historyOff = !saveHistory,
                            onOpen = { url ->
                                selected.session.loadUri(url)
                                showLibrary = false
                            },
                            onRemoveBookmark = { url -> bookmarks.remove(url) },
                            onRemoveHistory = { url -> history.remove(url) },
                            onClearHistory = { history.clear() },
                            onDone = { showLibrary = false }
                        )
                        PhoneOverlay.TABS -> TabOverviewContent(
                            tablet = isTablet,
                            profiles = profiles,
                            profileList = profileList,
                            profileId = profileId,
                            tabs = tabs,
                            onAddTab = onNewTab,
                            onAddPrivateTab = onNewPrivateTab,
                            onLockPrivate = if (appLockSet) ({ AppLock.lockIfEnabled(settings) }) else null,
                            onDone = { showTabGrid = false },
                            insets = Modifier.statusBarsPadding(),
                        )
                        PhoneOverlay.EXTENSIONS -> ExtensionsScreen(
                            repository = extensions,
                            onDone = { showExtensions = false },
                            tablet = isTablet,
                            onOpenUrl = { url -> tabs.newTab(url) },
                        )
                        PhoneOverlay.NONE -> Unit
                    }
                }
            }
        }
        // Phone bars float over the page, as iOS does: the page runs under them and nothing is drawn behind.
        if (tabletCompact) {
            // The page runs up under the status bar: a solid strip in the page's colour at the top, a soft fade once it scrolls.
            StatusBarOverlay(
                color = barColor,
                scrolled = selected.scrolled,
                modifier = Modifier.align(Alignment.TopCenter),
            )
            // Transparent around the pill: the page shows through, so the row seems to float over it.
            // It leaves while a full-screen panel is open. The measured height is kept during that move, so the page stays put.
            AnimatedVisibility(
                visible = !toolbarHidden,
                modifier = Modifier.align(Alignment.TopCenter),
                enter = fadeIn(OraMotion.enter()) + expandVertically(expandFrom = Alignment.Top, animationSpec = OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(shrinkTowards = Alignment.Top, animationSpec = OraMotion.exit()),
            ) {
                Box(
                    modifier = Modifier.onSizeChanged {
                        if (!toolbarHidden) compactRowHeight = with(density) { it.height.toDp() }
                    }
                ) { tabletCompactRow() }
            }
        }
        // Tablet: a rightward swipe from the left edge of the page opens the sidebar, when the setting is on.
        // The strip starts below the toolbar (or the Compact row), so it never takes taps meant for the buttons.
        if (isTablet && !showSidebar && edgeSwipeSidebar) {
            SidebarEdgeSwipe(
                onOpen = { settings.setTabletSidebar(true) },
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(top = if (tabletCompact) compactRowHeight else topBarHeight),
            )
        }
        if (!isTablet) {
            // The measured height is kept while a full-screen panel is open, as the tablet's top row is.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { if (!toolbarHidden) bottomBarPx = it.height }
            ) { bottomBarContent() }
            if (!topLayout) {
                // Phone: the strip behind the status bar matches the page's top colour, and fades once the page scrolls.
                StatusBarOverlay(
                    color = barColor,
                    scrolled = selected.scrolled,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
        }
    }
    // A site's permission prompt, under the address bar. The tablet tab overview below covers it.
    // On the phone bottom-bar layout there is no top bar, so the card starts below the status bar's height.
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    SitePromptOverlay(
        topOffset = when {
            tabletCompact -> compactRowHeight
            !isTablet && !topLayout -> statusBarTop
            else -> topBarHeight
        },
        modifier = Modifier.align(Alignment.TopCenter),
    )
    // Tablet tab overview covers the whole window, toolbar and sidebar included. It grows out of the tabs button.
    AnimatedVisibility(
        visible = tabletTabsOpen,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(initialScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleIn(), transformOrigin = MORPH_ORIGIN),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleOut(), transformOrigin = MORPH_ORIGIN),
    ) {
        TabOverviewContent(
            tablet = true,
            profiles = profiles,
            profileList = profileList,
            profileId = profileId,
            tabs = tabs,
            onAddTab = onNewTab,
                            onAddPrivateTab = onNewPrivateTab,
                            onLockPrivate = if (appLockSet) ({ AppLock.lockIfEnabled(settings) }) else null,
            onDone = { showTabGrid = false },
            insets = Modifier.statusBarsPadding().navigationBarsPadding(),
        )
    }
    // With the PIN lock on, a private tab stays behind the lock screen until the PIN is entered.
    // Leaving it goes to a regular tab, so the lock never traps the user.
    val lockWholeApp by settings.appLockWholeApp.collectAsState()
    LockScreen(
        settings = settings,
        visible = AppLock.locked && !lockWholeApp && selected.isPrivate,
        title = "Private tabs are locked",
        leaveLabel = "Go to a regular tab",
        onLeave = {
            UsageStats.count("security.leave_private")
            val regular = tabs.tabs.firstOrNull { !it.isPrivate }
            if (regular != null) tabs.select(regular.id) else tabs.newTab(BrowserTabs.START_URL)
        },
    )
    }
}

/**
 * The tab overview, shared by the phone TABS overlay and the tablet overview: profile chips above the tab grid.
 * Choosing a tab, New tab and Done all close it through [onDone]. [insets] pads the content clear of the system
 * bars after the background is painted, so those bars keep the grid colour.
 */
@Composable
private fun TabOverviewContent(
    tablet: Boolean,
    profiles: ProfilesRepository,
    profileList: List<Profile>,
    profileId: String,
    tabs: BrowserTabs,
    onAddTab: () -> Unit,
    onAddPrivateTab: () -> Unit,
    onLockPrivate: (() -> Unit)?,
    onDone: () -> Unit,
    insets: Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
            .then(insets)
    ) {
        ProfileSwitcher(
            profiles = profileList,
            activeId = profileId,
            onSelect = { id -> profiles.select(id) },
            onAdd = { name, tint -> profiles.add(name, tint) },
                        onEdit = { id, name, tint -> profiles.rename(id, name); profiles.setTint(id, tint) },
            onRemove = { id -> profiles.remove(id) },
        )
        TabGrid(
            tablet = tablet,
            tabs = tabs,
            onSelect = { id ->
                tabs.select(id)
                onDone()
            },
            onClose = { id -> tabs.close(id) },
            onNewTab = {
                onAddTab()
                onDone()
            },
            onNewPrivateTab = onAddPrivateTab,
            onLockPrivate = onLockPrivate,
            onDone = onDone,
        )
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
    androidAutofill: Boolean,
    belowStatusBar: Boolean,
    topInset: Dp,
    /** Tablet Compact only: the height of the space above the page that the floating row covers, status bar included. */
    zoneHeight: Dp,
    /** 1 while that space is shown at the top of an unscrolled page, 0 once it has folded away. */
    zoneFraction: Float,
    /** The page's top colour, which fills that space. */
    zoneColor: Color,
    /** Phone Compact only: the height of the floating row at the bottom, which the page ends above. 0 elsewhere. */
    bottomClipPx: Int,
    onSubmit: (String) -> Unit,
) {
    // The start page's content starts below the status bar on phones. Web pages run up under it: the strip behind
    // the bar takes the page's top colour, so the page reads as starting below it.
    val statusInset = if (belowStatusBar) Modifier.statusBarsPadding() else Modifier
    // The start page and a web page cross-fade instead of cutting over. The web layer stays composed, so the
    // GeckoView keeps its session while the start page is shown on top of it.
    val onStart = tab.url == BrowserTabs.START_URL
    // The page stays transparent while it arrives, then fades in with the shared enter motion.
    // With the setting off it is always fully shown, as before.
    val pageAlpha by animateFloatAsState(
        targetValue = if (!fadePagesIn || tab.pageRevealed) 1f else 0f,
        animationSpec = if (fadePagesIn && tab.pageRevealed) OraMotion.enter<Float>() else OraMotion.exit<Float>(),
        label = "page reveal",
    )
    // Switching tabs: the page area fades in on the shared enter motion instead of cutting over. The first tab shown
    // starts fully visible. With Remove animations on, the fade takes no time.
    val switchAlpha = remember { Animatable(1f) }
    var shownTabId by remember { mutableStateOf(tab.id) }
    LaunchedEffect(tab.id) {
        if (shownTabId != tab.id) {
            shownTabId = tab.id
            BrowserLog.info("tabs", "Tab switch fade to tab ${tab.id}")
            switchAlpha.snapTo(0f)
            switchAlpha.animateTo(1f, OraMotion.overlayFadeIn())
        }
    }
    // Pull to refresh: pulling down from the top of the page reloads it. A small indicator follows the finger
    // and stays while the page reloads. The page itself keeps scrolling as before.
    var pull by remember { mutableStateOf(0.dp) }
    val indicatorVisible = pull > 0.dp || tab.loading
    Box(
        modifier = Modifier
            .fillMaxSize()
            // While a page is arriving the web layer is transparent. Without this, the window's white shows
            // through as a flash; the browser's own background keeps it dark in dark modes.
            .background(browserColors().GridBackground)
            .pullToReload(
                canPull = { !onStart && tab.lastScrollY <= PULL_SCROLL_TOLERANCE_PX && !tab.loading },
                onDistance = { pull = it },
                onRefresh = {
                    BrowserLog.info("page", "Pull to refresh: ${tab.url}")
                    tab.session.reload()
                }
            )
    ) {
        // The space above the page, filled with the page's top colour. The page moves down by the same height, so
        // its own top row starts below the floating row; the move reverses when the page scrolls.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(zoneHeight * zoneFraction)
                .background(zoneColor)
        )
        // Hands the page's login fields to Android's autofill framework. See AutofillBridge.kt.
        val context = LocalContext.current
        DisposableEffect(context) {
            val detach = attachAutofillBridge(context) { pageView.view }
            onDispose { detach?.invoke() }
        }
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = pageAlpha * switchAlpha.value
                    translationY = zoneHeight.toPx() * zoneFraction
                },
            factory = { ctx ->
                GeckoView(ctx).also {
                    // Lets Android's autofill service (Bitwarden, Google and others) see the page's login fields.
                    it.importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                    pageView.view = it
                    pageView.appliedClip = 0
                }
            },
            update = { view ->
                if (view.session != tab.session) view.setSession(tab.session)
                // Phone Compact: the bottom space is the floating row's height. Set only when that height changes.
                if (pageView.appliedClip != bottomClipPx) {
                    pageView.appliedClip = bottomClipPx
                    view.setVerticalClipping(bottomClipPx)
                    view.setDynamicToolbarMaxHeight(bottomClipPx)
                }
                if (view.autofillEnabled != androidAutofill) {
                    view.setAutofillEnabled(androidAutofill)
                    BrowserLog.info("autofill", "Android autofill ${if (androidAutofill) "on" else "off"} for the page view")
                }
            }
        )
        // The start page fades in on the start address and out as a page starts, on the shared enter and exit timings.
        val startAlpha by animateFloatAsState(
            targetValue = if (onStart) 1f else 0f,
            animationSpec = if (onStart) OraMotion.enter<Float>() else OraMotion.exit<Float>(),
            label = "start page fade",
        )
        if (startAlpha > 0f) {
            // The background fills the whole window, so a floating Compact row sits over the start page, not a white strip.
            // The inset moves only the content, clear of the row. The layer takes taps so they do not reach the hidden page.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(statusInset)
                    .graphicsLayer { alpha = startAlpha * switchAlpha.value }
                    .background(browserColors().GridBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
            ) {
                StartPage(
                    bookmarks = bookmarks,
                    blockAds = blockAds,
                    onSubmit = onSubmit,
                    isPrivate = tab.isPrivate,
                    modifier = Modifier.fillMaxSize().padding(top = topInset),
                )
            }
        }
        AnimatedVisibility(
            visible = indicatorVisible,
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars).padding(top = 12.dp),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE),
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
    // Private tabs keep no picture of their pages, so the tab grid shows a placeholder for them.
    if (tab.isPrivate) return
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
                isPrivate = tab.isPrivate,
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
                    isPrivate = tab.isPrivate,
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

/** Compact layout for phone Top: the address bar with a menu button. Tabs open from the menu or a swipe up. */
@Composable
private fun CompactToolbar(
    tab: BrowserTab,
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
            AddressBar(
                currentUrl = tab.url,
                isPrivate = tab.isPrivate,
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
 * Floating Compact row, modeled on iOS Safari: round buttons either side of an address pill, with
 * no strip behind them. The phone puts it at the bottom with Back and ⋯. The tablet puts it at the top
 * and adds Forward and Tabs ([onTabs] set). Scrolling shrinks it: the round buttons fold away and the
 * pill gets shorter.
 */
@Composable
private fun FloatingCompactRow(
    tab: BrowserTab,
    /** True once the page has scrolled down: the round buttons fold away and the pill gets shorter. */
    shrunk: Boolean,
    /** Tablet sits at the top edge; phone sits at the bottom edge. */
    top: Boolean,
    onSubmit: (String) -> Unit,
    candidates: List<AddressSuggestion>,
    onMore: () -> Unit,
    onSwipeUp: () -> Unit,
    onSwitchTab: (Int) -> Unit,
    /** Tablet only: when set, Forward and Tabs sit beside the pill and this opens the tab overview. */
    onTabs: (() -> Unit)? = null,
    /** Caps the pill width on wide screens, so the row stays a compact floating control. */
    pillMaxWidth: Dp? = null,
    /** Tablet only: a button that shows or hides the tab sidebar, placed before Back. */
    onToggleSidebar: (() -> Unit)? = null,
    showSidebar: Boolean = false,
    /** Tablet: with the sidebar open, it holds the sidebar, Back and Forward buttons, and this row starts past it. */
    startInset: Dp = 0.dp,
    /** Tablet: find in page, drawn in the address capsule's slot while it is open. */
    findBar: (@Composable () -> Unit)? = null,
) {
    // 0 = full bar, 1 = shrunk. Shrinking uses the exit easing, growing back the enter easing.
    val shrink by animateFloatAsState(
        targetValue = if (shrunk) 1f else 0f,
        animationSpec = if (shrunk) OraMotion.exit<Float>() else OraMotion.enter<Float>(),
        label = "compactShrink"
    )
    val insets = if (top) {
        WindowInsets.statusBars.union(WindowInsets.displayCutout)
    } else {
        WindowInsets.navigationBars.union(WindowInsets.ime)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(insets)
            .padding(
                start = BrowserTokens.PhoneCompactPadding + startInset,
                end = BrowserTokens.PhoneCompactPadding,
                top = if (top) lerp(BrowserTokens.PhoneCompactPadding, BrowserTokens.TabletCompactShrunkPadding, shrink) else BrowserTokens.PhoneCompactPadding,
                bottom = if (top) {
                    lerp(BrowserTokens.PhoneCompactPadding, BrowserTokens.TabletCompactShrunkPadding, shrink)
                } else {
                    lerp(BrowserTokens.PhoneCompactFloatBottom, BrowserTokens.PhoneCompactShrunkBottom, shrink)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(lerp(BrowserTokens.PhoneCompactGap, 0.dp, shrink))
    ) {
        if (onToggleSidebar != null && !showSidebar) {
            CompactSlot(shrink) {
                FloatingCircleButton(
                    onClick = onToggleSidebar,
                    enabled = shrink < 0.5f,
                    description = if (showSidebar) "Hide sidebar" else "Show sidebar"
                ) {
                    Icon(Icons.Filled.Menu, contentDescription = null)
                }
            }
        }
        if (!(onToggleSidebar != null && showSidebar)) CompactSlot(shrink) {
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }
        if (onTabs != null && !showSidebar) {
            CompactSlot(shrink) {
                FloatingCircleButton(
                    onClick = { tab.session.goForward() },
                    enabled = tab.canGoForward && shrink < 0.5f,
                    description = "Forward"
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
        // The middle slot takes the free width and centres the pill. Shrinking narrows the pill around the
        // centre, as Safari does, instead of letting it widen into the space the round buttons leave.
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier = (if (pillMaxWidth != null) Modifier.widthIn(max = if (findBar != null) BrowserTokens.TabletFindWidth else pillMaxWidth) else Modifier)
                    .fillMaxWidth(1f + (BrowserTokens.PhoneCompactShrunkWidthFraction - 1f) * shrink)
            ) {
                // Find in page and the address capsule trade places in this slot, on the shared overlay timings.
                AnimatedContent(
                    targetState = findBar != null,
                    transitionSpec = {
                        (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE)) togetherWith
                            (fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE))
                    },
                    label = "find-or-address",
                ) { finding ->
                    if (finding && findBar != null) {
                        Surface(shape = BrowserTokens.PhoneCompactPill, color = browserColors().AddressBarBackground, modifier = Modifier.fillMaxWidth()) { findBar() }
                    } else AddressBar(
                    currentUrl = tab.url,
                    isPrivate = tab.isPrivate,
                    loading = tab.loading,
                    onSubmit = onSubmit,
                    candidates = candidates,
                    onReload = { tab.session.reload() }, onHardReload = { hardReload(tab) }, progress = tab.progress,
                    onStop = { tab.session.stop() },
                    modifier = Modifier.fillMaxWidth(),
                    height = lerp(BrowserTokens.AddressBarHeight, if (top) BrowserTokens.TabletCompactShrunkHeight else BrowserTokens.PhoneCompactShrunkHeight, shrink),
                    onSwipeUp = onSwipeUp,
                    onSwitchTab = onSwitchTab,
                    shape = BrowserTokens.PhoneCompactPill,
                    centered = true
                )
                }
                // Quality of life: while the row is shrunk (tablet top or phone bottom), a tap on the pill brings the full
                // toolbar back instead of opening the address for editing. The overlay sits only over the pill.
                if (shrunk) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clip(BrowserTokens.PhoneCompactPill)
                            .clickable {
                                BrowserLog.info("nav", "Tapped the shrunk address bar: showed the toolbar")
                                tab.showChrome()
                            }
                    )
                }
            }
        }
        if (onTabs != null) {
            CompactSlot(shrink) {
                FloatingCircleButton(onClick = onTabs, enabled = shrink < 0.5f, description = "Tabs") {
                    Icon(Icons.Outlined.Tab, contentDescription = null)
                }
            }
        }
        CompactSlot(shrink) {
            FloatingCircleButton(onClick = onMore, enabled = shrink < 0.5f, description = "More") {
                Icon(Icons.Filled.MoreVert, contentDescription = null)
            }
        }
    }
}

/** A slot for one round button. It narrows to nothing as the row shrinks, so the button folds away instead of vanishing. */
@Composable
private fun CompactSlot(shrink: Float, content: @Composable () -> Unit) {
    Box(modifier = Modifier.width(lerp(BrowserTokens.PhoneCompactCircle, 0.dp, shrink)).clipToBounds()) {
        content()
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
            val durationMs = change0Time(down.uptimeMillis)
            val detail = "dx=${dx.toInt()} dy=${dy.toInt()} ms=$durationMs"
            if (abs(dy) > abs(dx) && dy < -threshold) {
                BrowserLog.info("tabs", "Swipe up on address bar: all tabs")
                UsageStats.gesture("address_swipe", "registered", "up", detail)
                onSwipeUp()
            } else if (abs(dx) > abs(dy) && abs(dx) > threshold) {
                UsageStats.gesture("address_swipe", "registered", if (dx < 0) "left" else "right", detail)
                onSwitchTab(if (dx < 0) 1 else -1)
            } else if (abs(dx) > GESTURE_TAP_SLOP_PX || abs(dy) > GESTURE_TAP_SLOP_PX) {
                // The finger moved too far to be a tap, but not into a recognised swipe. Say why, to tune the thresholds.
                val reason = when {
                    abs(dx) > abs(dy) -> "too_short"
                    dy > 0 -> "downward"
                    abs(dy) > threshold * 0.5f && abs(dx) > threshold * 0.5f -> "diagonal"
                    else -> "too_short"
                }
                UsageStats.gesture("address_swipe", "ignored", reason, detail)
            }
        }
    }

/** Movement under this many px counts as a tap, not a swipe attempt, and is not logged as a gesture. */
private const val GESTURE_TAP_SLOP_PX = 24f

private fun change0Time(startMs: Long): Long = android.os.SystemClock.uptimeMillis() - startMs

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
    /** Private tabs get a dashed Fog outline and a Private chip. */
    isPrivate: Boolean = false,
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
            .shadow(BrowserTokens.AddressBarElevation, shape)
            .then(if (isPrivate) Modifier.dashedOutline(shape, Mineral.Fog) else Modifier),
        shape = shape,
        color = browserColors().AddressBarBackground
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = BrowserTokens.AddressBarInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isPrivate) PrivateChip()
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
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = com.makemission.ora.ui.theme.InstrumentSansAddress,
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
        modifier = Modifier
            .size(BrowserTokens.ToolbarButtonSize)
            .then(if (enabled) Modifier.pressFeedback() else Modifier)
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
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE),
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

/** Asks Android's autofill service for suggestions for the focused page field, as the system "Auto-fill" menu item does. */
private fun requestAutofill(context: android.content.Context, view: GeckoView?) {
    val manager = context.getSystemService(android.view.autofill.AutofillManager::class.java)
    if (manager == null || !manager.isEnabled) {
        BrowserLog.warn("autofill", "Auto-fill requested but no autofill service is turned on")
        InAppNotices.show("No autofill service is turned on. Choose one in your phone's settings.")
        return
    }
    val autofill = view?.session?.autofillSession
    val data = autofill?.focusedData
    val rect = autofill?.focused?.screenRect
    if (data == null || rect == null) {
        // GeckoView follows one page's fields at a time, so after another tab loads the focus can be lost; a reload restores it.
        BrowserLog.warn("autofill", "Auto-fill requested but no page field has focus")
        InAppNotices.show("Tap a login field on this page, then try again. If it still fails, reload the page.")
        return
    }
    val node = autofill.focused
    BrowserLog.info("autofill", "Focused field: domain=${node?.domain} hint=${node?.hint} inputType=${node?.inputType} tag=${node?.tag} attrs=${node?.attributes} rect=$rect id=${data.id}")
    manager.requestAutofill(view, data.id, rect)
    BrowserLog.info("autofill", "Auto-fill requested for the focused field")
}

/** The "Private" chip at the left of a private tab's address capsule. */
@Composable
private fun PrivateChip() {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .padding(start = 6.dp)
            .clip(CircleShape)
            .background(colors.GridBackground)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Private", color = Mineral.Fog, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
