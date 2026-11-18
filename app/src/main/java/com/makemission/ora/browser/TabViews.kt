package com.makemission.ora.browser

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.history.HistoryEntry
import androidx.compose.material3.AlertDialog
import com.makemission.ora.security.AppLock
import com.makemission.ora.security.shownTitle
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/**
 * Tab overview. Phone: two columns with a bottom bar (new tab, count, done).
 * Tablet ([tablet] = true): three columns, a header with the count and Done, and a centred New tab button.
 */
@Composable
fun TabGrid(
    tabs: BrowserTabs,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onNewTab: () -> Unit,
    onDone: () -> Unit,
    tablet: Boolean = false,
) {
    // Search open tabs by title or address, as the sidebar does; the grid shows only the matches.
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim()
    val allTabs = tabs.tabs
    val tabList = allTabs.filter { needle.isEmpty() || it.shownTitle(displayUrl(it.url)).contains(needle, true) || (!(it.isPrivate && AppLock.locked) && it.url.contains(needle, true)) }
    val countText = when {
        needle.isNotEmpty() -> "${tabList.size} of ${allTabs.size} tabs"
        allTabs.size == 1 -> "1 tab"
        else -> "${allTabs.size} tabs"
    }
    // Closing every tab asks first, since it cannot be undone with one tap. The tabs stay in "Reopen closed tab".
    var confirmCloseAll by remember { mutableStateOf(false) }
    if (confirmCloseAll) {
        AlertDialog(
            onDismissRequest = { confirmCloseAll = false },
            title = { Text("Close all ${allTabs.size} tabs?") },
            text = { Text("You can bring back the last 10 from \"Reopen closed tab\" in the menu. Private tabs are not kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCloseAll = false
                    tabs.closeAllToStart()
                    onDone()
                }) { Text("Close all", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmCloseAll = false }) { Text("Cancel") } },
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
    ) {
        if (tablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(BrowserTokens.TabHeaderPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { confirmCloseAll = true }, enabled = allTabs.size > 1) {
                    Text("Close all", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                }
                Text(text = countText, color = browserColors().AddressBarText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                PillButton("Done", onClick = onDone)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            SidebarSearch(query = query, placeholder = "Search ${allTabs.size} open tabs", onQueryChange = { query = it })
        }
        if (tabList.isEmpty() && needle.isNotEmpty()) {
            EmptySidebarText("No tabs match \"$needle\"")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (tablet) 3 else 2),
            modifier = Modifier.weight(1f).padding(BrowserTokens.GridPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing)
        ) {
            gridItems(tabList, key = { it.id }) { tab ->
                TabTile(
                    title = tab.shownTitle(displayUrl(tab.url)),
                    thumbnail = tab.thumbnail,
                    selected = tab.id == tabs.selectedId,
                    onClick = { onSelect(tab.id) },
                    onClose = { onClose(tab.id) }
                )
            }
        }
        if (tablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                PillButton("New tab", onClick = onNewTab, primary = true, icon = Icons.Filled.Add)
            }
            return@Column
        }
        // Bottom bar (design: UI v2 phone tab overview): Close all in Ember, a Chalk New tab pill, then Done.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(browserColors().GridBackground)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextButton(onClick = { confirmCloseAll = true }, enabled = allTabs.size > 1, modifier = Modifier.height(48.dp)) {
                Text("Close all", color = if (allTabs.size > 1) Mineral.Ember else browserColors().DisabledIcon, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(browserColors().Accent)
                        .clickable(onClick = onNewTab),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New tab", tint = browserColors().GridBackground, modifier = Modifier.size(24.dp))
                }
            }
            PillButton("Done", onClick = onDone)
        }
    }
}

/** Pill button from the Mineral design: Chalk fill with dark text when primary, Shale fill with Chalk text otherwise. */
@Composable
private fun PillButton(text: String, onClick: () -> Unit, primary: Boolean = false, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    val fill = if (primary) browserColors().Accent else browserColors().AddressBarBackground
    val content = if (primary) browserColors().GridBackground else browserColors().AddressBarText
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TabTile(
    title: String,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.TabTileHeight)
                .then(
                    if (selected) Modifier.border(BrowserTokens.SelectionWidth, browserColors().Accent, BrowserTokens.TabTileShape)
                    else Modifier
                ),
            shape = BrowserTokens.TabTileShape,
            color = browserColors().AddressBarBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Preview of the page, cropped to the tile. Falls back to the plain card until captured.
                if (thumbnail != null) {
                    Image(
                        bitmap = thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                // Plain box instead of IconButton: IconButton's minimum touch target clipped the badge.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(BrowserTokens.CloseBadgeSize)
                        .background(browserColors().GridBackground.copy(alpha = 0.6f), RoundedCornerShape(50))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close tab",
                        tint = browserColors().AddressBarText,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        // Initial chip and left-aligned title, as in the design.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).background(browserColors().AddressBarBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            }
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = browserColors().AddressBarText
            )
        }
    }
}

/**
 * Tablet sidebar: open tabs, bookmarks and a new-tab button.
 * Tapping a tab selects it; tapping a bookmark opens it in the current tab.
 */
