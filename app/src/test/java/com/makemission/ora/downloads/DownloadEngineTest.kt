package com.makemission.ora.downloads

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

class DownloadEngineTest {

    private lateinit var server: MockWebServer
    private val body = Random(7).nextBytes(9 * 1024 * 1024)
    private val tmp = File(System.getProperty("java.io.tmpdir"), "ora-engine-test").apply { mkdirs() }

    @Before
    fun start() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    /** Serves [body] with range support, answering Range requests with 206 and the matching slice. */
    private fun serveWithRanges() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val range = request.getHeader("Range")
                val m = range?.let { Regex("bytes=(\\d+)-(\\d*)").find(it) }
                if (m == null) return MockResponse().setResponseCode(200).setHeader("ETag", "\"v1\"").setBody(okio.Buffer().write(body))
                val start = m.groupValues[1].toInt()
                val end = m.groupValues[2].toIntOrNull() ?: (body.size - 1)
                val slice = body.copyOfRange(start, end + 1)
                return MockResponse().setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$end/${body.size}")
                    .setHeader("ETag", "\"v1\"")
                    .setBody(okio.Buffer().write(slice))
            }
        }
    }

    /** Ignores Range and always answers with the whole file, like a server without range support. */
    private fun serveWithoutRanges() {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse().setResponseCode(200).setBody(okio.Buffer().write(body))
        }
    }

    private fun download(engine: DownloadEngine, url: String, probe: Probe, out: File): List<Part> {
        val parts = engine.plan(probe.total, probe.ranges).mapIndexed { i, s -> Part(i, s) }
        java.io.RandomAccessFile(out, "rw").use { it.setLength(probe.total) }
        kotlinx.coroutines.runBlocking { engine.download(url, probe.etag, probe.ranges, out, parts) {} }
        return parts
    }

    @Test
    fun rangeServerIsSplitIntoFourPartsAndJoinsExactly() {
        serveWithRanges()
        val url = server.url("/file.bin").toString()
        val engine = DownloadEngine()
        val probe = engine.probe(url)
        assertTrue(probe.ranges)
        assertEquals(body.size.toLong(), probe.total)

        val out = File(tmp, "ranges.part")
        val parts = download(engine, url, probe, out)
        assertEquals(4, parts.size)
        assertTrue(parts.all { it.seg.complete })
        assertArrayEquals(body, out.readBytes())
    }

    @Test
    fun serverWithoutRangesFallsBackToOneStream() {
        serveWithoutRanges()
        val url = server.url("/file.bin").toString()
        val engine = DownloadEngine()
        val probe = engine.probe(url)
        assertFalse(probe.ranges)

        val out = File(tmp, "single.part")
        val parts = download(engine, url, probe, out)
        assertEquals(1, parts.size)
        assertArrayEquals(body, out.readBytes())
    }

    @Test
    fun smallFileIsOneStreamEvenWhenRangesWork() {
        val small = Random(3).nextBytes(1024 * 1024)
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val m = request.getHeader("Range")?.let { Regex("bytes=(\\d+)-(\\d*)").find(it) }
                if (m == null) return MockResponse().setResponseCode(200).setBody(okio.Buffer().write(small))
                val start = m.groupValues[1].toInt()
                val end = m.groupValues[2].toIntOrNull() ?: (small.size - 1)
                return MockResponse().setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$end/${small.size}")
                    .setBody(okio.Buffer().write(small.copyOfRange(start, end + 1)))
            }
        }
        val url = server.url("/small.bin").toString()
        val engine = DownloadEngine()
        val probe = engine.probe(url)
        assertEquals(1, engine.plan(probe.total, probe.ranges).size)
        assertEquals(1, download(engine, url, probe, File(tmp, "small.part")).size)
    }
}
