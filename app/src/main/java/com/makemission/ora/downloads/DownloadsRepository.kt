package com.makemission.ora.downloads

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.WebResponse
import java.io.File
import java.io.IOException
import android.net.ConnectivityManager
import android.net.Network
import java.io.RandomAccessFile
import java.util.concurrent.ConcurrentHashMap

/**
 * One download log line, naming the site the file came from and never the file name: a file name can say what
 * was downloaded. [suffix] carries the size or the reason, with its own leading space or colon.
 */
internal fun downloadLogLine(event: String, url: String?, suffix: String = ""): String =
    "$event a download from ${LogPrivacy.domainOf(url.orEmpty())}$suffix"

enum class DownloadStatus { RUNNING, PAUSED, DONE, FAILED, CANCELLED }

/** One file the browser is saving. Progress is in bytes; [totalBytes] is -1 when the server did not say. */
data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val totalBytes: Long,
    val bytes: Long = 0,
    /** Bytes per second over the last reporting window; 0 when not running or not measured yet. */
    val speed: Long = 0,
    val status: DownloadStatus = DownloadStatus.RUNNING,
    val contentUri: Uri? = null,
    val error: String? = null,
    /** A plain note for the row, such as "The file changed, so it restarted". */
    val notice: String? = null,
    /** False when the server cannot resume a part, so the download can only be cancelled. */
    val pausable: Boolean = true,
    /** When the save began, in epoch milliseconds. Shown as "Started" on the details page. */
    val startedAt: Long = System.currentTimeMillis(),
) {
    /** 0..1 while running, -1 when the total size is unknown. */
    val progress: Float get() = if (totalBytes > 0) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else -1f
}

/**
 * Saves files the pages hand to the browser into the public Downloads folder. Each file is fetched in
 * parallel parts when the server allows range requests, so it is quicker, and it can be paused, resumed
 * after a restart, and retried without starting over. Unfinished downloads are kept in app storage.
 * Logs every start, pause, resume, finish, failure and cancel through [BrowserLog].
 */
