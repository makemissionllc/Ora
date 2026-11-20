package com.makemission.ora.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.PillButton
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.browserColors

/**
 * The frame every setup step shares: Back at the top left, Skip at the top right, a segmented progress bar for the
 * numbered steps, a title and a sentence, then the choices. On a tablet the choices sit on the left and [preview]
 * on the right; on a phone there is one column.
 */
@Composable
internal fun SetupScaffold(
    tablet: Boolean,
    title: String,
    subtitle: String,
    onBack: (() -> Unit)?,
    onSkip: (() -> Unit)?,
    primaryLabel: String,
    onPrimary: () -> Unit,
    stepNumber: Int? = null,
    preview: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(browserColors().GridBackground), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = if (tablet) 960.dp else 640.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) TextAction("Back", onBack) else Spacer(Modifier.width(56.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (stepNumber != null) ProgressBar(stepNumber)
                }
                if (onSkip != null) TextAction("Skip", onSkip) else Spacer(Modifier.width(56.dp))
            }
            Row(Modifier.weight(1f).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText)
                    Text(subtitle, fontSize = 14.sp, lineHeight = 20.sp, color = browserColors().SecondaryText)
                    content()
                    Spacer(Modifier.height(8.dp))
                }
                if (tablet && preview != null) {
                    Box(Modifier.weight(1f).fillMaxSize().padding(bottom = 8.dp), contentAlignment = Alignment.Center) { preview() }
                }
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                PillButton(primaryLabel, onClick = onPrimary, primary = true)
            }
        }
    }
}

@Composable
internal fun TextAction(text: String, onClick: () -> Unit) {
    Text(
        text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = browserColors().Accent,
        modifier = Modifier.heightIn(min = 44.dp).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 12.dp),
    )
}

/** One segment for each numbered step (Look, Blocking, Profile, Discover); the ones reached so far are filled. */
@Composable
private fun ProgressBar(current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 1..SetupStep.NUMBERED) {
            Box(
                Modifier.width(36.dp).height(4.dp).clip(CircleShape)
                    .background(if (i <= current) browserColors().Accent else browserColors().AddressBarBackground),
            )
        }
    }
}

/** One selectable card: a preview, a title and a line of detail. The chosen card is filled. */
@Composable
internal fun ChoiceCard(selected: Boolean, title: String, detail: String, onClick: () -> Unit, preview: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 72.dp)
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(if (selected) browserColors().SidebarSelected else browserColors().Surface)
            .clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        preview()
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
            Text(detail, fontSize = 13.sp, lineHeight = 18.sp, color = browserColors().SecondaryText)
        }
        if (selected) Text("✓", fontSize = 16.sp, color = browserColors().Accent)
    }
}

/** A row of pills, one chosen. */
@Composable
internal fun PillChoices(options: List<String>, selectedIndex: Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).fillMaxHeight().heightIn(min = 44.dp).clip(RoundedCornerShape(22.dp))
                    .background(if (i == selectedIndex) browserColors().Accent else browserColors().AddressBarBackground)
                    .clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    fontWeight = if (i == selectedIndex) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (i == selectedIndex) browserColors().GridBackground else browserColors().AddressBarText,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
internal fun SettingRow(title: String, detail: String, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(browserColors().Surface).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = browserColors().AddressBarText)
            Text(detail, fontSize = 13.sp, lineHeight = 18.sp, color = browserColors().SecondaryText)
        }
        control()
    }
}
