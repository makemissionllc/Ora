package com.makemission.ora.browser

import com.makemission.ora.ui.pressFeedback
import com.makemission.ora.ui.oraRowMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.security.shownTitle
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.Mineral
import com.makemission.ora.ui.theme.browserColors
import com.makemission.ora.ui.theme.dashedOutline

/**
 * The private space (UI v2): an explanation card, one list row per private tab with no thumbnail, and
 * "Lock private tabs". Used by the phone overview and the tablet sidebar, which wrap it in the Obsidian colours.
 * Dashed lines mean private here: the favicon outline and the new-tab button.
 */
@Composable
fun PrivateSpacePanel(
    tabs: BrowserTabs,
    onSelect: (Long) -> Unit,
    onNewPrivateTab: () -> Unit,
    /** Null when no PIN is set, so there is nothing to lock with. */
    onLock: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    val privateTabs = tabs.tabs.filter { it.isPrivate }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BrowserTokens.CardRadius))
                .background(colors.Surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Private tabs", color = colors.AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Nothing here is saved: no history, no thumbnails. Closing the last private tab clears its cookies and site choices.",
                color = colors.SecondaryText,
                fontSize = 13.sp,
            )
        }
        LazyColumn(modifier = Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (privateTabs.isEmpty()) {
                item {
                    Text(
                        "No private tabs open.",
                        color = colors.SecondaryText,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    )
                }
            }
            items(privateTabs, key = { it.id }) { tab ->
                val title = tab.shownTitle(displayUrl(tab.url))
                val selected = tab.id == tabs.selectedId
                Row(
                    modifier = oraRowMotion()
                        .fillMaxWidth()
                        .height(BrowserTokens.SidebarRowHeight)
                        .pressFeedback().clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colors.SidebarSelected else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelect(tab.id) }
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(22.dp).dashedOutline(RoundedCornerShape(6.dp), Mineral.Fog, width = 1.dp, dash = 3.dp, gap = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(title.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "•", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.AddressBarText)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = colors.AddressBarText,
                    )
                    Box(
                        modifier = Modifier.size(40.dp).pressFeedback().clip(CircleShape).clickable { tabs.close(tab.id) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close private tab", tint = colors.SecondaryText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        // New private tab: a dashed Chalk outline instead of a fill, so it reads as temporary.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .pressFeedback().clip(CircleShape)
                .clickable(onClick = onNewPrivateTab)
                .dashedOutline(CircleShape, colors.Accent),
            contentAlignment = Alignment.Center,
        ) {
            Text("New private tab", color = colors.Accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        if (onLock != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .pressFeedback().clip(CircleShape)
                    .background(colors.Surface)
                    .clickable(onClick = onLock),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.AddressBarText, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Lock private tabs", color = colors.AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
