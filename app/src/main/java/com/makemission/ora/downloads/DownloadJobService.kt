package com.makemission.ora.downloads

import android.app.job.JobParameters
import android.app.job.JobService
import androidx.annotation.RequiresApi
import com.makemission.ora.log.BrowserLog

/**
 * Keeps downloads running in the background on Android 14 and later, as a user-initiated data-transfer job.
 * The job runs while downloads are active; the repository cancels it when the last one finishes.
 *
 * Android can stop the job at any time, for example after a timeout or a lost connection. The downloads then pause
 * here and start again when Android restarts the job, so a stop does not need a tap on Resume.
 */
@RequiresApi(34)
class DownloadJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        DownloadNotifications.createChannel(this)
        val repository = DownloadsRepository.current
        // Downloads that Android paused when it stopped the job continue now.
        repository?.resumeAfterSystemStop()
        val running = repository?.activeCount() ?: 0
        // Android requires the notification within 10 seconds of the start. DETACH keeps it visible after the job ends.
        setNotification(
            params,
            DownloadNotifications.NOTIFICATION_ID,
            DownloadNotifications.build(this, running),
            JOB_END_NOTIFICATION_POLICY_DETACH,
        )
        // With nothing to download there is no work, so the job ends here.
        return running > 0
    }

    override fun onStopJob(params: JobParameters): Boolean {
        val reason = stopReasonName(params.stopReason)
        val repository = DownloadsRepository.current
        if (repository == null || repository.activeCount() == 0) {
            BrowserLog.info("download", "The system stopped the background download job ($reason). No download was running")
            return false
        }
        if (isUserOrRestrictionStop(params.stopReason)) {
            BrowserLog.warn("download", "The system stopped the background download job ($reason), so downloads were paused")
            repository.pauseAll()
            return false
        }
        // The work has to stop when the job does. The transfers pause here and continue when Android restarts the job.
        BrowserLog.warn("download", "The system stopped the background download job ($reason). Downloads will continue when it restarts")
        repository.pauseForSystemStop()
        // true asks Android to start the job again once it can, after the retry delay set in the job's builder.
        return true
    }

    /**
     * Called when the network the job uses changes. A connection that drops on the old network is retried by the
     * download itself, so nothing is needed here. Overriding it stops Android logging a warning on every change.
     */
    override fun onNetworkChanged(params: JobParameters) {
    }

    companion object {
        const val JOB_ID = 4102

        /**
         * Stops that the user or a restriction asked for. Those downloads wait for the user to resume them. Every
         * other stop is temporary, so the downloads continue when Android restarts the job.
         */
        internal fun isUserOrRestrictionStop(reason: Int): Boolean = when (reason) {
            JobParameters.STOP_REASON_USER,
            JobParameters.STOP_REASON_BACKGROUND_RESTRICTION,
            JobParameters.STOP_REASON_APP_STANDBY -> true
            else -> false
        }

        internal fun stopReasonName(reason: Int): String = when (reason) {
            JobParameters.STOP_REASON_UNDEFINED -> "undefined"
            JobParameters.STOP_REASON_CANCELLED_BY_APP -> "cancelled by the app"
            JobParameters.STOP_REASON_PREEMPT -> "preempted"
            JobParameters.STOP_REASON_TIMEOUT -> "timeout"
            JobParameters.STOP_REASON_DEVICE_STATE -> "device state"
            JobParameters.STOP_REASON_CONSTRAINT_CONNECTIVITY -> "network"
            JobParameters.STOP_REASON_USER -> "user"
            JobParameters.STOP_REASON_QUOTA -> "quota"
            JobParameters.STOP_REASON_BACKGROUND_RESTRICTION -> "background restriction"
            JobParameters.STOP_REASON_APP_STANDBY -> "app standby"
            JobParameters.STOP_REASON_SYSTEM_PROCESSING -> "system processing"
            else -> "reason $reason"
        }
    }
}
