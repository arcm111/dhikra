package com.example.dhikra.ui.components

import android.icu.text.MessageFormat
import android.icu.util.ULocale
import android.os.SystemClock
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import com.example.dhikra.data.ArabicText
import com.example.dhikra.ui.theme.ElementColors

/** A word (or whitespace / ayah marker) of a text that can be read progressively. */
private class Token(val text: String, val isMarker: Boolean, val endWeight: Float)

private fun tokenize(text: String): List<Token> {
    var cumulative = 0f
    return Regex("""\s+|\S+""").findAll(text).map { match ->
        val value = match.value
        val isMarker = ArabicText.isAyahMarker(value)
        cumulative += when {
            value.isBlank() -> 0f
            isMarker -> MARKER_PAUSE_WEIGHT
            else -> ArabicText.letterWeight(value).coerceAtLeast(1).toFloat()
        }
        Token(value, isMarker, cumulative)
    }.toList()
}

private const val MARKER_PAUSE_WEIGHT = 2f
private const val AYAH_ID_PREFIX = "ayah:"
private const val NO_BREAK_SPACE = " "

/** One pixel of safety per line so a justified line never wraps differently than measured. */
private const val JUSTIFY_SAFETY_PX = 1f

/**
 * Displays [text] with an optional "reader" effect: while [playing], words are highlighted one
 * after another at [lettersPerSecond], like a teleprompter guiding the recitation. Changing
 * [cycle] restarts the effect from the first word (one cycle == one repetition).
 *
 * @param ornamentalAyahMarkers draws ayah numbers as gold rosettes (mushaf style) instead of
 * relying on the font's ۝ sign. Each marker is kept on the same line as the word it closes.
 * @param onCycleStart receives the uptime (ms) at which the current cycle started.
 * @param onReachedEnd called with true when the highlight reached the last word of the cycle.
 */
@Composable
fun ReaderText(
    text: String,
    style: TextStyle,
    colors: ElementColors,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    cycle: Int = 0,
    lettersPerSecond: Float = 7f,
    ornamentalAyahMarkers: Boolean = false,
    onCycleStart: (Long) -> Unit = {},
    onReachedEnd: (Boolean) -> Unit = {},
) {
    val tokens = remember(text) { tokenize(text) }
    var current by remember(text) { mutableIntStateOf(-1) }
    val speed by rememberUpdatedState(lettersPerSecond)
    val cycleStart by rememberUpdatedState(onCycleStart)
    val reachedEnd by rememberUpdatedState(onReachedEnd)

    LaunchedEffect(playing, cycle, tokens) {
        reachedEnd(false)
        if (!playing) {
            current = -1
            return@LaunchedEffect
        }
        val start = SystemClock.uptimeMillis()
        cycleStart(start)
        current = 0
        while (current < tokens.size) {
            withFrameMillis { }
            val readWeight = (SystemClock.uptimeMillis() - start) / 1000f * speed
            val index = tokens.indexOfFirst { it.endWeight > readWeight }
                .let { if (it < 0) tokens.size else it }
            if (index != current) current = index
        }
        // Highlight reached the end: wait for the user's tap (which starts the next cycle).
        reachedEnd(true)
    }

    // Ayah numbers present in the text, with their spoken form (for screen readers).
    val markers = remember(tokens, ornamentalAyahMarkers) {
        if (!ornamentalAyahMarkers) emptyMap()
        else tokens.filter { it.isMarker }.map { it.text.drop(1) }.distinct()
            .associateWith(::spokenAyahNumber)
    }
    // One entry per ayah number: the number travels in the id, not in the alternate text.
    val inlineContent = remember(markers, colors, style.fontSize) {
        markers.keys.associate { number ->
            val long = number.length > 2
            AYAH_ID_PREFIX + number to ayahMarker(
                number = number,
                widthEm = if (long) 1.8f else 1.4f,
                colors = colors,
                numberSize = style.fontSize * (if (long) 0.44f else 0.5f),
            )
        }
    }
    val textStyle = style.copy(color = colors.ink)

    // The platform's justification ignores inline content (the rosettes would drift away from
    // their slots), so a justified text with rosettes is justified here, by widening its spaces.
    if (markers.isEmpty() || style.textAlign != TextAlign.Justify) {
        val annotated = remember(tokens, current, colors, markers) {
            buildReaderText(tokens, current, colors, markers, inlineContent, justification = null).text
        }
        Text(text = annotated, style = textStyle, inlineContent = inlineContent, modifier = modifier)
        return
    }

    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val startAligned = textStyle.copy(textAlign = TextAlign.Start)
        val justification = remember(tokens, markers, inlineContent, startAligned, width, density) {
            if (width == Constraints.Infinity) {
                null
            } else {
                val natural = buildReaderText(tokens, -1, colors, markers, inlineContent, justification = null)
                val layout = measurer.measure(
                    text = natural.text,
                    style = startAligned,
                    placeholders = natural.placeholders,
                    constraints = Constraints(maxWidth = width),
                    density = density,
                )
                justify(tokens, natural, layout, width, density)
            }
        }
        val annotated = remember(tokens, current, colors, markers, justification) {
            buildReaderText(tokens, current, colors, markers, inlineContent, justification).text
        }
        Text(text = annotated, style = startAligned, inlineContent = inlineContent)
    }
}

