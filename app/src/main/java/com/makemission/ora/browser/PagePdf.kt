package com.makemission.ora.browser

import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import org.mozilla.geckoview.GeckoSession
import kotlin.concurrent.thread

/** Turns a page title into a safe file name ending in .pdf. Falls back to the site, then to "page". */
fun pdfFileName(title: String, url: String): String {
    val host = url.substringAfter("://", "").substringBefore("/").substringBefore("?").substringBefore(":").removePrefix("www.")
    val base = title.ifBlank { host }.ifBlank { "page" }
        .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), " ")
        .replace(Regex("\\s+"), " ").trim().trimEnd('.').take(80).trim()
        .ifBlank { "page" }
    return "$base.pdf"
}

/** Saves the page a session shows as a PDF in Downloads/Ora, named from its title. */
object PagePdf {
    /** [onDone] gets the saved file name, or null when it failed. It runs on a background thread. */
    fun save(context: Context, session: GeckoSession, title: String, url: String, onDone: (String?) -> Unit) {
        val name = pdfFileName(title, url)
        val domain = LogPrivacy.domainOf(url)
        BrowserLog.info("pdf", "Saving $domain as a PDF")
        val result = runCatching { session.saveAsPdf() }.getOrNull()
        if (result == null) {
            BrowserLog.warn("pdf", "This page cannot be saved as a PDF ($domain)")
            onDone(null)
            return
        }
        result.accept({ stream ->
            thread(name = "ora-pdf") {
                val saved = runCatching {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, name)
                        put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Ora")
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("No place to save")
                    try {
                        resolver.openOutputStream(uri)!!.use { out -> (stream ?: error("No PDF data")).use { it.copyTo(out) } }
                        resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
                    } catch (e: Exception) {
                        resolver.delete(uri, null, null)
                        throw e
                    }
                }
                saved.onSuccess { BrowserLog.info("pdf", "Saved a PDF of $domain to Downloads/Ora") }
                    .onFailure { BrowserLog.error("pdf", "Could not write the PDF of $domain", it) }
                onDone(if (saved.isSuccess) name else null)
            }
        }, { error ->
            BrowserLog.error("pdf", "GeckoView could not make a PDF of $domain", error ?: RuntimeException("no error"))
            onDone(null)
        })
    }
}
