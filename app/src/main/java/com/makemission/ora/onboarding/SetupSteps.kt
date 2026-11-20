package com.makemission.ora.onboarding

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.MineralSwitch
import com.makemission.ora.browser.PillButton
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.security.AppLockDialog
import com.makemission.ora.settings.AppearanceMode
import com.makemission.ora.settings.BarStyle
import com.makemission.ora.settings.DefaultBrowser
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.ProfileTint
import com.makemission.ora.ui.theme.browserColors

@Composable
internal fun WelcomeStep(tablet: Boolean, onSetUp: () -> Unit, onDefaults: () -> Unit, onSkip: () -> Unit) {
    Box(Modifier.fillMaxSize().background(browserColors().GridBackground), contentAlignment = Alignment.Center) {
        Column(
            Modifier.widthIn600(tablet).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(browserColors().Accent), contentAlignment = Alignment.Center) {
                Text("Ora", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = browserColors().GridBackground)
            }
            Text("Welcome to Ora", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText, textAlign = TextAlign.Center)
            Text(
                "A calm browser that keeps everything on your device. Four quick steps, and you can change all of them later.",
                fontSize = 14.sp, lineHeight = 20.sp, color = browserColors().SecondaryText, textAlign = TextAlign.Center,
            )
            PillButton("Set up Ora", onClick = onSetUp, primary = true)
            TextAction("Use the defaults and start browsing", onDefaults)
        }
        Box(Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) { TextAction("Skip", onSkip) }
    }
}

private fun Modifier.widthIn600(tablet: Boolean): Modifier = this.then(Modifier.width(if (tablet) 480.dp else 360.dp))

// ---- 1 Look ----

@Composable
internal fun LookStep(settings: SettingsRepository, tablet: Boolean, onBack: (() -> Unit)?, onSkip: () -> Unit, onNext: () -> Unit) {
    val phoneBar by settings.phoneBarStyle.collectAsState()
    val tabletBar by settings.tabletBarStyle.collectAsState()
    val appearance by settings.appearance.collectAsState()
    val bar = if (tablet) tabletBar else phoneBar
    SetupScaffold(
        tablet, "How should Ora look?", "Pick where the bar sits and the colours. You can change both in Settings.",
        onBack, onSkip, "Continue", onNext, stepNumber = 1,
        preview = { LookPreview(bar, tablet = true) },
    ) {
        if (tablet) {
            ChoiceCard(bar == BarStyle.COMPACT, "Floating bar", "A floating row at the top with the address bar.", { settings.setTabletBarStyle(BarStyle.COMPACT) }) { MiniBar(BarStyle.COMPACT, true) }
            ChoiceCard(bar == BarStyle.TOP, "Bar with a sidebar", "The bar at the top and a tab sidebar on the left.", { settings.setTabletBarStyle(BarStyle.TOP) }) { MiniBar(BarStyle.TOP, true) }
        } else {
            ChoiceCard(bar == BarStyle.BOTTOM, "Bar at the bottom", "A larger bar at the bottom with tabs, bookmarks and menu.", { settings.setPhoneBarStyle(BarStyle.BOTTOM) }) { MiniBar(BarStyle.BOTTOM, false) }
            ChoiceCard(bar == BarStyle.TOP, "Bar at the top", "The address bar and every control sit at the top.", { settings.setPhoneBarStyle(BarStyle.TOP) }) { MiniBar(BarStyle.TOP, false) }
            ChoiceCard(bar == BarStyle.COMPACT, "Compact", "Only the address bar at the bottom, with a ⋯ menu.", { settings.setPhoneBarStyle(BarStyle.COMPACT) }) { MiniBar(BarStyle.COMPACT, false) }
        }
        Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText, modifier = Modifier.padding(top = 4.dp))
        val modes = listOf(AppearanceMode.AUTO, AppearanceMode.LIGHT, AppearanceMode.DARK, AppearanceMode.PURE_DARK)
        PillChoices(modes.map { it.label }, modes.indexOf(appearance)) { settings.setAppearance(modes[it]) }
    }
}

