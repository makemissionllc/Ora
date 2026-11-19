package com.makemission.ora.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadStatsTest {
    private fun item(id: Long, status: DownloadStatus, bytes: Long = 0) =
        DownloadItem(id = id, fileName = "f$id", url = "https://a/$id", mimeType = null, totalBytes = bytes, bytes = bytes, status = status)

    @Test fun nothingToTotalGivesNoFooter() {
        assertNull(DownloadStats.footer(emptyList()))
        assertNull(DownloadStats.footer(listOf(item(1, DownloadStatus.RUNNING))))
    }

    @Test fun savedAndFailedAreCounted() {
        val text = DownloadStats.footer(listOf(item(1, DownloadStatus.DONE, 1024), item(2, DownloadStatus.DONE, 1024), item(3, DownloadStatus.FAILED)))!!
        assertEquals(true, text.startsWith("2 saved · "))
        assertEquals(true, text.endsWith(" · 1 failed"))
    }
}
