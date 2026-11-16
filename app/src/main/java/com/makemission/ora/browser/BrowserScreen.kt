package com.makemission.ora.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.makemission.ora.bookmarks.BookmarksRepository
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.BrowserTokens
import java.net.URLEncoder
import org.mozilla.geckoview.GeckoView

/** Screens narrower than this use the phone layout (tab grid); wider ones use the sidebar layout. */
private const val TABLET_MIN_WIDTH_DP = 600

/**
 * Browser screen. Phone: full-screen page, bottom toolbar, tab grid on demand.
 * Tablet: tab/bookmark sidebar on the left, page and bottom toolbar on the right.
 */
@Composable
fun BrowserScreen(
    settings: SettingsRepository,
    bookmarks: BookmarksRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val homePage by settings.homePage.collectAsState()
    val bookmarkList by bookmarks.bookmarks.collectAsState()
    val isTablet = LocalConfiguration.current.screenWidthDp >= TABLET_MIN_WIDTH_DP

    val tabs = remember(homePage) { BrowserTabs(context, homePage) }
    var showTabGrid by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showLibrary by remember { mutableStateOf(false) }

    val selected = tabs.selected ?: return
    val selectedId = tabs.selectedId

    // Only the selected tab should be active, so background pages stop rendering.
    LaunchedEffect(selectedId) {
        tabs.tabs.forEach { it.session.setActive(it.id == selectedId) }
    }

    val onNewTab: () -> Unit = { tabs.newTab(homePage) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (!(showTabGrid && !isTablet) && !(showLibrary && !isTablet)) {
                BrowserToolbar(
                    tab = selected,
                    isBookmarked = bookmarkList.any { it.url == selected.url },
                    onSubmit = { input -> selected.session.loadUri(toLoadableUrl(input, SettingsRepository.DEFAULT_SEARCH_TEMPLATE)) },
                    onBookmark = { bookmarks.toggle(selected.url, selected.title) },
                    onShare = { shareUrl(context, selected.url) },
                    onTabs = if (isTablet) onNewTab else { { showTabGrid = true } },
                    onMenu = if (isTablet) null else { { showMenu = true } }
                )
            }
        }
    ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isTablet) {
                TabSidebar(
                    tabs = tabs,
                    bookmarks = bookmarkList,
                    onNewTab = onNewTab,
                    onOpenBookmark = { url -> selected.session.loadUri(url) }
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                BrowserPage(tab = selected)
                if (showMenu && !isTablet) {
                    BrowserMenuSheet(
                            isBookmarked = bookmarkList.any { it.url == selected.url },
                        onDismiss = { showMenu = false },
                        onShare = {
                            showMenu = false
                            shareUrl(context, selected.url)
                        },
                        onBookmark = {
                            bookmarks.toggle(selected.url, selected.title)
                            showMenu = false
                        },
                        onNewTab = {
                            showMenu = false
                            onNewTab()
                        },
                        onLibrary = {
                            showMenu = false
                            showLibrary = true
                        }
                    )
                }
                if (showLibrary && !isTablet) {
                    BookmarksLibrary(
                        bookmarks = bookmarkList,
                        onOpen = { url ->
                            selected.session.loadUri(url)
                            showLibrary = false
                        },
                        onRemove = { url -> bookmarks.remove(url) },
                        onDone = { showLibrary = false }
                    )
                }
                if (showTabGrid && !isTablet) {
                    TabGrid(
                        tabs = tabs,
                        onSelect = { id ->
                            tabs.select(id)
                            showTabGrid = false
                        },
                        onClose = { id -> tabs.close(id) },
                        onNewTab = {
                            onNewTab()
                            showTabGrid = false
                        },
                        onDone = { showTabGrid = false }
                    )
                }
            }
        }
    }
}

/** Shows the selected tab's page. The single GeckoView is re-pointed at whichever session is selected. */
@Composable
private fun BrowserPage(tab: BrowserTab) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> GeckoView(ctx) },
        update = { view -> if (view.session != tab.session) view.setSession(tab.session) }
    )
}

