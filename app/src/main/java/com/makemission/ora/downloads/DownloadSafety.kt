package com.makemission.ora.downloads

import java.io.InputStream
import java.security.MessageDigest

/** The SHA-256 and MD5 of one file, both in lower-case hex. */
data class FileHashes(val sha256: String, val md5: String)

/**
 * Safety checks for downloads: file types that can run code, plain-http sources, and comparing a file with the
 * SHA-256 or MD5 a site publishes. Pure functions, so they can be tested without a device.
 */
object DownloadSafety {

    /** Types that install software or run code. Ora asks before opening them and never opens them by itself. */
    private val RISKY_EXTENSIONS = setOf(
        "apk", "apks", "xapk", "exe", "msi", "msix", "bat", "cmd", "com", "scr", "sh", "bin", "run",
        "jar", "dmg", "pkg", "app", "deb", "rpm", "appimage", "vbs", "ps1",
    )

    /** True when the file name ends in a type that can harm the device. Case is ignored. */
    fun isRisky(fileName: String): Boolean =
        fileName.substringAfterLast('.', "").lowercase() in RISKY_EXTENSIONS

    /** True for a plain http:// address, whose data anyone on the network can read or change. */
    fun isPlainHttp(url: String): Boolean = url.startsWith("http://", ignoreCase = true)

    /** The hash the user pasted, with spaces and colons removed and letters lower-cased. */
    fun normalize(pasted: String): String = pasted.filterNot { it == ' ' || it == ':' }.lowercase()

    /** Which hash the pasted text is: 64 hex characters is SHA-256, 32 is MD5. Null for anything else. */
    fun algorithmFor(pasted: String): String? {
        val hex = normalize(pasted)
        if (hex.isEmpty() || !hex.all { it in '0'..'9' || it in 'a'..'f' }) return null
        return when (hex.length) {
            64 -> "SHA-256"
            32 -> "MD5"
            else -> null
        }
    }

    /** Reads [input] once and returns both hashes. The caller closes the stream. */
    fun hashes(input: InputStream): FileHashes {
        val sha = MessageDigest.getInstance("SHA-256")
        val md5 = MessageDigest.getInstance("MD5")
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            sha.update(buffer, 0, read)
            md5.update(buffer, 0, read)
        }
        return FileHashes(sha256 = sha.digest().toHex(), md5 = md5.digest().toHex())
    }

    /**
     * Compares the pasted hash with the file's hashes, using the algorithm its length names.
     * Null when the text is not a complete SHA-256 or MD5, so there is nothing to compare yet.
     */
    fun compare(pasted: String, hashes: FileHashes): Boolean? = when (algorithmFor(pasted)) {
        "SHA-256" -> normalize(pasted) == hashes.sha256
        "MD5" -> normalize(pasted) == hashes.md5
        else -> null
    }

    private fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
