package com.makemission.ora.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.TABLET_MIN_WIDTH_DP
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.DiagnosticsScreen
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/** Categories shown in the tablet rail. Phones show every section in one list instead. */
private enum class SettingsCategory(val label: String) {
    APPEARANCE("Appearance"),
    TABS("Tabs"),
    PRIVACY("Privacy"),
    SEARCH("Search"),
    HOME("Home page"),
    DIAGNOSTICS("Diagnostics"),
}

/**
 * Settings page. Every setting appears here. Add a new section for each new setting.
 * Opened from the phone menu. Tablets get a category rail beside one centered column.
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
    val askEachSearch by settings.askEachSearch.collectAsState()
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
    var category by rememberSaveable { mutableStateOf(SettingsCategory.APPEARANCE) }

    if (showDiagnostics) {
        DiagnosticsScreen(onDone = { showDiagnostics = false })
        return
    }

    // On tablet only the selected category shows; on phone every section shows.
    fun shown(section: SettingsCategory) = !isTablet || section == category

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

        Row(modifier = Modifier.fillMaxSize()) {
            if (isTablet) {
                SettingsRail(
                    selected = category,
                    onSelect = { category = it },
                    modifier = Modifier.width(BrowserTokens.SettingsRailWidth).fillMaxHeight(),
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    // Cap the width before filling it, so the tablet column stops at the content max.
                    modifier = Modifier
                        .then(if (isTablet) Modifier.widthIn(max = BrowserTokens.SettingsContentMaxWidth) else Modifier)
                        .fillMaxWidth(),
                ) {
                    if (shown(SettingsCategory.APPEARANCE)) item {
                        SectionTitle("Appearance")
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(browserColors().AddressBarBackground)
                        ) {
                            if (isTablet) {
                                // Tablet: one row with a segmented control, as in the Settings mockup.
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = BrowserTokens.SettingsRailRowHeight)
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Theme", fontSize = 15.sp, color = browserColors().AddressBarText)
                                    AppearanceSegments(selected = appearance, onSelect = { settings.setAppearance(it) })
                                }
                            } else {
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
                    if (isTablet && shown(SettingsCategory.TABS)) item {
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
                    if (isTablet && shown(SettingsCategory.TABS)) item {
                        SectionTitle("Tablet tab bar")
                        BarStyleList(selected = tabletBar, onSelect = { settings.setTabletBarStyle(it) })
                        Text(
                            "Swipe the address bar up to open all tabs, or sideways to switch between open tabs.",
                            modifier = Modifier.padding(start = 4.dp, top = 6.dp),
                            fontSize = 12.sp,
                            color = browserColors().SecondaryText
                        )
                    }
                    if (shown(SettingsCategory.PRIVACY)) item {
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
                    if (shown(SettingsCategory.HOME)) item {
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
                    if (shown(SettingsCategory.SEARCH)) item {
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
                                "The selected dot is the default. Searches use it unless the switch below is on.",
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                fontSize = 12.sp,
                                color = browserColors().SecondaryText
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(browserColors().AddressBarBackground)
                                .clickable { settings.setAskEachSearch(!askEachSearch) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Ask which engine each search", fontSize = 15.sp, color = browserColors().AddressBarText)
                                Text(
                                    "When several engines are on, pick one from a sheet each time.",
                                    fontSize = 12.sp,
                                    color = browserColors().SecondaryText
                                )
                            }
                            Switch(checked = askEachSearch, onCheckedChange = { settings.setAskEachSearch(it) })
                        }
                    }
                    if (shown(SettingsCategory.PRIVACY)) item {
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
                    if (shown(SettingsCategory.DIAGNOSTICS)) item {
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

/** Tablet category rail: the selected category gets a card-coloured row, as in the Settings mockup. */
@Composable
private fun SettingsRail(
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(end = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SettingsCategory.entries.forEach { section ->
            val on = section == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BrowserTokens.SettingsRailRowHeight)
                    .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                    .background(if (on) browserColors().AddressBarBackground else Color.Transparent)
                    .clickable { onSelect(section) }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    section.label,
                    fontSize = 15.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) browserColors().Accent else browserColors().AddressBarText,
                )
            }
        }
    }
}

/** Light / Dark / Pure dark / Auto as one segmented control. The selected segment sits on the page surface. */
@Composable
private fun AppearanceSegments(selected: AppearanceMode, onSelect: (AppearanceMode) -> Unit) {
    Row(
        modifier = Modifier
            .width(BrowserTokens.SettingsSegmentWidth)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(browserColors().GridBackground)
            .padding(2.dp),
    ) {
        AppearanceMode.entries.forEach { mode ->
            val on = mode == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (on) browserColors().Surface else Color.Transparent)
                    .clickable { onSelect(mode) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    mode.label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (on) browserColors().AddressBarText else browserColors().SecondaryText,
                )
            }
        }
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
