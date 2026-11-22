package com.makemission.ora.devices

import com.makemission.ora.ui.pressFeedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.MineralSwitch
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Devices page in Settings: the on switch for linking, this device's name, the devices already paired, and
 * a way to pair another. Previous component: the "Devices" row in the Settings category list (the page grows out of
 * it); the name, pairing and forget cards grow out of the row or button that was pressed (OraDialog).
 */
@Composable
fun DevicesSection(settings: SettingsRepository) {
    val colors = browserColors()
    val context = LocalContext.current
    val link = remember { DeviceHub.link(context) }
    val switchOn by settings.deviceLink.collectAsState()
    var allowed by remember { mutableStateOf(LocalNetworkAccess.granted(context)) }
    val on = switchOn && allowed
    val name by settings.deviceName.collectAsState()
    val shareCurrent by settings.deviceContinue.collectAsState()
    val syncReading by settings.deviceReading.collectAsState()
    val paired by link.store.devices.collectAsState()
    val peers by link.peers.collectAsState()
    var renaming by remember { mutableStateOf(false) }
    var pairing by remember { mutableStateOf(false) }
    var forgetting by remember { mutableStateOf<PairedDevice?>(null) }
    // Android 17 asks before an app may talk to other devices on the Wi-Fi; the link only turns on once it is allowed.
    val askLocalNetwork = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { yes ->
        allowed = yes
        if (yes) {
            BrowserLog.info("devices", "Local network access allowed")
            settings.setDeviceLink(true)
        } else {
            BrowserLog.warn("devices", "Local network access was not allowed, so the device link stays off")
            InAppNotices.show("Linking needs Nearby devices access. You can allow it in Ora's app settings.")
        }
    }
    fun setLink(enable: Boolean) {
        if (enable && !LocalNetworkAccess.granted(context)) askLocalNetwork.launch(LocalNetworkAccess.PERMISSION) else settings.setDeviceLink(enable)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Link devices", modifier = Modifier.padding(start = 4.dp, bottom = 2.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
        Row(
            modifier = Modifier.fillMaxWidth().pressFeedback().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)
                .clickable { setLink(!on) }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Link my devices", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                Text(
                    "Find your other Ora devices on the same Wi-Fi to send them pages. Everything is sealed between your devices; nothing goes through a server.",
                    fontSize = 14.sp, color = colors.SecondaryText,
                )
            }
            MineralSwitch(checked = on, label = "Link my devices", onChange = { setLink(it) })
        }
        if (on) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).pressFeedback().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)
                    .clickable { renaming = true }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("This device is called", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                Text("$name ›", fontSize = 14.sp, color = colors.SecondaryText)
            }
            Row(
                modifier = Modifier.fillMaxWidth().pressFeedback().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)
                    .clickable { settings.setDeviceContinue(!shareCurrent) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Continue on another device", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                    Text("Let your paired devices see the page you have open, so they can offer to continue it. Private tabs are never shared.", fontSize = 14.sp, color = colors.SecondaryText)
                }
                MineralSwitch(checked = shareCurrent, label = "Continue on another device", onChange = { settings.setDeviceContinue(it) })
            }
            Row(
                modifier = Modifier.fillMaxWidth().pressFeedback().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)
                    .clickable { settings.setDeviceReading(!syncReading) }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Sync reading list", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                    Text("Keep the pages you saved to read later the same on every paired device. Applies to your first profile; private tabs are never added.", fontSize = 14.sp, color = colors.SecondaryText)
                }
                MineralSwitch(checked = syncReading, label = "Sync reading list", onChange = { settings.setDeviceReading(it) })
            }
            Text("Paired devices", modifier = Modifier.padding(start = 4.dp, top = 8.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)) {
                if (paired.isEmpty()) {
                    // Empty state: says what to do next.
                    Text(
                        "No devices yet. Open Ora on your other device, turn on Link my devices there, then tap Pair a device on either one.",
                        fontSize = 14.sp, color = colors.SecondaryText, modifier = Modifier.padding(16.dp),
                    )
                }
                paired.forEach { device ->
                    val here = peers.containsKey(device.id)
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(start = 16.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(device.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                            Text(if (here) "On this Wi-Fi now" else "Not nearby; things you send wait for it", fontSize = 13.sp, color = if (here) colors.Accent else colors.SecondaryText)
                        }
                        TextButton(onClick = { forgetting = device }) { Text("Forget", color = Mineral.Ember) }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).pressFeedback().clickable { pairing = true }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Pair a device", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = colors.Accent)
                }
            }
        }
    }

    if (renaming) NameDialog(name, onSave = { settings.setDeviceName(it); renaming = false }, onDismiss = { renaming = false })
    if (pairing) PairDialog(link = link, onDismiss = { pairing = false })
    forgetting?.let { device ->
        OraDialog(
            onDismissRequest = { forgetting = null },
            title = { Text("Forget \"${device.name}\"?") },
            text = { Text("You will need to pair again before you can send pages to it. Pages already waiting for it are dropped.") },
            dismissButton = { TextButton(onClick = { forgetting = null }) { Text("Cancel", color = colors.SecondaryText) } },
            confirmButton = { TextButton(onClick = { link.forget(device.id); forgetting = null }) { Text("Forget", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) } },
        )
    }
}