@Composable
private fun BrowserToolbar(
    tab: BrowserTab,
    isBookmarked: Boolean,
    onSubmit: (String) -> Unit,
    onBookmark: () -> Unit,
    onShare: () -> Unit,
    onTabs: () -> Unit,
    onMenu: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrowserTokens.ToolbarBackground)
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(BrowserTokens.HairlineHeight)
                .background(BrowserTokens.ToolbarBorder)
        )
        Column(
            modifier = Modifier.padding(BrowserTokens.ToolbarPadding),
            verticalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
        ) {
            // Top row: back, forward and the address pill (design: M-Browse).
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BrowserTokens.ToolbarRowSpacing)
            ) {
                ToolbarButton(onClick = { tab.session.goBack() }, enabled = tab.canGoBack, description = "Back") {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                AddressBar(
                    currentUrl = tab.url,
                    loading = tab.loading,
                    onSubmit = onSubmit,
                    onReload = { tab.session.reload() },
                    onStop = { tab.session.stop() },
                    modifier = Modifier.weight(1f)
                )
                ToolbarButton(onClick = { tab.session.goForward() }, enabled = tab.canGoForward, description = "Forward") {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
            // Bottom row: share, bookmark, tabs (design: bottom toolbar).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ToolbarButton(onClick = onShare, enabled = tab.url.isNotBlank(), description = "Share") {
                    Icon(Icons.Filled.Share, contentDescription = null)
                }
                ToolbarButton(onClick = onBookmark, enabled = tab.url.isNotBlank(), description = "Bookmark") {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null
                    )
                }
                ToolbarButton(onClick = onTabs, enabled = true, description = "Tabs") {
                    Icon(Icons.Outlined.Tab, contentDescription = null)
                }
                if (onMenu != null) {
                    ToolbarButton(onClick = onMenu, enabled = true, description = "Menu") {
                        Icon(Icons.Filled.MoreVert, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressBar(
    currentUrl: String,
    loading: Boolean,
    onSubmit: (String) -> Unit,
    onReload: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(displayUrl(currentUrl))) }
    var focused by remember { mutableStateOf(false) }

    // Show the current page's host while the user is not typing in the field.
    LaunchedEffect(currentUrl, focused) {
        if (!focused) fieldValue = TextFieldValue(displayUrl(currentUrl))
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(BrowserTokens.AddressBarHeight)
            .shadow(BrowserTokens.AddressBarElevation, BrowserTokens.AddressBarShape),
        shape = BrowserTokens.AddressBarShape,
        color = BrowserTokens.AddressBarBackground
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = BrowserTokens.AddressBarInnerPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentUrl.startsWith("https://")) {
                Spacer(Modifier.width(BrowserTokens.AddressBarTextStart))
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = "Secure connection",
                    modifier = Modifier.size(BrowserTokens.LockIconSize),
                    tint = BrowserTokens.SecondaryText
                )
            }
            Spacer(Modifier.width(BrowserTokens.AddressBarTextStart))
            BasicTextField(
                value = fieldValue,
                onValueChange = { fieldValue = it },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        if (state.isFocused) {
                            fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                        }
                    },
                singleLine = true,
                textStyle = TextStyle(
                    color = BrowserTokens.AddressBarText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                cursorBrush = SolidColor(BrowserTokens.Accent),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    keyboardType = KeyboardType.Uri,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        onSubmit(fieldValue.text)
                        focused = false
                    }
                )
            )
            if (loading) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop", tint = BrowserTokens.Accent)
                }
            } else {
                IconButton(onClick = onReload) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Reload", tint = BrowserTokens.AddressBarText)
                }
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    icon: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(BrowserTokens.ToolbarButtonSize)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) BrowserTokens.Accent else BrowserTokens.DisabledIcon
        ) {
            icon()
        }
    }
}

/** Turns typed text into a URL: full URLs as-is, bare domains get https, anything else is searched. */
internal fun toLoadableUrl(input: String, searchTemplate: String): String {
    val text = input.trim()
    return when {
        text.isEmpty() -> ""
        Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://.*").matches(text) -> text
        !text.contains(' ') && text.contains('.') -> "https://$text"
        else -> searchTemplate.replace("%s", URLEncoder.encode(text, "UTF-8"))
    }
}

/** The part of a URL shown in the address bar when not editing, e.g. "mozilla.org". */
internal fun displayUrl(url: String): String =
    runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.") ?: url

private fun shareUrl(context: Context, url: String) {
    if (url.isBlank()) return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(intent, null))
}
