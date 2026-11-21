package com.makemission.ora.torrents

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.makemission.ora.MainActivity
import com.makemission.ora.log.BrowserLog

/** Notifications for torrents: the ongoing one (kept up by [TorrentService]) and a "finished" one per torrent. */
object TorrentNotices {
    const val CHANNEL_ID = "torrents"
    const val ONGOING_ID = 4201
    private var running = false

    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Torrents", NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun open(context: Context) = PendingIntent.getActivity(
        context, 0, Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun ongoing(context: Context, rows: List<TorrentRow>) = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download)
        .setContentTitle("Ora torrents")
        .setContentText(
            "${rows.count { !it.finished && !it.paused }} downloading  ·  " + TorrentFormat.rate(rows.sumOf { it.downRate })
        )
        .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open(context)).build()

    /** Starts the foreground service while something is downloading, and stops it when nothing is. */
    fun sync(context: Context, rows: List<TorrentRow>) {
        val active = rows.any { !it.finished && !it.paused }
        try {
            if (active && !running) {
                running = true
                ContextCompat.startForegroundService(context, Intent(context, TorrentService::class.java))
            } else if (!active && running) {
                running = false
                context.stopService(Intent(context, TorrentService::class.java))
            } else if (active) {
                context.getSystemService(NotificationManager::class.java)?.let {
                    if (granted(context)) it.notify(ONGOING_ID, ongoing(context, rows))
                }
            }
        } catch (t: Throwable) {
            running = false
            BrowserLog.warn("torrents", "Could not update the torrent notification", t)
        }
    }

    private fun granted(context: Context) =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun finished(context: Context, name: String) {
        if (!granted(context)) return
        createChannel(context)
        context.getSystemService(NotificationManager::class.java)?.notify(
            name.hashCode(),
            NotificationCompat.Builder(context, CHANNEL_ID).setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Torrent finished").setContentText(name).setAutoCancel(true).setContentIntent(open(context)).build(),
        )
    }
}
