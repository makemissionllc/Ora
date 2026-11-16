package com.makemission.ora.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * Extension manager. Opened from the phone menu, same overlay motion as Settings and Library.
 * Install by address, switch each extension on or off, and remove with a confirmation.
 */
@Composable
fun ExtensionsScreen(
    repository: ExtensionsRepository,
    onDone: () -> Unit,
) {
    val extensions by repository.extensions.collectAsState()
    val error by repository.lastError.collectAsState()
    var address by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<ExtensionInfo?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Extensions", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                .background(browserColors().AddressBarBackground)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = address,
                onValueChange = {
                    address = it
                    repository.clearError()
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Extension address (.xpi)") }
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = {
                        repository.install(address)
                        address = ""
                    },
                    enabled = address.isNotBlank()
                ) {
                    Text("Install", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                }
            }
            error?.let {
                Text(it, color = browserColors().SecondaryText, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (extensions.isEmpty()) {
            Text(
                "No extensions installed. Paste an .xpi address above to add one.",
                color = browserColors().SecondaryText,
                fontSize = 14.sp
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(extensions, key = { it.id }) { extension ->
                    ExtensionRow(
                        extension = extension,
                        onToggle = { enabled -> repository.setEnabled(extension.id, enabled) },
                        onRemove = { pendingRemoval = extension }
                    )
                }
            }
        }
    }

    pendingRemoval?.let { extension ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${extension.name}?") },
            text = { Text("It will be uninstalled from Ora. You can install it again later.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.uninstall(extension.id)
                    pendingRemoval = null
                }) {
                    Text("Remove", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) {
                    Text("Cancel", color = browserColors().SecondaryText)
                }
            }
        )
    }
}

@Composable
private fun ExtensionRow(
    extension: ExtensionInfo,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(browserColors().AddressBarBackground)
            .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                extension.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = browserColors().AddressBarText
            )
            val subtitle = listOf(
                extension.version.takeIf { it.isNotBlank() }?.let { "v$it" },
                if (extension.enabled) "On" else "Off",
            ).filterNotNull().joinToString(" · ")
            Text(subtitle, fontSize = 12.sp, color = browserColors().SecondaryText)
        }
        Switch(checked = extension.enabled, onCheckedChange = onToggle)
        if (!extension.builtIn) {
            IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove ${extension.name}", tint = browserColors().SecondaryText)
            }
        }
    }
}
