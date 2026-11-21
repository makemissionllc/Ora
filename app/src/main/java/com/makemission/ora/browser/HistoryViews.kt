package com.makemission.ora.browser

import com.makemission.ora.ui.oraRowMotion
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.history.HistoryEntry
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Where the list and its Clear history button are on screen. Read when Clear history is tapped. */
private class ListGeometry {
    var origin = Offset.Zero
    var size = IntSize.Zero
    var clearCentre = Offset.Zero
}

/**
 * Browsing history grouped by day, newest first. Used by the tablet sidebar and the phone Library.
 * [query] filters by title or address. Removing one entry is immediate; Clear history asks first.
 */
@Composable
internal fun HistoryList(
    entries: List<HistoryEntry>,
    query: String,
    historyOff: Boolean,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    val needle = query.trim()
    val shown = entries.filter {
        needle.isEmpty() || it.title.contains(needle, true) || it.url.contains(needle, true)
    }
    val today = LocalDate.now()
    val groups = shown.groupBy { dayLabel(it.visitedAt, today) }
    val geometry = remember { ListGeometry() }
    var confirmClear by remember { mutableStateOf(false) }
    var clearOrigin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            geometry.origin = coords.positionInWindow()
            geometry.size = coords.size
        }
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            // Quality of life: say when saving is off, so an empty list does not look like a bug.
            if (historyOff) {
                item(key = "off") {
                    Text(
                        "Saving is off, so new visits are not listed. Turn it on in Settings > Privacy.",
                        color = colors.SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    Text(
                        when {
                            entries.isEmpty() -> "No history yet. Pages you visit show up here."
                            else -> "No history matches \"$needle\""
                        },
                        color = colors.SecondaryText,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp)
                    )
                }
            }
            groups.forEach { (label, rows) ->
                item(key = "day-$label") {
                    Text(
                        label,
                        color = colors.SecondaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 12.dp, top = 10.dp, bottom = 2.dp)
                    )
                }
                items(rows, key = { it.url }) { entry ->
                    Box(oraRowMotion()) { HistoryRow(entry, onOpen = { onOpen(entry.url) }, onRemove = { onRemove(entry.url) }) }
                }
            }
            if (entries.isNotEmpty()) {
                item(key = "clear") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quality of life: say what history never holds, so private browsing is not a worry here.
                        Text("Private tabs are never saved.", color = colors.SecondaryText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                // The confirmation grows out of this button, so its position is measured from the list.
                                clearOrigin = geometry.clearCentre - geometry.origin
                                confirmClear = true
                            },
                            modifier = Modifier
                                .height(44.dp)
                                .onGloballyPositioned { coords ->
                                    geometry.clearCentre = coords.positionInWindow() +
                                        Offset(coords.size.width / 2f, coords.size.height / 2f)
                                }
                        ) {
                            Text("Clear history", color = com.makemission.ora.ui.theme.Mineral.Ember, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (confirmClear) {
            ClearHistoryConfirm(
                origin = clearOrigin,
                listSize = geometry.size,
                onConfirm = onClear,
                onDismissed = { confirmClear = false }
            )
        }
    }
}

/**
 * "Clear all history?" over the list. The card grows out of the Clear history button and shrinks back into
 * it on Cancel, a tap outside, or Clear (AGENTS.md rule 10). Back closes only this card, not the sidebar or Library.
 */
@Composable
private fun ClearHistoryConfirm(
    origin: Offset,
    listSize: IntSize,
    onConfirm: () -> Unit,
    onDismissed: () -> Unit,
) {
    val colors = browserColors()
    val density = LocalDensity.current
    val visibleState = remember { MutableTransitionState(false) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    // Remove the card only after its exit animation has finished.
    LaunchedEffect(visibleState.currentState, visibleState.targetState) {
        if (!visibleState.currentState && !visibleState.targetState) onDismissed()
    }
    val close = { visibleState.targetState = false }
    BackHandler(onBack = close)

    // The card is centred over the list, so the button's position gives the pivot inside the card.
    val cardWidthPx = with(density) { BrowserTokens.QuickMenuWidth.toPx() }
    val cardLeft = (listSize.width - cardWidthPx) / 2f
    val cardTop = (listSize.height - cardSize.height) / 2f
    val pivotX = if (cardWidthPx > 0f) ((origin.x - cardLeft) / cardWidthPx).coerceIn(0f, 1f) else 0.5f
    val pivotY = if (cardSize.height > 0) ((origin.y - cardTop) / cardSize.height).coerceIn(0f, 1f) else 0.5f
    val transformOrigin = TransformOrigin(pivotX, pivotY)

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.matchParentSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()),
            exit = fadeOut(OraMotion.overlayFadeOut())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.Scrim)
                    .clickable(onClick = close)
            )
        }
        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(
                initialScale = OraMotion.MORPH_SCALE,
                animationSpec = OraMotion.overlayScaleIn(),
                transformOrigin = transformOrigin
            ),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(
                targetScale = OraMotion.MORPH_SCALE,
                animationSpec = OraMotion.overlayScaleOut(),
                transformOrigin = transformOrigin
            )
        ) {
            Column(
                modifier = Modifier
                    .width(BrowserTokens.QuickMenuWidth)
                    .onSizeChanged { cardSize = it }
                    .clip(RoundedCornerShape(BrowserTokens.QuickMenuRadius))
                    .background(colors.AddressBarBackground)
                    // Taps on the card do not reach the scrim behind it.
                    .clickable(onClick = {})
                    .padding(20.dp)
            ) {
                Text(
                    "Clear all history?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.AddressBarText
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Every saved page is removed from this profile. This can't be undone.",
                    fontSize = 14.sp,
                    color = colors.SecondaryText
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = close) {
                        Text("Cancel", color = colors.Accent)
                    }
                    TextButton(onClick = {
                        close()
                        onConfirm()
                    }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, onOpen: () -> Unit, onRemove: () -> Unit) {
    val colors = browserColors()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SiteChip(entry.title.ifBlank { displayUrl(entry.url) }, entry.url)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                entry.title.ifBlank { displayUrl(entry.url) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.AddressBarText
            )
            Text(
                "${displayUrl(entry.url)}, ${timeOf(entry.visitedAt)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = colors.SecondaryText
            )
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Remove from history", tint = colors.SecondaryText, modifier = Modifier.size(16.dp))
        }
    }
}

/** "Today", "Yesterday", or the date, so each day's pages sit under one heading. */
private fun dayLabel(epochMillis: Long, today: LocalDate): String {
    val day = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    }
}

private fun timeOf(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

/** A rounded square with the site's first letter, tinted by host, as in the Mineral lists. */
@Composable
internal fun SiteChip(title: String, url: String, size: androidx.compose.ui.unit.Dp = 32.dp, radius: androidx.compose.ui.unit.Dp = 9.dp, textSize: androidx.compose.ui.unit.TextUnit = 14.sp) {
    val host = displayUrl(url)
    val tints = com.makemission.ora.ui.theme.Mineral.SiteTints
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(tints[Math.floorMod(host.hashCode(), tints.size)]),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•",
            fontSize = textSize,
            fontWeight = FontWeight.Bold,
            color = com.makemission.ora.ui.theme.Mineral.Ink
        )
    }
}
