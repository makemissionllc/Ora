package com.makemission.ora.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.makemission.ora.MainActivity
import com.makemission.ora.R
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.notice.InAppNotices

/** Pins a shortcut to a page on the launcher's home screen. Tapping it opens the page in Ora, in a new tab. */
object HomeShortcut {
    fun add(context: Context, url: String, title: String) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            BrowserLog.warn("shortcut", "The launcher does not support pinned shortcuts")
            InAppNotices.show("Your launcher can't add shortcuts")
            return
        }
        val label = title.ifBlank { Uri.parse(url).host.orEmpty() }.ifBlank { "Ora page" }
        val open = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setClass(context, MainActivity::class.java)
        val shortcut = ShortcutInfoCompat.Builder(context, "page-" + url.hashCode())
            .setShortLabel(label.take(24))
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(open)
            .build()
        val asked = ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
        UsageStats.count("menu.add_to_home")
        // The address is not logged: only that a shortcut was asked for.
        BrowserLog.info("shortcut", "Asked the launcher to pin a page shortcut: ${if (asked) "accepted" else "refused"}")
        if (!asked) InAppNotices.show("The launcher did not add the shortcut")
    }
}
