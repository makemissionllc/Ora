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
import kotlinx.coroutines.currentCoroutineContext
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
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * One download log line, naming the site the file came from and never the file name: a file name can say what
 * was downloaded. [suffix] carries the size or the reason, with its own leading space or colon.
 */
internal fun downloadLogLine(event: String, url: String?, suffix: String = ""): String =
    "$event a download from ${LogPrivacy.siteLabel(LogPrivacy.domainOf(url.orEmpty()))}$suffix"

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
    /** True when Android paused this download because it stopped the background job. It starts again when Android restarts the job. */
    val pausedBySystem: Boolean = false,
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
    /** Read each time a download wants to start: how many may transfer at once. Zero or less means no limit. */
    private val maxSimultaneous: () -> Int = { 0 },
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dir = File(context.filesDir, "downloads").apply { mkdirs() }
    /** Downloads started in a private tab. They are never written to Room. */
    private val privateIds: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    /** The page each download started from, sent as the Referer. Kept for this run only. */
    private val referrers = ConcurrentHashMap<Long, String>()
    /** Pages' own streams for blob: and data: files, which the network cannot fetch. Each is read once, then dropped. */
    private val streams = ConcurrentHashMap<Long, WebResponse>()
    private val store = DownloadStore(context, File(dir, "queue.json"))
    private val jobs = ConcurrentHashMap<Long, Job>()
    /** The parts and validator for each unfinished download. Kept on failure so a retry resumes. */
    private val tasks = ConcurrentHashMap<Long, Task>()
    /** Ids the user paused; their job stops and is marked paused instead of cancelled. */
    private val pausing: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    private val lock = Any()
    /** Downloads waiting for a free slot, oldest first. They show as paused with [QUEUED_NOTICE] and start by themselves. */
    private val queued = java.util.LinkedHashSet<Long>()
    /** Downloads the user paused on purpose. Everything else that was unfinished continues by itself after a restart. */
    private val stateStore = context.getSharedPreferences("ora_downloads_state", Context.MODE_PRIVATE)
    private val userPaused: MutableSet<Long> = ConcurrentHashMap.newKeySet<Long>().also { set ->
        stateStore.getStringSet(KEY_USER_PAUSED, emptySet())?.mapNotNullTo(set) { it.toLongOrNull() }
    }
    /** Automatic retries used by each download since it last received a byte. */
    private val autoRetries = ConcurrentHashMap<Long, Int>()
    private var nextId = 1L
    /** Whether the background service was last asked to run, so it is started and stopped only on change. */
    @Volatile private var serviceRunning = false
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
            s.referrer?.let { referrers[s.id] = it }
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
                // Only a download the user paused on purpose waits for a tap; the rest carry on after a restart.
                pausedBySystem = s.ranged && s.id !in userPaused,
            )
        }
        _items.value = restored
        // Partial files with no saved row are left from a private download (never saved) or an earlier crash.
        val keep = saved.map { it.id.toString() }.toSet()
        dir.listFiles { f -> f.name.endsWith(".part") && f.name.removeSuffix(".part") !in keep }?.forEach { it.delete() }
        current = this
        DownloadNotifier(context, this, scope)
        if (restored.isNotEmpty()) BrowserLog.info("download", "Restored ${restored.size} unfinished downloads, ${restored.count { it.pausedBySystem }} will continue by themselves")
        resumeAfterSystemStop()
    }

    /** True for a download started in a private tab. */
    fun isPrivate(id: Long): Boolean = id in privateIds

    /** Starts saving a response that the page handed over as a file. The page's own stream is not used. */
    fun start(response: WebResponse, pageUrl: String, isPrivate: Boolean = false) {
        val url = response.uri
        val mime = header(response, "content-type")?.substringBefore(';')?.trim()
        val name = fileNameFor(response)
        val total = header(response, "content-length")?.toLongOrNull() ?: -1L
        if (isPageOwned(url)) {
            // A blob: or data: address only exists inside the page, so the app cannot request it again.
            // The stream Gecko handed over is saved as it is.
            begin(url, name, mime, total, pageUrl, isPrivate, response)
            return
        }
        runCatching { response.body?.close() }
        begin(url, name, mime, total, pageUrl, isPrivate)
    }

    /** Queues a file and starts it. [start] calls this; the tests call it directly, since they have no WebResponse. */
    internal fun begin(url: String, fileName: String, mime: String?, total: Long, pageUrl: String, isPrivate: Boolean, pageStream: WebResponse? = null) {
        val item = DownloadItem(id = nextId++, fileName = fileName, url = url, mimeType = mime, totalBytes = total)
        pageStream?.let { streams[item.id] = it }
        if (isPrivate) privateIds.add(item.id)
        referrers[item.id] = pageUrl
        update { listOf(item) + it }
        BrowserLog.info("download", downloadLogLine("Started", url, " (${sizeLabel(total)})"))
        _started.tryEmit(item)
        launch(item.id)
    }

    /** Downloads running right now. The background service stays up while this is above zero. */
    fun activeCount(): Int = _items.value.count { it.status == DownloadStatus.RUNNING }

    /** Pauses every running download that can resume. Called from the notification's Pause action. */
    fun pauseAll() {
        // A download Android paused is no longer waiting for its restart, so the user's pause covers it too.
        update { list -> list.map { if (it.pausedBySystem) it.copy(pausedBySystem = false) else it } }
        _items.value.filter { it.status == DownloadStatus.RUNNING && it.pausable }.forEach { pause(it.id) }
    }

    /** Cancels every running download. Called from the notification's Cancel action. */
    fun cancelAll() {
        _items.value.filter { it.status == DownloadStatus.RUNNING }.forEach { cancel(it.id) }
    }

    /** Pauses a running download. Its saved parts stay on disk, so Resume continues from the same bytes. */
    fun pause(id: Long) {
        // A pause the user asked for is remembered, so it survives a restart and nothing resumes it behind their back.
        if (stopTransfer(id)) rememberUserPause(id, true)
    }

    private fun rememberUserPause(id: Long, paused: Boolean) {
        if (paused) userPaused.add(id) else userPaused.remove(id)
        stateStore.edit().putStringSet(KEY_USER_PAUSED, userPaused.map { it.toString() }.toSet()).apply()
    }

    /**
     * Pauses the running downloads because Android stopped the background job. Each one is flagged as paused by
     * Android, so [resumeAfterSystemStop] starts it again when Android restarts the job.
     */
    fun pauseForSystemStop() {
        _items.value.filter { it.status == DownloadStatus.RUNNING && it.pausable }.forEach { item ->
            // Flagged before the transfer stops, so the job stays scheduled for its restart.
            flagSystemPause(item.id, true)
            if (!stopTransfer(item.id)) flagSystemPause(item.id, false)
        }
    }

    /** Starts again the downloads Android paused. Called when Android restarts the job, so nothing waits for a tap. */
    fun resumeAfterSystemStop() {
        val ids = _items.value.filter { it.pausedBySystem && it.status == DownloadStatus.PAUSED }.map { it.id }
        if (ids.isEmpty()) return
        BrowserLog.info("download", "${ids.size} download(s) that stopped on their own continue")
        ids.forEach { resume(it) }
    }

    /** Continues a paused or failed download from where it stopped. */
    fun resume(id: Long) {
        val item = find(id) ?: return
        if (item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return
        BrowserLog.info("download", downloadLogLine("Resumed", item.url))
        launch(id)
    }

    /** Stops the transfer of a running download. False when there was nothing to stop. */
    private fun stopTransfer(id: Long): Boolean {
        val job = jobs[id]?.takeIf { it.isActive } ?: return false
        if (find(id)?.pausable != true) return false
        pausing.add(id)
        waitingForNetwork.remove(id)
        job.cancel()
        return true
    }

    private fun flagSystemPause(id: Long, flagged: Boolean) =
        update { list -> list.map { if (it.id == id) it.copy(pausedBySystem = flagged) else it } }

    /** Stops a download and removes its partial file. */
    fun cancel(id: Long) {
        val item = find(id) ?: return
        if (item.status != DownloadStatus.RUNNING && item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return
        jobs[id]?.cancel()
        synchronized(queued) { queued.remove(id) }
        partFile(id).delete()
        tasks.remove(id)
        autoRetries.remove(id)
        rememberUserPause(id, false)
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.CANCELLED, speed = 0, pausedBySystem = false) else it } }
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

    /** Starts waiting downloads while there are free slots. Called when a transfer ends and when the limit changes. */
    fun startQueued() {
        while (true) {
            val next = synchronized(queued) {
                val limit = maxSimultaneous()
                if (limit > 0 && jobs.count { it.value.isActive } >= limit) return
                queued.firstOrNull()?.also { queued.remove(it) }
            } ?: return
            if (find(next)?.status == DownloadStatus.PAUSED) {
                BrowserLog.info("download", downloadLogLine("Left the queue", find(next)?.url))
                launch(next)
            }
        }
    }

    /** True when every slot is taken, so this download should wait its turn. */
    private fun mustWait(id: Long): Boolean {
        val limit = maxSimultaneous()
        return limit > 0 && jobs.count { it.key != id && it.value.isActive } >= limit
    }

    private fun launch(id: Long) {
        if (jobs[id]?.isActive == true) return
        if (mustWait(id)) {
            val item = find(id) ?: return
            synchronized(queued) { queued.add(id) }
            update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.PAUSED, speed = 0, error = null, notice = QUEUED_NOTICE) else it } }
            BrowserLog.info("download", downloadLogLine("Queued", item.url, " (${queued.size} waiting)"))
            return
        }
        synchronized(queued) { queued.remove(id) }
        rememberUserPause(id, false)
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.RUNNING, error = null, notice = it.notice.takeUnless { n -> n == AUTO_RETRY_NOTICE || n == QUEUED_NOTICE }, pausedBySystem = false) else it } }
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
                // A file that only the page can read says so, instead of offering a Retry that cannot work.
                fail(id, e.message ?: e.javaClass.simpleName, if (e is IllegalStateException) e.message ?: FAILED_TAP_TO_RETRY else FAILED_TAP_TO_RETRY)
                if (e is IOException) scheduleAutoRetry(id, e.message)
            } finally {
                // Removes only this run's job, so a resume that started a newer run is not dropped from the map.
                currentCoroutineContext()[Job]?.let { jobs.remove(id, it) }
                startQueued()
            }
        }
    }

    private suspend fun run(id: Long, restarted: Boolean) {
        val item = find(id) ?: return
        val pageStream = streams.remove(id)
        if (pageStream == null && isPageOwned(item.url)) {
            // The page's stream was used up (a retry, or the app restarted). The address cannot be fetched again.
            throw IllegalStateException("This file only exists in the page it came from. Open the page and save it again.")
        }
        val engine = DownloadEngine(if (pageStream != null) PageStreamTransport(pageStream) else transportFor(id in privateIds, referrers[id]))
        val part = partFile(id)
        var task = tasks[id]
        // Plain http without the setting: one connection, no probe, no range requests. A page's own stream is the same.
        val plainHttp = pageStream != null || (item.url.startsWith("http://", ignoreCase = true) && !fastHttp())
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
        if (bytes > task.lastBytes) autoRetries.remove(id)
        task.lastBytes = bytes
        task.lastTime = now
        update { list -> list.map { if (it.id == id) it.copy(bytes = bytes, speed = speed) else it } }
        if (now - lastSave > 2000) saveNow()
    }

    private fun fail(id: Long, message: String, shown: String = FAILED_TAP_TO_RETRY) {
        val url = find(id)?.url ?: return
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.FAILED, error = shown, speed = 0) else it } }
        saveNow()
        BrowserLog.error("download", downloadLogLine("Failed", url, ": $message"))
    }

    /**
     * A download that ran out of quick retries is tried again every few minutes instead of waiting for a tap,
     * so a long one survives a Wi-Fi sleep or a server that was busy. It gives up after [MAX_AUTO_RETRIES]
     * tries in a row with no new bytes, or at once for an error that will not go away (an expired link).
     */
    private fun scheduleAutoRetry(id: Long, message: String?) {
        if (!isTemporaryFailure(message)) return
        val tries = (autoRetries[id] ?: 0) + 1
        if (tries > MAX_AUTO_RETRIES) {
            BrowserLog.warn("download", downloadLogLine("Gave up retrying by itself", find(id)?.url, " after $MAX_AUTO_RETRIES tries"))
            return
        }
        autoRetries[id] = tries
        update { list -> list.map { if (it.id == id) it.copy(notice = AUTO_RETRY_NOTICE) else it } }
        scope.launch {
            kotlinx.coroutines.delay(AUTO_RETRY_MS)
            val item = find(id) ?: return@launch
            if (item.status == DownloadStatus.FAILED && id !in userPaused) {
                BrowserLog.info("download", downloadLogLine("Retrying by itself", item.url, ": try $tries of $MAX_AUTO_RETRIES"))
                waitingForNetwork.remove(id)
                resume(id)
            }
        }
    }

    private fun partFile(id: Long) = File(dir, "$id.part")

    private fun find(id: Long): DownloadItem? = _items.value.firstOrNull { it.id == id }

    /** Writes the unfinished downloads to the queue file, so they come back after a restart. */
    private fun saveNow() = synchronized(lock) {
        lastSave = System.currentTimeMillis()
        val saved = _items.value.mapNotNull { item ->
            if (item.status != DownloadStatus.RUNNING && item.status != DownloadStatus.PAUSED && item.status != DownloadStatus.FAILED) return@mapNotNull null
            // Private downloads never reach the queue file, so they cannot come back after a restart.
            if (item.id in privateIds) return@mapNotNull null
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
                referrer = referrers[item.id],
            )
        }
        store.save(saved)
    }

    private fun update(change: (List<DownloadItem>) -> List<DownloadItem>) = synchronized(lock) {
        _items.value = change(_items.value)
        syncService()
    }

    /**
     * Keeps downloads alive in the background while one is active: a user-initiated job on Android 14 and later,
     * a data-sync foreground service before that. Started and stopped only when the state changes.
     */
    private fun syncService() {
        // A download Android paused still needs the job, so the job stays scheduled until Android restarts it.
        val running = activeCount() > 0 || _items.value.any { it.pausedBySystem }
        if (running == serviceRunning) return
        serviceRunning = running
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) syncJob(running) else syncForegroundService(running)
        }.onFailure { BrowserLog.warn("download", "Could not ${if (running) "start" else "stop"} background downloads: ${it.message}") }
    }

    @RequiresApi(34)
    private fun syncJob(running: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!running) {
            scheduler.cancel(DownloadJobService.JOB_ID)
            return
        }
        val job = JobInfo.Builder(DownloadJobService.JOB_ID, ComponentName(context, DownloadJobService::class.java))
            .setUserInitiated(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setEstimatedNetworkBytes(JobInfo.NETWORK_BYTES_UNKNOWN.toLong(), JobInfo.NETWORK_BYTES_UNKNOWN.toLong())
            // When Android stops the job, it starts it again after this short wait, not after a long back-off.
            .setBackoffCriteria(RESTART_BACKOFF_MILLIS, JobInfo.BACKOFF_POLICY_LINEAR)
            .build()
        val result = scheduler.schedule(job)
        if (result != JobScheduler.RESULT_SUCCESS) {
            BrowserLog.warn("download", "The system did not start the background download job (result $result); downloads run only while Ora is open")
        }
    }

    private fun syncForegroundService(running: Boolean) {
        val intent = Intent(context, DownloadService::class.java)
        if (running) context.startForegroundService(intent) else context.stopService(intent)
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
        const val QUEUED_NOTICE = "Waiting for a free slot"
        const val FAILED_TAP_TO_RETRY = "Failed: tap to retry"
        const val AUTO_RETRY_NOTICE = "Connection lost. Trying again soon"
        private const val KEY_USER_PAUSED = "user_paused"
        private const val AUTO_RETRY_MS = 2 * 60 * 1000L
        const val MAX_AUTO_RETRIES = 30

        /** False for an error that will not fix itself: a missing or forbidden file, an expired link. */
        internal fun isTemporaryFailure(message: String?): Boolean {
            val code = message?.substringAfter("HTTP ", "")?.takeWhile { it.isDigit() }?.toIntOrNull() ?: return true
            return code == 408 || code == 429 || code >= 500
        }

        /** The repository, made now when this process does not have one yet (the system restarted a download job). */
        @Synchronized
        fun ensure(context: Context): DownloadsRepository {
            current?.let { return it }
            val app = context.applicationContext
            val settings = com.makemission.ora.settings.SettingsRepository(app)
            return DownloadsRepository(
                app,
                fastHttp = { settings.fastHttpDownloads.value },
                maxSimultaneous = { com.makemission.ora.settings.SettingsRepository.maxSimultaneousDownloads(app) },
                transportFor = { priv, referrer -> GeckoTransport(com.makemission.ora.browser.GeckoEngine.runtime(app), priv, referrer) },
            )
        }
        /** The shortest wait the system allows before it starts a stopped download job again. */
        private const val RESTART_BACKOFF_MILLIS = 10_000L

        /** The repository the service's notification buttons act on. Set when a repository is created. */
        @Volatile var current: DownloadsRepository? = null

        /** Bytes of a part that are on disk, never more than its size. */
        private fun doneOf(part: Part): Long = doneOf(part.seg)

        private fun doneOf(s: Segment): Long = if (s.end >= 0) minOf(s.done, s.size) else s.done

        /** True for blob: and data: addresses, which only the page that made them can read. */
        internal fun isPageOwned(url: String): Boolean =
            url.startsWith("blob:", ignoreCase = true) || url.startsWith("data:", ignoreCase = true)

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
