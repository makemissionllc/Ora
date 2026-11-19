package com.makemission.ora.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.outlined.Info
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** The four filters at the top of the Downloads page. */
private enum class DownloadFilter(val label: String, val emptyText: String) {
    ALL("All", "No downloads yet. Files you save from a page appear here and go to Downloads/Ora."),
    ACTIVE("Active", "Nothing is downloading right now."),
    DONE("Done", "No finished downloads yet."),
    FAILED("Failed", "No failed or cancelled downloads.");

    fun matches(item: DownloadItem): Boolean = when (this) {
        ALL -> true
        ACTIVE -> item.status == DownloadStatus.RUNNING || item.status == DownloadStatus.PAUSED
        DONE -> item.status == DownloadStatus.DONE
        FAILED -> item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED
    }
}

/** Running or paused: still on its way to the file, so it belongs in the Active section. */
private val DownloadItem.isLive: Boolean get() = status == DownloadStatus.RUNNING || status == DownloadStatus.PAUSED

/** How many parts the progress bar is drawn in. Each part fills as the file passes its share. */
private const val PROGRESS_PARTS = 4

/**
 * The Downloads page, opened as a full-screen overlay from the menu (phone) or the toolbar (tablet).
 * It grows out of [origin] and shrinks back into it on the shared overlay motion.
 * Phone: summary, filter and rows; tapping a finished row opens the file.
 * Tablet: the same list on the left and the selected file's details on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    repository: DownloadsRepository,
    tablet: Boolean,
    origin: TransformOrigin,
    onDismiss: () -> Unit,
    /** Installs a downloaded .xpi as an extension. */
    onInstall: (DownloadItem) -> Unit = {},
) {
    val all by repository.items.collectAsState()
    val context = LocalContext.current
    val colors = browserColors()
    var filter by rememberSaveable { mutableStateOf(DownloadFilter.ALL) }
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var confirmOpen by remember { mutableStateOf<DownloadItem?>(null) }
    // Where the tapped Open control sits, so the warning card grows out of it.
    var openFrom by remember { mutableStateOf(Offset.Zero) }
    // Where the last press landed, so the Delete and Clear cards grow out of the control that asked.
    var tapAt by remember { mutableStateOf(Offset.Zero) }
    // The download whose details the phone shows as its own screen, if any.
    var phoneDetailId by rememberSaveable { mutableStateOf<Long?>(null) }
    val phoneListState = rememberLazyListState()
    var confirmClear by remember { mutableStateOf(false) }

    val running = all.filter { it.status == DownloadStatus.RUNNING }
    val finishedCount = all.size - running.size
    val footer = DownloadStats.footer(all)
    val visible = all.filter { filter.matches(it) }
    val activeRows = visible.filter { it.isLive }
    val otherRows = visible.filterNot { it.isLive }
    // Tablet keeps one file selected: the one chosen, else the first in the visible list.
    val selected = if (tablet) visible.firstOrNull { it.id == selectedId } ?: visible.firstOrNull() else null

    // Enter when first shown; leave when the exit has finished, then tell the caller to remove this overlay.
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) onDismiss()
    }

    val rowActions = RowActions(
        onCancel = { repository.cancel(it.id) },
        onPause = { repository.pause(it.id) },
        onResume = { repository.resume(it.id) },
        onOpen = { item, from ->
            when {
                item.isExtensionFile -> onInstall(item)
                // A file type that can run code asks first, from the control that asked (AGENTS.md rule 10).
                DownloadSafety.isRisky(item.fileName) -> {
                    openFrom = from
                    confirmOpen = item
                }
                else -> openFile(context, item)
            }
        },
        onDetails = { phoneDetailId = it.id },
        onShare = { shareFile(context, it) },
        onDelete = { confirmDelete = it },
    )

    // The check before a file type that can run code opens. It is drawn inside whichever overlay is showing.
    val openWarning: @Composable () -> Unit = {
        confirmOpen?.let { item ->
            ConfirmCard(
                title = "Open ${item.fileName}?",
                body = "This type of file can harm your device. Only open it if you trust the site it came from.",
                confirmLabel = "Open anyway",
                origin = openFrom,
                onCancel = { BrowserLog.info("download", downloadLogLine("Kept closed after a safety warning", item.url)) },
                onConfirm = {
                    BrowserLog.warn("download", downloadLogLine("Opened after a safety warning", item.url))
                    openFile(context, item)
                },
                onDismissed = { confirmOpen = null },
            )
        }
        confirmDelete?.let { item ->
            ConfirmCard(
                title = "Delete ${item.fileName}?",
                body = "The file is removed from Downloads/Ora. This can't be undone.",
                confirmLabel = "Delete",
                origin = tapAt,
                onCancel = { BrowserLog.info("download", downloadLogLine("Delete cancelled", item.url)) },
                onConfirm = { repository.remove(item.id) },
                onDismissed = { confirmDelete = null },
            )
        }
        if (confirmClear) {
            ConfirmCard(
                title = "Clear finished downloads?",
                body = "Removes $finishedCount from this list. The files stay in Downloads/Ora.",
                confirmLabel = "Clear",
                origin = tapAt,
                onCancel = {},
                onConfirm = { repository.clearFinished() },
                onDismissed = { confirmClear = false },
            )
        }
    }

    // A full-screen page that grows from the toolbar button or the menu corner, on phone and tablet.
    Dialog(
        onDismissRequest = { visibleState.targetState = false },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()) +
                scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = origin),
            exit = fadeOut(OraMotion.overlayFadeOut()) +
                scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = origin),
        ) {
            Box(Modifier.fillMaxSize().background(colors.GridBackground).statusBarsPadding().navigationBarsPadding().trackPress { tapAt = it }) {
                if (tablet) {
                    TabletPage(
                        all = all,
                        running = running,
                        visible = visible,
                        filter = filter,
                        onFilter = { filter = it },
                        selected = selected,
                        onSelect = { selectedId = it.id },
                        onDone = { visibleState.targetState = false },
                        actions = rowActions,
                        onCopyLink = { copyLink(context, it) },
                        emptyText = filter.emptyText,
                        finishedCount = finishedCount,
                        footer = footer,
                        onClearFinished = { confirmClear = true },
                    )
                } else {
                    // A row's details replace the list in place; Back returns to the list before it closes the page.
                    val detail = all.firstOrNull { it.id == phoneDetailId }
                    BackHandler(enabled = detail != null) { phoneDetailId = null }
                    AnimatedContent(
                        targetState = detail,
                        contentKey = { it?.id },
                        transitionSpec = {
                            (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE))
                                .togetherWith(fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE))
                        },
                        label = "phone download detail",
                    ) { shown ->
                        if (shown == null) {
                            PhonePage(
                                running = running,
                                filter = filter,
                                onFilter = { filter = it },
                                visible = visible,
                                activeRows = activeRows,
                                otherRows = otherRows,
                                emptyText = filter.emptyText,
                                finishedCount = finishedCount,
                                footer = footer,
                                onClearFinished = { confirmClear = true },
                                onDone = { visibleState.targetState = false },
                                actions = rowActions,
                                listState = phoneListState,
                            )
                        } else {
                            PhoneDetail(
                                item = shown,
                                actions = rowActions,
                                onCopyLink = { copyLink(context, shown) },
                                onBack = { phoneDetailId = null },
                            )
                        }
                    }
                }
                openWarning()
            }
        }
    }
}

