package com.makemission.ora.settings

import android.content.Context
import android.content.SharedPreferences
import com.makemission.ora.log.BrowserLog
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
 * Single source of truth for all app settings.
 *
 * To add a setting: add a key constant, a default, a MutableStateFlow backed
 * by [prefs], and a setter. Future extension/tweak settings belong here too.
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "ora_settings"
        private const val KEY_HOME_PAGE = "home_page"
        private const val KEY_APPEARANCE = "appearance"
        private const val KEY_WEBSITE_THEME = "website_theme"
        const val DEFAULT_HOME_PAGE = "https://www.mozilla.org/"
        /** Search URL used when the address bar input is not a URL. %s is replaced by the query. */
        const val DEFAULT_SEARCH_TEMPLATE = "https://duckduckgo.com/?q=%s"
    }
}
