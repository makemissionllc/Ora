package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.oraRowMotion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import com.makemission.ora.ui.pulse
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import com.makemission.ora.ui.theme.OraMotion
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
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.makemission.ora.ui.OraDialog
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
    /** Opens a private tab. The overview's private space uses it for its New tab button. */
    onNewPrivateTab: () -> Unit = onNewTab,
    /** Locks the private tabs with the app PIN; null when no PIN is set. */
    onLockPrivate: (() -> Unit)? = null,
    /** Saved pages and visits the search can also match. Empty lists keep it to tabs only. */
    bookmarks: List<Bookmark> = emptyList(),
    history: List<HistoryEntry> = emptyList(),
    /** Opens a bookmark or history address in a new tab. */
    onOpenUrl: (String) -> Unit = {},
) {
    val haptics = com.makemission.ora.ui.LocalOraHaptics.current
    // Search open tabs by title or address, as the sidebar does; the grid shows only the matches.
    var query by rememberSaveable { mutableStateOf("") }
    val needle = query.trim()
    // Normal and private tabs live in separate spaces (UI v2). The segmented control switches between them.
    var inPrivate by rememberSaveable { mutableStateOf(tabs.selected?.isPrivate == true) }
    val groupState = remember { GroupDialogState() }
    // Long-pressing a New tab button opens the quick menu (private tab, paste and open, reopen, new tab in a group).
    val quick = rememberQuickMenu()
    QuickNewTabMenu(quick, tabs, inPrivate, onNewTab = { onNewTab() }, onNewPrivateTab = onNewPrivateTab, onDone = onDone)
    // The group whose tabs fill the overview, opened from its stacked card. Null shows groups and loose tabs.
    var openGroupId by rememberSaveable { mutableStateOf<String?>(null) }
    val openGroup = tabs.groups.firstOrNull { it.id == openGroupId }
    val allTabs = tabs.tabs.filter { !it.isPrivate }
    val privateCount = tabs.tabs.count { it.isPrivate }
    val tabList = allTabs.filter { needle.isEmpty() || it.shownTitle(displayUrl(it.url)).contains(needle, true) || (!(it.isPrivate && AppLock.locked) && it.url.contains(needle, true)) }
    val countText = when {
        needle.isNotEmpty() -> "${tabList.size} of ${allTabs.size} tabs"
        allTabs.size == 1 -> "1 tab"
        else -> "${allTabs.size} tabs"
    }
    // Closing every tab asks first, since it cannot be undone with one tap. The tabs stay in "Reopen closed tab".
    var confirmCloseAll by remember { mutableStateOf(false) }
    if (confirmCloseAll) {
        OraDialog(
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
    var confirmClosePrivate by remember { mutableStateOf(false) }
    if (confirmClosePrivate) {
        ClosePrivateDialog(privateCount, { confirmClosePrivate = false }) {
            confirmClosePrivate = false
            tabs.closePrivate()
            onDone()
        }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        com.makemission.ora.ui.theme.LocalBrowserColors provides if (inPrivate) com.makemission.ora.ui.theme.privateBrowserColors(browserColors()) else browserColors()
    ) {
    GroupDialogs(tabs, groupState, onGroupClosed = { openGroupId = null })
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
                TextButton(onClick = { if (inPrivate) confirmClosePrivate = true else confirmCloseAll = true }, enabled = if (inPrivate) privateCount > 0 else allTabs.size > 1) {
                    Text(if (inPrivate) "Close private" else "Close all", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                }
                Text(text = if (inPrivate) "$privateCount private" else countText, color = browserColors().AddressBarText, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                PillButton("Done", onClick = onDone)
            }
        }
        // Segmented control: normal tabs | private tabs (UI v2, phone overview).
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .height(44.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(browserColors().Surface)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentLabel(if (allTabs.size == 1) "1 tab" else "${allTabs.size} tabs", selected = !inPrivate, modifier = Modifier.weight(1f), pill = true) { inPrivate = false }
            SegmentLabel("$privateCount private", selected = inPrivate, modifier = Modifier.weight(1f), pill = true) { inPrivate = true }
        }
        if (inPrivate) {
            PrivateSpacePanel(
                tabs = tabs,
                onSelect = { id -> onSelect(id) },
                onNewPrivateTab = { onNewPrivateTab(); onDone() },
                onLock = onLockPrivate,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
        // A group opens in place of the overview and shrinks back into its card: shared overlay fade and scale.
        AnimatedContent(
            targetState = openGroup?.id,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE))
                    .togetherWith(fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE))
            },
            label = "group",
        ) { shownGroupId ->
        val shownGroup = tabs.groups.firstOrNull { it.id == shownGroupId }
        Column(Modifier.fillMaxSize()) {
        if (shownGroup != null) {
            var menu by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { openGroupId = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to all tabs", tint = browserColors().AddressBarText)
                }
                Box(Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(shownGroup.color.color))
                Spacer(Modifier.width(8.dp))
                Text(shownGroup.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Group options", tint = browserColors().AddressBarText)
                    }
                    GroupMenu(menu, { menu = false }, shownGroup, tabs, groupState, onUngrouped = { openGroupId = null })
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                SidebarSearch(query = query, placeholder = "Search tabs, bookmarks and history", onQueryChange = { query = it })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { groupState.newGroup() }) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New group", color = browserColors().SecondaryText, fontSize = 14.sp)
                }
            }
            if (tabList.isEmpty() && needle.isNotEmpty() && bookmarks.none { it.title.contains(needle, true) || it.url.contains(needle, true) } && history.none { it.title.contains(needle, true) || it.url.contains(needle, true) }) {
                EmptySidebarText("Nothing matches \"$needle\"")
            }
        }
        // Inside a group: its tabs. With a search: every match, flat. Otherwise: group cards, then loose tabs.
        val gridTabs = when {
            shownGroup != null -> tabs.tabsIn(shownGroup.id)
            needle.isNotEmpty() -> tabList
            else -> tabList.filter { t -> (t.groupId == null || tabs.groups.none { it.id == t.groupId }) && !tabs.isInactive(t) }
        }
        // Loose tabs not opened in 14 days wait in a collapsed row; a search still finds them.
        val inactiveTabs = if (shownGroup == null && needle.isEmpty()) tabList.filter { t -> (t.groupId == null || tabs.groups.none { it.id == t.groupId }) && tabs.isInactive(t) } else emptyList()
        var inactiveOpen by remember { mutableStateOf(false) }
        val duplicateCount = tabs.duplicateTabs().size
        var confirmDuplicates by remember { mutableStateOf(false) }
        if (confirmDuplicates) CloseDuplicatesDialog(duplicateCount, { confirmDuplicates = false }) {
            confirmDuplicates = false
            val closed = tabs.closeDuplicates()
            com.makemission.ora.notice.InAppNotices.show(if (closed == 1) "Closed 1 duplicate tab" else "Closed $closed duplicate tabs")
        }
        if (shownGroup != null && gridTabs.isEmpty()) {
            EmptySidebarText("No tabs in this group. Long-press a tab and choose Add to group.")
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (tablet) 3 else 2),
            modifier = Modifier.weight(1f).padding(BrowserTokens.GridPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing),
            horizontalArrangement = Arrangement.spacedBy(BrowserTokens.GridSpacing)
        ) {
            if (shownGroup == null && needle.isEmpty() && !inPrivate && duplicateCount > 0) {
                item(key = "duplicates", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.animateItem(fadeInSpec = OraMotion.standard(), placementSpec = OraMotion.standard(), fadeOutSpec = OraMotion.leave())) { DuplicatesRow(duplicateCount) { confirmDuplicates = true } }
                }
            }
            if (shownGroup == null && needle.isEmpty()) {
                gridItems(tabs.groups.toList(), key = { "g" + it.id }) { group ->
                    GroupCard(group, tabs.tabsIn(group.id).size, onClick = { openGroupId = group.id }, onDropTab = { tabs.moveToGroup(it, group.id) })
                }
            }
            gridItems(gridTabs, key = { it.id }) { tab ->
                // Closing a card fades it out and its neighbours slide together to close the gap.
                // The cards rise into place one after another when the overview opens, the first ones first.
                Box(Modifier.animateItem(fadeInSpec = OraMotion.standard(), placementSpec = OraMotion.standard(), fadeOutSpec = OraMotion.leave()).arriveInTurn(gridTabs.indexOf(tab))) {
                TabTile(
                    title = tab.shownTitle(displayUrl(tab.url)),
                    thumbnail = rememberTabPicture(tab),
                    host = displayUrl(tab.url),
                    selected = tab.id == tabs.selectedId,
                    onClick = { haptics.play(com.makemission.ora.ui.Feel.SELECT); onSelect(tab.id) },
                    onClose = { haptics.play(com.makemission.ora.ui.Feel.CLOSE); onClose(tab.id) },
                    onLongClick = { groupState.addTo = tab },
                    groupColor = tabs.groups.firstOrNull { it.id == tab.groupId }?.color?.color,
                    dragTabId = tab.id.takeIf { !tab.isPrivate },
                )
                }
            }
            // One search field: bookmarks and history that match are listed under the tabs (up to five each).
            if (shownGroup == null && needle.isNotEmpty() && !inPrivate) {
                val foundBookmarks = bookmarks.filter { it.title.contains(needle, true) || it.url.contains(needle, true) }.take(5)
                val foundHistory = history.filter { it.title.contains(needle, true) || it.url.contains(needle, true) }.take(5)
                if (foundBookmarks.isNotEmpty()) {
                    item(key = "found_bookmarks", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { FoundLabel("In bookmarks") }
                    gridItems(foundBookmarks, key = { "fb_" + it.url }, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { b -> SidebarRow(b.title.ifBlank { displayUrl(b.url) }, b.url, false, onClick = { onOpenUrl(b.url) }) }
                }
                if (foundHistory.isNotEmpty()) {
                    item(key = "found_history", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { FoundLabel("In history") }
                    gridItems(foundHistory, key = { "fh_" + it.url + it.visitedAt }, span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) { h -> SidebarRow(h.title.ifBlank { displayUrl(h.url) }, h.url, false, onClick = { onOpenUrl(h.url) }) }
                }
            }
            if (inactiveTabs.isNotEmpty()) {
                item(key = "inactive_header", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    InactiveHeader(inactiveTabs.size, inactiveOpen) { inactiveOpen = !inactiveOpen }
                }
                if (inactiveOpen) gridItems(inactiveTabs, key = { it.id }) { tab ->
                    TabTile(
                        title = tab.shownTitle(displayUrl(tab.url)),
                        thumbnail = rememberTabPicture(tab),
                    host = displayUrl(tab.url),
                        selected = false,
                        onClick = { haptics.play(com.makemission.ora.ui.Feel.SELECT); onSelect(tab.id) },
                        onClose = { haptics.play(com.makemission.ora.ui.Feel.CLOSE); onClose(tab.id) },
                        onLongClick = { groupState.addTo = tab },
                        groupColor = null,
                        dragTabId = tab.id,
                    )
                }
            }
        }
        }
        }
        }
        if (tablet) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                PillButton(
                    if (inPrivate) "New private tab" else "New tab",
                    onClick = { if (inPrivate) { onNewPrivateTab(); onDone() } else onNewTab() },
                    primary = true,
                    icon = Icons.Filled.Add,
                    modifier = Modifier.quickAnchor(quick),
                    onLongClick = { quick.open = true },
                )
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
            val closeEnabled = if (inPrivate) privateCount > 0 else allTabs.size > 1
            TextButton(onClick = { if (inPrivate) confirmClosePrivate = true else confirmCloseAll = true }, enabled = closeEnabled, modifier = Modifier.height(48.dp)) {
                Text(if (inPrivate) "Close private" else "Close all", color = if (closeEnabled) Mineral.Ember else browserColors().DisabledIcon, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(width = 64.dp, height = 52.dp)
                        .pressFeedback().clip(RoundedCornerShape(26.dp))
                        .background(browserColors().Accent)
                        .quickAnchor(quick)
                        .combinedClickable(onLongClick = { quick.open = true }) { if (inPrivate) { onNewPrivateTab(); onDone() } else onNewTab() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "New tab", tint = browserColors().GridBackground, modifier = Modifier.size(24.dp))
                }
            }
            PillButton("Done", onClick = onDone)
        }
    }
    }
}

