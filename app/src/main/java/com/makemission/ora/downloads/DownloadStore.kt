package com.makemission.ora.downloads

import com.makemission.ora.log.BrowserLog
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
)

/** Reads and writes the queue of unfinished downloads as one small JSON file in app storage. */
class DownloadStore(private val file: File) {

    fun load(): List<SavedDownload> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            (0 until array.length()).map { i -> fromJson(array.getJSONObject(i)) }
        } catch (e: Exception) {
            BrowserLog.error("download", "Could not read the saved download list: ${e.message}")
            emptyList()
        }
    }

    fun save(downloads: List<SavedDownload>) {
        val array = JSONArray()
        downloads.forEach { array.put(toJson(it)) }
        try {
            // Write beside the file, then swap, so a crash mid-write keeps the last good list.
            val temp = File(file.parentFile, file.name + ".tmp")
            temp.writeText(array.toString())
            if (!temp.renameTo(file)) {
                file.writeText(array.toString())
                temp.delete()
            }
        } catch (e: Exception) {
            BrowserLog.error("download", "Could not save the download list: ${e.message}")
        }
    }

    private fun toJson(d: SavedDownload) = JSONObject().apply {
        put("id", d.id)
        put("fileName", d.fileName)
        put("url", d.url)
        put("mimeType", d.mimeType ?: JSONObject.NULL)
        put("totalBytes", d.totalBytes)
        put("etag", d.etag ?: JSONObject.NULL)
        put("ranged", d.ranged)
        put("startedAt", d.startedAt)
        put("parts", JSONArray().apply {
            d.parts.forEach { p ->
                put(JSONObject().put("start", p.start).put("end", p.end).put("done", p.done).put("finished", p.finished))
            }
        })
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
