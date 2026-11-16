package com.makemission.ora.browser

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime

/**
 * GeckoView needs exactly one [GeckoRuntime] per process, so it is shared here
 * and every browser session is opened against it.
 */
object GeckoEngine {
    private var runtime: GeckoRuntime? = null

    fun runtime(context: Context): GeckoRuntime =
        runtime ?: GeckoRuntime.getDefault(context.applicationContext).also { runtime = it }
}
