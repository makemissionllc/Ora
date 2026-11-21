package com.makemission.ora.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Whether the quick new-tab menu is open and where its button is. One per New tab button: the button reports its
 * place with [quickAnchor], a long press sets [open], and [QuickNewTabMenu] draws the menu.
 */
class QuickMenuState {
    var open by mutableStateOf(false)
    var anchor by mutableStateOf(Rect.Zero)
}

@Composable
fun rememberQuickMenu(): QuickMenuState = remember { QuickMenuState() }

/** Tells [state] where this button is, so the menu can grow out of it. */
fun Modifier.quickAnchor(state: QuickMenuState): Modifier = onGloballyPositioned { state.anchor = it.boundsInWindow() }

/** The menu's row choices for the current state, as pure data so the order is testable. */
internal data class QuickEntry(val id: String, val title: String, val subtitle: String? = null)

internal fun quickEntries(
    inPrivate: Boolean,
    clipboard: String?,
    closedTitle: String?,
    groupNames: List<Pair<String, String>>,
    canPaste: Boolean,
): List<QuickEntry> = buildList {
    add(if (inPrivate) QuickEntry("normal", "New tab") else QuickEntry("private", "New private tab"))
    val clip = clipboard?.trim().orEmpty()
    // Only an address is shown back; anything else (a copied password, say) is never printed in a menu.
    val looksLikeAddress = clip.none { it.isWhitespace() } && (clip.contains("://") || clip.contains('.'))
    if (canPaste && clip.isNotEmpty()) add(QuickEntry("paste", "Paste and open", if (looksLikeAddress) clip.take(60) else "Text from the clipboard"))
    if (closedTitle != null) add(QuickEntry("reopen", "Reopen closed tab", closedTitle))
    if (!inPrivate) groupNames.take(3).forEach { (id, name) -> add(QuickEntry("group:$id", "New tab in $name")) }
}

/**
 * The menu a long press on any New tab button opens: a private tab (or a normal one from the private space), paste
 * and open, reopen the last closed tab, and a new tab in a group. It grows out of the button that was pressed
 * (scale from [OraMotion.MORPH_SCALE] at the button's centre) and folds back into it when dismissed. [onNewTab] and
 * [onNewPrivateTab] are the button's own actions; [onDone] runs after any choice, so the tab overview can close.
 */
@Composable
fun QuickNewTabMenu(
    state: QuickMenuState,
    tabs: BrowserTabs,
    inPrivate: Boolean,
    onNewTab: () -> Unit,
    onNewPrivateTab: () -> Unit,
    onDone: () -> Unit = {},
) {
    if (!state.open) return
    val colors = browserColors()
    val clipboard = LocalClipboardManager.current
    val density = LocalDensity.current
    // Read once when the menu opens, not on every redraw.
    val clipText = remember { clipboard.getText()?.text }
    val pasteOk = inPrivate == (tabs.selected?.isPrivate == true)
    val entries = remember(inPrivate, clipText, tabs.lastClosedTitle, tabs.groups.size) {
        quickEntries(inPrivate, clipText, tabs.lastClosedTitle, tabs.groups.map { it.id to it.name }, pasteOk)
    }
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    var cardTopLeft by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) state.open = false
    }
    val close = { visible.targetState = false }
    val anchor = state.anchor
    val marginPx = with(density) { 12.dp.roundToPx() }
    val gapPx = with(density) { 8.dp.roundToPx() }
    val pivot = if (cardSize.width > 0) {
        TransformOrigin(
            ((anchor.center.x - cardTopLeft.x) / cardSize.width).coerceIn(0f, 1f),
            ((anchor.center.y - cardTopLeft.y) / cardSize.height).coerceIn(0f, 1f),
        )
    } else TransformOrigin.Center

    // Above the button when it sits in the lower half of the window, below it otherwise; never off the sides.
    val position = remember(anchor) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
                val x = (anchor.center.x - popupContentSize.width / 2f).toInt()
                    .coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
                val above = anchor.center.y > windowSize.height / 2f
                val y = if (above) anchor.top.toInt() - popupContentSize.height - gapPx else anchor.bottom.toInt() + gapPx
                return IntOffset(x, y.coerceIn(marginPx, (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)))
            }
        }
    }
    LaunchedEffect(Unit) { BrowserLog.info("tabs", "Quick new tab menu opened (${entries.size} choices)") }
    Popup(popupPositionProvider = position, onDismissRequest = close, properties = PopupProperties(focusable = true)) {
        AnimatedVisibility(
            visibleState = visible,
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = pivot),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = pivot),
        ) {
            Column(
                Modifier
                    .widthIn(min = 220.dp, max = 300.dp)
                    .onGloballyPositioned { cardTopLeft = it.positionInWindow(); cardSize = it.size }
                    .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                    .background(colors.Surface)
                    .padding(vertical = 6.dp),
            ) {
                entries.forEach { entry ->
                    Column(
                        Modifier
                            .heightIn(min = 48.dp)
                            .clickable {
                                run(entry, tabs, onNewTab, onNewPrivateTab, clipText)
                                onDone()
                                close()
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                    ) {
                        Text(entry.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (entry.subtitle != null) Text(entry.subtitle, fontSize = 12.sp, color = colors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private fun run(entry: QuickEntry, tabs: BrowserTabs, onNewTab: () -> Unit, onNewPrivateTab: () -> Unit, clip: String?) {
    when {
        entry.id == "private" -> onNewPrivateTab()
        entry.id == "normal" -> onNewTab()
        entry.id == "paste" -> clip?.trim()?.takeIf { it.isNotEmpty() }?.let { text ->
            val open = AddressKeys.openInNewTab
            if (open != null) open(text) else InAppNotices.show("Could not open the pasted text")
        }
        entry.id == "reopen" -> if (!tabs.reopenLastClosed()) InAppNotices.show("No closed tabs to reopen")
        entry.id.startsWith("group:") -> {
            val id = entry.id.removePrefix("group:")
            val tab = tabs.newTab(BrowserTabs.START_URL, select = true)
            tabs.moveToGroup(tab.id, id)
            BrowserLog.info("groups", "New tab in group from the quick menu")
        }
    }
    BrowserLog.info("tabs", "Quick new tab menu: ${entry.id.substringBefore(':')}")
}
