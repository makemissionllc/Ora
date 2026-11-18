package com.makemission.ora.downloads

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

/**
 * The notification shown while downloads run, shared by the foreground service (Android 13) and the
 * user-initiated job (Android 14 and later). Its buttons go to [DownloadActionReceiver], so they work on both.
 */
object DownloadNotifications {
    const val CHANNEL_ID = "downloads"
    const val NOTIFICATION_ID = 4101
    const val ACTION_PAUSE_ALL = "com.makemission.ora.downloads.PAUSE_ALL"
    const val ACTION_CANCEL_ALL = "com.makemission.ora.downloads.CANCEL_ALL"

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW),
        )
    }

    fun build(context: Context, running: Int): Notification {
        val text = when {
            running <= 0 -> "Downloads in progress"
            running == 1 -> "1 download in progress"
            else -> "$running downloads in progress"
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Ora")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "Pause", actionIntent(context, ACTION_PAUSE_ALL, 1))
            .addAction(0, "Cancel", actionIntent(context, ACTION_CANCEL_ALL, 2))
            .build()
    }

    private fun actionIntent(context: Context, action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        code,
        Intent(context, DownloadActionReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** Runs the notification's Pause all and Cancel all buttons on the repository the app is using. */
class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = DownloadsRepository.current ?: return
        when (intent.action) {
            DownloadNotifications.ACTION_PAUSE_ALL -> repository.pauseAll()
            DownloadNotifications.ACTION_CANCEL_ALL -> repository.cancelAll()
        }
    }
}
