package com.makemission.ora.browser

import androidx.compose.ui.graphics.Color
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/** A page's `<meta name="theme-color">`, or null when the page does not set one. [url] is the page it came from. */
data class ThemeColorUpdate(val url: String, val color: Color?)

/**
 * Reads each page's theme-colour through a built-in WebExtension (assets/theme-color). GeckoView has no
 * theme-colour callback, so the extension's content script reports it and this object forwards it.
 */
object ThemeColor {
    const val EXTENSION_ID = "theme-color@ora.makemission"
    private const val LOCATION = "resource://android/assets/theme-color/"

    private val _updates = MutableSharedFlow<ThemeColorUpdate>(extraBufferCapacity = 16)
    /** Emits whenever a page reports its theme-colour, including when it has none. */
    val updates: SharedFlow<ThemeColorUpdate> = _updates.asSharedFlow()

    /** Installs the built-in reader once. Failures are logged; the status bar then falls back to sampling. */
    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(LOCATION, EXTENSION_ID).accept(
            { extension ->
                extension?.setMessageDelegate(delegate, "browser")
                BrowserLog.info("extensions", "Theme colour reader is ready")
            },
            { error -> BrowserLog.error("extensions", "Theme colour reader could not load", error) }
        )
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
            val json = message as? JSONObject ?: return null
            val url = json.optString("url")
            if (url.isEmpty()) return null
            val rgb = json.optJSONObject("color")
            val color = rgb?.let { Color(it.optInt("r"), it.optInt("g"), it.optInt("b")) }
            _updates.tryEmit(ThemeColorUpdate(url, color))
            return null
        }
    }
}
