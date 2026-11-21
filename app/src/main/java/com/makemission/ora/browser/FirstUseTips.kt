package com.makemission.ora.browser

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
import com.makemission.ora.onboarding.TextAction
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/** A one-line hint about a gesture, shown once on the first real page. Each tip has its own "seen" flag. */
data class FirstUseTip(val id: String, val title: String, val text: String, val tabletOnly: Boolean = false, val groupOnly: Boolean = false)

val FirstUseTips: List<FirstUseTip> = listOf(
    FirstUseTip("tabs-swipe", "Swipe the bar up for tabs", "Drag the address bar upwards to see all your tabs."),
    FirstUseTip("switch-swipe", "Flip between tabs", "Drag the address bar sideways to move to the next or previous tab. Drag it up to see them all."),
    FirstUseTip("group-strip", "This tab is in a group", "The row shows the group's tabs. Tap an icon to switch, tap + to add a tab to the group, or tap the coloured dot for the group's tabs and options.", groupOnly = true),
    FirstUseTip("quick-switcher", "Find anything fast", "Open the ⋯ menu and choose Quick switcher, or press Ctrl+K, to jump to a tab, bookmark or page."),
    FirstUseTip("search-shortcuts", "Search one site", "Type yt cats or gh compose in the address bar to search YouTube or GitHub directly."),
    FirstUseTip("sidebar-swipe", "Swipe from the left edge", "Drag in from the left edge of the screen to open the tab sidebar.", tabletOnly = true),
)

/**
 * Shows the first tip not yet seen, one at a time, with Got it. [ready] is true on a real page (not the start
 * page) with nothing else asking for attention. Nothing is shown while setup or the notice is open.
 */
@Composable
fun FirstUseTipHost(
    seen: Set<String>,
    tablet: Boolean,
    ready: Boolean,
    /** True while the open tab is in a group, which unlocks the group strip tip. */
    inGroup: Boolean = false,
    onGotIt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val next = FirstUseTips.firstOrNull { it.id !in seen && (!it.tabletOnly || tablet) && (!it.groupOnly || inGroup) }
    var shown by remember { mutableStateOf<FirstUseTip?>(null) }
    if (next != null && ready) shown = next
    AnimatedVisibility(
        visible = next != null && ready,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
        shown?.let { tip ->
            Column(
                Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(browserColors().Surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(tip.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                Text(tip.text, fontSize = 13.sp, lineHeight = 18.sp, color = browserColors().SecondaryText)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextAction("Got it") { onGotIt(tip.id) }
                }
            }
        }
    }
}
