package com.makemission.ora.extensions

import androidx.compose.runtime.mutableStateMapOf
import com.makemission.ora.labs.PageScan
import com.makemission.ora.log.BrowserLog
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/** Installs the page-reading helper for the scam-pattern lab and keeps the last report per address. */
object SiteScanBridge {
    private const val EXTENSION_ID = "site-scan@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/sitescan/"
    private const val NATIVE_APP = "ora"

    private val reports = mutableStateMapOf<String, PageScan>()
    private var port: WebExtension.Port? = null

    private fun key(url: String) = url.substringBefore('#').trimEnd('/')

    /** The latest scan of the page at [url], or null if none has reported. Read in composition, it updates when a report arrives. */
    fun reportFor(url: String): PageScan? = reports[key(url)]

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { loaded ->
                loaded?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Site scan ready")
            },
            { error -> BrowserLog.error("labs", "Site scan could not start", error) }
        )
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val json = message as? JSONObject ?: return
                    if (json.optString("type") != "scan") return
                    // Only keep the work when the lab is on; the page is read either way, but nothing is stored or judged.
                    if (!com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.SCAM_PATTERNS)) return
                    try {
                        val scan = PageScan.from(json)
                        reports[key(scan.url)] = scan
                    } catch (e: Exception) {
                        BrowserLog.error("labs", "Site scan report could not be read", e)
                    }
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@SiteScanBridge.port === port) this@SiteScanBridge.port = null
                    reports.clear()
                }
            })
        }
    }
}
