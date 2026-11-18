package com.makemission.ora.downloads

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var confirmClear by remember { mutableStateOf(false) }

    val running = all.filter { it.status == DownloadStatus.RUNNING }
    val finishedCount = all.size - running.size
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
        onOpen = { if (it.isExtensionFile) onInstall(it) else openFile(context, it) },
        onShare = { shareFile(context, it) },
        onDelete = { confirmDelete = it },
    )

    // Tablet: a full-screen overlay that grows from the toolbar button.
    if (tablet) Dialog(
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
            Box(Modifier.fillMaxSize().background(colors.GridBackground).statusBarsPadding().navigationBarsPadding()) {
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
                        onClearFinished = { confirmClear = true },
                    )
                } else {
                    PhonePage(
                        running = running,
                        filter = filter,
                        onFilter = { filter = it },
                        visible = visible,
                        activeRows = activeRows,
                        otherRows = otherRows,
                        emptyText = filter.emptyText,
                        finishedCount = finishedCount,
                        onClearFinished = { confirmClear = true },
                        onDone = { visibleState.targetState = false },
                        actions = rowActions,
                    )
                }
            }
        }
    }

    // Phone: a bottom sheet that opens halfway and drags up to full screen. Same list and actions as the tablet.
    if (!tablet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.GridBackground,
            dragHandle = {
                Box(
                    Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(colors.SecondaryText.copy(alpha = 0.4f))
                )
            },
        ) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                PhonePage(
                    running = running,
                    filter = filter,
                    onFilter = { filter = it },
                    visible = visible,
                    activeRows = activeRows,
                    otherRows = otherRows,
                    emptyText = filter.emptyText,
                    finishedCount = finishedCount,
                    onClearFinished = { confirmClear = true },
                    onDone = { scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() } },
                    actions = rowActions,
                )
            }
        }
    }

    confirmDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete ${item.fileName}?") },
            text = { Text("The file is removed from Downloads/Ora. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.remove(item.id)
                    confirmDelete = null
                }) { Text("Delete", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = colors.SecondaryText) }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear finished downloads?") },
            text = { Text("Removes $finishedCount from this list. The files stay in Downloads/Ora.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.clearFinished()
                    confirmClear = false
                }) { Text("Clear", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = colors.SecondaryText) }
            }
        )
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
    val onOpen: (DownloadItem) -> Unit,
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
    onClearFinished: () -> Unit,
    onDone: () -> Unit,
    actions: RowActions,
) {
    val colors = browserColors()
    LazyColumn(
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
    }
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
    val background by animateColorAsState(
        if (selected) colors.AddressBarBackground else Color.Transparent,
        OraMotion.enter(),
        label = "row selection",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
            .background(background)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = {
                    when {
                        onTap != null -> onTap()
                        item.status == DownloadStatus.DONE -> actions.onOpen(item)
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
                        modifier = Modifier.height(44.dp).clickable { actions.onOpen(item) }.padding(horizontal = 10.dp).wrapContentHeight(Alignment.CenterVertically),
                    )
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

/** Tablet details: the file, its progress, what is known about it, and the actions that apply. */
@Composable
private fun DownloadDetail(item: DownloadItem, actions: RowActions, onCopyLink: () -> Unit) {
    val colors = browserColors()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(BrowserTokens.RadiusLarge))
            .background(colors.Surface)
            .padding(horizontal = 30.dp, vertical = 26.dp),
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
                    "From ${sourceHost(item.url)}, saved to Downloads/Ora",
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
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailStat("Speed", if (item.speed > 0) speedLabel(item.speed) else "Not measured", Modifier.weight(1f))
            DetailStat("Size", if (item.totalBytes > 0) DownloadsRepository.sizeLabel(item.totalBytes) else "Not sent by the site", Modifier.weight(1f))
            DetailStat("Source", sourceHost(item.url), Modifier.weight(1f))
            DetailStat("Started", timeLabel(item.startedAt), Modifier.weight(1f))
        }
        Box(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    DetailPill(if (item.isExtensionFile) "Install" else "Open", colors.Accent, filled = true) { actions.onOpen(item) }
                    DetailPill("Share", colors.AddressBarText) { actions.onShare(item) }
                    DetailPill("Delete file", Mineral.Ember) { actions.onDelete(item) }
                }
                DownloadStatus.FAILED -> {
                    DetailPill("Retry", colors.Accent, filled = true) { actions.onResume(item) }
                    DetailPill("Remove from list", Mineral.Ember) { actions.onDelete(item) }
                }
                DownloadStatus.CANCELLED -> DetailPill("Remove from list", Mineral.Ember) { actions.onDelete(item) }
            }
            Box(Modifier.weight(1f))
            DetailPill("Copy link", colors.AddressBarText, onClick = onCopyLink)
        }
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

/** A round-cornered pill button: Accent filled when [filled], otherwise a quiet AddressBarBackground pill. */
@Composable
private fun DetailPill(label: String, textColor: Color, filled: Boolean = false, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        modifier = Modifier
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
        DownloadStatus.PAUSED -> "Paused · ${DownloadsRepository.sizeLabel(item.bytes)} of ${if (item.totalBytes > 0) size else "unknown size"}"
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
