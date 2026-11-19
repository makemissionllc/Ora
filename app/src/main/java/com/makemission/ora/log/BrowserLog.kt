package com.makemission.ora.log

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
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
    /** One entry never passes this many characters, so a 5,000 character address cannot fill the log. */
    private const val MAX_LINE = 400
    private const val MAX_ENTRIES = 5000
    /** The file is trimmed to the newest [MAX_ENTRIES] lines once it passes this size, so about two days of use fit. */
    private const val MAX_FILE_BYTES = 1_000_000L
    private const val LOGCAT_TAG = "Ora"
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    /** Newest entry last. Capped at [MAX_ENTRIES] so a noisy page cannot grow memory without limit. */
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    fun info(tag: String, message: String) = add(LogLevel.INFO, tag, message, null)

    fun warn(tag: String, message: String, error: Throwable? = null) = add(LogLevel.WARN, tag, message, error)

    fun error(tag: String, message: String, error: Throwable? = null) = add(LogLevel.ERROR, tag, message, error)

    private var file: File? = null
    private val writer = Executors.newSingleThreadExecutor { Thread(it, "ora-log").apply { isDaemon = true } }

    /**
     * Keeps the log across restarts. Loads the saved lines (capped at [MAX_ENTRIES]) in front of anything logged
     * so far, and appends every new entry to a private file in the app's storage.
     */
    fun init(context: Context) {
        if (file != null) return
        val f = File(context.applicationContext.filesDir, "ora-log.txt")
        file = f
        val earlier = runCatching {
            if (f.exists()) f.readLines().mapNotNull { LogCodec.decode(it) }.takeLast(MAX_ENTRIES) else emptyList()
        }.getOrDefault(emptyList())
        _entries.update { (earlier + it).takeLast(MAX_ENTRIES) }
        // Lines logged before init were only in memory; write them now.
        val pending = _entries.value.filter { e -> earlier.none { it === e } }
        writer.execute { runCatching { f.appendText(pending.joinToString("") { LogCodec.encode(it) + "\n" }) } }
    }

    fun clear() {
        _entries.value = emptyList()
        val f = file ?: return
        writer.execute { runCatching { f.delete() } }
    }

    private fun persist(entry: LogEntry) {
        val f = file ?: return
        writer.execute {
            runCatching {
                f.appendText(LogCodec.encode(entry) + "\n")
                if (f.length() > MAX_FILE_BYTES) {
                    f.writeText(f.readLines().takeLast(MAX_ENTRIES).joinToString("") { it + "\n" })
                }
            }
        }
    }

    /** Plain-text version of the log, one line per entry, oldest first. Used by the Copy action. */
    fun asText(): String = _entries.value.joinToString("\n") { entry ->
        "${timeFormat.format(Date(entry.time))} ${entry.level.label} [${entry.tag}] ${entry.message}"
    }

    private fun add(level: LogLevel, tag: String, message: String, error: Throwable?) {
        // Addresses become domains before anything is shown, copied, written to disk or sent to logcat.
        val full = LogPrivacy.redact(if (error != null) "$message: ${error.javaClass.simpleName}: ${error.message.orEmpty()}" else message).let { if (it.length > MAX_LINE) it.take(MAX_LINE) + "…" else it }
        when (level) {
            LogLevel.INFO -> Log.i(LOGCAT_TAG, "[$tag] $full")
            LogLevel.WARN -> Log.w(LOGCAT_TAG, "[$tag] $full", error)
            LogLevel.ERROR -> Log.e(LOGCAT_TAG, "[$tag] $full", error)
        }
        val entry = LogEntry(System.currentTimeMillis(), level, tag, full)
        _entries.update { (it + entry).takeLast(MAX_ENTRIES) }
        persist(entry)
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
