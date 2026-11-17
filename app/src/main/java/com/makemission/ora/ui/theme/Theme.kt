package com.makemission.ora.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
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
    val colorScheme = oraColorScheme(LocalContext.current, darkTheme, dynamicColor)

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
    // Corner radii: one scale for every rounded surface. Pills and the floating bars keep their own shapes.
    /** Small surfaces inside a card: thumbnails, inner rows, badges, chips in lists. */
    val RadiusSmall = 12.dp
    /** Medium surfaces: cards, the address field, tab tiles, settings sections. */
    val RadiusMedium = 16.dp
    /** Large surfaces: menus, side panels, the link menu, start-page tiles. */
    val RadiusLarge = 20.dp

    // Dimensions
    val ToolbarPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    val ToolbarRowSpacing = 6.dp
    val HairlineHeight = 1.dp
    val ToolbarButtonSize = 48.dp
    val AddressBarHeight = 48.dp
    /** Thickness of the load bar along the bottom of the address field. */
    val LoadBarHeight = 3.dp
    // Pull to refresh: how far a page must be pulled down at the top to reload, and the indicator size.
    val PullRefreshThreshold = 96.dp
    val PullRefreshIndicatorSize = 36.dp
    /** Space kept above the floating bottom bar, so the selection card sits clear of it. */
    val SelectionMenuBottomInset = 96.dp
    val AddressBarShape = RoundedCornerShape(BrowserTokens.RadiusMedium)
    val AddressBarElevation = 4.dp
    val AddressBarInnerPadding = 4.dp
    val AddressBarTextStart = 8.dp
    val LockIconSize = 13.dp
    /** Corner radius for cards on Settings, Extensions and the library. */
    val CardRadius = RadiusMedium

    // Tab grid (phone)
    val GridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    val GridSpacing = 16.dp
    /** Tablet tab overview header: sits above the grid, same side insets. */
    val TabHeaderPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp)
    val TabTileHeight = 196.dp
    val TabTileShape = RoundedCornerShape(BrowserTokens.RadiusMedium)
    val TabTileElevation = 2.dp
    val SelectionWidth = 3.dp
    val CloseBadgeSize = 28.dp

    // Sidebar (tablet)
    val SidebarWidth = 272.dp
    // The sidebar is a panel with rounded edges on its open (right) side, so it reads as a surface, not a stripe.
    val SidebarShape = RoundedCornerShape(topEnd = RadiusLarge, bottomEnd = RadiusLarge)
    val SidebarPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
    val SidebarRowHeight = 44.dp
    /** Widest the centered address pill grows on tablet (design: D-Browse). */
    val TabletAddressMaxWidth = 520.dp
    val SidebarSegmentHeight = 32.dp
    /** Gap from the screen edge to the swipe strip, so Android's own back gesture keeps the very edge. */
    val EdgeSwipeInset = 28.dp
    /** Width of the strip a rightward swipe starts from to open the sidebar. */
    val EdgeSwipeWidth = 40.dp
    /** How far a rightward drag from the edge must travel before the sidebar opens. */
    val EdgeSwipeDistance = 56.dp

    // Settings (tablet): a category rail beside one centered column, as in the Settings mockup.
    val SettingsRailWidth = 260.dp
    val SettingsRailRowHeight = 44.dp
    /** The header row of Settings and the other full-screen panels, under the status bar: back, title, Done. */
    val PanelHeaderHeight = 56.dp
    val SettingsContentMaxWidth = 720.dp
    /** Width of the Theme segmented control. Wider than the mockup's 220 dp so every mode label fits. */
    val SettingsSegmentWidth = 280.dp
    /** Tablet side panels (the ⋯ menu) slide in from the right edge at this width. */
    val SidePanelWidth = 360.dp
    val SidePanelRadius = RadiusLarge

    // Link quick menu (long press on a link or image)
    val QuickMenuWidth = 260.dp
    val QuickMenuRadius = RadiusLarge
    val QuickMenuRowHeight = 48.dp
    val QuickMenuEdgeGap = 8.dp

    // Start page (design: M-Start / D-Start).
    val StartContentMaxWidth = 640.dp
    val StartWideBreakpoint = 600.dp
    val StartSearchPadding = 16.dp
    val StartTileSize = 64.dp
    val StartTileRadius = RadiusLarge
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
    /** Pill height and bottom gap once the page scrolls and the Compact bar shrinks, as in iOS Safari. */
    val PhoneCompactShrunkHeight = 34.dp
    /** Tablet Compact shrinks further than the phone bar, so the page keeps more room on scroll. */
    val TabletCompactShrunkHeight = 30.dp
    val TabletCompactShrunkPadding = 4.dp
    val PhoneCompactShrunkBottom = 6.dp
    /** Share of the free width the pill keeps once shrunk; it stays centred between the folded buttons. */
    val PhoneCompactShrunkWidthFraction = 0.6f
    val PhoneCompactCircle = 44.dp
    /** Widest the tablet Compact address pill grows before it stops and the row centres it. */
    val TabletCompactPillMaxWidth = 440.dp
    /** Extra height of the status-bar fade below the status bar, so the page softens into it. */
    val StatusFadeExtra = 16.dp
    /** Total height of the soft fade behind the status bar once a page scrolls under it (design: SB-Scroll). */
    val StatusScrollFade = 84.dp
    val PhoneCompactPill = RoundedCornerShape(percent = 50)
    /** Phone Bottom and Top control bars float above the page as rounded cards, like iOS Safari. */
    val FloatingBarShape = RoundedCornerShape(28.dp)
    val FloatingBarInset = 12.dp
    val FloatingBarElevation = 8.dp
    /** Widest the find-in-page bar grows on a tablet. */
    val FindBarMaxWidth = 480.dp
    // Profile switcher above the tab overview.
    val ProfileChipHeight = 36.dp
    val ProfileChipPadding = PaddingValues(horizontal = 14.dp)
    val ProfileChipSpacing = 8.dp
    val ProfileSwitcherPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    /** How far a page slides when an address bar swipe switches it (Slide, and part of Jiggle). */
    val AddressSwipeDistance = 72.dp
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
    /** Dims the page behind an open side panel. Tapping it closes the panel. */
    val Scrim: Color,
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
    Scrim = Color(0x33000000),
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
    Scrim = Color(0x66000000),
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
    Scrim = Color(0x80000000),
)

