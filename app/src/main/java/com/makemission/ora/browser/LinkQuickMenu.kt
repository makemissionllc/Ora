package com.makemission.ora.browser

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.makemission.ora.notice.InAppNotices
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * The log line for opening a link or image from the quick menu, or null when nothing may be logged.
 * A link opened from a private tab is not logged, so private browsing leaves no trace.
 */
internal fun linkOpenLogLine(action: String, url: String, fromPrivate: Boolean): String? =
    if (fromPrivate) null else "$action: ${hostOf(url)}"

/** The domain of an address, for logs and the menu header. Empty when there is none. */
internal fun hostOf(url: String): String = runCatching { java.net.URI(url).host }.getOrNull().orEmpty()

/**
 * Quick menu opened by a long press on a link or image (phone and tablet).
 * The card grows out of the press point and shrinks back into it on close.
 */
@Composable
fun LinkQuickMenu(
    target: PressTarget,
    tabs: BrowserTabs,
    /** How far the page sits below the top of its box (tablet Compact's top space). Press points are page coordinates. */
    pageTop: Dp = 0.dp,
    /** True when the page that was pressed is a private tab: links it opens stay private, and nothing is logged. */
    fromPrivate: Boolean = false,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
    /** Saves a link to the reading list. Null when saving is not possible. */
    onAddToReading: ((String, String) -> Unit)? = null,
    /** Save image, Copy image and Save image as. Handed the image address; the screen owns the work. */
    /** Opens the group picker for a link. Null in private tabs and where groups are not offered. */
    onOpenInGroup: ((String) -> Unit)? = null,
    /** The open tab's group, shown on that row. */
    currentGroup: TabGroup? = null,
    onSaveImage: ((String) -> Unit)? = null,
    onCopyImage: ((String) -> Unit)? = null,
    onSaveImageAs: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    // Dismiss only after the exit animation has finished.
    LaunchedEffect(visibleState.currentState, visibleState.targetState) {
        if (!visibleState.currentState && !visibleState.targetState) onDismiss()
    }
    val close = { visibleState.targetState = false }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pressFeedback().clickable(onClick = close)
    ) {
        val cardWidth = BrowserTokens.QuickMenuWidth
        val gap = BrowserTokens.QuickMenuEdgeGap
        val rawX = with(density) { target.x.toDp() }
        val rawY = with(density) { target.y.toDp() } + pageTop
        val left = rawX.coerceIn(gap, maxWidth - cardWidth - gap)
        val top = rawY.coerceIn(gap, maxHeight - gap - 160.dp)
        val origin = TransformOrigin(
            pivotFractionX = ((rawX - left) / cardWidth).coerceIn(0f, 1f),
            pivotFractionY = 0f
        )

        AnimatedVisibility(
            visibleState = visibleState,
            modifier = Modifier.offset(x = left, y = top),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(
                initialScale = OraMotion.MORPH_SCALE,
                animationSpec = OraMotion.overlayScaleIn(),
                transformOrigin = origin
            ),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(
                targetScale = OraMotion.MORPH_SCALE,
                animationSpec = OraMotion.overlayScaleOut(),
                transformOrigin = origin
            )
        ) {
            val colors = browserColors()
            Column(
                modifier = Modifier
                    .width(cardWidth)
                    .heightIn(max = maxHeight - top - gap)
                    .pressFeedback().clip(RoundedCornerShape(BrowserTokens.QuickMenuRadius))
                    .background(colors.AddressBarBackground)
                    .clickable(onClick = {})
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 6.dp)
            ) {
                // Header: what was pressed. The link comes first; an image alone shows its own address.
                val headAddress = target.linkUri ?: target.imageUri.orEmpty()
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(
                        target.label ?: hostOf(headAddress).ifEmpty { headAddress },
                        fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Text(headAddress, fontSize = 12.sp, color = colors.SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                val link = target.linkUri
                val image = target.imageUri
                // Link menu. An image that is also a link gets the link section below its own.
                if (image != null) {
                    QuickRow(buildList {
                        onSaveImage?.let { add("Save image" to { close(); it(image) }) }
                        onCopyImage?.let { add("Copy image" to { close(); it(image) }) }
                        add("Share" to { close(); onShare(image) })
                    })
                    onSaveImageAs?.let { saveAs -> QuickMenuRow("Save image as…") { close(); saveAs(image) } }
                    QuickMenuRow("Open image in new tab") {
                        close()
                        linkOpenLogLine("Opened image in new tab", image, fromPrivate)?.let { BrowserLog.info("page", it) }
                        tabs.newTab(image, select = true, isPrivate = fromPrivate)
                    }
                    QuickMenuRow("Copy image address") {
                        close()
                        copyToClipboard(context, "Image", image, "Image address copied")
                    }
                    if (link != null) {
                        HorizontalDivider(color = colors.ToolbarBorder)
                        Text("This image is also a link", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                }
                if (link != null) {
                    if (image == null) QuickRow(listOf(
                        "Copy link" to { close(); copyToClipboard(context, "Link", link, "Link copied") },
                        "Share" to { close(); onShare(link) },
                    ))
                    QuickMenuRow("Open in new tab") {
                        close()
                        linkOpenLogLine("Opened link in new tab", link, fromPrivate)?.let { BrowserLog.info("page", it) }
                        // From a private tab the new tab is private too.
                        tabs.newTab(link, select = true, isPrivate = fromPrivate)
                    }
                    if (!fromPrivate) onOpenInGroup?.let { pick ->
                        QuickMenuRow("Open in new tab in group ›", dot = currentGroup?.color, sub = currentGroup?.name) { close(); pick(link) }
                    }
                    if (!fromPrivate) QuickMenuRow("Open in private tab") {
                        close()
                        BrowserLog.info("page", "Opened link in private tab: ${hostOf(link)}")
                        tabs.newTab(link, select = true, isPrivate = true)
                    }
                    QuickMenuRow("Open in background tab") {
                        close()
                        linkOpenLogLine("Opened link in background tab", link, fromPrivate)?.let { BrowserLog.info("page", it) }
                        tabs.newTab(link, select = false, isPrivate = fromPrivate)
                        InAppNotices.show("Opened in a background tab. Your current page stays put.")
                    }
                    if (image != null) QuickMenuRow("Copy link address") {
                        close()
                        copyToClipboard(context, "Link", link, "Link copied")
                    }
                    if (!fromPrivate && onAddToReading != null) QuickMenuRow("Add to reading list") {
                        close()
                        onAddToReading(link, target.label ?: hostOf(link))
                    }
                }
            }
        }
    }
}

/** The row of up to three quick buttons under the header. */
@Composable
private fun QuickRow(buttons: List<Pair<String, () -> Unit>>) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { (label, onClick) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(BrowserTokens.QuickMenuRowHeight)
                    .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                    .background(browserColors().ToolbarBackground)
                    .pressFeedback()
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) { Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText) }
        }
    }
}

@Composable
private fun QuickMenuRow(label: String, dot: GroupColor? = null, sub: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.QuickMenuRowHeight)
            .pressFeedback()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = browserColors().AddressBarText, modifier = Modifier.weight(1f, fill = false))
        if (dot != null) Box(Modifier.padding(start = 8.dp).size(10.dp).clip(androidx.compose.foundation.shape.CircleShape).background(dot.color))
        if (sub != null) Text(sub, fontSize = 12.sp, color = browserColors().SecondaryText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 6.dp))
    }
}

private fun copyToClipboard(context: Context, what: String, value: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(what, value))
    BrowserLog.info("page", "$message: $value")
    InAppNotices.show(message)
}
