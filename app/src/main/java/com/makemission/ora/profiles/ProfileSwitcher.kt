package com.makemission.ora.profiles

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.unit.dp
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.ProfileTint
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import com.makemission.ora.ui.OraDialog
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
    onAdd: (String, ProfileTint) -> Unit,
    /** Saves a changed name and tint for a profile. */
    onEdit: (String, String, ProfileTint) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Profile?>(null) }
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
                // Long-press opens the profile page: name, tint and Remove.
                onLongClick = { editing = profile },
                marker = if (selected) null else profile.tint.marker,
            )
        }
        ProfileChip(
            label = "+",
            selected = false,
            background = Color.Transparent,
            textColor = colors.SecondaryText,
            outlined = true,
            onClick = { adding = true },
            onLongClick = null,
        )
    }
    }

    if (adding) {
        ProfileSheet(
            profile = null,
            onDismiss = { adding = false },
            onSave = { name, tint -> adding = false; onAdd(name, tint) },
            onTintPreview = {},
            onRemove = null,
        )
    }
    editing?.let { profile ->
        ProfileSheet(
            profile = profile,
            // Cancel or swipe-away puts back the tint the profile had before the preview.
            onDismiss = { editing = null; onEdit(profile.id, profile.name, profile.tint) },
            onSave = { name, tint -> editing = null; onEdit(profile.id, name, tint) },
            // Quality of life: the colours change while you pick, so you see the tint before you save.
            onTintPreview = { tint -> onEdit(profile.id, profile.name, tint) },
            onRemove = if (profile.id != ProfilesRepository.DEFAULT_ID) ({ editing = null; onEdit(profile.id, profile.name, profile.tint); removing = profile }) else null,
        )
    }

    removing?.let { profile ->
        OraDialog(
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
    /** Small dot before the label: the profile's tint marker. */
    marker: Color? = null,
) {
    Box(
        modifier = Modifier
            .height(BrowserTokens.ProfileChipHeight)
            .pressFeedback().clip(RoundedCornerShape(50))
            .background(background)
            .then(if (outlined) Modifier.border(1.dp, browserColors().SecondaryText.copy(alpha = 0.35f), RoundedCornerShape(50)) else Modifier)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(BrowserTokens.ProfileChipPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            if (marker != null) Box(Modifier.size(8.dp).clip(CircleShape).background(marker))
            Text(
                label,
                fontSize = 14.sp,
                maxLines = 1,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
            )
        }
    }
}


/**
 * The profile page, as a sheet: a name, the five tints and, for an existing profile, Remove. A tint is named as well as
 * coloured, and the selected one is a Chalk fill like every other selected state.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSheet(
    profile: Profile?,
    onDismiss: () -> Unit,
    onSave: (String, ProfileTint) -> Unit,
    onTintPreview: (ProfileTint) -> Unit,
    onRemove: (() -> Unit)?,
) {
    val colors = browserColors()
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var tint by remember { mutableStateOf(profile?.tint ?: ProfileTint.BASALT) }
    com.makemission.ora.browser.OraSheet(onDismiss = onDismiss, tag = "profiles", containerColor = colors.Surface) { _ ->
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
        ) {
            Text(if (profile == null) "New profile" else "Edit profile", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
            )
            Text("Tint", fontSize = 13.sp, color = colors.SecondaryText)
            Text("Changes Ora's own colours for this profile. Web pages are never tinted.", fontSize = 13.sp, color = colors.SecondaryText)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ProfileTint.entries.forEach { option ->
                    val picked = option == tint
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .pressFeedback().clip(RoundedCornerShape(50))
                            .background(if (picked) colors.Accent else colors.AddressBarBackground)
                            .clickable { tint = option; onTintPreview(option) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(option.marker))
                        Text(option.label, fontSize = 14.sp, fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Normal, color = if (picked) colors.GridBackground else colors.AddressBarText)
                    }
                }
            }
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onRemove != null) {
                    TextButton(onClick = onRemove) { Text("Remove", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", color = colors.SecondaryText) }
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .pressFeedback().clip(RoundedCornerShape(50))
                        .background(colors.Accent)
                        .clickable { onSave(name, tint) }
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(if (profile == null) "Add" else "Done", fontWeight = FontWeight.SemiBold, color = colors.GridBackground) }
            }
        }
    }
}
