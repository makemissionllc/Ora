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
    data class State(
        val hasVideo: Boolean,
        val seconds: Double,
        val duration: Double,
        val paused: Boolean,
        val ended: Boolean = false,
        val rate: Double = 1.0,
        val muted: Boolean = false,
        val width: Int = 0,
        val height: Int = 0,
        val title: String = "",
    )

    private val _state = MutableStateFlow<State?>(null)
    val state: StateFlow<State?> = _state.asStateFlow()

    private var port: WebExtension.Port? = null

    /** Counts touches on a full-screen video's page, so the app can show its small controls handle for a moment. */
    private val _pokes = MutableStateFlow(0)
    val pokes: StateFlow<Int> = _pokes.asStateFlow()

    private var oraControls = false

    /**
     * Tells the page whether Ora's own controls are on. When they are not, the site's controls stay visible and
     * untouched; when they are, the site's are hidden so the two sets do not draw over each other.
     */
    fun setOraControls(on: Boolean) {
        oraControls = on
        port?.postMessage(JSONObject().put("type", "ui").put("ora", on))
    }

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

    private fun command(message: JSONObject, what: String) {
        val current = port
        if (current == null) {
            BrowserLog.error("video", "$what skipped: the video control is not connected")
            return
        }
        current.postMessage(message)
    }

    fun play() = command(JSONObject().put("type", "play"), "Play")
    fun pause() = command(JSONObject().put("type", "pause"), "Pause")

    /** Asks the page itself to leave full screen, so it can enter full screen again afterwards. */
    fun exitFullScreen() = command(JSONObject().put("type", "exit"), "Exit full screen")

    /** Sets the playback speed, 1.0 is normal. */
    fun setRate(rate: Double) = command(JSONObject().put("type", "rate").put("value", rate), "Speed")
    fun setMuted(muted: Boolean) = command(JSONObject().put("type", "mute").put("value", muted), "Mute")

    /** [cover] fills the screen and crops the edges; otherwise the whole picture is shown. */
    fun setFit(cover: Boolean) = command(JSONObject().put("type", "fit").put("value", if (cover) "cover" else "contain"), "Fit")

    /** Moves the full-screen video by [seconds] from where the page last reported it, clamped to its length. */
    fun skip(seconds: Double) {
        val now = _state.value
        if (now == null || !now.hasVideo) {
            BrowserLog.error("video", "Skip skipped: no full-screen video is reported")
            return
        }
        val end = if (now.duration > 0.0) now.duration else now.seconds + seconds
        seek((now.seconds + seconds).coerceIn(0.0, end))
    }

    private val delegate = object : WebExtension.MessageDelegate {
        override fun onConnect(connected: WebExtension.Port) {
            port = connected
            BrowserLog.info("video", "Video control connected")
            connected.postMessage(JSONObject().put("type", "ui").put("ora", oraControls))
            connected.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    val json = message as? JSONObject ?: return
                    if (json.optString("type") == "poke") { _pokes.value++; return }
                    if (json.optString("type") != "state") return
                    _state.value = State(
                        hasVideo = json.optBoolean("has"),
                        seconds = json.optDouble("t", 0.0),
                        duration = json.optDouble("d", 0.0),
                        paused = json.optBoolean("paused"),
                        ended = json.optBoolean("ended"),
                        rate = json.optDouble("rate", 1.0),
                        muted = json.optBoolean("muted"),
                        width = json.optInt("w"),
                        height = json.optInt("h"),
                        title = json.optString("title"),
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
