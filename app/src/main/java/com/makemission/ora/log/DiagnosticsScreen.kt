package com.makemission.ora.log

import com.makemission.ora.ui.theme.BrowserTokens
import android.content.ClipData
import android.content.ClipboardManager
import com.makemission.ora.notice.InAppNotices
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.browserColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Diagnostics page: the browser log, newest first. Copy puts the whole log on the clipboard;
 * Clear asks first because it cannot be undone.
 */
@Composable
fun DiagnosticsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val entries by BrowserLog.entries.collectAsState()
    val usage by UsageStats.entries.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    var confirmClearUsage by remember { mutableStateOf(false) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val fileStamp = remember { SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US) }

    // Writes the log to a file the user picks, so it can be sent for debugging.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(BrowserLog.asText()) }
                ?: error("Could not open the file")
            BrowserLog.info("log", "Exported ${BrowserLog.entries.value.size} entries")
            InAppNotices.show("Log exported")
        } catch (e: Exception) {
            BrowserLog.error("log", "Log export failed", e)
            InAppNotices.show("Export failed")
        }
    }

    // Back closes this page, not the whole Settings overlay.
    BackHandler(onBack = onDone)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Diagnostics", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Feature use", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
            TextButton(onClick = { confirmClearUsage = true }, enabled = usage.isNotEmpty()) {
                Text("Clear usage", color = browserColors().CloseBadge, fontWeight = FontWeight.SemiBold)
            }
        }
        if (usage.isEmpty()) {
            Text("No feature use counted yet", fontSize = 13.sp, color = browserColors().SecondaryText, modifier = Modifier.padding(vertical = 4.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                    .background(browserColors().AddressBarBackground)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                usage.take(14).forEach { item ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.action, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = browserColors().AddressBarText)
                        Text("${item.count} · ${timeFormat.format(Date(item.lastUsed))}", fontSize = 12.sp, color = browserColors().SecondaryText)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (entries.size == 1) "1 entry" else "${entries.size} entries",
                fontSize = 13.sp,
                color = browserColors().SecondaryText
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard.setPrimaryClip(ClipData.newPlainText("Ora log", BrowserLog.asText()))
                        InAppNotices.show("Log copied")
                    },
                    enabled = entries.isNotEmpty()
                ) {
                    Text("Copy", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { exportLauncher.launch("ora-log-${fileStamp.format(Date())}.txt") }, enabled = entries.isNotEmpty()) {
                    Text("Export", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = { confirmClear = true }, enabled = entries.isNotEmpty()) {
                    Text("Clear", color = browserColors().CloseBadge, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No log entries yet", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Page loads, tab changes, extension and settings events, and errors appear here.",
                    fontSize = 13.sp,
                    color = browserColors().SecondaryText
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(entries.asReversed(), key = { it.time.toString() + it.message }) { entry ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                            .background(browserColors().AddressBarBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            "${timeFormat.format(Date(entry.time))}  ${entry.level.label}  ${entry.tag}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (entry.level == LogLevel.INFO) browserColors().SecondaryText else browserColors().CloseBadge
                        )
                        Text(
                            entry.message,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = browserColors().AddressBarText
                        )
                    }
                }
            }
        }
    }

    if (confirmClearUsage) {
        AlertDialog(
            onDismissRequest = { confirmClearUsage = false },
            title = { Text("Clear feature use?") },
            text = { Text("All feature-use counts will be removed. The log is kept. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClearUsage = false
                    UsageStats.clear()
                    BrowserLog.info("usage", "Feature-use counts cleared")
                }) {
                    Text("Clear", color = browserColors().CloseBadge, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearUsage = false }) {
                    Text("Cancel", color = browserColors().SecondaryText)
                }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear the log?") },
            text = { Text("All ${entries.size} entries will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    BrowserLog.clear()
                }) {
                    Text("Clear", color = browserColors().CloseBadge, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", color = browserColors().SecondaryText)
                }
            }
        )
    }
}
