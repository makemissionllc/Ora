package com.makemission.ora.labs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * The Site stats lab's content. The surface itself (the capsule's colour and width, unfolding from the padlock) is
 * drawn by the address bar, which is the previous component; this only fills it. It scrolls, never cuts.
 */
@Composable
fun SiteStatsContent(stats: SiteStats, modifier: Modifier = Modifier) {
    val colors = browserColors()
    Column(
        modifier = modifier
            .heightIn(max = 380.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("About ${stats.host}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
        Fact(
            "Connection",
            when {
                stats.secure && stats.exception -> "Encrypted, with a security exception in place"
                stats.secure -> "Encrypted (HTTPS)"
                else -> "Not encrypted. Others on the network can read what you send."
            },
        )
        if (stats.issuer != null) {
            Fact("Certificate issued by", if (stats.selfIssued) "${stats.issuer} (self-issued)" else stats.issuer)
            if (stats.validFrom != null && stats.validUntil != null) {
                Fact("Valid", "${SiteStats.date(stats.validFrom)} to ${SiteStats.date(stats.validUntil)}")
            }
            if (stats.certSha256 != null) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label("Certificate fingerprint (SHA-256)")
                    Text(stats.certSha256, fontSize = 13.sp, fontFamily = FontFamily.Monospace, color = colors.AddressBarText)
                }
            }
        }
        if (stats.blocked.isEmpty()) {
            Fact("Blocked on this page", "Nothing so far")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Label("Blocked on this page: ${stats.blockedTotal}")
                stats.blocked.forEach { (kind, n) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = BrowserTokens.RowMinHeight / 2), verticalAlignment = Alignment.CenterVertically) {
                        Text(kind, fontSize = 15.sp, color = colors.AddressBarText, modifier = Modifier.weight(1f))
                        Text("$n", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.SecondaryText)
                    }
                }
            }
        }
    }
}

@Composable
private fun Label(text: String) = Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = browserColors().SecondaryText)

@Composable
private fun Fact(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Label(label)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
    }
}
