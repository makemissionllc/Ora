package com.makemission.ora.downloads

import android.app.job.JobParameters
import android.app.job.JobService
import androidx.annotation.RequiresApi
import com.makemission.ora.log.BrowserLog

/**
 * Keeps downloads running in the background on Android 14 and later, as a user-initiated data-transfer job.
 * The job runs while downloads are active; the repository cancels it when the last one finishes.
 */
@RequiresApi(34)
class DownloadJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        DownloadNotifications.createChannel(this)
        val running = DownloadsRepository.current?.activeCount() ?: 0
        // The job must show its notification. DETACH keeps it visible after the job ends.
        setNotification(
            params,
            DownloadNotifications.NOTIFICATION_ID,
            DownloadNotifications.build(this, running),
            JOB_END_NOTIFICATION_POLICY_DETACH,
        )
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        // The system ended the job early, so the downloads pause and can be resumed from the list.
        BrowserLog.warn("download", "The system ended the background download job, so downloads were paused")
        DownloadsRepository.current?.pauseAll()
        return false
    }

    companion object {
        const val JOB_ID = 4102
    }
}
