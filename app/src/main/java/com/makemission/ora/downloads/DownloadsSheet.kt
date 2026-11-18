package com.makemission.ora.downloads

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/** The four filters at the top of the Downloads page. */
private enum class DownloadFilter(val label: String, val emptyText: String) {
    ALL("All", "No downloads yet. Files you save from a page appear here and go to Downloads/Ora."),
    ACTIVE("Active", "Nothing is downloading right now."),
    DONE("Done", "No finished downloads yet."),
    FAILED("Failed", "No failed or cancelled downloads.");

    fun matches(item: DownloadItem): Boolean = when (this) {
        ALL -> true
        ACTIVE -> item.status == DownloadStatus.RUNNING
        DONE -> item.status == DownloadStatus.DONE
        FAILED -> item.status == DownloadStatus.FAILED || item.status == DownloadStatus.CANCELLED
    }
}

/** How many parts the progress bar is drawn in. Each part fills as the file passes its share. */
private const val PROGRESS_PARTS = 4

/**
 * Downloads page, shown as a full-height sheet from the menu and the tablet toolbar.
 * Summary of what is moving, a filter, then Active and Today rows. Running files can be cancelled;
 * finished files open on tap, share on long press, and delete from their trailing button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSheet(
    repository: DownloadsRepository,
    onDismiss: () -> Unit,
) {
    val all by repository.items.collectAsState()
    val context = LocalContext.current
    val colors = browserColors()
    var filter by rememberSaveable { mutableStateOf(DownloadFilter.ALL) }
    var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val running = all.filter { it.status == DownloadStatus.RUNNING }
    val finishedCount = all.size - running.size
    val visible = all.filter { filter.matches(it) }
    val activeRows = visible.filter { it.status == DownloadStatus.RUNNING }
    val otherRows = visible.filterNot { it.status == DownloadStatus.RUNNING }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.GridBackground,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BrowserTokens.RadiusSmall),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "header") { PageHeader(onDone = onDismiss) }
            item(key = "summary") { SummaryCard(running) }
            item(key = "filter") { FilterTrack(filter) { filter = it } }

            if (visible.isEmpty()) {
                item(key = "empty") {
                    Text(
                        filter.emptyText,
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp),
                        fontSize = 14.sp,
                        color = colors.SecondaryText,
                    )
                }
            }
            if (activeRows.isNotEmpty()) {
                item(key = "label-active") { SectionLabel("Active") }
                items(activeRows, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onCancel = { repository.cancel(item.id) },
                        onOpen = { openFile(context, item) },
                        onShare = { shareFile(context, item) },
                        onDelete = { confirmDelete = item },
                    )
                }
            }
            if (otherRows.isNotEmpty()) {
                item(key = "label-today") {
                    SectionLabel("Today") {
                        if (finishedCount > 0) {
                            TextButton(onClick = { confirmClear = true }) {
                                Text("Clear finished", color = Mineral.Ember, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                items(otherRows, key = { it.id }) { item ->
                    DownloadRow(
                        item = item,
                        onCancel = { repository.cancel(item.id) },
                        onOpen = { openFile(context, item) },
                        onShare = { shareFile(context, item) },
                        onDelete = { confirmDelete = item },
                    )
                }
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
    val title = if (running.isEmpty()) {
        "No downloads running"
    } else {
        val speed = running.sumOf { it.speed }
        "${running.size} downloading" + if (speed > 0) ", ${speedLabel(speed)}" else ""
    }
    val subtitle = if (running.isEmpty()) {
        "Files you save from a page appear here."
    } else {
        val known = running.all { it.totalBytes > 0 }
        if (known) {
            val left = running.sumOf { it.totalBytes - it.bytes }
            "${DownloadsRepository.sizeLabel(left)} left to download"
        } else {
            "Some sizes are not known yet"
        }
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
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
            Text(subtitle, fontSize = 13.sp, color = colors.SecondaryText)
        }
    }
}

/** Four equal pills in a track: the selected one is filled with Accent. */
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) colors.Accent else androidx.compose.ui.graphics.Color.Transparent)
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadRow(
    item: DownloadItem,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = browserColors()
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                // Only finished files can be opened or shared; other rows do nothing on tap.
                onClick = { if (item.status == DownloadStatus.DONE) onOpen() },
                onLongClick = { if (item.status == DownloadStatus.DONE) onShare() },
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
            if (item.status == DownloadStatus.RUNNING) SegmentBar(item.progress)
            Text(
                statusLine(item),
                fontSize = 13.sp,
                color = if (item.status == DownloadStatus.FAILED) Mineral.Ember else colors.SecondaryText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (item.status) {
            DownloadStatus.RUNNING -> IconTile(Icons.Outlined.Close, "Cancel download", onCancel)
            DownloadStatus.DONE -> IconTile(Icons.Outlined.Delete, "Delete file", onDelete)
            DownloadStatus.FAILED, DownloadStatus.CANCELLED -> IconTile(Icons.Outlined.Delete, "Remove from list", onDelete)
        }
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

/** Flat bar split into parts. Each part fills as the file passes its share of the whole. */
@Composable
private fun SegmentBar(progress: Float) {
    val colors = browserColors()
    Row(
        modifier = Modifier.fillMaxWidth().height(6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        repeat(PROGRESS_PARTS) { part ->
            val fill = if (progress < 0f) 0f else (progress * PROGRESS_PARTS - part).coerceIn(0f, 1f)
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
private fun IconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .size(BrowserTokens.RowIconSize)
            .clip(CircleShape)
            .background(colors.AddressBarBackground)
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

/** Opens a finished file with whichever app handles its type. */
private fun openFile(context: Context, item: DownloadItem) {
    val uri = item.contentUri ?: return
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, item.mimeType ?: "*/*")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Open with")) }
        .onFailure { BrowserLog.error("download", "No app can open ${item.fileName}") }
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
