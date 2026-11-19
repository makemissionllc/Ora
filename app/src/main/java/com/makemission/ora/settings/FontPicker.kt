package com.makemission.ora.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.DisclosureChevron
import com.makemission.ora.ui.theme.AppFont
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * The Font row for Settings > Appearance. Collapsed it shows the current typeface; opened it lists every font
 * set in its own letters, with a preview line that cross-fades when the choice changes. The list folds open
 * on the shared enter motion and the chevron turns with it.
 */
@Composable
fun FontPicker(settings: SettingsRepository) {
    val current by settings.font.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    val turn by animateFloatAsState(if (open) 90f else 0f, OraMotion.enter(), label = "font-chevron")
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { open = !open }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Font", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                Text(current.label, fontSize = 14.sp, color = browserColors().SecondaryText)
            }
            Row(Modifier.rotate(turn)) { DisclosureChevron() }
        }
        val onWeb by settings.webFont.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { settings.setWebFont(!onWeb) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text("Use font on websites", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                Text("Overrides each site's own font. Icons and code are left alone.", fontSize = 14.sp, color = browserColors().SecondaryText)
            }
            com.makemission.ora.browser.MineralSwitch(checked = onWeb, label = "Switch", onChange = { settings.setWebFont(it) })
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()),
            exit = fadeOut(OraMotion.exit()) + shrinkVertically(OraMotion.exit()),
        ) {
            Column(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                AnimatedContent(
                    targetState = current,
                    transitionSpec = { fadeIn(OraMotion.enter()) togetherWith fadeOut(OraMotion.exit()) },
                    label = "font-preview",
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BrowserTokens.RadiusSmall))
                        .background(browserColors().GridBackground),
                ) { font ->
                    Column(Modifier.padding(16.dp)) {
                        Text("Aa  Quiet, quick, and yours", fontFamily = font.family, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                        Text("Ora on every screen reads like this. Web pages keep their own fonts.", fontFamily = font.family, fontSize = 14.sp, color = browserColors().SecondaryText)
                    }
                }
                AppFont.entries.forEach { font ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp)
                            .clickable { settings.setFont(font) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(font.label, fontFamily = font.family, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
                            Text(font.note, fontFamily = font.family, fontSize = 13.sp, color = browserColors().SecondaryText)
                        }
                        if (font == current) Icon(Icons.Filled.Check, contentDescription = "Selected", tint = browserColors().Accent)
                    }
                }
            }
        }
    }
}
