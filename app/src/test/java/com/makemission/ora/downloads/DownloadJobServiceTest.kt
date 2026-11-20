package com.makemission.ora.downloads

import android.app.job.JobParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadJobServiceTest {

    @Test
    fun aStopTheUserOrARestrictionAskedForWaitsForTheUser() {
        assertTrue(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_USER))
        assertTrue(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_BACKGROUND_RESTRICTION))
    }

    @Test
    fun aTemporaryStopContinuesWhenAndroidRestartsTheJob() {
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_APP_STANDBY))
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_TIMEOUT))
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_PREEMPT))
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY))
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_QUOTA))
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_DEVICE_STATE))
        assertFalse(DownloadJobService.isUserOrRestrictionStop(JobParameters.STOP_REASON_SYSTEM_PROCESSING))
    }

    @Test
    fun theLogNamesTheStopReason() {
        assertEquals("timeout", DownloadJobService.stopReasonName(JobParameters.STOP_REASON_TIMEOUT))
        assertEquals("reason 999", DownloadJobService.stopReasonName(999))
    }
}
