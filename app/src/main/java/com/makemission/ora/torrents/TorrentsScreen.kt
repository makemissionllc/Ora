package com.makemission.ora.torrents

import com.makemission.ora.ui.pressFeedback
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.makemission.ora.browser.OraSheet
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

private fun report(result: AddResult) = when (result) {
    AddResult.ADDED -> InAppNotices.show("Torrent added")
    AddResult.DUPLICATE -> InAppNotices.show("That torrent is already in your list")
    AddResult.INVALID -> InAppNotices.show("That is not a valid torrent")
}

/**
 * The confirmation card for a magnet link or .torrent file that arrived from a page, another app or the paste button.
 * Previous component: the tapped link or the menu row that led to it (the card grows out of the last press, via OraDialog).
 */
@Composable
fun AddTorrentDialog(offer: TorrentOffer, engine: TorrentEngine, onDone: () -> Unit) {
    val colors = browserColors()
    val (title, detail) = when (offer) {
        is TorrentOffer.Magnet -> (offer.link.name ?: "Torrent ${offer.link.infoHash.take(8)}") to "Magnet link  ·  ${offer.link.trackers} trackers"
        is TorrentOffer.File -> offer.name.ifBlank { "Torrent file" } to "Torrent file  ·  ${TorrentFormat.bytes(offer.bytes.size.toLong())}"
    }
    OraDialog(
        onDismissRequest = { TorrentOffers.consume(); onDone() },
        title = { Text("Add this torrent?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(detail, fontSize = 13.sp, color = colors.SecondaryText)
                Text(
                    "Torrents share a connection with other people, so they can see your network address while it runs. Ora saves the files in its own folder and, by default, only uses Wi-Fi.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = colors.SecondaryText,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { report(engine.add(offer)); TorrentOffers.consume(); onDone() }) {
                Text("Add", color = colors.Accent, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = { TorrentOffers.consume(); onDone() }) { Text("Cancel", color = colors.SecondaryText) } },
    )
}

/**
 * The Torrents page: add a magnet link (typed or pasted), see every torrent with its progress, pause, resume or remove it.
 * Phone: a bottom sheet that drags up. Tablet: a full-screen overlay. Both grow out of [origin], the menu row or toolbar button.
 * Quality of life: a Paste button that takes a magnet link from the clipboard, and a "already in your list" check.
 */
@Composable
fun TorrentsScreen(engine: TorrentEngine, tablet: Boolean, origin: TransformOrigin, onDismiss: () -> Unit) {
    val colors = browserColors()
    val visibleState = remember { MutableTransitionState(false).apply { targetState = true } }
    LaunchedEffect(visibleState.isIdle, visibleState.currentState) {
        if (visibleState.isIdle && !visibleState.currentState) onDismiss()
    }
    if (tablet) Dialog(onDismissRequest = { visibleState.targetState = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = origin),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = origin),
        ) {
            Box(Modifier.fillMaxSize().background(colors.GridBackground).statusBarsPadding().navigationBarsPadding(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 720.dp)) { TorrentsPage(engine) { visibleState.targetState = false } }
            }
        }
    } else OraSheet(onDismiss = onDismiss, tag = "torrents", containerColor = colors.GridBackground, fill = true) { close ->
        Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) { TorrentsPage(engine, close) }
    }
}

