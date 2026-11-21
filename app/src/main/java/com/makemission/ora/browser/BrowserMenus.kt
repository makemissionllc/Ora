package com.makemission.ora.browser

import com.makemission.ora.ui.oraRowMotion
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.OpenInBrowser
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
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
    /** Reading list: true when the open page is saved there. */
    isReadLater: Boolean = false,
    onReadLater: () -> Unit = {},
    onSavePdf: () -> Unit = {},
    onQuickSwitcher: () -> Unit = {},
    onSessions: () -> Unit = {},
    onZap: () -> Unit = {},
    onTweaks: () -> Unit = {},
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
    /** Reader view: [readerOpen] marks it active, [readerHint] says why it can't open when the page has no article. */
    readerOpen: Boolean,
    readerHint: String,
    onReader: () -> Unit,
    onCopyAddress: () -> Unit,
    onAddToHome: () -> Unit,
    /** Null hides the row; set on https pages only (see [MainBrowserHandoff]). */
    onOpenInMainBrowser: (() -> Unit)? = null,
    /** Null hides the row. Set when Link my devices is on; opens the "Send to a device" card. [sendToDeviceHint] says how many are paired or waiting. */
    onSendToDevice: (() -> Unit)? = null,
    sendToDeviceHint: String? = null,
    /** Null hides the row. A page another device had open in the last 15 minutes that is not open here: "Continue from <device>". */
    continueOffer: com.makemission.ora.devices.ContinueOffer? = null,
    onContinue: () -> Unit = {},
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
            isReadLater = isReadLater,
            onReadLater = { act(onReadLater) },
            onSavePdf = { act(onSavePdf) },
            onQuickSwitcher = { act(onQuickSwitcher) },
            onSessions = { act(onSessions) },
            onZap = { act(onZap) },
            onTweaks = { act(onTweaks) },
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
            readerOpen = readerOpen,
            readerHint = readerHint,
            onReader = { act(onReader) },
            onCopyAddress = { act(onCopyAddress) },
            onAddToHome = { act(onAddToHome) },
            onOpenInMainBrowser = onOpenInMainBrowser?.let { open -> { act(open) } },
            onSendToDevice = onSendToDevice?.let { send -> { act(send) } },
            sendToDeviceHint = sendToDeviceHint,
            continueOffer = continueOffer,
            onContinue = { act(onContinue) },
            closedTabTitle = closedTabTitle,
            onReopenClosed = { act(onReopenClosed) },
        )
    }
    if (isTablet) {
        BrowserMenuSidePanel(onDismiss = onDismiss, topInset = topInset, closeRequests = closeRequests, content = items)
    } else {
        OraSheet(onDismiss = onDismiss, tag = "menu", containerColor = browserColors().Surface) { close ->
            items { action -> close(); action() }
        }
    }
}

