package com.makemission.ora.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.makemission.ora.browser.isLightStatusStrip
import com.makemission.ora.ui.theme.browserColors

/**
 * Who owns the status bar right now. While the browser shows a web page, the bar takes the page's own top colour.
 * A full-screen app page (Settings, Library, Downloads, Extensions, set-up, App lock) calls [ScreenOwnsStatusBar],
 * and the bar then takes that page's background with matching icons, with no scroll fade. When the page closes the
 * browser's tint comes back by itself, because the browser reads [color] and falls back to the page colour.
 */
object StatusBarOwner {
    class Owner(color: Color) {
        var color by mutableStateOf(color)
    }

    private val owners = mutableStateListOf<Owner>()

    /** The background of the page that owns the bar, or null when the browser does. The newest page wins. */
    val color: Color? get() = owners.lastOrNull()?.color

    internal fun add(owner: Owner) { owners.add(owner) }
    internal fun remove(owner: Owner) { owners.remove(owner) }
}

/**
 * Call this from a full-screen app page. The status bar shows the page's own background (Basalt in dark, Stone in
 * light, or the Pure dark and tint variants) with light or dark icons to match, for as long as the page is shown.
 * Pass [visible] false for a page that stays composed while hidden.
 */
@Composable
fun ScreenOwnsStatusBar(visible: Boolean = true) {
    val color = browserColors().GridBackground
    val owner = remember { StatusBarOwner.Owner(color) }
    SideEffect { owner.color = color }
    DisposableEffect(visible) {
        if (visible) StatusBarOwner.add(owner)
        onDispose { StatusBarOwner.remove(owner) }
    }
    // The browser screen sets the icons too, but it is not composed during set-up and sits under the lock screen.
    val window = (LocalContext.current as? Activity)?.window
    LaunchedEffect(color, visible, window) {
        if (visible) window?.let {
            WindowCompat.getInsetsController(it, it.decorView).isAppearanceLightStatusBars = isLightStatusStrip(color)
        }
    }
}
