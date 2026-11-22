package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.layout.Box
import com.makemission.ora.ui.oraRowMotion
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.readinglist.ReadingItem
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/** How long ago a page was saved, in plain words ("Today", "Yesterday", "5 days ago"). */
internal fun savedAgo(addedAt: Long, now: Long = System.currentTimeMillis()): String {
    val days = ((now - addedAt) / 86_400_000L).toInt()
    return when {
        days <= 0 -> "Saved today"
        days == 1 -> "Saved yesterday"
        else -> "Saved $days days ago"
    }
}

/**
 * Pages saved to read later, unread first under their own heading, then read ones. Used by the phone Library
 * and the tablet sidebar. [query] filters by title or address. Opening a page is the caller's job (it also marks
 * the page read). Clear read asks first and says how many pages go; the card grows out of the button
 * that was pressed (see [OraDialog]).
 */
@Composable
internal fun ReadingList(
    items: List<ReadingItem>,
    query: String,
    onOpen: (ReadingItem) -> Unit,
    onToggleRead: (ReadingItem) -> Unit,
    onRemove: (ReadingItem) -> Unit,
    onClearRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    val needle = query.trim()
    val shown = items.filter { needle.isEmpty() || it.title.contains(needle, true) || it.url.contains(needle, true) }
    val unread = shown.filterNot { it.read }
    val read = shown.filter { it.read }
    val readCount = items.count { it.read }
    var confirmClear by remember { mutableStateOf(false) }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        if (shown.isEmpty()) {
            item(key = "empty") {
                Text(
                    if (items.isEmpty()) "Nothing saved yet. Open a page, then choose Add to reading list in the menu."
                    else "Nothing matches \"$needle\"",
                    color = colors.SecondaryText,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
                )
            }
        }
        if (unread.isNotEmpty()) {
            item(key = "h-unread") { ReadingHeading("Unread (${unread.size})") }
            items(unread, key = { it.url }) { Box(oraRowMotion()) { ReadingRow(it, onOpen, onToggleRead, onRemove) } }
        }
        if (read.isNotEmpty()) {
            item(key = "h-read") { ReadingHeading("Read (${read.size})") }
            items(read, key = { it.url }) { Box(oraRowMotion()) { ReadingRow(it, onOpen, onToggleRead, onRemove) } }
        }
        if (readCount > 0) {
            item(key = "clear") {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Pages you have read can go in one tap.", color = colors.SecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { confirmClear = true }, modifier = Modifier.height(44.dp)) {
                        Text("Clear read", color = Mineral.Ember, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
    if (confirmClear) {
        OraDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear read pages?") },
            text = { Text(if (readCount == 1) "1 page you have read is removed from the reading list. Unread pages stay." else "$readCount pages you have read are removed from the reading list. Unread pages stay.") },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = colors.Accent) } },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearRead() }) {
                    Text("Clear", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                }
            },
        )
    }
}

@Composable
private fun ReadingHeading(text: String) {
    Text(
        text,
        color = browserColors().SecondaryText,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun ReadingRow(item: ReadingItem, onOpen: (ReadingItem) -> Unit, onToggleRead: (ReadingItem) -> Unit, onRemove: (ReadingItem) -> Unit) {
    val colors = browserColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
            .clickable { onOpen(item) }
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SiteChip(item.title.ifBlank { displayUrl(item.url) }, item.url)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.title.ifBlank { displayUrl(item.url) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = if (item.read) FontWeight.Normal else FontWeight.Medium,
                color = if (item.read) colors.SecondaryText else colors.AddressBarText,
            )
            Text(
                "${displayUrl(item.url)}, ${savedAgo(item.addedAt)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                color = colors.SecondaryText,
            )
        }
        IconButton(onClick = { onToggleRead(item) }, modifier = Modifier.size(44.dp)) {
            Icon(
                if (item.read) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                contentDescription = if (item.read) "Mark as unread" else "Mark as read",
                tint = if (item.read) colors.Accent else colors.SecondaryText,
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(onClick = { onRemove(item) }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Remove from reading list", tint = colors.SecondaryText, modifier = Modifier.size(16.dp))
        }
    }
}
