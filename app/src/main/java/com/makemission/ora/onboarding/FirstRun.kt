package com.makemission.ora.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import com.makemission.ora.log.BrowserLog
import com.makemission.ora.profiles.ProfilesRepository
import com.makemission.ora.settings.SettingsRepository
import com.makemission.ora.ui.theme.OraMotion

/** Bump this when the setup steps change enough to ask again. Existing installs are not asked, see SettingsRepository. */
const val ONBOARDING_VERSION = 1

/** The setup steps after the notice. Look, Blocking, Profile and Discover are the four numbered ones. */
enum class SetupStep(val key: String, val number: Int?) {
    WELCOME("welcome", null),
    LOOK("look", 1),
    BLOCKING("blocking", 2),
    PROFILE("profile", 3),
    DISCOVER("discover", 4),
    READY("ready", null);

    companion object {
        fun fromKey(key: String?): SetupStep = entries.firstOrNull { it.key == key } ?: WELCOME
        const val NUMBERED = 4
    }
}

/**
 * The first-launch sequence: the "Before you start" notice (step 0), then setup (Welcome, Look, Blocking, Profile,
 * Discover, Ready), then the browser. A fresh install runs all of it. The notice shows again whenever its version changes, and
 * Settings > About > Run setup again shows the setup over the running browser.
 *
 * The browser is composed only once the notice and setup are done, so nothing loads behind them on a fresh install.
 * After that it stays composed, so running setup again does not close the tabs.
 */
@Composable
fun FirstRunHost(settings: SettingsRepository, profiles: ProfilesRepository, browser: @Composable () -> Unit) {
    val accepted by settings.noticeAcceptedVersion.collectAsState()
    val setupDone by settings.onboardingDoneVersion.collectAsState()
    val noticeNeeded = accepted < NOTICE_VERSION
    val setupNeeded = setupDone < ONBOARDING_VERSION
    var browserShown by rememberSaveable { mutableStateOf(false) }
    if (!noticeNeeded && !setupNeeded) browserShown = true
    Box(Modifier.fillMaxSize()) {
        if (browserShown) browser()
        AnimatedVisibility(
            visible = setupNeeded && !noticeNeeded,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin.Center),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin.Center),
        ) {
            SetupFlow(settings, profiles)
        }
        AnimatedVisibility(
            visible = noticeNeeded,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin.Center),
            exit = fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin.Center),
        ) {
            BeforeYouStartScreen(onAccept = { settings.acceptNotice(NOTICE_VERSION) })
        }
    }
}

/**
 * Welcome, Look, Blocking, Profile, Discover and Ready. Every step has Skip (keeps what is set now and moves on) and Back.
 * The current step is saved, so closing Ora midway resumes there. Every choice saves as it is made, and each step
 * starts from the current settings, never from factory defaults.
 */
@Composable
private fun SetupFlow(settings: SettingsRepository, profiles: ProfilesRepository) {
    com.makemission.ora.ui.ScreenOwnsStatusBar()
    val tablet = LocalConfiguration.current.screenWidthDp >= 600
    val stepKey by settings.onboardingStep.collectAsState()
    val step = SetupStep.fromKey(stepKey)
    LaunchedEffect(step) { BrowserLog.info("onboarding", "Onboarding step ${step.key} shown") }

    fun go(next: SetupStep) = settings.setOnboardingStep(next.key)
    fun skip() {
        BrowserLog.info("onboarding", "Onboarding step ${step.key} skipped")
        go(if (step == SetupStep.WELCOME) SetupStep.READY else SetupStep.entries[step.ordinal + 1])
    }
    fun done(next: SetupStep) {
        BrowserLog.info("onboarding", "Onboarding step ${step.key} done")
        go(next)
    }
    val back: (() -> Unit)? = if (step == SetupStep.WELCOME) null else ({ go(SetupStep.entries[step.ordinal - 1]) })
    BackHandler(enabled = back != null) { back?.invoke() }

    AnimatedContent(
        targetState = step,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            (fadeIn(OraMotion.overlayFadeIn()) + scaleIn(OraMotion.overlayScaleIn(), OraMotion.MORPH_SCALE, TransformOrigin.Center)) togetherWith
                (fadeOut(OraMotion.overlayFadeOut()) + scaleOut(OraMotion.overlayScaleOut(), OraMotion.MORPH_SCALE, TransformOrigin.Center))
        },
        label = "setupStep",
    ) { s ->
        when (s) {
            SetupStep.WELCOME -> WelcomeStep(tablet, onSetUp = { done(SetupStep.LOOK) }, onDefaults = { done(SetupStep.READY) }, onSkip = ::skip)
            SetupStep.LOOK -> LookStep(settings, tablet, onBack = back, onSkip = ::skip, onNext = { done(SetupStep.BLOCKING) })
            SetupStep.BLOCKING -> BlockingStep(settings, tablet, onBack = back, onSkip = ::skip, onNext = { done(SetupStep.PROFILE) })
            SetupStep.PROFILE -> ProfileStep(profiles, tablet, onBack = back, onSkip = ::skip, onNext = { done(SetupStep.DISCOVER) })
            SetupStep.DISCOVER -> DiscoverStep(settings, tablet, onBack = back, onSkip = ::skip, onNext = { done(SetupStep.READY) })
            SetupStep.READY -> ReadyStep(settings, profiles, tablet, onBack = back, onFinish = {
                BrowserLog.info("onboarding", "Onboarding step ready done")
                settings.finishOnboarding(ONBOARDING_VERSION)
            })
        }
    }
}
