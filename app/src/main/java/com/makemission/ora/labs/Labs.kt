package com.makemission.ora.labs

import java.net.IDN
import java.net.URI

/**
 * Opt-in security features, shown under Settings > Labs. Each one is off until the person switches it on.
 * Every entry maps to something GeckoView or Ora really does; nothing here is a switch with no effect.
 */
enum class LabFeature(val key: String, val label: String, val detail: String, val restart: Boolean = false) {
    TRACKING_PARAMS(
        "tracking_params", "Strip tracking parameters",
        "Removes known tracking tags such as utm_source or fbclid from addresses before the page loads."
    ),
    BOUNCE_TRACKERS(
        "bounce_trackers", "Bounce-tracker protection",
        "Clears the cookies and storage of sites that only flash by in a redirect to follow you around."
    ),
    COOKIE_PURGING(
        "cookie_purging", "Purge tracker cookies",
        "Regularly deletes the cookies of known trackers you never visited on purpose."
    ),
    LOCAL_NETWORK(
        "local_network", "Block local network access",
        "Stops websites from reaching devices on your home or work network, such as a router or printer, unless you allow it."
    ),
    REDUCED_REFERRER(
        "reduced_referrer", "Reduced referrer",
        "Sends only the site's name, not the full address, as the page you came from, and nothing to other sites unless they match. Applies the next time Ora starts.",
        restart = true
    ),
    GENERIC_USER_AGENT(
        "generic_user_agent", "Generic browser identity",
        "Pages see a plain Firefox on Android identity with no device model or build details. Tabs in desktop-site mode keep their desktop identity. Applies on the next page load."
    ),
    LOOKALIKE_WARNING(
        "lookalike_warning", "Lookalike address warning",
        "Tells you when a page address is made of mixed alphabets or encoded (xn--) letters, which scammers use to imitate real sites. "
    ),
    MIXED_CONTENT(
        "mixed_content", "Mixed content warning",
        "Tells you when a secure page also loaded scripts, pictures or media over an unencrypted connection."
    ),
    INSECURE_PAGE(
        "insecure_page", "Unencrypted page notice",
        "Tells you when you are on a page that is not encrypted (http), where others on the network can read what you send."
    ),
    CERT_CHECKS(
        "cert_checks", "Certificate checks",
        "Tells you when a site's certificate is self-issued, runs out within two weeks, or has a security exception in place."
    ),
    REDIRECT_WARNING(
        "redirect_warning", "Redirect chain warning",
        "Tells you when a page was reached through four or more redirects in a row."
    ),
    SOCIAL_TRACKERS(
        "social_trackers", "Strict social tracker blocking",
        "Also blocks social network buttons and widgets that follow you across sites. Some embedded posts may not show."
    ),
    SCAM_PATTERNS(
        "scam_patterns", "Scam and AI-built site patterns",
        "Reads the page on your device and points out common scam signs (fake urgency, prizes, look-alike sign-ins, wallet phrases) and signs of a site-builder or generated page. These are hints, not proof: real shops can trigger them."
    ),
    SCENARIO_LOG(
        "scenario_log", "Log risky situations",
        "Writes to Diagnostics when a page fails its security check, crashes, or sends you through a long chain of redirects."
    );

    companion object {
        fun fromKey(key: String): LabFeature? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Which features are on, readable from any thread. [com.makemission.ora.settings.SettingsRepository] owns the saved
 * state and writes it here, the same way the motion style is shared.
 */
object Labs {
    @Volatile
    var enabled: Set<LabFeature> = emptySet()

    fun on(feature: LabFeature): Boolean = feature in enabled

    /** A redirect chain this long is worth a note in the log. */
    const val LONG_REDIRECT_CHAIN = 4

    /** What Firefox on Android sends when it hides device details. */
    const val GENERIC_USER_AGENT = "Mozilla/5.0 (Android 10; Mobile; rv:160.0) Gecko/160.0 Firefox/160.0"
}

/** One thing Ora noticed about a site. [leave] marks the serious ones, which add a Go back button to the card. */
data class SiteFinding(val id: String, val title: String, val detail: String, val leave: Boolean = false)

/** An address that looks like an imitation: what the browser sees, what it decodes to, and why it is flagged. */
data class Lookalike(val host: String, val shown: String, val reason: String)

object LookalikeCheck {
    /**
     * Returns the reason [url]'s host may be imitating another site, or null when it looks ordinary.
     * Flags a label in punycode (xn--) and a label mixing Latin letters with Cyrillic or Greek ones.
     */
    fun inspect(url: String): Lookalike? {
        val host = runCatching { URI(url).host }.getOrNull()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        if (!url.startsWith("http://") && !url.startsWith("https://")) return null
        val shown = runCatching { IDN.toUnicode(host) }.getOrDefault(host)
        for (label in host.split('.')) {
            if (label.startsWith("xn--")) {
                val decoded = runCatching { IDN.toUnicode(label) }.getOrDefault(label)
                return Lookalike(host, shown, "It uses encoded letters (\"$decoded\") that can look like ordinary ones.")
            }
        }
        for (label in shown.split('.')) {
            val scripts = label.filter { it.isLetter() }
                .map { Character.UnicodeScript.of(it.code) }
                .filter { it == Character.UnicodeScript.LATIN || it == Character.UnicodeScript.CYRILLIC || it == Character.UnicodeScript.GREEK }
                .toSet()
            if (scripts.size > 1) return Lookalike(host, shown, "It mixes letters from different alphabets, which can imitate a real site.")
        }
        return null
    }
}
