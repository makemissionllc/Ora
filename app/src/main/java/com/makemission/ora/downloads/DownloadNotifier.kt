package com.makemission.ora.downloads

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Posts one notification per download, grouped, with the buttons [DownloadNotificationText] allows for its state:
 * Pause or Resume and Cancel while it runs, Retry when it failed, Open when it is done. A private download never
 * shows its file name. The shared "N downloads in progress" notification stays the foreground one.
 */
internal class DownloadNotifier(
    private val context: Context,
    private val repository: DownloadsRepository,
    scope: CoroutineScope,
) {
    /** What was last posted for each download, so an unchanged line is not posted again. */
    private val posted = HashMap<Long, String>()

    init {
        DownloadNotifications.createChannel(context)
        scope.launch { repository.items.collect { items -> sync(items) } }
    }

    @SuppressLint("MissingPermission")
    private fun sync(items: List<DownloadItem>) {
        val manager = NotificationManagerCompat.from(context)
        val live = items.associateBy { it.id }
        (posted.keys - live.keys).toList().forEach { manager.cancel(notificationId(it)); posted.remove(it) }
        for (item in items) {
            if (item.status == DownloadStatus.CANCELLED) {
                if (posted.remove(item.id) != null) manager.cancel(notificationId(item.id))
                continue
            }
            val isPrivate = repository.isPrivate(item.id)
            val title = DownloadNotificationText.title(item, isPrivate)
            val line = DownloadNotificationText.line(item)
            val key = "${item.status}|$title|$line"
            if (posted[item.id] == key) continue
            posted[item.id] = key
            try {
                manager.notify(notificationId(item.id), build(item, title, line))
            } catch (e: Exception) {
                BrowserLog.error("download", "Could not post a download notification", e)
            }
        }
    }

    private fun build(item: DownloadItem, title: String, line: String): android.app.Notification {
        val running = item.status == DownloadStatus.RUNNING
        val builder = NotificationCompat.Builder(context, DownloadNotifications.CHANNEL_ID)
            .setSmallIcon(if (running) android.R.drawable.stat_sys_download else android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText(line)
            .setGroup(GROUP)
            .setOnlyAlertOnce(true)
            .setOngoing(running)
            .setAutoCancel(!running)
        if (running) {
            if (item.progress >= 0f) builder.setProgress(100, (item.progress * 100).toInt(), false) else builder.setProgress(0, 0, true)
        }
        for (action in DownloadNotificationText.actions(item)) {
            val label = when (action) {
                DownloadNotificationText.Action.PAUSE -> "Pause"
                DownloadNotificationText.Action.RESUME -> "Resume"
                DownloadNotificationText.Action.CANCEL -> "Cancel"
                DownloadNotificationText.Action.RETRY -> "Retry"
                DownloadNotificationText.Action.OPEN -> "Open"
            }
            val intent = if (action == DownloadNotificationText.Action.OPEN) openIntent(item) else actionIntent(item, action)
            if (intent != null) builder.addAction(0, label, intent)
        }
        return builder.build()
    }

    private fun actionIntent(item: DownloadItem, action: DownloadNotificationText.Action): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            (item.id * 8 + action.ordinal).toInt(),
            Intent(context, DownloadActionReceiver::class.java)
                .setAction(DownloadNotifications.ACTION_ITEM)
                .putExtra(DownloadNotifications.EXTRA_ID, item.id)
                .putExtra(DownloadNotifications.EXTRA_ACTION, action.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openIntent(item: DownloadItem): PendingIntent? {
        val uri = item.contentUri ?: return null
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, item.mimeType ?: "*/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(
            context,
            (item.id * 8 + DownloadNotificationText.Action.OPEN.ordinal).toInt(),
            view,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationId(id: Long): Int = ITEM_ID_BASE + (id % 100_000).toInt()

    private companion object {
        const val GROUP = "com.makemission.ora.downloads.GROUP"
        const val ITEM_ID_BASE = 5000
    }
}
