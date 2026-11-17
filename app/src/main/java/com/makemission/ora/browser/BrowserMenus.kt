package com.makemission.ora.browser

import com.makemission.ora.ui.DisclosureChevron
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/** "⋯" menu: page actions. Phones show a bottom sheet (design: M-Menu); tablets show a panel from the right. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserMenuSheet(
    isTablet: Boolean,
    isBookmarked: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onLibrary: () -> Unit,
    desktopSite: Boolean,
    onToggleDesktop: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onExtensions: () -> Unit,
    onDownloads: () -> Unit,
    onTabs: () -> Unit,
    onFind: () -> Unit,
    /** Tablet only: the height of the top bar, so the side panel starts below it. */
    topInset: Dp = 0.dp,
) {
    // Each action first closes the menu, then runs. The tablet panel animates out before the action runs.
    val items: @Composable ((() -> Unit) -> Unit) -> Unit = { act ->
        BrowserMenuItems(
            isBookmarked = isBookmarked,
            onShare = { act(onShare) },
            onBookmark = { act(onBookmark) },
            onNewTab = { act(onNewTab) },
            onNewPrivateTab = { act(onNewPrivateTab) },
            onLibrary = { act(onLibrary) },
            desktopSite = desktopSite,
            onToggleDesktop = { enabled -> act { onToggleDesktop(enabled) } },
            onSettings = { act(onSettings) },
            onExtensions = { act(onExtensions) },
            onDownloads = { act(onDownloads) },
            onTabs = { act(onTabs) },
            onFind = { act(onFind) },
        )
    }
    if (isTablet) {
        BrowserMenuSidePanel(onDismiss = onDismiss, topInset = topInset, content = items)
    } else {
        ModalBottomSheet(onDismissRequest = onDismiss, containerColor = browserColors().AddressBarBackground) {
            items { action -> onDismiss(); action() }
        }
    }
}

@Composable
private fun BrowserMenuItems(
    isBookmarked: Boolean,
    onShare: () -> Unit,
    onBookmark: () -> Unit,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onLibrary: () -> Unit,
    desktopSite: Boolean,
    onToggleDesktop: (Boolean) -> Unit,
    onSettings: () -> Unit,
    onExtensions: () -> Unit,
    onDownloads: () -> Unit,
    onTabs: () -> Unit,
    onFind: () -> Unit,
) {
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
                MenuAction(Icons.Filled.VisibilityOff, "New private tab", onNewPrivateTab)
                MenuAction(Icons.Filled.Dashboard, "Tabs", onTabs)
                MenuAction(Icons.Outlined.Tab, "Library", onLibrary)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onFind)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Find in page", fontSize = 15.sp, color = browserColors().AddressBarText)
                DisclosureChevron()
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
                DisclosureChevron()
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
                DisclosureChevron()
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
                DisclosureChevron()
            }
        }
}

/**
 * Tablet menu panel: slides in from the right over the page, with a scrim behind it.
 * Tapping the scrim or pressing Back slides it out first, then closes it.
 */
@Composable
private fun BrowserMenuSidePanel(onDismiss: () -> Unit, topInset: Dp, content: @Composable ((() -> Unit) -> Unit) -> Unit) {
    val visibleState = remember { MutableTransitionState(false) }
    var dismissing by remember { mutableStateOf(false) }
    // An action chosen from the menu waits until the panel has slid out, then runs.
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    // Once the exit has finished, run any chosen action and remove the panel.
    LaunchedEffect(dismissing, visibleState.isIdle) {
        if (dismissing && visibleState.isIdle && !visibleState.currentState) {
            pendingAction?.invoke()
            onDismiss()
        }
    }
    fun dismiss() {
        if (!dismissing) {
            dismissing = true
            visibleState.targetState = false
        }
    }
    val act: (() -> Unit) -> Unit = { action ->
        pendingAction = action
        dismiss()
    }
    BackHandler(enabled = !dismissing) { dismiss() }

    AnimatedVisibility(visibleState = visibleState, enter = fadeIn(OraMotion.enter()), exit = fadeOut(OraMotion.exit())) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(browserColors().Scrim)
                .clickable { dismiss() }
        )
    }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = Modifier.fillMaxSize(),
        enter = slideInHorizontally(OraMotion.enter()) { it } + fadeIn(OraMotion.enter()),
        exit = slideOutHorizontally(OraMotion.exit()) { it } + fadeOut(OraMotion.exit()),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = topInset)
                    .width(BrowserTokens.SidePanelWidth)
                    .shadow(BrowserTokens.FloatingBarElevation, RoundedCornerShape(topStart = BrowserTokens.SidePanelRadius, bottomStart = BrowserTokens.SidePanelRadius))
                    .clip(RoundedCornerShape(topStart = BrowserTokens.SidePanelRadius, bottomStart = BrowserTokens.SidePanelRadius))
                    .background(browserColors().AddressBarBackground)
                    .verticalScroll(rememberScrollState())
                    .then(if (topInset == 0.dp) Modifier.statusBarsPadding() else Modifier)
                    .navigationBarsPadding()
            ) {
                content(act)
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
        // One line only: "Library" was breaking into "Libr / ary" in the five-action row.
        Text(
            label,
            fontSize = 12.sp,
            color = browserColors().IconColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentWidth(unbounded = true)
        )
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
