package com.makemission.ora.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.TABLET_MIN_WIDTH_DP
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.DiagnosticsScreen
import com.makemission.ora.ui.theme.browserColors

/**
 * Settings page. Every setting appears here. Add a new section for each new setting.
 * Opened from the phone menu.
 */
@Composable
fun SettingsScreen(
    settings: SettingsRepository,
    onDone: () -> Unit,
    onClearData: () -> Unit,
) {
    val appearance by settings.appearance.collectAsState()
    val homePage by settings.homePage.collectAsState()
    val websiteTheme by settings.websiteTheme.collectAsState()
    val tabletSidebar by settings.tabletSidebar.collectAsState()
    // Each device gets its own options: tablets see the sidebar and tablet bar, phones see the phone bar.
    val isTablet = LocalConfiguration.current.screenWidthDp >= TABLET_MIN_WIDTH_DP
    val blockPopups by settings.blockPopups.collectAsState()
    val phoneBar by settings.phoneBarStyle.collectAsState()
    val tabletBar by settings.tabletBarStyle.collectAsState()
    val blockAds by settings.blockAds.collectAsState()
    val logEntries by BrowserLog.entries.collectAsState()
    val searchEngines by settings.searchEngines.collectAsState()
    val defaultEngine by settings.defaultSearchEngine.collectAsState()
    var homeDraft by remember(homePage) { mutableStateOf(homePage) }
    var confirmClear by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }

    if (showDiagnostics) {
        DiagnosticsScreen(onDone = { showDiagnostics = false })
        return
    }

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
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                SectionTitle("Appearance")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                ) {
                    AppearanceMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settings.setAppearance(mode) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode.label, fontSize = 15.sp, color = browserColors().AddressBarText)
                            if (mode == appearance) {
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = browserColors().Accent)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settings.setWebsiteTheme(!websiteTheme) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Match app theme on websites", fontSize = 15.sp, color = browserColors().AddressBarText)
                            Text(
                                "Web pages use this app's light or dark colors",
                                fontSize = 12.sp,
                                color = browserColors().SecondaryText
                            )
                        }
                        Switch(checked = websiteTheme, onCheckedChange = { settings.setWebsiteTheme(it) })
                    }
                }
            }
            if (isTablet) item {
                SectionTitle("Tablet")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                        .clickable { settings.setTabletSidebar(!tabletSidebar) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Show tab sidebar", fontSize = 15.sp, color = browserColors().AddressBarText)
                        Text(
                            "The tab and bookmark list on the left. The toolbar button also toggles it.",
                            fontSize = 12.sp,
                            color = browserColors().SecondaryText
                        )
                    }
                    Switch(checked = tabletSidebar, onCheckedChange = { settings.setTabletSidebar(it) })
                }
            }
            if (!isTablet) item {
                SectionTitle("Phone tab bar")
                BarStyleList(selected = phoneBar, onSelect = { settings.setPhoneBarStyle(it) })
                Text(
                    "Scroll down to fold the bars away; scroll up to bring them back.",
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    fontSize = 12.sp,
                    color = browserColors().SecondaryText
                )
            }
            if (isTablet) item {
                SectionTitle("Tablet tab bar")
                BarStyleList(selected = tabletBar, onSelect = { settings.setTabletBarStyle(it) })
                Text(
                    "Swipe the address bar up to open all tabs, or sideways to switch between open tabs.",
                    modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                    fontSize = 12.sp,
                    color = browserColors().SecondaryText
                )
            }
            item {
                SectionTitle("Pop-ups and ads")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settings.setBlockPopups(!blockPopups) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block pop-up windows", fontSize = 15.sp, color = browserColors().AddressBarText)
                            Text(
                                "Stops sites opening windows on their own. When off, pop-ups open as a new tab.",
                                fontSize = 12.sp,
                                color = browserColors().SecondaryText
                            )
                        }
                        Switch(checked = blockPopups, onCheckedChange = { settings.setBlockPopups(it) })
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { settings.setBlockAds(!blockAds) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block ads and trackers", fontSize = 15.sp, color = browserColors().AddressBarText)
                            Text(
                                "Strict tracking protection. Some pages may load more slowly or look different.",
                                fontSize = 12.sp,
                                color = browserColors().SecondaryText
                            )
                        }
                        Switch(checked = blockAds, onCheckedChange = { settings.setBlockAds(it) })
                    }
                }
            }
            item {
                SectionTitle("Home page")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = homeDraft,
                        onValueChange = { homeDraft = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Address") }
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { settings.setHomePage(homeDraft) }) {
                            Text("Save", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            item {
                SectionTitle("Search engines")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                ) {
                    SearchEngine.entries.forEach { engine ->
                        val on = engine in searchEngines
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { settings.setSearchEngineEnabled(engine, !on) }
                                .padding(start = 4.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = on, onCheckedChange = { settings.setSearchEngineEnabled(engine, it) })
                            Text(
                                engine.label,
                                modifier = Modifier.weight(1f),
                                fontSize = 15.sp,
                                color = browserColors().AddressBarText
                            )
                            // Default is chosen among the engines that are on.
                            RadioButton(
                                selected = engine == defaultEngine,
                                enabled = on,
                                onClick = { settings.setDefaultSearchEngine(engine) }
                            )
                        }
                    }
                    Text(
                        "Turn on several to pick one each time you search. The selected dot is the default.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 12.sp,
                        color = browserColors().SecondaryText
                    )
                }
            }
            item {
                SectionTitle("Privacy")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                        .clickable { confirmClear = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Clear browsing data", fontSize = 15.sp, color = browserColors().AddressBarText)
                    Text("›", fontSize = 18.sp, color = browserColors().SecondaryText)
                }
            }
            item {
                SectionTitle("Diagnostics")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(browserColors().AddressBarBackground)
                        .clickable { showDiagnostics = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Log and errors", fontSize = 15.sp, color = browserColors().AddressBarText)
                    Text("${logEntries.size} ›", fontSize = 14.sp, color = browserColors().SecondaryText)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear browsing data?") },
            text = { Text("Cookies, site data and cached files for every site will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearData()
                }) {
                    Text("Clear", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("Cancel", color = browserColors().SecondaryText)
                }
            }
        )
    }
}

@Composable
private fun BarStyleList(selected: BarStyle, onSelect: (BarStyle) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(browserColors().AddressBarBackground)
    ) {
        BarStyle.entries.forEach { style ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(style) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(style.label, fontSize = 15.sp, color = browserColors().AddressBarText)
                    Text(style.detail, fontSize = 12.sp, color = browserColors().SecondaryText)
                }
                RadioButton(selected = style == selected, onClick = { onSelect(style) })
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = browserColors().SecondaryText
    )
}
