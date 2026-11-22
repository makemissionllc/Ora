package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.settings.SavedSession
import com.makemission.ora.ui.OraDialog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors
import java.text.DateFormat
import java.util.Date

/**
 * Saved sessions: save the open tabs under a name, open a session again, or delete one. [defaultName] is suggested
 * in the name box and [pageCount] says how many pages Save would keep. Grows out of the control that opened it
 * (OraDialog); previous component: the "Saved sessions" menu row, the Quick switcher, or Ctrl+Shift+O.
 */
@Composable
fun SessionsDialog(
    sessions: List<SavedSession>,
    defaultName: String,
    pageCount: Int,
    onSave: (String) -> Unit,
    onOpen: (SavedSession) -> Unit,
    onDelete: (SavedSession) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = browserColors()
    var name by remember { mutableStateOf(defaultName) }
    var deleting by remember { mutableStateOf<SavedSession?>(null) }
    val dateFormat = remember { DateFormat.getDateInstance(DateFormat.MEDIUM) }
    OraDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved sessions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (pageCount == 0) "Open a page, then save it here to come back to it later." else "Save your $pageCount open ${if (pageCount == 1) "page" else "pages"} to open again later. Private tabs are never saved.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = colors.SecondaryText,
                )
                if (pageCount > 0) Row(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp).background(colors.AddressBarBackground, RoundedCornerShape(BrowserTokens.RadiusSmall)).padding(start = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BasicTextField(
                        value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                        textStyle = TextStyle(color = colors.AddressBarText, fontSize = 15.sp), cursorBrush = SolidColor(colors.Accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { onSave(name) }),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onSave(name) }) { Text("Save", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                }
                if (sessions.isEmpty()) {
                    Text("No saved sessions yet.", fontSize = 14.sp, color = colors.SecondaryText, modifier = Modifier.padding(vertical = 8.dp))
                }
                sessions.forEach { s ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).pressFeedback().clickable { onOpen(s) }.padding(vertical = 6.dp)) {
                            Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${s.pages.size} ${if (s.pages.size == 1) "page" else "pages"}  ·  ${dateFormat.format(Date(s.savedAt))}", fontSize = 12.sp, color = colors.SecondaryText)
                        }
                        TextButton(onClick = { onOpen(s) }) { Text("Open", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                        TextButton(onClick = { deleting = s }) { Text("Delete", color = Mineral.Ember) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = colors.Accent, fontWeight = FontWeight.SemiBold) } },
    )
    deleting?.let { s ->
        OraDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Delete \"${s.name}\"?") },
            text = { Text("This session and its ${s.pages.size} ${if (s.pages.size == 1) "page" else "pages"} will be removed. Your open tabs are not affected.") },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = colors.SecondaryText) } },
            confirmButton = { TextButton(onClick = { onDelete(s); deleting = null }) { Text("Delete", color = Mineral.Ember, fontWeight = FontWeight.SemiBold) } },
        )
    }
}
