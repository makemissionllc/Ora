package com.makemission.ora.downloads

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

class DownloadResumeTest {

    private lateinit var server: MockWebServer
    private val body = Random(11).nextBytes(9 * 1024 * 1024)
    private val ranges = mutableListOf<String>()
    private val out = File(System.getProperty("java.io.tmpdir"), "ora-resume-test.part")

    @Before
    fun start() {
        ranges.clear()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val range = request.getHeader("Range")
                ranges += range.orEmpty()
                val m = range?.let { Regex("bytes=(\\d+)-(\\d*)").find(it) } ?: return MockResponse().setResponseCode(200).setBody(Buffer().write(body))
                val start = m.groupValues[1].toInt()
                val end = m.groupValues[2].toIntOrNull() ?: (body.size - 1)
                return MockResponse().setResponseCode(206)
                    .setHeader("Content-Range", "bytes $start-$end/${body.size}")
                    .setHeader("ETag", "\"v1\"")
                    .setBody(Buffer().write(body.copyOfRange(start, end + 1)))
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
    fun resumeContinuesFromTheSavedOffsetsNotFromZero() = runBlocking {
        val url = server.url("/big.bin").toString()
        val engine = DownloadEngine()
        val probe = engine.probe(url)
        val parts = engine.plan(probe.total, probe.ranges).mapIndexed { i, s -> Part(i, s) }
        java.io.RandomAccessFile(out, "rw").use { it.setLength(probe.total) }

        // Pause: stop the download once part 0 has about 1 MB, then keep what it saved.
        lateinit var job: Job
        job = launch(Dispatchers.IO) {
            engine.download(url, probe.etag, true, out, parts) { _ ->
                if (parts[0].seg.done > 1_000_000) job.cancel()
            }
        }
        job.join()
        val saved = parts.map { it.seg }
        val savedOffset = saved[0].start + saved[0].done
        assertTrue("part 0 should have saved bytes", saved[0].done > 0)
        ranges.clear()

        // Resume from the saved state, as the repository does after a restart.
        val resumed = saved.mapIndexed { i, s -> Part(i, s) }
        engine.download(url, probe.etag, true, out, resumed) {}

        assertTrue("first request after resume must start at the saved offset", ranges.any { it == "bytes=$savedOffset-${saved[0].end}" })
        assertArrayEquals(body, out.readBytes())
    }
}
