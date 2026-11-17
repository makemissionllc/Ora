package com.makemission.ora.extensions

import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension

/**
 * Talks to the bundled video-control WebExtension (assets/videoctl). The extension reports the
 * full-screen video's time and duration, and the seek bar sends seek requests back through here.
 */
object VideoControlBridge {

    private const val EXTENSION_ID = "video-control@ora.makemission.com"
    private const val RESOURCE_PATH = "resource://android/assets/videoctl/"
    private const val NATIVE_APP = "ora"

    /** The full-screen video as the page reported it. [hasVideo] is false when no video is full screen. */
    data class State(val hasVideo: Boolean, val seconds: Double, val duration: Double, val paused: Boolean)

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    private var port: WebExtension.Port? = null

    fun install(runtime: GeckoRuntime) {
        runtime.webExtensionController.ensureBuiltIn(RESOURCE_PATH, EXTENSION_ID).accept(
            { extension ->
                extension?.setMessageDelegate(delegate, NATIVE_APP)
                BrowserLog.info("extensions", "Video control ready")
            },
            { error -> BrowserLog.error("video", "Video control could not start", error) }
        )
    }

    /** Asks the page's full-screen video to jump to [seconds]. */
    fun seek(seconds: Double) {
        val current = port
        if (current == null) {
            BrowserLog.error("video", "Seek skipped: the video control is not connected")
            return
        }
        current.postMessage(JSONObject().put("type", "seek").put("time", seconds))
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            BrowserLog.info("video", "Video control connected")
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val json = message as? JSONObject ?: return
                    if (json.optString("type") != "state") return
                    _state.value = State(
                        hasVideo = json.optBoolean("has"),
                        seconds = json.optDouble("t", 0.0),
                        duration = json.optDouble("d", 0.0),
                        paused = json.optBoolean("paused"),
                    )
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    if (this@VideoControlBridge.port === port) this@VideoControlBridge.port = null
                    _state.value = null
                    BrowserLog.info("video", "Video control disconnected")
                }
            })
        }
    }
}
