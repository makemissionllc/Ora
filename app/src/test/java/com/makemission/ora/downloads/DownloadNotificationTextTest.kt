package com.makemission.ora.downloads

import com.makemission.ora.downloads.DownloadNotificationText.Action
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadNotificationTextTest {

    private fun item(
        name: String = "archive.zip",
        status: DownloadStatus = DownloadStatus.RUNNING,
        total: Long = 1000,
        bytes: Long = 0,
        speed: Long = 0,
        pausable: Boolean = true,
    ) = DownloadItem(
        id = 1,
        fileName = name,
        url = "https://example.com/$name",
        mimeType = null,
        totalBytes = total,
        bytes = bytes,
        speed = speed,
        status = status,
        pausable = pausable,
    )

    @Test
    fun privateDownloadsHideTheFileName() {
        assertEquals("Private download", DownloadNotificationText.title(item(name = "tax-form.pdf"), isPrivate = true))
        assertEquals("tax-form.pdf", DownloadNotificationText.title(item(name = "tax-form.pdf"), isPrivate = false))
    }

    @Test
    fun runningShowsPercentAndSpeedOnlyWhenKnown() {
        assertEquals("Downloading · 42% · 2 KB/s", DownloadNotificationText.line(item(bytes = 420, speed = 2048)))
        assertEquals("Downloading", DownloadNotificationText.line(item(total = -1, bytes = 420)))
    }

    @Test
    fun pausedShowsHowFarItGot() {
        assertEquals("Paused · 420 B of 1000 B", DownloadNotificationText.line(item(status = DownloadStatus.PAUSED, bytes = 420)))
    }

    @Test
    fun savedFailedAndCancelledEachHaveOneLine() {
        assertEquals("Saved · 1000 B", DownloadNotificationText.line(item(status = DownloadStatus.DONE, bytes = 1000)))
        assertEquals("Failed · Retry to continue", DownloadNotificationText.line(item(status = DownloadStatus.FAILED)))
        assertEquals("Cancelled", DownloadNotificationText.line(item(status = DownloadStatus.CANCELLED)))
    }

    @Test
    fun actionsFollowTheState() {
        assertEquals(listOf(Action.PAUSE, Action.CANCEL), DownloadNotificationText.actions(item()))
        assertEquals(listOf(Action.CANCEL), DownloadNotificationText.actions(item(pausable = false)))
        assertEquals(listOf(Action.RESUME, Action.CANCEL), DownloadNotificationText.actions(item(status = DownloadStatus.PAUSED)))
        assertEquals(listOf(Action.RETRY, Action.CANCEL), DownloadNotificationText.actions(item(status = DownloadStatus.FAILED)))
        assertEquals(listOf(Action.OPEN), DownloadNotificationText.actions(item(name = "notes.pdf", status = DownloadStatus.DONE)))
        assertEquals(emptyList<Action>(), DownloadNotificationText.actions(item(name = "setup.apk", status = DownloadStatus.DONE)))
        assertEquals(emptyList<Action>(), DownloadNotificationText.actions(item(status = DownloadStatus.CANCELLED)))
    }
}
