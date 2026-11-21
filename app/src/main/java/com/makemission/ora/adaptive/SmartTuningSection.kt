package com.makemission.ora.adaptive

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.notice.InAppNotices
import com.makemission.ora.settings.PreloadMode
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.CheckMark
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * Settings > Diagnostics: a health check that reads Ora's own log in plain words, the switch for how much Ora may tune
 * itself from use, and the list of changes it made with an Undo for each. The check unfolds out of its own row and
 * folds back into it. Everything is worked out on this device.
 */
@Composable
fun SmartTuningSection(settings: SettingsRepository) {
    val colors = browserColors()
    val mode by settings.adaptiveMode.collectAsState()
    val changes by settings.adaptiveChanges.collectAsState()
    val scale by settings.pullScale.collectAsState()
    val preload by settings.preloadMode.collectAsState()
    val entries by BrowserLog.entries.collectAsState()
    var open by rememberSaveable { mutableStateOf(false) }
    var rerun by remember { mutableIntStateOf(0) }
    val insights = remember(entries.size, scale, preload, mode, rerun, changes.size) {
        if (mode == AdaptiveMode.OFF) emptyList()
        else LogAnalyzer.analyse(entries, preload == PreloadMode.ALWAYS, PullLearner.recommendedScale(scale), PullLearner.counts())
    }
    val turn by animateFloatAsState(if (open) 90f else 0f, OraMotion.enter(), label = "check chevron")
    val card = Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.SettingsCardRadius)).background(colors.Surface)

    Column {
        Column(card) {
            Row(
                Modifier.fillMaxWidth().clickable { open = !open }.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f).padding(end = 12.dp)) {
                    Text("Health check", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                    Text(
                        when {
                            mode == AdaptiveMode.OFF -> "Switched off below."
                            insights.isEmpty() -> "Nothing needs attention."
                            insights.size == 1 -> "1 thing to look at."
                            else -> "${insights.size} things to look at."
                        },
                        fontSize = 14.sp, color = colors.SecondaryText,
                    )
                }
                Text("›", fontSize = 20.sp, color = colors.SecondaryText, modifier = Modifier.rotate(turn))
            }
            AnimatedVisibility(
                visible = open,
                enter = fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()),
                exit = fadeOut(OraMotion.exit()) + shrinkVertically(OraMotion.exit()),
            ) {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Ora reads its own log on this device and says what it found. ${entries.size} entries were read; nothing leaves the device.",
                        fontSize = 13.sp, color = colors.SecondaryText,
                    )
                    insights.forEach { insight ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(insight.title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                            Text(insight.detail, fontSize = 14.sp, color = colors.SecondaryText)
                            insight.fix?.let { fix ->
                                TextButton(onClick = {
                                    val said = settings.applyAdaptiveFix(fix, automatic = false)
                                    InAppNotices.show(said ?: "Nothing to change.")
                                }) { Text(fix.label, color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                            }
                        }
                    }
                    TextButton(onClick = { rerun++ }) { Text("Check again", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(card.padding(vertical = 8.dp)) {
            Text(
                "Adapt to how I use Ora. Ora counts which controls you use and how far you really pull or tap, on this device only, and uses that to fit itself to you. No pages or searches are looked at.",
                fontSize = 14.sp, color = colors.SecondaryText,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp),
            )
            AdaptiveMode.entries.forEach { m ->
                Row(
                    Modifier.fillMaxWidth().clickable { settings.setAdaptiveMode(m) }.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(m.label, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = colors.AddressBarText)
                        Text(m.detail, fontSize = 14.sp, color = colors.SecondaryText)
                    }
                    CheckMark(m == mode, "Selected")
                }
            }
        }
        AnimatedVisibility(
            visible = changes.isNotEmpty(),
            enter = fadeIn(OraMotion.enter()) + expandVertically(OraMotion.enter()),
            exit = fadeOut(OraMotion.exit()) + shrinkVertically(OraMotion.exit()),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                Column(card.padding(vertical = 8.dp)) {
                    Text("What Ora changed for you", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.SecondaryText, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    changes.forEach { change ->
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(change.text, fontSize = 14.sp, color = colors.AddressBarText, modifier = Modifier.weight(1f).padding(end = 8.dp))
                            TextButton(onClick = { settings.undoAdaptiveChange(change) }) { Text("Undo", color = colors.Accent, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        }
    }
}

/** Applies the safe changes by themselves when the person chose Automatic, and returns a sentence for each. */
object AdaptiveRunner {
    fun runAutomatic(settings: SettingsRepository): List<String> {
        if (settings.adaptiveMode.value != AdaptiveMode.AUTO) return emptyList()
        val insights = LogAnalyzer.analyse(
            BrowserLog.entries.value,
            settings.preloadMode.value == PreloadMode.ALWAYS,
            PullLearner.recommendedScale(settings.pullScale.value),
            PullLearner.counts(),
        )
        return insights.mapNotNull { it.fix }.distinct().mapNotNull { settings.applyAdaptiveFix(it, automatic = true) }
    }
}
