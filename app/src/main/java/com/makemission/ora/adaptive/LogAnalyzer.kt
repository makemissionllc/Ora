package com.makemission.ora.adaptive

import com.makemission.ora.log.LogEntry
import com.makemission.ora.log.LogLevel

/**
 * Reads Ora's own log and turns it into a short list of things worth knowing, each in plain words. Runs on the
 * device on what is already in the log; nothing is sent anywhere. Page addresses in the log are already short
 * codes when the "Hash sites in the log" lab is on, so this never needs to see more than the log shows.
 */
object LogAnalyzer {
    private val loaded = Regex("Loaded .* in (\\d+) ms")
    private val downloadHost = Regex("Failed a download from ([^:\\s]+)")

    /**
     * [preloadAlways] says whether look-ahead is on for every connection, which is the only case where its failures
     * can be cut by changing the setting. [pullScale] is what pulling needs now, [pullRecommended] what the learner suggests.
     */
    fun analyse(
        entries: List<LogEntry>,
        preloadAlways: Boolean,
        pullRecommended: Float?,
        pulls: PullLearner.Pulls,
    ): List<Insight> {
        val out = mutableListOf<Insight>()

        val crashes = entries.filter { it.level == LogLevel.ERROR && it.tag == "crash" }
        if (crashes.isNotEmpty()) {
            val last = crashes.last()
            // Only the kind of failure, not the whole message, and when it last happened.
            val kind = last.message.removePrefix("Uncaught exception on main: ").substringBefore(':').take(60)
            val whenText = java.text.SimpleDateFormat("d MMM HH:mm", java.util.Locale.getDefault()).format(java.util.Date(last.time))
            out += Insight("crash", "Ora stopped unexpectedly ${times(crashes.size)}", "The latest was a $kind on $whenText. If you have updated Ora since, it may already be fixed. The full report is in Log and errors.")
        }

        val times = entries.mapNotNull { loaded.find(it.message)?.groupValues?.get(1)?.toLongOrNull() }.filter { it > 50 }.sorted()
        if (times.size >= 15) {
            val p90 = times[(times.size * 0.9).toInt().coerceAtMost(times.size - 1)]
            if (p90 >= 4000) out += Insight("slow", "Some pages are slow to open", "One page in ten took over ${p90 / 1000} seconds to load. That is usually the connection or the site, not Ora.", problem = true)
        }

        val aheadFails = entries.count { it.level == LogLevel.WARN && it.tag == "preload" && it.message.contains("ahead of time") }
        if (aheadFails >= 5 && preloadAlways) {
            out += Insight("preload", "Looking ahead often fails", "Ora tried to look up a site early and could not reach it $aheadFails times. On Wi-Fi only it does this less often and spends no mobile data.", Fix.PRELOAD_WIFI)
        }

        if (pullRecommended != null) {
            val cut = ((1f - pullRecommended) * 100).toInt()
            out += Insight("pull", "Pull to refresh is hard to reach", "You let go before the refresh point ${pulls.early} times, against ${pulls.registered} pulls that worked. Ora can shorten the pull by $cut%.", Fix.SHORTER_PULL)
        }

        // Blocks from before the person allowed Local network no longer matter.
        val allowedAt = entries.indexOfLast { it.tag == "devices" && it.message.contains("Local network access allowed") }
        val blocked = entries.drop(allowedAt + 1).count { it.level == LogLevel.WARN && it.tag == "devices" && it.message.contains("Missing local network") }
        if (blocked >= 3) out += Insight("localnet", "Device linking was blocked", "Android did not let Ora use the local network $blocked times. Allow Local network for Ora in Android's app settings to link devices.")

        val suggestions = entries.count { it.level == LogLevel.WARN && it.tag == "search" && it.message.contains("Suggestions") }
        if (suggestions >= 3) out += Insight("suggest", "Search suggestions arrive slowly", "Suggestions timed out $suggestions times, which points to a weak connection.", problem = false)

        entries.filter { it.level == LogLevel.ERROR && it.tag == "download" }
            .mapNotNull { downloadHost.find(it.message)?.groupValues?.get(1) }
            .groupingBy { it }.eachCount().filter { it.value >= 3 }.entries.maxByOrNull { it.value }
            ?.let { out += Insight("download", "Downloads from ${it.key} keep failing", "${it.key} refused ${it.value} download attempts. Try opening the page again, or the site may need you to sign in.") }

        return out
    }

    private fun times(n: Int) = if (n == 1) "once" else "$n times"
}