/**
 * Closing every private tab asks first: nothing about a private tab is kept, so it cannot be brought back. The card is
 * the shared OraDialog, raised by the Close private button (its previous component).
 */
@Composable
internal fun ClosePrivateDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Close this private tab?" else "Close all $count private tabs?") },
        text = { Text("Private tabs are not saved anywhere, so they cannot be brought back.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(if (count == 1) "Close tab" else "Close all", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Pill button from the Mineral design: Chalk fill with dark text when primary, Shale fill with Chalk text otherwise. */
@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun PillButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    val fill = if (primary) browserColors().Accent else browserColors().AddressBarBackground
    val content = if (primary) browserColors().GridBackground else browserColors().AddressBarText
    Row(
        modifier = modifier
            .height(48.dp)
            .pressFeedback().clip(RoundedCornerShape(24.dp))
            .background(fill)
            .combinedClickable(onLongClick = onLongClick, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Rises 14 dp and fades in on the stagger timing, [order] places after the first. Later rows (past eight) just appear. */
@Composable
private fun Modifier.arriveInTurn(order: Int): Modifier {
    var shown by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    val progress by androidx.compose.animation.core.animateFloatAsState(
        if (shown || order >= 8) 1f else 0f,
        OraMotion.stagger(order),
        label = "tile arrive",
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * 14.dp.toPx()
        val scale = 0.96f + 0.04f * progress
        scaleX = scale
        scaleY = scale
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TabTile(
    title: String,
    thumbnail: ImageBitmap?,
    selected: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    /** Long press: opens "Add to group". */
    onLongClick: (() -> Unit)? = null,
    /** Colour of the tab's group, drawn as a dot before the title. The group's name is in its card. */
    groupColor: Color? = null,
    /** Set to let the tile be dragged onto a group. Long press without moving still calls [onLongClick]. */
    dragTabId: Long? = null,
    /** The site's address, shown on the card when no picture of the page exists yet, so the tab is still recognisable. */
    host: String = "",
) {
    Column(
        modifier = if (dragTabId != null) Modifier.tabDragSource(dragTabId, browserColors().Accent, onLongClick).pressFeedback().clickable(onClick = onClick)
        else Modifier.pressFeedback().combinedClickable(onClick = onClick, onLongClick = onLongClick),
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
                } else {
                    // No picture yet: the card shows the site's letter and address, so the tab can still be told apart.
                    Column(Modifier.fillMaxSize().background(browserColors().GridBackground.copy(alpha = 0.35f)).padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
                        if (host.isNotBlank()) Text(host, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = browserColors().SecondaryText)
                    }
                }
                // Plain box instead of IconButton: IconButton's minimum touch target clipped the badge.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(BrowserTokens.CloseBadgeSize)
                        .pressFeedback().background(browserColors().GridBackground.copy(alpha = 0.6f), RoundedCornerShape(50))
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
            if (groupColor != null) Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(groupColor))
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
 * A tab group in the overview: a card with two layers peeking out behind it, so it reads as a stack.
 * The colour dot sits next to the name; tapping opens the group's tabs.
 */
@Composable
private fun GroupCard(group: TabGroup, count: Int, onClick: () -> Unit, onDropTab: (Long) -> Unit) {
    val colors = browserColors()
    var hovering by remember { mutableStateOf(false) }
    Column(modifier = Modifier.tabDropTarget({ hovering = it }, onDropTab).then(if (hovering) Modifier.pressFeedback().border(BrowserTokens.SelectionWidth, colors.Accent, BrowserTokens.TabTileShape) else Modifier).clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(BrowserTokens.TabTileHeight)) {
            Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal = 18.dp).fillMaxWidth().height(BrowserTokens.TabTileHeight - 4.dp), shape = BrowserTokens.TabTileShape, color = colors.SidebarSelected.copy(alpha = 0.5f)) {}
            Surface(Modifier.align(Alignment.BottomCenter).padding(horizontal = 9.dp).fillMaxWidth().height(BrowserTokens.TabTileHeight - 10.dp), shape = BrowserTokens.TabTileShape, color = colors.SidebarSelected) {}
            Surface(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(BrowserTokens.TabTileHeight - 18.dp), shape = BrowserTokens.TabTileShape, color = colors.AddressBarBackground) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.size(12.dp).clip(androidx.compose.foundation.shape.CircleShape).background(group.color.color))
                        Text(group.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
                    }
                    Text(if (count == 1) "1 tab" else "$count tabs", fontSize = 13.sp, color = colors.SecondaryText)
                }
            }
        }
        Text("Group", fontSize = 12.sp, color = colors.SecondaryText, modifier = Modifier.padding(start = 2.dp))
    }
}

