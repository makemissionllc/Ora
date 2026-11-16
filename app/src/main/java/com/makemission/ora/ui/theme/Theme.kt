package com.makemission.ora.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun OraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * All browser-specific styling tokens live here, taken from the
 * "Android Browser Mockups" design. Change values here to restyle the browser.
 */
object BrowserTokens {
    // Dimensions
    val ToolbarPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    val ToolbarRowSpacing = 6.dp
    val HairlineHeight = 1.dp
    val ToolbarButtonSize = 48.dp
    val AddressBarHeight = 48.dp
    val AddressBarShape = RoundedCornerShape(14.dp)
    val AddressBarElevation = 4.dp
    val AddressBarInnerPadding = 4.dp
    val AddressBarTextStart = 8.dp
    val LockIconSize = 13.dp
    /** Corner radius for cards on Settings, Extensions and the library. */
    val CardRadius = 12.dp

    // Tab grid (phone)
    val GridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    val GridSpacing = 16.dp
    /** Tablet tab overview header: sits above the grid, same side insets. */
    val TabHeaderPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp)
    val TabTileHeight = 196.dp
    val TabTileShape = RoundedCornerShape(14.dp)
    val TabTileElevation = 2.dp
    val SelectionWidth = 3.dp
    val CloseBadgeSize = 28.dp

    // Sidebar (tablet)
    val SidebarWidth = 272.dp
    val SidebarPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
    val SidebarRowHeight = 44.dp
    /** Widest the centered address pill grows on tablet (design: D-Browse). */
    val TabletAddressMaxWidth = 520.dp
    val SidebarSegmentHeight = 32.dp

    // Link quick menu (long press on a link or image)
    val QuickMenuWidth = 260.dp
    val QuickMenuRadius = 16.dp
    val QuickMenuRowHeight = 48.dp
    val QuickMenuEdgeGap = 8.dp

    // Start page (design: M-Start / D-Start).
    val StartContentMaxWidth = 640.dp
    val StartWideBreakpoint = 600.dp
    val StartSearchPadding = 16.dp
    val StartTileSize = 64.dp
    val StartTileRadius = 16.dp
    val StartTileSlot = 84.dp
    val StartCardPadding = 16.dp
    val StartCardSpacing = 12.dp
    val StartTopPadding = 48.dp

    // Phone Compact bar (design: Safari-style floating row).
    val PhoneCompactPadding = 12.dp
    /** Gap under the floating Compact row, so it hovers above the navigation area. */
    val PhoneCompactFloatBottom = 24.dp
    val PhoneCompactElevation = 8.dp
    val PhoneCompactGap = 8.dp
    val PhoneCompactCircle = 44.dp
    val PhoneCompactPill = RoundedCornerShape(percent = 50)
    /** Phone Bottom and Top control bars float above the page as rounded cards, like iOS Safari. */
    val FloatingBarShape = RoundedCornerShape(28.dp)
    val FloatingBarInset = 12.dp
    val FloatingBarElevation = 8.dp
}

/** Every browser color. Pick one of [LightBrowserColors], [DarkBrowserColors] or [PureDarkBrowserColors]. */
data class BrowserColors(
    val Accent: Color,
    val ToolbarBackground: Color,
    val ToolbarBorder: Color,
    val AddressBarBackground: Color,
    val AddressBarText: Color,
    val SecondaryText: Color,
    val IconColor: Color,
    val DisabledIcon: Color,
    val GridBackground: Color,
    val CloseBadge: Color,
    val SidebarBackground: Color,
    val SidebarSelected: Color,
    /** Page background behind the tab sidebar and grid chrome. */
    val Surface: Color,
)

/** Light palette from the "Android Browser Mockups" design. */
val LightBrowserColors = BrowserColors(
    Accent = Color(0xFF3B6DF0),
    ToolbarBackground = Color(0xFFF4F5F7),
    ToolbarBorder = Color(0x14000000),
    AddressBarBackground = Color(0xFFE6E8ED),
    AddressBarText = Color(0xFF23262F),
    SecondaryText = Color(0xFF6B7080),
    IconColor = Color(0xFF4A4F5C),
    DisabledIcon = Color(0xFFB4B4BA),
    GridBackground = Color(0xFFE9E9EE),
    CloseBadge = Color(0x9C3C3C43),
    SidebarBackground = Color(0xFFF2F2F6),
    SidebarSelected = Color(0x1F0B6BDE),
    Surface = Color(0xFFFFFFFF),
)

/** Dark palette from the "Dark" browser mockups. */
val DarkBrowserColors = BrowserColors(
    Accent = Color(0xFF6C93FF),
    ToolbarBackground = Color(0xFF141518),
    ToolbarBorder = Color(0x14FFFFFF),
    AddressBarBackground = Color(0xFF2E3138),
    AddressBarText = Color(0xFFECEDF0),
    SecondaryText = Color(0xFF9097A6),
    IconColor = Color(0xFFB4B8C4),
    DisabledIcon = Color(0xFF5A5E6A),
    GridBackground = Color(0xFF141518),
    CloseBadge = Color(0x9C8E8E93),
    SidebarBackground = Color(0xFF1A1B1F),
    SidebarSelected = Color(0x1F6C93FF),
    Surface = Color(0xFF23252B),
)

/** Pure (OLED) black variant: same layout as dark, with true black chrome and surfaces. */
val PureDarkBrowserColors = BrowserColors(
    Accent = Color(0xFF6C93FF),
    ToolbarBackground = Color(0xFF000000),
    ToolbarBorder = Color(0x14FFFFFF),
    AddressBarBackground = Color(0xFF1C1C1E),
    AddressBarText = Color(0xFFF2F2F7),
    SecondaryText = Color(0xFF8E8E93),
    IconColor = Color(0xFFC7C7CC),
    DisabledIcon = Color(0xFF48484A),
    GridBackground = Color(0xFF000000),
    CloseBadge = Color(0x9C8E8E93),
    SidebarBackground = Color(0xFF000000),
    SidebarSelected = Color(0x1F6C93FF),
    Surface = Color(0xFF121212),
)

/** Active browser palette, provided by the browser screen. Light by default. */
val LocalBrowserColors = staticCompositionLocalOf { LightBrowserColors }

/** The active browser palette. Read it inside composables: `browserColors().Accent`. */
@Composable
@ReadOnlyComposable
fun browserColors(): BrowserColors = LocalBrowserColors.current