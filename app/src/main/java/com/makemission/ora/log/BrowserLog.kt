package com.makemission.ora.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class LogLevel(val label: String) { INFO("INFO"), WARN("WARN"), ERROR("ERROR") }

data class LogEntry(
    val time: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
)

/**
 * In-app log of browser events and errors. Every entry is also written to logcat.
 * The Diagnostics page in Settings reads [entries]; copy and clear live there too.
 */
object BrowserLog {
    private const val MAX_ENTRIES = 500
    private const val LOGCAT_TAG = "Ora"
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    /** Newest entry last. Capped at [MAX_ENTRIES] so a noisy page cannot grow memory without limit. */
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun info(tag: String, message: String) = add(LogLevel.INFO, tag, message, null)

    fun warn(tag: String, message: String, error: Throwable? = null) = add(LogLevel.WARN, tag, message, error)

    fun error(tag: String, message: String, error: Throwable? = null) = add(LogLevel.ERROR, tag, message, error)

    fun clear() {
        _entries.value = emptyList()
    }

    /** Plain-text version of the log, one line per entry, oldest first. Used by the Copy action. */
    fun asText(): String = _entries.value.joinToString("\n") { entry ->
        "${timeFormat.format(Date(entry.time))} ${entry.level.label} [${entry.tag}] ${entry.message}"
    }

    private fun add(level: LogLevel, tag: String, message: String, error: Throwable?) {
        val full = if (error != null) "$message: ${error.javaClass.simpleName}: ${error.message.orEmpty()}" else message
        when (level) {
            LogLevel.INFO -> Log.i(LOGCAT_TAG, "[$tag] $full")
            LogLevel.WARN -> Log.w(LOGCAT_TAG, "[$tag] $full", error)
            LogLevel.ERROR -> Log.e(LOGCAT_TAG, "[$tag] $full", error)
        }
        val entry = LogEntry(System.currentTimeMillis(), level, tag, full)
        _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
    }

    /**
     * Records uncaught crashes before the process dies, then hands them to the previous handler
     * so the system crash dialog and logcat still behave as usual.
     */
    fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            error("crash", "Uncaught exception on ${thread.name}", throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }
}
