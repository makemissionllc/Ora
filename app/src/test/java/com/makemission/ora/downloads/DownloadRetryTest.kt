package com.makemission.ora.downloads

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

class DownloadRetryTest {

    private lateinit var server: MockWebServer
    private val body = Random(13).nextBytes(9 * 1024 * 1024)
    /** How many of the next range requests are cut short halfway, like a Wi-Fi drop mid-transfer. */
    private val dropsLeft = AtomicInteger(0)
    private val out = File(System.getProperty("java.io.tmpdir"), "ora-retry-test.part")

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val m = request.getHeader("Range")?.let { Regex("bytes=(\\d+)-(\\d*)").find(it) }
                    ?: return MockResponse().setResponseCode(200).setBody(Buffer().write(body))
                val start = m.groupValues[1].toInt()
                val end = m.groupValues[2].toIntOrNull() ?: (body.size - 1)
                val slice = body.copyOfRange(start, end + 1)
                val drop = dropsLeft.getAndDecrement() > 0
                val sent = if (drop) slice.copyOf(slice.size / 2) else slice
                return MockResponse().setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$end/${body.size}")
                    .setHeader("ETag", "\"v1\"")
                    .setBody(Buffer().write(sent))
            }
        }
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
        out.delete()
    }

    @Test
    fun theRetryScheduleIsTwoFiveFifteenThirtyThenSixtySeconds() {
        assertEquals(2_000L, retryDelayMillis(1))
        assertEquals(5_000L, retryDelayMillis(2))
        assertEquals(15_000L, retryDelayMillis(3))
        assertEquals(30_000L, retryDelayMillis(4))
        assertEquals(60_000L, retryDelayMillis(5))
        assertEquals(60_000L, retryDelayMillis(10))
    }

    @Test
    fun aDroppedConnectionRetriesThenFinishes() = runBlocking {
        val url = server.url("/file.bin").toString()
        val engine = DownloadEngine(retryDelayMs = { 1L })
        val probe = engine.probe(url)
        val parts = engine.plan(probe.total, probe.ranges).mapIndexed { i, s -> Part(i, s) }
        java.io.RandomAccessFile(out, "rw").use { it.setLength(probe.total) }

        dropsLeft.set(2)
        engine.download(url, probe.etag, true, out, parts) {}

        assertTrue("the two drops should have been used", dropsLeft.get() <= 0)
        assertArrayEquals(body, out.readBytes())
    }

    @Test
    fun givesUpAfterTheRetriesRunOut() = runBlocking {
        val url = server.url("/file.bin").toString()
        val engine = DownloadEngine(retryDelayMs = { 1L })
        val probe = engine.probe(url)
        val parts = engine.plan(probe.total, probe.ranges).mapIndexed { i, s -> Part(i, s) }
        java.io.RandomAccessFile(out, "rw").use { it.setLength(probe.total) }

        dropsLeft.set(100_000)
        try {
            engine.download(url, probe.etag, true, out, parts) {}
            fail("expected an IOException after the retries ran out")
        } catch (e: IOException) {
            // Expected: the first try and 10 retries failed, so the error goes up to the repository.
        }
    }
}
