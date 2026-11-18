package com.makemission.ora.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Asks about a pop-up that a page opened without a tap. Open shows it as a tab, Always allow remembers
 * the site for this profile, Block drops it. A private page never gets Always allow, because a saved
 * site would be written to disk.
 */
@Composable
fun PopupCard(
    pending: PendingPopup,
    more: Int,
    onOpen: () -> Unit,
    onAlways: () -> Unit,
    onBlock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    Column(
        modifier = modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(colors.AddressBarBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                "${pending.fromSite} wants to open a new tab",
                color = colors.AddressBarText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "You didn't tap a link, so the page opened this on its own.",
                color = colors.SecondaryText,
                fontSize = 13.sp,
            )
            // Quality of life: the rest of the queue is counted, and each one comes up after this choice.
            if (more > 0) {
                Text(
                    "$more more ${if (more == 1) "pop-up waits" else "pop-ups wait"} after this one",
                    color = colors.SecondaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Block",
                color = colors.SecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .height(44.dp)
                    .clickable(onClick = onBlock)
                    .padding(horizontal = 12.dp)

            )
            Spacer(Modifier.weight(1f))
            if (!pending.fromPrivate) {
                PillButton(text = "Always allow", onClick = onAlways)
                Spacer(Modifier.width(8.dp))
            }
            PillButton(text = "Open", onClick = onOpen, primary = true)
        }
    }
}

/**
 * Shows [pending] on the shared overlay motion. The page raised the pop-up, so there is no button to grow from:
 * the card eases in and out from the bottom edge where it sits. The last pop-up is kept for the exit, and each
 * callback receives that pop-up, so the caller can ignore a tap that arrives after the choice was made.
 */
@Composable
fun PopupCardHost(
    pending: PendingPopup?,
    more: Int,
    onOpen: (PendingPopup) -> Unit,
    onAlways: (PendingPopup) -> Unit,
    onBlock: (PendingPopup) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<PendingPopup?>(null) }
    if (pending != null) shown = pending
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) +
            scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) +
            scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
        shown?.let { popup ->
            PopupCard(
                pending = popup,
                more = more,
                onOpen = { onOpen(popup) },
                onAlways = { onAlways(popup) },
                onBlock = { onBlock(popup) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
