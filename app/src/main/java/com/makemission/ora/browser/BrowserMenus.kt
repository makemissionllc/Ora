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
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.makemission.ora.history.HistoryEntry
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
    onCopyAddress: () -> Unit,
    /** Title of the tab "Reopen closed tab" would bring back, or null when no tab was closed this run. */
    closedTabTitle: String?,
    onReopenClosed: () -> Unit,
    /** Tablet only: the height of the bar across the top (toolbar or floating Compact row), so the side panel starts below it. */
    topInset: Dp = 0.dp,
    /** Tablet only: each increase slides an open panel closed, so the ⋯ button can toggle it. */
    closeRequests: Int = 0,
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
            onCopyAddress = { act(onCopyAddress) },
            closedTabTitle = closedTabTitle,
            onReopenClosed = { act(onReopenClosed) },
        )
    }
    if (isTablet) {
        BrowserMenuSidePanel(onDismiss = onDismiss, topInset = topInset, closeRequests = closeRequests, content = items)
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
    onCopyAddress: () -> Unit,
    closedTabTitle: String?,
    onReopenClosed: () -> Unit,
) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            // Two rows of three, each action sharing its row's width, so labels never run into a neighbour
            // (the six-action row overflowed the 360 dp tablet panel).
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Filled.Share, "Share", onShare, Modifier.weight(1f))
                    MenuAction(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        if (isBookmarked) "Bookmarked" else "Bookmark",
                        onBookmark,
                        Modifier.weight(1f),
                    )
                    MenuAction(Icons.Filled.Add, "New tab", onNewTab, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MenuAction(Icons.Filled.VisibilityOff, "New private tab", onNewPrivateTab, Modifier.weight(1f))
                    MenuAction(Icons.Filled.Dashboard, "Tabs", onTabs, Modifier.weight(1f))
                    MenuAction(Icons.Outlined.Tab, "Library", onLibrary, Modifier.weight(1f))
                }
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
            // Quality of life: one tap copies the page's address, without opening Share first.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCopyAddress)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Copy page address", fontSize = 15.sp, color = browserColors().AddressBarText)
            }
            // Tapping with nothing closed explains itself (the screen shows a snackbar) rather than doing nothing.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onReopenClosed)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Reopen closed tab", fontSize = 15.sp, color = browserColors().AddressBarText)
                    Text(
                        text = closedTabTitle?.takeIf { it.isNotBlank() } ?: "Nothing closed yet",
                        fontSize = 12.sp,
                        color = browserColors().SecondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
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
private fun BrowserMenuSidePanel(
    onDismiss: () -> Unit,
    topInset: Dp,
    closeRequests: Int,
    content: @Composable ((() -> Unit) -> Unit) -> Unit,
) {
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
    // Quality of life: tapping ⋯ again closes the open panel with the same slide-out as Back and the scrim.
    // The count at first composition is the baseline, so a panel opened later does not close at once.
    val baseline = remember { closeRequests }
    LaunchedEffect(closeRequests) {
        if (closeRequests != baseline) dismiss()
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
private fun MenuAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .size(56.dp)
                .background(browserColors().AddressBarBackground.copy(alpha = 1f), RoundedCornerShape(BrowserTokens.RadiusMedium)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = browserColors().Accent)
        }
        // One line only, kept inside its cell: "Library" was breaking into "Libr / ary" in the five-action row.
        Text(
            label,
            fontSize = 12.sp,
            color = browserColors().IconColor,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Phone library: bookmarks and browsing history in one full-screen list, with open, remove and search
 * (design: M-Bookmarks, whose Library shows Bookmarks, Reading and History).
 */
@Composable
fun LibraryScreen(
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    historyOff: Boolean,
    onOpen: (String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDone: () -> Unit,
) {
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim()
    val shownBookmarks = bookmarks.filter {
        needle.isEmpty() || it.title.contains(needle, true) || it.url.contains(needle, true)
    }
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
            Text("Library", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.SidebarSegmentHeight)
                .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                .background(browserColors().AddressBarBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switching lists clears the search, as the tablet sidebar does.
            SegmentLabel("Bookmarks", selected = !showHistory, modifier = Modifier.weight(1f)) { showHistory = false; query = "" }
            SegmentLabel("History", selected = showHistory, modifier = Modifier.weight(1f)) { showHistory = true; query = "" }
        }
        Spacer(Modifier.height(8.dp))
        SidebarSearch(
            query = query,
            placeholder = if (showHistory) "Search history" else "Search bookmarks",
            onQueryChange = { query = it }
        )
        Spacer(Modifier.height(8.dp))
        if (showHistory) {
            HistoryList(
                entries = history,
                query = query,
                historyOff = historyOff,
                onOpen = onOpen,
                onRemove = onRemoveHistory,
                onClear = onClearHistory,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (shownBookmarks.isEmpty()) {
                    item {
                        Text(
                            if (bookmarks.isEmpty()) "No bookmarks yet. Use the bookmark button to save a page." else "No bookmarks match \"$needle\"",
                            color = browserColors().SecondaryText,
                            fontSize = 14.sp
                        )
                    }
                }
                items(shownBookmarks, key = { it.url }) { bookmark ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
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
                        IconButton(onClick = { onRemoveBookmark(bookmark.url) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove bookmark", tint = browserColors().SecondaryText)
                        }
                    }
                }
            }
        }
    }
}
