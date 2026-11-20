package com.makemission.ora.labs

import org.mozilla.geckoview.ContentBlocking
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.text.DateFormat
import java.util.Date

/**
 * Lab: what Ora knows about the page on a tab, read from GeckoView's own connection and content-blocking reports.
 * Nothing is stored: it is rebuilt on every page load and dropped with the tab, so private tabs leave no trace.
 */
data class SiteStats(
    val host: String,
    val secure: Boolean,
    /** Who issued the certificate (organisation, else common name), or null when there is no certificate. */
    val issuer: String? = null,
    val validFrom: Long? = null,
    val validUntil: Long? = null,
    /** SHA-256 of the certificate, grouped for comparing with the site's published one. */
    val certSha256: String? = null,
    val selfIssued: Boolean = false,
    val exception: Boolean = false,
    /** Blocked items on this page by kind, for example "Ads" to 3. */
    val blocked: Map<String, Int> = emptyMap(),
) {
    val blockedTotal: Int get() = blocked.values.sum()

    companion object {
        /** Reads the certificate of a loaded page. */
        fun from(host: String, secure: Boolean, exception: Boolean, cert: X509Certificate?, blocked: Map<String, Int>): SiteStats {
            if (cert == null) return SiteStats(host, secure, exception = exception, blocked = blocked)
            val name = cert.issuerX500Principal.name
            val org = field(name, "O")
            val cn = field(name, "CN")
            val digest = runCatching { MessageDigest.getInstance("SHA-256").digest(cert.encoded) }.getOrNull()
            return SiteStats(
                host = host,
                secure = secure,
                issuer = org ?: cn ?: "Unknown issuer",
                validFrom = cert.notBefore.time,
                validUntil = cert.notAfter.time,
                certSha256 = digest?.let { AppFingerprints.grouped(it.joinToString("") { b -> "%02x".format(b) }) },
                selfIssued = cert.issuerX500Principal == cert.subjectX500Principal,
                exception = exception,
                blocked = blocked,
            )
        }

        private fun field(dn: String, key: String): String? =
            Regex("(?:^|,)$key=((?:\\\\,|[^,])+)").find(dn)?.groupValues?.get(1)?.replace("\\,", ",")?.takeIf { it.isNotBlank() }

        /** The kinds of tracker in a block event's category flags, one name per flag that is set. */
        fun kindsOf(anti: Int, cookies: Int, harmful: Int): List<String> {
            val out = mutableListOf<String>()
            if (anti and ContentBlocking.AntiTracking.AD != 0) out += "Ads"
            if (anti and ContentBlocking.AntiTracking.ANALYTIC != 0) out += "Analytics"
            if (anti and ContentBlocking.AntiTracking.SOCIAL != 0) out += "Social trackers"
            if (anti and ContentBlocking.AntiTracking.CONTENT != 0) out += "Tracking content"
            if (anti and ContentBlocking.AntiTracking.CRYPTOMINING != 0) out += "Cryptominers"
            if (anti and ContentBlocking.AntiTracking.FINGERPRINTING != 0) out += "Fingerprinters"
            if (cookies != 0) out += "Tracker cookies"
            if (harmful != 0) out += "Harmful content"
            return out
        }

        fun date(ms: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(ms))
    }
}
