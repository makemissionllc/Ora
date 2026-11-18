package com.makemission.ora.settings

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
        val value = url.trim().ifEmpty { DEFAULT_HOME_PAGE }
        prefs.edit().putString(KEY_HOME_PAGE, value).apply()
        _homePage.value = value
        BrowserLog.info("settings", "Home page set to $value")
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

    private val _videoBackExits = MutableStateFlow(prefs.getBoolean(KEY_VIDEO_BACK_EXITS, true))
    /** When on, Back leaves a full-screen video instead of going back a page. */
    val videoBackExits: StateFlow<Boolean> = _videoBackExits.asStateFlow()

    fun setVideoBackExits(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIDEO_BACK_EXITS, enabled).apply()
        _videoBackExits.value = enabled
        BrowserLog.info("settings", "Back leaves full-screen video ${if (enabled) "on" else "off"}")
    }

    private val _autofillPasswords = MutableStateFlow(prefs.getBoolean(KEY_AUTOFILL_PASSWORDS, true))
    /** When on, login fields fill from saved passwords: GeckoView's saved logins, and the Android autofill service (Bitwarden, Google Password Manager and others). */
    val autofillPasswords: StateFlow<Boolean> = _autofillPasswords.asStateFlow()

    fun setAutofillPasswords(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOFILL_PASSWORDS, enabled).apply()
        _autofillPasswords.value = enabled
        BrowserLog.info("settings", "Password autofill ${if (enabled) "on" else "off"}")
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

    private val _appLockRecord = MutableStateFlow(prefs.getString(KEY_APP_LOCK, null))
    /** Salted PBKDF2 record of the app lock PIN, or null when the lock is off. The PIN itself is never stored. */
    val appLockRecord: StateFlow<String?> = _appLockRecord.asStateFlow()

    fun setAppLockRecord(record: String?) {
        prefs.edit().apply { if (record == null) remove(KEY_APP_LOCK) else putString(KEY_APP_LOCK, record) }.apply()
        _appLockRecord.value = record
        UsageStats.count("setting.app_lock")
        BrowserLog.info("settings", "App lock ${if (record != null) "on" else "off"}")
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

    private val _paletteStyle = MutableStateFlow(PaletteStyle.fromKey(prefs.getString(KEY_PALETTE_STYLE, null)))
    /** Original blue controls, or controls in Material colours from the theme. Backgrounds never change. */
    val paletteStyle: StateFlow<PaletteStyle> = _paletteStyle.asStateFlow()

    fun setPaletteStyle(style: PaletteStyle) {
        prefs.edit().putString(KEY_PALETTE_STYLE, style.key).apply()
        _paletteStyle.value = style
        UsageStats.count("setting.palette")
        BrowserLog.info("settings", "Control colours set to ${style.label}")
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
        private const val KEY_HOME_PAGE = "home_page"
        private const val KEY_USAGE_STATS = "usage_stats"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_WEBSITE_THEME = "website_theme"
        private const val KEY_TABLET_SIDEBAR = "tablet_sidebar"
        private const val KEY_BLOCK_POPUPS = "block_popups"
        private const val KEY_BLOCK_ADS = "block_ads"
        private const val KEY_VIDEO_GESTURES = "video_gestures"
        private const val KEY_VIDEO_BACK_EXITS = "video_back_exits"
        private const val KEY_FADE_PAGES_IN = "fade_pages_in"
        private const val KEY_QUICK_RESUME = "quick_resume"
        private const val KEY_SAVE_HISTORY = "save_history"
        private const val KEY_EDGE_SWIPE_SIDEBAR = "edge_swipe_sidebar"
        private const val KEY_MOTION_STYLE = "motion_style"
        private const val KEY_ADDRESS_SWIPE_TARGET = "address_swipe_target"
        private const val KEY_ADDRESS_SWIPE_EFFECT = "address_swipe_effect"
        private const val KEY_PALETTE_STYLE = "palette_style"
        private const val KEY_SECURE_DNS = "secure_dns"
        private const val KEY_GLOBAL_PRIVACY_CONTROL = "global_privacy_control"
        private const val KEY_FINGERPRINTING_PROTECTION = "fingerprinting_protection"
        private const val KEY_READER_SIZE = "reader_size"
        private const val KEY_READER_SERIF = "reader_serif"
        private const val KEY_READER_PAGE = "reader_page"
        private const val KEY_APP_LOCK = "app_lock_record"
        private const val KEY_APP_LOCK_FAILS = "app_lock_fails"
        private const val KEY_APP_LOCK_UNTIL = "app_lock_until"
        private const val KEY_HTTPS_ONLY = "https_only"
        private const val KEY_DEVICE_FONT_WEB = "device_font_web"
        private const val KEY_TEXT_SIZE_PERCENT = "text_size_percent"
        const val TEXT_SIZE_MIN = 50
        const val TEXT_SIZE_MAX = 200
        const val TEXT_SIZE_STEP = 10
        private const val KEY_AUTOFILL_PASSWORDS = "autofill_passwords"
        private const val KEY_COOKIE_BEHAVIOR = "cookie_behavior"
        /** GeckoView ContentBlocking.CookieBehavior.ACCEPT_ALL. */
        const val COOKIES_ALLOW_ALL = 0
        /** GeckoView ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY. */
        const val COOKIES_BLOCK_THIRD_PARTY = 1
        /** GeckoView ContentBlocking.CookieBehavior.ACCEPT_NONE. */
        const val COOKIES_BLOCK_ALL = 2
        private const val KEY_PHONE_BAR = "phone_bar_style"
        private const val KEY_TABLET_BAR = "tablet_bar_style"
        const val DEFAULT_HOME_PAGE = "https://www.mozilla.org/"
        /** Search URL used when the address bar input is not a URL. %s is replaced by the query. */
        const val DEFAULT_SEARCH_TEMPLATE = "https://duckduckgo.com/?q=%s"
    }
}