/** A finished .xpi file: its action is Install, not Open. */
private val DownloadItem.isExtensionFile: Boolean
    get() = fileName.endsWith(".xpi", ignoreCase = true)

/** The four row actions, bundled so both layouts share them. */
private class RowActions(
    val onCancel: (DownloadItem) -> Unit,
    val onPause: (DownloadItem) -> Unit,
    val onResume: (DownloadItem) -> Unit,
    val onOpen: (DownloadItem, Offset) -> Unit,
    val onDetails: (DownloadItem) -> Unit,
    val onShare: (DownloadItem) -> Unit,
    val onDelete: (DownloadItem) -> Unit,
)

@Composable
private fun PhonePage(
    running: List<DownloadItem>,
    filter: DownloadFilter,
    onFilter: (DownloadFilter) -> Unit,
    visible: List<DownloadItem>,
    activeRows: List<DownloadItem>,
    otherRows: List<DownloadItem>,
    emptyText: String,
    finishedCount: Int,
    footer: String?,
    onClearFinished: () -> Unit,
    onDone: () -> Unit,
    actions: RowActions,
    listState: LazyListState,
) {
    val colors = browserColors()
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(horizontal = BrowserTokens.RadiusSmall),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") { PageHeader(onDone = onDone) }
        item(key = "summary") { SummaryCard(running) }
        item(key = "filter") { FilterTrack(filter, onFilter) }
        if (visible.isEmpty()) {
            item(key = "empty") {
                Text(
                    emptyText,
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                    fontSize = 14.sp,
                    color = colors.SecondaryText,
                )
            }
        }
        if (activeRows.isNotEmpty()) {
            item(key = "label-active") { SectionLabel("Active") }
            items(activeRows, key = { it.id }) { item ->
                DownloadRow(item = item, selected = false, onTap = null, actions = actions, modifier = Modifier.animateItem())
            }
        }
        if (otherRows.isNotEmpty()) {
            item(key = "label-today") {
                SectionLabel("Today") {
                    if (finishedCount > 0) {
                        TextButton(onClick = onClearFinished) {
                            Text("Clear finished", color = Mineral.Ember, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
            items(otherRows, key = { it.id }) { item ->
                DownloadRow(item = item, selected = false, onTap = null, actions = actions, modifier = Modifier.animateItem())
            }
        }
        if (footer != null) item(key = "footer") { StatsFooter(footer) }
    }
}

/** The totals line at the end of the list. */
@Composable
private fun StatsFooter(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        fontSize = 12.sp,
        color = browserColors().SecondaryText,
    )
}

@Composable
private fun TabletPage(
    all: List<DownloadItem>,
    running: List<DownloadItem>,
    visible: List<DownloadItem>,
    filter: DownloadFilter,
    onFilter: (DownloadFilter) -> Unit,
    selected: DownloadItem?,
    onSelect: (DownloadItem) -> Unit,
    onDone: () -> Unit,
    actions: RowActions,
    onCopyLink: (DownloadItem) -> Unit,
    emptyText: String,
    finishedCount: Int,
    footer: String?,
    onClearFinished: () -> Unit,
) {
    val colors = browserColors()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = BrowserTokens.RadiusMedium, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TabletHeader(running = running, onBack = onDone, onDone = onDone)
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(0.42f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterTrack(filter, onFilter)
                if (visible.isEmpty()) {
                    Text(emptyText, modifier = Modifier.padding(horizontal = 4.dp), fontSize = 14.sp, color = colors.SecondaryText)
                }
                val activeRows = visible.filter { it.isLive }
                val otherRows = visible.filterNot { it.isLive }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                    if (activeRows.isNotEmpty()) {
                        item(key = "label-active") { SectionLabel("Active") }
                        items(activeRows, key = { it.id }) { item ->
                            DownloadRow(
                                item = item,
                                selected = item.id == selected?.id,
                                onTap = { onSelect(item) },
                                actions = actions,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (otherRows.isNotEmpty()) {
                        item(key = "label-today") {
                            SectionLabel("Today") {
                                if (finishedCount > 0) {
                                    TextButton(onClick = onClearFinished) {
                                        Text("Clear finished", color = Mineral.Ember, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                        items(otherRows, key = { it.id }) { item ->
                            DownloadRow(
                                item = item,
                                selected = item.id == selected?.id,
                                onTap = { onSelect(item) },
                                actions = actions,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                    if (footer != null) item(key = "footer") { StatsFooter(footer) }
                }
            }
            Box(Modifier.weight(0.58f).fillMaxHeight()) {
                if (all.isEmpty() || selected == null) {
                    Box(
                        Modifier.fillMaxSize().clip(RoundedCornerShape(BrowserTokens.RadiusLarge)).background(colors.Surface),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (all.isEmpty()) "Select a download to see its details." else "Select a download to see its details.",
                            fontSize = 14.sp,
                            color = colors.SecondaryText,
                        )
                    }
                } else {
                    // The details change on the shared overlay timings, like the extension detail on phone.
                    AnimatedContent(
                        targetState = selected,
                        transitionSpec = {
                            (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE))
                                .togetherWith(fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE))
                        },
                        label = "download detail",
                        contentKey = { it.id },
                    ) { item ->
                        DownloadDetail(item = item, actions = actions, onCopyLink = { onCopyLink(item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TabletHeader(running: List<DownloadItem>, onBack: () -> Unit, onDone: () -> Unit) {
    val colors = browserColors()
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(BrowserTokens.ToolbarButtonSize)
                .clip(CircleShape)
                .background(colors.Surface)
                .pressFeedback()
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colors.IconColor, modifier = Modifier.size(20.dp))
        }
        Text(
            "Downloads",
            modifier = Modifier.weight(1f),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.AddressBarText,
        )
        Text(summaryLine(running), fontSize = 15.sp, color = colors.SecondaryText)
        Box(
            modifier = Modifier
                .height(BrowserTokens.ToolbarButtonSize)
                .clip(CircleShape)
                .background(colors.Accent)
                .pressFeedback()
                .clickable(onClick = onDone)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.GridBackground)
        }
    }
}

@Composable
private fun PageHeader(onDone: () -> Unit) {
    val colors = browserColors()
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Downloads",
            modifier = Modifier.weight(1f),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.AddressBarText,
        )
        Box(
            modifier = Modifier
                .height(BrowserTokens.RowIconSize)
                .clip(CircleShape)
                .background(colors.AddressBarBackground)
                .pressFeedback()
                .clickable(onClick = onDone)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Done", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
        }
    }
}

/** "2 downloading, 6.4 MB/s" with the bytes left under it, as the design shows. */
@Composable
private fun SummaryCard(running: List<DownloadItem>) {
    val colors = browserColors()
    val subtitle = if (running.isEmpty()) {
        "Files you save from a page appear here."
    } else if (running.all { it.totalBytes > 0 }) {
        "${DownloadsRepository.sizeLabel(running.sumOf { it.totalBytes - it.bytes })} left to download"
    } else {
        "Some sizes are not known yet"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
            .background(colors.Surface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(summaryLine(running), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
            Text(subtitle, fontSize = 13.sp, color = colors.SecondaryText)
        }
    }
}

private fun summaryLine(running: List<DownloadItem>): String {
    if (running.isEmpty()) return "No downloads running"
    val speed = running.sumOf { it.speed }
    return "${running.size} downloading" + if (speed > 0) ", ${speedLabel(speed)}" else ""
}

/** Four equal pills in a track: the selected one is filled with Accent and eases to its new place. */
@Composable
private fun FilterTrack(selected: DownloadFilter, onSelect: (DownloadFilter) -> Unit) {
    val colors = browserColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.RadiusLarge))
            .background(colors.Surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        DownloadFilter.values().forEach { option ->
            val isSelected = option == selected
            val fill by animateColorAsState(if (isSelected) colors.Accent else Color.Transparent, OraMotion.enter(), label = "filter fill")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    option.label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.GridBackground else colors.SecondaryText,
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, trailing: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = browserColors().SecondaryText,
        )
        trailing()
    }
}

/**
 * One file row. On phone [onTap] is null: a finished row opens the file on tap and shares on long press.
 * On tablet [onTap] selects the row for the details pane, and the detail's buttons do the rest.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(
    item: DownloadItem,
    selected: Boolean,
    onTap: (() -> Unit)?,
    actions: RowActions,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    val interaction = remember { MutableInteractionSource() }
    val anchor = remember { TriggerAnchor() }
    val background by animateColorAsState(
        if (selected) colors.AddressBarBackground else Color.Transparent,
        OraMotion.enter(),
        label = "row selection",
    )
    Row(
        modifier = modifier
            .onGloballyPositioned { anchor.coords = it }
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
            .background(background)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    when {
                        onTap != null -> onTap()
                        item.status == DownloadStatus.DONE -> actions.onOpen(item, anchor.centre())
                        else -> actions.onDetails(item)
                    }
                },
                onLongClick = { if (onTap == null && item.status == DownloadStatus.DONE) actions.onShare(item) },
            )
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FileTile(item)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.fileName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.AddressBarText,
            )
            if (item.isLive) SegmentBar(item.progress, height = 6.dp)
            Text(
                statusLine(item),
                fontSize = 13.sp,
                color = if (item.status == DownloadStatus.FAILED) Mineral.Ember else colors.SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            when (item.status) {
                DownloadStatus.RUNNING -> {
                    if (item.pausable) IconTile(Icons.Outlined.Pause, "Pause download") { actions.onPause(item) }
                    IconTile(Icons.Outlined.Close, "Cancel download") { actions.onCancel(item) }
                }
                DownloadStatus.PAUSED -> {
                    IconTile(Icons.Outlined.PlayArrow, "Resume download") { actions.onResume(item) }
                    IconTile(Icons.Outlined.Close, "Cancel download") { actions.onCancel(item) }
                }
                DownloadStatus.DONE -> if (onTap == null) {
                    if (item.isExtensionFile) Text(
                        "Install",
                        color = colors.Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.height(44.dp).clickable { actions.onOpen(item, anchor.centre()) }.padding(horizontal = 10.dp).wrapContentHeight(Alignment.CenterVertically),
                    )
                    IconTile(Icons.Outlined.Info, "Show details") { actions.onDetails(item) }
                    IconTile(Icons.Outlined.Delete, "Delete file") { actions.onDelete(item) }
                } else Icon(Icons.Outlined.Check, contentDescription = "Done", tint = Mineral.Lichen, modifier = Modifier.size(18.dp))
                DownloadStatus.FAILED -> {
                    IconTile(Icons.Outlined.Refresh, "Retry download") { actions.onResume(item) }
                    IconTile(Icons.Outlined.Delete, "Remove from list") { actions.onDelete(item) }
                }
                DownloadStatus.CANCELLED -> IconTile(Icons.Outlined.Delete, "Remove from list") { actions.onDelete(item) }
            }
        }
    }
}

/**
 * Details for one file: the file, its progress, what is known about it, the checksum check and the actions that
 * apply. The tablet shows it as one pane. The phone shows it as its own screen ([compact]): it scrolls, and the stats
 * and buttons wrap onto more lines instead of sharing one row.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadDetail(
    item: DownloadItem,
    actions: RowActions,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    compact: Boolean = false,
) {
    val colors = browserColors()
    val openAnchor = remember { TriggerAnchor() }
    val speed = if (item.speed > 0) speedLabel(item.speed) else "Not measured"
    val size = if (item.totalBytes > 0) DownloadsRepository.sizeLabel(item.totalBytes) else "Not sent by the site"
    val source = sourceHost(item.url)
    val started = timeLabel(item.startedAt)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BrowserTokens.RadiusLarge))
            .background(colors.Surface)
            .then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = if (compact) 20.dp else 30.dp, vertical = if (compact) 20.dp else 26.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            FileTile(item)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    item.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.AddressBarText,
                )
                Text(
                    "From $source, saved to Downloads/Ora",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 14.sp,
                    color = colors.SecondaryText,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SegmentBar(item.progress, height = 10.dp)
            Text(statusLine(item), fontSize = 15.sp, color = if (item.status == DownloadStatus.FAILED) Mineral.Ember else colors.AddressBarText)
            if (item.status == DownloadStatus.DONE && DownloadSafety.isRisky(item.fileName)) {
                Text(
                    "This type of file can harm your device. Check where it came from before you open it.",
                    fontSize = 14.sp,
                    color = Mineral.Ember,
                )
            }
            if (DownloadSafety.isPlainHttp(item.url)) {
                Text(
                    "Sent over an unencrypted connection. Check the file against a published checksum before you trust it.",
                    fontSize = 14.sp,
                    color = colors.SecondaryText,
                )
            }
        }
        if (compact) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailStat("Speed", speed, Modifier.widthIn(min = 120.dp))
                DetailStat("Size", size, Modifier.widthIn(min = 120.dp))
                DetailStat("Source", source, Modifier.widthIn(min = 120.dp))
                DetailStat("Started", started, Modifier.widthIn(min = 120.dp))
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DetailStat("Speed", speed, Modifier.weight(1f))
                DetailStat("Size", size, Modifier.weight(1f))
                DetailStat("Source", source, Modifier.weight(1f))
                DetailStat("Started", started, Modifier.weight(1f))
            }
        }
        if (item.status == DownloadStatus.DONE) ChecksumCheck(item)
        if (compact) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPills(item, actions, openAnchor)
                DetailPill("Copy link", colors.AddressBarText, onClick = onCopyLink)
            }
        } else {
            Box(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPills(item, actions, openAnchor)
                Box(Modifier.weight(1f))
                DetailPill("Copy link", colors.AddressBarText, onClick = onCopyLink)
            }
        }
    }
}

/** The buttons that suit the file's state: pause or cancel, open, retry, share or delete. */
@Composable
private fun StatusPills(item: DownloadItem, actions: RowActions, openAnchor: TriggerAnchor) {
    val colors = browserColors()
    when (item.status) {
        DownloadStatus.RUNNING -> {
            if (item.pausable) DetailPill("Pause", colors.AddressBarText) { actions.onPause(item) }
            DetailPill("Cancel download", Mineral.Ember) { actions.onCancel(item) }
        }
        DownloadStatus.PAUSED -> {
            DetailPill("Resume", colors.Accent, filled = true) { actions.onResume(item) }
            DetailPill("Cancel download", Mineral.Ember) { actions.onCancel(item) }
        }
        DownloadStatus.DONE -> {
            DetailPill(
                if (item.isExtensionFile) "Install" else "Open",
                colors.Accent,
                filled = true,
                anchor = openAnchor,
            ) { actions.onOpen(item, openAnchor.centre()) }
            DetailPill("Share", colors.AddressBarText) { actions.onShare(item) }
            DetailPill("Delete file", Mineral.Ember) { actions.onDelete(item) }
        }
        DownloadStatus.FAILED -> {
            DetailPill("Retry", colors.Accent, filled = true) { actions.onResume(item) }
            DetailPill("Remove from list", Mineral.Ember) { actions.onDelete(item) }
        }
        DownloadStatus.CANCELLED -> DetailPill("Remove from list", Mineral.Ember) { actions.onDelete(item) }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = browserColors()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 13.sp, color = colors.SecondaryText)
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * A round-cornered pill button: Accent filled when [filled], otherwise a quiet AddressBarBackground pill.
 * [anchor] remembers where the pill sits, so a card can grow out of it.
 */
@Composable
private fun DetailPill(label: String, textColor: Color, filled: Boolean = false, anchor: TriggerAnchor? = null, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .onGloballyPositioned { anchor?.coords = it }
            .height(BrowserTokens.ToolbarButtonSize)
            .clip(CircleShape)
            .background(if (filled) colors.Accent else colors.AddressBarBackground)
            .pressFeedback()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = if (filled) colors.GridBackground else textColor)
    }
}

/** Type tile: the file extension on a soft tint picked from the palette. */
@Composable
private fun FileTile(item: DownloadItem) {
    val ext = item.fileName.substringAfterLast('.', "").take(4).uppercase().ifEmpty { "FILE" }
    val tint = Mineral.SiteTints[Math.floorMod(item.fileName.hashCode(), Mineral.SiteTints.size)]
    Box(
        modifier = Modifier
            .size(BrowserTokens.RowIconSize)
            .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
            .background(tint),
        contentAlignment = Alignment.Center,
    ) {
        Text(ext, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Mineral.Ink, maxLines = 1)
    }
}

/**
 * Flat bar split into parts. Each part fills as the file passes its share of the whole;
 * the fill eases on the shared enter motion so a jump in progress glides rather than snaps.
 */
@Composable
private fun SegmentBar(progress: Float, height: androidx.compose.ui.unit.Dp) {
    val colors = browserColors()
    Row(
        modifier = Modifier.fillMaxWidth().height(height),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(PROGRESS_PARTS) { part ->
            val target = if (progress < 0f) 0f else (progress * PROGRESS_PARTS - part).coerceIn(0f, 1f)
            val fill by animateFloatAsState(target, OraMotion.enter(), label = "progress part")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.AddressBarBackground),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .background(colors.Accent),
                )
            }
        }
    }
}

@Composable
private fun IconTile(icon: ImageVector, description: String, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .size(BrowserTokens.RowIconSize)
            .clip(CircleShape)
            .background(colors.AddressBarBackground)
            .pressFeedback()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = colors.IconColor, modifier = Modifier.size(18.dp))
    }
}


private fun statusLine(item: DownloadItem): String {
    val size = DownloadsRepository.sizeLabel(item.bytes.takeIf { it > 0 } ?: item.totalBytes)
    return when (item.status) {
        DownloadStatus.RUNNING -> when {
            item.totalBytes <= 0 -> "${DownloadsRepository.sizeLabel(item.bytes)} so far"
            else -> {
                val parts = mutableListOf("${DownloadsRepository.sizeLabel(item.bytes)} of $size")
                if (item.speed > 0) parts += speedLabel(item.speed)
                if (item.speed > 0) timeLeft(item.totalBytes - item.bytes, item.speed)?.let { parts += it }
                parts.joinToString(", ")
            }
        }
        DownloadStatus.PAUSED -> if (item.pausedBySystem) "Paused by Android · resumes automatically" else "Paused · ${DownloadsRepository.sizeLabel(item.bytes)} of ${if (item.totalBytes > 0) size else "unknown size"}"
        DownloadStatus.DONE -> "Done · $size · Downloads/Ora"
        DownloadStatus.FAILED -> "Failed: ${item.error ?: "unknown error"}"
        DownloadStatus.CANCELLED -> "Cancelled"
    }
}

private fun speedLabel(bytesPerSecond: Long): String = when {
    bytesPerSecond >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSecond / 1024.0 / 1024.0)
    else -> "${bytesPerSecond / 1024} KB/s"
}

private fun timeLeft(bytesLeft: Long, bytesPerSecond: Long): String? {
    if (bytesLeft <= 0 || bytesPerSecond <= 0) return null
    val seconds = bytesLeft / bytesPerSecond
    return when {
        seconds < 60 -> "$seconds s left"
        else -> "${seconds / 60} min left"
    }
}

private fun sourceHost(url: String): String = runCatching { Uri.parse(url).host }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unknown site"

private fun timeLabel(epochMillis: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMillis))

/** Opens a finished file with whichever app handles its type. */
private fun openFile(context: Context, item: DownloadItem) {
    val uri = item.contentUri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Open with")) }
        .onFailure { BrowserLog.error("download", downloadLogLine("No app can open", item.url)) }
}

/** Shares a finished file through the system share sheet. */
private fun shareFile(context: Context, item: DownloadItem) {
    val uri = item.contentUri ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = item.mimeType ?: "*/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, item.fileName))
}

/** Puts the file's page address on the clipboard and confirms with a short toast. */
private fun copyLink(context: Context, item: DownloadItem) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Download link", item.url))
    BrowserLog.info("download", downloadLogLine("Copied the link for", item.url))
    Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
}

/**
 * Optional check: paste the SHA-256 or MD5 the site publishes. Ora reads the saved file once, when a complete hash is
 * first entered. Every later edit only compares against that result, so typing does not read the file again.
 */
@Composable
private fun ChecksumCheck(item: DownloadItem) {
    val colors = browserColors()
    val context = LocalContext.current
    var pasted by rememberSaveable(item.id) { mutableStateOf("") }
    val algorithm = DownloadSafety.algorithmFor(pasted)
    // The file is read once, the first time a complete hash is typed. Later edits only compare against that result.
    var outcome by remember(item.contentUri) { mutableStateOf<Result<FileHashes>?>(null) }
    LaunchedEffect(item.contentUri, algorithm != null) {
        val uri = item.contentUri ?: return@LaunchedEffect
        if (algorithm != null && outcome == null) {
            outcome = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { DownloadSafety.hashes(it) }
                        ?: error("The file could not be opened")
                }
            }
        }
    }
    val hashes = outcome?.getOrNull()
    val verdict: Boolean? = hashes?.let { DownloadSafety.compare(pasted, it) }
    val failed = outcome?.isFailure == true
    LaunchedEffect(verdict) {
        when (verdict) {
            true -> BrowserLog.info("download", downloadLogLine("Checksum matched for", item.url))
            false -> BrowserLog.warn("download", downloadLogLine("Checksum did not match for", item.url))
            null -> Unit
        }
    }
    LaunchedEffect(outcome) {
        outcome?.exceptionOrNull()?.let {
            BrowserLog.error("download", downloadLogLine("Could not read the file to check it for", item.url), it)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Check the file (optional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
        BasicTextField(
            value = pasted,
            onValueChange = { pasted = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            textStyle = TextStyle(fontSize = 15.sp, color = colors.AddressBarText),
            cursorBrush = SolidColor(colors.Accent),
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.ToolbarButtonSize)
                .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
                .background(colors.AddressBarBackground)
                .padding(horizontal = 20.dp),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (pasted.isEmpty()) Text("Paste a SHA-256 or MD5 from the website", fontSize = 15.sp, color = colors.SecondaryText)
                    inner()
                }
            },
        )
        val message: String? = when {
            pasted.isBlank() -> null
            algorithm == null -> "Paste the full SHA-256 (64 characters) or MD5 (32 characters)."
            item.contentUri == null -> "The file is missing from Downloads/Ora."
            failed -> "The file could not be read, so it cannot be checked."
            verdict == null -> "Checking the file…"
            verdict == true -> "Matches"
            else -> "Doesn't match"
        }
        message?.let {
            Text(
                it,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = when {
                    failed || verdict == false -> Mineral.Ember
                    verdict == true -> Mineral.Lichen
                    else -> colors.SecondaryText
                },
            )
        }
    }
}