/** A group's header in the tablet sidebar: chevron, colour dot, name, count and a ⋯ menu. Tapping folds the group. */
@Composable
private fun SidebarGroupHeader(group: TabGroup, count: Int, tabs: BrowserTabs, state: GroupDialogState) {
    val colors = browserColors()
    var menu by remember { mutableStateOf(false) }
    var hovering by remember { mutableStateOf(false) }
    val turn by animateFloatAsState(if (group.collapsed) 0f else 90f, OraMotion.enter(), label = "chevron")
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).pressFeedback().clip(RoundedCornerShape(12.dp)).background(if (hovering) colors.SidebarSelected else Color.Transparent).tabDropTarget({ hovering = it }) { tabs.moveToGroup(it, group.id) }.clickable { tabs.toggleCollapsed(group.id) }.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (group.collapsed) "Expand ${group.name}" else "Collapse ${group.name}", tint = colors.SecondaryText, modifier = Modifier.size(20.dp).rotate(turn))
        Spacer(Modifier.width(6.dp))
        Box(Modifier.size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(group.color.color))
        Spacer(Modifier.width(8.dp))
        Text(group.name, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
        Text("$count", fontSize = 13.sp, color = colors.SecondaryText)
        Box {
            IconButton(onClick = { menu = true }, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Group options", tint = colors.SecondaryText, modifier = Modifier.size(18.dp))
            }
            GroupMenu(menu, { menu = false }, group, tabs, state)
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
    reading: com.makemission.ora.readinglist.ReadingListRepository,
    onOpenReading: (com.makemission.ora.readinglist.ReadingItem) -> Unit,
    onNewTab: () -> Unit,
    onOpenBookmark: (String) -> Unit,
    onOpenHistory: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onNewPrivateTab: () -> Unit = onNewTab,
    onLockPrivate: (() -> Unit)? = null,
) {
    // The private space opens inside the sidebar from the pinned bottom row, in the Obsidian colours.
    var inPrivate by remember { mutableStateOf(tabs.selected?.isPrivate == true) }
    val groupState = remember { GroupDialogState() }
    val quick = rememberQuickMenu()
    QuickNewTabMenu(quick, tabs, inPrivate, onNewTab = onNewTab, onNewPrivateTab = onNewPrivateTab)
    var panel by remember { mutableStateOf(SidebarPanel.TABS) }
    val pillPos = remember { androidx.compose.animation.core.Animatable(0f) }
    val sidebarScope = androidx.compose.runtime.rememberCoroutineScope()
    val reducedMotion = com.makemission.ora.ui.theme.LocalReducedMotion.current
    var query by remember { mutableStateOf("") }
    val selectPanel: (SidebarPanel) -> Unit = { target ->
        panel = target
        query = ""
        sidebarScope.launch { if (reducedMotion) pillPos.snapTo(target.ordinal.toFloat()) else pillPos.animateTo(target.ordinal.toFloat(), OraMotion.standard()) }
    }
    // The reading list opens inside the sidebar from a pinned row, so it never crowds the Tabs / Bookmarks / History switch.
    var inReading by remember { mutableStateOf(false) }
    var inactiveOpen by remember { mutableStateOf(false) }
    val readingItems by reading.items.collectAsState()
    val unreadCount = readingItems.count { !it.read }
    val needle = query.trim()
    val shownTabs = tabs.tabs.filter { !it.isPrivate }.filter {
        needle.isEmpty() || it.shownTitle(displayUrl(it.url)).contains(needle, true) || (!(it.isPrivate && AppLock.locked) && it.url.contains(needle, true))
    }
    val shownBookmarks = bookmarks.filter {
        needle.isEmpty() || it.title.contains(needle, true) || it.url.contains(needle, true)
    }

    var confirmClosePrivate by remember { mutableStateOf(false) }
    val sidebarDuplicates = tabs.duplicateTabs().size
    var sidebarConfirmDuplicates by remember { mutableStateOf(false) }
    if (sidebarConfirmDuplicates) CloseDuplicatesDialog(sidebarDuplicates, { sidebarConfirmDuplicates = false }) {
        sidebarConfirmDuplicates = false
        val closed = tabs.closeDuplicates()
        com.makemission.ora.notice.InAppNotices.show(if (closed == 1) "Closed 1 duplicate tab" else "Closed $closed duplicate tabs")
    }
    androidx.compose.runtime.CompositionLocalProvider(
        com.makemission.ora.ui.theme.LocalBrowserColors provides if (inPrivate) com.makemission.ora.ui.theme.privateBrowserColors(browserColors()) else browserColors()
    ) {
    val privateCount = tabs.tabs.count { it.isPrivate }
    if (confirmClosePrivate) {
        ClosePrivateDialog(privateCount, { confirmClosePrivate = false }) {
            confirmClosePrivate = false
            tabs.closePrivate()
            inPrivate = false
        }
    }
    GroupDialogs(tabs, groupState)
    Column(
        modifier = Modifier
            .width(BrowserTokens.SidebarWidth)
            .fillMaxSize()
            .background(browserColors().SidebarBackground)
            .padding(BrowserTokens.SidebarPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (inPrivate) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp).pressFeedback().clip(androidx.compose.foundation.shape.CircleShape).clickable { inPrivate = false }.padding(horizontal = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to tabs", color = browserColors().SecondaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            PrivateSpacePanel(tabs = tabs, onSelect = { tabs.select(it) }, onNewPrivateTab = onNewPrivateTab, onLock = onLockPrivate, modifier = Modifier.weight(1f))
            if (privateCount > 0) Box(
                modifier = Modifier.fillMaxWidth().height(44.dp).pressFeedback().clip(androidx.compose.foundation.shape.CircleShape).clickable { confirmClosePrivate = true },
                contentAlignment = Alignment.Center,
            ) { Text("Close private", color = Mineral.Ember, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            return@Column
        }
        if (inReading) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp).pressFeedback().clip(androidx.compose.foundation.shape.CircleShape).clickable { inReading = false; query = "" }.padding(horizontal = 8.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to tabs", color = browserColors().SecondaryText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            SidebarSearch(query = query, pill = true, fill = browserColors().GridBackground, placeholder = "Search reading list", onQueryChange = { query = it })
            Text(
                if (unreadCount > 0) "Reading list, $unreadCount unread" else "Reading list",
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText,
            )
            ReadingList(
                items = readingItems,
                query = query,
                onOpen = onOpenReading,
                onToggleRead = { reading.setRead(it.url, !it.read) },
                onRemove = { reading.remove(it.url) },
                onClearRead = { reading.clearRead() },
                modifier = Modifier.weight(1f),
            )
            return@Column
        }
        // Tabs, Bookmarks and History switch, like the segmented control in the design (D-Sidebar).
        // Tabs, Bookmarks and History: one Chalk pill slides under the labels, and follows the finger when the list is swiped.
        // Quality of life: switching lists clears the search, so a filter for tabs does not hide bookmarks.
        SlidingSegments(
            labels = listOf("Tabs", "Bookmarks", "History"),
            position = pillPos.value,
            onSelect = { i -> selectPanel(SidebarPanel.values()[i]) },
        )
        // Filters the list below by title or address.
        SidebarSearch(
            query = query,
            pill = true,
            fill = browserColors().GridBackground,
            placeholder = when (panel) {
                SidebarPanel.TABS -> "Search tabs, bookmarks and history"
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
            // The + is on every panel (Tabs, Bookmarks, History), so a new tab is one tap away wherever the sidebar is.
            // Long-press opens the quick menu. In the private space it opens a private tab.
            Box(
                Modifier
                    .size(48.dp)
                    .pressFeedback().clip(androidx.compose.foundation.shape.CircleShape)
                    .quickAnchor(quick)
                    .combinedClickable(onLongClick = { quick.open = true }) { if (inPrivate) onNewPrivateTab() else onNewTab() }
                    .semantics { contentDescription = if (inPrivate) "New private tab" else "New tab" },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = browserColors().Accent)
            }
        }
        // The list moves 24 dp in the direction of the change and fades through. A sideways swipe on it changes section,
        // and the pill above follows the finger.
        var listWidthPx by remember { mutableStateOf(1f) }
        var swipeStart by remember { mutableStateOf(0f) }
        var swipeDx by remember { mutableStateOf(0f) }
        val slidePx = with(androidx.compose.ui.platform.LocalDensity.current) { 24.dp.roundToPx() }
        AnimatedContent(
            targetState = panel,
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { listWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { swipeStart = pillPos.value; swipeDx = 0f },
                        onHorizontalDrag = { _, dx ->
                            swipeDx += dx
                            sidebarScope.launch { pillPos.snapTo((swipeStart - swipeDx / listWidthPx * 1.5f).coerceIn(0f, 2f)) }
                        },
                        onDragEnd = { selectPanel(SidebarPanel.values()[pillPos.value.roundToInt().coerceIn(0, 2)]) },
                        onDragCancel = { selectPanel(panel) },
                    )
                },
            transitionSpec = {
                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                if (reducedMotion) fadeIn(OraMotion.reduced()) togetherWith fadeOut(OraMotion.reduced())
                else (slideInHorizontally(OraMotion.standard()) { dir * slidePx } + fadeIn(OraMotion.standard())) togetherWith
                    (slideOutHorizontally(OraMotion.leave()) { -dir * slidePx } + fadeOut(OraMotion.leave()))
            },
            label = "sidebar list",
        ) { shownPanel ->
        val panel = shownPanel
        if (panel == SidebarPanel.HISTORY) {
            HistoryList(
                entries = history,
                query = query,
                historyOff = historyOff,
                onOpen = onOpenHistory,
                onRemove = onRemoveHistory,
                onClear = onClearHistory,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (panel == SidebarPanel.BOOKMARKS) {
                    if (shownBookmarks.isEmpty()) {
                        item { EmptySidebarText(if (bookmarks.isEmpty()) "No bookmarks yet. Use the bookmark button to save a page." else "No bookmarks match \"$needle\"") }
                    }
                    items(shownBookmarks, key = { it.url }) { bookmark ->
                        Box(oraRowMotion()) { SidebarRow(
                            label = bookmark.title,
                            url = bookmark.url,
                            selected = false,
                            onClick = { onOpenBookmark(bookmark.url) }
                        ) }
                    }
                } else {
                    if (shownTabs.isEmpty() && (needle.isEmpty() || (shownBookmarks.isEmpty() && history.none { it.title.contains(needle, true) || it.url.contains(needle, true) }))) {
                        item { EmptySidebarText("Nothing matches \"$needle\"") }
                    }
                    if (needle.isEmpty()) {
                        // Groups fold with a chevron; tabs outside any group sit under "Not in a group".
                        tabs.groups.toList().forEach { group ->
                            val members = shownTabs.filter { it.groupId == group.id }
                            item(key = "gh_" + group.id) { SidebarGroupHeader(group, members.size, tabs, groupState) }
                            item(key = "gb_" + group.id) {
                                AnimatedVisibility(
                                    visible = !group.collapsed,
                                    enter = fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()),
                                    exit = fadeOut(OraMotion.exit()) + shrinkVertically(OraMotion.exit()),
                                ) {
                                    Column(Modifier.padding(start = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        if (members.isEmpty()) EmptySidebarText("Empty. Long-press a tab to add it.")
                                        members.forEach { tab ->
                                            SidebarRow(tab.shownTitle(displayUrl(tab.url)), tab.url, tab.id == tabs.selectedId, onClick = { tabs.select(tab.id) }, onLongClick = { groupState.addTo = tab }, dragTabId = tab.id)
                                        }
                                    }
                                }
                            }
                        }
                        val loose = shownTabs.filter { t -> (t.groupId == null || tabs.groups.none { it.id == t.groupId }) && (!tabs.isInactive(t) || needle.isNotEmpty()) }
                        if (needle.isEmpty() && sidebarDuplicates > 0) item(key = "duplicates") { Box(Modifier.animateItem()) { DuplicatesRow(sidebarDuplicates) { sidebarConfirmDuplicates = true } } }
                        if (tabs.groups.isNotEmpty()) item(key = "loose") {
                            var hovering by remember { mutableStateOf(false) }
                            Text("Not in a group", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (hovering) browserColors().SidebarSelected else Color.Transparent).tabDropTarget({ hovering = it }) { tabs.moveToGroup(it, null) }.padding(start = 12.dp, top = 10.dp, bottom = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText)
                        }
                        items(loose, key = { it.id }) { tab ->
                            SidebarRow(tab.shownTitle(displayUrl(tab.url)), tab.url, tab.id == tabs.selectedId, onClick = { tabs.select(tab.id) }, onLongClick = { groupState.addTo = tab }, dragTabId = tab.id)
                        }
                        inactiveSection(
                            shownTabs.filter { t -> (t.groupId == null || tabs.groups.none { it.id == t.groupId }) && tabs.isInactive(t) && needle.isEmpty() },
                            inactiveOpen, { inactiveOpen = !inactiveOpen }, tabs.selectedId, { tabs.select(it) }, { groupState.addTo = it },
                        )
                        item(key = "new_group") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp).pressFeedback().clip(RoundedCornerShape(12.dp)).clickable { groupState.newGroup() }.padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("New tab group", fontSize = 15.sp, color = browserColors().SecondaryText)
                            }
                        }
                    } else {
                        if (needle.isEmpty() && sidebarDuplicates > 0) item(key = "duplicates") { Box(Modifier.animateItem()) { DuplicatesRow(sidebarDuplicates) { sidebarConfirmDuplicates = true } } }
                        items(shownTabs.filter { !tabs.isInactive(it) || needle.isNotEmpty() }, key = { it.id }) { tab ->
                            SidebarRow(tab.shownTitle(displayUrl(tab.url)), tab.url, tab.id == tabs.selectedId, onClick = { tabs.select(tab.id) }, onLongClick = { groupState.addTo = tab }, dragTabId = tab.id)
                        }
                        inactiveSection(
                            shownTabs.filter { tabs.isInactive(it) && needle.isEmpty() },
                            inactiveOpen, { inactiveOpen = !inactiveOpen }, tabs.selectedId, { tabs.select(it) }, { groupState.addTo = it },
                        )
                    }
                    // One search field: bookmarks and history that match are listed under the tabs.
                    if (needle.isNotEmpty()) searchMatches(shownBookmarks, history.filter { it.title.contains(needle, true) || it.url.contains(needle, true) }, onOpenBookmark, onOpenHistory)
                }
            }
        }
        }
        // Pinned at the bottom: the reading list, then the private space.
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).pressFeedback().clip(androidx.compose.foundation.shape.CircleShape).background(browserColors().GridBackground).clickable { inReading = true; query = "" }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Article, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(if (unreadCount > 0) "Reading list ($unreadCount unread)" else "Reading list", color = browserColors().AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(48.dp).pressFeedback().clip(androidx.compose.foundation.shape.CircleShape).background(browserColors().GridBackground).clickable { inPrivate = true }.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Private tabs ($privateCount)", color = browserColors().AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
    }
}

