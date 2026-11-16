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
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.ui.theme.BrowserTokens
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
    val tabList = tabs.tabs
    val countText = if (tabList.size == 1) "1 tab" else "${tabList.size} tabs"
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
                Spacer(modifier = Modifier.width(64.dp))
                Text(text = countText, color = browserColors().AddressBarText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                TextButton(onClick = onDone) {
                    Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (tablet) 3 else 2),
            modifier = Modifier.weight(1f).padding(BrowserTokens.GridPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing)
        ) {
            gridItems(tabList, key = { it.id }) { tab ->
                TabTile(
                    title = tab.title.ifBlank { displayUrl(tab.url) },
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
                TextButton(
                    onClick = onNewTab,
                    modifier = Modifier.background(browserColors().AddressBarBackground, RoundedCornerShape(10.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = browserColors().Accent)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New tab", color = browserColors().AddressBarText, fontSize = 14.sp)
                }
            }
            return@Column
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(browserColors().ToolbarBackground)
                .padding(BrowserTokens.ToolbarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNewTab, modifier = Modifier.size(BrowserTokens.ToolbarButtonSize)) {
                Icon(Icons.Filled.Add, contentDescription = "New tab", tint = browserColors().Accent)
            }
            Text(
                text = if (tabList.size == 1) "1 tab" else "${tabList.size} tabs",
                color = browserColors().SecondaryText,
                fontSize = 14.sp
            )
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
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
            shadowElevation = BrowserTokens.TabTileElevation
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
                        .background(browserColors().CloseBadge, RoundedCornerShape(50))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close tab",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = browserColors().AddressBarText
        )
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
    onNewTab: () -> Unit,
    onOpenBookmark: (String) -> Unit,
) {
    var showBookmarks by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val needle = query.trim()
    val shownTabs = tabs.tabs.filter {
        needle.isEmpty() || it.title.contains(needle, true) || it.url.contains(needle, true)
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
        // Tabs and Bookmarks switch, like the segmented control in the design (D-Sidebar).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.SidebarSegmentHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(browserColors().AddressBarBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentLabel("Tabs", selected = !showBookmarks, modifier = Modifier.weight(1f)) { showBookmarks = false }
            SegmentLabel("Bookmarks", selected = showBookmarks, modifier = Modifier.weight(1f)) { showBookmarks = true }
        }
        // Filters the list below by title or address.
        SidebarSearch(
            query = query,
            placeholder = if (showBookmarks) "Search bookmarks" else "Search open tabs",
            onQueryChange = { query = it }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (showBookmarks) "Bookmarks" else "${shownTabs.size} of ${tabs.tabs.size} tabs",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = browserColors().SecondaryText
            )
            if (!showBookmarks) {
                IconButton(onClick = onNewTab) {
                    Icon(Icons.Filled.Add, contentDescription = "New tab", tint = browserColors().Accent)
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showBookmarks) {
                if (shownBookmarks.isEmpty()) {
                    item { EmptySidebarText(if (bookmarks.isEmpty()) "No bookmarks yet. Use the bookmark button to save a page." else "No bookmarks match \"$needle\"") }
                }
                items(shownBookmarks, key = { it.url }) { bookmark ->
                    SidebarRow(
                        label = bookmark.title,
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
                        label = tab.title.ifBlank { displayUrl(tab.url) },
                        selected = tab.id == tabs.selectedId,
                        onClick = { tabs.select(tab.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentLabel(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) browserColors().SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (selected) browserColors().AddressBarText else browserColors().SecondaryText
        )
    }
}

@Composable
private fun SidebarSearch(query: String, placeholder: String, onQueryChange: (String) -> Unit) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = browserColors().AddressBarText, fontSize = 14.sp),
        cursorBrush = SolidColor(browserColors().Accent),
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.SidebarSegmentHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(browserColors().AddressBarBackground)
            .padding(horizontal = 10.dp),
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
private fun SidebarRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.SidebarRowHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) browserColors().SidebarSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = browserColors().AddressBarText
        )
    }
}
