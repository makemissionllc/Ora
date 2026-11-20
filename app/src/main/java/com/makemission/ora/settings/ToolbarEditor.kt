package com.makemission.ora.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/** The icon for a toolbar button. A saved page shows a filled bookmark. */
fun ToolbarItem.icon(bookmarked: Boolean = false): ImageVector = when (this) {
    ToolbarItem.BACK -> Icons.AutoMirrored.Filled.ArrowBack
    ToolbarItem.FORWARD -> Icons.AutoMirrored.Filled.ArrowForward
    ToolbarItem.TABS -> Icons.Outlined.Tab
    ToolbarItem.SHARE -> Icons.Filled.Share
    ToolbarItem.BOOKMARK -> if (bookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder
    ToolbarItem.HOME -> Icons.Filled.Home
    ToolbarItem.RELOAD -> Icons.Filled.Refresh
    ToolbarItem.NEW_TAB -> Icons.Filled.Add
    ToolbarItem.PRIVATE_TAB -> Icons.Filled.VisibilityOff
    ToolbarItem.FIND -> Icons.Filled.Search
    ToolbarItem.READER -> Icons.AutoMirrored.Outlined.Article
    ToolbarItem.DOWNLOADS -> Icons.Filled.Download
}

private val RowHeight = 52.dp

/**
 * Settings > Appearance > Toolbar for the phone Top layout: a preview, the buttons in the bar (drag the handle to
 * reorder, minus to remove), the buttons that can be added, presets and Reset. Menu is always last.
 */
@Composable
fun ToolbarEditor(settings: SettingsRepository) {
    val colors = browserColors()
    val items by settings.toolbarItems.collectAsState()
    val preset = ToolbarConfig.presetName(items)
    val rowPx = with(LocalDensity.current) { RowHeight.toPx() }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preview: the bar as it will look, with Menu last.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius))
                .background(colors.Surface).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
        ) {
            (items.map { it.icon() } + Icons.Filled.MoreVert).forEach { Icon(it, contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.size(24.dp)) }
        }
        // Presets. Editing a preset switches the chip to Custom.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (ToolbarConfig.PRESETS.map { it.first } + "Custom").forEach { name ->
                val on = name == preset
                Box(
                    Modifier.heightIn48().clip(RoundedCornerShape(24.dp))
                        .background(if (on) colors.Accent else colors.Surface)
                        .clickable(enabled = name != "Custom") { ToolbarConfig.PRESETS.firstOrNull { it.first == name }?.let { settings.setToolbarItems(it.second) } }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = if (on) colors.GridBackground else colors.AddressBarText) }
            }
        }
        Text("In the bar", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface).animateContentSize()) {
            if (items.isEmpty()) Text("Only Menu. Add a button below.", fontSize = 14.sp, color = colors.SecondaryText, modifier = Modifier.padding(16.dp))
            items.forEachIndexed { index, item ->
                key(item) {
                    val currentIndex by rememberUpdatedState(index)
                    val currentItems by rememberUpdatedState(items)
                    var drag by remember { mutableFloatStateOf(0f) }
                    Row(Modifier.fillMaxWidth().height(RowHeight).padding(start = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(48.dp).semantics { contentDescription = "Drag to move ${item.label}" }
                                .pointerInput(item) {
                                    detectVerticalDragGestures(onDragEnd = { drag = 0f }, onDragCancel = { drag = 0f }) { change, dy ->
                                        change.consume()
                                        drag += dy
                                        val list = currentItems.toMutableList()
                                        val i = currentIndex
                                        if (drag > rowPx / 2 && i < list.lastIndex) { list.add(i + 1, list.removeAt(i)); drag -= rowPx; settings.setToolbarItems(list) }
                                        else if (drag < -rowPx / 2 && i > 0) { list.add(i - 1, list.removeAt(i)); drag += rowPx; settings.setToolbarItems(list) }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.DragHandle, contentDescription = null, tint = colors.SecondaryText) }
                        Icon(item.icon(), contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.padding(end = 12.dp))
                        Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(48.dp).clickable { settings.setToolbarItems(items - item) }.semantics { contentDescription = "Remove ${item.label}" },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.Remove, contentDescription = null, tint = Mineral.Ember) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(RowHeight).padding(start = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = colors.SecondaryText, modifier = Modifier.padding(end = 12.dp))
                Text("Menu, always last", fontSize = 15.sp, color = colors.SecondaryText)
            }
        }
        val addable = ToolbarItem.entries.filter { it !in items }
        if (addable.isNotEmpty()) {
            Text("Add a button", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)) {
                val full = items.size >= ToolbarConfig.MAX_BUTTONS
                addable.forEach { item ->
                    key(item) {
                        Row(
                            Modifier.fillMaxWidth().height(RowHeight).clickable(enabled = !full) { settings.setToolbarItems(items + item) }.padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(item.icon(), contentDescription = null, tint = if (full) colors.SecondaryText else colors.AddressBarText, modifier = Modifier.padding(end = 12.dp))
                            Text(item.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = if (full) colors.SecondaryText else colors.AddressBarText, modifier = Modifier.weight(1f))
                            Box(Modifier.size(48.dp).semantics { contentDescription = "Add ${item.label}" }, contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Add, contentDescription = null, tint = colors.SecondaryText)
                            }
                        }
                    }
                }
            }
            if (items.size >= ToolbarConfig.MAX_BUTTONS) Text("The bar holds ${ToolbarConfig.MAX_BUTTONS} buttons. Remove one to add another.", fontSize = 13.sp, color = colors.SecondaryText)
        }
        Text(
            "Reset to Classic", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).clickable { settings.resetToolbar() }.padding(horizontal = 4.dp, vertical = 12.dp),
        )
    }
}

private fun Modifier.heightIn48(): Modifier = this.height(48.dp)
