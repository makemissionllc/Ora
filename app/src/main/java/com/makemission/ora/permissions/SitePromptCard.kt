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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.makemission.ora.ui.theme.Mineral
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

private fun SitePermission.icon(): ImageVector = when (this) {
    SitePermission.LOCATION -> Icons.Outlined.LocationOn
    SitePermission.CAMERA -> Icons.Outlined.Videocam
    SitePermission.MICROPHONE -> Icons.Outlined.Mic
    SitePermission.NOTIFICATIONS -> Icons.Outlined.Notifications
}

@Composable
private fun SitePromptCard(prompt: SitePrompt) {
    val wants = prompt.permissions.joinToString(" and ") { it.label }
    val colors = browserColors()
    val shape = RoundedCornerShape(BrowserTokens.RadiusLarge)
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .shadow(BrowserTokens.AddressBarElevation, shape)
            .clip(shape)
            .background(colors.Surface)
            // Touches on the card belong to the card. The page under it is a native view, and without this it still gets them.
            // Buttons inside the card handle their own clicks first, because this runs after them.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(colors.AddressBarBackground),
                contentAlignment = Alignment.Center,
            ) {
                Icon(prompt.permissions.first().icon(), contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.size(22.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Let ${prompt.host} use $wants?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    color = colors.AddressBarText,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                // Quality of life: say what "Not now" does, so nobody fears it saved a Block.
                Text(
                    if (prompt.privateTab) {
                        "Private tab: your choice is not remembered."
                    } else {
                        "Allow and Block are remembered for this site. Not now asks again next time."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.SecondaryText,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PromptButton("Block", Mineral.Ember, Color.Transparent) { SitePrompts.answerCurrent(SitePromptChoice.BLOCK) }
            Spacer(Modifier.weight(1f))
            PromptButton("Not now", colors.AddressBarText, colors.AddressBarBackground) { SitePrompts.answerCurrent(SitePromptChoice.NOT_NOW) }
            PromptButton("Allow", colors.GridBackground, colors.Accent) { SitePrompts.answerCurrent(SitePromptChoice.ALLOW) }
        }
    }
}

/** A 48 dp pill button: Block is bare Ember, Not now is Shale, Allow is the one Chalk action. */
@Composable
private fun PromptButton(text: String, content: Color, fill: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = content, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
