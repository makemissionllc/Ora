package com.makemission.ora.downloads

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.mozilla.geckoview.WebResponse
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

enum class DownloadStatus { RUNNING, DONE, FAILED, CANCELLED }

/** One file the browser is saving. Progress is in bytes; [totalBytes] is -1 when the server did not say. */
data class DownloadItem(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val totalBytes: Long,
    val bytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.RUNNING,
    val contentUri: Uri? = null,
    val error: String? = null,
) {
    /** 0..1 while running, -1 when the total size is unknown. */
    val progress: Float get() = if (totalBytes > 0) (bytes.toFloat() / totalBytes).coerceIn(0f, 1f) else -1f
}

/**
 * Saves files the pages hand to the browser (links to files, downloads started by scripts)
 * into the public Downloads folder, with progress, cancel, open and delete.
 * Logs every start, finish, failure and cancel through [BrowserLog].
 */
class DownloadsRepository(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = ConcurrentHashMap<Long, Job>()
    /** Ids the user cancelled; the copy loop checks this between reads. */
    private val cancelled: MutableSet<Long> = ConcurrentHashMap.newKeySet()
    private var nextId = 1L

    private val _items = MutableStateFlow<List<DownloadItem>>(emptyList())
    /** Newest first. */
    val items: StateFlow<List<DownloadItem>> = _items.asStateFlow()

    /** Starts saving a response that the page handed over as a file. */
    fun start(response: WebResponse, pageUrl: String) {
        val url = response.uri
        val mime = header(response, "content-type")?.substringBefore(';')?.trim()
        val name = fileNameFor(response)
        val total = header(response, "content-length")?.toLongOrNull() ?: -1L
        val item = DownloadItem(id = nextId++, fileName = name, url = url, mimeType = mime, totalBytes = total)
        update { listOf(item) + it }
        BrowserLog.info("download", "Started $name from $url (${sizeLabel(total)})")

        jobs[item.id] = scope.launch {
            try {
                save(item, response)
            } catch (e: CancelledDownload) {
                // cancel() already updated the row and logged it.
            } catch (e: IOException) {
                fail(item.id, e.message ?: "Read failed")
            } catch (e: Exception) {
                fail(item.id, e.message ?: e.javaClass.simpleName)
            } finally {
                jobs.remove(item.id)
                cancelled.remove(item.id)
            }
        }
    }

    private fun save(item: DownloadItem, response: WebResponse) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, item.fileName)
            put(MediaStore.Downloads.MIME_TYPE, item.mimeType ?: "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Ora")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Could not create file in Downloads")
        update { list -> list.map { if (it.id == item.id) it.copy(contentUri = uri) else it } }

        var written = 0L
        var lastReport = 0L
        val body = response.body ?: throw IOException("No response body")
        resolver.openOutputStream(uri)?.use { out ->
            body.use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    if (item.id in cancelled) throw CancelledDownload()
                    val read = input.read(buffer)
                    if (read < 0) break
                    out.write(buffer, 0, read)
                    written += read
                    // Throttle UI updates to about one per 256 KB.
                    if (written - lastReport >= 256 * 1024) {
                        lastReport = written
                        val w = written
                        update { list -> list.map { if (it.id == item.id) it.copy(bytes = w) else it } }
                    }
                }
            }
        } ?: throw IOException("Could not open file for writing")

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        update { list ->
            list.map { if (it.id == item.id) it.copy(status = DownloadStatus.DONE, bytes = written, contentUri = uri) else it }
        }
        BrowserLog.info("download", "Saved ${item.fileName} (${sizeLabel(written)}) to Downloads/Ora")
    }

    /** Stops a running download and removes its partial file. */
    fun cancel(id: Long) {
        if (jobs[id] == null) return
        cancelled.add(id)
        val item = _items.value.firstOrNull { it.id == id } ?: return
        item.contentUri?.let { context.contentResolver.delete(it, null, null) }
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.CANCELLED) else it } }
        BrowserLog.warn("download", "Cancelled ${item.fileName}")
    }

    /** Deletes the saved file and removes the row. */
    fun remove(id: Long) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        item.contentUri?.let { context.contentResolver.delete(it, null, null) }
        update { list -> list.filterNot { it.id == id } }
        BrowserLog.info("download", "Deleted ${item.fileName}")
    }

    /** Removes finished, failed and cancelled rows. Files stay on disk. */
    fun clearFinished() {
        val before = _items.value.size
        update { list -> list.filter { it.status == DownloadStatus.RUNNING } }
        BrowserLog.info("download", "Cleared ${before - _items.value.size} finished downloads from the list")
    }

    private fun fail(id: Long, message: String) {
        val item = _items.value.firstOrNull { it.id == id } ?: return
        update { list -> list.map { if (it.id == id) it.copy(status = DownloadStatus.FAILED, error = message) else it } }
        BrowserLog.error("download", "Failed ${item.fileName}: $message")
    }

    private fun update(change: (List<DownloadItem>) -> List<DownloadItem>) {
        _items.value = change(_items.value)
    }

    private class CancelledDownload : IOException("cancelled")

    companion object {
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
            else -> String.format("%.1f MB", bytes / 1024.0 / 1024.0)
        }

        private fun header(response: WebResponse, name: String): String? =
            response.headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value
    }
}
