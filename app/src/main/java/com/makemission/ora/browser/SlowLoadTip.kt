package com.makemission.ora.browser

import android.content.Context
import android.net.ConnectivityManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.log.LogPrivacy
import com.makemission.ora.onboarding.TextAction
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import com.makemission.ora.ui.theme.browserColors
import kotlinx.coroutines.delay

/** What a slow page tip says and offers. [retry] is true when a fresh attempt is the useful action. */
data class SlowLoadAdvice(val title: String, val text: String, val retry: Boolean)

/**
 * Picks the tip for a page that has been loading for a while. [online] is false when the device has no
 * network, [progress] is how far the page got (0 to 100), and [saverOn] is true when Data Saver is on.
 */
fun slowLoadAdvice(online: Boolean, progress: Int, saverOn: Boolean): SlowLoadAdvice = when {
    !online -> SlowLoadAdvice(
        "You seem to be offline",
        "Check Wi-Fi or mobile data, then try again. Pages you already opened may still be in your tabs.",
        retry = true,
    )
    progress < STALLED_PROGRESS -> SlowLoadAdvice(
        "This site is slow to answer",
        "The site has not sent anything yet. Waiting a little longer often works, or try again.",
        retry = true,
    )
    saverOn -> SlowLoadAdvice(
        "Data Saver can slow pages down",
        "Android is limiting data in the background. Turn Data Saver off for Ora, or stop loading to keep what came in.",
        retry = false,
    )
    else -> SlowLoadAdvice(
        "Still loading the page",
        "Most of the page is in. Stop loading to keep what is here, or open Reader view for just the text.",
        retry = false,
    )
}

/** Shows [SlowLoadAdvice] once a page has loaded for [SLOW_AFTER_MS] without finishing, and hides it when it ends. */
@Composable
fun SlowLoadTipHost(
    enabled: Boolean,
    url: String,
    loading: Boolean,
    progress: Int,
    onTryAgain: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var slow by remember(url) { mutableStateOf(false) }
    var dismissedFor by remember { mutableStateOf<String?>(null) }
    var advice by remember { mutableStateOf<SlowLoadAdvice?>(null) }
    // The effect below waits 8 s; it must read the progress and address at the end of the wait, not at its start.
    val latestProgress by rememberUpdatedState(progress)
    val latestUrl by rememberUpdatedState(url)
    LaunchedEffect(url, loading, enabled) {
        slow = false
        if (!enabled || !loading || url == BrowserTabs.START_URL) return@LaunchedEffect
        delay(SLOW_AFTER_MS)
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val online = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) != null } ?: false
        val saver = cm?.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED
        advice = slowLoadAdvice(online, latestProgress, saver)
        slow = true
        BrowserLog.info("tips", "Showed a slow page tip for ${LogPrivacy.domainOf(latestUrl)} (${advice?.title})")
    }
    AnimatedVisibility(
        visible = slow && loading && dismissedFor != url,
        modifier = modifier,
        enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
        exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin(0.5f, 1f)),
    ) {
        advice?.let { tip ->
            Column(
                Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(BrowserTokens.CardRadius)).background(browserColors().Surface).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(tip.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = browserColors().AddressBarText)
                Text(tip.text, fontSize = 13.sp, lineHeight = 18.sp, color = browserColors().SecondaryText)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextAction("Keep waiting") { dismissedFor = url }
                    if (tip.retry) {
                        TextAction("Try again") { dismissedFor = url; onTryAgain() }
                    } else {
                        TextAction("Stop loading") { dismissedFor = url; onStop() }
                    }
                }
            }
        }
    }
}

private const val SLOW_AFTER_MS = 8_000L
private const val STALLED_PROGRESS = 20
