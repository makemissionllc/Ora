package com.makemission.ora.permissions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/** The prompt grows out of the top centre, under the address bar, and shrinks back to the same point. */
private val PROMPT_ORIGIN = TransformOrigin(0.5f, 0f)

/**
 * Shows the first prompt waiting in [SitePrompts]. [topOffset] is the height of the bar above it, so the card
 * sits just under the address bar.
 */
@Composable
fun SitePromptOverlay(topOffset: Dp, modifier: Modifier = Modifier) {
    val queue by SitePrompts.queue.collectAsState()
    val prompt = queue.firstOrNull()
    // Keep the last prompt while it shrinks away, so its text does not change during the exit.
    var lastShown by remember { mutableStateOf<SitePrompt?>(null) }
    if (prompt != null) lastShown = prompt
    AnimatedVisibility(
        visible = prompt != null,
        modifier = modifier.padding(top = topOffset + 8.dp),
        enter = fadeIn(OraMotion.overlayFadeIn()) +
            scaleIn(initialScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleIn(), transformOrigin = PROMPT_ORIGIN),
        exit = fadeOut(OraMotion.overlayFadeOut()) +
            scaleOut(targetScale = OraMotion.MORPH_SCALE, animationSpec = OraMotion.overlayScaleOut(), transformOrigin = PROMPT_ORIGIN),
    ) {
        lastShown?.let { SitePromptCard(it) }
    }
}

@Composable
private fun SitePromptCard(prompt: SitePrompt) {
    val wants = prompt.permissions.joinToString(" and ") { it.label }
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(BrowserTokens.AddressBarElevation, RoundedCornerShape(BrowserTokens.RadiusMedium))
            .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
            .background(browserColors().Surface)
            // Touches on the card belong to the card. The page under it is a native view, and without this it still gets them.
            // Buttons inside the card handle their own clicks first, because this runs after them.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .padding(16.dp),
    ) {
        Text(
            "${prompt.host} asks for $wants",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = browserColors().AddressBarText,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (prompt.privateTab) {
                "Private tab: your choice is not remembered."
            } else {
                "Allow and Block are remembered for this site. Change them in Settings > Privacy."
            },
            fontSize = 13.sp,
            color = browserColors().SecondaryText,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { SitePrompts.answerCurrent(SitePromptChoice.BLOCK) }) {
                Text("Block", color = browserColors().SecondaryText)
            }
            TextButton(onClick = { SitePrompts.answerCurrent(SitePromptChoice.NOT_NOW) }) {
                Text("Not now", color = browserColors().SecondaryText)
            }
            TextButton(onClick = { SitePrompts.answerCurrent(SitePromptChoice.ALLOW) }) {
                Text("Allow", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
