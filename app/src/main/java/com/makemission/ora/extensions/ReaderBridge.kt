package com.makemission.ora.extensions

import androidx.compose.runtime.mutableStateMapOf
import com.makemission.ora.log.BrowserLog
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/** Page colours the reader offers. [key] is what the settings store and the extension use. */
enum class ReaderPage(val key: String, val label: String) {
    STONE("stone", "Stone"),
    PAPER("paper", "Paper"),
    BASALT("basalt", "Basalt");

    companion object {
        fun fromKey(key: String?): ReaderPage = entries.firstOrNull { it.key == key } ?: STONE
    }
}

/** What the reader looks like: text size step (0 to [MAX_SIZE_STEP]), serif or sans, and page colour. */
data class ReaderLook(val sizeStep: Int, val serif: Boolean, val page: ReaderPage) {
    val sizePx: Int get() = SIZES_PX[sizeStep.coerceIn(0, MAX_SIZE_STEP)]

    companion object {
        private val SIZES_PX = intArrayOf(17, 20, 23, 27, 32)
        const val MAX_SIZE_STEP = 4
    }
}

/**
 * One tab's reader, read from what the page's content script last reported. A page is matched to its
 * tab by address, because the extension reports in terms of its own tab ids.
 */
class ReaderState(private val urlOf: () -> String) {
    private val page: ReaderBridge.Page? get() = ReaderBridge.pageFor(urlOf())

    /** True when the loaded page has an article the reader can show. */
    val readable: Boolean get() = page?.readable == true

    /** True while the reader is drawn over the page. */
    val open: Boolean get() = page?.open == true

    val site: String get() = page?.site.orEmpty()

    val minutes: Int get() = page?.minutes ?: 0

    /** Asks the page to show the reader. Returns false when the page cannot be reached. */
    fun show(look: ReaderLook): Boolean = ReaderBridge.send(page, ReaderBridge.lookMessage("open", look))

    fun restyle(look: ReaderLook) {
        if (open) ReaderBridge.send(page, ReaderBridge.lookMessage("style", look))
    }

    fun close() {
        val current = page ?: return
        if (!current.open) return
        ReaderBridge.send(current, JSONObject().put("type", "close"))
        ReaderBridge.update(current.copy(open = false))
        BrowserLog.info("reader", "Reader closed")
    }
}

/** Installs the reader extension and keeps what each page reported. */
object ReaderBridge {
    private const val EXTENSION_ID = "reader-view@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/reader/"
    private const val NATIVE_APP = "ora"

    /** What a page's content script last reported, by the extension's tab id. [at] orders reports. */
    data class Page(val tab: Int, val url: String, val readable: Boolean, val site: String, val minutes: Int, val open: Boolean, val at: Long)

    private val pages = mutableStateMapOf<Int, Page>()
    private var port: WebExtension.Port? = null

    private fun key(url: String) = url.substringBefore('#').trimEnd('/')

    /** The most recent report for [url], or null when no page at that address has reported. */
    fun pageFor(url: String): Page? =
        if (url.isBlank()) null else pages.values.filter { key(it.url) == key(url) }.maxByOrNull { it.at }

    internal fun update(page: Page) {
        pages[page.tab] = page
    }

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { loaded ->
                loaded?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Reader view ready")
            },
            { error -> BrowserLog.error("reader", "Reader view could not start", error) }
        )
    }

    internal fun lookMessage(type: String, look: ReaderLook) =
        JSONObject().put("type", type).put("size", look.sizePx).put("serif", look.serif).put("page", look.page.key)

    internal fun send(page: Page?, message: JSONObject): Boolean {
        val current = port
        if (current == null || page == null) {
            BrowserLog.error("reader", "Reader could not reach the page")
            return false
        }
        // Several tabs can hold the same address and the page cannot tell which one is ours, so every
        // tab on that address gets the message. The extra ones are background tabs and stay unseen.
        pages.values.filter { key(it.url) == key(page.url) }.ifEmpty { listOf(page) }.forEach {
            current.postMessage(JSONObject(message.toString()).put("tab", it.tab))
        }
        return true
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            BrowserLog.info("reader", "Reader connected")
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val json = message as? JSONObject ?: return
                    val tab = json.optInt("tab", -1)
                    if (tab < 0) return
                    val url = json.optString("url")
                    when (json.optString("type")) {
                        "ready" -> {
                            // A new document in this tab: the reader is gone and the page has been judged again.
                            val readable = json.optBoolean("readable")
                            update(Page(tab, url, readable, json.optString("site"), json.optInt("minutes"), json.optBoolean("open"), System.nanoTime()))
                            BrowserLog.info("reader", "Page report: ${if (readable) "article found" else "no article"}")
                        }
                        "opened" -> {
                            val before = pages[tab] ?: return
                            val ok = json.optBoolean("ok")
                            update(before.copy(open = ok, site = json.optString("site", before.site), minutes = json.optInt("minutes", before.minutes)))
                            if (ok) BrowserLog.info("reader", "Reader opened") else BrowserLog.warn("reader", "Reader found no article on the page")
                        }
                        "error" -> BrowserLog.error("reader", "Reader script failed: ${json.optString("message")}")
                    }
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@ReaderBridge.port === port) this@ReaderBridge.port = null
                    pages.clear()
                    BrowserLog.info("reader", "Reader disconnected")
                }
            })
        }
    }
}
