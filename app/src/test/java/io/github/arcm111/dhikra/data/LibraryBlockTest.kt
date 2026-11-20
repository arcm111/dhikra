package io.github.arcm111.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Library subsections: read by sections with one step, unfolded when shown, kept in the JSON. */
class LibraryBlockTest {

    private val istighfar = LibraryEntry(id = "istighfar", type = ItemType.DHIKR, title = "أستغفر الله", text = "أستغفر الله")
    private val block = LibraryBlock(
        id = "after_prayer",
        title = "أوراد بعد الصلاة",
        items = listOf(
            ProgramItem(id = "s1", type = ItemType.DHIKR, entryId = "istighfar", count = 3),
            ProgramItem(id = "s2", type = ItemType.TASK, text = "صلِّ ركعتين"),
        ),
    )
    private val library = Library(listOf(istighfar), blocks = listOf(block))

    private fun section(id: String) = ProgramSection(
        id = id,
        title = id,
        items = listOf(
            ProgramItem(id = "$id-task", type = ItemType.TASK, text = "الصلاة"),
            ProgramItem(id = "$id-block", type = ItemType.HEADING, blockId = "after_prayer"),
        ),
    )

    private val program = Program(name = "p", sections = listOf(section("fajr"), section("asr")))

    @Test
    fun aSubsectionStep_unfoldsIntoItsTitleAndSteps() {
        val items = program.resolvedWith(library).sections.first().items
        assertEquals(listOf("fajr-task", "fajr-block", "fajr-block/s1", "fajr-block/s2"), items.map { it.id })
        assertEquals(ItemType.HEADING, items[1].type)
        assertEquals("أوراد بعد الصلاة", items[1].title)
        assertEquals("أستغفر الله", items[2].text)
        assertEquals(3, items[2].count)
    }

    @Test
    fun eachSection_tracksTheSubsectionsStepsOnItsOwn() {
        val (fajr, asr) = program.resolvedWith(library).sections
        val fajrIds = fajr.trackableItems.map { it.id }.toSet()
        val asrIds = asr.trackableItems.map { it.id }.toSet()
        assertTrue(fajrIds.none { it in asrIds })
        // Today's progress is reset by the section step's id, before the first slash.
        assertTrue(fajr.trackableItems.filter { it.id.contains('/') }.all { it.id.substringBefore('/') == "fajr-block" })
    }

    @Test
    fun aSubsectionGone_readsNothing() {
        val items = program.resolvedWith(library.removeBlock("after_prayer")).sections.first().items
        assertEquals(listOf("fajr-task"), items.map { it.id })
    }

    @Test
    fun subsections_areKeptInTheStore() {
        val store = ProgramStore(listOf(program), program.id, library)
        val back = ProgramJson.decodeStore(ProgramJson.encodeStore(store))
        assertEquals(listOf(block), back.library.blocks)
        assertEquals("after_prayer", back.programs.first().sections.first().items[1].blockId)
    }

    @Test
    fun aTasksInstructions_areKeptInTheStore() {
        val task = ProgramItem(id = "t", type = ItemType.TASK, text = "صلاة الضحى", instructions = listOf("ركعتان", "أو أكثر"))
        val withTask = program.copy(sections = listOf(ProgramSection(id = "x", title = "x", items = listOf(task))))
        val back = ProgramJson.decodeStore(ProgramJson.encodeStore(ProgramStore(listOf(withTask), withTask.id, library)))
        assertEquals(listOf("ركعتان", "أو أكثر"), back.programs.first().sections.first().items.first().instructions)
    }
}
