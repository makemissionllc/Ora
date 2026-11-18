package com.makemission.ora.ui.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
        // Surfaces are flat: no ripple. Android 12+ draws its ripple with a speckled "sparkle" noise, which
        // showed as grain behind tiles. The press response is the 94% scale from pressFeedback() only.
        content = { CompositionLocalProvider(LocalRippleConfiguration provides null, content = content) }
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
    val SettingsCardRadius = 20.dp

    /** Widest the reader's control bar grows (design: tablet reader view). */
    val ReaderBarWidth = 560.dp

    // Tab grid (phone)
    val GridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    val GridSpacing = 16.dp
    /** Tablet tab overview header: sits above the grid, same side insets. */
    val TabHeaderPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
    val TabTileHeight = 196.dp
    val TabTileShape = RoundedCornerShape(BrowserTokens.RadiusMedium)
    val TabTileElevation = 2.dp
    val SelectionWidth = 2.dp
    val CloseBadgeSize = 32.dp

    // Sidebar (tablet)
    val SidebarWidth = 300.dp
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
    val SidePanelWidth = 380.dp
    val SidePanelRadius = 22.dp
    /** Gap around the tablet ⋯ panel and around the page card beside the sidebar. */
    val PanelInset = 12.dp
    val PageCardRadius = 22.dp

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
    /** The tablet find bar takes the address capsule's slot at this width. */
    val TabletFindWidth = 560.dp
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

/** Mineral swatches from the UI v2 design. Read them through [browserColors], not directly, except for meaning colours. */
object Mineral {
    val Basalt = Color(0xFF1A1B1E)
    val Slate = Color(0xFF232528)
    val Shale = Color(0xFF2D3034)
    val Seam = Color(0xFF383B40)
    val Chalk = Color(0xFFECE6DC)
    val Fog = Color(0xFFA29D94)
    /** Protection working: trackers blocked, secure connection. Meaning only, never decoration. */
    val Lichen = Color(0xFFA9C08C)
    /** Destructive actions: Block, Close all, Clear. Meaning only, never decoration. */
    val Ember = Color(0xFFE58A6B)
    /** Soft tints behind a site's initial in lists. Decoration chosen per host, always with Basalt text. */
    val SiteTints = listOf(Color(0xFFCBD8C6), Color(0xFFCDD6E2), Color(0xFFD9D2E2), Color(0xFFE6D5C0), Color(0xFFDCDCD6))
    val Stone = Color(0xFFE9E8E4)
    val Paper = Color(0xFFF6F5F2)
    val Ink = Color(0xFF1A1B1E)
}

/** Light Mineral: Stone page, Paper bars, Ink text. The selected state is an Ink fill with Paper text. */
val LightBrowserColors = BrowserColors(
    Accent = Mineral.Ink,
    ToolbarBackground = Mineral.Paper,
    ToolbarBorder = Color(0x1A1A1B1E),
    AddressBarBackground = Mineral.Stone,
    AddressBarText = Mineral.Ink,
    SecondaryText = Color(0xFF6C6962),
    IconColor = Mineral.Ink,
    DisabledIcon = Color(0xFFB4B2AB),
    GridBackground = Mineral.Stone,
    CloseBadge = Color(0x9C1A1B1E),
    SidebarBackground = Mineral.Paper,
    SidebarSelected = Mineral.Stone,
    Surface = Mineral.Paper,
    Scrim = Color(0x33000000),
)

/** Dark Mineral (default): Basalt page, Slate bars, Shale tiles, Chalk text. The selected state is a Chalk fill with Basalt text. */
val DarkBrowserColors = BrowserColors(
    Accent = Mineral.Chalk,
    ToolbarBackground = Mineral.Slate,
    ToolbarBorder = Color(0x14ECE6DC),
    AddressBarBackground = Mineral.Shale,
    AddressBarText = Mineral.Chalk,
    SecondaryText = Mineral.Fog,
    IconColor = Mineral.Chalk,
    DisabledIcon = Color(0x66A29D94),
    GridBackground = Mineral.Basalt,
    CloseBadge = Color(0x9CA29D94),
    SidebarBackground = Mineral.Slate,
    SidebarSelected = Mineral.Shale,
    Surface = Mineral.Slate,
    Scrim = Color(0x8C1A1B1E),
)

