package com.makemission.ora.devices

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android 17 made talking to other devices on the Wi-Fi a runtime permission (ACCESS_LOCAL_NETWORK). Without it the
 * link cannot announce this device or look for the others, so pairing finds nothing. Older Android versions need
 * nothing, so [granted] is true there.
 */
object LocalNetworkAccess {
    const val PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"

    /** True when this Android version asks for the permission at all. */
    val required: Boolean get() = Build.VERSION.SDK_INT >= 37

    fun granted(context: Context): Boolean =
        !required || context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED
}
