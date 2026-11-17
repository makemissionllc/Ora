package com.makemission.ora.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.UsageStats
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors
import org.mozilla.geckoview.GeckoSession

/**
 * Find-in-page bar, opened from the menu. Typing searches the page as you go; the arrows step between
 * matches and the count shows the position. Closing clears the highlights.
 */
@Composable
fun FindBar(session: GeckoSession, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = browserColors()
    var query by remember { mutableStateOf("") }
    var current by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }

    // Runs one search and shows its result. Failures are logged, not swallowed.
    fun find(backwards: Boolean) {
        if (query.isBlank()) {
            session.finder.clear()
            total = 0
            current = 0
            return
        }
        val flags = if (backwards) {
            GeckoSession.FINDER_FIND_BACKWARDS
        } else {
            GeckoSession.FINDER_FIND_FORWARD
        }
        runCatching {
            session.finder.find(query, flags).accept { result ->
                if (result != null) {
                    current = result.current
                    total = result.total
                }
            }
        }.onFailure { BrowserLog.warn("find", "Find in page failed: ${it.message}") }
    }

    // Searching as the user types, from the top of the page.
    LaunchedEffect(query) { find(backwards = false) }
    DisposableEffect(Unit) {
        BrowserLog.info("find", "Find in page opened")
        UsageStats.count("find.open")
        onDispose {
            session.finder.clear()
            BrowserLog.info("find", "Find in page closed")
        }
    }

    Surface(
        modifier = modifier
            .widthIn(max = BrowserTokens.FindBarMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = BrowserTokens.FloatingBarInset)
            .shadow(BrowserTokens.FloatingBarElevation, BrowserTokens.FloatingBarShape),
        shape = BrowserTokens.FloatingBarShape,
        color = colors.AddressBarBackground
    ) {
        Row(
            modifier = Modifier.padding(BrowserTokens.ToolbarPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.AddressBarText, fontSize = 16.sp),
                cursorBrush = SolidColor(colors.Accent),
                modifier = Modifier.weight(1f).padding(start = 8.dp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Find in page", color = colors.SecondaryText, fontSize = 16.sp)
                    }
                    inner()
                }
            )
            Text(
                text = if (total == 0) "0" else "$current/$total",
                color = colors.SecondaryText,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Previous match",
                tint = if (total > 0) colors.Accent else colors.DisabledIcon,
                modifier = Modifier.size(BrowserTokens.ToolbarButtonSize).clickable(enabled = total > 0) { find(backwards = true) }.padding(12.dp)
            )
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Next match",
                tint = if (total > 0) colors.Accent else colors.DisabledIcon,
                modifier = Modifier.size(BrowserTokens.ToolbarButtonSize).clickable(enabled = total > 0) { find(backwards = false) }.padding(12.dp)
            )
            Icon(
                Icons.Filled.Close,
                contentDescription = "Close find",
                tint = colors.IconColor,
                modifier = Modifier.size(BrowserTokens.ToolbarButtonSize).clickable(onClick = onClose).padding(12.dp)
            )
        }
    }
}
