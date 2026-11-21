package com.makemission.ora.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/** How much Ora vibrates. Saved in Settings. */
enum class HapticStrength(val key: String, val label: String, val detail: String) {
    OFF("off", "Off", "No vibration from Ora"),
    SUBTLE("subtle", "Subtle", "Only for things that matter: long presses, saved, closed, refused"),
    FULL("full", "Full", "Also a light tick on every press and choice");

    companion object {
        fun fromKey(key: String?): HapticStrength = entries.firstOrNull { it.key == key } ?: FULL
    }
}

/**
 * The kinds of touch response, from lightest to heaviest. Each one is a fixed feeling, so the same event feels the same
 * everywhere. [minimum] is the lowest [HapticStrength] that plays it: the ticks need Full, the rest play on Subtle too.
 */
enum class Feel(val minimum: HapticStrength) {
    /** Finger down on a button or tile. */
    TICK(HapticStrength.FULL),
    /** A choice was made: a tab opened, a chip picked, a row tapped. */
    SELECT(HapticStrength.FULL),
    TOGGLE_ON(HapticStrength.SUBTLE),
    TOGGLE_OFF(HapticStrength.SUBTLE),
    /** A drag or pull passed the point where letting go will act. */
    THRESHOLD(HapticStrength.SUBTLE),
    /** A press held long enough to open a menu or lift an item. */
    LONG_PRESS(HapticStrength.SUBTLE),
    /** Something was saved or finished: bookmark added, tab restored. */
    CONFIRM(HapticStrength.SUBTLE),
    /** Something was closed or removed. */
    CLOSE(HapticStrength.SUBTLE),
    /** An action was refused: nothing to do, not allowed. */
    REJECT(HapticStrength.SUBTLE),
}

/** Whether [feel] plays at [strength]. Off plays nothing. */
fun feelAllowed(strength: HapticStrength, feel: Feel): Boolean =
    strength != HapticStrength.OFF && strength.ordinal >= feel.minimum.ordinal

/** Plays [Feel]s at the user's chosen [HapticStrength]. Read from [LocalOraHaptics]; the strength is read at each call. */
class OraHaptics(private val platform: HapticFeedback?, private val strength: () -> HapticStrength) {
    fun play(feel: Feel) {
        if (platform == null || !feelAllowed(strength(), feel)) return
        platform.performHapticFeedback(
            when (feel) {
                Feel.TICK -> HapticFeedbackType.SegmentFrequentTick
                Feel.SELECT -> HapticFeedbackType.SegmentTick
                Feel.TOGGLE_ON -> HapticFeedbackType.ToggleOn
                Feel.TOGGLE_OFF -> HapticFeedbackType.ToggleOff
                Feel.THRESHOLD -> HapticFeedbackType.GestureThresholdActivate
                Feel.LONG_PRESS -> HapticFeedbackType.LongPress
                Feel.CONFIRM -> HapticFeedbackType.Confirm
                Feel.CLOSE -> HapticFeedbackType.GestureEnd
                Feel.REJECT -> HapticFeedbackType.Reject
            }
        )
    }
    fun toggle(on: Boolean) = play(if (on) Feel.TOGGLE_ON else Feel.TOGGLE_OFF)

    companion object { val Silent = OraHaptics(null) { HapticStrength.OFF } }
}

val LocalOraHaptics = compositionLocalOf { OraHaptics.Silent }

/** Builds the haptics for the app root from the saved strength. */
@Composable
fun rememberOraHaptics(strength: State<HapticStrength>): OraHaptics {
    val platform = LocalHapticFeedback.current
    return remember(platform) { OraHaptics(platform) { strength.value } }
}
