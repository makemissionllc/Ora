package com.makemission.ora.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * "Send to a device": pick one of the paired devices to send the open page to, and open pages other devices sent
 * here. [url] is null when there is nothing to send (start page, private tab); the card then only lists what arrived.
 * Devices that are on the Wi-Fi come first, then the one used most recently. A device that is away still gets the
 * page: it waits in the outbox and goes when the device is next seen.
 * Previous component: the "Send to a device" menu row (the card grows out of it, via OraDialog).
 */
@Composable
fun SendToDeviceDialog(
    url: String?,
    title: String,
    inbox: TabInbox,
    onOpenReceived: (ReceivedTab) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = browserColors()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val link = remember { DeviceHub.link(context) }
    val devices by link.store.devices.collectAsState()
    val peers by link.peers.collectAsState()
    val waiting by link.store.queue.collectAsState()
    val received by inbox.items.collectAsState()
    val ordered = devices.sortedWith(compareByDescending<PairedDevice> { peers.containsKey(it.id) }.thenByDescending { it.lastUsed })

    fun send(device: PairedDevice) {
        val page = url ?: return
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                link.send(device.id, DeviceHub.TYPE_TAB, JSONObject().put("url", page).put("title", title.take(200)))
            }
            when (result) {
                DeviceLink.SendResult.DELIVERED -> InAppNotices.show("Sent to ${device.name}")
                DeviceLink.SendResult.QUEUED -> InAppNotices.show("${device.name} is not nearby. It will get the page when it is back on this Wi-Fi.")
                DeviceLink.SendResult.FAILED -> InAppNotices.show("Could not send to ${device.name}")
            }
        }
        onDismiss()
    }

    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send to a device") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (url != null) {
                    Text(title.ifBlank { LogPrivacy.domainOf(url) }, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else {
                    Text("Open a page to send it. Private tabs are never sent.", fontSize = 13.sp, color = colors.SecondaryText)
                }
                if (devices.isEmpty()) {
                    Text("No devices paired yet. Turn on Link my devices on both, then use Settings, Devices, Pair a device.", fontSize = 14.sp, lineHeight = 19.sp, color = colors.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
                }
                if (url != null) ordered.forEachIndexed { index, device ->
                    val here = peers.containsKey(device.id)
                    val pending = waiting.count { it.to == device.id }
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(device.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                            Text(
                                when {
                                    here -> if (index == 0 && device.lastUsed > 0) "On this Wi-Fi, used last" else "On this Wi-Fi now"
                                    pending > 0 -> "Not nearby, $pending waiting"
                                    else -> "Not nearby, will get it later"
                                },
                                fontSize = 12.sp, color = if (here) colors.Accent else colors.SecondaryText,
                            )
                        }
                        TextButton(onClick = { send(device) }) { Text(if (here) "Send" else "Send later", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                    }
                }
                if (received.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Sent to you", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.weight(1f))
                        TextButton(onClick = { inbox.clear() }) { Text("Clear all", color = Mineral.Ember, fontSize = 13.sp) }
                    }
                    received.forEach { item ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(item.title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("From ${item.fromName}  ·  ${LogPrivacy.domainOf(item.url)}", fontSize = 12.sp, color = colors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = { onOpenReceived(item); onDismiss() }) { Text("Open", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                            TextButton(onClick = { inbox.remove(item.id) }) { Text("Dismiss", color = colors.SecondaryText) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
}
