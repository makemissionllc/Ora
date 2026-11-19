package com.makemission.ora.downloads

import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile

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
 * Requests go through [transport], so the app and the tests can send them differently.
 */
/** The wait before retry [retry] (1 is the first): 2 s, 5 s, 15 s, 30 s, then 60 s. */
internal fun retryDelayMillis(retry: Int): Long = when (retry) {
    1 -> 2_000L
    2 -> 5_000L
    3 -> 15_000L
    4 -> 30_000L
    else -> 60_000L
}

class DownloadEngine(
    private val transport: DownloadTransport = HttpUrlTransport(),
    /** Wait before retry number n (1 is the first). Tests pass a short one. */
    private val retryDelayMs: (Int) -> Long = ::retryDelayMillis,
) {

    companion object {
        /** Files smaller than this are fetched in one piece. */
        const val SPLIT_THRESHOLD = 4L * 1024 * 1024
        const val MAX_PARTS = 4
        /** Failures in a row before a part gives up: 2 s, 5 s, 15 s, 30 s, then every 60 s. Bytes saved between failures reset the count. */
        const val MAX_RETRIES = 10
        private const val BUFFER = 64 * 1024
    }

    /** Asks the server for the size and range support with a one-byte request. Blocking; call on IO. */
    fun probe(url: String): Probe {
        transport.get(url, mapOf("Range" to "bytes=0-0")).use { response ->
            val etag = response.header("ETag")
            return when (response.code) {
                206 -> {
                    val total = response.header("Content-Range")?.substringAfterLast('/')?.toLongOrNull() ?: -1L
                    Probe(total, etag, total > 0)
                }
                200 -> Probe(response.header("Content-Length")?.toLongOrNull() ?: -1L, etag, false)
                else -> throw IOException("The server answered HTTP ${response.code}")
            }
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
            val before = part.seg.done
            try {
                fetchOnce(url, etag, ranged, file, part, onBytes)
                return
            } catch (e: FileChangedException) {
                throw e
            } catch (e: IOException) {
                currentCoroutineContext().ensureActive()
                // A ranged part that saved bytes before the drop keeps them, so its count starts again. Only
                // failures in a row use up the retries, which lets a long download survive occasional drops.
                attempt = if (ranged && part.seg.end >= 0 && part.seg.done > before) 1 else attempt + 1
                if (attempt > MAX_RETRIES) throw e
                BrowserLog.warn("download", "Part ${part.index + 1} stopped (${e.message ?: "network error"}), retry $attempt of $MAX_RETRIES")
                // A stream without ranges has no offset to resume from, so it starts over.
                if (!ranged || part.seg.end < 0) part.seg = part.seg.copy(done = 0)
                delay(retryDelayMs(attempt))
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
        val wantRange = ranged && seg.end >= 0
        val headers = mutableMapOf<String, String>()
        if (wantRange) {
            headers["Range"] = "bytes=${seg.start + seg.done}-${seg.end}"
            etag?.let { headers["If-Range"] = it }
        }
        var open: RemoteResponse? = null
        // Stopping the caller closes the response so a blocked read ends at once.
        val watcher = CoroutineScope(currentCoroutineContext()).launch(Dispatchers.IO) {
            try {
                awaitCancellation()
            } finally {
                open?.close()
            }
        }
        try {
            val response = transport.get(url, headers)
            open = response
            response.use { r ->
                val code = r.code
                if (wantRange && code == 200) throw FileChangedException("The file changed on the server. Retry to start it again.")
                if (wantRange && code != 206 && code != 416) throw IOException("The server answered HTTP $code")
                if (!wantRange && code != 200) throw IOException("The server answered HTTP $code")
                if (code == 416) throw FileChangedException("The file changed on the server. Retry to start it again.")

                val start = if (wantRange) seg.start + seg.done else 0L
                RandomAccessFile(file, "rw").use { raf ->
                    raf.seek(start)
                    r.body.use { input ->
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
            }
            // Unknown-size streams end at EOF. Otherwise the part is complete only when every byte arrived; the
            // byte count decides, before the finished flag is set (the flag alone would hide a short read).
            if (!wantRange && seg.end < 0) {
                part.seg = part.seg.copy(finished = true)
                return@withContext
            }
            if (part.seg.start + part.seg.done <= part.seg.end) throw IOException("The connection closed early")
            part.seg = part.seg.copy(finished = true)
        } catch (e: CancellationException) {
            throw e
        } finally {
            watcher.cancel()
            open?.close()
        }
    }
}
