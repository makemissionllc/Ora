package com.makemission.ora.extensions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.launch

/**
 * Extension manager. Opened from the phone menu, same overlay motion as Settings and Library.
 * Search and install from Firefox Add-ons, install by address, switch extensions on or off,
 * and remove with a confirmation.
 */
@Composable
fun ExtensionsScreen(
    repository: ExtensionsRepository,
    onDone: () -> Unit,
) {
    val extensions by repository.extensions.collectAsState()
    val error by repository.lastError.collectAsState()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StoreAddon>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var storeError by remember { mutableStateOf<String?>(null) }
    var searchedQuery by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<ExtensionInfo?>(null) }

    fun runSearch() {
        val text = query.trim()
        if (text.isEmpty()) return
        scope.launch {
            searching = true
            storeError = null
            try {
                results = AddonsStore.search(text)
                searchedQuery = text
            } catch (e: Exception) {
                storeError = "Could not reach Firefox Add-ons. Check your connection and try again."
            } finally {
                searching = false
            }
        }
    }

    // An add-on counts as installed when an extension with the same name is already present.
    fun isInstalled(addon: StoreAddon): Boolean =
        extensions.any { it.name.equals(addon.name, ignoreCase = true) }

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

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Store search card (design: the Firefox Add-ons store).
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                        .background(browserColors().AddressBarBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search Firefox Add-ons") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { runSearch() })
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (searching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = browserColors().Accent
                            )
                            Spacer(Modifier.size(8.dp))
                        }
                        TextButton(onClick = { runSearch() }, enabled = query.isNotBlank() && !searching) {
                            Text("Search", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    storeError?.let {
                        Text(it, color = browserColors().SecondaryText, fontSize = 13.sp)
                    }
                }
            }

            // Store results slide and fade in as a group when a search finishes.
            item {
                AnimatedVisibility(
                    visible = results.isNotEmpty(),
                    enter = fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()),
                    exit = fadeOut(OraMotion.exit()) + shrinkVertically(OraMotion.exit())
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Results for \"${searchedQuery.orEmpty()}\"",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = browserColors().SecondaryText
                        )
                        results.forEach { addon ->
                            StoreRow(
                                addon = addon,
                                installed = isInstalled(addon),
                                onAdd = { repository.install(addon.xpiUrl) }
                            )
                        }
                    }
                }
            }
            if (searchedQuery != null && results.isEmpty() && !searching && storeError == null) {
                item {
                    Text("No extensions match \"${searchedQuery}\".", color = browserColors().SecondaryText, fontSize = 14.sp)
                }
            }

            // Install by address, for extensions outside the store.
            item {
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
                        label = { Text("Or install from an .xpi address") }
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
            }

            // Installed extensions.
            item {
                Text(
                    "Installed",
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = browserColors().SecondaryText
                )
            }
            if (extensions.isEmpty()) {
                item {
                    Text(
                        "No extensions installed yet. Search above to add one.",
                        color = browserColors().SecondaryText,
                        fontSize = 14.sp
                    )
                }
            } else {
                itemsIndexed(extensions, key = { _, ext -> ext.id }) { _, extension ->
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
private fun StoreRow(addon: StoreAddon, installed: Boolean, onAdd: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(browserColors().AddressBarBackground)
            .padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                addon.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = browserColors().AddressBarText
            )
            if (addon.summary.isNotBlank()) {
                Text(
                    addon.summary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = browserColors().SecondaryText
                )
            }
        }
        if (installed) {
            Text(
                "Installed",
                modifier = Modifier.padding(horizontal = 12.dp),
                fontSize = 13.sp,
                color = browserColors().SecondaryText
            )
        } else {
            TextButton(onClick = onAdd) {
                Text("Add", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
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
            ).joinToString(" · ")
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
