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
class FilePromptDelegate(private val context: Context) : PromptDelegate {
    override fun onFilePrompt(session: GeckoSession, prompt: PromptDelegate.FilePrompt): GeckoResult<PromptDelegate.PromptResponse>? {
        val result = GeckoResult<PromptDelegate.PromptResponse>()
        val multiple = prompt.type == PromptDelegate.FilePrompt.Type.MULTIPLE
        FilePicker.pick(multiple, prompt.mimeTypes) { uris ->
            try {
                when {
                    uris.isEmpty() -> result.complete(prompt.dismiss())
                    multiple -> result.complete(prompt.confirm(context, uris.toTypedArray()))
                    else -> result.complete(prompt.confirm(context, uris[0]))
                }
                if (uris.isNotEmpty()) BrowserLog.info("upload", "Gave the page ${uris.size} chosen file(s)")
            } catch (error: Exception) {
                BrowserLog.error("upload", "Could not hand the chosen file to the page", error)
                result.complete(prompt.dismiss())
            }
        }
        return result
    }
}
