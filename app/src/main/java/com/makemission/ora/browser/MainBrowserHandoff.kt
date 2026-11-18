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

    /** Every browser that can open [url] except Ora, by package name. */
    private fun otherBrowsers(context: Context, url: String): List<String> {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
        return context.packageManager.queryIntentActivities(view, PackageManager.ResolveInfoFlags.of(0))
            .map { it.activityInfo.packageName }
            .filter { it != context.packageName }
            .distinct()
    }

    /** True when the row is worth showing: an https page and at least one other browser to hand it to. */
    fun isAvailable(context: Context, url: String): Boolean =
        isOffered(url) && try { otherBrowsers(context, url).isNotEmpty() } catch (e: Exception) { false }

    /** Opens the chooser with every browser except Ora. Returns false when there is none or Android refused. */
    fun open(context: Context, url: String, isPrivate: Boolean): Boolean {
        val site = if (isPrivate) "a private tab" else LogPrivacy.domainOf(url)
        return try {
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
            val others = otherBrowsers(context, url)
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
