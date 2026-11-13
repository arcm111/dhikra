package com.example.dhikra.ui.home

import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection

/**
 * One screen of the full-screen reader: a single step, or one text of a group step.
 *
 * [item] is what is read and tracked on this screen. [step] is the step of the schedule it belongs
 * to: the same item for a single text, the group for one of the group's texts.
 */
data class ReaderPage(val item: ProgramItem, val step: ProgramItem, val indexInGroup: Int = -1) {
    val id: String get() = item.id
    val inGroup: Boolean get() = indexInGroup >= 0
}

/**
 * The screens the reader swipes through for [section], in reading order: every step except the
 * headings, with a group step unfolded into one screen per text.
 */
fun readerPages(section: ProgramSection): List<ReaderPage> = section.items.flatMap { step ->
    when {
        // Headings and messages are only read on the section's page, not in the reader.
        step.type == ItemType.HEADING || step.type == ItemType.NOTICE -> emptyList()
        step.isGroup -> step.members.mapIndexed { index, member -> ReaderPage(member, step, index) }
        else -> listOf(ReaderPage(step, step))
    }
}

/**
 * The screen to open when the user taps the step [stepId] (or a text of it). A screen with exactly
 * that id wins; for a group step it is the first text not done yet, so the user picks up where
 * they stopped, or the first text when all are done. Falls back to the first screen.
 */
fun List<ReaderPage>.pageToOpen(stepId: String, isComplete: (ProgramItem) -> Boolean): Int {
    indexOfFirst { it.id == stepId }.takeIf { it >= 0 }?.let { return it }
    val ofStep = withIndex().filter { it.value.step.id == stepId }
    return (ofStep.firstOrNull { !isComplete(it.value.item) } ?: ofStep.firstOrNull())?.index ?: 0
}

/**
 * Whether the reader may move on from this screen: its text is done, or its whole group is. A text
 * of a choice group (e.g. any 2 of 5) may be passed by, except the group's last one while the group
 * is not done yet.
 */
fun ReaderPage.canMoveOn(isComplete: (ProgramItem) -> Boolean): Boolean = when {
    isComplete(item) -> true
    inGroup && isComplete(step) -> true
    inGroup && step.isChoice -> indexInGroup < step.members.lastIndex
    else -> false
}

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
