package com.makemission.ora.log

/**
 * Logs a search without what was searched for. The log only says which engine was used. A search from a
 * private tab is not logged at all, so private browsing leaves no trace in the log.
 */
object SearchLog {
    /** The line written for a search, or null when nothing may be written. The search text is never part of it. */
    fun line(engineLabel: String, privateTab: Boolean): String? =
        if (privateTab) null else "Searched with $engineLabel"

    fun record(engineLabel: String, privateTab: Boolean) {
        line(engineLabel, privateTab)?.let { BrowserLog.info("search", it) }
    }
}