class DownloadsRepository(
    private val context: Context,
    /** Read when a download starts or resumes: true lets plain http:// files use the parts engine. */
    private val fastHttp: () -> Boolean = { false },
    /** Makes the transport for one download. The app uses GeckoWebExecutor; tests use the default plain connection. */
    private val transportFor: (isPrivate: Boolean, referrer: String?) -> DownloadTransport = { _, _ -> HttpUrlTransport() },
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dir = File(context.filesDir, "downloads").apply { mkdirs() }
    /** Downloads started in a private tab. They are never written to Room. */
    private val privateIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    /** The page each download started from, sent as the Referer. Kept for this run only. */
    private val referrers = ConcurrentHashMap<Long, String>()
    private val store = DownloadStore(context, File(dir, "queue.json"))
    private val jobs = ConcurrentHashMap<Long, Job>()
    /** The parts and validator for each unfinished download. Kept on failure so a retry resumes. */
    private val tasks = ConcurrentHashMap<Long, Task>()
    /** Ids the user paused; their job stops and is marked paused instead of cancelled. */
    private val pausing: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    private val lock = Any()
    private var nextId = 1L
    @Volatile private var lastSave = 0L

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    /** Newest first. */
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    private val _started = MutableSharedFlow<DownloadItem>(extraBufferCapacity = 8)
    /** Fires once for each download that begins, so the start notice can show its name. */
    val started: SharedFlow<DownloadItem> = _started.asSharedFlow()

    /** Downloads that failed on a network error. They resume when a network comes back. */
    private val waitingForNetwork: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val ids = waitingForNetwork.toList()
            if (ids.isEmpty()) return
            BrowserLog.info("download", "Network is back, resuming ${ids.size} download(s)")
            ids.forEach { waitingForNetwork.remove(it); resume(it) }
        }
    }

    init {
        runCatching { context.getSystemService(ConnectivityManager::class.java)?.registerDefaultNetworkCallback(networkCallback) }
        // Downloads left unfinished by an earlier run come back paused, so the user decides when to continue.
        val saved = store.load()
        nextId = (saved.maxOfOrNull { it.id } ?: 0L) + 1
        val restored = saved.map { s ->
            tasks[s.id] = Task(s.url, s.etag, s.ranged, s.totalBytes, s.parts.mapIndexed { i, seg -> Part(i, seg) }.toMutableList())
            DownloadItem(
                id = s.id,
                fileName = s.fileName,
                url = s.url,
                mimeType = s.mimeType,
                totalBytes = s.totalBytes,
                bytes = s.parts.sumOf { doneOf(it) },
                status = DownloadStatus.PAUSED,
                startedAt = s.startedAt,
                pausable = s.ranged,
            )
        }
        _items.value = restored
        if (restored.isNotEmpty()) BrowserLog.info("download", "Restored ${restored.size} unfinished downloads as paused")
    }

    /** Starts saving a response that the page handed over as a file. The page's own stream is not used. */
    fun start(response: WebResponse, pageUrl: String, isPrivate: Boolean = false) {
        val url = response.uri
        val mime = header(response, "content-type")?.substringBefore(';')?.trim()
        val name = fileNameFor(response)
        val total = header(response, "content-length")?.toLongOrNull() ?: -1L
        runCatching { response.body?.close() }
        val item = DownloadItem(id = nextId++, fileName = name, url = url, mimeType = mime, totalBytes = total)
        if (isPrivate) privateIds.add(item.id)
        referrers[item.id] = pageUrl
        update { listOf(item) + it }
        BrowserLog.info("download", downloadLogLine("Started", url, " (${sizeLabel(total)})"))
        _started.tryEmit(item)
        launch(item.id)
    }

    /** Pauses a running download. Its saved parts stay on disk, so Resume continues from the same bytes. */
    fun pause(id: Long) {
        val job = jobs[id]?.takeIf { it.isActive } ?: return
        if (find(id)?.pausable != true) return
        pausing.add(id)
        waitingForNetwork.remove(id)
        job.cancel()
    }

    /** Continues a paused or failed download from where it stopped. */
    fun resume(id: Long) {
        val item = find(id) ?: return
        if (item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return
        BrowserLog.info("download", downloadLogLine("Resumed", item.url))
        launch(id)
    }

    /** Stops a download and removes its partial file. */
    fun cancel(id: Long) {
        val item = find(id) ?: return
        if (item.status != DownloadStatus.RUNNING && item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return
        jobs[id]?.cancel()
        partFile(id).delete()
        tasks.remove(id)
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.CANCELLED, speed = 0) else it } }
        saveNow()
        BrowserLog.warn("download", downloadLogLine("Cancelled", item.url))
    }

    /** Deletes the saved file and removes the row. An unfinished download is cancelled first. */
    fun remove(id: Long) {
        val item = find(id) ?: return
        if (item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.PAUSED) cancel(id)
        item.contentUri?.let { context.contentResolver.delete(it, null, null) }
        partFile(id).delete()
        tasks.remove(id)
        update { list -> list.filterNot { it.id == id } }
        saveNow()
        BrowserLog.info("download", downloadLogLine("Deleted", item.url))
    }

    /** Removes finished, failed and cancelled rows. Files stay on disk. */
    fun clearFinished() {
        val before = _items.value.size
        val gone = _items.value.filter { it.status == DownloadStatus.DONE || it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED }
        gone.forEach { partFile(it.id).delete(); tasks.remove(it.id) }
        update { list -> list.filterNot { it in gone } }
        saveNow()
        BrowserLog.info("download", "Cleared ${before - _items.value.size} finished downloads from the list")
    }

    private fun launch(id: Long) {
        if (jobs[id]?.isActive == true) return
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.RUNNING, error = null) else it } }
        jobs[id] = scope.launch {
            try {
                run(id, restarted = false)
            } catch (e: CancellationException) {
                if (id in pausing) {
                    pausing.remove(id)
                    update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speed = 0) else it } }
                    saveNow()
                    BrowserLog.info("download", downloadLogLine("Paused", find(id)?.url))
                }
                throw e
            } catch (e: FileChangedException) {
                // The saved parts no longer match the server's file, so the next Retry starts again.
                tasks.remove(id)
                partFile(id).delete()
                fail(id, e.message ?: "The file changed on the server")
            } catch (e: Exception) {
                if (e is IOException) waitingForNetwork.add(id)
                fail(id, e.message ?: e.javaClass.simpleName)
            } finally {
                jobs.remove(id)
            }
        }
    }

    private suspend fun run(id: Long, restarted: Boolean) {
        val item = find(id) ?: return
        val engine = DownloadEngine(transportFor(id in privateIds, referrers[id]))
        val part = partFile(id)
        var task = tasks[id]
        // Plain http without the setting: one connection, no probe, no range requests.
        val plainHttp = item.url.startsWith("http://", ignoreCase = true) && !fastHttp()
        if (task != null && !task.ranged) {
            // A stream without ranges cannot continue from its bytes, so it starts over.
            task.parts.forEach { it.seg = it.seg.copy(done = 0, finished = false) }
        }
        if (task == null || task.parts.isEmpty() || !part.exists()) {
            val probe = if (plainHttp) Probe(item.totalBytes, null, false) else engine.probe(item.url)
            val total = if (probe.total > 0) probe.total else item.totalBytes
            task = Task(item.url, probe.etag, probe.ranges && total > 0, total, mutableListOf())
            RandomAccessFile(part, "rw").use { raf -> if (total > 0) raf.setLength(total) }
            task.parts += engine.plan(total, task.ranged).mapIndexed { i, s -> Part(i, s) }
            tasks[id] = task
            BrowserLog.info("download", downloadLogLine("Planned", item.url, ": ${task.parts.size} part(s), ${if (task.ranged) "range requests" else "one stream, cannot pause"}"))
        }
        val active = task
        update { list -> list.map { if (it.id == id) it.copy(totalBytes = active.total, status = DownloadStatus.RUNNING, pausable = active.ranged) else it } }
        saveNow()

        try {
            engine.download(active.url, active.etag, active.ranged, part, active.parts) { onProgress(id, active) }
        } catch (e: FileChangedException) {
            if (restarted) throw e
            // The server's file changed, so the saved parts cannot be joined. Start over and say so.
            BrowserLog.warn("download", downloadLogLine("File changed, restarting", item.url))
            tasks.remove(id)
            partFile(id).delete()
            update { list -> list.map { if (it.id == id) it.copy(notice = FILE_CHANGED_NOTICE, bytes = 0) else it } }
            saveNow()
            return run(id, restarted = true)
        }

        publish(id, item, part, active)
    }

    /** Copies the finished part file into Downloads/Ora and marks the row done. */
    private fun publish(id: Long, item: DownloadItem, part: File, task: Task) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, item.fileName)
            put(MediaStore.Downloads.MIME_TYPE, item.mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Ora")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create file in Downloads")
        try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("Could not open file for writing")
            out.use { output -> part.inputStream().use { it.copyTo(output, 64 * 1024) } }
        } catch (e: IOException) {
            resolver.delete(uri, null, null)
            throw e
        }
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        val size = part.length()
        part.delete()
        tasks.remove(id)
        update { list ->
            list.map { if (it.id == id) it.copy(status = DownloadStatus.DONE, bytes = size, speed = 0, contentUri = uri) else it }
        }
        saveNow()
        BrowserLog.info("download", downloadLogLine("Saved", item.url, " (${sizeLabel(size)}) to Downloads/Ora"))
        if (task.parts.size > 1) BrowserLog.info("download", downloadLogLine("Finished", item.url, " in ${task.parts.size} parts"))
    }

    /** Called by the parts as bytes arrive. Throttled, so the list redraws about four times a second. */
    private fun onProgress(id: Long, task: Task) {
        val now = System.currentTimeMillis()
        if (now - task.lastUi < 250) return
        task.lastUi = now
        val bytes = task.parts.sumOf { doneOf(it.seg) }
        val seconds = (now - task.lastTime) / 1000.0
        val speed = if (task.lastTime > 0 && seconds > 0) ((bytes - task.lastBytes) / seconds).toLong() else 0L
        task.lastBytes = bytes
        task.lastTime = now
        update { list -> list.map { if (it.id == id) it.copy(bytes = bytes, speed = speed) else it } }
        if (now - lastSave > 2000) saveNow()
    }

    private fun fail(id: Long, message: String) {
        val url = find(id)?.url ?: return
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.FAILED, error = FAILED_TAP_TO_RETRY, speed = 0) else it } }
        saveNow()
        BrowserLog.error("download", downloadLogLine("Failed", url, ": $message"))
    }

    private fun partFile(id: Long) = File(dir, "$id.part")

    private fun find(id: Long): DownloadItem? = _items.value.firstOrNull { it.id == id }

    /** Writes the unfinished downloads to the queue file, so they come back after a restart. */
    private fun saveNow() = synchronized(lock) {
        lastSave = System.currentTimeMillis()
        val saved = _items.value.mapNotNull { item ->
            if (item.status != DownloadStatus.RUNNING && item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return@mapNotNull null
            // A download that failed before its plan was made is saved with no parts, so it can be retried after a restart.
            val task = tasks[item.id]
            SavedDownload(
                id = item.id,
                fileName = item.fileName,
                url = item.url,
                mimeType = item.mimeType,
                totalBytes = task?.total ?: item.totalBytes,
                etag = task?.etag,
                ranged = task?.ranged ?: false,
                startedAt = item.startedAt,
                parts = task?.parts?.map { it.seg } ?: emptyList(),
            )
        }
        store.save(saved)
    }

    private fun update(change: (List<DownloadItem>) -> List<DownloadItem>) = synchronized(lock) {
        _items.value = change(_items.value)
    }

    /** The state one download needs to continue: where it comes from, how it was checked, and its parts. */
    private class Task(
        val url: String,
        val etag: String?,
        val ranged: Boolean,
        val total: Long,
        val parts: MutableList<Part>,
    ) {
        @Volatile var lastUi = 0L
        @Volatile var lastTime = 0L
        @Volatile var lastBytes = 0L
    }

    companion object {
        const val FILE_CHANGED_NOTICE = "The file changed, so it restarted"
        const val FAILED_TAP_TO_RETRY = "Failed: tap to retry"

        /** Bytes of a part that are on disk, never more than its size. */
        private fun doneOf(part: Part): Long = doneOf(part.seg)

        private fun doneOf(s: Segment): Long = if (s.end >= 0) minOf(s.done, s.size) else s.done

        /** Name from Content-Disposition, else the last URL segment, else a fallback. */
        fun fileNameFor(response: WebResponse): String {
            val disposition = header(response, "content-disposition")
            val fromHeader = disposition?.let {
                Regex("""filename\*?=(?:UTF-8'')?"?([^";]+)"?""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1)
            }
            val fromUrl = runCatching { Uri.parse(response.uri).lastPathSegment }.getOrNull()
            val raw = (fromHeader ?: fromUrl ?: "download").trim()
            return raw.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "download" }
        }

        fun sizeLabel(bytes: Long): String = when {
            bytes < 0 -> "unknown size"
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
            else -> String.format("%.1f GB", bytes / 1024.0 / 1024.0 / 1024.0)
        }

        private fun header(response: WebResponse, name: String): String? =
            response.headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }
}
