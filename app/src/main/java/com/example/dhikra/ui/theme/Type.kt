package com.example.dhikra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.dhikra.R

/**
 * IBM Plex Sans Arabic: a modern, highly legible sans with full tashkeel support. Used for the
 * whole interface and for every card except the Quran, so the app reads as one clean family.
 */
val AppFont = FontFamily(
    Font(R.font.ibm_plex_sans_arabic_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_arabic_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_arabic_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_arabic_bold, FontWeight.Bold),
)

/** Amiri Quran: classic Naskh with full support for the Uthmani script marks. */
val QuranFont = FontFamily(Font(R.font.amiri_quran))

/** Amiri: the mushaf ornaments (surah title cartouche, ayah numbers). */
val MushafFont = FontFamily(
    Font(R.font.amiri_regular, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold),
)

/** Arabic marks sit above and below the letters: never trim the line box around them. */
private val ArabicLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun uiStyle(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = AppFont,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    // Arabic is cursive: any tracking would break the joins between letters.
    letterSpacing = 0.sp,
    lineHeightStyle = ArabicLineHeight,
)

/**
 * Material 3 type scale set in IBM Plex Sans Arabic. Arabic glyphs look smaller than Latin ones at
 * the same size, so body and label styles are a notch larger with roomier line heights.
 */
val Typography = Typography(
    displayLarge = uiStyle(FontWeight.Normal, 57, 68),
    displayMedium = uiStyle(FontWeight.Normal, 45, 56),
    displaySmall = uiStyle(FontWeight.Medium, 36, 48),
    headlineLarge = uiStyle(FontWeight.SemiBold, 32, 44),
    headlineMedium = uiStyle(FontWeight.SemiBold, 28, 40),
    headlineSmall = uiStyle(FontWeight.SemiBold, 24, 34),
    titleLarge = uiStyle(FontWeight.SemiBold, 21, 30),
    titleMedium = uiStyle(FontWeight.SemiBold, 17, 26),
    titleSmall = uiStyle(FontWeight.SemiBold, 15, 22),
    bodyLarge = uiStyle(FontWeight.Normal, 17, 28),
    bodyMedium = uiStyle(FontWeight.Normal, 15, 24),
    bodySmall = uiStyle(FontWeight.Normal, 13, 20),
    labelLarge = uiStyle(FontWeight.Medium, 15, 22),
    labelMedium = uiStyle(FontWeight.Medium, 13, 18),
    labelSmall = uiStyle(FontWeight.Medium, 12, 16),
)

/** Reading styles for each kind of element, scaled by the user's preferred text size. */
object ReadingStyles {
    fun quran(scale: Float) = reading(QuranFont, 23.sp, 2.05f, scale, TextAlign.Justify)
    fun quranOpening(scale: Float) = reading(QuranFont, 21.sp, 1.9f, scale, TextAlign.Center)
    fun surahTitle(scale: Float) =
        reading(MushafFont, 18.sp, 1.5f, scale, TextAlign.Center).copy(fontWeight = FontWeight.Bold)
    fun dua(scale: Float) = reading(AppFont, 19.sp, 1.9f, scale, TextAlign.Justify)
    fun hadith(scale: Float) = reading(AppFont, 18.sp, 1.9f, scale, TextAlign.Justify)
    fun hadithMeta(scale: Float) = reading(AppFont, 13.sp, 1.6f, scale, TextAlign.Start)
    fun dhikr(scale: Float) =
        reading(AppFont, 23.sp, 1.75f, scale, TextAlign.Center).copy(fontWeight = FontWeight.SemiBold)
    fun task(scale: Float) =
        reading(AppFont, 16.sp, 1.65f, scale, TextAlign.Start).copy(fontWeight = FontWeight.Medium)

    private fun reading(
        family: FontFamily,
        size: TextUnit,
        lineHeightFactor: Float,
        scale: Float,
        align: TextAlign,
    ) = TextStyle(
        fontFamily = family,
        fontSize = size * scale,
        lineHeight = size * scale * lineHeightFactor,
        letterSpacing = 0.sp,
        lineHeightStyle = ArabicLineHeight,
        textAlign = align,
    )
}
