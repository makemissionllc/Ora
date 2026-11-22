package com.makemission.ora.extensions

import com.makemission.ora.ui.pressFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
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
import com.makemission.ora.browser.PillButton
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/**
 * The one confirmation before an extension is added: what it is, where it came from, what it can do, and the
 * "Allow in private tabs" switch (off). The list of abilities scrolls when it is long, so nothing is cut off.
 */
@Composable
fun ExtensionInstallSheet(
    pending: PendingInstall,
    onCancel: () -> Unit,
    onAdd: (allowPrivate: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = browserColors()
    var allowPrivate by remember(pending) { mutableStateOf(false) }
    Column(
        modifier = modifier
            .widthIn(max = 460.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(BrowserTokens.CardRadius))
            .background(colors.AddressBarBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "Add ${pending.name}?",
                    color = colors.AddressBarText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (pending.version.isNotBlank()) {
                    Text("Version ${pending.version}", color = colors.SecondaryText, fontSize = 13.sp)
                }
                Text(
                    when {
                        pending.fromFile -> "From a file on this device"
                        pending.fromAmo && pending.signed -> "Signed by Mozilla, from addons.mozilla.org"
                        pending.fromAmo -> "From addons.mozilla.org"
                        else -> "From ${pending.source}, not addons.mozilla.org"
                    },
                    color = colors.SecondaryText,
                    fontSize = 13.sp,
                )
            }
            Text("It can", color = colors.SecondaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (pending.abilities.isEmpty()) {
                Text("Nothing beyond the pages you open it on.", color = colors.AddressBarText, fontSize = 14.sp)
            } else {
                pending.abilities.forEach { Text("· $it", color = colors.AddressBarText, fontSize = 14.sp) }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .pressFeedback().clickable { allowPrivate = !allowPrivate },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Allow in private tabs", color = colors.AddressBarText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    Text("Off unless you turn it on.", color = colors.SecondaryText, fontSize = 12.sp)
                }
                Switch(checked = allowPrivate, onCheckedChange = { allowPrivate = it })
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Cancel",
                color = colors.SecondaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.height(44.dp).pressFeedback().clickable(onClick = onCancel).padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            PillButton(text = "Add extension", onClick = { onAdd(allowPrivate) }, primary = true)
        }
    }
}

/** Shows [pending] on the shared overlay motion, growing from and shrinking to the bottom edge where it sits. */
@Composable
fun ExtensionInstallHost(
    pending: PendingInstall?,
    onCancel: () -> Unit,
    onAdd: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf<PendingInstall?>(null) }
    if (pending != null) shown = pending
    AnimatedVisibility(
        visible = pending != null,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
        shown?.let { ExtensionInstallSheet(it, onCancel, onAdd, Modifier.padding(horizontal = 16.dp)) }
    }
}
