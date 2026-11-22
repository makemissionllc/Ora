package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.makemission.ora.notice.InAppNotices
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** A phone number or a date found in the selected text, which the card offers an action for. */
private sealed interface Detected {
    data class Phone(val digits: String) : Detected
    data class Date(val millis: Long) : Detected
}

private val phonePattern = Regex("""\+?\(?\d[\d\s().-]{5,}\d""")
private val isoDatePattern = Regex("""\b\d{4}-\d{2}-\d{2}\b""")
private val monthDatePattern = Regex(
    """\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.? \d{1,2},? \d{4}\b|\b\d{1,2} (?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]* \d{4}\b""",
    RegexOption.IGNORE_CASE
)

/** Finds the first phone number or date in [text], if any. Phone numbers need 7 to 15 digits. */
private fun detect(text: String): Detected? {
    phonePattern.findAll(text).firstNotNullOfOrNull { match ->
        val digits = match.value.filter { it.isDigit() || it == '+' }
        if (digits.count { it.isDigit() } in 7..15) Detected.Phone(digits) else null
    }?.let { return it }
    val dateText = isoDatePattern.find(text)?.value ?: monthDatePattern.find(text)?.value ?: return null
    val formats = listOf("yyyy-MM-dd", "MMM d, yyyy", "MMM d yyyy", "d MMM yyyy", "MMMM d, yyyy", "d MMMM yyyy")
    for (format in formats) {
        val parsed = runCatching {
            SimpleDateFormat(format, Locale.ENGLISH).apply { isLenient = false }.parse(dateText)
        }.getOrNull()
        if (parsed != null) {
            val calendar = Calendar.getInstance().apply { time = parsed }
            return Detected.Date(calendar.timeInMillis)
        }
    }
    return null
}

/** The page URL with a text fragment, so opening it highlights [text] (the same link a browser shares with a highlight). */
internal fun highlightUrl(pageUrl: String, text: String): String {
    val base = pageUrl.substringBefore('#')
    return "$base#:~:text=${Uri.encode(text.trim())}"
}

/**
 * Card shown above the bottom bar when text is selected on a page. Offers search, copy, select all and
 * share-with-highlight, plus a call, message or calendar action when the text holds a phone number or date.
 */
@Composable
fun SelectionMenu(
    text: String,
    onSearch: (String) -> Unit,
    onCopy: () -> Unit,
    onSelectAll: () -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /** Set when the selection sits in a text field: offers the Android autofill service's suggestions. */
    onAutofill: (() -> Unit)? = null,
) {
    val colors = browserColors()
    val context = LocalContext.current
    val detected = remember(text) { detect(text) }
    val shown = text.trim().take(60)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(colors.Surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (shown.isEmpty()) "Text field" else "“$shown”",
                color = colors.AddressBarText,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                "Close",
                color = colors.Accent,
                fontSize = 14.sp,
                modifier = Modifier.pressFeedback().clickable(onClick = onDismiss).padding(4.dp)
            )
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onAutofill != null) SelectionChip("Auto-fill", onClick = onAutofill)
            if (shown.isNotEmpty()) {
                SelectionChip("Search", onClick = { onSearch(text.trim()) })
                SelectionChip("Copy", onClick = onCopy)
            }
            SelectionChip("Select all", onClick = onSelectAll)
            if (shown.isNotEmpty()) SelectionChip("Share with highlight", onClick = { onShare(text.trim()) })
            when (val found = detected) {
                is Detected.Phone -> {
                    SelectionChip("Call", onClick = { launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${found.digits}"))) })
                    SelectionChip("Message", onClick = { launch(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${found.digits}"))) })
                }
                is Detected.Date -> SelectionChip("Add to calendar", onClick = {
                    launch(context, Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
                        putExtra(CalendarContract.Events.TITLE, text.trim().take(120))
                        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, found.millis)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, found.millis + 24 * 60 * 60 * 1000L)
                    })
                })
                null -> Unit
            }
        }
    }
}

@Composable
private fun SelectionChip(label: String, onClick: () -> Unit) {
    val colors = browserColors()
    Text(
        label,
        color = colors.AddressBarText,
        fontSize = 14.sp,
        modifier = Modifier
            .pressFeedback().clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(colors.AddressBarBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

/** Shows [SelectionMenu] with the shared overlay motion, growing up from the bottom edge where the selection sits. */
@Composable
fun SelectionMenuOverlay(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(
            OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)
        ),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(
            OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)
        ),
    ) { content() }
}

/** Starts an outside activity; a missing app is logged and explained with a toast instead of crashing. */
private fun launch(context: Context?, intent: Intent) {
    if (context == null) return
    runCatching { context.startActivity(intent) }
        .onFailure {
            BrowserLog.warn("select", "No app for ${intent.action}: ${it.message}")
            InAppNotices.show("No app can open that.")
        }
}
