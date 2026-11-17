package com.makemission.ora.browser

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import com.makemission.ora.settings.AddressSwipeEffect
import com.makemission.ora.ui.theme.BrowserTokens
import com.makemission.ora.ui.theme.OraMotion
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.abs

/**
 * Moves the page for a sideways swipe on the address bar. [progress] is the page's displacement: 0 at rest,
 * and +1 or -1 when it has moved fully out, in the direction of the swipe. Swipes queue up, so a second swipe
 * waits for the first to finish rather than cutting it short.
 */
class AddressSwipeMotion {
    val progress = Animatable(0f)
    private val queue = Mutex()

    /**
     * Plays one swipe. [direction] is +1 for a swipe to the left (the next item) and -1 for a swipe to the right.
     * The page moves out, [switch] changes what it shows while it is out of view, and the new page comes in from
     * the other side. When [canSwitch] is false there is nothing that way, so the page only bounces and stays put.
     */
    suspend fun play(direction: Int, effect: AddressSwipeEffect, canSwitch: Boolean, switch: () -> Unit) {
        queue.withLock {
            val sign = direction.toFloat()
            if (!canSwitch) {
                progress.animateTo(sign * EDGE_BOUNCE, OraMotion.exit())
                progress.animateTo(0f, OraMotion.pressRelease())
                return
            }
            progress.animateTo(sign, OraMotion.exit())
            switch()
            progress.snapTo(-sign)
            // Jiggle settles with a spring, so the page bounces into place. Fade and slide use the shared enter timing.
            val settle = if (effect == AddressSwipeEffect.JIGGLE) OraMotion.pressRelease<Float>() else OraMotion.enter<Float>()
            progress.animateTo(0f, settle)
        }
    }

    private companion object {
        /** How far an edge bounce pushes the page, as a fraction of its own displacement. */
        const val EDGE_BOUNCE = 0.12f
    }
}

@Composable
fun rememberAddressSwipeMotion(): AddressSwipeMotion = remember { AddressSwipeMotion() }

/** Moves and fades the page by [motion]'s displacement in the chosen [effect]. */
@Composable
fun Modifier.addressSwipeEffect(motion: AddressSwipeMotion, effect: AddressSwipeEffect): Modifier {
    val slidePx = with(LocalDensity.current) { BrowserTokens.AddressSwipeDistance.toPx() }
    return graphicsLayer {
        val p = motion.progress.value
        val amount = abs(p)
        when (effect) {
            AddressSwipeEffect.FADE -> alpha = 1f - amount
            AddressSwipeEffect.SLIDE -> {
                translationX = -p * slidePx
                alpha = 1f - amount * 0.6f
            }
            AddressSwipeEffect.JIGGLE -> {
                translationX = -p * slidePx * 0.3f
                rotationZ = -p * 2.5f
                alpha = 1f - amount * 0.5f
            }
        }
    }
}
