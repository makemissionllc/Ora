package com.makemission.ora.extensions

import com.makemission.ora.log.BrowserLog
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/**
 * Talks to the bundled element zapper WebExtension (assets/zapper). Ora sends it the hide rules and asks for a pick;
 * the page on screen shows the picker, and the element chosen comes back through [onZapped] to be saved.
 */
object ZapperBridge {
    private const val EXTENSION_ID = "zapper@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/zapper/"
    private const val NATIVE_APP = "ora"

    private var port: WebExtension.Port? = null
    private var rules: Map<String, List<String>> = emptyMap()

    /** Called with the site and CSS selector of an element the user chose to hide (never for private tabs). */
    var onZapped: ((host: String, selector: String) -> Unit)? = null
    /** Called when the picker closes, whether an element was hidden or not. */
    var onEnded: (() -> Unit)? = null

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { extension ->
                extension?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Element zapper ready")
            },
            { error -> BrowserLog.error("zapper", "Element zapper could not start", error) }
        )
    }

    /** Sends the full set of rules to every page. */
    fun setRules(map: Map<String, List<String>>) {
        rules = map
        sendRules()
    }

    /** Starts the picker on the page on screen. [persist] is false for a private tab, so nothing is saved. */
    fun pick(persist: Boolean): Boolean {
        val p = port ?: return false
        p.postMessage(JSONObject().put("type", "pick").put("persist", persist))
        BrowserLog.info("zapper", "Picker started")
        return true
    }

    fun cancel() { port?.postMessage(JSONObject().put("type", "cancel")) }

    private fun sendRules() {
        val p = port ?: return
        val o = JSONObject()
        rules.forEach { (h, l) -> o.put(h, JSONArray(l)) }
        p.postMessage(JSONObject().put("type", "rules").put("rules", o))
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val o = message as? JSONObject ?: return
                    when (o.optString("type")) {
                        "zapped" -> if (o.optBoolean("persist", true)) onZapped?.invoke(o.optString("host"), o.optString("selector"))
                        "ended" -> onEnded?.invoke()
                    }
                }
                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@ZapperBridge.port === port) this@ZapperBridge.port = null
                    BrowserLog.warn("zapper", "Element zapper disconnected")
                }
            })
            sendRules()
        }
    }
}
