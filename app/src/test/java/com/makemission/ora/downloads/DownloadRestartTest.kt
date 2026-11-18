package com.makemission.ora.downloads

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadRestartTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun aRestartKeepsProgressAndShowsThePausedDownload() = runBlocking {
        val queue = File(File(context.filesDir, "downloads").apply { mkdirs() }, "queue.json")
        DownloadStore(context, queue).save(
            listOf(
                SavedDownload(
                    id = 9,
                    fileName = "movie.mp4",
                    url = "https://media.example.com/movie.mp4",
                    mimeType = "video/mp4",
                    totalBytes = 9_000,
                    etag = "\"v3\"",
                    ranged = true,
                    startedAt = 1L,
                    parts = listOf(Segment(0, 2249, done = 100), Segment(2250, 4499, done = 2250)),
                ),
            ),
        ).join()

        // A new repository is what the app builds after it was killed or the phone restarted.
        val repo = DownloadsRepository(context)
        val items = repo.items.value
        assertEquals(1, items.size)
        assertEquals(DownloadStatus.PAUSED, items[0].status)
        assertEquals("movie.mp4", items[0].fileName)
        assertEquals(2350L, items[0].bytes)
    }
}
