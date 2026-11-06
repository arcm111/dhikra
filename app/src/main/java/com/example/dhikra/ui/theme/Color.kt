package com.example.dhikra.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand palette. The six base colours are used as they are for fills, rules and ornaments; where a
// colour becomes text on a light card it is deepened (same hue) to stay readable (≥ 4.5:1).
/** Primary: dark green. */
val Forest = Color(0xFF174B35)
val ForestDeep = Color(0xFF113826)
/** The primary lightened for dark backgrounds. */
val ForestLight = Color(0xFF8CC4A8)
/** Secondary: orange, for calls to action and "now". */
val Orange = Color(0xFFD98945)
/** Accent 1: mustard, the decorative gold (stars, rules) and the Quran's colour. */
val Mustard = Color(0xFFD6B85A)
val MustardLight = Color(0xFFE6CF86)
/** Accent 2: dusty rose, the duas. */
val Rose = Color(0xFFC98286)
/** Accent 3: sage, the hadiths. */
val Sage = Color(0xFF8FA9A0)
/** Accent 4: slate blue, the adhkar. */
val Slate = Color(0xFF7C91B0)
val Ivory = Color(0xFFF9F7F2)

/** Colors of one element style (Quran, Dua, Hadith...). */
@Immutable
data class ElementColors(
    val container: Color,
    val border: Color,
    val accent: Color,
    /** Content drawn on top of a solid [accent] fill (counter progress, done button). */
    val onAccent: Color,
    val ink: Color,
    /** Background behind the word currently being read. */
    val highlight: Color,
)

@Immutable
data class DhikraPalette(
    val quran: ElementColors,
    val dua: ElementColors,
    val hadith: ElementColors,
    val dhikr: ElementColors,
    val task: ElementColors,
    /** Decorative mustard: stars, dividers, timeline heading marks. */
    val ornament: Color,
    /** Mustard dark/light enough to be used for text on the background. */
    val ornamentText: Color,
    /** The branded header (top bar + tabs) gradient. */
    val headerStart: Color,
    val headerEnd: Color,
    /** Selected tab inside the header. */
    val tabSelected: Color,
    val onTabSelected: Color,
    /** The immersive counter's "night" gradient. */
    val nightStart: Color,
    val nightEnd: Color,
)

val LightPalette = DhikraPalette(
    // Each element card is a soft tint of its accent colour, with the accent deepened for text.
    quran = ElementColors(
        container = Color(0xFFFBF6E6),
        border = Color(0xFFEDE0B6),
        accent = Color(0xFF866D21),
        onAccent = Color.White,
        ink = Color(0xFF2A2414),
        highlight = Color(0x40D6B85A),
    ),
    dua = ElementColors(
        container = Color(0xFFFAF0F0),
        border = Color(0xFFEDD5D7),
        accent = Color(0xFFA9545A),
        onAccent = Color.White,
        ink = Color(0xFF2B1718),
        highlight = Color(0x33C98286),
    ),
    hadith = ElementColors(
        container = Color(0xFFEFF4F2),
        border = Color(0xFFD3E0DB),
        accent = Color(0xFF536F65),
        onAccent = Color.White,
        ink = Color(0xFF1A2420),
        highlight = Color(0x408FA9A0),
    ),
    dhikr = ElementColors(
        container = Color(0xFFEEF2F8),
        border = Color(0xFFD3DCEA),
        accent = Color(0xFF546C91),
        onAccent = Color.White,
        ink = Color(0xFF172030),
        highlight = Color(0x337C91B0),
    ),
    task = ElementColors(
        container = Color.White,
        border = Color(0xFFE5E1D6),
        accent = Forest,
        onAccent = Color.White,
        ink = Color(0xFF1C1B17),
        highlight = Color(0x26174B35),
    ),
    ornament = Mustard,
    ornamentText = Color(0xFF866D21),
    headerStart = Color(0xFF1F5E43),
    headerEnd = ForestDeep,
    tabSelected = Color.White,
    onTabSelected = ForestDeep,
    nightStart = Color(0xFF163F2D),
    nightEnd = Color(0xFF07160F),
)

val DarkPalette = DhikraPalette(
    // On dark cards the base colours are light enough for text as they are (or lifted a little).
    quran = ElementColors(
        container = Color(0xFF221F15),
        border = Color(0xFF453D24),
        accent = Mustard,
        onAccent = Color(0xFF2A2208),
        ink = Color(0xFFF0EADA),
        highlight = Color(0x40D6B85A),
    ),
    dua = ElementColors(
        container = Color(0xFF231A1B),
        border = Color(0xFF463234),
        accent = Color(0xFFDDA3A6),
        onAccent = Color(0xFF3A1418),
        ink = Color(0xFFF2E3E4),
        highlight = Color(0x40C98286),
    ),
    hadith = ElementColors(
        container = Color(0xFF1A201E),
        border = Color(0xFF33403B),
        accent = Color(0xFFAFC6BE),
        onAccent = Color(0xFF1A2B25),
        ink = Color(0xFFE3ECE8),
        highlight = Color(0x408FA9A0),
    ),
    dhikr = ElementColors(
        container = Color(0xFF1A1E26),
        border = Color(0xFF333C4D),
        accent = Color(0xFFA9BAD3),
        onAccent = Color(0xFF1A2840),
        ink = Color(0xFFE3E8F0),
        highlight = Color(0x407C91B0),
    ),
    task = ElementColors(
        container = Color(0xFF1A1F1D),
        border = Color(0xFF303935),
        accent = ForestLight,
        onAccent = Color(0xFF0B2A1D),
        ink = Color(0xFFE2E7E4),
        highlight = Color(0x408CC4A8),
    ),
    ornament = Mustard,
    ornamentText = MustardLight,
    headerStart = Color(0xFF194A35),
    headerEnd = Color(0xFF0C271B),
    tabSelected = Color(0xFFD4EADF),
    onTabSelected = Color(0xFF0C271B),
    nightStart = Color(0xFF133826),
    nightEnd = Color(0xFF050F0A),
)

val LocalDhikraPalette = staticCompositionLocalOf { LightPalette }
