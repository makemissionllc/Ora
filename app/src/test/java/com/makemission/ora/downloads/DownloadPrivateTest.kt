package com.makemission.ora.downloads

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadPrivateTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private val body = Random(5).nextBytes(9 * 1024 * 1024)

    @Before
    fun start() {
        context = ApplicationProvider.getApplicationContext()
        // Slow body, so a download is still running when the test pauses it.
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val range = request.getHeader("Range")
                val m = range?.let { Regex("bytes=(\\d+)-(\\d*)").find(it) }
                    ?: return MockResponse().setResponseCode(200).setBody(Buffer().write(body)).throttleBody(32 * 1024, 1, TimeUnit.SECONDS)
                val start = m.groupValues[1].toInt()
                val end = m.groupValues[2].toIntOrNull() ?: (body.size - 1)
                return MockResponse().setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$end/${body.size}")
                    .setHeader("ETag", "\"v1\"")
                    .setBody(Buffer().write(body.copyOfRange(start, end + 1)))
                    .throttleBody(32 * 1024, 1, TimeUnit.SECONDS)
            }
        }
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun privateDownloadIsNeverWrittenToTheQueue() = runBlocking {
        val repository = DownloadsRepository(context, fastHttp = { true }, transportFor = { _, _ -> HttpUrlTransport() })
        repository.begin(server.url("/private.bin").toString(), "private.bin", null, -1, "https://example.test/", isPrivate = true)
        repository.begin(server.url("/normal.bin").toString(), "normal.bin", null, -1, "https://example.test/", isPrivate = false)

        withTimeout(20_000) { while (repository.items.value.none { it.bytes > 0 } || repository.items.value.any { it.bytes == 0L }) delay(20) }
        repository.pauseAll()
        withTimeout(20_000) { while (repository.items.value.any { it.status == DownloadStatus.RUNNING }) delay(20) }

        // Only the normal download is in the queue. The private one is gone after a restart.
        val saved = DownloadStore(context, File(context.filesDir, "downloads/queue.json")).load()
        assertEquals(listOf("normal.bin"), saved.map { it.fileName })
    }
}
