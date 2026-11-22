package com.makemission.ora.labs

import com.makemission.ora.ui.pressFeedback
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
 * Tells the user what Ora noticed about the site (lookalike address, unencrypted page, mixed content, certificate
 * problems, redirect chains). It is the pop-up card's own implementation: the same surface, padding, radius and
 * enter and exit motion as [com.makemission.ora.browser.PopupCardHost]. Previous component: none (the page load
 * raised it), so it eases in from the bottom edge. The last findings are kept for the exit. A long list scrolls
 * instead of being cut. Go back appears only when a finding is serious.
 */
@Composable
fun SiteFindingsHost(
    findings: List<SiteFinding>,
    site: String,
    onDismiss: () -> Unit,
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<List<SiteFinding>>(emptyList()) }
    if (findings.isNotEmpty()) shown = findings
    AnimatedVisibility(
        visible = findings.isNotEmpty(),
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) +
            scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) +
            scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
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
            Text(
                if (shown.size == 1) "Something to know about $site" else "${shown.size} things to know about $site",
                color = colors.AddressBarText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            )
            Column(
                modifier = Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                shown.forEach { f ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(f.title, color = colors.AddressBarText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(f.detail, color = colors.SecondaryText, fontSize = 13.sp)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Dismiss",
                    color = colors.SecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.heightIn(min = 44.dp).pressFeedback().clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 12.dp),
                )
                Spacer(Modifier.weight(1f))
                if (shown.any { it.leave }) PillButton(text = "Go back", onClick = onGoBack, primary = true)
            }
        }
    }
}
