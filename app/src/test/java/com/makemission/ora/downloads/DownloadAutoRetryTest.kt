package com.makemission.ora.downloads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadAutoRetryTest {

    @Test
    fun aDroppedConnectionIsTriedAgain() {
        assertTrue(DownloadsRepository.isTemporaryFailure("The connection closed early"))
        assertTrue(DownloadsRepository.isTemporaryFailure(null))
    }

    @Test
    fun aBusyOrThrottledServerIsTriedAgain() {
        assertTrue(DownloadsRepository.isTemporaryFailure("The server answered HTTP 503"))
        assertTrue(DownloadsRepository.isTemporaryFailure("The server answered HTTP 429"))
        assertTrue(DownloadsRepository.isTemporaryFailure("The server answered HTTP 408"))
    }

    @Test
    fun aMissingOrForbiddenFileIsNotRetriedByItself() {
        assertFalse(DownloadsRepository.isTemporaryFailure("The server answered HTTP 403"))
        assertFalse(DownloadsRepository.isTemporaryFailure("The server answered HTTP 404"))
    }
}
