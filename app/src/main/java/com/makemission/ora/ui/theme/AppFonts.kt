package com.makemission.ora.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.makemission.ora.R

/**
 * The typefaces Ora can use for its own screens. All are open source (SIL Open Font License 1.1) and bundled
 * as variable fonts in res/font, so choosing one needs no download and works offline. Apple's San Francisco
 * is not licensed for other apps, so Inter (the closest open design) stands in for it.
 */
@OptIn(ExperimentalTextApi::class)
enum class AppFont(val key: String, val label: String, val note: String, private val res: Int?) {
    INSTRUMENT_SANS("instrument_sans", "Instrument Sans", "Ora's own. Clean and slightly condensed", null),
    INTER("inter", "Inter", "Close to San Francisco. Neutral and precise", R.font.inter),
    MANROPE("manrope", "Manrope", "Modern and geometric, with a calm rhythm", R.font.manrope),
    DM_SANS("dm_sans", "DM Sans", "Low contrast and friendly, like Google Sans", R.font.dm_sans),
    NEWSREADER("newsreader", "Newsreader", "A reading serif, close to the one Claude uses", R.font.newsreader),
    FRAUNCES("fraunces", "Fraunces", "A warm, soft serif with character", R.font.fraunces),
    JETBRAINS_MONO("jetbrains_mono", "JetBrains Mono", "Monospaced, for people who like code", R.font.jetbrains_mono);

    val family: FontFamily by lazy {
        val id = res ?: return@lazy InstrumentSans
        FontFamily(
            listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold).map { w ->
                Font(id, weight = w, variationSettings = FontVariation.Settings(FontVariation.weight(w.weight)))
            }
        )
    }

    companion object {
        fun fromKey(key: String?): AppFont = entries.firstOrNull { it.key == key } ?: INSTRUMENT_SANS
    }
}

private val Base = Typography()

/** Material typography with every style set in [font]. */
fun typographyFor(font: AppFont): Typography {
    val f = font.family
    fun TextStyle.s() = copy(fontFamily = f)
    return Typography(
        displayLarge = Base.displayLarge.s(), displayMedium = Base.displayMedium.s(), displaySmall = Base.displaySmall.s(),
        headlineLarge = Base.headlineLarge.s(), headlineMedium = Base.headlineMedium.s(), headlineSmall = Base.headlineSmall.s(),
        titleLarge = Base.titleLarge.s(), titleMedium = Base.titleMedium.s(), titleSmall = Base.titleSmall.s(),
        bodyLarge = Base.bodyLarge.s(), bodyMedium = Base.bodyMedium.s(), bodySmall = Base.bodySmall.s(),
        labelLarge = Base.labelLarge.s(), labelMedium = Base.labelMedium.s(), labelSmall = Base.labelSmall.s(),
    )
}