@Composable
private fun BrowserMenuItems(
    isBookmarked: Boolean,
    isReadLater: Boolean,
    onReadLater: () -> Unit,
    onSavePdf: () -> Unit,
    onQuickSwitcher: () -> Unit,
    onSessions: () -> Unit,
    onZap: () -> Unit,
    onTweaks: () -> Unit,
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
    /** Reader view: [readerOpen] marks it active, [readerHint] says why it can't open when the page has no article. */
    readerOpen: Boolean,
    readerHint: String,
    onReader: () -> Unit,
    onCopyAddress: () -> Unit,
    onAddToHome: () -> Unit,
    onOpenInMainBrowser: (() -> Unit)?,
    onSendToDevice: (() -> Unit)?,
    sendToDeviceHint: String?,
    continueOffer: com.makemission.ora.devices.ContinueOffer?,
    onContinue: () -> Unit,
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
            // Quality of life: the row only shows when there is something new to continue, so it never offers a page already open here.
            if (continueOffer != null) {
                MenuRow(Icons.Outlined.Devices, "Continue from ${continueOffer.deviceName}", onContinue, subtitle = continueOffer.title.ifBlank { continueOffer.url }, chevron = true)
            }
            MenuRow(Icons.Outlined.Search, "Find in page", onFind, chevron = true)
            MenuRow(
                Icons.Outlined.Article, "Reader view", onReader,
                subtitle = readerHint,
                trailing = if (readerOpen) ({ Text("On", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = browserColors().Accent) }) else null,
                chevron = !readerOpen,
            )
            // Quality of life: one tap copies the page's address, without opening Share first.
            MenuRow(
                if (isReadLater) Icons.Outlined.PlaylistAddCheck else Icons.Outlined.PlaylistAdd, if (isReadLater) "Remove from reading list" else "Add to reading list", onReadLater,
                subtitle = if (isReadLater) "Saved to read later" else null,
            )
            MenuRow(Icons.Outlined.Terminal, "Quick switcher", onQuickSwitcher, subtitle = "Find a tab, bookmark, page or action")
            MenuRow(Icons.Outlined.Tune, "Site tweaks", onTweaks, subtitle = "Dark colours, wider text or a font for this site")
            MenuRow(Icons.Outlined.VisibilityOff, "Hide an element", onZap, subtitle = "Pick a banner or bar to hide on this site")
            MenuRow(Icons.Outlined.Folder, "Saved sessions", onSessions, subtitle = "Save your open tabs, or open a saved set")
            MenuRow(Icons.Outlined.PictureAsPdf, "Save as PDF", onSavePdf, subtitle = "To Downloads, named after the page")
            MenuRow(Icons.Outlined.ContentCopy, "Copy page address", onCopyAddress)
            // Quality of life: pages sent to a device that is asleep wait in an outbox and arrive when it wakes.
            if (onSendToDevice != null) {
                MenuRow(Icons.Outlined.Devices, "Send to a device", onSendToDevice, subtitle = sendToDeviceHint, chevron = true)
            }
            MenuRow(Icons.Outlined.AddToHomeScreen, "Add to home screen", onAddToHome)
            // Quality of life: when a site's passkey sign-in fails in Ora, one tap carries the page to another browser.
            if (onOpenInMainBrowser != null) {
                MenuRow(Icons.Outlined.OpenInBrowser, "Passkey not working? Open in main browser", onOpenInMainBrowser)
            }
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
    val haptics = com.makemission.ora.ui.LocalOraHaptics.current
    // The thumb slides and the track colour crossfades, both with the Quick token.
    // While the finger is down the knob stretches toward the side it will move to, and the slide then starts from there.
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val knobWidth by androidx.compose.animation.core.animateDpAsState(if (pressed) 26.dp else 20.dp, OraMotion.quick(), label = "switch knob width")
    val thumbX by androidx.compose.animation.core.animateDpAsState(if (checked) 28.dp - knobWidth else 0.dp, OraMotion.quick(), label = "switch thumb")
    val track by androidx.compose.animation.animateColorAsState(if (checked) colors.Accent else colors.AddressBarBackground, OraMotion.quick(), label = "switch track")
    val thumb by androidx.compose.animation.animateColorAsState(if (checked) colors.GridBackground else colors.SecondaryText, OraMotion.quick(), label = "switch thumb colour")
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(track)
            .toggleable(value = checked, role = Role.Switch, interactionSource = press, indication = null, onValueChange = {
                haptics.toggle(it)
                onChange(it)
            })
            .semantics { contentDescription = label }
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.offset(x = thumbX).size(width = knobWidth, height = 20.dp).clip(CircleShape).background(thumb))
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
    val reducedPanel = com.makemission.ora.ui.theme.LocalReducedMotion.current

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
        // The panel grows from the ⋯ button (top right) from 92% to 100%, and folds back into it. Remove animations: fades only.
        enter = fadeIn(OraMotion.standard()) + if (reducedPanel) androidx.compose.animation.EnterTransition.None else androidx.compose.animation.scaleIn(OraMotion.standard(), initialScale = 0.92f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
        exit = fadeOut(OraMotion.leave()) + if (reducedPanel) androidx.compose.animation.ExitTransition.None else androidx.compose.animation.scaleOut(OraMotion.leave(), targetScale = 0.92f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            // 12 dp from the right and bottom edges, below the floating row. Only the panel's own shape clips;
            // content taller than the panel scrolls instead of being cut off.
            val shape = RoundedCornerShape(BrowserTokens.SidePanelRadius)
            val menuScroll = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(if (topInset == 0.dp) Modifier.statusBarsPadding() else Modifier.padding(top = topInset))
                    .navigationBarsPadding()
                    .padding(top = BrowserTokens.PanelInset, end = BrowserTokens.PanelInset, bottom = BrowserTokens.PanelInset)
                    .width(BrowserTokens.SidePanelWidth)
                    .shadow(BrowserTokens.FloatingBarElevation, shape)
                    .clip(shape)
                    .background(browserColors().Surface)
                    // A 24 dp fade at the bottom edge while more rows wait below, so a half-cut row reads as "more".
                    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (menuScroll.canScrollForward) {
                            val fade = 24.dp.toPx()
                            drawRect(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    listOf(androidx.compose.ui.graphics.Color.Black, androidx.compose.ui.graphics.Color.Transparent),
                                    startY = size.height - fade, endY = size.height,
                                ),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - fade),
                                size = androidx.compose.ui.geometry.Size(size.width, fade),
                                blendMode = androidx.compose.ui.graphics.BlendMode.DstIn,
                            )
                        }
                    }
                    .verticalScroll(menuScroll)
                    .padding(top = 16.dp)
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
    reading: com.makemission.ora.readinglist.ReadingListRepository,
    onOpenReading: (com.makemission.ora.readinglist.ReadingItem) -> Unit,
    onOpen: (String) -> Unit,
    onRemoveBookmark: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onDone: () -> Unit,
) {
    // 0 Bookmarks, 1 Reading, 2 History.
    var segment by rememberSaveable { mutableStateOf(0) }
    val showHistory = segment == 2
    val readingItems by reading.items.collectAsState()
    val unreadCount = readingItems.count { !it.read }
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
            SegmentLabel("Bookmarks", selected = segment == 0, modifier = Modifier.weight(1f), pill = true) { segment = 0; query = "" }
            // Quality of life: the label carries the number still to read, so the list is worth opening.
            SegmentLabel(if (unreadCount > 0) "Reading · $unreadCount" else "Reading", selected = segment == 1, modifier = Modifier.weight(1f), pill = true) { segment = 1; query = "" }
            SegmentLabel("History", selected = segment == 2, modifier = Modifier.weight(1f), pill = true) { segment = 2; query = "" }
        }
        Spacer(Modifier.height(14.dp))
        SidebarSearch(
            query = query,
            placeholder = when (segment) { 2 -> "Search history"; 1 -> "Search reading list"; else -> "Search bookmarks" },
            pill = true,
            onQueryChange = { query = it }
        )
        Spacer(Modifier.height(14.dp))
        if (segment == 1) {
            ReadingList(
                items = readingItems,
                query = query,
                onOpen = onOpenReading,
                onToggleRead = { reading.setRead(it.url, !it.read) },
                onRemove = { reading.remove(it.url) },
                onClearRead = { reading.clearRead() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else if (showHistory) {
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
                        modifier = oraRowMotion()
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
