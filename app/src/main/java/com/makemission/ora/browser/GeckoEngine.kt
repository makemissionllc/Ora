package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.extensions.ReaderBridge
import com.makemission.ora.extensions.VideoControlBridge
import com.makemission.ora.passwords.PasswordStore
import com.makemission.ora.passwords.PasswordStorageDelegate
import org.mozilla.geckoview.GeckoRuntime

/**
 * GeckoView needs exactly one [GeckoRuntime] per process, so it is shared here
 * and every browser session is opened against it.
 */
object GeckoEngine {
    private var runtime: GeckoRuntime? = null

    fun runtime(context: Context): GeckoRuntime =
        runtime ?: GeckoRuntime.getDefault(context.applicationContext).also {
            runtime = it
            // Saved logins: GeckoView asks this delegate for logins to fill and hands back submitted ones.
            PasswordStore.init(context.applicationContext)
            it.setAutocompleteStorageDelegate(PasswordStorageDelegate)
            // Bundled helper that lets the full-screen video seek bar read and move the page's video.
            VideoControlBridge.install(it)
            // Bundled helper that finds a page's article for Reader view.
            ReaderBridge.install(it)
        }
}