/** Reports where each press lands, in window coordinates, without consuming it. */
private fun Modifier.trackPress(onPress: (Offset) -> Unit): Modifier = composed {
    var topLeft by remember { mutableStateOf(Offset.Zero) }
    this
        .onGloballyPositioned { topLeft = it.positionInWindow() }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.firstOrNull { it.pressed && !it.previousPressed }?.let { onPress(topLeft + it.position) }
                }
            }
        }
}

/** Remembers where a control sits in its window, so a card can grow out of it when the control is tapped. */
private class TriggerAnchor {
    var coords: LayoutCoordinates? = null

    /** The middle of the control in window coordinates. Zero until the control has been placed. */
    fun centre(): Offset =
        coords?.let { it.positionInWindow() + Offset(it.size.width / 2f, it.size.height / 2f) } ?: Offset.Zero
}

/**
 * A confirmation card (AGENTS.md rule 10), used for the open warning, Delete file and Clear finished. The card grows out of the control that
 * asked, from [origin] in window coordinates, and shrinks back into it on Cancel, a tap outside, Back or Open anyway.
 */
@Composable
private fun ConfirmCard(
    title: String,
    body: String,
    confirmLabel: String,
    origin: Offset,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    onDismissed: () -> Unit,
) {
    val colors = browserColors()
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    var cardTopLeft by remember { mutableStateOf(Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) onDismissed()
    }
    val close = { visibleState.targetState = false }
    BackHandler(onBack = close)
    // The pivot is the trigger's position inside the card's own box, so the card scales out of that point.
    val pivot = if (cardSize.width > 0 && cardSize.height > 0) {
        TransformOrigin(
            ((origin.x - cardTopLeft.x) / cardSize.width).coerceIn(0f, 1f),
            ((origin.y - cardTopLeft.y) / cardSize.height).coerceIn(0f, 1f),
        )
    } else {
        TransformOrigin.Center
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()),
            exit = fadeOut(OraMotion.overlayFadeOut()),
        ) {
            Box(Modifier.fillMaxSize().background(colors.Scrim).clickable(onClick = close))
        }
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(OraMotion.overlayFadeIn()) +
                scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = pivot),
            exit = fadeOut(OraMotion.overlayFadeOut()) +
                scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = pivot),
        ) {
            Column(
                modifier = Modifier
                    .width(BrowserTokens.QuickMenuWidth)
                    .onGloballyPositioned {
                        cardTopLeft = it.positionInWindow()
                        cardSize = it.size
                    }
                    .clip(RoundedCornerShape(BrowserTokens.QuickMenuRadius))
                    .background(colors.AddressBarBackground)
                    // Taps on the card do not reach the scrim behind it.
                    .clickable(onClick = {})
                    .padding(20.dp),
            ) {
                Text(
                    title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.AddressBarText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    body,
                    fontSize = 14.sp,
                    color = colors.SecondaryText,
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        onCancel()
                        close()
                    }) {
                        Text("Cancel", color = colors.Accent)
                    }
                    TextButton(onClick = {
                        onConfirm()
                        close()
                    }) {
                        Text(confirmLabel, color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** A phone download's details as their own screen, reached from its row. Back returns to the list. */
@Composable
private fun PhoneDetail(item: DownloadItem, actions: RowActions, onCopyLink: () -> Unit, onBack: () -> Unit) {
    val colors = browserColors()
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = BrowserTokens.RadiusSmall),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(BrowserTokens.ToolbarButtonSize)
                    .clip(CircleShape)
                    .background(colors.Surface)
                    .pressFeedback()
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to downloads", tint = colors.IconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Details", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
        }
        DownloadDetail(item = item, actions = actions, onCopyLink = onCopyLink, modifier = Modifier.weight(1f), compact = true)
    }
}
