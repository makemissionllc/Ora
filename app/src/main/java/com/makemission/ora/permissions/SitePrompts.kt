package com.makemission.ora.permissions

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the user chose on a prompt. [NOT_NOW] answers this one request only, and nothing is saved. */
enum class SitePromptChoice { ALLOW, NOT_NOW, BLOCK }

/**
 * A question to the user: [host] wants [permissions]. [answer] is called once, with the choice.
 * [privateTab] says the choice will not be remembered.
 */
class SitePrompt(
    val tabId: Long,
    val host: String,
    val permissions: List<SitePermission>,
    val privateTab: Boolean,
    val answer: (SitePromptChoice) -> Unit,
)

/** Android permissions a page needs that Ora does not hold yet. [onResult] gets each permission's grant state. */
class AndroidPermissionRequest(
    val permissions: Array<String>,
    val onResult: (Map<String, Boolean>) -> Unit,
)

/**
 * The prompts waiting for an answer, and the Android permission request waiting for the system dialog.
 * One prompt shows at a time. The rest queue behind it, in the order the sites asked.
 */
object SitePrompts {
    private val _queue = MutableStateFlow<List<SitePrompt>>(emptyList())
    val queue: StateFlow<List<SitePrompt>> = _queue.asStateFlow()

    private val _android = MutableStateFlow<AndroidPermissionRequest?>(null)
    val androidRequest: StateFlow<AndroidPermissionRequest?> = _android.asStateFlow()

    fun ask(prompt: SitePrompt) {
        _queue.update { it + prompt }
    }

    /** Answers the prompt on screen. Does nothing when no prompt is waiting. */
    fun answerCurrent(choice: SitePromptChoice) {
        val current = _queue.value.firstOrNull() ?: return
        _queue.update { it.drop(1) }
        current.answer(choice)
    }

    /** A tab closed. Its prompts are answered "not now", so the site's request is released and nothing is saved. */
    fun cancelFor(tabId: Long) {
        val mine = _queue.value.filter { it.tabId == tabId }
        if (mine.isEmpty()) return
        _queue.update { list -> list.filterNot { it.tabId == tabId } }
        mine.forEach { it.answer(SitePromptChoice.NOT_NOW) }
    }

    fun askAndroid(request: AndroidPermissionRequest) {
        // Only one system dialog can show at a time. An older request still waiting is refused, so its page is not left hanging.
        _android.value?.onResult(emptyMap())
        _android.value = request
    }

    /** The system permission dialog has answered. */
    fun finishAndroid(results: Map<String, Boolean>) {
        val request = _android.value ?: return
        _android.value = null
        request.onResult(results)
    }
}
