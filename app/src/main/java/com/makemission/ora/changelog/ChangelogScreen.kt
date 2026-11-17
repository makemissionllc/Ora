package com.makemission.ora.changelog

import com.makemission.ora.ui.theme.BrowserTokens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.ui.theme.browserColors

/**
 * What's new: the changes made in each recent update, newest first. Opened from Settings > About.
 * Back closes this page, not the whole Settings overlay.
 */
@Composable
fun ChangelogScreen(onDone: () -> Unit) {
    BackHandler(onBack = onDone)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(browserColors().GridBackground)
            .padding(top = 24.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("What's new", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
            TextButton(onClick = onDone) {
                Text("Done", color = browserColors().Accent, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            "The changes in each update, newest first.",
            fontSize = 13.sp,
            color = browserColors().SecondaryText,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Changelog, key = { it.date }) { day ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BrowserTokens.RadiusMedium))
                        .background(browserColors().AddressBarBackground)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(day.date, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                    Spacer(Modifier.height(8.dp))
                    day.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                item.kind.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = browserColors().Accent,
                                modifier = Modifier.width(64.dp).padding(end = 10.dp, top = 1.dp)
                            )
                            Text(
                                item.text,
                                fontSize = 14.sp,
                                color = browserColors().AddressBarText,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