@Composable
private fun NameDialog(current: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val colors = browserColors()
    var text by remember { mutableStateOf(current) }
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this device") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Other devices show this name when you send them a page.", fontSize = 13.sp, color = colors.SecondaryText)
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).background(colors.AddressBarBackground, RoundedCornerShape(BrowserTokens.RadiusSmall)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(
                        value = text, onValueChange = { text = it.take(40) }, singleLine = true,
                        textStyle = TextStyle(color = colors.AddressBarText, fontSize = 15.sp), cursorBrush = SolidColor(colors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onSave(text) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.SecondaryText) } },
        confirmButton = { TextButton(onClick = { onSave(text) }) { Text("Save", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
}

/**
 * Pairs two devices. One side shows a six digit code ("Show a code"), the other lists the devices showing one and
 * asks for it ("Enter a code"). Closes itself the moment a new device is paired, from whichever side.
 */
@Composable
private fun PairDialog(link: DeviceLink, onDismiss: () -> Unit) {
    val colors = browserColors()
    val scope = rememberCoroutineScope()
    val peers by link.peers.collectAsState()
    val paired by link.store.devices.collectAsState()
    val code by link.pairingCode.collectAsState()
    val startCount = remember { paired.size }
    var chosen by remember { mutableStateOf<Peer?>(null) }
    var typed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val candidates = peers.values.filter { it.pairing && link.store.find(it.id) == null }

    // Showing a code is the default, so the other device can find this one at once.
    LaunchedEffect(Unit) { if (link.pairingCode.value == null) link.beginPairing() }
    DisposableEffect(Unit) { onDispose { link.endPairing() } }
    // Re-read the other devices' announcements so a code one of them starts showing appears in the list.
    LaunchedEffect(Unit) { while (true) { link.refreshPeers(); kotlinx.coroutines.delay(3000) } }
    LaunchedEffect(paired.size) { if (paired.size > startCount) onDismiss() }

    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (chosen == null) "Pair a device" else "Pair with ${chosen!!.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (chosen == null) {
                    Text("On the other device, open Settings, Devices, then Pair a device, and type this code.", fontSize = 13.sp, lineHeight = 18.sp, color = colors.SecondaryText)
                    Text(
                        code?.let { it.take(3) + " " + it.drop(3) } ?: "…",
                        fontSize = 34.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText,
                        modifier = Modifier.fillMaxWidth().background(colors.AddressBarBackground, RoundedCornerShape(BrowserTokens.RadiusSmall)).padding(vertical = 14.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Text("Or, if the other device is showing a code:", fontSize = 13.sp, color = colors.SecondaryText)
                    if (candidates.isEmpty()) {
                        Text("No device is showing a code yet.", fontSize = 14.sp, color = colors.SecondaryText)
                    }
                    candidates.forEach { peer ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 48.dp).pressFeedback().clip(RoundedCornerShape(BrowserTokens.RadiusSmall)).clickable { chosen = peer; error = null }.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) { Text(peer.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText) }
                    }
                } else {
                    Text("Type the six digit code shown on ${chosen!!.name}.", fontSize = 13.sp, color = colors.SecondaryText)
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).background(colors.AddressBarBackground, RoundedCornerShape(BrowserTokens.RadiusSmall)).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = typed, onValueChange = { typed = it.filter(Char::isDigit).take(6); error = null }, singleLine = true,
                            textStyle = TextStyle(color = colors.AddressBarText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold), cursorBrush = SolidColor(colors.Accent),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    error?.let { Text(it, fontSize = 13.sp, color = Mineral.Ember) }
                    Spacer(Modifier.height(2.dp))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (chosen != null) { chosen = null; typed = ""; error = null } else onDismiss() }) { Text(if (chosen != null) "Back" else "Cancel", color = colors.SecondaryText) }
        },
        confirmButton = {
            if (chosen != null) TextButton(enabled = typed.length == 6 && !busy, onClick = {
                val peer = chosen ?: return@TextButton
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { link.pairWith(peer, typed) }
                    busy = false
                    if (result != null) error = result
                }
            }) { Text(if (busy) "Pairing…" else "Pair", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
            else TextButton(onClick = onDismiss) { Text("Done", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
        },
    )
}
