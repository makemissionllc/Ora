package com.makemission.ora.log

/**
 * Keeps the log free of what was browsed: any address in a message is cut down to its domain
 * ("https://www.google.com/search?q=cats" becomes "google.com"). Search terms are never logged at all.
 */
object LogPrivacy {
    private val address = Regex("""\b[A-Za-z][A-Za-z0-9+.\-]*://[^\s"'<>)\]]*""")
    private val dataUri = Regex("""\bdata:[^\s"'<>)\]]*""")

    fun redact(message: String): String =
        message.replace(dataUri, "data:").replace(address) {
            // Punctuation that ends a sentence is not part of the address.
            val trimmed = it.value.trimEnd(':', '.', ',', ';')
            siteLabel(domainOf(trimmed)) + it.value.substring(trimmed.length)
        }

    /** Random per run, so a code cannot be reversed by hashing a list of well-known sites. */
    private val salt = java.security.SecureRandom().let { r -> ByteArray(16).also { r.nextBytes(it) } }

    /** Lab "Hash sites in the log": a short code for a site. Local schemes such as "ora:" stay readable. */
    fun siteLabel(domain: String): String {
        if (domain.endsWith(":") || !com.makemission.ora.labs.Labs.on(com.makemission.ora.labs.LabFeature.HASH_LOG_SITES)) return domain
        val digest = java.security.MessageDigest.getInstance("SHA-256").apply { update(salt) }.digest(domain.toByteArray())
        return "site-" + digest.take(4).joinToString("") { "%02x".format(it) }
    }

    /** The domain of [url] without "www.", port, user info, path, query or fragment. Local schemes keep only their name. */
    fun domainOf(url: String): String {
        val scheme = url.substringBefore("://", "")
        if (scheme == "ora" || scheme == "about" || scheme == "file" || scheme == "content") return "$scheme:"
        var host = url.substringAfter("://", url).substringBefore('/').substringBefore('?').substringBefore('#')
        host = host.substringAfterLast('@').substringBefore(':')
        return host.removePrefix("www.").ifEmpty { "unknown" }
    }
}

/** One log line as stored on disk: tab-separated, newlines and tabs in the message escaped. */
object LogCodec {
    fun encode(e: LogEntry): String =
        "${e.time}\t${e.level.name}\t${e.tag}\t${e.message.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t")}"

    fun decode(line: String): LogEntry? {
        val parts = line.split('\t', limit = 4)
        if (parts.size != 4) return null
        val time = parts[0].toLongOrNull() ?: return null
        val level = LogLevel.entries.firstOrNull { it.name == parts[1] } ?: return null
        val msg = buildString {
            var i = 0
            val m = parts[3]
            while (i < m.length) {
                if (m[i] == '\\' && i + 1 < m.length) {
                    append(when (m[i + 1]) { 'n' -> '\n'; 't' -> '\t'; else -> m[i + 1] }); i += 2
                } else { append(m[i]); i++ }
            }
        }
        return LogEntry(time, level, parts[2], msg)
    }
}
