package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.settings.ShortcutSource
import com.makemission.ora.settings.StartPageConfig
import com.makemission.ora.settings.TileShape
import com.makemission.ora.settings.TileSize
import com.makemission.ora.settings.movedSlot
import com.makemission.ora.settings.normalizePinnedUrl
import com.makemission.ora.settings.withPinned
import com.makemission.ora.settings.withSlotEnabled
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/**
 * The sheet that changes the start page: which parts show and in what order, where the shortcuts come from, how
 * many rows, and the look of the tiles. Every change is saved at once and the page behind the sheet follows, so
 * the result is seen while choosing. Previous component: the Customize button on the start page (the sheet rises
 * on the shared sheet motion, as every phone sheet does).
 */
@Composable
fun StartPageEditor(config: StartPageConfig, settings: SettingsRepository, onDismiss: () -> Unit) {
    val colors = browserColors()
    var confirmReset by remember { mutableStateOf(false) }
    fun save(next: StartPageConfig, what: String) = settings.setStartPage(next, what)

    OraSheet(onDismiss = onDismiss, tag = "start", containerColor = colors.Surface) { _ ->
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Customize start page", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
            Text("Changes show on the page behind this sheet.", fontSize = 13.sp, color = colors.SecondaryText)

            EditorLabel("Parts")
            config.slots.forEachIndexed { index, slot ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(slot.section.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                        Text(slot.section.hint, fontSize = 12.sp, color = colors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    MoveButton(Icons.Filled.KeyboardArrowUp, "Move ${slot.section.label} up", index > 0) {
                        save(config.copy(slots = movedSlot(config.slots, index, -1)), "moved ${slot.section.label} up")
                    }
                    MoveButton(Icons.Filled.KeyboardArrowDown, "Move ${slot.section.label} down", index < config.slots.lastIndex) {
                        save(config.copy(slots = movedSlot(config.slots, index, 1)), "moved ${slot.section.label} down")
                    }
                    MineralSwitch(checked = slot.enabled, label = slot.section.label) {
                        save(config.copy(slots = withSlotEnabled(config.slots, slot.section, it)), "${slot.section.label} ${if (it) "on" else "off"}")
                    }
                }
            }

            EditorLabel("Shortcuts from")
            ChoiceRow(ShortcutSource.entries, config.source, { it.label }) {
                save(config.copy(source = it), "shortcuts from ${it.label}")
            }
            if (config.source == ShortcutSource.PINNED) PinnedEditor(config) { next, what -> save(next, what) }
            EditorLabel("Rows of shortcuts")
            ChoiceRow((StartPageConfig.MIN_ROWS..StartPageConfig.MAX_ROWS).toList(), config.rows, { it.toString() }) {
                save(config.copy(rows = it), "$it rows of shortcuts")
            }

            EditorLabel("Tile shape")
            ChoiceRow(TileShape.entries, config.shape, { it.label }) { save(config.copy(shape = it), "${it.label} tiles") }
            EditorLabel("Tile size")
            ChoiceRow(TileSize.entries, config.size, { it.label }) { save(config.copy(size = it), "${it.label} tile size") }
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).pressFeedback().clickable { save(config.copy(labels = !config.labels), "tile names ${if (!config.labels) "on" else "off"}") },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Show names under tiles", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                MineralSwitch(checked = config.labels, label = "Show names under tiles") { save(config.copy(labels = it), "tile names ${if (it) "on" else "off"}") }
            }

            TextButton(onClick = { confirmReset = true }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Reset to default", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    if (confirmReset) {
        OraDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset the start page?") },
            text = { Text("The parts, order, shortcut source, rows and tile look go back to the default. Your pinned sites are removed too.") },
            confirmButton = {
                TextButton(onClick = { settings.resetStartPage(); confirmReset = false }) {
                    Text("Reset", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel", color = colors.AddressBarText) } },
        )
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun MoveButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = browserColors().AddressBarText)
    }
}

/** A row of equal choices; the picked one is filled with the accent. */
@Composable
private fun <T> ChoiceRow(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    val colors = browserColors()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                    .background(if (on) colors.Accent else colors.AddressBarBackground)
                    .clickable { onPick(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label(option),
                    fontSize = 14.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) colors.GridBackground else colors.AddressBarText,
                )
            }
        }
    }
}

/** The list of pinned sites, with a name and address form to add one. */
@Composable
private fun PinnedEditor(config: StartPageConfig, onChange: (StartPageConfig, String) -> Unit) {
    val colors = browserColors()
    var name by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (config.pinned.isEmpty()) {
            Text("No pinned sites yet. Add one below.", fontSize = 13.sp, color = colors.SecondaryText)
        }
        config.pinned.forEach { site ->
            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(site.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(site.url, fontSize = 12.sp, color = colors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                MoveButton(Icons.Filled.Close, "Remove ${site.title}", true) {
                    onChange(config.copy(pinned = config.pinned - site), "unpinned one site")
                    InAppNotices.show("Removed ${site.title}")
                }
            }
        }
        OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text("Name (optional)") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = address, onValueChange = { address = it }, singleLine = true, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = {
                val url = normalizePinnedUrl(address)
                when {
                    url == null -> InAppNotices.show("Enter an address such as example.com")
                    config.pinned.size >= StartPageConfig.MAX_PINNED -> InAppNotices.show("You can pin up to ${StartPageConfig.MAX_PINNED} sites")
                    else -> {
                        onChange(config.copy(pinned = withPinned(config.pinned, url, name)), "pinned a site")
                        name = ""; address = ""
                    }
                }
            }) { Text("Pin site", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
        }
    }
}
