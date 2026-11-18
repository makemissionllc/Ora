package com.makemission.ora.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy

/**
 * "Passkey not working? Open in main browser". Ora's GeckoView asks Android for passkeys, and Google has to approve
 * the browser's package before it answers, so a site's passkey sign-in can fail here. This hands the page's address to
 * another browser through the system chooser; Ora never touches the passkey itself.
 */
object MainBrowserHandoff {
    /** The row is only offered on pages served over https. */
    fun isOffered(url: String): Boolean = url.startsWith("https://", ignoreCase = true)

    /** Opens the chooser with every browser except Ora. Returns false when there is none or Android refused. */
    fun open(context: Context, url: String, isPrivate: Boolean): Boolean {
        val site = if (isPrivate) "a private tab" else LogPrivacy.domainOf(url)
        return try {
            val pm = context.packageManager
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
            val others = pm.queryIntentActivities(view, PackageManager.ResolveInfoFlags.of(0))
                .map { it.activityInfo.packageName }
                .filter { it != context.packageName }
                .distinct()
            if (others.isEmpty()) {
                BrowserLog.warn("passkey", "No other browser to hand $site to")
                return false
            }
            val intents = others.map { Intent(view).setPackage(it) }
            val chooser = Intent.createChooser(intents.first(), "Open in main browser")
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.drop(1).toTypedArray())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            BrowserLog.info("passkey", "Handed $site to the main browser chooser")
            true
        } catch (e: Exception) {
            BrowserLog.warn("passkey", "Could not hand $site to another browser", e)
            false
        }
    }
}
