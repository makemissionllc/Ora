package com.makemission.ora.downloads

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.makemission.ora.log.BrowserLog

/**
 * Keeps downloads running while the app is in the background. It runs only while a download is active, and its
 * notification has Pause all and Cancel all. Android may stop it after its time limit; then the downloads pause.
 */
class DownloadService : Service() {

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val repository = DownloadsRepository.current
        when (intent?.action) {
            ACTION_PAUSE_ALL -> repository?.pauseAll()
            ACTION_CANCEL_ALL -> repository?.cancelAll()
        }
        val running = repository?.activeCount() ?: 0
        // Android requires startForeground after a startForegroundService call, even when nothing is left to run.
        startForeground(NOTIFICATION_ID, notification(running), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
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

    private fun notification(running: Int): Notification {
        val text = if (running == 1) "1 download in progress" else "$running downloads in progress"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Ora")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Pause", actionIntent(ACTION_PAUSE_ALL, 1))
            .addAction(0, "Cancel", actionIntent(ACTION_CANCEL_ALL, 2))
            .build()
    }

    private fun actionIntent(action: String, code: Int): PendingIntent = PendingIntent.getService(
        this,
        code,
        Intent(this, DownloadService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    companion object {
        const val CHANNEL_ID = "downloads"
        const val NOTIFICATION_ID = 4101
        const val ACTION_PAUSE_ALL = "com.makemission.ora.downloads.PAUSE_ALL"
        const val ACTION_CANCEL_ALL = "com.makemission.ora.downloads.CANCEL_ALL"
    }
}