@Composable
fun TabSidebar(
    tabs: BrowserTabs,
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    historyOff: Boolean,
    onNewTab: () -> Unit,
    onOpenBookmark: (String) -> Unit,
    onOpenHistory: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    var panel by remember { mutableStateOf(SidebarPanel.TABS) }
    var query by remember { mutableStateOf("") }
    val needle = query.trim()
    val shownTabs = tabs.tabs.filter {
        needle.isEmpty() || it.shownTitle(displayUrl(it.url)).contains(needle, true) || (!(it.isPrivate && AppLock.locked) && it.url.contains(needle, true))
    }
    val shownBookmarks = bookmarks.filter {
        needle.isEmpty() || it.title.contains(needle, true) || it.url.contains(needle, true)
    }

    Column(
        modifier = Modifier
            .width(BrowserTokens.SidebarWidth)
            .fillMaxSize()
            .background(browserColors().SidebarBackground)
            .padding(BrowserTokens.SidebarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Tabs, Bookmarks and History switch, like the segmented control in the design (D-Sidebar).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(browserColors().GridBackground)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quality of life: switching lists clears the search, so a filter for tabs does not hide bookmarks.
            SegmentLabel("Tabs", selected = panel == SidebarPanel.TABS, modifier = Modifier.weight(1f), pill = true) { panel = SidebarPanel.TABS; query = "" }
            SegmentLabel("Bookmarks", selected = panel == SidebarPanel.BOOKMARKS, modifier = Modifier.weight(1f), pill = true) { panel = SidebarPanel.BOOKMARKS; query = "" }
            SegmentLabel("History", selected = panel == SidebarPanel.HISTORY, modifier = Modifier.weight(1f), pill = true) { panel = SidebarPanel.HISTORY; query = "" }
        }
        // Filters the list below by title or address.
        SidebarSearch(
            query = query,
            pill = true,
            fill = browserColors().GridBackground,
            placeholder = when (panel) {
                SidebarPanel.TABS -> "Search open tabs"
                SidebarPanel.BOOKMARKS -> "Search bookmarks"
                SidebarPanel.HISTORY -> "Search history"
            },
            onQueryChange = { query = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                when (panel) {
                    SidebarPanel.TABS -> "${shownTabs.size} of ${tabs.tabs.size} tabs"
                    SidebarPanel.BOOKMARKS -> "Bookmarks"
                    SidebarPanel.HISTORY -> "History"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = browserColors().SecondaryText
            )
            if (panel == SidebarPanel.TABS) {
                IconButton(onClick = onNewTab) {
                    Icon(Icons.Filled.Add, contentDescription = "New tab", tint = browserColors().Accent)
                }
            }
        }
        if (panel == SidebarPanel.HISTORY) {
            HistoryList(
                entries = history,
                query = query,
                historyOff = historyOff,
                onOpen = onOpenHistory,
                onRemove = onRemoveHistory,
                onClear = onClearHistory,
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (panel == SidebarPanel.BOOKMARKS) {
                    if (shownBookmarks.isEmpty()) {
                        item { EmptySidebarText(if (bookmarks.isEmpty()) "No bookmarks yet. Use the bookmark button to save a page." else "No bookmarks match \"$needle\"") }
                    }
                    items(shownBookmarks, key = { it.url }) { bookmark ->
                        SidebarRow(
                            label = bookmark.title,
                            url = bookmark.url,
                            selected = false,
                            onClick = { onOpenBookmark(bookmark.url) }
                        )
                    }
                } else {
                    if (shownTabs.isEmpty()) {
                        item { EmptySidebarText("No tabs match \"$needle\"") }
                    }
                    items(shownTabs, key = { it.id }) { tab ->
                        SidebarRow(
                            label = tab.shownTitle(displayUrl(tab.url)),
                            url = tab.url,
                            selected = tab.id == tabs.selectedId,
                            onClick = { tabs.select(tab.id) }
                        )
                    }
                }
            }
        }
    }
}

/** Which list the tablet sidebar shows. */
private enum class SidebarPanel { TABS, BOOKMARKS, HISTORY }

@Composable
internal fun SegmentLabel(text: String, selected: Boolean, modifier: Modifier, pill: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(if (pill) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(BrowserTokens.RadiusSmall))
            .background(if (selected) (if (pill) browserColors().Accent else browserColors().SidebarSelected) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = if (pill) 14.sp else 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) (if (pill) browserColors().GridBackground else browserColors().AddressBarText) else browserColors().SecondaryText
        )
    }
}

@Composable
internal fun SidebarSearch(query: String, placeholder: String, pill: Boolean = false, fill: Color = browserColors().AddressBarBackground, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = browserColors().AddressBarText, fontSize = 14.sp),
        cursorBrush = SolidColor(browserColors().Accent),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (pill) 44.dp else BrowserTokens.SidebarSegmentHeight)
            .clip(if (pill) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(BrowserTokens.RadiusSmall))
            .background(fill)
            .padding(horizontal = if (pill) 16.dp else 10.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(placeholder, fontSize = 14.sp, color = browserColors().SecondaryText)
                    }
                    inner()
                }
                // Quality of life: a clear button appears once there is text, so a search can be
                // dropped in one tap instead of backspacing.
                if (query.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = browserColors().SecondaryText,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        }
    )
}

@Composable
private fun EmptySidebarText(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
        fontSize = 13.sp,
        color = browserColors().SecondaryText
    )
}

@Composable
private fun SidebarRow(label: String, url: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.SidebarRowHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) browserColors().SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SiteChip(label, url, size = 22.dp, radius = 6.dp, textSize = 10.sp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = browserColors().AddressBarText
        )
        // The open tab also carries a small Chalk dot, so it reads without colour.
        if (selected) Spacer(Modifier.width(8.dp))
        if (selected) Box(Modifier.size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(browserColors().Accent))
    }
}
