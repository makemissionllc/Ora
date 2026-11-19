package com.makemission.ora.browser

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.security.shownTitle
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import com.makemission.ora.ui.pressFeedback

/**
 * A slim row of chips for the other tabs in the open tab's group, floating at the bottom of the page, so a tab in
 * the same group is one tap away without opening the overview. It shows only when the open tab is in a group of
 * two or more tabs, and never for private tabs. It grows from and shrinks to its bottom edge.
 */
@Composable
fun GroupTabStrip(tabs: BrowserTabs, modifier: Modifier = Modifier) {
    val selected = tabs.selected
    val group = selected?.takeIf { !it.isPrivate }?.groupId?.let { id -> tabs.groups.firstOrNull { it.id == id } }
    val members = if (group != null) tabs.tabsIn(group.id) else emptyList()
    AnimatedVisibility(
        visible = group != null && members.size >= 2,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), initialScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), targetScale = OraMotion.MORPH_SCALE, transformOrigin = TransformOrigin(0.5f, 1f)),
    ) {
        val colors = browserColors()
        LazyRow(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(colors.AddressBarBackground),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(members, key = { it.id }) { tab ->
                val isOpen = tab.id == tabs.selectedId
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isOpen) colors.Accent else colors.GridBackground)
                        .pressFeedback()
                        .clickable { tabs.select(tab.id) }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(group!!.color.color))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tab.shownTitle(displayUrl(tab.url)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.width(110.dp),
                        fontSize = 13.sp,
                        fontWeight = if (isOpen) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isOpen) colors.GridBackground else colors.AddressBarText,
                    )
                }
            }
        }
    }
}
