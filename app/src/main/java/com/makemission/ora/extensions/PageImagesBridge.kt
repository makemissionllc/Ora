package com.makemission.ora.extensions

import android.os.Handler
import android.os.Looper
import com.makemission.ora.log.BrowserLog
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/** One image found on a page: its address and the size the page reported (0 when not loaded yet). */
data class PageImage(val url: String, val width: Int, val height: Int)

/**
 * Installs the helper that lists a page's images for Save all images. The page is read only when
 * [collect] is called; nothing is gathered in the background. Pages tell the helper which tab they are in
 * when they load, and a request names that tab.
 */
object PageImagesBridge {
    private const val EXTENSION_ID = "page-images@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/pageimages/"
    private const val NATIVE_APP = "ora"
    private const val TIMEOUT_MS = 4_000L

    private val main = Handler(Looper.getMainLooper())
    private val tabs = mutableMapOf<String, Int>()
    private val waiting = mutableMapOf<Int, (List<PageImage>?) -> Unit>()
    private var nextId = 1
    private var port: WebExtension.Port? = null

    private fun key(url: String) = url.substringBefore('#').trimEnd('/')

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { loaded ->
                loaded?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Page images helper ready")
            },
            { error -> BrowserLog.error("extensions", "Page images helper could not start", error) }
        )
    }

    /** True once the helper has seen a page at [url], so [collect] can reach it. */
    fun canRead(url: String): Boolean = port != null && tabs.containsKey(key(url))

    /** Asks the page at [url] for its images. [onDone] runs on the main thread with the list, or null if the page could not be read. */
    fun collect(url: String, onDone: (List<PageImage>?) -> Unit) {
        val tab = tabs[key(url)]
        val p = port
        if (tab == null || p == null) { onDone(null); return }
        val id = nextId++
        waiting[id] = onDone
        try {
            p.postMessage(JSONObject().put("type", "collect").put("tab", tab).put("id", id))
        } catch (e: Exception) {
            waiting.remove(id)
            BrowserLog.warn("extensions", "Could not ask the page for its images", e)
            onDone(null)
            return
        }
        main.postDelayed({
            waiting.remove(id)?.let {
                BrowserLog.warn("extensions", "The page did not answer the image request in time")
                it(null)
            }
        }, TIMEOUT_MS)
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val json = message as? JSONObject ?: return
                    val url = json.optString("url")
                    when (json.optString("type")) {
                        "hello" -> if (json.has("tab")) main.post { tabs[key(url)] = json.getInt("tab") }
                        "images" -> {
                            val items = json.optJSONArray("items")
                            val list = ArrayList<PageImage>()
                            if (items != null) for (i in 0 until items.length()) {
                                val o = items.optJSONObject(i) ?: continue
                                list.add(PageImage(o.optString("src"), o.optInt("w"), o.optInt("h")))
                            }
                            main.post { waiting.remove(json.optInt("id"))?.invoke(list) }
                        }
                    }
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@PageImagesBridge.port === port) this@PageImagesBridge.port = null
                    main.post { tabs.clear() }
                }
            })
        }
    }
}
