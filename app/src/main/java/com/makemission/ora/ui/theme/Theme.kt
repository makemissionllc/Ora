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
    // Colors
    val Accent = Color(0xFF3B6DF0)
    val ToolbarBackground = Color(0xFFF4F5F7)
    val ToolbarBorder = Color(0x14000000)
    val AddressBarBackground = Color(0xFFE6E8ED)
    val AddressBarText = Color(0xFF23262F)
    val SecondaryText = Color(0xFF6B7080)
    val IconColor = Color(0xFF4A4F5C)
    val DisabledIcon = Color(0xFFB4B4BA)
    val TabStripBackground = Color(0xFFE6E8ED)
    val TabStripActive = Color(0xFFFFFFFF)

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

    // Tab grid (phone)
    val GridBackground = Color(0xFFE9E9EE)
    val GridPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    val GridSpacing = 16.dp
    val TabTileHeight = 196.dp
    val TabTileShape = RoundedCornerShape(14.dp)
    val TabTileElevation = 2.dp
    val SelectionWidth = 3.dp
    val CloseBadge = Color(0x9C3C3C43)
    val CloseBadgeSize = 28.dp

    // Sidebar (tablet)
    val SidebarBackground = Color(0xFFF2F2F6)
    val SidebarSelected = Color(0x1F0B6BDE)
    val SidebarWidth = 272.dp
    val SidebarPadding = PaddingValues(horizontal = 12.dp, vertical = 20.dp)
    val SidebarRowHeight = 44.dp
}