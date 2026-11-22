package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import com.makemission.ora.history.HistoryEntry
import com.makemission.ora.settings.ShortcutSource
import com.makemission.ora.settings.StartPageConfig
import com.makemission.ora.settings.StartSection
import com.makemission.ora.settings.TileShape
import com.makemission.ora.settings.TileSize
import com.makemission.ora.settings.greetingFor
import com.makemission.ora.settings.mostVisited
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
    /** How the page is arranged: which parts show, in what order, and the look of the tiles. */
    config: StartPageConfig = StartPageConfig(),
    /** Visits behind the Most visited shortcuts. Never read for a private tab. */
    history: List<HistoryEntry> = emptyList(),
    /** Opens the customize sheet. Null hides the Customize button. */
    onCustomize: (() -> Unit)? = null,
    /** Long-pressing a shortcut opens it in a new tab. */
    onOpenInNewTab: (String) -> Unit = {},
    /** Private tabs say so on the start page, so the user knows what is not kept. */
    isPrivate: Boolean = false,
    /** Strongest private tabs is on: the Privacy card says what a private tab hides. */
    strongPrivate: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    var query by rememberSaveable { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(colors.GridBackground)) {
        val baseColumns = if (maxWidth >= BrowserTokens.StartWideBreakpoint) 6 else 4
        val columns = when (config.size) {
            TileSize.LARGE -> baseColumns - 1
            else -> baseColumns
        }
        val tiles = when (config.source) {
            ShortcutSource.BOOKMARKS -> bookmarks
            ShortcutSource.PINNED -> config.pinned
            ShortcutSource.MOST_VISITED -> if (isPrivate) emptyList() else mostVisited(history, columns * config.rows)
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = BrowserTokens.StartTopPadding, start = 16.dp, end = 16.dp, bottom = 96.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            if (isPrivate) {
                Text(
                    "Private tab. Ora keeps no history, cookies or thumbnails for it, and it cannot be reopened after it closes.",
                    color = colors.SecondaryText,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            // The parts appear in the order the user chose. A part that is switched off is simply not drawn.
            config.shown.forEach { section ->
                when (section) {
                    StartSection.GREETING -> StartGreeting()
                    StartSection.SEARCH -> StartSearchField(query, { query = it }) {
                        // Quality of life: the keyboard closes as the search starts, instead of staying up.
                        view.hideKeyboard()
                        keyboard?.hide()
                        focusManager.clearFocus()
                        if (query.isNotBlank()) onSubmit(query)
                    }
                    StartSection.SHORTCUTS -> Column(
                        modifier = Modifier.widthIn(max = BrowserTokens.StartContentMaxWidth).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(BrowserTokens.StartCardSpacing)
                    ) {
                        if (tiles.isEmpty()) {
                            // Empty state: say how to fill the shortcuts instead of leaving a blank grid.
                            Text(
                                when {
                                    config.source == ShortcutSource.MOST_VISITED && isPrivate ->
                                        "Most visited sites are not shown in a private tab."
                                    config.source == ShortcutSource.MOST_VISITED ->
                                        "The sites you open most will appear here."
                                    config.source == ShortcutSource.PINNED ->
                                        "No pinned sites yet. Tap Customize to add some."
                                    else -> "Bookmarked pages appear here. Tap the bookmark button on any page to add one."
                                },
                                color = colors.SecondaryText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        } else {
                            tiles.take(columns * config.rows).chunked(columns).forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    row.forEach { tile -> ShortcutTile(tile, config, onLongClick = { onOpenInNewTab(tile.url) }) { onSubmit(tile.url) } }
                                    // A short last row keeps its tiles in the same columns as the rows above.
                                    repeat(columns - row.size) { Spacer(Modifier.width(config.size.slot())) }
                                }
                            }
                        }
                    }
                    StartSection.STATUS -> Row(
                        modifier = Modifier.widthIn(max = BrowserTokens.StartContentMaxWidth).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BrowserTokens.StartCardSpacing)
                    ) {
                        StatusCard(
                            label = "PRIVACY",
                            value = when {
                                isPrivate && strongPrivate -> "Private: details hidden, HTTPS only"
                                blockAds -> "Blocking ads and trackers"
                                else -> "Standard protection"
                            },
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
            if (config.shown.isEmpty()) {
                // Everything is switched off: say so, rather than leave a blank page with no way back.
                Text(
                    "Nothing is switched on for this page.",
                    color = colors.SecondaryText,
                    textAlign = TextAlign.Center,
                )
            }
            if (onCustomize != null) {
                val pillShape = RoundedCornerShape(BrowserTokens.RadiusLarge)
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .pressFeedback().clip(pillShape)
                        .background(colors.AddressBarBackground)
                        .clickable(onClick = onCustomize)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Customize", color = colors.AddressBarText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun StartGreeting() {
    val colors = browserColors()
    val now = remember { java.util.Calendar.getInstance() }
    val date = remember { java.text.SimpleDateFormat("EEEE d MMMM", java.util.Locale.getDefault()).format(now.time) }
    Column(
        modifier = Modifier.widthIn(max = BrowserTokens.StartContentMaxWidth).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(greetingFor(now.get(java.util.Calendar.HOUR_OF_DAY)), color = colors.AddressBarText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(date, color = colors.SecondaryText, fontSize = 15.sp)
    }
}

/** Search field: Enter or the keyboard's Go button submits, the same as the address bar. */
@Composable
private fun StartSearchField(query: String, onChange: (String) -> Unit, onGo: () -> Unit) {
    val colors = browserColors()
    BasicTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(color = colors.AddressBarText, fontSize = 16.sp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
        keyboardActions = KeyboardActions(onGo = { onGo() }),
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
}

internal fun TileSize.tile() = when (this) {
    TileSize.SMALL -> BrowserTokens.StartTileSmall
    TileSize.REGULAR -> BrowserTokens.StartTileSize
    TileSize.LARGE -> BrowserTokens.StartTileLarge
}

internal fun TileSize.slot() = tile() + BrowserTokens.StartTileSlotExtra

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun ShortcutTile(bookmark: Bookmark, config: StartPageConfig, onLongClick: () -> Unit, onClick: () -> Unit) {
    val colors = browserColors()
    val initial = bookmark.title.ifBlank { bookmark.url }.trim().take(1).uppercase()
    Column(
        modifier = Modifier
            .width(config.size.slot())
            .pressFeedback().combinedClickable(onLongClick = onLongClick, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(config.size.tile())
                .clip(if (config.shape == TileShape.CIRCLE) CircleShape else RoundedCornerShape(BrowserTokens.StartTileRadius))
                .background(colors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Text(initial, color = colors.Accent, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
        }
        if (config.labels) Text(
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
