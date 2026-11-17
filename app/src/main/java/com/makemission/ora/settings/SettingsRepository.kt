package com.makemission.ora.settings

import android.content.Context
import android.content.SharedPreferences
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
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
enum class BarStyle(val key: String, val label: String, val detail: String) {
    TOP("top", "Top", "The address bar and every control sit at the top."),
    BOTTOM("bottom", "Bottom", "A larger bar at the bottom with tabs, bookmarks and menu."),
    COMPACT("compact", "Compact", "Only the address bar at the bottom, with a ⋯ menu for tabs.");

    companion object {
        fun fromKey(key: String?, default: BarStyle): BarStyle = entries.firstOrNull { it.key == key } ?: default
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

    private val _secureDns = MutableStateFlow(prefs.getBoolean(KEY_SECURE_DNS, true))
    /** When on, host names resolve over encrypted DNS (DNS over HTTPS), falling back to the network's DNS if that fails. */
    val secureDns: StateFlow<Boolean> = _secureDns.asStateFlow()

    fun setSecureDns(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SECURE_DNS, enabled).apply()
        _secureDns.value = enabled
        BrowserLog.info("settings", "Secure DNS ${if (enabled) "on" else "off"}")
    }

    private val _fadePagesIn = MutableStateFlow(prefs.getBoolean(KEY_FADE_PAGES_IN, true))
    /** When on, a page fades in once it is half loaded instead of showing its pieces as they arrive. */
    val fadePagesIn: StateFlow<Boolean> = _fadePagesIn.asStateFlow()

    fun setFadePagesIn(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_FADE_PAGES_IN, enabled).apply()
        _fadePagesIn.value = enabled
        BrowserLog.info("settings", "Page fade-in ${if (enabled) "on" else "off"}")
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
        private const val KEY_FADE_PAGES_IN = "fade_pages_in"
        private const val KEY_SECURE_DNS = "secure_dns"
        private const val KEY_PHONE_BAR = "phone_bar_style"
        private const val KEY_TABLET_BAR = "tablet_bar_style"
        const val DEFAULT_HOME_PAGE = "https://www.mozilla.org/"
        /** Search URL used when the address bar input is not a URL. %s is replaced by the query. */
        const val DEFAULT_SEARCH_TEMPLATE = "https://duckduckgo.com/?q=%s"
    }
}
