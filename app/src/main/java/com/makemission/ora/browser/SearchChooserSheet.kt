package com.makemission.ora.browser

import com.makemission.ora.ui.CheckMark
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.settings.SearchEngine
import com.makemission.ora.ui.theme.browserColors

/**
 * Asks which of the turned-on search engines to use for [query].
 * Shown only when more than one engine is on; otherwise the default is used directly.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchChooserSheet(
    query: String,
    engines: List<SearchEngine>,
    default: SearchEngine,
    onChoose: (SearchEngine) -> Unit,
    onDismiss: () -> Unit,
) {
    OraSheet(onDismiss = onDismiss, tag = "search", containerColor = browserColors().AddressBarBackground) { _ ->
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text(
                "Search for \"$query\"",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = browserColors().AddressBarText,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            engines.forEach { engine ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChoose(engine) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(engine.label, fontSize = 15.sp, color = browserColors().AddressBarText)
                    CheckMark(engine == default, "Default")
                }
            }
        }
    }
}
