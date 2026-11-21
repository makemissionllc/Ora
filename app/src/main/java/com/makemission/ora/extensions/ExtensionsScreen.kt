package com.makemission.ora.extensions

import com.makemission.ora.ui.oraRowMotion
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.makemission.ora.ui.OraDialog
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
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.dashedOutline
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
    /** Tablet: the list and an extension's detail sit side by side. Phone: the detail is its own screen. */
    tablet: Boolean = false,
    /** Opens an address in a new tab. "Get extensions" and an extension's own settings page use it. */
    onOpenUrl: (String) -> Unit = {},
) {
    val extensions by repository.extensions.collectAsState()
    val error by repository.lastError.collectAsState()
    val installingNow by repository.installing.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    // The system file picker, limited to what Android lets a browser filter by: any file, then the .xpi check below.
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
            }.getOrNull() ?: uri.lastPathSegment.orEmpty()
            if (name.endsWith(".xpi", ignoreCase = true)) repository.installFromFile(context, uri)
            else {
                BrowserLog.warn("extensions", "A file that isn't an .xpi was picked")
                com.makemission.ora.notice.InAppNotices.show("That isn't an extension file. Pick a file that ends in .xpi.")
            }
        } else BrowserLog.info("extensions", "File picker closed without a choice")
    }
    // An .xpi already saved in Downloads/Ora is offered without a trip through the picker.
    val foundXpi = remember(extensions) { repository.xpiInDownloads(context) }

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<StoreAddon>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var storeError by remember { mutableStateOf<String?>(null) }
    var searchedQuery by remember { mutableStateOf<String?>(null) }
    var address by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<ExtensionInfo?>(null) }
    var filter by remember { mutableStateOf("") }
    var openId by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    // Tablet shows the first extension's detail until another is chosen; phone opens a detail only on a tap.
    val detail = extensions.firstOrNull { it.id == (openId ?: if (tablet) extensions.firstOrNull()?.id else null) }

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

        val listPane: @Composable () -> Unit = {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                com.makemission.ora.browser.SidebarSearch(query = filter, placeholder = "Search installed extensions", pill = true, onQueryChange = { filter = it })
            }
            // Opens Firefox Add-ons in a new tab.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                        .background(browserColors().AddressBarBackground)
                        .clickable { onOpenUrl("https://addons.mozilla.org/android/"); onDone() }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Get extensions", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                    Text("addons.mozilla.org", fontSize = 12.sp, color = browserColors().SecondaryText)
                }
            }
            // An .xpi already in Downloads/Ora. Dashed, because it is a temporary file waiting to be used.
            foundXpi.firstOrNull()?.let { (fileName, uri) ->
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .dashedOutline(RoundedCornerShape(BrowserTokens.CardRadius), browserColors().SecondaryText)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Found in Downloads", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText)
                            Text(fileName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                        }
                        TextButton(onClick = { repository.installFromFile(context, uri) }) {
                            Text("Install", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            // Pick an .xpi with the system file picker.
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                        .background(browserColors().AddressBarBackground)
                        .clickable { picker.launch(arrayOf("*/*")) }
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Install from a file", modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                    Text(".xpi", fontSize = 12.sp, color = browserColors().SecondaryText)
                }
            }
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
                                busy = addon.xpiUrl.trim() in installingNow,
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
                            enabled = address.isNotBlank() && address.trim() !in installingNow
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
                    "Only extensions signed by Mozilla can be installed.",
                    modifier = Modifier.padding(start = 4.dp),
                    fontSize = 12.sp,
                    color = browserColors().SecondaryText
                )
            }
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
                        "No extensions yet. Tap Get extensions, then Add to Firefox on a page, or install from a file.",
                        color = browserColors().SecondaryText,
                        fontSize = 14.sp
                    )
                }
            } else {
                val shown = extensions.filter { filter.isBlank() || it.name.contains(filter.trim(), ignoreCase = true) }
                if (shown.isEmpty()) item { Text("No installed extension matches \"${filter.trim()}\".", color = browserColors().SecondaryText, fontSize = 14.sp) }
                itemsIndexed(shown, key = { _, ext -> ext.id }) { _, extension ->
                    Box(oraRowMotion()) {
                    ExtensionRow(
                        extension = extension,
                        selected = tablet && extension.id == detail?.id,
                        onOpen = { openId = extension.id },
                        onToggle = { enabled -> repository.setEnabled(extension.id, enabled) },
                    )
                    }
                }
            }
        }
        }
        if (tablet) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.weight(0.45f)) { listPane() }
                Box(Modifier.weight(0.55f)) {
                    if (detail != null) ExtensionDetail(detail, repository, onBack = null, onOpenUrl = { onOpenUrl(it); onDone() }, onRemove = { pendingRemoval = detail })
                    else Text("Select an extension to see its details.", color = browserColors().SecondaryText, fontSize = 14.sp)
                }
            }
        } else {
            // The detail grows out of the list and shrinks back into it on the shared overlay timings.
            AnimatedContent(
                targetState = detail?.id,
                transitionSpec = {
                    (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE))
                        .togetherWith(fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE))
                },
                label = "extension detail",
            ) { shownId ->
                val shown = extensions.firstOrNull { it.id == shownId }
                if (shown != null) ExtensionDetail(shown, repository, onBack = { openId = null }, onOpenUrl = { onOpenUrl(it); onDone() }, onRemove = { pendingRemoval = shown })
                else listPane()
            }
        }
    }

    pendingRemoval?.let { extension ->
        OraDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${extension.name}?") },
            text = { Text("It will be uninstalled from Ora. You can install it again later.") },
            confirmButton = {
                TextButton(onClick = {
                    repository.uninstall(extension.id)
                    pendingRemoval = null
                    openId = null
                }) {
                    Text("Remove", color = com.makemission.ora.ui.theme.Mineral.Ember, fontWeight = FontWeight.SemiBold)
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
private fun StoreRow(addon: StoreAddon, installed: Boolean, busy: Boolean, onAdd: () -> Unit) {
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
            // Quality of life: while this add-on installs the button says so and ignores taps.
            TextButton(onClick = onAdd, enabled = !busy) {
                Text(
                    if (busy) "Adding…" else "Add",
                    color = if (busy) browserColors().SecondaryText else browserColors().Accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ExtensionRow(
    extension: ExtensionInfo,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(if (selected) browserColors().SidebarSelected else browserColors().AddressBarBackground)
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
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
    }
}

/**
 * One extension's page: version, the switch, "Allow in private tabs" (off unless turned on), what it can do,
 * its own settings and Remove in Ember. [onBack] is null beside the list on a tablet.
 */
@Composable
private fun ExtensionDetail(
    extension: ExtensionInfo,
    repository: ExtensionsRepository,
    onBack: (() -> Unit)?,
    onOpenUrl: (String) -> Unit,
    onRemove: () -> Unit,
) {
    val colors = browserColors()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier.height(40.dp).clip(RoundedCornerShape(50)).clickable(onClick = onBack).padding(end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = colors.SecondaryText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text("Extensions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
            }
        }
        Text(extension.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
        Text(listOfNotNull(extension.version.takeIf { it.isNotBlank() }?.let { "Version $it" }, if (extension.builtIn) "Built in" else null).joinToString(" · "), fontSize = 13.sp, color = colors.SecondaryText)
        if (extension.description.isNotBlank()) Text(extension.description, fontSize = 14.sp, color = colors.AddressBarText)
        DetailSwitchRow("On", extension.enabled) { repository.setEnabled(extension.id, it) }
        DetailSwitchRow("Allow in private tabs", extension.allowedInPrivate, note = "Off unless you turn it on. Private tabs keep nothing, but the extension may.") {
            repository.setAllowedInPrivate(extension.id, it)
        }
        if (extension.abilities.isNotEmpty()) {
            Text("What it can do", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(colors.AddressBarBackground).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                extension.abilities.forEach { Text(it, fontSize = 14.sp, color = colors.AddressBarText) }
            }
        }
        extension.optionsUrl?.let { url ->
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(colors.AddressBarBackground).clickable { onOpenUrl(url) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { Text("Extension settings", fontSize = 15.sp, color = colors.AddressBarText) }
        }
        if (!extension.builtIn) {
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(colors.AddressBarBackground).clickable(onClick = onRemove).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { Text("Remove", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = com.makemission.ora.ui.theme.Mineral.Ember) }
        }
    }
}

@Composable
private fun DetailSwitchRow(label: String, checked: Boolean, note: String? = null, onChange: (Boolean) -> Unit) {
    val colors = browserColors()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(colors.AddressBarBackground).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
            if (note != null) Text(note, fontSize = 12.sp, color = colors.SecondaryText)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
