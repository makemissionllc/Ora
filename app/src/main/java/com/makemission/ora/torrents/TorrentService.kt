package com.makemission.ora.torrents

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.makemission.ora.log.BrowserLog

/** Keeps Ora alive (a data-sync foreground service) while a torrent is downloading with the app in the background. */
class TorrentService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        TorrentNotices.createChannel(this)
        try {
            ServiceCompat.startForeground(
                this, TorrentNotices.ONGOING_ID,
                TorrentNotices.ongoing(this, TorrentEngine.get(this).rows.value),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } catch (t: Throwable) {
            BrowserLog.error("torrents", "The torrent service could not start in the foreground", t)
            stopSelf()
        }
        return START_NOT_STICKY
    }
}
