package com.makemission.ora.browser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
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
fun FindBar(session: GeckoSession, onClose: () -> Unit, modifier: Modifier = Modifier, embedded: Boolean = false) {
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
        val asked = query
        runCatching {
            session.finder.find(query, flags).accept { result ->
                // An answer for text the user has since changed must not overwrite the newer count.
                if (result != null && asked == query) {
                    current = result.current
                    total = result.total
                }
            }
        }.onFailure { BrowserLog.warn("find", "Find in page failed: ${it.message}") }
    }

    // Searching as the user types, from the top of the page.
    // Clearing first restarts the search at the top, so typing more letters shows "1 of N", not a count that
    // moved on one match per letter typed.
    // It waits a moment for the next letter: a search started for every letter in quick succession can end
    // on "No matches" for a word that is on the page.
    LaunchedEffect(query) {
        if (query.isNotBlank()) {
            kotlinx.coroutines.delay(250)
            session.finder.clear()
        }
        find(backwards = false)
    }
    DisposableEffect(Unit) {
        BrowserLog.info("find", "Find in page opened")
        UsageStats.count("find.open")
        onDispose {
            session.finder.clear()
            BrowserLog.info("find", "Find in page closed")
        }
    }

    // Quality of life: the field takes focus at once, so the keyboard opens and typing starts the search.
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    if (embedded) {
        // Tablet Compact: the bar sits in the address capsule's slot, in the same pill (design: Fix · Find in page).
        Row(
            modifier = modifier.fillMaxWidth().height(BrowserTokens.AddressBarHeight).padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = colors.SecondaryText, modifier = Modifier.size(18.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.AddressBarText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(colors.Accent),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp).focusRequester(focus),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Find in page", color = colors.SecondaryText, fontSize = 16.sp)
                    inner()
                }
            )
            Text(
                text = if (total == 0) "No matches" else "$current of $total",
                color = colors.SecondaryText,
                fontSize = 14.sp,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            FindRoundButton(Icons.Filled.KeyboardArrowUp, "Previous match", total > 0) { find(backwards = true) }
            Spacer(Modifier.width(4.dp))
            FindRoundButton(Icons.Filled.KeyboardArrowDown, "Next match", total > 0) { find(backwards = false) }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(colors.Accent)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Done", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.GridBackground) }
        }
        return
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

/** A round 40 dp Shale button with an arrow, for stepping between matches. */
@Composable
private fun FindRoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(colors.ToolbarBackground)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) colors.IconColor else colors.DisabledIcon, modifier = Modifier.size(20.dp))
    }
}