@Composable
private fun TorrentsPage(engine: TorrentEngine, onDone: () -> Unit) {
    val colors = browserColors()
    val context = LocalContext.current
    val rows by engine.rows.collectAsState()
    var typed by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<TorrentRow?>(null) }
    LaunchedEffect(Unit) { engine.ensureStarted() }

    fun addFrom(text: String) {
        val link = MagnetLink.parse(text)
        if (link == null) { InAppNotices.show("That is not a magnet link"); return }
        report(engine.add(TorrentOffer.Magnet(link)))
        typed = ""
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Torrents", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText, modifier = Modifier.weight(1f))
            TextButton(onClick = onDone) { Text("Done", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).background(colors.AddressBarBackground).padding(start = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                if (typed.isEmpty()) Text("Paste a magnet link", fontSize = 14.sp, color = colors.SecondaryText)
                BasicTextField(
                    value = typed, onValueChange = { typed = it }, singleLine = true,
                    textStyle = TextStyle(color = colors.AddressBarText, fontSize = 14.sp),
                    cursorBrush = SolidColor(colors.Accent),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { addFrom(typed) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(onClick = {
                val clip = context.getSystemService(ClipboardManager::class.java)?.primaryClip?.getItemAt(0)?.text?.toString()
                if (clip.isNullOrBlank()) InAppNotices.show("The clipboard is empty") else addFrom(clip)
            }) { Text("Paste", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
            TextButton(enabled = typed.isNotBlank(), onClick = { addFrom(typed) }) { Text("Add", color = if (typed.isNotBlank()) colors.Accent else colors.SecondaryText, fontWeight = FontWeight.SemiBold) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (rows.isEmpty()) "" else "${TorrentFormat.rate(rows.sumOf { it.downRate })} down  ·  ${TorrentFormat.rate(rows.sumOf { it.upRate })} up",
                fontSize = 12.sp, color = colors.SecondaryText, modifier = Modifier.weight(1f),
            )
            if (rows.any { it.finished }) TextButton(onClick = { engine.clearFinished(false); InAppNotices.show("Finished torrents cleared. The files stay.") }) {
                Text("Clear finished", color = colors.SecondaryText, fontSize = 13.sp)
            }
        }
        if (rows.isEmpty()) {
            Text(
                "No torrents yet. Tap a magnet link on a page, or paste one above.",
                fontSize = 14.sp, lineHeight = 19.sp, color = colors.SecondaryText, modifier = Modifier.padding(top = 24.dp, start = 4.dp),
            )
        } else LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(rows, key = { it.hash }) { row -> TorrentRowCard(row, onToggle = { engine.setPaused(row.hash, !row.paused) }, onRemove = { removing = row }) }
            item { Box(Modifier.height(24.dp)) }
        }
    }
    removing?.let { row ->
        OraDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${row.name.take(40)}?") },
            text = { Text(if (row.finished) "It leaves the list. You can also delete the downloaded files." else "Stops it and removes it from the list. You can also delete what was downloaded so far (${TorrentFormat.bytes(row.done)}).", fontSize = 14.sp, color = colors.SecondaryText) },
            confirmButton = { TextButton(onClick = { engine.remove(row.hash, true); removing = null }) { Text("Delete files too", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { removing = null }) { Text("Cancel", color = colors.SecondaryText) }
                    TextButton(onClick = { engine.remove(row.hash, false); removing = null }) { Text("Keep files", color = colors.Accent) }
                }
            },
        )
    }
}

@Composable
private fun TorrentRowCard(row: TorrentRow, onToggle: () -> Unit, onRemove: () -> Unit) {
    val colors = browserColors()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(colors.Surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(row.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.AddressBarBackground)) {
            Box(Modifier.fillMaxWidth(row.progress.coerceIn(0f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(if (row.finished) colors.SecondaryText else colors.Accent))
        }
        val line = buildString {
            append(row.state)
            if (row.total > 0) append("  ·  ${TorrentFormat.bytes(row.done)} of ${TorrentFormat.bytes(row.total)}")
            if (row.downRate > 0 && !row.finished) append("  ·  ${TorrentFormat.rate(row.downRate)}")
            row.eta?.let { append("  ·  $it") }
            if (row.state != "Paused" && row.state != "Done") append("  ·  ${row.peers} peers")
        }
        Text(line, fontSize = 12.sp, color = colors.SecondaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onToggle) { Text(if (row.paused) "Resume" else "Pause", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
            TextButton(onClick = onRemove) { Text("Remove", color = Mineral.Ember) }
        }
    }
}
