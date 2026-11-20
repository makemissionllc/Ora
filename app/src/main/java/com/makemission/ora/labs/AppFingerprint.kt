package com.makemission.ora.labs

import android.content.Context
import android.content.pm.PackageManager
import com.makemission.ora.log.BrowserLog
import java.io.File
import java.security.MessageDigest

/** SHA-256 of what is installed: the package file and the certificate it is signed with. Read on the device, nothing is sent. */
data class AppFingerprint(val packageSha256: String, val signerSha256: String)

object AppFingerprints {
    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    /** Null when it cannot be read; the reason goes to the log. Slow for a large package, so call it off the main thread. */
    fun read(context: Context): AppFingerprint? = try {
        val pm = context.packageManager
        val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signer = info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            ?: error("No signing certificate found")
        val md = MessageDigest.getInstance("SHA-256")
        File(context.applicationInfo.sourceDir).inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) { val n = input.read(buf); if (n < 0) break; md.update(buf, 0, n) }
        }
        BrowserLog.info("labs", "App fingerprint read")
        AppFingerprint(hex(md.digest()), hex(MessageDigest.getInstance("SHA-256").digest(signer)))
    } catch (e: Exception) {
        BrowserLog.error("labs", "Could not read the app fingerprint", e)
        null
    }

    /** Groups a hex string in fours so it is easier to compare by eye. */
    fun grouped(hex: String) = hex.chunked(8).joinToString(" ")
}
