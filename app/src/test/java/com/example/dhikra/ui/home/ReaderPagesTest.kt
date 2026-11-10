package com.example.dhikra.ui.home

import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which screens the full-screen reader swipes through, and which one a tap opens. */
class ReaderPagesTest {

    private val heading = ProgramItem(id = "heading", type = ItemType.HEADING, title = "عنوان")
    private val task = ProgramItem(id = "task", type = ItemType.TASK, title = "صلاة")
    private val dhikr = ProgramItem(id = "dhikr", type = ItemType.DHIKR, text = "سبحان الله", count = 33)
    private val members = listOf(
        ProgramItem(id = "group/a", type = ItemType.DUA, text = "أ"),
        ProgramItem(id = "group/b", type = ItemType.DUA, text = "ب", count = 3),
        ProgramItem(id = "group/c", type = ItemType.DUA, text = "ج"),
    )
    private val group = ProgramItem(id = "group", type = ItemType.DUA, groupId = "g", members = members)
    private val section = ProgramSection(id = "s", title = "s", items = listOf(heading, task, dhikr, group))

    private val pages = readerPages(section)

    @Test
    fun readerPages_skipHeadings_andUnfoldGroupsIntoTheirTexts() {
        assertEquals(listOf("task", "dhikr", "group/a", "group/b", "group/c"), pages.map { it.id })
    }

    @Test
    fun readerPages_ofSingleSteps_areTheStepItself() {
        assertTrue(pages.take(2).all { it.item == it.step && !it.inGroup })
    }

    @Test
    fun readerPages_ofGroupTexts_knowTheirGroupAndPosition() {
        val inGroup = pages.drop(2)
        assertTrue(inGroup.all { it.inGroup && it.step == group })
        assertEquals(listOf(0, 1, 2), inGroup.map { it.indexInGroup })
        assertFalse(pages.first().inGroup)
    }

    @Test
    fun readerPages_ofASectionWithoutSteps_isEmpty() {
        assertTrue(readerPages(ProgramSection(id = "empty", title = "e", items = listOf(heading))).isEmpty())
    }

    @Test
    fun pageToOpen_findsTheTappedSingleStep() {
        assertEquals(0, pages.pageToOpen("task") { false })
        assertEquals(1, pages.pageToOpen("dhikr") { true })
    }

    @Test
    fun pageToOpen_opensTheExactTextWhenGivenItsId() {
        assertEquals(3, pages.pageToOpen("group/b") { true })
    }

    @Test
    fun pageToOpen_ofAGroup_resumesAtTheFirstTextNotDoneYet() {
        val done = setOf("group/a")
        assertEquals(3, pages.pageToOpen("group") { it.id in done })
    }

    @Test
    fun pageToOpen_ofAGroup_startsAtTheFirstTextWhenAllAreDone_orNoneIs() {
        assertEquals(2, pages.pageToOpen("group") { true })
        assertEquals(2, pages.pageToOpen("group") { false })
    }

    @Test
    fun pageToOpen_ofAnUnknownStep_fallsBackToTheFirstScreen() {
        assertEquals(0, pages.pageToOpen("gone") { false })
    }

    // ---- Moving on only once the screen shown is done

    @Test
    fun reachable_endsAtTheFirstScreenNotDoneFromTheOneShown() {
        // Nothing done: from the first screen, only it.
        assertEquals(1, pages.reachableCount(0) { false })
        // The task done: on to the dhikr, which stops the way.
        assertEquals(2, pages.reachableCount(0) { it.id == "task" })
        // Everything done: every screen.
        assertEquals(5, pages.reachableCount(0) { true })
    }

    @Test
    fun reachable_keepsEveryScreenBefore_theOneShown() {
        // Opened on the group's second text with nothing done: back is open, forward is not.
        assertEquals(4, pages.reachableCount(3) { false })
    }

    @Test
    fun aGroupDone_letsItsTextsBePassed() {
        assertEquals(5, pages.reachableCount(2) { it.id == "group" })
    }

    @Test
    fun aChoiceGroup_letsTextsBeSkipped_butNotItsLastWhileNotDone() {
        val choice = group.copy(choose = 1)
        val pages = readerPages(section.copy(items = listOf(task, choice)))
        // The task done: the group's first two texts may be passed by, its last one must be done.
        assertEquals(4, pages.reachableCount(0) { it.id == "task" })
        assertTrue(pages[1].canMoveOn { false })
        assertFalse(pages[3].canMoveOn { false })
        assertTrue(pages[3].canMoveOn { it.id == "group" })
    }
}
