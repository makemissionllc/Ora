package com.makemission.ora.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import org.mozilla.geckoview.GeckoWebExecutor
import org.mozilla.geckoview.WebRequest
import java.io.File
import kotlin.concurrent.thread

/** An image's bytes and the type the server reported. */
class FetchedImage(val bytes: ByteArray, val mime: String?)

/** The format Save image as writes. Original keeps the bytes as they are. */
enum class ImageFormat(val label: String, val mime: String?, val ext: String?) {
    ORIGINAL("Original", null, null),
    PNG("PNG", "image/png", "png"),
    WEBP("WebP", "image/webp", "webp"),
}

/** File name parts from an image address: the base name without extension, and the extension (from the type, else the name). */
internal fun imageNameParts(url: String, mime: String?): Pair<String, String> {
    val last = url.substringBefore('#').substringBefore('?').substringAfterLast('/').ifBlank { "image" }
    val dot = last.lastIndexOf('.')
    val base = (if (dot > 0) last.substring(0, dot) else last)
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ").trim().take(80).ifBlank { "image" }
    val fromMime = when (mime?.substringBefore(';')?.trim()?.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"
        "image/gif" -> "gif"; "image/svg+xml" -> "svg"; "image/avif" -> "avif"; else -> null
    }
    val fromName = if (dot > 0) last.substring(dot + 1).lowercase().takeIf { it.length in 2..4 } else null
    return base to (fromMime ?: fromName ?: "jpg")
}

/** Fetches, converts, saves and copies images from the long-press menu. Logs name the domain only, never the file. */
object ImageSaver {
    private const val MAX_BYTES = 40 * 1024 * 1024
    private const val COPY_LIFETIME_MS = 10 * 60 * 1000L
    private val main = Handler(Looper.getMainLooper())

    /** Downloads the image through Gecko's own network stack, so the page's cookies apply. [onDone] runs on the main thread. */
    fun fetch(context: Context, url: String, pageUrl: String, isPrivate: Boolean, onDone: (FetchedImage?) -> Unit) {
        val app = context.applicationContext
        thread(name = "ora-image-fetch") {
            val result = runCatching {
                val executor = GeckoWebExecutor(GeckoEngine.runtime(app))
                val request = WebRequest.Builder(url).method("GET").referrer(pageUrl).build()
                val flags = if (isPrivate) GeckoWebExecutor.FETCH_FLAGS_PRIVATE else GeckoWebExecutor.FETCH_FLAGS_NONE
                val response = executor.fetch(request, flags).poll(30_000) ?: error("no response")
                if (response.statusCode !in 200..299) error("status ${response.statusCode}")
                val bytes = response.body!!.use { stream ->
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val n = stream.read(buffer)
                        if (n < 0) break
                        out.write(buffer, 0, n)
                        if (out.size() > MAX_BYTES) error("too large")
                    }
                    out.toByteArray()
                }
                val type = response.headers.entries.firstOrNull { it.key.equals("content-type", true) }?.value
                FetchedImage(bytes, type)
            }
            result.exceptionOrNull()?.let { if (!isPrivate) BrowserLog.warn("page", "Could not fetch an image from ${LogPrivacy.domainOf(url)}", it) }
            main.post { onDone(result.getOrNull()) }
        }
    }

    /** Re-encodes to PNG or WebP. This drops metadata. Returns null when the bytes are not a readable image. */
    fun convert(bytes: ByteArray, format: ImageFormat): ByteArray? {
        if (format == ImageFormat.ORIGINAL) return bytes
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val out = java.io.ByteArrayOutputStream()
        val ok = when (format) {
            ImageFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            else -> bitmap.compress(
                if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSLESS else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP,
                100, out,
            )
        }
        bitmap.recycle()
        return if (ok) out.toByteArray() else null
    }

    /** Saves into Pictures/Ora with the MediaStore, or into the chosen folder ([tree]). Returns where it went, or null. */
    fun save(context: Context, bytes: ByteArray, fileName: String, mime: String, tree: String?): String? = runCatching {
        val resolver = context.contentResolver
        if (tree != null) {
            val treeUri = Uri.parse(tree)
            val parent = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
            val doc = DocumentsContract.createDocument(resolver, parent, mime, fileName) ?: error("no document")
            resolver.openOutputStream(doc)!!.use { it.write(bytes) }
            "the chosen folder"
        } else {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Ora")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("no row")
            try {
                resolver.openOutputStream(uri)!!.use { it.write(bytes) }
                resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }
            "Pictures/Ora"
        }
    }.onFailure { BrowserLog.warn("page", "Could not save an image", it) }.getOrNull()

    /** Puts the image on the clipboard as a content address (FileProvider). The cache file is deleted after 10 minutes. */
    fun copy(context: Context, image: FetchedImage, url: String): Boolean = runCatching {
        val (_, ext) = imageNameParts(url, image.mime)
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        // Anything left by an earlier run (the app was closed before its timer fired) goes now.
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > COPY_LIFETIME_MS }?.forEach { it.delete() }
        val file = File(dir, "copy-${System.currentTimeMillis()}.$ext")
        file.writeBytes(image.bytes)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        val mime = image.mime?.substringBefore(';')?.trim()?.takeIf { it.startsWith("image/") } ?: "image/$ext"
        val clip = ClipData(ClipData.newUri(context.contentResolver, "Image", uri))
        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
        main.postDelayed({ runCatching { file.delete() } }, COPY_LIFETIME_MS)
        mime.isNotEmpty()
    }.onFailure { BrowserLog.warn("page", "Could not copy an image", it) }.getOrDefault(false)
}
