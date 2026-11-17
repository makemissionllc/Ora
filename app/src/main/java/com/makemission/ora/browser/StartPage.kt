package com.makemission.ora.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.bookmarks.Bookmark
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * Native start page shown by tabs at [BrowserTabs.START_URL]: a search field, bookmark shortcuts,
 * and status cards. Phones show four shortcuts per row, wider screens six (design: M-Start, D-Start).
 */
@Composable
fun StartPage(
    bookmarks: List<Bookmark>,
    blockAds: Boolean,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.GridBackground)) {
        val columns = if (maxWidth >= BrowserTokens.StartWideBreakpoint) 6 else 4
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = BrowserTokens.StartTopPadding, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Search field: Enter or the keyboard's Go button submits, the same as the address bar.
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                textStyle = TextStyle(color = colors.AddressBarText, fontSize = 16.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    // Quality of life: the keyboard closes as the search starts, instead of staying up.
                    view.hideKeyboard()
                    keyboard?.hide()
                    focusManager.clearFocus()
                    if (query.isNotBlank()) onSubmit(query)
                }),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BrowserTokens.AddressBarHeight + 8.dp)
                            .clip(BrowserTokens.AddressBarShape)
                            .background(colors.AddressBarBackground)
                            .padding(horizontal = BrowserTokens.StartSearchPadding),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text("Search or enter address", color = colors.SecondaryText, fontSize = 16.sp)
                        }
                        inner()
                    }
                },
                modifier = Modifier.widthIn(max = BrowserTokens.StartContentMaxWidth).fillMaxWidth()
            )

            Column(
                modifier = Modifier.widthIn(max = BrowserTokens.StartContentMaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(BrowserTokens.StartCardSpacing)
            ) {
                if (bookmarks.isEmpty()) {
                    // Empty state: say how to fill the shortcuts instead of leaving a blank grid.
                    Text(
                        "Bookmarked pages appear here. Tap the bookmark button on any page to add one.",
                        color = colors.SecondaryText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                } else {
                    bookmarks.take(columns * 2).chunked(columns).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            row.forEach { bookmark ->
                                ShortcutTile(bookmark) { onSubmit(bookmark.url) }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(BrowserTokens.StartCardSpacing)) {
                    StatusCard(
                        label = "PRIVACY",
                        value = if (blockAds) "Blocking ads and trackers" else "Standard protection",
                        modifier = Modifier.weight(1f)
                    )
                    StatusCard(
                        label = "BOOKMARKS",
                        value = if (bookmarks.size == 1) "1 saved" else "${bookmarks.size} saved",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutTile(bookmark: Bookmark, onClick: () -> Unit) {
    val colors = browserColors()
    val initial = bookmark.title.ifBlank { bookmark.url }.trim().take(1).uppercase()
    Column(
        modifier = Modifier
            .width(BrowserTokens.StartTileSlot)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(BrowserTokens.StartTileSize)
                .clip(RoundedCornerShape(BrowserTokens.StartTileRadius))
                .background(colors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = colors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        }
        Text(
            bookmark.title.ifBlank { bookmark.url },
            color = colors.SecondaryText,
            fontSize = 12.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatusCard(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = browserColors()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(colors.Surface)
            .padding(BrowserTokens.StartCardPadding),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = colors.SecondaryText, fontSize = 12.sp)
        Text(value, color = colors.AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
