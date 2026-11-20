package com.makemission.ora.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/** What a quick switcher row is: an open tab, a saved or visited page, or something Ora can do. */
enum class QuickKind { TAB, BOOKMARK, HISTORY, ACTION }

/** One row. [key] is the tab id, the address, or the action name; it keeps rows stable while the list changes. */
data class QuickItem(val kind: QuickKind, val key: String, val title: String, val subtitle: String = "")

/**
 * Ranks [items] for [query]: a title that starts with the text first, then one that contains it, then an address
 * that contains it. Ties keep the order of [QuickKind] (tabs, bookmarks, history, actions) and then the input order.
 * An empty query lists the open tabs and the actions. A page that is already an open tab is not listed twice.
 * Pure, so it can be tested.
 */
fun quickMatches(query: String, items: List<QuickItem>, limit: Int = 8): List<QuickItem> {
    val q = query.trim()
    if (q.isEmpty()) {
        return items.filter { it.kind == QuickKind.TAB || it.kind == QuickKind.ACTION }.take(limit)
    }
    val openAddresses = items.filter { it.kind == QuickKind.TAB }.map { it.subtitle }.toSet()
    fun score(item: QuickItem): Int = when {
        item.title.startsWith(q, ignoreCase = true) -> 0
        item.title.contains(q, ignoreCase = true) -> 1
        item.subtitle.contains(q, ignoreCase = true) -> 2
        else -> -1
    }
    return items
        .withIndex()
        .filter { (_, item) -> item.kind == QuickKind.TAB || item.kind == QuickKind.ACTION || item.subtitle !in openAddresses }
        .map { (index, item) -> Triple(item, score(item), index) }
        .filter { it.second >= 0 }
        .sortedWith(compareBy({ it.second }, { it.first.kind.ordinal }, { it.third }))
        .take(limit)
        .map { it.first }
}

/**
 * The quick switcher (Ctrl+K, or "Quick switcher" in the menu): one field that finds an open tab, bookmark,
 * history page or action as you type. Up and Down move the highlight, Enter picks it, Escape or Back closes it.
 * Opened by a shortcut or the menu, it uses OraDialog's grow-from-the-last-press motion, or its centred default
 * when a keyboard opened it (no control to grow from; recorded in PROGRESS.md).
 */
@Composable
fun QuickSwitcher(items: List<QuickItem>, onPick: (QuickItem) -> Unit, onDismiss: () -> Unit) {
    val colors = browserColors()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableIntStateOf(0) }
    val results = remember(query, items) { quickMatches(query, items) }
    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(results.size) { if (selected > results.lastIndex) selected = maxOf(0, results.lastIndex) }
    LaunchedEffect(selected) { if (results.isNotEmpty()) listState.animateScrollToItem(selected) }
    fun pick(item: QuickItem?) {
        if (item == null) return
        BrowserLog.info("keyboard", "Quick switcher picked a ${item.kind.name.lowercase()}")
        onPick(item)
    }
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick switcher") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).background(colors.AddressBarBackground, RoundedCornerShape(BrowserTokens.RadiusSmall)).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it; selected = 0 },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = colors.AddressBarText, fontSize = 15.sp),
                        cursorBrush = SolidColor(colors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { pick(results.getOrNull(selected)) }),
                        modifier = Modifier.weight(1f).focusRequester(focus).onPreviewKeyEvent { e ->
                            if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (e.key) {
                                Key.DirectionDown -> { if (results.isNotEmpty()) selected = (selected + 1) % results.size; true }
                                Key.DirectionUp -> { if (results.isNotEmpty()) selected = (selected - 1 + results.size) % results.size; true }
                                Key.Enter, Key.NumPadEnter -> { pick(results.getOrNull(selected)); true }
                                else -> false
                            }
                        },
                        decorationBox = { inner ->
                            if (query.isEmpty()) Text("Search tabs, bookmarks, history, actions", color = colors.SecondaryText, fontSize = 15.sp)
                            inner()
                        },
                    )
                }
                if (results.isEmpty()) {
                    Text("Nothing matches", color = colors.SecondaryText, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                } else LazyColumn(state = listState, modifier = Modifier.heightIn(max = 360.dp)) {
                    itemsIndexed(results, key = { _, item -> "${item.kind}:${item.key}" }) { index, item ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp)
                                .background(if (index == selected) colors.AddressBarBackground else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(BrowserTokens.RadiusSmall))
                                .clickable { pick(item) }.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                when (item.kind) {
                                    QuickKind.TAB -> Icons.Outlined.Tab
                                    QuickKind.BOOKMARK -> Icons.Outlined.BookmarkBorder
                                    QuickKind.HISTORY -> Icons.Outlined.History
                                    QuickKind.ACTION -> Icons.Outlined.Bolt
                                },
                                contentDescription = null, tint = colors.SecondaryText, modifier = Modifier.size(20.dp),
                            )
                            Column(Modifier.weight(1f)) {
                                Text(item.title, color = colors.AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (item.subtitle.isNotEmpty()) Text(item.subtitle, color = colors.SecondaryText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Text("Up and Down to move, Enter to open, Esc to close", color = colors.SecondaryText, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
}
