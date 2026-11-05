package com.example.dhikra.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand palette: deep emerald, warm gold and soft ivory neutrals.
val Emerald = Color(0xFF0B6B57)
val EmeraldDeep = Color(0xFF064538)
val Mint = Color(0xFF86D8BE)
val Gold = Color(0xFFB8893A)
val GoldLight = Color(0xFFE9C47E)
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
    /** Decorative gold: stars, dividers, timeline heading marks. */
    val ornament: Color,
    /** Gold dark/light enough to be used for text on the background. */
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

/**
 * The Quran is always set on a deep green "night" card with light ink, in both themes, so it
 * stands apart from every other card. It has no border.
 */
private val QuranLight = ElementColors(
    container = Color(0xFF0F3D30),
    border = Color.Transparent,
    accent = Color(0xFFE7C982),
    onAccent = Color(0xFF2A1F00),
    ink = Color(0xFFF5EFDF),
    highlight = Color(0x40E7C982),
)

private val QuranDark = QuranLight.copy(container = Color(0xFF11372C))

val LightPalette = DhikraPalette(
    quran = QuranLight,
    dua = ElementColors(
        container = Color(0xFFF6F2FC),
        border = Color(0xFFE4DAF3),
        accent = Color(0xFF6A4BA4),
        onAccent = Color.White,
        ink = Color(0xFF251A38),
        highlight = Color(0x266A4BA4),
    ),
    hadith = ElementColors(
        container = Color(0xFFF1F7EE),
        border = Color(0xFFD6E7CE),
        accent = Color(0xFF3B7A2C),
        onAccent = Color.White,
        ink = Color(0xFF1B2A16),
        highlight = Color(0x263B7A2C),
    ),
    dhikr = ElementColors(
        container = Color(0xFFF1F5FD),
        border = Color(0xFFD6E1F5),
        accent = Color(0xFF2B5AA6),
        onAccent = Color.White,
        ink = Color(0xFF152541),
        highlight = Color(0x262B5AA6),
    ),
    task = ElementColors(
        container = Color.White,
        border = Color(0xFFE5E1D6),
        accent = Emerald,
        onAccent = Color.White,
        ink = Color(0xFF1C1B17),
        highlight = Color(0x260B6B57),
    ),
    ornament = Gold,
    ornamentText = Color(0xFF8A6114),
    headerStart = Color(0xFF0F7A62),
    headerEnd = EmeraldDeep,
    tabSelected = Color.White,
    onTabSelected = EmeraldDeep,
    nightStart = Color(0xFF0C3A30),
    nightEnd = Color(0xFF041814),
)

val DarkPalette = DhikraPalette(
    quran = QuranDark,
    dua = ElementColors(
        container = Color(0xFF1D1726),
        border = Color(0xFF3A2E4D),
        accent = Color(0xFFD3BBFF),
        onAccent = Color(0xFF3B2366),
        ink = Color(0xFFEDE4F8),
        highlight = Color(0x40D3BBFF),
    ),
    hadith = ElementColors(
        container = Color(0xFF172216),
        border = Color(0xFF2E4329),
        accent = Color(0xFFA6D690),
        onAccent = Color(0xFF11380A),
        ink = Color(0xFFE2EEDB),
        highlight = Color(0x40A6D690),
    ),
    dhikr = ElementColors(
        container = Color(0xFF151C2B),
        border = Color(0xFF2C3A57),
        accent = Color(0xFFAFC6FF),
        onAccent = Color(0xFF0B2F66),
        ink = Color(0xFFE1E8FA),
        highlight = Color(0x40AFC6FF),
    ),
    task = ElementColors(
        container = Color(0xFF1A1F1D),
        border = Color(0xFF303935),
        accent = Mint,
        onAccent = Color(0xFF00382C),
        ink = Color(0xFFE2E7E4),
        highlight = Color(0x4086D8BE),
    ),
    ornament = Color(0xFFD9B56E),
    ornamentText = GoldLight,
    headerStart = Color(0xFF0E5546),
    headerEnd = Color(0xFF082B24),
    tabSelected = Color(0xFFD4F1E6),
    onTabSelected = Color(0xFF062C24),
    nightStart = Color(0xFF0B3329),
    nightEnd = Color(0xFF030F0C),
)

val LocalDhikraPalette = staticCompositionLocalOf { LightPalette }
