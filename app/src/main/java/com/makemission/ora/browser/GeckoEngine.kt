package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.extensions.ReaderBridge
import com.makemission.ora.extensions.VideoControlBridge
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.passwords.PasswordStore
import com.makemission.ora.passwords.PasswordStorageDelegate
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import java.io.File
import java.io.IOException

/**
 * GeckoView needs exactly one [GeckoRuntime] per process, so it is shared here
 * and every browser session is opened against it.
 */
object GeckoEngine {
    private var runtime: GeckoRuntime? = null

    fun runtime(context: Context): GeckoRuntime =
        runtime ?: createRuntime(context.applicationContext).also {
            runtime = it
            // Saved logins: GeckoView asks this delegate for logins to fill and hands back submitted ones.
            PasswordStore.init(context.applicationContext)
            it.setAutocompleteStorageDelegate(PasswordStorageDelegate)
            // Bundled helper that lets the full-screen video seek bar read and move the page's video.
            VideoControlBridge.install(it)
            // Bundled helper that finds a page's article for Reader view.
            ReaderBridge.install(it)
        }

    /**
     * Starts GeckoView with Gecko's own pop-up blocker switched off. Gecko drops a window.open() that no tap
     * caused before GeckoView reports it, so the pop-up card never got a chance to show. With the blocker off,
     * every pop-up reaches the navigation delegate in [BrowserTab], which opens it, asks, or blocks it.
     *
     * GeckoView sets preferences at start-up only through a config file, so the setting is written to app
     * storage and passed as the config file path. If the file cannot be written, the runtime starts with its
     * default settings, the no-tap card will not appear, and the failure is logged.
     */
    private fun createRuntime(context: Context): GeckoRuntime {
        val config = writePopupConfig(context) ?: return GeckoRuntime.getDefault(context)
        val settings = GeckoRuntimeSettings.Builder().configFilePath(config.absolutePath).build()
        return GeckoRuntime.create(context, settings)
    }

    private fun writePopupConfig(context: Context): File? =
        try {
            File(context.filesDir, "gecko-config.yaml").also { it.writeText(POPUP_CONFIG) }
        } catch (error: IOException) {
            BrowserLog.warn("popup", "Could not write the GeckoView config, so no-tap pop-ups may be dropped", error)
            null
        }

    private const val POPUP_CONFIG = """prefs:
  dom.disable_open_during_load: false
env: {}
args: []
"""
}
