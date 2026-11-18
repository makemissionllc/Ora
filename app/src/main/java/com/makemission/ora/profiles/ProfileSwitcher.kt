package com.makemission.ora.profiles

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
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

    // The scroll area spans the full width and the padding sits inside it. Chips scroll to the
    // edge rather than being cut 16 dp in from it, and the last chip keeps its margin at the end.
    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
    Row(
        modifier = Modifier.padding(BrowserTokens.ProfileSwitcherPadding),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(BrowserTokens.ProfileChipSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        profiles.forEach { profile ->
            val selected = profile.id == activeId
            ProfileChip(
                label = profile.name,
                selected = selected,
                background = if (selected) colors.Accent else colors.AddressBarBackground,
                textColor = if (selected) colors.GridBackground else colors.AddressBarText,
                onClick = { onSelect(profile.id) },
                onLongClick = { if (profile.id != ProfilesRepository.DEFAULT_ID) removing = profile },
            )
        }
        ProfileChip(
            label = "+",
            selected = false,
            background = Color.Transparent,
            textColor = colors.SecondaryText,
            outlined = true,
            onClick = { newName = ""; adding = true },
            onLongClick = null,
        )
    }
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

/** One pill: the label is centred both ways inside a fixed-height capsule. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileChip(
    label: String,
    selected: Boolean,
    background: Color,
    textColor: Color,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    outlined: Boolean = false,
) {
    Box(
        modifier = Modifier
            .height(BrowserTokens.ProfileChipHeight)
            .clip(RoundedCornerShape(50))
            .background(background)
            .then(if (outlined) Modifier.border(1.dp, browserColors().SecondaryText.copy(alpha = 0.35f), RoundedCornerShape(50)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(BrowserTokens.ProfileChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            maxLines = 1,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
    }
}
