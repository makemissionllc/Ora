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
 * Asks whether to open a link in an app. Stay in Ora keeps it here, Always remembers the choice for this
 * site and opens the app, Open opens it this time. A private tab never gets Always, because it would be saved.
 */
@Composable
fun AppLinkCard(
    pending: PendingAppLink,
    onStay: () -> Unit,
    onAlways: () -> Unit,
    onOpen: () -> Unit,
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
                if (pending.web) "Open in the ${pending.label} app?" else "Open in ${pending.label}?",
                color = colors.AddressBarText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                when {
                    pending.web -> "${pending.key} has an app. The page keeps loading in Ora behind this."
                    pending.fallbackUrl != null -> "This link is for an app. Stay in Ora loads the page the site gave instead."
                    else -> "This link is for an app, so Ora can't show it."
                },
                color = colors.SecondaryText,
                fontSize = 13.sp,
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Stay in Ora",
                color = colors.SecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.height(44.dp).clickable(onClick = onStay).padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            if (!pending.fromPrivate) {
                PillButton(text = "Always", onClick = onAlways)
                Spacer(Modifier.width(8.dp))
            }
            PillButton(text = "Open", onClick = onOpen, primary = true)
        }
    }
}

/** Shows [pending] on the shared overlay motion, easing in and out from the bottom edge where it sits. */
@Composable
fun AppLinkCardHost(
    pending: PendingAppLink?,
    onStay: (PendingAppLink) -> Unit,
    onAlways: (PendingAppLink) -> Unit,
    onOpen: (PendingAppLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<PendingAppLink?>(null) }
    if (pending != null) shown = pending
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
        shown?.let { link ->
            AppLinkCard(link, { onStay(link) }, { onAlways(link) }, { onOpen(link) }, Modifier.padding(horizontal = 16.dp))
        }
    }
}
