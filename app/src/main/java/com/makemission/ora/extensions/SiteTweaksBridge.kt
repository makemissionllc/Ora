package com.makemission.ora.extensions

import com.makemission.ora.log.BrowserLog
import com.makemission.ora.settings.SiteTweak
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/** Talks to the bundled site tweaks WebExtension (assets/sitetweaks): sends it the per-site tweaks, which it applies on each page. */
object SiteTweaksBridge {
    private const val EXTENSION_ID = "sitetweaks@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/sitetweaks/"
    private const val NATIVE_APP = "ora"

    private var port: WebExtension.Port? = null
    private var wanted: Map<String, SiteTweak> = emptyMap()

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { extension ->
                extension?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Site tweaks ready")
            },
            { error -> BrowserLog.error("tweaks", "Site tweaks could not start", error) }
        )
    }

    fun apply(tweaks: Map<String, SiteTweak>) {
        wanted = tweaks
        send()
    }

    private fun send() {
        val p = port ?: return
        val o = JSONObject()
        wanted.forEach { (site, t) -> o.put(site, JSONObject().put("dark", t.dark).put("wide", t.wide).put("font", t.font)) }
        p.postMessage(JSONObject().put("type", "tweaks").put("tweaks", o))
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@SiteTweaksBridge.port === port) this@SiteTweaksBridge.port = null
                    BrowserLog.warn("tweaks", "Site tweaks disconnected")
                }
            })
            send()
        }
    }
}
