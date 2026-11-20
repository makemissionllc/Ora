package com.makemission.ora.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.settings.SiteTweak
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/**
 * Per-site tweaks: dark colours, text that fills the width, and a font. [site] is the host, empty for a private or
 * blank tab, where nothing is saved and the controls are left out. Grows out of the control that opened it (OraDialog);
 * previous component: the "Site tweaks" menu row, the command bar, or Ctrl+Shift+D.
 */
@Composable
fun SiteTweaksDialog(
    site: String,
    tweak: SiteTweak,
    onChange: (SiteTweak) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = browserColors()
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (site.isEmpty()) "Site tweaks" else "Tweaks for $site") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (site.isEmpty()) {
                    Text("Open a page in a normal tab to change how its site looks. Private tabs never save tweaks.", fontSize = 13.sp, lineHeight = 18.sp, color = colors.SecondaryText)
                } else {
                    Text("These stay on for every page of this site.", fontSize = 13.sp, color = colors.SecondaryText)
                    TweakSwitch("Dark colours", "Light pages turn dark. Pictures keep their colours.", tweak.dark) { onChange(tweak.copy(dark = it)) }
                    TweakSwitch("Wider text", "Let the text fill the screen.", tweak.wide) { onChange(tweak.copy(wide = it)) }
                    Text("Font", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.padding(top = 6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SiteTweak.FONTS.forEach { (key, label) ->
                            val on = tweak.font == key
                            Box(
                                Modifier.weight(1f).height(40.dp).clip(CircleShape).background(if (on) colors.Accent else colors.AddressBarBackground).clickable { onChange(tweak.copy(font = key)) },
                                contentAlignment = Alignment.Center,
                            ) { Text(label, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium, color = if (on) colors.GridBackground else colors.AddressBarText, maxLines = 1) }
                        }
                    }
                    if (!tweak.isDefault) TextButton(onClick = onReset) { Text("Reset this site", color = Mineral.Ember) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
}

@Composable
private fun TweakSwitch(title: String, subtitle: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    val colors = browserColors()
    Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onChecked(!checked) }, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
            Text(subtitle, fontSize = 12.sp, color = colors.SecondaryText)
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}
