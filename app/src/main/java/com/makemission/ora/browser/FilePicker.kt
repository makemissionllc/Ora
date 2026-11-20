package com.makemission.ora.browser

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.makemission.ora.log.BrowserLog
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate

/**
 * Opens Android's file picker when a page asks for a file (an upload button, a file input). GeckoView does
 * nothing on its own for this, so without it tapping Upload seems dead. The picker only lists the kinds of
 * file the page accepts. Choosing nothing, or going back, tells the page that no file was chosen.
 */
object FilePicker {
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var pending: ((List<Uri>) -> Unit)? = null

    /** Called from the activity's onCreate, before it starts. */
    fun register(activity: ComponentActivity) {
        launcher = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val done = pending
            pending = null
            val uris = if (result.resultCode == Activity.RESULT_OK) urisOf(result.data) else emptyList()
            done?.invoke(uris)
        }
    }

    /** Shows the picker. [onDone] gets the chosen files, or an empty list when none was chosen or it could not open. */
    fun pick(multiple: Boolean, mimeTypes: Array<String>?, onDone: (List<Uri>) -> Unit) {
        val start = launcher
        if (start == null || pending != null) {
            BrowserLog.warn("upload", "Could not open the file picker (${if (start == null) "no window" else "already open"})")
            onDone(emptyList())
            return
        }
        val types = mimeTypes?.filter { it.isNotBlank() && it != "*/*" }.orEmpty()
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = if (types.size == 1) types[0] else "*/*"
            if (types.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, types.toTypedArray())
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple)
        }
        pending = onDone
        try {
            start.launch(Intent.createChooser(intent, null))
            BrowserLog.info("upload", "File picker opened (${if (multiple) "several" else "one"} file, ${types.size} type filters)")
        } catch (error: ActivityNotFoundException) {
            pending = null
            BrowserLog.error("upload", "No app can pick files", error)
            onDone(emptyList())
        }
    }

    private fun urisOf(data: Intent?): List<Uri> {
        data ?: return emptyList()
        val clip = data.clipData
        if (clip != null) return List(clip.itemCount) { clip.getItemAt(it).uri }
        return listOfNotNull(data.data)
    }
}

/** Answers a page's file request by showing [FilePicker]. */
open class FilePromptDelegate(private val context: Context) : PromptDelegate {
    override fun onFilePrompt(session: GeckoSession, prompt: PromptDelegate.FilePrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val result = GeckoResult<PromptDelegate.PromptResponse>()
        val multiple = prompt.type == PromptDelegate.FilePrompt.Type.MULTIPLE
        FilePicker.pick(multiple, prompt.mimeTypes) { uris ->
            if (uris.isEmpty()) {
                result.complete(prompt.dismiss())
                return@pick
            }
            // Gecko cannot read a content address from the picker, so each file is copied to the app's cache
            // first (off the main thread, big files take a moment) and the copy is handed over.
            Thread({
                try {
                    val files = uris.map { Uri.fromFile(copyToCache(it)) }
                    val response = if (multiple) prompt.confirm(context, files.toTypedArray()) else prompt.confirm(context, files[0])
                    result.complete(response)
                    BrowserLog.info("upload", "Gave the page ${files.size} chosen file(s)")
                } catch (error: Exception) {
                    BrowserLog.error("upload", "Could not hand the chosen file to the page", error)
                    result.complete(prompt.dismiss())
                }
            }, "ora-upload").start()
        }
        return result
    }

    /** Copies the picked file into the cache under its own name and returns the copy. Copies older than an hour are cleared first. */
    private fun copyToCache(uri: Uri): java.io.File {
        val dir = java.io.File(context.cacheDir, "uploads").apply { mkdirs() }
        // Earlier copies are no longer needed once a page has taken them; keep only the last hour's.
        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 60 * 60 * 1000L }?.forEach { it.deleteRecursively() }
        val name = context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { if (it.moveToFirst()) it.getString(0) else null }
            ?.replace('/', '_')?.ifBlank { null } ?: "upload"
        val folder = java.io.File(dir, System.nanoTime().toString()).apply { mkdirs() }
        val out = java.io.File(folder, name)
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "The file could not be opened" }
            out.outputStream().use { input.copyTo(it) }
        }
        return out
    }
}
