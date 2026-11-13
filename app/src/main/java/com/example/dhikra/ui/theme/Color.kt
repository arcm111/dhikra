package com.example.dhikra.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand palette. The six base colours are used as they are for fills, rules and ornaments; where a
// colour becomes text on a light card it is deepened (same hue) to stay readable (≥ 4.5:1).
/** Primary: dark green, a little deeper than the softened #215B42 it was (the first was #174B35). */
val Forest = Color(0xFF1B4E38)
val ForestDeep = Color(0xFF133A28)
/** The primary lightened for dark backgrounds. */
val ForestLight = Color(0xFF8CC4A8)
/** Secondary: orange, for calls to action and "now". */
val Orange = Color(0xFFD98945)
/** The secondary orange lightened, for dark backgrounds (the dark theme, the night cards). */
val OrangeLight = Color(0xFFE9A46A)
/** Accent 1: mustard, the decorative gold (stars, rules) and the Quran's colour. */
val Mustard = Color(0xFFD6B85A)
val MustardLight = Color(0xFFE6CF86)
/** Accent 2: dusty rose, the duas. */
val Rose = Color(0xFFC98286)
/** Accent 3: sage, the hadiths. */
val Sage = Color(0xFF8FA9A0)
/** Accent 4: slate blue, the cards' labels, the tasks and the prayer rail. */
val Slate = Color(0xFF7C91B0)
/** The slate blue lightened, for dark backgrounds (the dark theme, the night cards): the colour of time. */
val SlateLight = Color(0xFFA9BAD3)
/** Accent 5: a muted violet, the adhkar and what is important. */
val Purple = Color(0xFF957BC2)
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
    /** The neutral slate blue of the step cards' labels and counts, the tasks and the prayer rail. */
    val slate: ElementColors,
    /** The purple accent (the adhkar's colour), also what is important. */
    val purple: Color,
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
    /**
     * Finished things (completed steps and sections): a golden amber between the secondary orange
     * and the mustard, so "done" reads warm and distinct from the green of what is going on.
     */
    val done: Color,
    /** Text and icons in [done]'s hue, dark/light enough to read on the background. */
    val doneText: Color,
    /**
     * The current time across the day's schedule: a calendar's red, apart from the green of what
     * is going on, the blue of the other prayers and the gold of what is done.
     */
    val now: Color,
    /** Steps done along a schedule card: a deep shade of the secondary orange, clear against the card. */
    val progress: Color,
)

val LightPalette = DhikraPalette(
    // Each element card is a soft tint of its accent colour, with the accent deepened for text.
    // The Quran: a fresher shade of the primary green.
    quran = ElementColors(
        container = Color(0xFFEEF5F0),
        border = Color(0xFFD0E3D7),
        accent = Color(0xFF3A7D5C),
        onAccent = Color.White,
        ink = Color(0xFF14261C),
        highlight = Color(0x333A7D5C),
    ),
    // The duas: a clear blue.
    dua = ElementColors(
        container = Color(0xFFEDF4FA),
        border = Color(0xFFCFE0EE),
        accent = Color(0xFF2F6A9F),
        onAccent = Color.White,
        ink = Color(0xFF14222E),
        highlight = Color(0x332F6A9F),
    ),
    hadith = ElementColors(
        container = Color(0xFFEFF4F2),
        border = Color(0xFFD3E0DB),
        accent = Color(0xFF536F65),
        onAccent = Color.White,
        ink = Color(0xFF1A2420),
        highlight = Color(0x408FA9A0),
    ),
    // The adhkar: the purple accent.
    dhikr = ElementColors(
        container = Color(0xFFF4F0F9),
        border = Color(0xFFE2D9EF),
        accent = Color(0xFF7A5BA6),
        onAccent = Color.White,
        ink = Color(0xFF221A2E),
        highlight = Color(0x33957BC2),
    ),
    task = ElementColors(
        container = Color.White,
        border = Color(0xFFE5E1D6),
        accent = Forest,
        onAccent = Color.White,
        ink = Color(0xFF1C1B17),
        highlight = Color(0x26174B35),
    ),
    slate = ElementColors(
        container = Color(0xFFEEF2F8),
        border = Color(0xFFD3DCEA),
        accent = Color(0xFF546C91),
        onAccent = Color.White,
        ink = Color(0xFF172030),
        highlight = Color(0x337C91B0),
    ),
    purple = Color(0xFF7A5BA6),
    ornament = Mustard,
    ornamentText = Color(0xFF866D21),
    headerStart = Color(0xFF215C43),
    headerEnd = ForestDeep,
    tabSelected = Color.White,
    onTabSelected = ForestDeep,
    nightStart = Color(0xFF20523C),
    nightEnd = Color(0xFF0F281D),
    done = Color(0xFFD9A043),
    doneText = Color(0xFF8C5A12),
    now = Color(0xFFD23F3F),
    progress = Color(0xFFC2611A),
)

val DarkPalette = DhikraPalette(
    // On dark cards the base colours are light enough for text as they are (or lifted a little).
    quran = ElementColors(
        container = Color(0xFF18211C),
        border = Color(0xFF2F4237),
        accent = Color(0xFF93CFAE),
        onAccent = Color(0xFF0D2A1B),
        ink = Color(0xFFE2EDE6),
        highlight = Color(0x4093CFAE),
    ),
    dua = ElementColors(
        container = Color(0xFF182029),
        border = Color(0xFF2E3E4E),
        accent = Color(0xFF9CC3E6),
        onAccent = Color(0xFF0E2538),
        ink = Color(0xFFE2EAF2),
        highlight = Color(0x407BA7D0),
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
        container = Color(0xFF1F1A27),
        border = Color(0xFF3D3350),
        accent = Color(0xFFC3ACE6),
        onAccent = Color(0xFF2A1840),
        ink = Color(0xFFEAE3F2),
        highlight = Color(0x40957BC2),
    ),
    task = ElementColors(
        container = Color(0xFF1A1F1D),
        border = Color(0xFF303935),
        accent = ForestLight,
        onAccent = Color(0xFF0B2A1D),
        ink = Color(0xFFE2E7E4),
        highlight = Color(0x408CC4A8),
    ),
    slate = ElementColors(
        container = Color(0xFF1A1E26),
        border = Color(0xFF333C4D),
        accent = SlateLight,
        onAccent = Color(0xFF1A2840),
        ink = Color(0xFFE3E8F0),
        highlight = Color(0x407C91B0),
    ),
    purple = Color(0xFFC3ACE6),
    ornament = Mustard,
    ornamentText = MustardLight,
    headerStart = Color(0xFF225A42),
    headerEnd = Color(0xFF143627),
    tabSelected = Color(0xFFD4EADF),
    onTabSelected = Color(0xFF0C271B),
    nightStart = Color(0xFF1C4A35),
    nightEnd = Color(0xFF0C2017),
    done = Color(0xFFE5B261),
    doneText = Color(0xFFF0C987),
    now = Color(0xFFFF6E6E),
    progress = Color(0xFFF5A35C),
)

val LocalDhikraPalette = staticCompositionLocalOf { LightPalette }
