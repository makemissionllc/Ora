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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    onAddToHome: () -> Unit,
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
            onAddToHome = { act(onAddToHome) },
            closedTabTitle = closedTabTitle,
            onReopenClosed = { act(onReopenClosed) },
        )
    }
    if (isTablet) {
        BrowserMenuSidePanel(onDismiss = onDismiss, topInset = topInset, closeRequests = closeRequests, content = items)
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            containerColor = browserColors().Surface,
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            dragHandle = { Box(Modifier.padding(vertical = 10.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(browserColors().ToolbarBorder.copy(alpha = 0.5f))) },
        ) {
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
    onAddToHome: () -> Unit,
    closedTabTitle: String?,
    onReopenClosed: () -> Unit,
) {
        Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Two rows of three Shale tiles (design: UI v2 phone menu). The bookmarked tile takes the one active state.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuAction(Icons.Filled.Share, "Share", onShare, Modifier.weight(1f))
                MenuAction(
                    if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    if (isBookmarked) "Bookmarked" else "Bookmark",
                    onBookmark,
                    Modifier.weight(1f),
                    active = isBookmarked,
                )
                MenuAction(Icons.Filled.Add, "New tab", onNewTab, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MenuAction(Icons.Filled.VisibilityOff, "Private tab", onNewPrivateTab, Modifier.weight(1f))
                MenuAction(Icons.Filled.Dashboard, "Tabs", onTabs, Modifier.weight(1f))
                MenuAction(Icons.Outlined.Tab, "Library", onLibrary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            MenuRow(Icons.Outlined.Search, "Find in page", onFind, chevron = true)
            // Quality of life: one tap copies the page's address, without opening Share first.
            MenuRow(Icons.Outlined.ContentCopy, "Copy page address", onCopyAddress)
            MenuRow(Icons.Outlined.AddToHomeScreen, "Add to home screen", onAddToHome)
            // Tapping with nothing closed explains itself (the screen shows a snackbar) rather than doing nothing.
            MenuRow(
                Icons.Outlined.History, "Reopen closed tab", onReopenClosed,
                subtitle = closedTabTitle?.takeIf { it.isNotBlank() } ?: "Nothing closed yet",
                chevron = true,
            )
            MenuRow(
                Icons.Outlined.DesktopWindows, "Desktop site", { onToggleDesktop(!desktopSite) },
                trailing = { MineralSwitch(desktopSite, "Desktop site", onToggleDesktop) },
            )
            MenuRow(Icons.Outlined.Download, "Downloads", onDownloads, chevron = true)
            MenuRow(Icons.Outlined.Extension, "Extensions", onExtensions, chevron = true)
            MenuRow(Icons.Outlined.Tune, "Settings", onSettings, chevron = true)
        }
}

/** One list row in the menu: Fog icon, Chalk title, optional Fog subtitle, then a chevron or a control. */
@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
            if (subtitle != null) {
                Text(subtitle, fontSize = 13.sp, color = browserColors().SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) trailing() else if (chevron) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(16.dp))
        }
    }
}

/** The Mineral switch: Chalk track with a Basalt knob when on, Shale track with a Fog knob when off. */
@Composable
fun MineralSwitch(checked: Boolean, label: String, onChange: (Boolean) -> Unit) {
    val colors = browserColors()
    // Quality of life: every flip gives a short haptic tick, so a switch feels like it moved.
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (checked) colors.Accent else colors.AddressBarBackground)
            .toggleable(value = checked, role = Role.Switch, onValueChange = {
                haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                onChange(it)
            })
            .semantics { contentDescription = label }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(if (checked) colors.GridBackground else colors.SecondaryText))
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
                    .background(browserColors().Surface)
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
private fun MenuAction(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    val colors = browserColors()
    // One active state everywhere: a Chalk fill with Basalt text.
    val fill = if (active) colors.Accent else colors.AddressBarBackground
    val content = if (active) colors.GridBackground else colors.AddressBarText
    Column(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
            .background(fill)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterVertically),
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(21.dp))
        // One line only, kept inside its cell.
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = content, maxLines = 1, textAlign = TextAlign.Center)
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
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Library", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(browserColors().AddressBarBackground)
                    .clickable(onClick = onDone)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", color = browserColors().AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(browserColors().ToolbarBackground)
                .padding(4.dp)
                .height(38.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switching lists clears the search, as the tablet sidebar does.
            SegmentLabel("Bookmarks", selected = !showHistory, modifier = Modifier.weight(1f), pill = true) { showHistory = false; query = "" }
            SegmentLabel("History", selected = showHistory, modifier = Modifier.weight(1f), pill = true) { showHistory = true; query = "" }
        }
        Spacer(Modifier.height(14.dp))
        SidebarSearch(
            query = query,
            placeholder = if (showHistory) "Search history" else "Search bookmarks",
            pill = true,
            onQueryChange = { query = it }
        )
        Spacer(Modifier.height(14.dp))
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
                            .heightIn(min = 60.dp)
                            .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                            .clickable { onOpen(bookmark.url) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SiteChip(bookmark.title.ifBlank { displayUrl(bookmark.url) }, bookmark.url)
                        Spacer(Modifier.width(12.dp))
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
                        IconButton(onClick = { onRemoveBookmark(bookmark.url) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Remove bookmark", tint = browserColors().SecondaryText, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