/**
 * The five profile tints from the UI v2 design. A tint recolours only Ora's own surfaces in the dark themes, never web
 * pages. Chalk, Fog, Lichen and Ember stay the same in every tint. [marker] is the small dot beside a profile's name.
 */
enum class ProfileTint(val key: String, val label: String, val base: Color, val surface: Color, val raised: Color, val marker: Color) {
    BASALT("basalt", "Basalt", Color(0xFF1A1B1E), Color(0xFF232528), Color(0xFF2D3034), Color(0xFF9A968F)),
    MOSS("moss", "Moss", Color(0xFF1A1E19), Color(0xFF232922), Color(0xFF2D342B), Color(0xFF8FA77A)),
    CLAY("clay", "Clay", Color(0xFF201B19), Color(0xFF2A2321), Color(0xFF352C29), Color(0xFFC98E72)),
    TIDE("tide", "Tide", Color(0xFF191C21), Color(0xFF21262D), Color(0xFF2B3139), Color(0xFF7E9BB8)),
    PLUM("plum", "Plum", Color(0xFF1E1A21), Color(0xFF28222C), Color(0xFF332B38), Color(0xFFA88BB8));

    companion object {
        fun fromKey(key: String?): ProfileTint = entries.firstOrNull { it.key == key } ?: BASALT
    }
}

/**
 * The browser palette for a mode and a profile tint. Tints apply to the Dark palette only: Light keeps Stone, Paper
 * and Ink, and Pure dark ignores tints. [ProfileTint.BASALT] is the Dark palette as it already is.
 */
fun browserColors(base: BrowserColors, tint: ProfileTint): BrowserColors {
    if (base != DarkBrowserColors || tint == ProfileTint.BASALT) return base
    return base.copy(
        GridBackground = tint.base,
        ToolbarBackground = tint.surface,
        SidebarBackground = tint.surface,
        Surface = tint.surface,
        AddressBarBackground = tint.raised,
        SidebarSelected = tint.raised,
        ToolbarBorder = base.ToolbarBorder,
    )
}

/** Pure (OLED) black variant: same layout as dark, with true black chrome and surfaces, and the same Mineral text. */
val PureDarkBrowserColors = BrowserColors(
    Accent = Mineral.Chalk,
    ToolbarBackground = Color(0xFF000000),
    ToolbarBorder = Color(0x14ECE6DC),
    AddressBarBackground = Color(0xFF1B1C1E),
    AddressBarText = Mineral.Chalk,
    SecondaryText = Mineral.Fog,
    IconColor = Mineral.Chalk,
    DisabledIcon = Color(0x66A29D94),
    GridBackground = Color(0xFF000000),
    CloseBadge = Color(0x9CA29D94),
    SidebarBackground = Color(0xFF000000),
    SidebarSelected = Color(0xFF1B1C1E),
    Surface = Color(0xFF0E0F10),
    Scrim = Color(0x99000000),
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
fun browserColors(): BrowserColors = LocalBrowserColors.current/** Obsidian: the private space's colours (UI v2). Dark only; Light and Pure dark keep their own palettes. */
fun privateBrowserColors(base: BrowserColors): BrowserColors {
    if (base != DarkBrowserColors && !isTintedDark(base)) return base
    val obsidian = Color(0xFF111214)
    val raised = Color(0xFF1B1C20)
    return base.copy(
        GridBackground = obsidian,
        ToolbarBackground = raised,
        SidebarBackground = raised,
        Surface = raised,
        AddressBarBackground = raised,
        SidebarSelected = Color(0xFF25272C),
    )
}

private fun isTintedDark(base: BrowserColors): Boolean = ProfileTint.values().any { it != ProfileTint.BASALT && base.GridBackground == it.base }

/** True while the selected tab is private. The palette reads it, so the whole browser takes the Obsidian colours. */
object PrivateSpace {
    var active by mutableStateOf(false)
}

