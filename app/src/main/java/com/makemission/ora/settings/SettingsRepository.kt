package com.makemission.ora.settings

import com.makemission.ora.ui.theme.AppFont
import android.content.Context
import android.content.SharedPreferences
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.ui.theme.MotionStyle
import com.makemission.ora.ui.theme.PaletteStyle
import com.makemission.ora.ui.theme.OraMotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How the browser is colored. [AUTO] follows the system light or dark setting. */
enum class AppearanceMode(val key: String, val label: String) {
    AUTO("auto", "Auto"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    PURE_DARK("pure_dark", "Pure dark");

    companion object {
        fun fromKey(key: String?): AppearanceMode = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

/**
 * Where the address bar and tab controls sit, modeled on Safari's tab bar options.
 * [BOTTOM] keeps the bottom toolbar with a tab row; [COMPACT] keeps only the address bar and a
 * menu button; [TOP] moves the whole toolbar to the top.
 */
/** What the extra round button in the Compact bar does. Tabs is the default; the rest let people put a favourite one tap away. */
enum class BarButton(val key: String, val label: String, val detail: String) {
    TABS("tabs", "Tabs", "Open all your tabs. On a phone the button stays hidden; swipe the bar up instead."),
    DOWNLOADS("downloads", "Downloads", "Open your downloaded files."),
    LIBRARY("library", "Library", "Bookmarks, reading list and history."),
    BOOKMARK("bookmark", "Bookmark this page", "Save or unsave the page you are on."),
    SHARE("share", "Share", "Send the page's address to another app."),
    NEW_TAB("new_tab", "New tab", "Open a fresh tab."),
    FIND("find", "Find in page", "Search the text of the page."),
    SAVE_PDF("save_pdf", "Save as PDF", "Save the page to Downloads."),
    SETTINGS("settings", "Settings", "Open Ora's settings.");

    companion object {
        fun fromKey(key: String?): BarButton = entries.firstOrNull { it.key == key } ?: TABS
    }
}

enum class BarStyle(val key: String, val label: String, val detail: String, val tabletDetail: String = detail) {
    TOP("top", "Top", "The address bar and every control sit at the top."),
    BOTTOM("bottom", "Bottom", "A larger bar at the bottom with tabs, bookmarks and menu."),
    COMPACT(
        "compact", "Compact", "Only the address bar at the bottom, with a ⋯ menu for tabs.",
        tabletDetail = "A floating row at the top: back, forward, the address bar, tabs and menu."
    );

    companion object {
        fun fromKey(key: String?, default: BarStyle): BarStyle = entries.firstOrNull { it.key == key } ?: default
    }
}

/** What a sideways swipe on the address bar moves through: open tabs, the current tab's pages, or profiles. */
enum class AddressSwipeTarget(val key: String, val label: String, val detail: String) {
    TABS("tabs", "Open tabs", "Swipe to the next or previous open tab."),
    PAGES("pages", "Pages in this tab", "Swipe forward and back through this tab's history."),
    PROFILES("profiles", "Profiles", "Swipe to the next or previous profile.");

    companion object {
        fun fromKey(key: String?): AddressSwipeTarget = entries.firstOrNull { it.key == key } ?: TABS
    }
}

/** How the page moves when a sideways swipe on the address bar switches it. */
enum class AddressSwipeEffect(val key: String, val label: String, val detail: String) {
    FADE("fade", "Fade", "The page fades out and the next one fades in."),
    SLIDE("slide", "Slide", "The page slides out the way the swipe went and the next one slides in, with a fade."),
    JIGGLE("jiggle", "Jiggle", "The page tilts and settles back with a small bounce. At the end of the list it bounces without moving.");

    companion object {
        fun fromKey(key: String?): AddressSwipeEffect = entries.firstOrNull { it.key == key } ?: SLIDE
    }
}

/** When Ora may get a head start on the site you are most likely to open next. */
enum class PreloadMode(val key: String, val label: String, val detail: String) {
    OFF("off", "Off", "Nothing is looked up ahead of time."),
    WIFI("wifi", "Wi-Fi only", "Looks up the likely next site on Wi-Fi, never on mobile data or with Data Saver on."),
    ALWAYS("always", "Always", "Looks up the likely next site on any connection. Uses a very small amount of data.");

    companion object {
        fun fromKey(key: String?): PreloadMode = entries.firstOrNull { it.key == key } ?: WIFI
    }
}

/**
 * Single source of truth for all app settings.
 *
 * To add a setting: add a key constant, a default, a MutableStateFlow backed
 * by [prefs], and a setter. Future extension/tweak settings belong here too.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _usageStats = MutableStateFlow(prefs.getBoolean(KEY_USAGE_STATS, true))
    /** When on, the app counts which actions and settings you use, on this device only. */
    val usageStats: StateFlow<Boolean> = _usageStats.asStateFlow()

    init {
        UsageStats.setEnabled(_usageStats.value)
    }

    fun setUsageStats(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USAGE_STATS, enabled).apply()
        _usageStats.value = enabled
        UsageStats.setEnabled(enabled)
        BrowserLog.info("settings", "Usage counting ${if (enabled) "on" else "off"}")
    }

    private val _homePage = MutableStateFlow(prefs.getString(KEY_HOME_PAGE, DEFAULT_HOME_PAGE)!!)
    val homePage: StateFlow<String> = _homePage.asStateFlow()

    fun setHomePage(url: String) {
        val typed = url.trim()
        val value = if (typed.isEmpty() || typed.contains("://")) typed else "https://$typed"
        prefs.edit().putString(KEY_HOME_PAGE, value).apply()
        _homePage.value = value
        BrowserLog.info("settings", if (value.isEmpty()) "Home page reset to the start page" else "Home page set")
    }

    private val _appearance = MutableStateFlow(
        AppearanceMode.fromKey(prefs.getString(KEY_APPEARANCE, null))
    )
    val appearance: StateFlow<AppearanceMode> = _appearance.asStateFlow()

    fun setAppearance(mode: AppearanceMode) {
        prefs.edit().putString(KEY_APPEARANCE, mode.key).apply()
        _appearance.value = mode
        UsageStats.count("setting.appearance")
        BrowserLog.info("settings", "Appearance set to ${mode.label}")
    }

    private val _websiteTheme = MutableStateFlow(prefs.getBoolean(KEY_WEBSITE_THEME, true))
    /** When on, web pages use the app's light or dark palette instead of the system setting. */
    val websiteTheme: StateFlow<Boolean> = _websiteTheme.asStateFlow()

    fun setWebsiteTheme(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEBSITE_THEME, enabled).apply()
        _websiteTheme.value = enabled
        BrowserLog.info("settings", "Website theme ${if (enabled) "on" else "off"}")
    }

    private val _tabletSidebar = MutableStateFlow(prefs.getBoolean(KEY_TABLET_SIDEBAR, true))
    /** Tablet only: whether the tab and bookmark sidebar is shown. Remembered across launches. */
    val tabletSidebar: StateFlow<Boolean> = _tabletSidebar.asStateFlow()

    fun setTabletSidebar(shown: Boolean) {
        prefs.edit().putBoolean(KEY_TABLET_SIDEBAR, shown).apply()
        _tabletSidebar.value = shown
        BrowserLog.info("settings", "Tab sidebar ${if (shown) "shown" else "hidden"}")
    }

