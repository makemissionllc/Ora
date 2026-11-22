package com.makemission.ora.browser

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.makemission.ora.log.BrowserLog
import java.io.File
import java.security.MessageDigest

/**
 * Keeps a small picture of each open normal tab's page on disk, so the tab grid can show it after a restart
 * and for tabs that were opened in the background. Pictures are keyed by the page address, because tab numbers
 * start again on each launch. Private tabs never reach this store, and a picture is deleted once no open tab
 * shows that address.
 */
object ThumbnailStore {
    private const val MAX_WIDTH = 360
    private const val QUALITY = 80
    private var dir: File? = null

    fun init(context: Context) {
        dir = File(context.cacheDir, "tab-thumbs").apply { mkdirs() }
    }

    private fun fileFor(url: String): File? {
        val d = dir ?: return null
        val hash = MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).take(12).joinToString("") { "%02x".format(it) }
        return File(d, "$hash.jpg")
    }

    /** True when [bitmap] is almost one flat colour, such as a page that has not painted yet. Such a picture would replace a good one. */
    fun looksBlank(bitmap: Bitmap): Boolean {
        val steps = 12
        val seen = HashSet<Int>()
        for (i in 0 until steps) for (j in 0 until steps) {
            val px = bitmap.getPixel((bitmap.width - 1) * i / (steps - 1), (bitmap.height - 1) * j / (steps - 1))
            // Colours are bucketed so slight shading does not count as content.
            seen.add((px shr 20 and 0xF) or ((px shr 12 and 0xF) shl 4) or ((px shr 4 and 0xF) shl 8))
        }
        return seen.size <= 3
    }

    /** Saves a scaled-down copy of [full] for [url]. The caller keeps and frees [full]. */
    fun save(url: String, full: Bitmap) {
        if (url.isBlank() || url.startsWith("about:") || url.startsWith("data:")) return
        val file = fileFor(url) ?: return
        try {
            val scale = MAX_WIDTH.toFloat() / full.width
            val small = if (scale < 1f) Bitmap.createScaledBitmap(full, MAX_WIDTH, maxOf(1, (full.height * scale).toInt()), true) else full
            file.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
            if (small !== full) small.recycle()
        } catch (e: Exception) {
            BrowserLog.warn("tabs", "Could not save a tab picture", e)
        }
    }

    /** Reads the saved picture for [url], or null. Blocking; call off the main thread. */
    fun load(url: String): Bitmap? {
        val file = fileFor(url)?.takeIf { it.exists() } ?: return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            BrowserLog.warn("tabs", "Could not read a tab picture", e)
            null
        }
    }

    /** Deletes every picture whose address is not in [openUrls]. */
    fun retainOnly(openUrls: Collection<String>) {
        val d = dir ?: return
        val keep = openUrls.mapNotNull { fileFor(it)?.name }.toSet()
        d.listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
    }
}
