package com.makemission.ora.downloads

import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URL

/** A byte range of the file: [start] to [end] inclusive. [end] is -1 when the size is unknown. */
data class Segment(val start: Long, val end: Long, val done: Long = 0, val finished: Boolean = false) {
    val size: Long get() = if (end >= 0) end - start + 1 else -1
    val complete: Boolean get() = finished || (end >= 0 && start + done > end)
}

/** One part of a download being fetched. [seg] changes as bytes arrive, so a retry resumes from the last byte saved. */
class Part(val index: Int, var seg: Segment)

/** What the server said before any bytes were saved. */
data class Probe(val total: Long, val etag: String?, val ranges: Boolean)

/** The file on the server changed since the download began, so the saved parts cannot be joined. Not retried. */
class FileChangedException(message: String) : Exception(message)

/**
 * Fetches a file in parallel parts with HTTP range requests. Each part writes its own slice of a single
 * part file, so the parts can be paused, resumed after an app restart, and retried one by one.
 * Servers without range support are fetched as one stream that restarts on a retry.
 */
class DownloadEngine {

    companion object {
        /** Files smaller than this are fetched in one piece. */
        const val SPLIT_THRESHOLD = 4L * 1024 * 1024
        const val MAX_PARTS = 4
        const val MAX_ATTEMPTS = 6
        private const val BUFFER = 64 * 1024
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 30_000
    }

    /** Asks the server for the size and range support with a one-byte request. Blocking; call on IO. */
    fun probe(url: String): Probe {
        val conn = open(url)
        conn.setRequestProperty("Range", "bytes=0-0")
        try {
            val code = conn.responseCode
            val etag = conn.getHeaderField("ETag")
            return when (code) {
                206 -> {
                    val total = conn.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: -1L
                    Probe(total, etag, total > 0)
                }
                200 -> Probe(conn.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L, etag, false)
                else -> throw IOException("The server answered HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }

    /** Splits a file into parts. Unknown sizes and servers without ranges get one part. */
    fun plan(total: Long, ranges: Boolean): List<Segment> {
        if (total < 0) return listOf(Segment(0, -1))
        if (!ranges || total < SPLIT_THRESHOLD) return listOf(Segment(0, total - 1))
        val parts = MAX_PARTS
        val size = total / parts
        return (0 until parts).map { i ->
            val start = i * size
            val end = if (i == parts - 1) total - 1 else start + size - 1
            Segment(start, end)
        }
    }

    /**
     * Fetches every part into [file] and returns when all are complete. Throws on a failure that retries
     * did not fix, or [FileChangedException]. Cancelling the caller stops the open connections at once.
     * [onBytes] is called with the part index after each read.
     */
    suspend fun download(
        url: String,
        etag: String?,
        ranged: Boolean,
        file: File,
        parts: List<Part>,
        onBytes: (Int) -> Unit,
    ) {
        coroutineScope {
            parts.map { part ->
                async(Dispatchers.IO) {
                    if (part.seg.complete) return@async
                    fetchWithRetry(url, etag, ranged, file, part, onBytes)
                }
            }.awaitAll()
        }
    }

    private suspend fun fetchWithRetry(
        url: String,
        etag: String?,
        ranged: Boolean,
        file: File,
        part: Part,
        onBytes: (Int) -> Unit,
    ) {
        var attempt = 0
        while (true) {
            try {
                fetchOnce(url, etag, ranged, file, part, onBytes)
                return
            } catch (e: FileChangedException) {
                throw e
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                attempt++
                if (attempt >= MAX_ATTEMPTS) throw e
                BrowserLog.warn("download", "Part ${part.index + 1} stopped (${e.message ?: "network error"}), retry $attempt of ${MAX_ATTEMPTS - 1}")
                // A stream without ranges has no offset to resume from, so it starts over.
                if (!ranged || part.seg.end < 0) part.seg = part.seg.copy(done = 0)
                delay(minOf(30_000L, 1_000L shl minOf(attempt, 5)))
            }
        }
    }

    private suspend fun fetchOnce(
        url: String,
        etag: String?,
        ranged: Boolean,
        file: File,
        part: Part,
        onBytes: (Int) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val seg = part.seg
        val conn = open(url)
        // Stopping the caller closes the socket so a blocked read ends at once.
        val watcher = CoroutineScope(currentCoroutineContext()).launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                conn.disconnect()
            }
        }
        try {
            val wantRange = ranged && seg.end >= 0
            if (wantRange) {
                conn.setRequestProperty("Range", "bytes=${seg.start + seg.done}-${seg.end}")
                etag?.let { conn.setRequestProperty("If-Range", it) }
            }
            val code = conn.responseCode
            if (wantRange && code == 200) throw FileChangedException("The file changed on the server. Retry to start it again.")
            if (wantRange && code != 206 && code != 416) throw IOException("The server answered HTTP $code")
            if (!wantRange && code != 200) throw IOException("The server answered HTTP $code")
            if (code == 416) throw FileChangedException("The file changed on the server. Retry to start it again.")

            val start = if (wantRange) seg.start + seg.done else 0L
            RandomAccessFile(file, "rw").use { raf ->
                raf.seek(start)
                conn.inputStream.use { input ->
                    val buffer = ByteArray(BUFFER)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        raf.write(buffer, 0, read)
                        part.seg = part.seg.copy(done = part.seg.done + read)
                        onBytes(part.index)
                    }
                }
            }
            // Unknown-size streams end at EOF; ranged parts are complete once every byte arrived.
            part.seg = part.seg.copy(finished = true)
            if (!wantRange && seg.end < 0) return@withContext
            if (!part.seg.complete) throw IOException("The connection closed early")
        } catch (e: CancellationException) {
            throw e
        } finally {
            watcher.cancel()
            conn.disconnect()
        }
    }

    private fun open(url: String): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("Accept-Encoding", "identity")
        return conn
    }
}
