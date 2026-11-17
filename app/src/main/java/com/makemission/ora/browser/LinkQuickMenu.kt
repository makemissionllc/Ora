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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Quick menu opened by a long press on a link or image (phone and tablet).
 * The card grows out of the press point and shrinks back into it on close.
 */
@Composable
fun LinkQuickMenu(
    target: PressTarget,
    tabs: BrowserTabs,
    onDismiss: () -> Unit,
    onShare: (String) -> Unit,
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
            .clickable(onClick = close)
    ) {
        val cardWidth = BrowserTokens.QuickMenuWidth
        val gap = BrowserTokens.QuickMenuEdgeGap
        val rawX = with(density) { target.x.toDp() }
        val rawY = with(density) { target.y.toDp() }
        val left = rawX.coerceIn(gap, maxWidth - cardWidth - gap)
        val top = rawY.coerceIn(gap, maxHeight - gap - 240.dp)
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
            Column(
                modifier = Modifier
                    .width(cardWidth)
                    .clip(RoundedCornerShape(BrowserTokens.QuickMenuRadius))
                    .background(browserColors().AddressBarBackground)
                    .clickable(onClick = {})
                    .padding(vertical = 6.dp)
            ) {
                target.linkUri?.let { link ->
                    QuickMenuRow("Open in new tab") {
                        close()
                        BrowserLog.info("page", "Opened link in new tab: $link")
                        tabs.newTab(link, select = true)
                        InAppNotices.show("Opened in a new tab")
                    }
                    QuickMenuRow("Open in background tab") {
                        close()
                        BrowserLog.info("page", "Opened link in background tab: $link")
                        tabs.newTab(link, select = false)
                        InAppNotices.show("Opened in a background tab. Your current page stays put.")
                    }
                    QuickMenuRow("Copy link address") {
                        close()
                        copyToClipboard(context, "Link", link, "Link copied")
                    }
                    QuickMenuRow("Share link") {
                        close()
                        onShare(link)
                    }
                }
                target.imageUri?.let { image ->
                    if (target.linkUri != null) HorizontalDivider(color = browserColors().ToolbarBorder)
                    QuickMenuRow("Open image in new tab") {
                        close()
                        BrowserLog.info("page", "Opened image in new tab: $image")
                        tabs.newTab(image, select = false)
                        InAppNotices.show("Image opened in a background tab")
                    }
                    QuickMenuRow("Copy image address") {
                        close()
                        copyToClipboard(context, "Image", image, "Image address copied")
                    }
                    QuickMenuRow("Share image") {
                        close()
                        onShare(image)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickMenuRow(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(BrowserTokens.QuickMenuRowHeight)
            .pressFeedback()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(label, fontSize = 15.sp, color = browserColors().AddressBarText)
    }
}

private fun copyToClipboard(context: Context, what: String, value: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(what, value))
    BrowserLog.info("page", "$message: $value")
    InAppNotices.show(message)
}