/** A small drawing of a screen with the bar where [style] puts it. Colours come from the palette, so a theme change shows at once. */
@Composable
private fun MiniBar(style: BarStyle, tablet: Boolean) {
    val c = browserColors()
    val w = if (tablet) 84.dp else 44.dp
    val h = if (tablet) 60.dp else 72.dp
    Box(Modifier.size(w, h).clip(RoundedCornerShape(8.dp)).background(c.GridBackground).border(BorderStroke(1.dp, c.ToolbarBorder), RoundedCornerShape(8.dp))) {
        if (tablet && style == BarStyle.TOP) Box(Modifier.align(Alignment.CenterStart).width(18.dp).fillMaxSize().background(c.SidebarBackground))
        val barModifier = when {
            style == BarStyle.BOTTOM -> Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(16.dp)
            style == BarStyle.TOP -> Modifier.align(Alignment.TopCenter).padding(start = if (tablet) 18.dp else 0.dp).fillMaxWidth().height(12.dp)
            else -> Modifier.align(if (tablet) Alignment.TopCenter else Alignment.BottomCenter).padding(6.dp).fillMaxWidth().height(10.dp)
        }
        Box(barModifier.clip(if (style == BarStyle.COMPACT) CircleShape else RoundedCornerShape(0.dp)).background(c.ToolbarBackground))
    }
}

@Composable
private fun LookPreview(bar: BarStyle, tablet: Boolean) {
    val c = browserColors()
    Box(Modifier.fillMaxWidth().heightIn(max = 360.dp).height(300.dp).clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(c.GridBackground).border(BorderStroke(1.dp, c.ToolbarBorder), RoundedCornerShape(BrowserTokens.CardRadius))) {
        if (bar == BarStyle.TOP) Box(Modifier.align(Alignment.CenterStart).width(80.dp).fillMaxSize().background(c.SidebarBackground))
        Column(Modifier.align(Alignment.Center).padding(start = if (bar == BarStyle.TOP) 80.dp else 0.dp).padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth(0.7f).height(14.dp).clip(CircleShape).background(c.Surface))
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.Surface))
            Box(Modifier.fillMaxWidth(0.85f).height(10.dp).clip(CircleShape).background(c.Surface))
        }
        val m = if (bar == BarStyle.TOP) Modifier.align(Alignment.TopCenter).padding(start = 80.dp).fillMaxWidth().height(36.dp)
        else Modifier.align(Alignment.TopCenter).padding(12.dp).fillMaxWidth().height(32.dp)
        Box(m.clip(if (bar == BarStyle.TOP) RoundedCornerShape(0.dp) else CircleShape).background(c.ToolbarBackground), contentAlignment = Alignment.CenterStart) {
            Text("  ora.example", fontSize = 12.sp, color = c.SecondaryText)
        }
    }
}

// ---- 2 Blocking ----

@Composable
internal fun BlockingStep(settings: SettingsRepository, tablet: Boolean, onBack: (() -> Unit)?, onSkip: () -> Unit, onNext: () -> Unit) {
    val strict by settings.fingerprintingProtection.collectAsState()
    val httpsOnly by settings.httpsOnly.collectAsState()
    val lockRecord by settings.appLockRecord.collectAsState()
    var lockDialog by remember { mutableStateOf(false) }
    var lockWasOff by remember { mutableStateOf(false) }
    SetupScaffold(
        tablet, "What should Ora block?", "Standard keeps pages working. Strict also hides details sites use to recognise your device.",
        onBack, onSkip, "Continue", onNext, stepNumber = 2,
        preview = { BlockingDetails(strict) },
    ) {
        PillChoices(listOf("Standard", "Strict"), if (strict) 1 else 0) { settings.setFingerprintingProtection(it == 1) }
        if (!tablet) BlockingDetails(strict)
        SettingRow("HTTPS-only", "Ask before opening a page that isn't encrypted.") {
            MineralSwitch(checked = httpsOnly, label = "HTTPS-only", onChange = { settings.setHttpsOnly(it) })
        }
        SettingRow("App lock", "Ask for a PIN when you open Ora.") {
            MineralSwitch(checked = lockRecord != null, label = "App lock", onChange = { lockWasOff = lockRecord == null; lockDialog = true })
        }
    }
    if (lockDialog) {
        AppLockDialog(settings = settings, turningOff = lockRecord != null, onDismiss = {
            lockDialog = false
            // Turning the lock on went through Set a PIN; carry on to Profile.
            if (lockWasOff && settings.appLockRecord.value != null) onNext()
        })
    }
}

