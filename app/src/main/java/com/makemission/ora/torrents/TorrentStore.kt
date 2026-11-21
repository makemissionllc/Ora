package com.makemission.ora.torrents

import android.content.Context
import com.makemission.ora.log.BrowserLog
import org.json.JSONArray
import org.json.JSONObject

/** What Ora remembers about one torrent between runs. The downloaded files are the real state; this is how to find them again. */
data class TorrentEntry(
    val hash: String,
    val name: String,
    /** The magnet link, or null when the torrent came from a .torrent file. */
    val magnet: String?,
    /** The saved copy of the .torrent file, or null for a magnet. */
    val filePath: String?,
    val addedAt: Long,
    val paused: Boolean,
    /** When it finished downloading; 0 while it has not. */
    val finishedAt: Long = 0L,
)

/** The list of torrents, in private preferences (`ora_torrents`). Private tabs never add to it (see [TorrentOffers]). */
class TorrentStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("ora_torrents", Context.MODE_PRIVATE)

    @Volatile var entries: List<TorrentEntry> = load()
        private set

    fun find(hash: String) = entries.firstOrNull { it.hash == hash }

    fun upsert(entry: TorrentEntry) = save(entries.filterNot { it.hash == entry.hash } + entry)

    fun update(hash: String, change: (TorrentEntry) -> TorrentEntry) {
        if (find(hash) != null) save(entries.map { if (it.hash == hash) change(it) else it })
    }

    fun remove(hash: String) = save(entries.filterNot { it.hash == hash })

    private fun save(list: List<TorrentEntry>) {
        entries = list
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().put("hash", it.hash).put("name", it.name).put("magnet", it.magnet ?: JSONObject.NULL)
                .put("file", it.filePath ?: JSONObject.NULL).put("at", it.addedAt).put("paused", it.paused).put("done", it.finishedAt))
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }

    private fun load(): List<TorrentEntry> {
        val raw = prefs.getString("entries", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                TorrentEntry(
                    o.getString("hash"), o.optString("name"), if (o.isNull("magnet")) null else o.getString("magnet"),
                    if (o.isNull("file")) null else o.getString("file"), o.optLong("at"), o.optBoolean("paused"), o.optLong("done"),
                )
            }
        }.getOrElse {
            BrowserLog.warn("torrents", "The torrent list could not be read, starting empty", it)
            emptyList()
        }
    }
}
