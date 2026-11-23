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
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadQueueTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private val body = ByteArray(300_000) { it.toByte() }

    @Before
    fun start() {
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(200)
                .setBody(Buffer().write(body)).throttleBody(50_000, 200, TimeUnit.MILLISECONDS)
        }
        server.start()
    }

    @After
    fun stop() = server.shutdown()

    @Test
    fun onlyTheLimitRunsAndTheRestWaitThenFinish() = runBlocking {
        val repo = DownloadsRepository(context, fastHttp = { true }, maxSimultaneous = { 1 })
        val url = server.url("/f.bin").toString()
        repo.begin(url, "a.bin", null, body.size.toLong(), "", false)
        repo.begin(url, "b.bin", null, body.size.toLong(), "", false)
        assertEquals(1, repo.items.value.count { it.status == DownloadStatus.RUNNING })
        val waiting = repo.items.value.single { it.status == DownloadStatus.PAUSED }
        assertEquals(DownloadsRepository.QUEUED_NOTICE, waiting.notice)
        withTimeout(30_000) {
            while (repo.items.value.any { it.status != DownloadStatus.DONE }) delay(100)
        }
        assertEquals(2, repo.items.value.count { it.status == DownloadStatus.DONE })
    }
}
