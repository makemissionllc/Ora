package com.makemission.ora.onboarding

import com.makemission.ora.ui.pressFeedback
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.browser.PillButton
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors

/** How long "I understand" stays unavailable, so an accidental tap can't dismiss the notice. */
private const val ARM_DELAY_MS = 1000L

/**
 * The first screen of a first launch. One button, "I understand", which unlocks after about a second.
 * Back is not handled here, so it leaves the app and never skips the notice. "Read the full notice" grows
 * the full text out of the link and shrinks back into it.
 */
@Composable
fun BeforeYouStartScreen(onAccept: () -> Unit) {
    com.makemission.ora.ui.ScreenOwnsStatusBar()
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    var armed by remember { mutableStateOf(false) }
    var showFull by remember { mutableStateOf(false) }
    var origin by remember { mutableStateOf(TransformOrigin(0.5f, 0.8f)) }
    var rootSize by remember { mutableStateOf(Offset(1f, 1f)) }
    LaunchedEffect(Unit) {
        BrowserLog.info("notice", "Before you start shown")
        kotlinx.coroutines.delay(ARM_DELAY_MS)
        armed = true
    }
    val buttonAlpha by animateFloatAsState(if (armed) 1f else 0.4f, OraMotion.enter(), label = "arm")
    Box(
        Modifier.fillMaxSize().background(browserColors().GridBackground).onGloballyPositioned { rootSize = Offset(it.size.width.toFloat(), it.size.height.toFloat()) },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.widthIn(max = if (tablet) 560.dp else 900.dp).fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Before you start", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = browserColors().AddressBarText, modifier = Modifier.padding(top = 12.dp))
                NoticeShort.forEach { (head, body) ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(browserColors().Surface).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(head, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                        Text(body, fontSize = 14.sp, lineHeight = 20.sp, color = browserColors().SecondaryText)
                    }
                }
                Text(NoticeSmallPrint, fontSize = 12.sp, lineHeight = 17.sp, color = browserColors().SecondaryText)
                Text(
                    "Read the full notice",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = browserColors().Accent,
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .onGloballyPositioned {
                            val b = it.boundsInRoot()
                            origin = TransformOrigin(b.center.x / rootSize.x, b.center.y / rootSize.y)
                        }
                        .pressFeedback().clickable {
                            BrowserLog.info("notice", "Full notice opened from Before you start")
                            showFull = true
                        }
                        .padding(vertical = 12.dp),
                )
            }
            Box(Modifier.fillMaxWidth().padding(top = 12.dp).alpha(buttonAlpha), contentAlignment = Alignment.Center) {
                PillButton(
                    "I understand",
                    onClick = { if (armed) onAccept() },
                    primary = true,
                )
            }
        }
        AnimatedVisibility(
            visible = showFull,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, origin),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, origin),
        ) {
            NoticeScreen(tablet = tablet, onDone = { showFull = false })
        }
    }
}
