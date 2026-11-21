package com.makemission.ora.devices

import android.content.Context

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
    fun apply(context: Context, enabled: Boolean, name: String) {
        val link = link(context)
        if (enabled) {
            link.on(TYPE_TAB) { from, body -> inbox(context).receive(from, body) }
            link.start(name)
        } else {
            link.stop()
        }
    }

    const val TYPE_TAB = "tab"
}
