package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors

/**
 * Hide parts of a page. "Pick an element" closes the card and starts the picker on the page; below it, every site
 * with hidden elements can be shown again. Grows out of the control that opened it (OraDialog); previous component:
 * the "Hide an element" menu row, the command bar, or Ctrl+Shift+X.
 */
@Composable
fun ZapperDialog(
    siteName: String,
    isPrivate: Boolean,
    summary: List<Pair<String, Int>>,
    onPick: () -> Unit,
    onShowAgain: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = browserColors()
    var confirming by remember { mutableStateOf<Pair<String, Int>?>(null) }
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hide an element") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isPrivate) "Tap something on $siteName to hide it until this tab closes. Nothing is saved in a private tab."
                    else "Tap something on $siteName, such as a banner or a pop-up bar, and Ora hides it every time you visit.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = colors.SecondaryText,
                )
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).pressFeedback().background(colors.Accent, RoundedCornerShape(BrowserTokens.RadiusSmall)).clickable(onClick = onPick).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) { Text("Pick an element", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.GridBackground) }
                if (summary.isEmpty()) {
                    Text("Nothing is hidden yet.", fontSize = 14.sp, color = colors.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    Text("Hidden so far", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.padding(top = 8.dp))
                }
                summary.forEach { (host, count) ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(host, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("$count hidden", fontSize = 12.sp, color = colors.SecondaryText)
                        }
                        TextButton(onClick = { confirming = host to count }) { Text("Show again", color = Mineral.Ember) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
    confirming?.let { (host, count) ->
        OraDialog(
            onDismissRequest = { confirming = null },
            title = { Text("Show hidden parts of $host?") },
            text = { Text("The $count hidden ${if (count == 1) "element" else "elements"} on $host will appear again. You can hide them again one by one.") },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text("Cancel", color = colors.SecondaryText) } },
            confirmButton = { TextButton(onClick = { onShowAgain(host); confirming = null }) { Text("Show again", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) } },
        )
    }
}