@Composable
private fun BlockingDetails(strict: Boolean) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(browserColors().Surface).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(if (strict) "Strict" else "Standard", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
        Text(
            if (strict) "Fingerprinting protection is on. Some sites may look slightly different."
            else "Fingerprinting protection is off. Pages work as they do in most browsers.",
            fontSize = 13.sp, lineHeight = 18.sp, color = browserColors().SecondaryText,
        )
    }
}

// ---- 3 Profile ----

@Composable
internal fun ProfileStep(profiles: ProfilesRepository, tablet: Boolean, onBack: (() -> Unit)?, onSkip: () -> Unit, onNext: () -> Unit) {
    val list by profiles.profiles.collectAsState()
    val activeId by profiles.activeId.collectAsState()
    val active = list.firstOrNull { it.id == activeId } ?: list.first()
    var name by remember(active.id) { mutableStateOf(active.name) }
    // The name saves when the step is left, however it is left (Continue, Back, Skip or a new step).
    val nameNow = androidx.compose.runtime.rememberUpdatedState(name)
    DisposableEffect(active.id) { onDispose { profiles.rename(active.id, nameNow.value) } }
    SetupScaffold(
        tablet, "Your first profile", "A profile keeps its own tabs, bookmarks and history. Add more for work or school.",
        onBack, onSkip, "Continue", { profiles.rename(active.id, nameNow.value); onNext() }, stepNumber = 3,
        preview = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.forEach { p ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(browserColors().Surface).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(p.tint.marker))
                        Text(if (p.id == active.id) name.ifBlank { p.name } else p.name, fontSize = 15.sp, color = browserColors().AddressBarText)
                    }
                }
            }
        },
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(24) },
            singleLine = true,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
        )
        Text("Colour", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfileTint.entries.forEach { t ->
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(t.marker)
                        .border(BorderStroke(if (t == active.tint) 3.dp else 0.dp, browserColors().Accent), CircleShape)
                        .clickable { profiles.setTint(active.id, t) },
                )
            }
        }
        Text("Add another", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Work", "School", "Other").forEach { label ->
                val exists = list.any { it.name.equals(label, ignoreCase = true) }
                Box(
                    Modifier.heightIn(min = 44.dp).clip(CircleShape)
                        .background(if (exists) browserColors().SidebarSelected else browserColors().AddressBarBackground)
                        .clickable(enabled = !exists) {
                            // Adding makes the new profile active; put the first one back so setup stays on it.
                            val keep = profiles.activeId.value
                            profiles.add(label)
                            profiles.select(keep)
                        }
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (exists) "✓ $label" else "+ $label", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                }
            }
        }
    }
}

// ---- 4 Discover ----

/** One thing worth knowing about, with where it lives. [tabletOnly] rows need a hardware keyboard, which is mostly a tablet thing. */
private data class Feature(val title: String, val detail: String, val tabletOnly: Boolean = false, val phoneOnly: Boolean = false)

private val Features = listOf(
    Feature("Quick switcher", "Find any open tab, bookmark, history page or action from one box. Press Ctrl+K, or choose Quick switcher in the ⋯ menu."),
    Feature("Keyboard shortcuts", "Ctrl+T, Ctrl+W, Ctrl+L, Ctrl+Plus and many more work with a keyboard. Press F1 to see and search the list.", tabletOnly = true),
    Feature("Text size for each site", "Make one site bigger or smaller without touching the rest. Ctrl+Plus, Ctrl+Minus and Ctrl+0 on a page.", tabletOnly = true),
    Feature("Your own bottom bar", "Choose, reorder and remove the buttons. Press and hold an empty part of the bar, or open Settings > Appearance.", phoneOnly = true),
    Feature("Tab groups", "Keep related tabs together. A strip of the group's tabs shows by the bar, and you can open links straight into a group."),
    Feature("Labs", "Switch on extras such as a site card behind the padlock with the certificate and the trackers blocked. Settings > Labs."),
)

