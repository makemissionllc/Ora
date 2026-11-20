package com.makemission.ora.browser

import android.content.Context
import com.makemission.ora.log.BrowserLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.PromptDelegate
import org.mozilla.geckoview.GeckoSession.PromptDelegate.ChoicePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.DateTimePrompt
import org.mozilla.geckoview.GeckoSession.PromptDelegate.PromptResponse

/** Which date or time control a page asked for. */
enum class PickerKind { DATE, MONTH, WEEK, TIME, DATE_TIME }

/** One row of a page's list: a choice, or a group heading when [heading] is set. */
class ChoiceRow(val id: String, val label: String, val selected: Boolean, val disabled: Boolean, val heading: Boolean, val indent: Boolean)

/**
 * A question a page put to the user: an alert, a confirmation, a text box, a list to choose from, or a date or time.
 * Each one is answered once. [cancel] answers it as dismissed, so the page is never left waiting.
 */
sealed class PagePrompt {
    abstract val title: String?
    abstract fun cancel()

    class Message(override val title: String?, val message: String, val asksYesNo: Boolean, val okLabel: String, val onDone: (Boolean) -> Unit) : PagePrompt() {
        override fun cancel() = onDone(false)
    }

    class Text(override val title: String?, val message: String, val initial: String, val onDone: (String?) -> Unit) : PagePrompt() {
        override fun cancel() = onDone(null)
    }

    class Choices(override val title: String?, val rows: List<ChoiceRow>, val multiple: Boolean, val onDone: (List<String>?) -> Unit) : PagePrompt() {
        override fun cancel() = onDone(null)
    }

    class DateTime(
        override val title: String?, val kind: PickerKind, val initial: String?, val min: String?, val max: String?, val stepSeconds: Int?,
        val onDone: (String?) -> Unit,
    ) : PagePrompt() {
        override fun cancel() = onDone(null)
    }
}

/** The page prompts waiting for an answer. One shows at a time, the rest queue behind it in the order the pages asked. */
object PagePrompts {
    private val _queue = MutableStateFlow<List<PagePrompt>>(emptyList())
    val queue: StateFlow<List<PagePrompt>> = _queue.asStateFlow()

    fun ask(prompt: PagePrompt) {
        _queue.update { it + prompt }
    }

    /** Takes [prompt] off the queue without answering it. The caller answers it. */
    fun finish(prompt: PagePrompt) {
        _queue.update { list -> list.filterNot { it === prompt } }
    }
}

/**
 * Shows a page's prompts in Ora's own dialogs, and keeps the file picker from [FilePromptDelegate].
 * Texts typed or chosen are never logged, only the kind of prompt.
 */
class PagePromptDelegate(context: Context) : FilePromptDelegate(context) {
    /** Queues [prompt], and drops it quietly when the page itself withdraws the question (it navigated away). */
    private fun <T : PromptDelegate.BasePrompt> queue(
        base: T,
        kind: String,
        build: (T, (PromptResponse) -> Unit) -> PagePrompt,
    ): GeckoResult<PromptResponse> {
        val result = GeckoResult<PromptResponse>()
        var shown: PagePrompt? = null
        val finish: (PromptResponse) -> Unit = { response ->
            shown?.let { PagePrompts.finish(it) }
            runCatching { result.complete(response) }
        }
        val prompt = build(base) { finish(it) }
        shown = prompt
        base.delegate = object : PromptDelegate.PromptInstanceDelegate {
            override fun onPromptDismiss(prompt: PromptDelegate.BasePrompt) {
                shown?.let { PagePrompts.finish(it) }
                runCatching { result.complete(prompt.dismiss()) }
            }
        }
        BrowserLog.info("prompt", "A page asked: $kind")
        PagePrompts.ask(prompt)
        return result
    }

    override fun onAlertPrompt(session: GeckoSession, prompt: PromptDelegate.AlertPrompt): GeckoResult<PromptResponse> =
        queue(prompt, "alert") { p, done ->
            PagePrompt.Message(p.title, p.message.orEmpty(), asksYesNo = false, okLabel = "OK") { done(p.dismiss()) }
        }