/** The Material colour scheme for one mode: wallpaper-based on Android 12+, otherwise the fixed Ora scheme. */
fun oraColorScheme(context: Context, dark: Boolean, dynamicColor: Boolean = true): ColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    dark -> DarkColorScheme
    else -> LightColorScheme
}

/** Which colours the browser's controls use: the original blue design, or Material colours from the theme. */
enum class PaletteStyle(val key: String, val label: String) {
    ORIGINAL("original", "Original"),
    MATERIAL("material", "Material");

    companion object {
        fun fromKey(key: String?): PaletteStyle = entries.firstOrNull { it.key == key } ?: ORIGINAL
    }
}

/**
 * The original palette, with its controls (accent, icons, text, address bar, selected sidebar tab) taken from the
 * Material colour scheme. Backgrounds, the sidebar fill, the top bar and the status strip keep their original colours,
 * so the page look and the frozen status bar do not change.
 */
fun materialBrowserColors(scheme: ColorScheme, base: BrowserColors): BrowserColors = base.copy(
    Accent = scheme.primary,
    AddressBarBackground = scheme.surfaceContainerHigh,
    AddressBarText = scheme.onSurface,
    SecondaryText = scheme.onSurfaceVariant,
    IconColor = scheme.onSurfaceVariant,
    DisabledIcon = scheme.onSurface.copy(alpha = 0.38f),
    SidebarSelected = scheme.primary.copy(alpha = 0.16f),
)

/** Active browser palette, provided by the browser screen. Light by default. */
val LocalBrowserColors = staticCompositionLocalOf { LightBrowserColors }

/** The active browser palette. Read it inside composables: `browserColors().Accent`. */
@Composable
@ReadOnlyComposable
fun browserColors(): BrowserColors = LocalBrowserColors.current