@Composable
internal fun DiscoverStep(settings: SettingsRepository, tablet: Boolean, onBack: (() -> Unit)?, onSkip: () -> Unit, onNext: () -> Unit) {
    val shortcuts by settings.searchShortcutsOn.collectAsState()
    val shown = Features.filter { (!it.tabletOnly || tablet) && (!it.phoneOnly || !tablet) }
    SetupScaffold(
        tablet, "Things worth trying", "A few of Ora's newer tools. Nothing here needs setting up, and each one has a place in Settings.",
        onBack, onSkip, "Continue", onNext, stepNumber = 4,
        preview = { DiscoverPreview(shortcuts) },
    ) {
        SettingRow("Search shortcuts", "Type a keyword and a space in the address bar to search one site: yt cats, gh compose, w kenya. Add your own in Settings > Search.") {
            MineralSwitch(checked = shortcuts, label = "Search shortcuts", onChange = { settings.setSearchShortcutsOn(it) })
        }
        shown.forEach { f -> SettingRow(f.title, f.detail) {} }
    }
}

/** On a tablet: the address bar with a shortcut typed in, and what it opens. Drawn with palette colours only. */
@Composable
private fun DiscoverPreview(on: Boolean) {
    val c = browserColors()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(c.Surface).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.fillMaxWidth().height(44.dp).clip(CircleShape).background(c.ToolbarBackground), contentAlignment = Alignment.CenterStart) {
            Text("   yt funny cats", fontSize = 15.sp, color = c.AddressBarText)
        }
        Text(
            if (on) "Opens a YouTube search for \"funny cats\"." else "Search shortcuts are off, so this searches the web for the whole line.",
            fontSize = 13.sp, lineHeight = 18.sp, color = c.SecondaryText,
        )
        Text("Ctrl+K  Quick switcher", fontSize = 13.sp, color = c.AddressBarText)
        Text("Ctrl+Enter  Add .com", fontSize = 13.sp, color = c.AddressBarText)
        Text("Alt+Enter  Open in a new tab", fontSize = 13.sp, color = c.AddressBarText)
    }
}

// ---- Ready ----

@Composable
internal fun ReadyStep(settings: SettingsRepository, profiles: ProfilesRepository, tablet: Boolean, onBack: (() -> Unit)?, onFinish: () -> Unit) {
    val context = LocalContext.current
    val phoneBar by settings.phoneBarStyle.collectAsState()
    val tabletBar by settings.tabletBarStyle.collectAsState()
    val appearance by settings.appearance.collectAsState()
    val strict by settings.fingerprintingProtection.collectAsState()
    val httpsOnly by settings.httpsOnly.collectAsState()
    val lock by settings.appLockRecord.collectAsState()
    val shortcutsOn by settings.searchShortcutsOn.collectAsState()
    val list by profiles.profiles.collectAsState()
    var isDefault by remember { mutableStateOf(DefaultBrowser.isDefault(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isDefault = DefaultBrowser.isDefault(context)
        BrowserLog.info("onboarding", if (result.resultCode == Activity.RESULT_OK || isDefault) "Default browser request accepted" else "Default browser request declined")
        // Accepted or declined, carry on to the browser.
        onFinish()
    }
    val bar = if (tablet) tabletBar else phoneBar
    SetupScaffold(
        tablet, "Ora is ready", "Here is what you chose, and where to change each one later.",
        onBack, null, "Start browsing", onFinish,
        preview = null,
    ) {
        SummaryRow("Look", (if (tablet) (if (bar == BarStyle.TOP) "Bar with a sidebar" else "Floating bar") else "${bar.label} bar") + ", ${appearance.label} theme", "Settings > Appearance")
        SummaryRow("Blocking", (if (strict) "Strict" else "Standard") + ", HTTPS-only " + (if (httpsOnly) "on" else "off") + ", app lock " + (if (lock != null) "on" else "off"), "Settings > Privacy")
        SummaryRow("Profiles", list.joinToString(", ") { it.name }, "Profiles menu")
        SummaryRow("Search shortcuts", if (shortcutsOn) "On (yt, gh, w and more)" else "Off", "Settings > Search")
        if (!isDefault) {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                PillButton("Make Ora the default", onClick = {
                    val intent = DefaultBrowser.requestIntent(context)
                    if (intent == null) onFinish() else launcher.launch(intent)
                })
            }
        }
    }
}

@Composable
private fun SummaryRow(title: String, value: String, where: String) {
    SettingRow(title, "$value\nChange it in $where.") {}
}
