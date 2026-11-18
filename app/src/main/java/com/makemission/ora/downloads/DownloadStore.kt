package com.makemission.ora.downloads

import android.content.Context
import androidx.room.Room
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** What is saved for one unfinished download, enough to resume it after the app restarts. */
data class SavedDownload(
    val id: Long,
    val fileName: String,
    val url: String,
    val mimeType: String?,
    val totalBytes: Long,
    val etag: String?,
    val ranged: Boolean,
    val startedAt: Long,
    val parts: List<Segment>,
    /** The page the file came from, sent as the Referer when the download resumes. */
    val referrer: String? = null,
)

/**
 * Saves the queue of unfinished downloads in a Room database. The first run after an update moves the old
 * queue.json into the database and deletes the file. Writes run one at a time, in the order they were made.
 */
class DownloadStore(context: Context, private val legacyFile: File) {

    private val dao = Room.databaseBuilder(context, DownloadDatabase::class.java, "ora_downloads.db")
        .addMigrations(MIGRATION_1_2)
        .build()
        .downloadDao()
    private val writes = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))

    /** Reads the saved queue. Called once at start-up, so it blocks briefly. */
    fun load(): List<SavedDownload> = runBlocking {
        migrateLegacyFile()
        val parts = dao.parts().groupBy { it.downloadId }
        dao.rows().map { row ->
            SavedDownload(
                id = row.id,
                fileName = row.fileName,
                url = row.url,
                mimeType = row.mimeType,
                totalBytes = row.totalBytes,
                etag = row.etag,
                ranged = row.ranged,
                startedAt = row.startedAt,
                parts = parts[row.id].orEmpty().sortedBy { it.idx }.map { Segment(it.start, it.end, it.done, it.finished) },
                referrer = row.referrer,
            )
        }
    }

    /** Replaces the saved queue. The returned job finishes when the write is on disk. */
    fun save(downloads: List<SavedDownload>): Job {
        val rows = downloads.map { d ->
            DownloadRow(d.id, d.fileName, d.url, d.mimeType, d.totalBytes, d.etag, d.ranged, d.startedAt, d.referrer)
        }
        val parts = downloads.flatMap { d ->
            d.parts.mapIndexed { i, s -> PartRow(d.id, i, s.start, s.end, s.done, s.finished) }
        }
        return writes.launch {
            try {
                dao.replaceAll(rows, parts)
            } catch (e: Exception) {
                BrowserLog.error("download", "Could not save the download list", e)
            }
        }
    }

    private suspend fun migrateLegacyFile() {
        if (!legacyFile.exists()) return
        if (dao.rows().isEmpty()) {
            try {
                val array = JSONArray(legacyFile.readText())
                val saved = (0 until array.length()).map { fromJson(array.getJSONObject(it)) }
                dao.replaceAll(
                    saved.map { d -> DownloadRow(d.id, d.fileName, d.url, d.mimeType, d.totalBytes, d.etag, d.ranged, d.startedAt) },
                    saved.flatMap { d -> d.parts.mapIndexed { i, s -> PartRow(d.id, i, s.start, s.end, s.done, s.finished) } },
                )
                BrowserLog.info("download", "Moved ${saved.size} saved downloads into the database")
            } catch (e: Exception) {
                BrowserLog.error("download", "Could not read the old download list", e)
                return
            }
        }
        legacyFile.delete()
    }

    private fun fromJson(o: JSONObject) = SavedDownload(
        id = o.getLong("id"),
        fileName = o.getString("fileName"),
        url = o.getString("url"),
        mimeType = o.optString("mimeType").takeIf { it.isNotEmpty() && it != "null" },
        totalBytes = o.getLong("totalBytes"),
        etag = o.optString("etag").takeIf { it.isNotEmpty() && it != "null" },
        ranged = o.getBoolean("ranged"),
        startedAt = o.getLong("startedAt"),
        parts = o.getJSONArray("parts").let { arr ->
            (0 until arr.length()).map { i ->
                val p = arr.getJSONObject(i)
                Segment(p.getLong("start"), p.getLong("end"), p.getLong("done"), p.getBoolean("finished"))
            }
        },
    )
}
