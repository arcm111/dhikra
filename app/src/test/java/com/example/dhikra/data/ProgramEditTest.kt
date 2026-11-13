package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramEditTest {

    private val program = Program(
        name = "p",
        sections = listOf(
            ProgramSection(id = "a", title = "الفجر", reminderMinutes = 300, items = listOf(
                ProgramItem(id = "a1", type = ItemType.TASK, title = "x"),
                ProgramItem(id = "a2", type = ItemType.TASK, title = "y"),
            )),
            ProgramSection(id = "b", title = "الضحى"),
        ),
    )

    @Test
    fun duplicateSection_insertsACopyRightAfter_withFreshIds() {
        val copied = program.duplicateSection("a") { "$it (نسخة)" }
        assertEquals(listOf("الفجر", "الفجر (نسخة)", "الضحى"), copied.sections.map { it.title })
        val copy = copied.sections[1]
        assertNotEquals("a", copy.id)
        assertEquals(listOf("x", "y"), copy.items.map { it.title })
        assertEquals(300, copy.reminderMinutes)
        // Fresh ids: today's progress and reminders stay the original's own.
        assertEquals(copied.ids.size, copied.sections.size + copied.sections.sumOf { it.items.size })
    }

    @Test
    fun duplicateSection_ofAnUnknownSection_changesNothing() {
        assertSame(program, program.duplicateSection("zzz"))
    }

    @Test
    fun moveSection_andMoveItem_byMoreThanOnePlace() {
        val three = program.upsertSection(ProgramSection(id = "c", title = "c"))
        assertEquals(listOf("c", "a", "b"), three.moveSection("c", -2).sections.map { it.id })
        assertEquals(listOf("a2", "a1"), program.moveItem("a", "a1", 1).sections[0].items.map { it.id })
    }

    // ---- A group where only some of the texts are needed

    private fun libraryRequiring(required: Int) = Library(
        entries = (1..5).map { LibraryEntry(id = "e$it", type = ItemType.DUA, title = "دعاء $it", text = "…") },
        groups = listOf(
            LibraryGroup(id = "g", type = ItemType.DUA, title = "أدعية", items = (1..5).map { GroupItem("e$it") }, required = required),
            LibraryGroup(id = "h", type = ItemType.DUA, title = "أخرى", items = listOf(GroupItem("e1"))),
        ),
    )

    private val library = libraryRequiring(2)

    /** A step reading the group: it takes the group's minimum when resolved. */
    private fun groupStep(required: Int) =
        ProgramItem(id = "step", type = ItemType.DUA, groupId = "g").resolvedWith(libraryRequiring(required))!!

    @Test
    fun aChoiceGroup_needsOnlyItsNumberOfTexts() {
        val choice = groupStep(required = 2)
        assertEquals(true, choice.isChoice)
        assertEquals(2, choice.target)
        val all = groupStep(required = 0)
        assertEquals(false, all.isChoice)
        assertEquals(5, all.target)
        // Asking for all of them (or more) is the same as a plain group.
        assertEquals(5, groupStep(required = 5).target)
    }

    @Test
    fun aChoiceGroup_isDoneOnceThatManyTextsAreRead() {
        val choice = groupStep(required = 2)
        val one = com.example.dhikra.ui.UiState(progress = mapOf("step/e4" to 1))
        val two = com.example.dhikra.ui.UiState(progress = mapOf("step/e4" to 1, "step/e1" to 1, "step/e5" to 1))
        assertEquals(1, one.doneCount(choice))
        assertEquals(false, one.isComplete(choice))
        assertEquals(2, two.doneCount(choice)) // never more than needed
        assertEquals(true, two.isComplete(choice))
    }

    // ---- A section whose time follows a prayer

    private val makkah = MockPrayerTimes.timetable("makkah", "2026-11-06")!! // Fajr 05:12

    @Test
    fun aSectionTiedToAPrayer_takesThatDaysTime() {
        val section = ProgramSection(id = "s", title = "بعد الفجر", reminderMinutes = 300, anchor = SectionAnchor(Prayer.FAJR, 15))
        assertEquals(5 * 60 + 27, section.withPrayerTimes(makkah).reminderMinutes)
        // Without prayer times it keeps the time it last had.
        assertEquals(300, section.withPrayerTimes(null).reminderMinutes)
        // A fixed time does not move.
        val fixed = ProgramSection(id = "f", title = "x", reminderMinutes = 600)
        assertSame(fixed, fixed.withPrayerTimes(makkah))
        // Before midnight wraps to the evening before.
        val early = section.copy(anchor = SectionAnchor(Prayer.FAJR, -6 * 60))
        assertEquals(Math.floorMod(5 * 60 + 12 - 6 * 60, 24 * 60), early.withPrayerTimes(makkah).reminderMinutes)
    }

    @Test
    fun anchorsAndChoices_roundTripThroughJson() {
        val program = Program(
            name = "p",
            id = "p",
            sections = listOf(
                ProgramSection(
                    id = "s", title = "بعد الفجر", reminderMinutes = 327, anchor = SectionAnchor(Prayer.FAJR, 15),
                    items = listOf(ProgramItem(id = "step", type = ItemType.DUA, groupId = "g")),
                ),
                ProgramSection(id = "t", title = "الضحى", reminderMinutes = 540),
            ),
        )
        val decoded = ProgramJson.decode(ProgramJson.encode(ProgramBundle(program, library))).program
        assertEquals(SectionAnchor(Prayer.FAJR, 15), decoded.sections[0].anchor)
        assertEquals(null, decoded.sections[1].anchor)
        // The minimum is the library group's, and the steps reading it take it.
        val bundle = ProgramJson.decode(ProgramJson.encode(ProgramBundle(program, library)))
        assertEquals(2, bundle.library.group("g")!!.required)
        assertEquals(0, bundle.library.group("h")!!.required)
        assertEquals(2, bundle.program.resolvedWith(bundle.library).sections[0].items[0].target)
    }

    @Test
    fun duplicateGroup_placesAFreshCopyRightAfterTheOriginal() {
        val (copied, copy) = library.duplicateGroup("g") { "$it (نسخة)" }!!
        assertEquals(listOf("أدعية", "أدعية (نسخة)", "أخرى"), copied.groups.map { it.title })
        assertNotEquals("g", copy.id)
        assertEquals(library.group("g")!!.items, copy.items)
        assertEquals(2, copy.required)
        assertEquals(null, library.duplicateGroup("zzz") { it })
    }

    @Test
    fun moveItemToSection_takesTheSameStepToTheEndOfTheOther() {
        val moved = program.moveItemToSection("a", "a1", "b")
        assertEquals(listOf("a2"), moved.sections[0].items.map { it.id })
        // The same step and id: today's progress on it goes along.
        assertEquals(listOf("a1"), moved.sections[1].items.map { it.id })
        assertSame(program, program.moveItemToSection("a", "a1", "a"))
        assertSame(program, program.moveItemToSection("a", "gone", "b"))
        assertSame(program, program.moveItemToSection("a", "a1", "nowhere"))
    }

    @Test
    fun updateItem_changesOnlyThatStep() {
        val changed = program.updateItem("a", "a2") { it.copy(title = "z") }
        assertEquals(listOf("x", "z"), changed.sections[0].items.map { it.title })
    }

    @Test
    fun sectionNotices_becomeMessageSteps_inOrder_andBlankOnesAreDropped() {
        val notices = listOf(
            SectionNotice(NoticeKind.IMPORTANT, "لا تؤخر الصلاة"),
            SectionNotice(NoticeKind.INFO, "  "),
            SectionNotice(NoticeKind.INFO, "تقرأ بعد كل صلاة"),
            SectionNotice(NoticeKind.WARNING, "انتبه للوقت"),
        )
        val program = Program(name = "p", id = "p", sections = listOf(ProgramSection(id = "s", title = "s", notices = notices)))
        val decoded = ProgramJson.decode(ProgramJson.encode(ProgramBundle(program, library))).program
        // Read back, the section's messages are message steps at its top, the blank one left out.
        assertEquals(emptyList<SectionNotice>(), decoded.sections[0].notices)
        assertEquals(notices.filter { it.text.isNotBlank() }, decoded.sections[0].items.map { it.asNotice })
        assertTrue(decoded.sections[0].items.all { it.type == ItemType.NOTICE })
        // A section without messages has none.
        val plain = ProgramJson.decode(ProgramJson.encode(ProgramBundle(this.program, library))).program
        assertTrue(plain.sections[0].items.none { it.type == ItemType.NOTICE })
    }
}
