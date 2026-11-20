package com.makemission.ora.labs

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
import com.makemission.ora.browser.PillButton
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Warns that the page just opened has a lookalike address. Previous component: none, because the page's own
 * navigation raised it, so it eases in from the bottom edge exactly as the pop-up card does. "Go back" leaves
 * the page; "Keep browsing" dismisses the card for this site in this tab.
 */
@Composable
fun LookalikeCardHost(
    pending: Lookalike?,
    onGoBack: (Lookalike) -> Unit,
    onKeep: (Lookalike) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<Lookalike?>(null) }
    if (pending != null) shown = pending
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) +
            scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) +
            scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
        shown?.let { item ->
            val colors = browserColors()
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                    .background(colors.AddressBarBackground)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("This address may be a lookalike", color = colors.AddressBarText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.host, color = colors.AddressBarText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(item.reason, color = colors.SecondaryText, fontSize = 13.sp)
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Keep browsing",
                        color = colors.SecondaryText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.height(44.dp).clickable { onKeep(item) }.padding(horizontal = 12.dp),
                    )
                    Spacer(Modifier.weight(1f))
                    PillButton(text = "Go back", onClick = { onGoBack(item) }, primary = true)
                }
            }
        }
    }
}
