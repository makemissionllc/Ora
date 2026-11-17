package com.makemission.ora.profiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * Row of profile chips above the tab overview. Tap switches profile; the + chip adds one;
 * long-press removes a profile other than the default, after asking.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileSwitcher(
    profiles: List<Profile>,
    activeId: String,
    onSelect: (String) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    var adding by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var removing by remember { mutableStateOf<Profile?>(null) }

    Row(
        modifier = modifier
            .padding(BrowserTokens.ProfileSwitcherPadding)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(BrowserTokens.ProfileChipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        profiles.forEach { profile ->
            val selected = profile.id == activeId
            Text(
                profile.name,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) colors.Accent else colors.AddressBarText,
                modifier = Modifier
                    .height(BrowserTokens.ProfileChipHeight)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) colors.SidebarSelected else colors.AddressBarBackground)
                    .combinedClickable(
                        onClick = { onSelect(profile.id) },
                        onLongClick = { if (profile.id != ProfilesRepository.DEFAULT_ID) removing = profile }
                    )
                    .padding(BrowserTokens.ProfileChipPadding)
            )
        }
        Text(
            "+ New profile",
            fontSize = 14.sp,
            color = colors.SecondaryText,
            modifier = Modifier
                .height(BrowserTokens.ProfileChipHeight)
                .clip(RoundedCornerShape(50))
                .background(colors.AddressBarBackground)
                .combinedClickable(onClick = { newName = ""; adding = true })
                .padding(BrowserTokens.ProfileChipPadding),
        )
    }

    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("New profile") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    placeholder = { Text("Name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    adding = false
                    onAdd(newName)
                }) { Text("Add", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { adding = false }) { Text("Cancel", color = colors.SecondaryText) }
            }
        )
    }

    removing?.let { profile ->
        AlertDialog(
            onDismissRequest = { removing = null },
            title = { Text("Remove ${profile.name}?") },
            text = { Text("Its tabs and bookmarks are removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    removing = null
                    onRemove(profile.id)
                }) { Text("Remove", color = colors.CloseBadge, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { removing = null }) { Text("Cancel", color = colors.SecondaryText) }
            }
        )
    }
}
