package com.makemission.ora.browser

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import com.makemission.ora.ui.OraDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/** Which group sheet is open. The tab overview and the tablet sidebar each keep one. */
@Stable
class GroupDialogState {
    /** Long-pressed tab: shows "Add to group". */
    var addTo by mutableStateOf<BrowserTab?>(null)
    /** Group being renamed or recoloured. */
    var editing by mutableStateOf<TabGroup?>(null)
    /** "New group" editor is open; [pendingTabId] is the tab to put in it once saved, or null. */
    var creating by mutableStateOf(false)
    var pendingTabId by mutableStateOf<Long?>(null)
    /** Group waiting for the Close group confirmation. */
    var closing by mutableStateOf<TabGroup?>(null)

    fun newGroup() { pendingTabId = null; creating = true }
}

/** The sheets and the confirmation behind [GroupDialogState]. Place once near the top of a screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDialogs(tabs: BrowserTabs, state: GroupDialogState, onGroupClosed: () -> Unit = {}) {
    val colors = browserColors()
    state.addTo?.let { tab ->
        OraSheet(onDismiss = { state.addTo = null }, tag = "groups", containerColor = colors.Surface) { _ ->
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Add to group", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
                if (tabs.groups.isEmpty()) {
                    Text("No groups yet. Make one to keep related tabs together.", fontSize = 13.sp, color = colors.SecondaryText, modifier = Modifier.padding(vertical = 6.dp))
                }
                tabs.groups.forEach { group ->
                    SheetRow(group.name, dot = group.color, checked = tab.groupId == group.id) {
                        tabs.moveToGroup(tab.id, group.id); state.addTo = null
                    }
                }
                SheetRow("New group", icon = true) { state.pendingTabId = tab.id; state.addTo = null; state.creating = true }
                if (tab.groupId != null) SheetRow("Remove from group", ember = true) { tabs.moveToGroup(tab.id, null); state.addTo = null }
            }
        }
    }
    if (state.creating) {
        GroupEditorSheet(
            initial = null,
            onDismiss = { state.creating = false },
            onSave = { name, color ->
                val group = tabs.addGroup(name, color)
                state.pendingTabId?.let { tabs.moveToGroup(it, group.id) }
                state.creating = false
            },
        )
    }
    state.editing?.let { group ->
        GroupEditorSheet(
            initial = group,
            onDismiss = { state.editing = null },
            onSave = { name, color -> tabs.updateGroup(group.id, name, color); state.editing = null },
        )
    }
    state.closing?.let { group ->
        val count = tabs.tabsIn(group.id).size
        OraDialog(
            onDismissRequest = { state.closing = null },
            title = { Text("Close ${group.name}?") },
            text = { Text(if (count == 0) "The group is empty and will be removed." else "Its $count ${if (count == 1) "tab" else "tabs"} close with it. You can bring back the last 10 from \"Reopen closed tab\".") },
            confirmButton = {
                TextButton(onClick = { tabs.closeGroup(group.id); state.closing = null; onGroupClosed() }) {
                    Text("Close group", color = Mineral.Ember, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = { TextButton(onClick = { state.closing = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SheetRow(label: String, dot: GroupColor? = null, checked: Boolean = false, icon: Boolean = false, ember: Boolean = false, onClick: () -> Unit) {
    val colors = browserColors()
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (dot != null) Box(Modifier.size(12.dp).clip(CircleShape).background(dot.color))
        if (icon) Icon(Icons.Filled.Add, contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp, color = if (ember) Mineral.Ember else colors.AddressBarText)
        if (checked) Icon(Icons.Filled.Check, contentDescription = "In this group", tint = colors.Accent, modifier = Modifier.size(18.dp))
    }
}

/** Name field and the six colours, each shown with its name. The selected colour is a Chalk fill. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun GroupEditorSheet(initial: TabGroup?, onDismiss: () -> Unit, onSave: (String, GroupColor) -> Unit) {
    val colors = browserColors()
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var color by remember { mutableStateOf(initial?.color ?: GroupColor.LICHEN) }
    OraSheet(onDismiss = onDismiss, tag = "groups", containerColor = colors.Surface) { _ ->
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(if (initial == null) "New group" else "Edit group", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Name") }, modifier = Modifier.fillMaxWidth())
            Text("Colour", fontSize = 13.sp, color = colors.SecondaryText)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GroupColor.entries.forEach { option ->
                    val picked = option == color
                    Row(
                        modifier = Modifier.height(44.dp).clip(RoundedCornerShape(50))
                            .background(if (picked) colors.Accent else colors.AddressBarBackground)
                            .clickable { color = option }.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(option.color))
                        Text(option.label, fontSize = 14.sp, fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Normal, color = if (picked) colors.GridBackground else colors.AddressBarText)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = colors.SecondaryText) }
                Box(Modifier.weight(1f))
                Box(
                    Modifier.height(48.dp).clip(RoundedCornerShape(24.dp)).background(colors.Accent).clickable { onSave(name, color) }.padding(horizontal = 26.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (initial == null) "Create" else "Save", color = colors.GridBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** The ⋯ menu of a group: Rename, Colour, Ungroup and Close group (Ember). */
@Composable
fun GroupMenu(expanded: Boolean, onDismiss: () -> Unit, group: TabGroup, tabs: BrowserTabs, state: GroupDialogState, onUngrouped: () -> Unit = {}) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = browserColors().Surface) {
        DropdownMenuItem(text = { Text("Rename") }, onClick = { onDismiss(); state.editing = group })
        DropdownMenuItem(text = { Text("Colour") }, onClick = { onDismiss(); state.editing = group })
        DropdownMenuItem(text = { Text("Ungroup") }, onClick = { onDismiss(); tabs.ungroup(group.id); onUngrouped() })
        DropdownMenuItem(text = { Text("Close group", color = Mineral.Ember) }, onClick = { onDismiss(); state.closing = group })
    }
}
