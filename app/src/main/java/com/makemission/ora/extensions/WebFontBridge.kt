package com.makemission.ora.extensions

import com.makemission.ora.log.BrowserLog
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/**
 * Talks to the bundled web-font WebExtension (assets/webfont). It puts the font chosen in Settings on every
 * page when the "Use on websites" switch is on. The font files ship inside the extension.
 */
object WebFontBridge {
    private const val EXTENSION_ID = "web-font@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/webfont/"
    private const val NATIVE_APP = "ora"

    private var port: WebExtension.Port? = null
    /** The last key asked for, sent again when the extension connects. Empty means off. */
    private var wanted = ""

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { extension ->
                extension?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Web font ready")
            },
            { error -> BrowserLog.error("webfont", "Web font could not start", error) }
        )
    }

    /** Shows pages in the font with [key], or in their own fonts when [key] is null. */
    fun apply(key: String?) {
        wanted = key.orEmpty()
        send()
    }

    private fun send() {
        val current = port ?: return
        current.postMessage(JSONObject().put("type", "font").put("key", wanted))
        BrowserLog.info("webfont", if (wanted.isEmpty()) "Pages use their own fonts" else "Pages use the $wanted font")
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@WebFontBridge.port === port) this@WebFontBridge.port = null
                    BrowserLog.warn("webfont", "Web font disconnected")
                }
            })
            send()
        }
    }
}
