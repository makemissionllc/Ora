package com.makemission.ora.browser

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.browserColors

/**
 * Every keyboard shortcut, grouped, in a card that scrolls when it is too tall (AGENTS.md rule 24). Opened by Ctrl+/ or F1
 * and from Settings > About. From Settings the card grows out of the row that was pressed; from the keyboard there is no
 * control to grow from, so it uses the dialog's centred default (recorded in PROGRESS.md).
 */
@Composable
fun ShortcutsCard(onDismiss: () -> Unit) {
    val colors = browserColors()
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keyboard shortcuts") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Shortcut.values().groupBy { it.group }.forEach { (group, items) ->
                    Text(group, color = colors.SecondaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 2.dp))
                    items.forEach { s ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(s.label, color = colors.AddressBarText, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(end = 12.dp))
                            Text(s.keys, color = colors.SecondaryText, fontSize = 13.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
}
