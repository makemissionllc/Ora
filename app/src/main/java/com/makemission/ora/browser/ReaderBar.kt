package com.makemission.ora.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.extensions.ReaderLook
import com.makemission.ora.extensions.ReaderPage
import com.makemission.ora.extensions.ReaderState
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * The reader's control bar (design: tablet reader view): smaller and larger text, Sans or Serif, three page
 * colours and a close button. It grows out of the bottom edge when the reader opens and shrinks back into it.
 * Every choice is saved in [settings], so the next article opens the same way.
 */
@Composable
fun ReaderBar(reader: ReaderState, settings: SettingsRepository, compact: Boolean, modifier: Modifier = Modifier) {
    val sizeStep by settings.readerSizeStep.collectAsState()
    val serif by settings.readerSerif.collectAsState()
    val page by settings.readerPage.collectAsState()
    val look = ReaderLook(sizeStep, serif, page)

    // A change in the bar restyles the page that is already showing.
    LaunchedEffect(look, reader.open) { reader.restyle(look) }

    AnimatedVisibility(
        visible = reader.open,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)),
    ) {
        val colors = browserColors()
        val button = if (compact) 40.dp else 48.dp
        Row(
            modifier = Modifier
                .padding(horizontal = if (compact) 10.dp else 0.dp)
                .widthIn(max = BrowserTokens.ReaderBarWidth)
                .fillMaxWidth()
                .height(if (compact) 56.dp else 64.dp)
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.Surface)
                .padding(horizontal = if (compact) 6.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 2.dp else 6.dp),
        ) {
            TextSizeButton("Smaller text", 15.sp, button, enabled = sizeStep > 0) { settings.setReaderSizeStep(sizeStep - 1) }
            TextSizeButton("Larger text", 22.sp, button, enabled = sizeStep < ReaderLook.MAX_SIZE_STEP) { settings.setReaderSizeStep(sizeStep + 1) }
            Divider()
            Row(
                modifier = Modifier.clip(CircleShape).background(colors.GridBackground).padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                FaceChoice("Sans", selected = !serif, family = FontFamily.Default) { settings.setReaderSerif(false) }
                FaceChoice("Serif", selected = serif, family = FontFamily.Serif) { settings.setReaderSerif(true) }
            }
            Divider()
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReaderPage.entries.forEach { choice ->
                    PageDot(choice, selected = choice == page, size = if (compact) 20.dp else 28.dp) { settings.setReaderPage(choice) }
                }
            }
            Box(
                modifier = Modifier
                    .size(button)
                    .clip(CircleShape)
                    .background(colors.AddressBarBackground)
                    .clickable(role = Role.Button) { reader.close() }
                    .semantics { contentDescription = "Close reader" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Divider() {
    Box(Modifier.size(width = 1.dp, height = 28.dp).background(browserColors().SecondaryText.copy(alpha = 0.35f)))
}

@Composable
private fun TextSizeButton(label: String, fontSize: androidx.compose.ui.unit.TextUnit, size: androidx.compose.ui.unit.Dp, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text("A", fontSize = fontSize, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText.copy(alpha = if (enabled) 1f else 0.35f))
    }
}

@Composable
private fun FaceChoice(label: String, selected: Boolean, family: FontFamily, onClick: () -> Unit) {
    val colors = browserColors()
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (selected) colors.Accent else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontFamily = family,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.GridBackground else colors.SecondaryText,
        )
    }
}

@Composable
private fun PageDot(choice: ReaderPage, selected: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val colors = browserColors()
    val fill = when (choice) {
        ReaderPage.STONE -> Mineral.Stone
        ReaderPage.PAPER -> Mineral.Paper
        ReaderPage.BASALT -> Mineral.Basalt
    }
    // The chosen colour wears a Chalk ring with a gap, as in the design, so Stone and Paper stay told apart.
    Box(
        modifier = Modifier
            .size(size + 8.dp)
            .border(2.dp, if (selected) colors.Accent else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
            .padding(4.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, colors.SecondaryText.copy(alpha = 0.4f), CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = "${choice.label} page" },
    )
}
