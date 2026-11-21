package com.makemission.ora.torrents

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.frostwire.jlibtorrent.Sha1Hash
import com.frostwire.jlibtorrent.SessionManager
import com.frostwire.jlibtorrent.TorrentHandle
import com.frostwire.jlibtorrent.TorrentInfo
import com.frostwire.jlibtorrent.TorrentStatus
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/** One line of the torrent list, as the screen shows it. */
data class TorrentRow(
    val hash: String,
    val name: String,
    /** 0f..1f of the wanted data. */
    val progress: Float,
    val total: Long,
    val done: Long,
    val downRate: Int,
    val upRate: Int,
    val peers: Int,
    val seeds: Int,
    val paused: Boolean,
    val finished: Boolean,
    /** Plain words: "Getting details", "Downloading", "Waiting for Wi-Fi", "Paused", "Done", "Sharing". */
    val state: String,
    val eta: String?,
)

enum class AddResult { ADDED, DUPLICATE, INVALID }

/**
 * The one torrent session (jlibtorrent). Started on first use, it re-adds the saved torrents, is polled once a
 * second for the list, and obeys the Settings: Wi-Fi only (the whole session pauses on a metered network), the
 * speed cap, and whether a finished torrent keeps sharing. Files go to Ora's own folder (Android/data/.../Torrents).
 */
