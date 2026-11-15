package com.example.dhikra.ui.home

import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Which screens the full-screen reader swipes through, which one a tap opens, and its pages. */
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
    fun readerPages_skipHeadings_andGiveEachStepOneScreen() {
        assertEquals(listOf("task", "dhikr", "group"), pages.map { it.id })
    }

    @Test
    fun aSingleStep_readsItself_aGroupItsTexts() {
        assertEquals(listOf(task), pages[0].texts)
        assertEquals(members, pages[2].texts)
    }

    @Test
    fun readerPages_ofASectionWithoutSteps_isEmpty() {
        assertTrue(readerPages(ProgramSection(id = "empty", title = "e", items = listOf(heading))).isEmpty())
    }

    @Test
    fun pageToOpen_findsTheTappedStep_orTheGroupOfTheTappedText() {
        assertEquals(0, pages.pageToOpen("task"))
        assertEquals(1, pages.pageToOpen("dhikr"))
        assertEquals(2, pages.pageToOpen("group"))
        assertEquals(2, pages.pageToOpen("group/b"))
    }

    @Test
    fun pageToOpen_ofAnUnknownStep_fallsBackToTheFirstScreen() {
        assertEquals(0, pages.pageToOpen("gone"))
    }

    @Test
    fun textToOpen_isTheTappedText() {
        assertEquals(1, pages[2].textToOpen("group/b") { true })
    }

    @Test
    fun textToOpen_ofAGroup_resumesAtTheFirstTextNotDoneYet() {
        assertEquals(1, pages[2].textToOpen("group") { it.id == "group/a" })
    }

    @Test
    fun textToOpen_ofAGroup_startsAtTheFirstTextWhenAllAreDone_orNoneIs() {
        assertEquals(0, pages[2].textToOpen("group") { true })
        assertEquals(0, pages[2].textToOpen("group") { false })
    }

    // ---- Moving on only once the step shown is done

    @Test
    fun reachable_endsAtTheFirstStepNotDoneFromTheOneShown() {
        // Nothing done: from the first screen, only it.
        assertEquals(1, pages.reachableCount(0) { false })
        // The task done: on to the dhikr, which stops the way.
        assertEquals(2, pages.reachableCount(0) { it.id == "task" })
        // Everything done: every screen.
        assertEquals(3, pages.reachableCount(0) { true })
    }

    @Test
    fun reachable_keepsEveryScreenBefore_theOneShown() {
        assertEquals(3, pages.reachableCount(2) { false })
    }

    @Test
    fun aGroup_isPassedOnceDone() {
        assertFalse(pages[2].canMoveOn { it.id != "group" })
        assertTrue(pages[2].canMoveOn { it.id == "group" })
    }

    // ---- Going on through a group's texts

    @Test
    fun nextText_isTheNextOneNotRead_roundTheGroup() {
        // From the second of four, the third is read: on to the fourth, then round to the first.
        assertEquals(3, nextText(1, 4, isDone = { false }, isRead = { it == 2 }))
        assertEquals(0, nextText(3, 4, isDone = { false }, isRead = { it == 1 || it == 2 }))
    }

    @Test
    fun nextText_whenAllOthersAreRead_isOneNotDoneYet() {
        assertEquals(2, nextText(0, 3, isDone = { it == 1 }, isRead = { true }))
    }

    @Test
    fun nextText_whenAllOthersAreDone_isNone() {
        assertNull(nextText(0, 3, isDone = { it != 0 }, isRead = { false }))
        assertNull(nextText(0, 1, isDone = { false }, isRead = { false }))
    }

    // ---- Cutting a long text into pages

    /** Lines of 40 px, one after the other. */
    private fun lines(count: Int) = List(count) { Span(it * 40, it * 40 + 40) }

    @Test
    fun aTextThatFits_isOnePage() {
        assertEquals(listOf(Span(0, 120)), cutIntoPages(lines(3), height = 200))
    }

    @Test
    fun aLongText_isCutBetweenWholeLines() {
        // 5 lines a page (200 px), 12 lines: 5, 5 and 2.
        assertEquals(
            listOf(Span(0, 200), Span(200, 400), Span(400, 480)),
            cutIntoPages(lines(12), height = 210),
        )
    }

    @Test
    fun theBlockUnderTheText_goesOnTheNextPageWhenItDoesNotFit() {
        val spans = lines(4) + Span(170, 260)
        assertEquals(listOf(Span(0, 160), Span(170, 260)), cutIntoPages(spans, height = 200))
    }

    @Test
    fun aSpanTallerThanAPage_getsAPageOfItsOwn() {
        val spans = listOf(Span(0, 40), Span(40, 400), Span(400, 440))
        assertEquals(listOf(Span(0, 40), Span(40, 400), Span(400, 440)), cutIntoPages(spans, height = 100))
    }

    @Test
    fun nothingToCut_isOneEmptyPage() {
        assertEquals(listOf(Span(0, 0)), cutIntoPages(emptyList(), height = 100))
    }
}
