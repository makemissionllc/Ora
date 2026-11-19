package com.makemission.ora.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.SegmentLabel
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * Settings > Tabs > Profile rules: pick a site and a profile, and typing that site in the address bar opens it
 * in that profile. Lists the rules with a Remove button, and says what to do when there are none.
 */
@Composable
fun ProfileRulesSection(rules: ProfileRules, profiles: ProfilesRepository) {
    val colors = browserColors()
    val ruleMap by rules.rules.collectAsState()
    val profileList by profiles.profiles.collectAsState()
    var site by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<String?>(null) }
    val target = chosen?.takeIf { id -> profileList.any { it.id == id } } ?: profileList.firstOrNull()?.id
    val valid = ProfileRules.hostOf(site) != null

    Text(
        "Profile rules",
        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 8.dp),
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = colors.SecondaryText,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius))
            .background(colors.Surface)
            .padding(vertical = 8.dp),
    ) {
        Text(
            "Open a site in a profile of its own. Typing the site in the address bar switches to that profile. Private tabs never follow a rule.",
            fontSize = 14.sp,
            color = colors.SecondaryText,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        if (ruleMap.isEmpty()) {
            Text(
                "No rules yet. Add a site and a profile below.",
                fontSize = 14.sp,
                color = colors.SecondaryText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        ruleMap.toSortedMap().forEach { (host, id) ->
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$host  →  ${profileList.firstOrNull { it.id == id }?.name ?: "Removed profile"}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.AddressBarText,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { rules.remove(host) }) { Text("Remove", color = colors.SecondaryText) }
            }
        }
        OutlinedTextField(
            value = site,
            onValueChange = { site = it },
            singleLine = true,
            label = { Text("Site, such as example.com") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            profileList.forEach { p ->
                SegmentLabel(p.name, selected = target == p.id, modifier = Modifier.weight(1f), pill = true) { chosen = p.id }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(
                enabled = valid && target != null,
                onClick = { target?.let { rules.set(site, it); site = "" } },
            ) { Text("Add rule", color = if (valid) colors.Accent else colors.DisabledIcon, fontWeight = FontWeight.SemiBold) }
        }
    }
}
