package com.makemission.ora.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/** Phone "⋯" menu: page actions as a bottom sheet (design: M-Menu). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onNewTab: () -> Unit,
    onLibrary: () -> Unit,
    desktopSite: Boolean,
    onToggleDesktop: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onExtensions: () -> Unit,
    onDownloads: () -> Unit,
    onTabs: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = browserColors().AddressBarBackground) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MenuAction(Icons.Filled.Share, "Share", onShare)
                MenuAction(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (isBookmarked) "Bookmarked" else "Bookmark",
                    onBookmark
                )
                MenuAction(Icons.Filled.Add, "New tab", onNewTab)
                MenuAction(Icons.Filled.Dashboard, "Tabs", onTabs)
                MenuAction(Icons.Outlined.Tab, "Library", onLibrary)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleDesktop(!desktopSite) }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Desktop site", fontSize = 15.sp, color = browserColors().AddressBarText)
                Switch(checked = desktopSite, onCheckedChange = onToggleDesktop)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDownloads)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Downloads", fontSize = 15.sp, color = browserColors().AddressBarText)
                Text("›", fontSize = 18.sp, color = browserColors().SecondaryText)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExtensions)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Extensions", fontSize = 15.sp, color = browserColors().AddressBarText)
                Text("›", fontSize = 18.sp, color = browserColors().SecondaryText)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSettings)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Settings", fontSize = 15.sp, color = browserColors().AddressBarText)
                Text("›", fontSize = 18.sp, color = browserColors().SecondaryText)
            }
        }
    }
}

@Composable
private fun MenuAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .size(56.dp)
                .background(browserColors().AddressBarBackground.copy(alpha = 1f), RoundedCornerShape(14.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = browserColors().Accent)
        }
        Text(label, fontSize = 12.sp, color = browserColors().IconColor)
    }
}

/** Phone bookmark library: full-screen list with open and remove (design: M-Bookmarks). */
@Composable
fun BookmarksLibrary(
    bookmarks: List<Bookmark>,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Bookmarks", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        if (bookmarks.isEmpty()) {
            Text("No bookmarks yet. Use the bookmark button to save a page.", color = browserColors().SecondaryText, fontSize = 14.sp)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bookmarks, key = { it.url }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(browserColors().AddressBarBackground)
                            .clickable { onOpen(bookmark.url) }
                            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                bookmark.title.ifBlank { displayUrl(bookmark.url) },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = browserColors().AddressBarText
                            )
                            Text(
                                displayUrl(bookmark.url),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp,
                                color = browserColors().SecondaryText
                            )
                        }
                        IconButton(onClick = { onRemove(bookmark.url) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove bookmark", tint = browserColors().SecondaryText)
                        }
                    }
                }
            }
        }
    }
}
