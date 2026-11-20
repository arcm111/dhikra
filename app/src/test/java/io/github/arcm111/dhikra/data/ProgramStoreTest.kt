package io.github.arcm111.dhikra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** The user's several programs, sharing one library. */
class ProgramStoreTest {

    private val library = Library(
        entries = listOf(
            LibraryEntry(id = "dua", type = ItemType.DUA, title = "دعاء", text = "اللهم"),
            LibraryEntry(id = "dhikr", type = ItemType.DHIKR, title = "ذكر", text = "سبحان الله"),
        ),
        groups = listOf(
            LibraryGroup(id = "g", type = ItemType.DUA, title = "مجموعة", items = listOf(GroupItem("dua"))),
        ),
    )

    private val morning = Program(
        id = "morning",
        name = "الصباح",
        sections = listOf(
            ProgramSection(
                id = "s1",
                title = "بعد الفجر",
                items = listOf(
                    ProgramItem(id = "a", type = ItemType.DUA, entryId = "dua"),
                    ProgramItem(id = "b", type = ItemType.DUA, groupId = "g"),
                ),
            ),
        ),
    )

    private val evening = Program(
        id = "evening",
        name = "المساء",
        sections = listOf(
            ProgramSection(
                id = "s2",
                title = "بعد المغرب",
                items = listOf(
                    ProgramItem(id = "c", type = ItemType.DHIKR, entryId = "dhikr", count = 33),
                    ProgramItem(id = "d", type = ItemType.DUA, entryId = "dua"),
                ),
            ),
        ),
    )

    private val store = ProgramStore(listOf(morning, evening), activeId = "evening", library = library)

    @Test
    fun store_roundTripsThroughJson() {
        assertEquals(store, ProgramJson.decodeStore(ProgramJson.encodeStore(store)))
    }

    @Test
    fun defaultStore_roundTripsThroughJson() {
        val defaults = DefaultProgram.store()
        val stored = defaults.copy(programs = defaults.programs + Program(name = "فارغ"))
        assertEquals(stored, ProgramJson.decodeStore(ProgramJson.encodeStore(stored)))
    }

    @Test
    fun decodeStore_keepsSectionAndStepIdsUniqueAcrossPrograms() {
        // A second program holding the same ids, as two imports of one file would.
        val clash = evening.copy(id = "copy", sections = morning.sections)
        val json = ProgramJson.encodeStore(store.copy(programs = listOf(morning, clash)))
        val decoded = ProgramJson.decodeStore(json)
        val first = decoded.programs[0].ids
        val second = decoded.programs[1].ids
        assertEquals(morning.ids, first)
        assertTrue(first.intersect(second).isEmpty())
        assertEquals(first.size, second.size)
    }

    @Test(expected = ProgramFormatException::class)
    fun decodeStore_rejectsAProgramFile() {
        ProgramJson.decodeStore(ProgramJson.encode(store.bundle))
    }

    @Test
    fun encodeStore_isNotAProgramFile() {
        val root = JSONObject(ProgramJson.encodeStore(store))
        assertEquals("evening", root.getString("active"))
        assertFalse(root.has("sections"))
    }

    @Test
    fun active_isTheChosenProgram_orTheFirstOne() {
        assertSame(evening, store.active)
        assertSame(morning, store.copy(activeId = "gone").active)
        assertEquals(ProgramBundle(evening, library), store.bundle)
    }

    @Test
    fun withBundle_editsTheActiveProgramAndTheLibrary_keepingItsId() {
        val renamed = evening.copy(name = "مساءً", id = "other")
        val library = library.remove("dhikr")
        val edited = store.withBundle(ProgramBundle(renamed, library))
        assertEquals(listOf("morning", "evening"), edited.programs.map { it.id })
        assertEquals("مساءً", edited.active.name)
        assertEquals(morning, edited.programs[0])
        assertEquals(library, edited.library)
    }

    @Test
    fun add_appendsAndSwitches_select_switches() {
        val added = store.add(Program(id = "new", name = "جديد"))
        assertEquals(listOf("morning", "evening", "new"), added.programs.map { it.id })
        assertEquals("new", added.active.id)
        assertEquals("morning", added.select("morning").active.id)
        assertEquals(added, added.select("gone"))
    }

    @Test
    fun remove_switchesToTheFirstRemainingProgram_butKeepsTheLastOne() {
        val removed = store.remove("evening")
        assertEquals(listOf(morning), removed.programs)
        assertEquals("morning", removed.active.id)
        assertEquals("evening", store.remove("morning").active.id)
        assertEquals(removed, removed.remove("morning"))
    }

    @Test
    fun withoutEntry_removesItsStepsFromEveryProgram() {
        val edited = store.withoutEntry("dua")
        assertEquals(listOf("b"), edited.programs[0].sections[0].items.map { it.id })
        assertEquals(listOf("c"), edited.programs[1].sections[0].items.map { it.id })
        assertTrue(edited.library.group("g")!!.items.isEmpty())
    }

    @Test
    fun withoutGroup_removesItsStepsFromEveryProgram() {
        val edited = store.withoutGroup("g")
        assertEquals(listOf("a"), edited.programs[0].sections[0].items.map { it.id })
        assertEquals(evening, edited.programs[1])
    }

    @Test
    fun usages_countAcrossPrograms() {
        assertEquals(2, store.programs.usages("dua"))
        assertEquals(1, store.programs.groupUsages("g"))
        assertTrue(store.programs.reads("dhikr", library))
        assertFalse(listOf(morning).reads("dhikr", library))
    }

    @Test
    fun withIdsOutside_renamesOnlyTheTakenIds() {
        val fresh = evening.withIdsOutside(setOf("s2", "c", "x"))
        val section = fresh.sections.single()
        assertNotEquals("s2", section.id)
        assertNotEquals("c", section.items[0].id)
        assertEquals("d", section.items[1].id)
        // Only the ids change.
        val withoutIds = { p: Program -> p.sections.map { s -> s.copy(id = "", items = s.items.map { it.copy(id = "") }) } }
        assertEquals(withoutIds(evening), withoutIds(fresh))
        assertTrue(fresh.ids.none { it in setOf("s2", "c", "x") })
    }

    @Test
    fun withIdsOutside_alsoSeparatesIdsRepeatedWithinTheProgram() {
        val twice = morning.copy(sections = morning.sections + morning.sections)
        val fresh = twice.withIdsOutside(emptySet())
        assertEquals(6, fresh.ids.size)
        assertEquals(morning.sections[0], fresh.sections[0])
    }
}
