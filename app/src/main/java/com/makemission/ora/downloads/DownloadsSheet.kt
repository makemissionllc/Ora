package com.makemission.ora.downloads

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.browserColors

/**
 * Downloads list, shown as a bottom sheet from the phone menu and the tablet toolbar.
 * Running files show progress and can be cancelled; finished files can be opened, shared or deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsSheet(
    repository: DownloadsRepository,
    onDismiss: () -> Unit,
) {
    val items by repository.items.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val finishedCount = items.count { it.status != DownloadStatus.RUNNING }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = browserColors().AddressBarBackground) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Downloads", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
                if (finishedCount > 0) {
                    TextButton(onClick = { confirmClear = true }) {
                        Text("Clear finished", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (items.isEmpty()) {
                Text(
                    "No downloads yet. Files you save from a page appear here and go to Downloads/Ora.",
                    modifier = Modifier.padding(vertical = 16.dp),
                    fontSize = 14.sp,
                    color = browserColors().SecondaryText
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items, key = { it.id }) { item ->
                        DownloadRow(
                            item = item,
                            onCancel = { repository.cancel(item.id) },
                            onOpen = { openFile(context, item) },
                            onShare = { shareFile(context, item) },
                            onDelete = { confirmDelete = item }
                        )
                    }
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
                }) { Text("Delete", color = browserColors().Accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel", color = browserColors().SecondaryText) }
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
                }) { Text("Clear", color = browserColors().Accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Cancel", color = browserColors().SecondaryText) }
            }
        )
    }
}

@Composable
private fun DownloadRow(
    item: DownloadItem,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(browserColors().Surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            item.fileName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = browserColors().AddressBarText
        )
        Text(statusLine(item), fontSize = 12.sp, color = browserColors().SecondaryText)
        if (item.status == DownloadStatus.RUNNING) {
            if (item.progress >= 0f) {
                LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            when (item.status) {
                DownloadStatus.RUNNING -> TextButton(onClick = onCancel) {
                    Text("Cancel", color = browserColors().Accent)
                }
                DownloadStatus.DONE -> {
                    TextButton(onClick = onOpen) { Text("Open", color = browserColors().Accent) }
                    TextButton(onClick = onShare) { Text("Share", color = browserColors().Accent) }
                    TextButton(onClick = onDelete) { Text("Delete", color = browserColors().SecondaryText) }
                }
                DownloadStatus.FAILED, DownloadStatus.CANCELLED -> TextButton(onClick = onDelete) {
                    Text("Remove", color = browserColors().SecondaryText)
                }
            }
        }
    }
}

private fun statusLine(item: DownloadItem): String {
    val size = DownloadsRepository.sizeLabel(item.bytes.takeIf { it > 0 } ?: item.totalBytes)
    return when (item.status) {
        DownloadStatus.RUNNING ->
            if (item.totalBytes > 0) "${DownloadsRepository.sizeLabel(item.bytes)} of $size" else "${DownloadsRepository.sizeLabel(item.bytes)} so far"
        DownloadStatus.DONE -> "Done · $size · Downloads/Ora"
        DownloadStatus.FAILED -> "Failed: ${item.error ?: "unknown error"}"
        DownloadStatus.CANCELLED -> "Cancelled"
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
