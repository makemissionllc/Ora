package com.makemission.ora.downloads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class DownloadLogTest {

    @Test
    fun startedNamesTheSiteAndSizeNotTheFile() {
        val line = downloadLogLine("Started", "https://www.example.com/files/report-2026.pdf", " (2.0 MB)")
        assertEquals("Started a download from example.com (2.0 MB)", line)
        assertFalse(line.contains("report-2026"))
    }

    @Test
    fun savedKeepsTheSubdomainAndDropsThePathAndQuery() {
        val line = downloadLogLine("Saved", "https://cdn.example.org/a/b.zip?token=1", " (3 KB) to Downloads/Ora")
        assertEquals("Saved a download from cdn.example.org (3 KB) to Downloads/Ora", line)
    }

    @Test
    fun aMissingAddressSaysUnknown() {
        assertEquals("Paused a download from unknown", downloadLogLine("Paused", null))
    }
}
