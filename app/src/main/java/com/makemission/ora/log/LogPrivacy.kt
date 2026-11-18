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
            domainOf(trimmed) + it.value.substring(trimmed.length)
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
