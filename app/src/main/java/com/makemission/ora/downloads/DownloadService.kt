package com.makemission.ora.downloads

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.makemission.ora.log.BrowserLog

/**
 * Keeps downloads running in the background on Android 13, as a data-sync foreground service. It runs only while
 * a download is active. Android 14 and later use [DownloadJobService] instead.
 */
class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        DownloadNotifications.createChannel(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val running = DownloadsRepository.current?.activeCount() ?: 0
        // Android requires startForeground after a startForegroundService call, even when nothing is left to run.
        startForeground(
            DownloadNotifications.NOTIFICATION_ID,
            DownloadNotifications.build(this, running),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        if (running == 0) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android stopped the time allowed for this service. Pause what is running so it can be resumed later.
        BrowserLog.warn("download", "The background time for downloads ran out, so they were paused")
        DownloadsRepository.current?.pauseAll()
        stopSelf()
    }
}
