package io.github.arcm111.dhikra.data

import io.github.arcm111.dhikra.ui.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Groups of library texts: resolution for display, editing, progress and the file format. */
class GroupTest {

    private val first = LibraryEntry(id = "first", type = ItemType.DUA, title = "الأول", text = "نص أول")
    private val second = LibraryEntry(id = "second", type = ItemType.DUA, title = "الثاني", text = "نص ثانٍ")
    private val group = LibraryGroup(
        id = "group",
        type = ItemType.DUA,
        title = "أدعية",
        description = "وصف",
        items = listOf(GroupItem("first"), GroupItem("second", count = 3)),
    )
    private val library = Library(listOf(first, second), listOf(group))
    private val step = ProgramItem(id = "step", type = ItemType.DUA, groupId = "group", note = "ملاحظة")
    private val program = Program(name = "p", sections = listOf(ProgramSection(id = "s", title = "s", items = listOf(step))))

    @Test
    fun group_resolvesItsTextsInOrder_eachWithItsCountAndOwnId() {
        val resolved = checkNotNull(step.resolvedWith(library))
        assertEquals("أدعية", resolved.title)
        assertEquals("وصف", resolved.source)
        assertEquals("ملاحظة", resolved.note)
        assertEquals(listOf("step/first", "step/second"), resolved.members.map { it.id })
        assertEquals(listOf("نص أول", "نص ثانٍ"), resolved.members.map { it.text })
        assertEquals(listOf(1, 3), resolved.members.map { it.target })
        assertEquals(2, resolved.target)
        assertFalse(resolved.isRepeatable)
    }

    @Test
    fun group_withoutAnyTextLeft_isNotShown() {
        assertNull(step.resolvedWith(Library(groups = listOf(group))))
        assertNull(step.resolvedWith(Library(listOf(first, second))))
    }

    @Test
    fun removingAnEntry_takesItOutOfItsGroups() {
        val edited = library.remove("first")
        assertEquals(listOf("second"), edited.group("group")?.items?.map { it.entryId })
        assertEquals(listOf("step/second"), step.resolvedWith(edited)?.members?.map { it.id })
    }

    @Test
    fun deletingAGroup_removesTheStepsReadingIt_notItsTexts() {
        assertEquals(1, program.groupUsages("group"))
        assertTrue(program.reads("second", library))
        assertEquals(0, program.withoutGroup("group").groupUsages("group"))
        assertEquals(2, library.removeGroup("group").entries.size)
    }

    @Test
    fun aGroup_isDoneOnceEachOfItsTextsIs() {
        val shown = checkNotNull(step.resolvedWith(library))
        val state = UiState(progress = mapOf("step/first" to 1, "step/second" to 2))
        assertEquals(1, state.doneCount(shown))
        assertFalse(state.isComplete(shown))
        val done = state.copy(progress = state.progress + ("step/second" to 3))
        assertEquals(2, done.doneCount(shown))
        assertTrue(done.isComplete(shown))
    }

    @Test
    fun json_roundTripsGroupsAndGroupSteps() {
        val bundle = ProgramBundle(program, library)
        assertEquals(bundle, ProgramJson.decode(ProgramJson.encode(bundle)).withProgramId(program.id))
    }

    @Test
    fun decode_keepsGroupsConsistent() {
        val json = """
            {"version":4,"library":[
              {"id":"a","type":"dhikr","title":"أ","text":"سبحان الله"}
            ],"groups":[
              {"id":"g","type":"dhikr","title":"تسبيح","items":[
                {"entry":"a","count":33},{"entry":"a"},{"entry":"missing"}
              ]},
              {"id":"untitled","type":"dhikr","items":[{"entry":"a"}]}
            ],"sections":[{"title":"s","items":[{"type":"dua","group":"g"}]}]}
        """.trimIndent()
        val (program, library) = ProgramJson.decode(json)
        // Unknown texts and repeats are dropped, groups without a title too.
        assertEquals(listOf(GroupItem("a", 33)), library.groups.single().items)
        // A group step belongs to its group's collection.
        assertEquals(ItemType.DHIKR, program.sections.single().items.single().type)
        assertFalse(ProgramJson.isOutdated(json))
    }

    @Test
    fun decode_version2Files_haveNoGroups() {
        val json = """{"version":2,"library":[],"sections":[{"title":"s","items":[]}]}"""
        assertTrue(ProgramJson.isOutdated(json))
        assertTrue(ProgramJson.decode(json).library.groups.isEmpty())
    }
}
