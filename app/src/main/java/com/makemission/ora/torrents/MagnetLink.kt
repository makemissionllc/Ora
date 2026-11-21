package com.makemission.ora.torrents

import java.net.URLDecoder

/** A parsed `magnet:` link: what the torrent is called (if the link says), its 40-letter info hash, and how many trackers it names. */
data class MagnetLink(val uri: String, val infoHash: String, val name: String?, val trackers: Int) {
    companion object {
        private const val B32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

        /** Reads a magnet link, or returns null when it has no valid `xt=urn:btih:` hash. The hash may be 40 hex letters or 32 base32 letters. */
        fun parse(text: String): MagnetLink? {
            val uri = text.trim()
            if (uri.length > 8000 || !uri.startsWith("magnet:?", ignoreCase = true)) return null
            var hash: String? = null
            var name: String? = null
            var trackers = 0
            for (pair in uri.substring(8).split('&')) {
                val eq = pair.indexOf('=')
                if (eq <= 0) continue
                val key = pair.substring(0, eq).lowercase()
                val value = runCatching { URLDecoder.decode(pair.substring(eq + 1), "UTF-8") }.getOrNull() ?: continue
                when (key) {
                    "xt" -> if (hash == null && value.startsWith("urn:btih:", ignoreCase = true)) hash = normaliseHash(value.substring(9))
                    "dn" -> if (name == null) name = value.take(200).ifBlank { null }
                    "tr" -> trackers++
                }
            }
            return hash?.let { MagnetLink(uri, it, name, trackers) }
        }

        /** 40 lower-case hex letters, from a hex or base32 hash; null when it is neither. */
        fun normaliseHash(raw: String): String? {
            val h = raw.trim()
            if (h.length == 40 && h.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return h.lowercase()
            if (h.length == 32) {
                var bits = 0L
                var count = 0
                val out = StringBuilder()
                for (c in h.uppercase()) {
                    val v = B32.indexOf(c)
                    if (v < 0) return null
                    bits = (bits shl 5) or v.toLong()
                    count += 5
                    if (count >= 8) {
                        count -= 8
                        out.append("%02x".format(((bits shr count) and 0xff).toInt()))
                    }
                }
                return out.toString().takeIf { it.length == 40 }
            }
            return null
        }
    }
}

/** Plain-words sizes, speeds and times for the torrent list. */
object TorrentFormat {
    fun bytes(n: Long): String {
        if (n < 1024) return "$n B"
        val units = listOf("KB", "MB", "GB", "TB")
        var v = n / 1024.0
        var i = 0
        while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
        return (if (v >= 100) "%.0f" else "%.1f").format(v) + " " + units[i]
    }

    fun rate(bytesPerSecond: Int): String = bytes(bytesPerSecond.toLong()) + "/s"

    /** "5 min left", "1 h 20 min left", or null when the speed is too low to say. */
    fun eta(remaining: Long, bytesPerSecond: Int): String? {
        if (remaining <= 0 || bytesPerSecond < 1024) return null
        val s = remaining / bytesPerSecond
        return when {
            s < 60 -> "$s s left"
            s < 3600 -> "${s / 60} min left"
            s < 86400 -> "${s / 3600} h ${(s % 3600) / 60} min left"
            else -> "more than a day left"
        }
    }
}
