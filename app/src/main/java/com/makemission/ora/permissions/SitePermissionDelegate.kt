package com.makemission.ora.permissions

import android.net.Uri
import com.makemission.ora.log.BrowserLog
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PermissionDelegate.ContentPermission

/**
 * Answers one tab's permission requests. A site's saved answer is used when there is one. Otherwise the user
 * is asked through [SitePrompts]. Private tabs ask every time and save nothing.
 */
class SitePermissionDelegate(
    private val tabId: Long,
    private val isPrivate: Boolean,
    private val store: SitePermissionStore,
) : GeckoSession.PermissionDelegate {

    override fun onContentPermissionRequest(
        session: GeckoSession,
        perm: ContentPermission,
    ): GeckoResult<Int>? {
        val permission = when (perm.permission) {
            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> SitePermission.LOCATION
            GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> SitePermission.NOTIFICATIONS
            // Permissions Ora does not handle keep GeckoView's own behaviour.
            else -> return null
        }
        val result = GeckoResult<Int>()
        decide(perm.uri, listOf(permission)) { allowed ->
            result.complete(if (allowed) ContentPermission.VALUE_ALLOW else ContentPermission.VALUE_DENY)
        }
        return result
    }

    override fun onMediaPermissionRequest(
        session: GeckoSession,
        uri: String,
        video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback,
    ) {
        val types = buildList {
            if (!video.isNullOrEmpty()) add(SitePermission.CAMERA)
            if (!audio.isNullOrEmpty()) add(SitePermission.MICROPHONE)
        }
        if (types.isEmpty()) {
            callback.reject()
            return
        }
        decide(uri, types) { allowed ->
            if (allowed) {
                callback.grant(video?.firstOrNull()?.id, audio?.firstOrNull()?.id)
            } else {
                callback.reject()
            }
        }
    }

    override fun onAndroidPermissionsRequest(
        session: GeckoSession,
        permissions: Array<String>?,
        callback: GeckoSession.PermissionDelegate.Callback,
    ) {
        val needed = permissions.orEmpty().toList()
        if (needed.isEmpty()) {
            callback.grant()
            return
        }
        BrowserLog.info("permission", "Asking Android for ${needed.joinToString { it.substringAfterLast('.') }}")
        SitePrompts.askAndroid(AndroidPermissionRequest(needed.toTypedArray()) { results ->
            val granted = needed.all { results[it] == true }
            BrowserLog.info("permission", if (granted) "Android permission granted" else "Android permission not granted")
            if (granted) callback.grant() else callback.reject()
        })
    }

    /**
     * Uses the saved answers for [types] when they are known, otherwise asks the user about the ones that are not.
     * [done] is called once with true when the site may use them.
     */
    private fun decide(uri: String?, types: List<SitePermission>, done: (Boolean) -> Unit) {
        val origin = originOf(uri)
        if (origin == null) {
            BrowserLog.warn("permission", "A request came without a web address, so it was denied")
            done(false)
            return
        }
        val names = types.joinToString { it.key }
        // Private tabs have no saved answers, so they always ask.
        val saved = if (isPrivate) emptyMap() else types.associateWith { store.saved(origin, it) }
        if (saved.values.any { it == false }) {
            BrowserLog.info("permission", "Blocked by a saved choice: $names")
            done(false)
            return
        }
        val unknown = types.filter { saved[it] == null }
        if (unknown.isEmpty()) {
            BrowserLog.info("permission", "Allowed by a saved choice: $names")
            done(true)
            return
        }
        BrowserLog.info("permission", if (isPrivate) "Asking about $names in a private tab" else "Asking about $names")
        SitePrompts.ask(SitePrompt(tabId, hostOf(origin), unknown, isPrivate) { choice ->
            val allowed = choice == SitePromptChoice.ALLOW
            // Allow and Block are remembered. Not now is answered for this request only.
            if (!isPrivate && choice != SitePromptChoice.NOT_NOW) {
                unknown.forEach { store.save(origin, it, allowed) }
            }
            BrowserLog.info(
                "permission",
                when (choice) {
                    SitePromptChoice.ALLOW -> "User allowed $names"
                    SitePromptChoice.BLOCK -> "User blocked $names"
                    SitePromptChoice.NOT_NOW -> "User chose not now for $names"
                }
            )
            done(allowed)
        })
    }

    companion object {
        /** The scheme, host and port of a web address, such as https://example.com. Null for anything that is not http or https. */
        fun originOf(uri: String?): String? {
            if (uri.isNullOrBlank()) return null
            val parsed = Uri.parse(uri)
            val scheme = parsed.scheme ?: return null
            if (scheme != "http" && scheme != "https") return null
            val host = parsed.host?.takeIf { it.isNotBlank() } ?: return null
            return if (parsed.port == -1) "$scheme://$host" else "$scheme://$host:${parsed.port}"
        }

        /** The host of an origin from [originOf], for showing to the user. */
        fun hostOf(origin: String): String = Uri.parse(origin).host ?: origin
    }
}