    override fun onButtonPrompt(session: GeckoSession, prompt: PromptDelegate.ButtonPrompt): GeckoResult<PromptResponse> =
        queue(prompt, "confirm") { p, done ->
            PagePrompt.Message(p.title, p.message.orEmpty(), asksYesNo = true, okLabel = "OK") { ok ->
                done(if (ok) p.confirm(PromptDelegate.ButtonPrompt.Type.POSITIVE) else p.confirm(PromptDelegate.ButtonPrompt.Type.NEGATIVE))
            }
        }

    override fun onBeforeUnloadPrompt(session: GeckoSession, prompt: PromptDelegate.BeforeUnloadPrompt): GeckoResult<PromptResponse> =
        queue(prompt, "leave page") { p, done ->
            PagePrompt.Message("Leave this page?", "Changes you made may not be saved.", asksYesNo = true, okLabel = "Leave") { ok ->
                done(p.confirm(if (ok) AllowOrDeny.ALLOW else AllowOrDeny.DENY))
            }
        }

    override fun onRepostConfirmPrompt(session: GeckoSession, prompt: PromptDelegate.RepostConfirmPrompt): GeckoResult<PromptResponse> =
        queue(prompt, "send again") { p, done ->
            PagePrompt.Message("Send the form again?", "This page was made from a form you sent. Reloading it sends the form again.", asksYesNo = true, okLabel = "Send again") { ok ->
                done(p.confirm(if (ok) AllowOrDeny.ALLOW else AllowOrDeny.DENY))
            }
        }

    override fun onTextPrompt(session: GeckoSession, prompt: PromptDelegate.TextPrompt): GeckoResult<PromptResponse> =
        queue(prompt, "text box") { p, done ->
            PagePrompt.Text(p.title, p.message.orEmpty(), p.defaultValue.orEmpty()) { text ->
                done(if (text == null) p.dismiss() else p.confirm(text))
            }
        }

    override fun onChoicePrompt(session: GeckoSession, prompt: ChoicePrompt): GeckoResult<PromptResponse> =
        queue(prompt, "list") { p, done ->
            val rows = ArrayList<ChoiceRow>()
            fun add(list: Array<ChoicePrompt.Choice>?, indent: Boolean) {
                list?.forEach { c ->
                    if (c.separator) return@forEach
                    if (c.items != null) {
                        rows += ChoiceRow(c.id, c.label.orEmpty(), false, true, heading = true, indent = indent)
                        add(c.items, true)
                    } else {
                        rows += ChoiceRow(c.id, c.label.orEmpty(), c.selected, c.disabled, heading = false, indent = indent)
                    }
                }
            }
            add(p.choices, false)
            PagePrompt.Choices(p.title, rows, p.type == ChoicePrompt.Type.MULTIPLE) { ids ->
                done(
                    when {
                        ids == null -> p.dismiss()
                        p.type == ChoicePrompt.Type.MULTIPLE -> p.confirm(ids.toTypedArray())
                        ids.isEmpty() -> p.dismiss()
                        else -> p.confirm(ids[0])
                    }
                )
            }
        }

    override fun onDateTimePrompt(session: GeckoSession, prompt: DateTimePrompt): GeckoResult<PromptResponse> =
        queue(prompt, "date or time") { p, done ->
            val kind = when (p.type) {
                DateTimePrompt.Type.MONTH -> PickerKind.MONTH
                DateTimePrompt.Type.WEEK -> PickerKind.WEEK
                DateTimePrompt.Type.TIME -> PickerKind.TIME
                DateTimePrompt.Type.DATETIME_LOCAL -> PickerKind.DATE_TIME
                else -> PickerKind.DATE
            }
            PagePrompt.DateTime(p.title, kind, p.defaultValue, p.minValue, p.maxValue, p.stepValue?.toDoubleOrNull()?.toInt()) { value ->
                done(if (value == null) p.dismiss() else p.confirm(value))
            }
        }
}
