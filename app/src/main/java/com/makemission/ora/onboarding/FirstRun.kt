package com.makemission.ora.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.TransformOrigin
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraMotion

/**
 * The first-launch sequence. Step 0 is the "Before you start" notice; it is shown whenever the accepted
 * notice version is older than [NOTICE_VERSION], so existing installs see it once and a changed notice
 * shows again. The onboarding steps that follow it are added after this step.
 */
@Composable
fun FirstRunHost(settings: SettingsRepository, browser: @Composable () -> Unit) {
    val accepted by settings.noticeAcceptedVersion.collectAsState()
    val needsNotice = accepted < NOTICE_VERSION
    AnimatedContent(
        targetState = needsNotice,
        transitionSpec = {
            (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin.Center)) togetherWith
                (fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin.Center))
        },
        label = "firstRun",
    ) { notice ->
        if (notice) BeforeYouStartScreen(onAccept = { settings.acceptNotice(NOTICE_VERSION) }) else browser()
    }
}
