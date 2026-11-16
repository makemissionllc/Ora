package com.makemission.ora.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.ui.theme.BrowserTokens

/** Phone tab overview: a grid of open tabs with a bottom bar for new tab and done. */
@Composable
fun TabGrid(
    tabs: BrowserTabs,
    onSelect: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onNewTab: () -> Unit,
    onDone: () -> Unit,
) {
    val tabList = tabs.tabs
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BrowserTokens.GridBackground)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).padding(BrowserTokens.GridPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing)
        ) {
            gridItems(tabList, key = { it.id }) { tab ->
                TabTile(
                    title = tab.title.ifBlank { displayUrl(tab.url) },
                    selected = tab.id == tabs.selectedId,
                    onClick = { onSelect(tab.id) },
                    onClose = { onClose(tab.id) }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BrowserTokens.ToolbarBackground)
                .padding(BrowserTokens.ToolbarPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onNewTab, modifier = Modifier.size(BrowserTokens.ToolbarButtonSize)) {
                Icon(Icons.Filled.Add, contentDescription = "New tab", tint = BrowserTokens.Accent)
            }
            Text(
                text = if (tabList.size == 1) "1 tab" else "${tabList.size} tabs",
                color = BrowserTokens.SecondaryText,
                fontSize = 14.sp
            )
            TextButton(onClick = onDone) {
                Text("Done", color = BrowserTokens.Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TabTile(
    title: String,
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
                    if (selected) Modifier.border(BrowserTokens.SelectionWidth, BrowserTokens.Accent, BrowserTokens.TabTileShape)
                    else Modifier
                ),
            shape = BrowserTokens.TabTileShape,
            color = BrowserTokens.AddressBarBackground,
            shadowElevation = BrowserTokens.TabTileElevation
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Plain box instead of IconButton: IconButton's minimum touch target clipped the badge.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(BrowserTokens.CloseBadgeSize)
                        .background(BrowserTokens.CloseBadge, RoundedCornerShape(50))
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
            color = BrowserTokens.AddressBarText
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
    Column(
        modifier = Modifier
            .width(BrowserTokens.SidebarWidth)
            .fillMaxSize()
            .background(BrowserTokens.SidebarBackground)
            .padding(BrowserTokens.SidebarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Tabs", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = BrowserTokens.AddressBarText)
            IconButton(onClick = onNewTab) {
                Icon(Icons.Filled.Add, contentDescription = "New tab", tint = BrowserTokens.Accent)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(tabs.tabs, key = { it.id }) { tab ->
                SidebarRow(
                    label = tab.title.ifBlank { displayUrl(tab.url) },
                    selected = tab.id == tabs.selectedId,
                    onClick = { tabs.select(tab.id) }
                )
            }
            if (bookmarks.isNotEmpty()) {
                item {
                    Text(
                        "Bookmarks",
                        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 12.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrowserTokens.SecondaryText
                    )
                }
                items(bookmarks, key = { it.url }) { bookmark ->
                    SidebarRow(
                        label = bookmark.title,
                        selected = false,
                        onClick = { onOpenBookmark(bookmark.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.SidebarRowHeight)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) BrowserTokens.SidebarSelected else Color.Transparent)
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
            color = BrowserTokens.AddressBarText
        )
    }
}