class TorrentEngine private constructor(private val context: Context) {
    val store = TorrentStore(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val session = SessionManager(false)
    private var poll: Job? = null

    private val _rows = MutableStateFlow<List<TorrentRow>>(emptyList())
    val rows: StateFlow<List<TorrentRow>> = _rows.asStateFlow()

    @Volatile private var wifiOnly = true
    @Volatile private var seed = false
    @Volatile private var limitKb = 0
    @Volatile private var started = false

    val saveDir: File = (context.getExternalFilesDir(null) ?: context.filesDir).resolve("Torrents").also { it.mkdirs() }

    /** Called from the browser screen whenever a torrent setting changes (and once at startup). */
    fun applySettings(wifiOnly: Boolean, seed: Boolean, limitKbPerSecond: Int) {
        this.wifiOnly = wifiOnly
        this.seed = seed
        this.limitKb = limitKbPerSecond
        if (started) scope.launch { enforce() }
    }

    /** Starts the session and re-adds saved torrents. Safe to call again; does nothing the second time. */
    @Synchronized
    fun ensureStarted() {
        if (started) return
        started = true
        scope.launch {
            try {
                session.start()
                BrowserLog.info("torrents", "Torrent session started, ${store.entries.size} saved")
                store.entries.forEach { readd(it) }
                enforce()
            } catch (t: Throwable) {
                started = false
                BrowserLog.error("torrents", "The torrent session could not start", t)
            }
        }
        poll = scope.launch {
            while (isActive) {
                refresh()
                delay(1000)
            }
        }
    }

    private fun readd(entry: TorrentEntry) {
        try {
            when {
                entry.filePath != null -> session.download(TorrentInfo(File(entry.filePath)), saveDir)
                entry.magnet != null -> session.download(entry.magnet, saveDir)
            }
        } catch (t: Throwable) {
            BrowserLog.error("torrents", "Could not resume ${entry.name}", t)
        }
    }

    fun add(offer: TorrentOffer): AddResult {
        ensureStarted()
        return when (offer) {
            is TorrentOffer.Magnet -> addMagnet(offer.link)
            is TorrentOffer.File -> addFile(offer.name, offer.bytes)
        }
    }

    fun has(hash: String) = store.find(hash) != null

    private fun addMagnet(link: MagnetLink): AddResult {
        if (has(link.infoHash)) return AddResult.DUPLICATE
        val entry = TorrentEntry(link.infoHash, link.name ?: "Torrent ${link.infoHash.take(8)}", link.uri, null, System.currentTimeMillis(), false)
        store.upsert(entry)
        scope.launch { readd(entry); BrowserLog.info("torrents", "Added a magnet link (${link.trackers} trackers)") }
        return AddResult.ADDED
    }

    private fun addFile(name: String, bytes: ByteArray): AddResult {
        val info = try { TorrentInfo(bytes) } catch (t: Throwable) {
            BrowserLog.warn("torrents", "That file is not a valid .torrent", t)
            return AddResult.INVALID
        }
        val hash = info.infoHash().toHex().lowercase()
        if (has(hash)) return AddResult.DUPLICATE
        val saved = File(context.filesDir, "torrent-files").also { it.mkdirs() }.resolve("$hash.torrent")
        saved.writeBytes(bytes)
        val entry = TorrentEntry(hash, info.name().ifBlank { name }, null, saved.absolutePath, System.currentTimeMillis(), false)
        store.upsert(entry)
        scope.launch { readd(entry); BrowserLog.info("torrents", "Added a .torrent file with ${info.numFiles()} files") }
        return AddResult.ADDED
    }

    fun setPaused(hash: String, paused: Boolean) {
        store.update(hash) { it.copy(paused = paused) }
        scope.launch {
            handle(hash)?.let { if (paused) it.pause() else it.resume() }
            BrowserLog.info("torrents", if (paused) "Paused a torrent" else "Resumed a torrent")
            refresh()
        }
    }

    /** Removes a torrent; [deleteFiles] also deletes what it downloaded. */
    fun remove(hash: String, deleteFiles: Boolean) {
        val entry = store.find(hash)
        store.remove(hash)
        scope.launch {
            try {
                handle(hash)?.let { h ->
                    if (deleteFiles) session.remove(h, com.frostwire.jlibtorrent.swig.session_handle.delete_files) else session.remove(h)
                }
                entry?.filePath?.let { File(it).delete() }
                BrowserLog.info("torrents", "Removed a torrent${if (deleteFiles) " and its files" else ""}")
            } catch (t: Throwable) {
                BrowserLog.error("torrents", "Could not remove a torrent", t)
            }
            refresh()
        }
    }

    fun clearFinished(deleteFiles: Boolean) {
        rows.value.filter { it.finished }.forEach { remove(it.hash, deleteFiles) }
    }

    private fun handle(hash: String): TorrentHandle? =
        try { session.find(Sha1Hash(hash))?.takeIf { it.isValid } } catch (_: Throwable) { null }

    private fun onUnmetered(): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    private var waitingForWifi = false

    private fun enforce() {
        try {
            session.downloadRateLimit(if (limitKb > 0) limitKb * 1024 else 0)
            val hold = wifiOnly && !onUnmetered()
            if (hold && !session.isPaused) session.pause()
            if (!hold && session.isPaused) session.resume()
            waitingForWifi = hold
        } catch (t: Throwable) {
            BrowserLog.error("torrents", "Could not apply the torrent settings", t)
        }
    }

    private fun refresh() {
        if (!started) return
        enforce()
        val out = ArrayList<TorrentRow>()
        for (entry in store.entries) {
            val h = handle(entry.hash)
            val st: TorrentStatus? = h?.status()
            if (h != null && st != null) {
                if (entry.paused && !paused(h)) h.pause()
                val finished = st.isFinished || st.isSeeding
                if (finished && entry.finishedAt == 0L) {
                    store.update(entry.hash) { it.copy(finishedAt = System.currentTimeMillis()) }
                    BrowserLog.info("torrents", "A torrent finished downloading")
                    TorrentNotices.finished(context, entry.name)
                }
                if (finished && !seed && !entry.paused && !paused(h)) h.pause()
                val remaining = (st.totalWanted() - st.totalWantedDone()).coerceAtLeast(0)
                val name = st.name().takeIf { it.isNotBlank() } ?: entry.name
                if (name != entry.name && st.hasMetadata()) store.update(entry.hash) { it.copy(name = name) }
                val isPaused = entry.paused || paused(h)
                out += TorrentRow(
                    entry.hash, name, st.progress(), st.totalWanted(), st.totalWantedDone(), st.downloadRate(), st.uploadRate(),
                    st.numPeers(), st.numSeeds(), entry.paused, finished,
                    when {
                        entry.paused -> "Paused"
                        waitingForWifi -> "Waiting for Wi-Fi"
                        finished && isPaused -> "Done"
                        finished -> "Sharing"
                        !st.hasMetadata() -> "Getting details"
                        st.state() == TorrentStatus.State.CHECKING_FILES -> "Checking files"
                        else -> "Downloading"
                    },
                    TorrentFormat.eta(remaining, st.downloadRate()).takeIf { !finished && !isPaused },
                )
            } else {
                out += TorrentRow(entry.hash, entry.name, 0f, 0, 0, 0, 0, 0, 0, entry.paused, entry.finishedAt > 0, if (entry.paused) "Paused" else "Starting", null)
            }
        }
        _rows.value = out
        TorrentNotices.sync(context, out)
    }

    private fun paused(h: TorrentHandle): Boolean =
        (h.flags().and_(com.frostwire.jlibtorrent.TorrentFlags.PAUSED)).nonZero()

    companion object {
        @Volatile private var instance: TorrentEngine? = null
        fun get(context: Context): TorrentEngine =
            instance ?: synchronized(this) { instance ?: TorrentEngine(context.applicationContext).also { instance = it } }
    }
}