    private val _blockPopups = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_POPUPS, true))
    /** When on, sites cannot open pop-up windows. When off, a pop-up opens as a new tab. */
    val blockPopups: StateFlow<Boolean> = _blockPopups.asStateFlow()

    fun setBlockPopups(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_POPUPS, enabled).apply()
        _blockPopups.value = enabled
        BrowserLog.info("settings", "Pop-up blocking ${if (enabled) "on" else "off"}")
    }

    // First-launch setup. A fresh install (nothing saved yet) runs it; an install that already has data is marked
    // done at once and can use Settings > About > Run setup again. Decided once, before anything else is written.
    init {
        if (!prefs.contains(KEY_ONBOARDING_DONE)) {
            val profilesFile = context.applicationContext.getSharedPreferences("ora_profiles", Context.MODE_PRIVATE)
            val fresh = prefs.all.isEmpty() && profilesFile.all.isEmpty()
            prefs.edit().putInt(KEY_ONBOARDING_DONE, if (fresh) 0 else com.makemission.ora.onboarding.ONBOARDING_VERSION).apply()
            BrowserLog.info("onboarding", if (fresh) "Fresh install: setup will run" else "Existing install: setup not run")
        }
    }

    private val _onboardingDone = MutableStateFlow(prefs.getInt(KEY_ONBOARDING_DONE, 0))
    /** The setup version that was finished, 0 while setup is still to do. */
    val onboardingDoneVersion: StateFlow<Int> = _onboardingDone.asStateFlow()

    private val _onboardingStep = MutableStateFlow(prefs.getString(KEY_ONBOARDING_STEP, "welcome") ?: "welcome")
    /** The last setup step reached, so closing Ora midway resumes there. */
    val onboardingStep: StateFlow<String> = _onboardingStep.asStateFlow()

    fun setOnboardingStep(step: String) {
        prefs.edit().putString(KEY_ONBOARDING_STEP, step).apply()
        _onboardingStep.value = step
    }

    fun finishOnboarding(version: Int) {
        prefs.edit().putInt(KEY_ONBOARDING_DONE, version).putString(KEY_ONBOARDING_STEP, "welcome").apply()
        _onboardingDone.value = version
        BrowserLog.info("onboarding", "Onboarding finished")
    }

    /** Settings > About > Run setup again. Choices already made stay; each step starts from them. */
    fun restartOnboarding() {
        prefs.edit().putInt(KEY_ONBOARDING_DONE, 0).putString(KEY_ONBOARDING_STEP, "welcome").apply()
        _onboardingStep.value = "welcome"
        _onboardingDone.value = 0
        BrowserLog.info("onboarding", "Setup started again from Settings")
    }

    private val _tipsSeen = MutableStateFlow(prefs.getStringSet(KEY_TIPS_SEEN, emptySet()) ?: emptySet())
    /** Ids of the first-use tips already shown and dismissed. */
    val tipsSeen: StateFlow<Set<String>> = _tipsSeen.asStateFlow()

    fun markTipSeen(id: String) {
        val next = _tipsSeen.value + id
        prefs.edit().putStringSet(KEY_TIPS_SEEN, next).apply()
        _tipsSeen.value = next
        BrowserLog.info("onboarding", "Tip seen: $id")
    }

    private val _noticeVersion = MutableStateFlow(prefs.getInt(KEY_NOTICE_VERSION, 0))
    /** The notice version the user accepted, 0 if never. A newer notice version shows it again. */
    val noticeAcceptedVersion: StateFlow<Int> = _noticeVersion.asStateFlow()
    /** When the notice was accepted, as yyyy-MM-dd, or empty. */
    fun noticeAcceptedDate(): String = prefs.getString(KEY_NOTICE_DATE, "") ?: ""

    fun acceptNotice(version: Int) {
        val date = java.time.LocalDate.now().toString()
        prefs.edit().putInt(KEY_NOTICE_VERSION, version).putString(KEY_NOTICE_DATE, date).apply()
        _noticeVersion.value = version
        BrowserLog.info("notice", "Notice accepted (version $version)")
    }

    private val _openLinksInApps = MutableStateFlow(com.makemission.ora.browser.AppLinkMode.fromKey(prefs.getString(KEY_OPEN_LINKS_IN_APPS, null)))
    /** Settings > Open links in apps: Ask (default), Always or Never. Private tabs always ask. */
    val openLinksInApps: StateFlow<com.makemission.ora.browser.AppLinkMode> = _openLinksInApps.asStateFlow()

    fun setOpenLinksInApps(mode: com.makemission.ora.browser.AppLinkMode) {
        prefs.edit().putString(KEY_OPEN_LINKS_IN_APPS, mode.key).apply()
        _openLinksInApps.value = mode
        BrowserLog.info("settings", "Open links in apps set to ${mode.label}")
    }

    private val _blockAds = MutableStateFlow(prefs.getBoolean(KEY_BLOCK_ADS, true))
    /** When on, ads and trackers are blocked with GeckoView's strict tracking protection. */
    val blockAds: StateFlow<Boolean> = _blockAds.asStateFlow()

    fun setBlockAds(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_ADS, enabled).apply()
        _blockAds.value = enabled
        BrowserLog.info("settings", "Ad and tracker blocking ${if (enabled) "on" else "off"}")
    }

    private val _videoGestures = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_GESTURES, true))
    /** When on, a full-screen video takes swipes: up and down on the left changes brightness, on the right the volume. */
    val videoGestures: StateFlow<Boolean> = _videoGestures.asStateFlow()

    fun setVideoGestures(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_GESTURES, enabled).apply()
        _videoGestures.value = enabled
        BrowserLog.info("settings", "Video brightness and volume swipes ${if (enabled) "on" else "off"}")
    }

    private val _readerSizeStep = MutableStateFlow(prefs.getInt(KEY_READER_SIZE, 1))
    /** Reader view text size, 0 (smallest) to 4. */
    val readerSizeStep: StateFlow<Int> = _readerSizeStep.asStateFlow()

    fun setReaderSizeStep(step: Int) {
        val clamped = step.coerceIn(0, com.makemission.ora.extensions.ReaderLook.MAX_SIZE_STEP)
        prefs.edit().putInt(KEY_READER_SIZE, clamped).apply()
        _readerSizeStep.value = clamped
        BrowserLog.info("settings", "Reader text size set to step $clamped")
    }

    private val _readerSerif = MutableStateFlow(prefs.getBoolean(KEY_READER_SERIF, true))
    /** Reader view text face: serif (true) or sans (false). */
    val readerSerif: StateFlow<Boolean> = _readerSerif.asStateFlow()

    fun setReaderSerif(serif: Boolean) {
        prefs.edit().putBoolean(KEY_READER_SERIF, serif).apply()
        _readerSerif.value = serif
        BrowserLog.info("settings", "Reader text set to ${if (serif) "serif" else "sans"}")
    }

    private val _readerPage = MutableStateFlow(com.makemission.ora.extensions.ReaderPage.fromKey(prefs.getString(KEY_READER_PAGE, null)))
    /** Reader view page colour. */
    val readerPage: StateFlow<com.makemission.ora.extensions.ReaderPage> = _readerPage.asStateFlow()

    fun setReaderPage(page: com.makemission.ora.extensions.ReaderPage) {
        prefs.edit().putString(KEY_READER_PAGE, page.key).apply()
        _readerPage.value = page
        BrowserLog.info("settings", "Reader page colour set to ${page.label}")
    }

    private val _videoOraControls = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_ORA_CONTROLS, false))
    /** When on, a full-screen video opens with Ora's controls. Off, the site's own controls show and Ora's open from a small icon. */
    val videoOraControls: StateFlow<Boolean> = _videoOraControls.asStateFlow()

    fun setVideoOraControls(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_ORA_CONTROLS, enabled).apply()
        _videoOraControls.value = enabled
        BrowserLog.info("settings", "Ora video controls by default ${if (enabled) "on" else "off"}")
    }

    private val _startPage = MutableStateFlow(loadStartPage())
    /** How the start page is laid out and what it shows. */
    val startPage: StateFlow<StartPageConfig> = _startPage.asStateFlow()

    private fun loadStartPage() = StartPageConfig(
        slots = parseSlots(prefs.getString(KEY_START_SLOTS, null)),
        source = ShortcutSource.fromKey(prefs.getString(KEY_START_SOURCE, null)),
        rows = prefs.getInt(KEY_START_ROWS, 2).coerceIn(StartPageConfig.MIN_ROWS, StartPageConfig.MAX_ROWS),
        shape = TileShape.fromKey(prefs.getString(KEY_START_SHAPE, null)),
        size = TileSize.fromKey(prefs.getString(KEY_START_SIZE, null)),
        labels = prefs.getBoolean(KEY_START_LABELS, true),
        pinned = parsePinned(prefs.getString(KEY_START_PINNED, null)),
    )

    /** Saves a new start page layout and says what changed in the log. */
    fun setStartPage(config: StartPageConfig, what: String) {
        prefs.edit()
            .putString(KEY_START_SLOTS, encodeSlots(config.slots))
            .putString(KEY_START_SOURCE, config.source.key)
            .putInt(KEY_START_ROWS, config.rows)
            .putString(KEY_START_SHAPE, config.shape.key)
            .putString(KEY_START_SIZE, config.size.key)
            .putBoolean(KEY_START_LABELS, config.labels)
            .putString(KEY_START_PINNED, encodePinned(config.pinned))
            .apply()
        _startPage.value = config
        BrowserLog.info("settings", "Start page: $what")
    }

    fun resetStartPage() = setStartPage(StartPageConfig(), "reset to the default layout")

    private val _videoBackExits = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_BACK_EXITS, true))
    /** When on, Back leaves a full-screen video instead of going back a page. */
    val videoBackExits: StateFlow<Boolean> = _videoBackExits.asStateFlow()

    fun setVideoBackExits(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_BACK_EXITS, enabled).apply()
        _videoBackExits.value = enabled
        BrowserLog.info("settings", "Back leaves full-screen video ${if (enabled) "on" else "off"}")
    }

    private val _videoAutoPip = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_AUTO_PIP, true))
    /** When on, leaving Ora while a video plays full screen keeps it playing in a small window. */
    val videoAutoPip: StateFlow<Boolean> = _videoAutoPip.asStateFlow()

    fun setVideoAutoPip(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_AUTO_PIP, enabled).apply()
        _videoAutoPip.value = enabled
        BrowserLog.info("settings", "Small window when leaving a video ${if (enabled) "on" else "off"}")
    }

    private val _autofillPasswords = MutableStateFlow(prefs.getBoolean(KEY_AUTOFILL_PASSWORDS, true))
    /** When on, login fields fill from saved passwords: GeckoView's saved logins, and the Android autofill service (Bitwarden, Google Password Manager and others). */
    val autofillPasswords: StateFlow<Boolean> = _autofillPasswords.asStateFlow()

    fun setAutofillPasswords(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOFILL_PASSWORDS, enabled).apply()
        _autofillPasswords.value = enabled
        BrowserLog.info("settings", "Password autofill ${if (enabled) "on" else "off"}")
    }

    private val _deviceLink = MutableStateFlow(prefs.getBoolean(KEY_DEVICE_LINK, false))
    /** When on, this device finds the person's other Ora devices on the same Wi-Fi and can exchange pages with the ones it was paired with. Off until chosen. */
    val deviceLink: StateFlow<Boolean> = _deviceLink.asStateFlow()

    fun setDeviceLink(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_LINK, enabled).apply()
        _deviceLink.value = enabled
        BrowserLog.info("settings", "Link devices ${if (enabled) "on" else "off"}")
    }

    private val _deviceContinue = MutableStateFlow(prefs.getBoolean(KEY_DEVICE_CONTINUE, true))
    /** When on (and Link my devices is on), your other devices may ask which page you have open, so they can offer "Continue from this device". Private tabs are never shared. */
    val deviceContinue: StateFlow<Boolean> = _deviceContinue.asStateFlow()

    fun setDeviceContinue(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_CONTINUE, enabled).apply()
        _deviceContinue.value = enabled
        BrowserLog.info("settings", "Continue on another device ${if (enabled) "on" else "off"}")
    }

    private val _deviceReading = MutableStateFlow(prefs.getBoolean(KEY_DEVICE_READING, true))
    /** When on (and Link my devices is on), the reading list is kept the same on every paired device. Only the first profile is synced. */
    val deviceReading: StateFlow<Boolean> = _deviceReading.asStateFlow()

    fun setDeviceReading(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_READING, enabled).apply()
        _deviceReading.value = enabled
        BrowserLog.info("settings", "Reading list sync ${if (enabled) "on" else "off"}")
    }

    private val _deviceName = MutableStateFlow(prefs.getString(KEY_DEVICE_NAME, null) ?: com.makemission.ora.devices.PairedDevices.defaultName())
    /** The name other devices see for this one. Starts as the phone or tablet model. */
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    fun setDeviceName(name: String) {
        val clean = name.trim().take(40).ifBlank { com.makemission.ora.devices.PairedDevices.defaultName() }
        prefs.edit().putString(KEY_DEVICE_NAME, clean).apply()
        _deviceName.value = clean
        BrowserLog.info("settings", "Device name changed")
    }

    private val _torrentsWifiOnly = MutableStateFlow(prefs.getBoolean(KEY_TORRENTS_WIFI_ONLY, true))
    /** When on, torrents only transfer on Wi-Fi (or another unmetered network) and wait on mobile data. On by default. */
    val torrentsWifiOnly: StateFlow<Boolean> = _torrentsWifiOnly.asStateFlow()

    fun setTorrentsWifiOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TORRENTS_WIFI_ONLY, enabled).apply()
        _torrentsWifiOnly.value = enabled
        BrowserLog.info("settings", "Torrents on Wi-Fi only ${if (enabled) "on" else "off"}")
    }

    private val _torrentsSeed = MutableStateFlow(prefs.getBoolean(KEY_TORRENTS_SEED, false))
    /** When on, a finished torrent keeps sharing (seeding) until removed. When off it stops as soon as it is complete. */
    val torrentsSeed: StateFlow<Boolean> = _torrentsSeed.asStateFlow()

    fun setTorrentsSeed(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_TORRENTS_SEED, enabled).apply()
        _torrentsSeed.value = enabled
        BrowserLog.info("settings", "Keep sharing finished torrents ${if (enabled) "on" else "off"}")
    }

    private val _torrentsSpeedLimit = MutableStateFlow(prefs.getInt(KEY_TORRENTS_SPEED_LIMIT, 0))
    /** Download speed cap in kilobytes per second; 0 means no cap. */
    val torrentsSpeedLimit: StateFlow<Int> = _torrentsSpeedLimit.asStateFlow()

    fun setTorrentsSpeedLimit(kbPerSecond: Int) {
        val clean = kbPerSecond.coerceIn(0, 100_000)
        prefs.edit().putInt(KEY_TORRENTS_SPEED_LIMIT, clean).apply()
        _torrentsSpeedLimit.value = clean
        BrowserLog.info("settings", "Torrent speed limit $clean KB/s")
    }

    private val _cookieBehavior = MutableStateFlow(prefs.getInt(KEY_COOKIE_BEHAVIOR, COOKIES_ALLOW_ALL))
    /** Cookie policy, as GeckoView's CookieBehavior values: [COOKIES_ALLOW_ALL], [COOKIES_BLOCK_THIRD_PARTY] or [COOKIES_BLOCK_ALL]. */
    val cookieBehavior: StateFlow<Int> = _cookieBehavior.asStateFlow()

    fun setCookieBehavior(behavior: Int) {
        prefs.edit().putInt(KEY_COOKIE_BEHAVIOR, behavior).apply()
        _cookieBehavior.value = behavior
        val label = when (behavior) {
            COOKIES_BLOCK_THIRD_PARTY -> "third-party blocked"
            COOKIES_BLOCK_ALL -> "all blocked"
            else -> "all allowed"
        }
        BrowserLog.info("settings", "Cookies: $label")
    }

    private val _secureDns = MutableStateFlow(prefs.getBoolean(KEY_SECURE_DNS, true))
    /** When on, host names resolve over encrypted DNS (DNS over HTTPS), falling back to the network's DNS if that fails. */
    val secureDns: StateFlow<Boolean> = _secureDns.asStateFlow()

    fun setSecureDns(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_DNS, enabled).apply()
        _secureDns.value = enabled
        BrowserLog.info("settings", "Secure DNS ${if (enabled) "on" else "off"}")
    }

    private val _font = MutableStateFlow(AppFont.fromKey(prefs.getString(KEY_FONT, null)))
    /** The typeface used on Ora's own screens. Web pages keep their own fonts. */
    val font: StateFlow<AppFont> = _font.asStateFlow()

    fun setFont(font: AppFont) {
        prefs.edit().putString(KEY_FONT, font.key).apply()
        _font.value = font
        UsageStats.count("setting.font")
        BrowserLog.info("settings", "Font set to ${font.label}")
    }

    private val _webFont = MutableStateFlow(prefs.getBoolean(KEY_WEB_FONT, false))
    /** When on, web pages are shown in [font] too. Off by default, since it overrides every site's own look. */
    val webFont: StateFlow<Boolean> = _webFont.asStateFlow()

    fun setWebFont(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_WEB_FONT, enabled).apply()
        _webFont.value = enabled
        BrowserLog.info("settings", "Font on websites ${if (enabled) "on" else "off"}")
    }

    private val _hints = MutableStateFlow(prefs.getBoolean(KEY_HINTS, true))
    /** When on, a short gesture cue plays once on a control the first time it could help. Turning it on again replays them. */
    val hints: StateFlow<Boolean> = _hints.asStateFlow()

    fun setHints(enabled: Boolean) {
        val edit = prefs.edit().putBoolean(KEY_HINTS, enabled)
        if (enabled) edit.remove(KEY_SEEN_HINTS)
        edit.apply()
        _hints.value = enabled
        BrowserLog.info("settings", "Hints ${if (enabled) "on, replaying from the start" else "off"}")
    }

    /** True when hints are on and [id] has not played yet. */
    fun shouldShowHint(id: String): Boolean =
        _hints.value && id !in prefs.getStringSet(KEY_SEEN_HINTS, emptySet()).orEmpty()

    fun markHintSeen(id: String) {
        val seen = prefs.getStringSet(KEY_SEEN_HINTS, emptySet()).orEmpty() + id
        prefs.edit().putStringSet(KEY_SEEN_HINTS, seen).apply()
        BrowserLog.info("hints", "Hint shown: $id")
    }

    private val _fastHttpDownloads = MutableStateFlow(prefs.getBoolean(KEY_FAST_HTTP_DOWNLOADS, false))
    /**
     * When on, files from plain http:// links use Ora's fast download (parts, pause, resume). Off by default,
     * because plain http can be read or changed on the network. Those files then use one stream without pause.
     * https:// files always use the fast download.
     */
    val fastHttpDownloads: StateFlow<Boolean> = _fastHttpDownloads.asStateFlow()

    fun setFastHttpDownloads(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FAST_HTTP_DOWNLOADS, enabled).apply()
        _fastHttpDownloads.value = enabled
        BrowserLog.info("settings", "Fast downloads for plain http ${if (enabled) "on" else "off"}")
    }

    private val _showGroupStrip = MutableStateFlow(prefs.getBoolean(KEY_SHOW_GROUP_STRIP, true))
    /** Whether the group strip shows while the open tab is in a group. On by default. */
    val showGroupStrip: StateFlow<Boolean> = _showGroupStrip.asStateFlow()

    fun setShowGroupStrip(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_GROUP_STRIP, enabled).apply()
        _showGroupStrip.value = enabled
        BrowserLog.info("settings", "Group strip ${if (enabled) "on" else "off"}")
    }

    private val _toolbarItems = MutableStateFlow(ToolbarConfig.parse(prefs.getString(KEY_TOOLBAR_ITEMS, null)))
    /** The buttons in the phone Top layout's bottom toolbar, in order. Menu is always added last by the bar. */
    val toolbarItems: StateFlow<List<ToolbarItem>> = _toolbarItems.asStateFlow()

    fun setToolbarItems(items: List<ToolbarItem>) {
        val clean = items.distinct().take(ToolbarConfig.MAX_BUTTONS)
        prefs.edit().putString(KEY_TOOLBAR_ITEMS, ToolbarConfig.encode(clean)).apply()
        _toolbarItems.value = clean
        BrowserLog.info("settings", "Toolbar set to ${ToolbarConfig.presetName(clean)}: ${clean.size} buttons")
    }

    /** Back to the Classic set. */
    fun resetToolbar() = setToolbarItems(ToolbarConfig.CLASSIC)

    private val _imageAlwaysAsk = MutableStateFlow(prefs.getBoolean(KEY_IMAGE_ALWAYS_ASK, false))
    /** When on, saving an image from a long press opens the Save image as sheet instead of saving at once. */
    val imageAlwaysAsk: StateFlow<Boolean> = _imageAlwaysAsk.asStateFlow()

    fun setImageAlwaysAsk(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IMAGE_ALWAYS_ASK, enabled).apply()
        _imageAlwaysAsk.value = enabled
        BrowserLog.info("settings", "Always ask where to save images ${if (enabled) "on" else "off"}")
    }

    private val _imageSaveFolder = MutableStateFlow(prefs.getString(KEY_IMAGE_SAVE_FOLDER, null))
    /** The folder (a document tree address) chosen in Save image as, or null for Pictures/Ora. */
    val imageSaveFolder: StateFlow<String?> = _imageSaveFolder.asStateFlow()

    fun setImageSaveFolder(tree: String?) {
        prefs.edit().apply { if (tree == null) remove(KEY_IMAGE_SAVE_FOLDER) else putString(KEY_IMAGE_SAVE_FOLDER, tree) }.apply()
        _imageSaveFolder.value = tree
        BrowserLog.info("settings", if (tree == null) "Image save folder reset to Pictures/Ora" else "Image save folder changed")
    }

    private val _downloadNotifications = MutableStateFlow(prefs.getBoolean(KEY_DOWNLOAD_NOTIFICATIONS, true))
    /**
     * When on, each download has its own notification in the shade, with Pause, Resume, Cancel, Retry and Open,
     * and a finished or failed download is announced. A private download shows no file name. Ora still keeps its
     * one background notice while downloads run, because Android requires it.
     */
    val downloadNotifications: StateFlow<Boolean> = _downloadNotifications.asStateFlow()

    fun setDownloadNotifications(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_NOTIFICATIONS, enabled).apply()
        _downloadNotifications.value = enabled
        BrowserLog.info("settings", "Download notifications ${if (enabled) "on" else "off"}")
    }

    private val _appLockRecord = MutableStateFlow(prefs.getString(KEY_APP_LOCK, null))
    /** Salted PBKDF2 record of the app lock PIN, or null when the lock is off. The PIN itself is never stored. */
    val appLockRecord: StateFlow<String?> = _appLockRecord.asStateFlow()

    fun setAppLockRecord(record: String?) {
        prefs.edit().apply { if (record == null) remove(KEY_APP_LOCK) else putString(KEY_APP_LOCK, record) }.apply()
        _appLockRecord.value = record
        UsageStats.count("setting.app_lock")
        BrowserLog.info("settings", "App lock ${if (record != null) "on" else "off"}")
    }

    private val _appLockWholeApp = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_WHOLE, false))
    /** False (the default): the PIN guards private tabs only. True: it covers the whole app. */
    val appLockWholeApp: StateFlow<Boolean> = _appLockWholeApp.asStateFlow()

    fun setAppLockWholeApp(whole: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_WHOLE, whole).apply()
        _appLockWholeApp.value = whole
        UsageStats.count("setting.app_lock_scope")
        BrowserLog.info("settings", "PIN lock covers ${if (whole) "the whole app" else "private tabs only"}")
    }

    fun appLockFailures(): Int = prefs.getInt(KEY_APP_LOCK_FAILS, 0)
    fun appLockLockedUntil(): Long = prefs.getLong(KEY_APP_LOCK_UNTIL, 0L)
    fun setAppLockFailures(count: Int, lockedUntil: Long) {
        prefs.edit().putInt(KEY_APP_LOCK_FAILS, count).putLong(KEY_APP_LOCK_UNTIL, lockedUntil).apply()
    }

    private val _httpsOnly = MutableStateFlow(prefs.getBoolean(KEY_HTTPS_ONLY, false))
    /** When on, pages load over HTTPS only. A site that has no HTTPS version does not load. */
    val httpsOnly: StateFlow<Boolean> = _httpsOnly.asStateFlow()

    fun setHttpsOnly(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HTTPS_ONLY, enabled).apply()
        _httpsOnly.value = enabled
        UsageStats.count("setting.https_only")
        BrowserLog.info("settings", "HTTPS-only mode ${if (enabled) "on" else "off"}")
    }

    private val _globalPrivacyControl = MutableStateFlow(prefs.getBoolean(KEY_GLOBAL_PRIVACY_CONTROL, true))
    /** When on, pages receive the Global Privacy Control signal, asking sites not to sell or share your data. */
    val globalPrivacyControl: StateFlow<Boolean> = _globalPrivacyControl.asStateFlow()

    fun setGlobalPrivacyControl(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GLOBAL_PRIVACY_CONTROL, enabled).apply()
        _globalPrivacyControl.value = enabled
        BrowserLog.info("settings", "Global Privacy Control ${if (enabled) "on" else "off"}")
    }

    private val _strongPrivateTabs = MutableStateFlow(prefs.getBoolean(KEY_STRONG_PRIVATE_TABS, true))
    /** When on, private tabs (only) get the strictest protections: fingerprint resistance, HTTPS-only, isolated cookies, tracking-parameter stripping and email-tracker blocking. On by default. */
    val strongPrivateTabs: StateFlow<Boolean> = _strongPrivateTabs.asStateFlow()

    fun setStrongPrivateTabs(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STRONG_PRIVATE_TABS, enabled).apply()
        _strongPrivateTabs.value = enabled
        BrowserLog.info("settings", "Strongest private tabs ${if (enabled) "on" else "off"}")
    }

    private val _fingerprintingProtection = MutableStateFlow(prefs.getBoolean(KEY_FINGERPRINTING_PROTECTION, false))
    /** When on, GeckoView hides details that sites use to recognise your device, such as some hardware and font details. */
    val fingerprintingProtection: StateFlow<Boolean> = _fingerprintingProtection.asStateFlow()

    fun setFingerprintingProtection(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FINGERPRINTING_PROTECTION, enabled).apply()
        _fingerprintingProtection.value = enabled
        BrowserLog.info("settings", "Fingerprinting protection ${if (enabled) "on" else "off"}")
    }

    private val _deviceFontWeb = MutableStateFlow(prefs.getBoolean(KEY_DEVICE_FONT_WEB, false))
    /**
     * Lab: when on, web pages use the browser's default font, which is the device's font, instead of the
     * fonts each site asks for. Icon fonts on some sites may show as empty boxes while this is on.
     */
    val deviceFontWeb: StateFlow<Boolean> = _deviceFontWeb.asStateFlow()

    fun setDeviceFontWeb(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DEVICE_FONT_WEB, enabled).apply()
        _deviceFontWeb.value = enabled
        BrowserLog.info("settings", "Lab: device font on web pages ${if (enabled) "on" else "off"}")
    }

    private val _labs = MutableStateFlow(
        com.makemission.ora.labs.LabFeature.entries.filter { prefs.getBoolean(LAB_KEY_PREFIX + it.key, false) }.toSet()
    ).also { com.makemission.ora.labs.Labs.enabled = it.value }
    /** Labs: the opt-in security features that are switched on. All are off until chosen. */
    val labs: StateFlow<Set<com.makemission.ora.labs.LabFeature>> = _labs.asStateFlow()

    fun setLab(feature: com.makemission.ora.labs.LabFeature, enabled: Boolean) {
        prefs.edit().putBoolean(LAB_KEY_PREFIX + feature.key, enabled).apply()
        val next = if (enabled) _labs.value + feature else _labs.value - feature
        _labs.value = next
        com.makemission.ora.labs.Labs.enabled = next
        UsageStats.count("setting.lab")
        BrowserLog.info("settings", "Lab: ${feature.label} ${if (enabled) "on" else "off"}${if (feature.restart) " (applies at next start)" else ""}")
    }

    private val _textSizePercent = MutableStateFlow(prefs.getInt(KEY_TEXT_SIZE_PERCENT, 100).coerceIn(TEXT_SIZE_MIN, TEXT_SIZE_MAX))
    /** How large text is on web pages, as a percentage of the page's own size. 100 leaves pages as they are. */
    val textSizePercent: StateFlow<Int> = _textSizePercent.asStateFlow()

    fun setTextSizePercent(percent: Int) {
        val value = percent.coerceIn(TEXT_SIZE_MIN, TEXT_SIZE_MAX)
        prefs.edit().putInt(KEY_TEXT_SIZE_PERCENT, value).apply()
        _textSizePercent.value = value
        UsageStats.count("setting.text_size")
        BrowserLog.info("settings", "Text size on web pages $value%")
    }

    private val _edgeSwipeSidebar = MutableStateFlow(prefs.getBoolean(KEY_EDGE_SWIPE_SIDEBAR, true))
    /**
     * Tablet only: a rightward swipe from the left edge of a page opens the sidebar. Off for sites
     * whose own swipes (carousels, drawers) conflict with it.
     */
    val edgeSwipeSidebar: StateFlow<Boolean> = _edgeSwipeSidebar.asStateFlow()

    fun setEdgeSwipeSidebar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EDGE_SWIPE_SIDEBAR, enabled).apply()
        _edgeSwipeSidebar.value = enabled
        BrowserLog.info("settings", "Edge swipe for the sidebar ${if (enabled) "on" else "off"}")
    }

    private val _quickResume = MutableStateFlow(prefs.getBoolean(KEY_QUICK_RESUME, true))
    /** When on, the open tabs of each profile are saved as the app leaves the screen and reopened on the next launch. */
    val quickResume: StateFlow<Boolean> = _quickResume.asStateFlow()

    fun setQuickResume(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_QUICK_RESUME, enabled).apply()
        _quickResume.value = enabled
        BrowserLog.info("settings", "Quick resume ${if (enabled) "on" else "off"}")
    }

    private val _saveHistory = MutableStateFlow(prefs.getBoolean(KEY_SAVE_HISTORY, true))
    /** When on, pages that finish loading in a normal tab are listed in the History of the profile. */
    val saveHistory: StateFlow<Boolean> = _saveHistory.asStateFlow()

    fun setSaveHistory(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SAVE_HISTORY, enabled).apply()
        _saveHistory.value = enabled
        BrowserLog.info("settings", "Save browsing history ${if (enabled) "on" else "off"}")
    }

    private val _fadePagesIn = MutableStateFlow(prefs.getBoolean(KEY_FADE_PAGES_IN, true))
    /** When on, a page fades in once it is half loaded instead of showing its pieces as they arrive. */
    val fadePagesIn: StateFlow<Boolean> = _fadePagesIn.asStateFlow()

    fun setFadePagesIn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FADE_PAGES_IN, enabled).apply()
        _fadePagesIn.value = enabled
        BrowserLog.info("settings", "Page fade-in ${if (enabled) "on" else "off"}")
    }

    private val _motionStyle = MutableStateFlow(MotionStyle.fromKey(prefs.getString(KEY_MOTION_STYLE, null)))
    /** How transitions are timed and eased: Smooth, Elegant, Fast or Premium. */
    val motionStyle: StateFlow<MotionStyle> = _motionStyle.asStateFlow()

    init {
        OraMotion.style = _motionStyle.value
    }

    fun setMotionStyle(style: MotionStyle) {
        prefs.edit().putString(KEY_MOTION_STYLE, style.key).apply()
        _motionStyle.value = style
        OraMotion.style = style
        BrowserLog.info("settings", "Motion style ${style.label}")
    }

    private val _addressSwipeTarget = MutableStateFlow(AddressSwipeTarget.fromKey(prefs.getString(KEY_ADDRESS_SWIPE_TARGET, null)))
    /** What a sideways swipe on the address bar moves through. */
    val addressSwipeTarget: StateFlow<AddressSwipeTarget> = _addressSwipeTarget.asStateFlow()

    fun setAddressSwipeTarget(target: AddressSwipeTarget) {
        prefs.edit().putString(KEY_ADDRESS_SWIPE_TARGET, target.key).apply()
        _addressSwipeTarget.value = target
        UsageStats.count("setting.address_swipe_target")
        BrowserLog.info("settings", "Address bar swipe moves through ${target.label}")
    }

    private val _addressSwipeEffect = MutableStateFlow(AddressSwipeEffect.fromKey(prefs.getString(KEY_ADDRESS_SWIPE_EFFECT, null)))
    /** How the page moves when an address bar swipe switches it. */
    val addressSwipeEffect: StateFlow<AddressSwipeEffect> = _addressSwipeEffect.asStateFlow()

    fun setAddressSwipeEffect(effect: AddressSwipeEffect) {
        prefs.edit().putString(KEY_ADDRESS_SWIPE_EFFECT, effect.key).apply()
        _addressSwipeEffect.value = effect
        UsageStats.count("setting.address_swipe_effect")
        BrowserLog.info("settings", "Address bar swipe effect ${effect.label}")
    }

    private val _loadAhead = MutableStateFlow(prefs.getBoolean(KEY_LOAD_AHEAD, false))
    /** When on, the page behind the top address suggestion is fetched ahead of time. Off by default; never in private tabs. */
    val loadAhead: StateFlow<Boolean> = _loadAhead.asStateFlow()

    fun setLoadAhead(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOAD_AHEAD, enabled).apply()
        _loadAhead.value = enabled
        UsageStats.count("setting.load_ahead")
        BrowserLog.info("settings", "Load the page ahead ${if (enabled) "on" else "off"}")
    }

    private val _slowLoadTips = MutableStateFlow(prefs.getBoolean(KEY_SLOW_LOAD_TIPS, true))
    /** When on, a page that takes a long time to load shows a tip with things to try. */
    val slowLoadTips: StateFlow<Boolean> = _slowLoadTips.asStateFlow()

    fun setSlowLoadTips(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLOW_LOAD_TIPS, enabled).apply()
        _slowLoadTips.value = enabled
        UsageStats.count("setting.slow_load_tips")
        BrowserLog.info("settings", "Slow page tips ${if (enabled) "on" else "off"}")
    }

    private val _searchSuggestions = MutableStateFlow(prefs.getBoolean(KEY_SEARCH_SUGGESTIONS, true))
    /** When on, the text typed in the address bar is sent to the default search engine to get suggestions. */
    val searchSuggestions: StateFlow<Boolean> = _searchSuggestions.asStateFlow()

    fun setSearchSuggestions(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH_SUGGESTIONS, enabled).apply()
        _searchSuggestions.value = enabled
        UsageStats.count("setting.search_suggestions")
        BrowserLog.info("settings", "Search suggestions ${if (enabled) "on" else "off"}")
    }

    private val _preloadMode = MutableStateFlow(PreloadMode.fromKey(prefs.getString(KEY_PRELOAD_MODE, null)))
    /** Whether Ora looks up the likely next site ahead of time, and on which connections. */
    val preloadMode: StateFlow<PreloadMode> = _preloadMode.asStateFlow()

    fun setPreloadMode(mode: PreloadMode) {
        prefs.edit().putString(KEY_PRELOAD_MODE, mode.key).apply()
        _preloadMode.value = mode
        UsageStats.count("setting.preload_mode")
        BrowserLog.info("settings", "Preloading ${mode.label}")
    }

    private val _paletteStyle = MutableStateFlow(PaletteStyle.fromKey(prefs.getString(KEY_PALETTE_STYLE, null)))
    /** Original blue controls, or controls in Material colours from the theme. Backgrounds never change. */
    val paletteStyle: StateFlow<PaletteStyle> = _paletteStyle.asStateFlow()

    fun setPaletteStyle(style: PaletteStyle) {
        prefs.edit().putString(KEY_PALETTE_STYLE, style.key).apply()
        _paletteStyle.value = style
        UsageStats.count("setting.palette")
        BrowserLog.info("settings", "Control colours set to ${style.label}")
    }

    private val _hapticStrength = MutableStateFlow(com.makemission.ora.ui.HapticStrength.fromKey(prefs.getString(KEY_HAPTICS, null)))
    /** How much Ora vibrates on touch. */
    val hapticStrength: StateFlow<com.makemission.ora.ui.HapticStrength> = _hapticStrength.asStateFlow()

    fun setHapticStrength(strength: com.makemission.ora.ui.HapticStrength) {
        prefs.edit().putString(KEY_HAPTICS, strength.key).apply()
        _hapticStrength.value = strength
        BrowserLog.info("settings", "Touch feedback set to ${strength.label}")
    }

    private val _barButton = MutableStateFlow(BarButton.fromKey(prefs.getString(KEY_BAR_BUTTON, null)))
    /** The action of the extra round button in the Compact bar. */
    val barButton: StateFlow<BarButton> = _barButton.asStateFlow()

    fun setBarButton(button: BarButton) {
        prefs.edit().putString(KEY_BAR_BUTTON, button.key).apply()
        _barButton.value = button
        BrowserLog.info("settings", "Compact bar button set to ${button.label}")
    }

    private val _phoneBarStyle = MutableStateFlow(BarStyle.fromKey(prefs.getString(KEY_PHONE_BAR, null), BarStyle.BOTTOM))
    /** Phone tab bar layout. */
    val phoneBarStyle: StateFlow<BarStyle> = _phoneBarStyle.asStateFlow()

    fun setPhoneBarStyle(style: BarStyle) {
        prefs.edit().putString(KEY_PHONE_BAR, style.key).apply()
        _phoneBarStyle.value = style
        UsageStats.count("setting.phone_bar")
        BrowserLog.info("settings", "Phone tab bar set to ${style.label}")
    }

    private val _tabletBarStyle = MutableStateFlow(BarStyle.fromKey(prefs.getString(KEY_TABLET_BAR, null), BarStyle.TOP))
    /** Tablet tab bar layout. */
    val tabletBarStyle: StateFlow<BarStyle> = _tabletBarStyle.asStateFlow()

    fun setTabletBarStyle(style: BarStyle) {
        prefs.edit().putString(KEY_TABLET_BAR, style.key).apply()
        _tabletBarStyle.value = style
        UsageStats.count("setting.tablet_bar")
        BrowserLog.info("settings", "Tablet tab bar set to ${style.label}")
    }

    private val _askEachSearch = MutableStateFlow(prefs.getBoolean(KEY_ASK_EACH_SEARCH, false))
    /** When on and several engines are on, each search asks which engine to use. Off searches with the default engine. */
    val askEachSearch: StateFlow<Boolean> = _askEachSearch.asStateFlow()

    fun setAskEachSearch(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ASK_EACH_SEARCH, enabled).apply()
        _askEachSearch.value = enabled
        UsageStats.count("setting.ask_each_search")
        BrowserLog.info("settings", "Ask engine each search ${if (enabled) "on" else "off"}")
    }

    private val _tutorialDone = MutableStateFlow(prefs.getStringSet(KEY_TUTORIAL_DONE, emptySet()) ?: emptySet())
    /** Ids of the interactive tutorial lessons the user has finished. */
    val tutorialDone: StateFlow<Set<String>> = _tutorialDone.asStateFlow()

    fun markTutorialLesson(id: String) {
        if (id in _tutorialDone.value) return
        val next = _tutorialDone.value + id
        prefs.edit().putStringSet(KEY_TUTORIAL_DONE, next).apply()
        _tutorialDone.value = next
        UsageStats.count("tutorial.lesson")
        BrowserLog.info("tutorial", "Tutorial lesson \"$id\" finished")
    }

    fun resetTutorial() {
        prefs.edit().remove(KEY_TUTORIAL_DONE).apply()
        _tutorialDone.value = emptySet()
        BrowserLog.info("tutorial", "Tutorial progress cleared")
    }

    private val _searchShortcutsOn = MutableStateFlow(prefs.getBoolean(KEY_SEARCH_SHORTCUTS, true))
    /** When on, typing a keyword and a space in the address bar (such as "yt cats") searches that site. */
    val searchShortcutsOn: StateFlow<Boolean> = _searchShortcutsOn.asStateFlow()

    fun setSearchShortcutsOn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH_SHORTCUTS, enabled).apply()
        _searchShortcutsOn.value = enabled
        SearchShortcuts.enabled = enabled
        UsageStats.count("setting.search_shortcuts")
        BrowserLog.info("settings", "Search shortcuts ${if (enabled) "on" else "off"}")
    }

    private val _customShortcuts = MutableStateFlow(loadCustomShortcuts())
    /** Shortcuts the user added. They win over a built-in one with the same keyword. */
    val customShortcuts: StateFlow<List<SearchShortcut>> = _customShortcuts.asStateFlow()

    private fun loadCustomShortcuts(): List<SearchShortcut> {
        val raw = prefs.getString(KEY_CUSTOM_SHORTCUTS, null) ?: return emptyList()
        return runCatching {
            val arr = org.json.JSONArray(raw)
            (0 until arr.length()).map { arr.getJSONObject(it) }.map { SearchShortcut(it.getString("k"), it.getString("l"), it.getString("t")) }
        }.getOrElse { BrowserLog.warn("settings", "Could not read search shortcuts", it); emptyList() }
    }

    private fun saveCustomShortcuts(list: List<SearchShortcut>) {
        val arr = org.json.JSONArray()
        list.forEach { arr.put(org.json.JSONObject().put("k", it.keyword).put("l", it.label).put("t", it.template)) }
        prefs.edit().putString(KEY_CUSTOM_SHORTCUTS, arr.toString()).apply()
        _customShortcuts.value = list
        SearchShortcuts.custom = list
    }

    /** Adds or replaces the shortcut with this keyword. Returns false when the keyword or address is not valid. */
    fun addCustomShortcut(keyword: String, label: String, template: String): Boolean {
        val k = keyword.trim().lowercase()
        val t = template.trim()
        if (!SearchShortcuts.validKeyword(k) || !SearchShortcuts.validTemplate(t)) return false
        saveCustomShortcuts(_customShortcuts.value.filter { it.keyword != k } + SearchShortcut(k, label.trim().ifEmpty { k }, t))
        UsageStats.count("setting.search_shortcut_add")
        BrowserLog.info("settings", "Search shortcut \"$k\" saved")
        return true
    }

    fun removeCustomShortcut(keyword: String) {
        saveCustomShortcuts(_customShortcuts.value.filter { it.keyword != keyword })
        BrowserLog.info("settings", "Search shortcut \"$keyword\" removed")
    }

    init {
        SearchShortcuts.enabled = _searchShortcutsOn.value
        SearchShortcuts.custom = _customShortcuts.value
    }

    private val _searchEngines = MutableStateFlow(loadSearchEngines())
    /** Engines the user has turned on. When more than one is on, a search asks which to use. */
    val searchEngines: StateFlow<Set<SearchEngine>> = _searchEngines.asStateFlow()

    private val _defaultSearchEngine = MutableStateFlow(
        SearchEngine.fromKey(prefs.getString(KEY_SEARCH_DEFAULT, null)) ?: SearchEngine.DUCKDUCKGO
    )
    /** The engine used when only one is on, and the one highlighted in the chooser. */
    val defaultSearchEngine: StateFlow<SearchEngine> = _defaultSearchEngine.asStateFlow()

    fun setSearchEngineEnabled(engine: SearchEngine, enabled: Boolean) {
        val current = _searchEngines.value
        // Always keep at least one engine on, so searching never has nothing to use.
        if (!enabled && current.size == 1 && engine in current) return
        val updated = if (enabled) current + engine else current - engine
        _searchEngines.value = updated
        prefs.edit().putStringSet(KEY_SEARCH_ENGINES, updated.map { it.key }.toSet()).apply()
        if (!enabled && _defaultSearchEngine.value == engine) {
            setDefaultSearchEngine(updated.first())
        }
        BrowserLog.info("settings", "${engine.label} ${if (enabled) "on" else "off"} for search")
    }

    fun setDefaultSearchEngine(engine: SearchEngine) {
        if (engine !in _searchEngines.value) setSearchEngineEnabled(engine, true)
        prefs.edit().putString(KEY_SEARCH_DEFAULT, engine.key).apply()
        _defaultSearchEngine.value = engine
        UsageStats.count("setting.default_search")
        BrowserLog.info("settings", "Default search set to ${engine.label}")
    }

    private fun loadSearchEngines(): Set<SearchEngine> {
        val saved = prefs.getStringSet(KEY_SEARCH_ENGINES, null)
            ?.mapNotNull { SearchEngine.fromKey(it) }
            ?.toSet()
        return if (saved.isNullOrEmpty()) setOf(SearchEngine.DUCKDUCKGO) else saved
    }

    companion object {
        private const val PREFS_NAME = "ora_settings"
        private const val KEY_SEARCH_ENGINES = "search_engines"
        private const val KEY_SEARCH_DEFAULT = "search_default"
        private const val KEY_ASK_EACH_SEARCH = "ask_each_search"
        private const val KEY_TUTORIAL_DONE = "tutorial_done"
        private const val KEY_SEARCH_SHORTCUTS = "search_shortcuts_on"
        private const val KEY_CUSTOM_SHORTCUTS = "search_shortcuts_custom"
        private const val KEY_HOME_PAGE = "home_page"
        private const val KEY_USAGE_STATS = "usage_stats"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_WEBSITE_THEME = "website_theme"
        private const val KEY_TABLET_SIDEBAR = "tablet_sidebar"
        private const val KEY_BLOCK_POPUPS = "block_popups"
        private const val KEY_ONBOARDING_DONE = "onboarding_done_version"
        private const val KEY_ONBOARDING_STEP = "onboarding_step"
        private const val KEY_TIPS_SEEN = "tips_seen"
        private const val KEY_NOTICE_VERSION = "notice_version"
        private const val KEY_NOTICE_DATE = "notice_date"
        private const val KEY_OPEN_LINKS_IN_APPS = "open_links_in_apps"
        private const val KEY_BLOCK_ADS = "block_ads"
        private const val KEY_VIDEO_GESTURES = "video_gestures"
        private const val KEY_VIDEO_BACK_EXITS = "video_back_exits"
        private const val KEY_VIDEO_ORA_CONTROLS = "video_ora_controls"
        private const val KEY_START_SLOTS = "start_slots"
        private const val KEY_START_SOURCE = "start_source"
        private const val KEY_START_ROWS = "start_rows"
        private const val KEY_START_SHAPE = "start_shape"
        private const val KEY_START_SIZE = "start_size"
        private const val KEY_START_LABELS = "start_labels"
        private const val KEY_START_PINNED = "start_pinned"
        private const val KEY_VIDEO_AUTO_PIP = "video_auto_pip"
        private const val KEY_READER_SIZE = "reader_size"
        private const val KEY_READER_SERIF = "reader_serif"
        private const val KEY_READER_PAGE = "reader_page"
        private const val KEY_FADE_PAGES_IN = "fade_pages_in"
        private const val KEY_QUICK_RESUME = "quick_resume"
        private const val KEY_SAVE_HISTORY = "save_history"
        private const val KEY_EDGE_SWIPE_SIDEBAR = "edge_swipe_sidebar"
        private const val KEY_MOTION_STYLE = "motion_style"
        private const val KEY_ADDRESS_SWIPE_TARGET = "address_swipe_target"
        private const val KEY_ADDRESS_SWIPE_EFFECT = "address_swipe_effect"
        private const val KEY_PRELOAD_MODE = "preload_mode"
        private const val KEY_SEARCH_SUGGESTIONS = "search_suggestions"
        private const val KEY_LOAD_AHEAD = "load_ahead"
        private const val KEY_SLOW_LOAD_TIPS = "slow_load_tips"
        private const val KEY_PALETTE_STYLE = "palette_style"
        private const val KEY_SECURE_DNS = "secure_dns"
        private const val KEY_WEB_FONT = "web_font"
        private const val KEY_FONT = "app_font"
        private const val KEY_HINTS = "hints_enabled"
        private const val KEY_SEEN_HINTS = "hints_seen"
        private const val KEY_FAST_HTTP_DOWNLOADS = "fast_http_downloads"
        private const val KEY_SHOW_GROUP_STRIP = "show_group_strip"
        private const val KEY_TOOLBAR_ITEMS = "toolbar_items"
        private const val KEY_IMAGE_ALWAYS_ASK = "image_always_ask"
        private const val KEY_IMAGE_SAVE_FOLDER = "image_save_folder"
        private const val KEY_DOWNLOAD_NOTIFICATIONS = "download_notifications"
        private const val KEY_GLOBAL_PRIVACY_CONTROL = "global_privacy_control"
        private const val KEY_STRONG_PRIVATE_TABS = "strong_private_tabs"
        private const val KEY_FINGERPRINTING_PROTECTION = "fingerprinting_protection"
        private const val KEY_APP_LOCK = "app_lock_record"
        private const val KEY_APP_LOCK_WHOLE = "app_lock_whole"
        private const val KEY_APP_LOCK_FAILS = "app_lock_fails"
        private const val KEY_APP_LOCK_UNTIL = "app_lock_until"
        private const val KEY_HTTPS_ONLY = "https_only"
        private const val KEY_DEVICE_FONT_WEB = "device_font_web"
        private const val LAB_KEY_PREFIX = "lab_"
        private const val KEY_TEXT_SIZE_PERCENT = "text_size_percent"
        const val TEXT_SIZE_MIN = 50
        const val TEXT_SIZE_MAX = 200
        const val TEXT_SIZE_STEP = 10
        private const val KEY_AUTOFILL_PASSWORDS = "autofill_passwords"
        private const val KEY_COOKIE_BEHAVIOR = "cookie_behavior"
        private const val KEY_DEVICE_LINK = "device_link"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_TORRENTS_WIFI_ONLY = "torrents_wifi_only"
        private const val KEY_TORRENTS_SEED = "torrents_seed"
        private const val KEY_TORRENTS_SPEED_LIMIT = "torrents_speed_limit"
        private const val KEY_DEVICE_CONTINUE = "device_continue"
        private const val KEY_DEVICE_READING = "device_reading"
        /** GeckoView ContentBlocking.CookieBehavior.ACCEPT_ALL. */
        const val COOKIES_ALLOW_ALL = 0
        /** GeckoView ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY. */
        const val COOKIES_BLOCK_THIRD_PARTY = 1
        /** GeckoView ContentBlocking.CookieBehavior.ACCEPT_NONE. */
        const val COOKIES_BLOCK_ALL = 2
        private const val KEY_PHONE_BAR = "phone_bar_style"
        private const val KEY_TABLET_BAR = "tablet_bar_style"
        private const val KEY_BAR_BUTTON = "bar_button"
        private const val KEY_HAPTICS = "haptic_strength"
        /** Empty means the Ora start page, which is what Home opened before the address was honoured. */
        const val DEFAULT_HOME_PAGE = ""

        /** Read straight from storage, for code with no repository instance (the download notifier). */
        fun downloadNotificationsEnabled(context: Context): Boolean =
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_DOWNLOAD_NOTIFICATIONS, true)
        /** Search URL used when the address bar input is not a URL. %s is replaced by the query. */
        const val DEFAULT_SEARCH_TEMPLATE = "https://duckduckgo.com/?q=%s"
    }
}
