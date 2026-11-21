package com.makemission.ora.torrents

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.MineralSwitch
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/** The Torrents category on the Settings page: Wi-Fi only, keep sharing, and a speed cap. */
@Composable
fun TorrentsSettingsSection(settings: SettingsRepository) {
    val colors = browserColors()
    val wifiOnly by settings.torrentsWifiOnly.collectAsState()
    val seed by settings.torrentsSeed.collectAsState()
    val limit by settings.torrentsSpeedLimit.collectAsState()
    val steps = listOf(0, 250, 500, 1000, 2000, 5000)

    @Composable
    fun toggle(title: String, body: String, on: Boolean, change: (Boolean) -> Unit) = Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)
            .clickable { change(!on) }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
            Text(body, fontSize = 13.sp, color = colors.SecondaryText)
        }
        MineralSwitch(on, title, change)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Torrents", Modifier.padding(start = 4.dp, bottom = 2.dp), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText)
        toggle("Wi-Fi only", "Torrents wait on mobile data and carry on when you are back on Wi-Fi.", wifiOnly, settings::setTorrentsWifiOnly)
        toggle("Keep sharing when finished", "Seed until you remove the torrent. Off stops as soon as it is complete.", seed, settings::setTorrentsSeed)
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface).padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Download speed limit", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
            Text(if (limit == 0) "No limit" else "$limit KB/s", fontSize = 13.sp, color = colors.SecondaryText)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                steps.forEach { s ->
                    TextButton(onClick = { settings.setTorrentsSpeedLimit(s) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                        Text(if (s == 0) "None" else if (s >= 1000) "${s / 1000} MB" else "$s KB", fontSize = 12.sp, fontWeight = if (s == limit) FontWeight.Bold else FontWeight.Normal, color = if (s == limit) colors.Accent else colors.SecondaryText)
                    }
                }
            }
        }
        Text("Private tabs never start torrents. Peers can see your network address while a torrent runs.", fontSize = 12.sp, color = colors.SecondaryText, modifier = Modifier.padding(start = 4.dp))
    }
}
