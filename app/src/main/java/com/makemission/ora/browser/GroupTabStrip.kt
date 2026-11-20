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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.security.shownTitle
import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * The group strip: shown only while the open tab is in a group (never for a private tab), and hidden with the
 * toolbar when the page scrolls. Phone: a 60 dp row of site icons with the group's dot and chevron on the left (it
 * opens the group sheet) and a plus on the right. Tablet: a 48 dp row of title pills under the address row; it is
 * hidden while the sidebar is open, because the sidebar already shows the group. It grows from and shrinks to its
 * bottom edge (phone) or top edge (tablet).
 */
@Composable
fun GroupTabStrip(
    tabs: BrowserTabs,
    enabled: Boolean,
    tablet: Boolean,
    /** False while the toolbar is folded away, or while the tablet sidebar is open. */
    shown: Boolean,
    state: GroupDialogState,
    onNewTabInGroup: (TabGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selected = tabs.selected
    val group = selected?.takeIf { !it.isPrivate }?.groupId?.let { id -> tabs.groups.firstOrNull { it.id == id } }
    val members = if (group != null) tabs.tabsIn(group.id) else emptyList()
    var sheetFor by remember { mutableStateOf<String?>(null) }
    val origin = TransformOrigin(0.5f, if (tablet) 0f else 1f)
    AnimatedVisibility(
        visible = enabled && shown && group != null,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = origin),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = origin),
    ) {
        // The last group stays drawn while the exit plays.
        val g = group ?: return@AnimatedVisibility
        val colors = browserColors()
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .height(if (tablet) 48.dp else 60.dp)
                .clip(RoundedCornerShape(if (tablet) 24.dp else 30.dp))
                .background(colors.AddressBarBackground)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // The group chip: dot and chevron (phone), dot, name and chevron (tablet). It opens the group sheet.
            Row(
                Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).pressFeedback()
                    .clickable { sheetFor = g.id }
                    .semantics { contentDescription = "Group ${g.name}, ${members.size} tabs" }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(14.dp).clip(CircleShape).background(g.color.color))
                if (tablet) Text(g.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 8.dp).width(90.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = colors.SecondaryText, modifier = Modifier.size(18.dp))
            }
            // Scrolls sideways when there are more tabs than fit.
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(members, key = { it.id }) { tab ->
                    val isOpen = tab.id == tabs.selectedId
                    if (tablet) {
                        Row(
                            Modifier.height(36.dp).clip(RoundedCornerShape(18.dp))
                                .background(if (isOpen) colors.Accent else colors.GridBackground)
                                .pressFeedback().clickable { tabs.select(tab.id) }.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SiteDot(tab, 18.dp)
                            Text(
                                tab.shownTitle(displayUrl(tab.url)), maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 8.dp).width(110.dp), fontSize = 13.sp,
                                fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (isOpen) colors.GridBackground else colors.AddressBarText,
                            )
                        }
                    } else {
                        // The open tab has a ring in the group colour and a small close badge.
                        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .then(if (isOpen) Modifier.border(2.dp, g.color.color, CircleShape) else Modifier)
                                    .pressFeedback().clickable { tabs.select(tab.id) }
                                    .semantics { contentDescription = tab.shownTitle(displayUrl(tab.url)) },
                                contentAlignment = Alignment.Center,
                            ) { SiteDot(tab, 30.dp) }
                            if (isOpen) Box(
                                Modifier.align(Alignment.TopEnd).size(18.dp).clip(CircleShape).background(colors.Accent)
                                    .clickable { BrowserLog.info("groups", "Closed a tab from the group strip"); tabs.close(tab.id) }
                                    .semantics { contentDescription = "Close tab" },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Close, contentDescription = null, tint = colors.GridBackground, modifier = Modifier.size(12.dp)) }
                        }
                    }
                }
            }
            Box(
                Modifier.size(40.dp).clip(CircleShape).pressFeedback()
                    .clickable { onNewTabInGroup(g) }
                    .semantics { contentDescription = "New tab in ${g.name}" },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Add, contentDescription = null, tint = colors.AddressBarText) }
        }
    }
    sheetFor?.let { id ->
        val g = tabs.groups.firstOrNull { it.id == id }
        if (g == null) sheetFor = null else GroupSheet(
            tabs = tabs, group = g, state = state,
            onNewTab = { onNewTabInGroup(g) }, onDismiss = { sheetFor = null },
        )
    }
}

/** The site's initial on a tinted disc. The page's own icon is not available from GeckoView on this build. */
@Composable
private fun SiteDot(tab: BrowserTab, size: androidx.compose.ui.unit.Dp) {
    val colors = browserColors()
    val initial = displayUrl(tab.url).removePrefix("www.").firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "•"
    Box(Modifier.size(size).clip(CircleShape).background(colors.GridBackground), contentAlignment = Alignment.Center) {
        Text(initial, fontSize = (size.value * 0.46f).sp, fontWeight = FontWeight.SemiBold, color = colors.AddressBarText)
    }
}
