package com.makemission.ora.downloads

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DownloadStoreTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun sample() = SavedDownload(
        id = 4,
        fileName = "report.pdf",
        url = "https://files.example.com/report.pdf",
        mimeType = "application/pdf",
        totalBytes = 9_000,
        etag = "\"v1\"",
        ranged = true,
        startedAt = 1_700_000_000_000,
        parts = listOf(Segment(0, 2249, done = 100), Segment(2250, 4499, done = 2250, finished = true)),
    )

    @Test
    fun savedQueueComesBackAfterANewStoreOpens() = runBlocking {
        val legacy = File(context.filesDir, "unused-queue.json")
        val first = DownloadStore(context, legacy)
        first.save(listOf(sample())).join()

        val reopened = DownloadStore(context, legacy).load()
        assertEquals(listOf(sample()), reopened)
    }

    @Test
    fun anOldJsonQueueMovesIntoTheDatabaseAndTheFileGoes() = runBlocking {
        val legacy = File(context.filesDir, "legacy-queue.json")
        legacy.writeText(
            """[{"id":7,"fileName":"a.zip","url":"https://x.example.org/a.zip","mimeType":null,"totalBytes":500,
            |"etag":null,"ranged":false,"startedAt":5,"parts":[{"start":0,"end":-1,"done":40,"finished":false}]}]""".trimMargin()
        )
        val loaded = DownloadStore(context, legacy).load()
        assertEquals(1, loaded.size)
        assertEquals("a.zip", loaded[0].fileName)
        assertEquals(40L, loaded[0].parts[0].done)
        assertFalse(legacy.exists())
        assertTrue(DownloadStore(context, legacy).load().size == 1)
    }
}
