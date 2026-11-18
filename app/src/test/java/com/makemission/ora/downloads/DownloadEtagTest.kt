package com.makemission.ora.downloads

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.random.Random

class DownloadEtagTest {

    private lateinit var server: MockWebServer
    private val newBody = Random(5).nextBytes(9 * 1024 * 1024)
    private val out = File(System.getProperty("java.io.tmpdir"), "ora-etag-test.part")

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                // The file on the server is now version 2. A real server ignores If-Range for a different
                // ETag and answers 200 with the whole file, so do the same here.
                if (request.getHeader("If-Range") != null && request.getHeader("If-Range") != "\"v2\"") {
                    return MockResponse().setResponseCode(200).setHeader("ETag", "\"v2\"").setBody(Buffer().write(newBody))
                }
                return MockResponse().setResponseCode(206).setHeader("ETag", "\"v2\"")
                    .setHeader("Content-Range", "bytes 0-0/${newBody.size}").setBody(Buffer().write(newBody.copyOfRange(0, 1)))
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
    fun aChangedFileIsRejectedSoTheCallerCanRestart() = runBlocking {
        val url = server.url("/file.bin").toString()
        val engine = DownloadEngine()
        val parts = engine.plan(newBody.size.toLong(), true).mapIndexed { i, s ->
            Part(i, if (i == 0) s.copy(done = 1024) else s)
        }
        java.io.RandomAccessFile(out, "rw").use { it.setLength(newBody.size.toLong()) }
        try {
            // The saved ETag is v1, the server now has v2, so the resume must not join the old bytes.
            engine.download(url, "\"v1\"", true, out, parts) {}
            fail("expected FileChangedException")
        } catch (e: FileChangedException) {
            assertTrue(e.message.orEmpty().isNotEmpty())
        }
    }
}