/**
 * A segmented control with one Chalk pill under the labels. [position] is the pill's place in segments (0 is the
 * first), so it can slide on a tap and follow a finger.
 */
@Composable
internal fun SlidingSegments(labels: List<String>, position: Float, onSelect: (Int) -> Unit) {
    val colors = browserColors()
    androidx.compose.foundation.layout.BoxWithConstraints(
        Modifier.fillMaxWidth().height(40.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colors.GridBackground).padding(3.dp)
    ) {
        val segment = maxWidth / labels.size
        Box(
            Modifier.offset(x = segment * position).width(segment).fillMaxHeight()
                .clip(androidx.compose.foundation.shape.CircleShape).background(colors.Accent)
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, label ->
                val selected = position.roundToInt() == i
                Box(Modifier.weight(1f).fillMaxHeight().pressFeedback().clip(androidx.compose.foundation.shape.CircleShape).clickable { onSelect(i) }, contentAlignment = Alignment.Center) {
                    com.makemission.ora.ui.RollingText(
                        label, fontSize = 14.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (selected) colors.GridBackground else colors.SecondaryText,
                    )
                }
            }
        }
    }
}

/** Which list the tablet sidebar shows. */
private enum class SidebarPanel { TABS, BOOKMARKS, HISTORY }

@Composable
internal fun SegmentLabel(text: String, selected: Boolean, modifier: Modifier, pill: Boolean = false, onClick: () -> Unit) {
    val haptics = com.makemission.ora.ui.LocalOraHaptics.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .pressFeedback().clip(if (pill) androidx.compose.foundation.shape.CircleShape else RoundedCornerShape(BrowserTokens.RadiusSmall))
            .background(if (selected) (if (pill) browserColors().Accent else browserColors().SidebarSelected) else Color.Transparent)
            .clickable { if (!selected) haptics.play(com.makemission.ora.ui.Feel.SELECT); onClick() },
        contentAlignment = Alignment.Center
    ) {
        com.makemission.ora.ui.RollingText(
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
                            .pressFeedback().clickable { onQueryChange("") },
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SidebarRow(label: String, url: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, dragTabId: Long? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.SidebarRowHeight)
            .pressFeedback().clip(RoundedCornerShape(12.dp))
            .background(if (selected) browserColors().SidebarSelected else Color.Transparent)
            .then(if (dragTabId != null) Modifier.tabDragSource(dragTabId, browserColors().Accent, onLongClick).clickable(onClick = onClick) else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick))
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

/** The collapsed "Inactive tabs (N)" row: tabs not opened in 14 days. Tap to fold them in and out. */
@Composable
private fun InactiveHeader(count: Int, open: Boolean, onToggle: () -> Unit) {
    val turn by androidx.compose.animation.core.animateFloatAsState(if (open) 90f else 0f, OraMotion.enter(), label = "inactive chevron")
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).pressFeedback().clip(RoundedCornerShape(12.dp)).clickable(onClick = onToggle).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = browserColors().SecondaryText, modifier = Modifier.size(18.dp).rotate(turn))
        Spacer(Modifier.width(10.dp))
        Text("Inactive tabs ($count)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText)
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.inactiveSection(
    inactive: List<BrowserTab>,
    open: Boolean,
    onToggle: () -> Unit,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onLongClick: (BrowserTab) -> Unit,
) {
    if (inactive.isEmpty()) return
    item(key = "inactive_header") { InactiveHeader(inactive.size, open, onToggle) }
    if (open) items(inactive, key = { it.id }) { tab ->
        val haptics = com.makemission.ora.ui.LocalOraHaptics.current
        SidebarRow(tab.shownTitle(displayUrl(tab.url)), tab.url, tab.id == selectedId, onClick = { haptics.play(com.makemission.ora.ui.Feel.SELECT); onSelect(tab.id) }, onLongClick = { onLongClick(tab) }, dragTabId = tab.id)
    }
}

/** Bookmarks and history entries that match the sidebar search, shown under the matching tabs. */
private fun androidx.compose.foundation.lazy.LazyListScope.searchMatches(
    bookmarks: List<Bookmark>,
    history: List<HistoryEntry>,
    onOpenBookmark: (String) -> Unit,
    onOpenHistory: (String) -> Unit,
) {
    if (bookmarks.isNotEmpty()) {
        item(key = "found_bookmarks") { FoundLabel("In bookmarks") }
        items(bookmarks.take(5), key = { "fb_" + it.url }) { b -> SidebarRow(b.title.ifBlank { displayUrl(b.url) }, b.url, false, onClick = { onOpenBookmark(b.url) }) }
    }
    if (history.isNotEmpty()) {
        item(key = "found_history") { FoundLabel("In history") }
        items(history.take(5), key = { "fh_" + it.url + it.visitedAt }) { h -> SidebarRow(h.title.ifBlank { displayUrl(h.url) }, h.url, false, onClick = { onOpenHistory(h.url) }) }
    }
}

@Composable
private fun FoundLabel(text: String) {
    Text(text, modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 4.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText)
}

/** A row offering to close tabs that repeat an open page. Tapping it asks first, saying how many would close. */
@Composable
private fun DuplicatesRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(44.dp).pressFeedback().clip(RoundedCornerShape(12.dp)).background(browserColors().AddressBarBackground).clickable(onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(if (count == 1) "1 duplicate tab" else "$count duplicate tabs", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText, modifier = Modifier.weight(1f))
        Text("Close duplicates", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = browserColors().Accent)
    }
}

/** Confirmation for closing duplicate tabs: the one you keep stays open, and the others can be reopened. */
@Composable
private fun CloseDuplicatesDialog(count: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Close 1 duplicate tab?" else "Close $count duplicate tabs?") },
        text = { Text("One tab of each page stays open. You can bring the others back from \"Reopen closed tab\" in the menu.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Close duplicates", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * The picture to show for a tab: the fresh one from this run, else the one in memory, else the one saved on disk
 * for its address (which survives a restart). Private tabs have none, by design.
 */
@Composable
internal fun rememberTabPicture(tab: BrowserTab): ImageBitmap? {
    val fresh = tab.thumbnail ?: ThumbnailCache.get(tab.id)?.asImageBitmap()
    if (fresh != null || tab.isPrivate) return fresh
    return androidx.compose.runtime.produceState<ImageBitmap?>(null, tab.url) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { ThumbnailStore.load(tab.url) }?.asImageBitmap()
    }.value
}
