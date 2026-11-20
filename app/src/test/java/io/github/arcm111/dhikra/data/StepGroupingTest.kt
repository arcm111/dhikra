package io.github.arcm111.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Taking a text out of a group as a step of its own, and grouping steps into a new group. */
class StepGroupingTest {

    private val a = LibraryEntry(id = "a", type = ItemType.DUA, title = "أ", text = "أ")
    private val b = LibraryEntry(id = "b", type = ItemType.DUA, title = "ب", text = "ب")
    private val c = LibraryEntry(id = "c", type = ItemType.DUA, title = "ج", text = "ج")
    private val z = LibraryEntry(id = "z", type = ItemType.DHIKR, title = "ذ", text = "ذ")
    private val group = LibraryGroup(
        id = "g", type = ItemType.DUA, title = "مجموعة",
        items = listOf(GroupItem("a"), GroupItem("b", count = 3), GroupItem("c")), required = 2,
    )
    private val library = Library(listOf(a, b, c, z), listOf(group))

    private val task = ProgramItem(id = "task", type = ItemType.TASK, text = "صلاة")
    private val groupStep = ProgramItem(id = "step", type = ItemType.DUA, groupId = "g", note = "ملاحظة")
    private val after = ProgramItem(id = "after", type = ItemType.DHIKR, entryId = "z")
    private val steps = listOf(task, groupStep, after)

    @Test
    fun aTextMovedOutAbove_isAStepRightBeforeTheGroup_withItsCount() {
        val result = checkNotNull(steps.moveOutOfGroup("step", "b", below = false, library))
        assertEquals(listOf("task", null, "step", "after"), result.items.map { it.id.takeIf { id -> id in setOf("task", "step", "after") } })
        val moved = result.items[1]
        assertEquals("b", moved.entryId)
        assertEquals(3, moved.count)
        assertEquals(ItemType.DUA, moved.type)
        assertEquals(listOf("a", "c"), result.group?.items?.map { it.entryId })
    }

    @Test
    fun aTextMovedOutBelow_isAStepRightAfterTheGroup() {
        val result = checkNotNull(steps.moveOutOfGroup("step", "a", below = true, library))
        assertEquals("step", result.items[1].id)
        assertEquals("a", result.items[2].entryId)
        assertEquals("after", result.items[3].id)
    }

    @Test
    fun aChoiceOfAsManyAsAreLeft_becomesAllOfThem() {
        val result = checkNotNull(steps.moveOutOfGroup("step", "a", below = true, library))
        assertEquals(0, result.group?.required)
    }

    @Test
    fun theLastTextMovedOut_takesTheGroupStepsPlace() {
        val one = library.copy(groups = listOf(group.copy(items = listOf(GroupItem("a")), required = 0)))
        val result = checkNotNull(steps.moveOutOfGroup("step", "a", below = false, one))
        assertEquals(3, result.items.size)
        assertEquals("a", result.items[1].entryId)
        assertEquals("ملاحظة", result.items[1].note)
        assertNull(result.group)
    }

    @Test
    fun movingOutWhatIsNotThere_doesNothing() {
        assertNull(steps.moveOutOfGroup("task", "a", below = false, library))
        assertNull(steps.moveOutOfGroup("step", "z", below = false, library))
    }

    // ---- Grouping

    private val s1 = ProgramItem(id = "s1", type = ItemType.DUA, entryId = "a", note = "أولى")
    private val s2 = ProgramItem(id = "s2", type = ItemType.DUA, entryId = "b", count = 3)
    private val s3 = ProgramItem(id = "s3", type = ItemType.DUA, entryId = "c", note = "ثالثة")
    private val singles = listOf(task, s1, after, s2, s3)

    @Test
    fun groupedSteps_becomeOneGroupStep_inTheFirstOnesPlace() {
        val result = checkNotNull(singles.groupSteps(setOf("s3", "s1", "s2"), "  أدعية  "))
        val group = checkNotNull(result.group)
        assertEquals("أدعية", group.title)
        assertEquals(ItemType.DUA, group.type)
        assertEquals(listOf(GroupItem("a"), GroupItem("b", 3), GroupItem("c")), group.items)
        assertEquals(listOf("task", null, "after"), result.items.map { it.id.takeIf { id -> id in setOf("task", "after") } })
        val step = result.items[1]
        assertEquals(group.id, step.groupId)
        assertEquals("أولى\nثالثة", step.note)
    }

    @Test
    fun grouping_needsTwoSingleTextsOfOneCollection() {
        assertNull(singles.groupSteps(setOf("s1"), "x"))
        assertNull(singles.groupSteps(setOf("s1", "after"), "x"))
        assertNull(singles.groupSteps(setOf("s1", "task"), "x"))
        assertTrue(s1.isGroupable && !task.isGroupable && !groupStep.isGroupable)
    }
}
