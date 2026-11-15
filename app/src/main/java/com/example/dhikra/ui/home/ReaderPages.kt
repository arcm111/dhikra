package com.example.dhikra.ui.home

import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection

/**
 * One screen of the full-screen reader: one step of the section. A group step reads its texts side
 * by side on that one screen, swiped sideways.
 */
data class ReaderPage(val step: ProgramItem) {
    val id: String get() = step.id

    /** What is read on this screen, in order: the group's texts, or the step itself. */
    val texts: List<ProgramItem>
        get() = if (step.isGroup && step.members.isNotEmpty()) step.members else listOf(step)
}

/**
 * The screens the reader swipes through for [section], in reading order: one per step, except the
 * headings and messages.
 */
fun readerPages(section: ProgramSection): List<ReaderPage> = section.items
    // Headings and messages are only read on the section's page, not in the reader.
    .filter { it.type != ItemType.HEADING && it.type != ItemType.NOTICE }
    .map(::ReaderPage)

/** The screen of the step [id] (or of the step one of whose texts is [id]); the first one if none. */
fun List<ReaderPage>.pageToOpen(id: String): Int =
    indexOfFirst { page -> page.id == id || page.texts.any { it.id == id } }.coerceAtLeast(0)

/**
 * The text to show first on this screen: the text [id] when it is one of them, otherwise the first
 * one not done yet, so the user picks up where they stopped (the first one when all are done).
 */
fun ReaderPage.textToOpen(id: String, isComplete: (ProgramItem) -> Boolean): Int =
    texts.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        ?: texts.indexOfFirst { !isComplete(it) }.takeIf { it >= 0 }
        ?: 0

/** Whether the reader may move on from this screen to the next step: its step is done. */
fun ReaderPage.canMoveOn(isComplete: (ProgramItem) -> Boolean): Boolean = isComplete(step)

/**
 * How many screens the reader can reach from the screen [from]: every screen before it, and the
 * following ones up to the first that is not done yet, which must be done (with its button or its
 * count) before swiping on.
 */
fun List<ReaderPage>.reachableCount(from: Int, isComplete: (ProgramItem) -> Boolean): Int {
    if (isEmpty()) return 0
    val start = from.coerceIn(indices)
    val stop = (start..lastIndex).firstOrNull { !this[it].canMoveOn(isComplete) } ?: lastIndex
    return stop + 1
}

/**
 * The text of a group to go on to from the text [from] of [count]: the next one round the group
 * that is not done yet, preferably one not read yet either; null when every other text is done.
 */
fun nextText(from: Int, count: Int, isDone: (Int) -> Boolean, isRead: (Int) -> Boolean): Int? {
    val others = (1 until count).map { (from + it) % count }
    return others.firstOrNull { !isDone(it) && !isRead(it) } ?: others.firstOrNull { !isDone(it) }
}

/** A vertical span of a text's layout (a line, or a block under the text), in pixels from its top. */
data class Span(val top: Int, val bottom: Int)

/**
 * Cuts content into pages of [height]: whole lines (and blocks) [spans], top to bottom, as many as
 * fit on each page. The first page starts at the content's top; a span taller than a page gets a
 * page of its own. Content that fits is one page.
 */
fun cutIntoPages(spans: List<Span>, height: Int): List<Span> {
    if (spans.isEmpty() || height <= 0) return listOf(Span(0, spans.maxOfOrNull { it.bottom } ?: 0))
    val pages = mutableListOf<Span>()
    var first = 0
    while (first < spans.size) {
        val top = if (pages.isEmpty()) 0 else spans[first].top
        var end = first
        while (end < spans.size && spans[end].bottom - top <= height) end++
        if (end == first) end = first + 1
        pages += Span(top, spans[end - 1].bottom)
        first = end
    }
    return pages
}