/** The text with its reading-progress colors, rosette placeholders and optional justification. */
private class ReaderString(
    val text: AnnotatedString,
    /** Offset of each token in [text]. */
    val tokenStarts: IntArray,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
)

/** Line ends turned into hard breaks, and the extra spacing given to each stretchable space. */
private class Justification(val breaks: Set<Int>, val stretch: Map<Int, TextUnit>)

private fun buildReaderText(
    tokens: List<Token>,
    current: Int,
    colors: ElementColors,
    markers: Map<String, String>,
    inlineContent: Map<String, InlineTextContent>,
    justification: Justification?,
): ReaderString {
    val starts = IntArray(tokens.size)
    val placeholders = mutableListOf<AnnotatedString.Range<Placeholder>>()
    val markerStyle = SpanStyle(color = colors.accent)
    val readStyle = SpanStyle(color = colors.ink)
    val currentStyle = SpanStyle(color = colors.accent, background = colors.highlight)
    val upcomingStyle = SpanStyle(color = colors.ink.copy(alpha = 0.38f))
    val text = buildAnnotatedString {
        tokens.forEachIndexed { index, token ->
            starts[index] = length
            val number = if (token.isMarker) token.text.drop(1) else null
            val spoken = number?.let(markers::get)
            if (number != null && spoken != null) {
                val id = AYAH_ID_PREFIX + number
                val start = length
                appendInlineContent(id, spoken)
                inlineContent[id]?.let { placeholders += AnnotatedString.Range(it.placeholder, start, length) }
                return@forEachIndexed
            }
            val value = when {
                justification?.breaks?.contains(index) == true -> "\n"
                // A no-break space glues every ayah marker to the word it closes.
                isGlue(tokens, index) -> NO_BREAK_SPACE
                else -> token.text
            }
            val colorStyle = when {
                token.isMarker -> markerStyle
                current < 0 -> null
                token.text.isBlank() -> null
                index < current -> readStyle
                index == current -> currentStyle
                else -> upcomingStyle
            }
            val stretch = justification?.stretch?.get(index)
            val spanStyle = if (stretch != null) (colorStyle ?: SpanStyle()).copy(letterSpacing = stretch) else colorStyle
            if (spanStyle == null) append(value) else withStyle(spanStyle) { append(value) }
        }
    }
    return ReaderString(text, starts, placeholders)
}

private fun isGlue(tokens: List<Token>, index: Int): Boolean {
    val token = tokens[index]
    return token.text.isBlank() && '\n' !in token.text && tokens.getOrNull(index + 1)?.isMarker == true
}

/**
 * Computes a full justification from the natural (start-aligned) layout: every line but the last
 * of a paragraph ends with a hard break at its natural break point, and its plain spaces share
 * the remaining width. Glue spaces before ayah markers keep their natural width.
 */
private fun justify(
    tokens: List<Token>,
    natural: ReaderString,
    layout: TextLayoutResult,
    width: Int,
    density: Density,
): Justification {
    val lastLine = layout.lineCount - 1
    if (lastLine <= 0) return Justification(emptySet(), emptyMap())
    val breaks = mutableSetOf<Int>()
    val spacesByLine = HashMap<Int, MutableList<Int>>()
    tokens.forEachIndexed { index, token ->
        if (!token.text.isBlank() || '\n' in token.text || isGlue(tokens, index)) return@forEachIndexed
        val start = natural.tokenStarts[index]
        val line = layout.getLineForOffset(start)
        if (line >= lastLine) return@forEachIndexed
        if (start + token.text.length >= layout.getLineEnd(line)) breaks += index
        else spacesByLine.getOrPut(line) { mutableListOf() } += index
    }
    val stretch = HashMap<Int, TextUnit>()
    spacesByLine.forEach { (line, spaces) ->
        // Lines closing a paragraph stay natural, like in any justified text.
        val endsParagraph = natural.text.text.getOrNull(layout.getLineEnd(line) - 1) == '\n'
        val slack = width - (layout.getLineRight(line) - layout.getLineLeft(line)) - JUSTIFY_SAFETY_PX
        if (endsParagraph || slack <= 0f) return@forEach
        val extra = with(density) { (slack / spaces.size).toSp() }
        spaces.forEach { stretch[it] = extra }
    }
    return Justification(breaks, stretch)
}

/**
 * Accessible text of an ayah marker: the number spelled out in Arabic ("تسعة"), falling back to
 * "آية". Letters rather than digits keep the marker inside the surrounding right-to-left run.
 */
private fun spokenAyahNumber(number: String): String = runCatching {
    MessageFormat("{0,spellout}", ULocale("ar"))
        .format(arrayOf<Any>(ArabicText.toLatinDigits(number).toLong()))
}.getOrNull()?.takeIf { text -> text.isNotBlank() && text.none { it.isDigit() } } ?: "آية"

private fun ayahMarker(number: String, widthEm: Float, colors: ElementColors, numberSize: TextUnit) =
    InlineTextContent(
        placeholder = Placeholder(
            width = widthEm.em,
            height = 1.4.em,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        ),
    ) { AyahMarker(number, colors, numberSize) }
