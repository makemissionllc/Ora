package com.makemission.ora.devices

import android.content.Context
import com.makemission.ora.log.BrowserLog

/**
 * Wires the device-link features to the one [DeviceLink]. Each feature adds its message handler here, so the
 * link itself stays ignorant of tabs, pages and lists.
 */
object DeviceHub {
    @Volatile private var inboxInstance: TabInbox? = null

    fun link(context: Context): DeviceLink = DeviceLink.get(context)

    fun inbox(context: Context): TabInbox =
        inboxInstance ?: synchronized(this) { inboxInstance ?: TabInbox(context.applicationContext).also { inboxInstance = it } }

    /** Turns the link on or off to match the Settings switch, and keeps the advertised name current. */
    fun apply(context: Context, enabled: Boolean, name: String, shareCurrentPage: Boolean = true, syncReading: Boolean = true) {
        ContinueState.sharing = shareCurrentPage
        val link = link(context)
        if (enabled && !LocalNetworkAccess.granted(context)) {
            BrowserLog.warn("devices", "Device link is on but Android has not allowed local network access, so it is not started")
            link.stop()
        } else if (enabled) {
            link.on(TYPE_TAB) { from, body -> inbox(context).receive(from, body) }
            if (syncReading) link.on(TYPE_READING) { _, body -> ReadingSync.handle(context.applicationContext, body) }
            link.on(TYPE_WHERE) { _, _ -> ContinueState.answer() }
            link.start(name)
        } else {
            link.stop()
        }
    }

    const val TYPE_TAB = "tab"
    const val TYPE_WHERE = "where"
    const val TYPE_READING = "reading"
}
