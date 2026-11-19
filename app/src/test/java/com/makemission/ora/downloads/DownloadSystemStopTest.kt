package com.makemission.ora.downloads

import android.app.job.JobScheduler
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/** What happens to running downloads when Android stops the background job and later starts it again. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadSystemStopTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private val body = Random(7).nextBytes(9 * 1024 * 1024)

    @Before
    fun start() {
        context = ApplicationProvider.getApplicationContext()
        // Slow body, so a download is still running when the test stops it.
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

    private fun repository() = DownloadsRepository(context, fastHttp = { true }, transportFor = { _, _ -> HttpUrlTransport() })

    private fun DownloadsRepository.startSlowDownload() {
        begin(server.url("/system.bin").toString(), "system.bin", null, -1, "https://example.test/", isPrivate = false)
    }

    @Test
    fun aStopByAndroidPausesTheDownloadAndTheRestartContinuesIt() = runBlocking {
        val repository = repository()
        repository.startSlowDownload()
        withTimeout(20_000) { while (repository.items.value.none { it.bytes > 0 }) delay(20) }

        repository.pauseForSystemStop()
        withTimeout(20_000) { while (repository.items.value.any { it.status == DownloadStatus.RUNNING }) delay(20) }
        val paused = repository.items.value.single()
        assertEquals(DownloadStatus.PAUSED, paused.status)
        assertTrue("the row says Android paused it", paused.pausedBySystem)
        assertTrue("the bytes saved before the stop stay", paused.bytes > 0)

        // Android restarts the job: the download continues without a tap on Resume.
        repository.resumeAfterSystemStop()
        val resumed = repository.items.value.single()
        assertEquals(DownloadStatus.RUNNING, resumed.status)
        assertFalse(resumed.pausedBySystem)
        withTimeout(20_000) { while (repository.items.value.single().bytes <= paused.bytes) delay(20) }

        repository.pauseAll()
        withTimeout(20_000) { while (repository.items.value.any { it.status == DownloadStatus.RUNNING }) delay(20) }
    }

    @Test
    fun aStopByAndroidKeepsTheJobScheduledUntilItRestarts() = runBlocking {
        val repository = repository()
        val scheduler = context.getSystemService(JobScheduler::class.java)!!
        repository.startSlowDownload()
        withTimeout(20_000) { while (repository.items.value.none { it.bytes > 0 }) delay(20) }
        assertNotNull("the job is scheduled while a download runs", scheduler.getPendingJob(DownloadJobService.JOB_ID))

        repository.pauseForSystemStop()
        withTimeout(20_000) { while (repository.items.value.any { it.status == DownloadStatus.RUNNING }) delay(20) }
        // Without this job, Android would never start the paused download again.
        assertNotNull("the job stays scheduled for the restart", scheduler.getPendingJob(DownloadJobService.JOB_ID))

        repository.pauseAll()
        withTimeout(20_000) { while (repository.items.value.any { it.status == DownloadStatus.RUNNING }) delay(20) }
    }

    @Test
    fun aPauseTheUserAsksForIsNotUndoneByTheRestart() = runBlocking {
        val repository = repository()
        repository.startSlowDownload()
        withTimeout(20_000) { while (repository.items.value.none { it.bytes > 0 }) delay(20) }

        repository.pauseForSystemStop()
        withTimeout(20_000) { while (repository.items.value.any { it.status == DownloadStatus.RUNNING }) delay(20) }
        // The user pauses everything from the notification before Android restarts the job.
        repository.pauseAll()
        assertFalse(repository.items.value.single().pausedBySystem)

        repository.resumeAfterSystemStop()
        assertEquals(DownloadStatus.PAUSED, repository.items.value.single().status)
    }
}
