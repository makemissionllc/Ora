package com.makemission.ora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.makemission.ora.R

/** Instrument Sans, the variable font bundled in res/font, used across the app. */
@OptIn(ExperimentalTextApi::class)
val InstrumentSans = FontFamily(
    listOf(
        FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold,
    ).map { w ->
        Font(
            R.font.instrument_sans,
            weight = w,
            variationSettings = FontVariation.Settings(FontVariation.weight(w.weight), FontVariation.Setting("wdth", 100f)),
        )
    }
)

/** Instrument Sans at 88% width, for address text. */
@OptIn(ExperimentalTextApi::class)
val InstrumentSansAddress = FontFamily(
    listOf(
        FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold,
    ).map { w ->
        Font(
            R.font.instrument_sans,
            weight = w,
            variationSettings = FontVariation.Settings(FontVariation.weight(w.weight), FontVariation.Setting("wdth", 88f)),
        )
    }
)

private fun TextStyle.sans() = copy(fontFamily = InstrumentSans)

private val Base = Typography()

/** Material typography with every style set in Instrument Sans. */
val Typography = Typography(
    displayLarge = Base.displayLarge.sans(), displayMedium = Base.displayMedium.sans(), displaySmall = Base.displaySmall.sans(),
    headlineLarge = Base.headlineLarge.sans(), headlineMedium = Base.headlineMedium.sans(), headlineSmall = Base.headlineSmall.sans(),
    titleLarge = Base.titleLarge.sans(), titleMedium = Base.titleMedium.sans(), titleSmall = Base.titleSmall.sans(),
    bodyLarge = Base.bodyLarge.sans(), bodyMedium = Base.bodyMedium.sans(), bodySmall = Base.bodySmall.sans(),
    labelLarge = Base.labelLarge.sans(), labelMedium = Base.labelMedium.sans(), labelSmall = Base.labelSmall.sans(),
)
