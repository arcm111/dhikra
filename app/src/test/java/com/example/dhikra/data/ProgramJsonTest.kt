package com.example.dhikra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramJsonTest {

    @Test
    fun defaultBundle_roundTripsThroughJson() {
        val bundle = DefaultProgram.bundle()
        val decoded = ProgramJson.decode(ProgramJson.encode(bundle))
        assertEquals(bundle, decoded)
    }

    @Test
    fun defaultProgram_referencesOnlyLibraryTextsAndGroups() {
        val bundle = DefaultProgram.bundle()
        val contentSteps = bundle.program.sections.flatMap { it.items }.filter { it.type.isContent }
        assertTrue(contentSteps.isNotEmpty())
        assertTrue(contentSteps.any { it.isGroup } && contentSteps.any { !it.isGroup })
        assertTrue(
            contentSteps.all { step ->
                if (step.isGroup) bundle.library.group(step.groupId)?.type == step.type
                else bundle.library.entry(step.entryId)?.type == step.type
            }
        )
        // Every step still resolves to a text (or a group of texts) once displayed.
        assertEquals(bundle.program.itemCount, bundle.program.resolvedWith(bundle.library).itemCount)
    }

    @Test
    fun defaultBundle_hasUniqueIdsAndUsesEveryTextAndGroup() {
        val (program, library) = DefaultProgram.bundle()
        // Sections and steps share one id space: progress is stored per step id.
        val ids = program.sections.map { it.id } + program.sections.flatMap { s -> s.items.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(library.entries.size, library.entries.map { it.id }.toSet().size)
        assertEquals(library.groups.size, library.groups.map { it.id }.toSet().size)
        assertTrue(library.entries.all { program.reads(it.id, library) })
        assertTrue(library.groups.all { program.groupUsages(it.id) > 0 })
        assertTrue(ItemType.CONTENT.all { library.ofType(it).isNotEmpty() })
        // A group lists texts of its own collection, each once.
        library.groups.forEach { group ->
            assertTrue(group.title, group.items.isNotEmpty())
            assertTrue(group.title, group.items.all { library.entry(it.entryId)?.type == group.type })
            assertEquals(group.title, group.items.size, group.items.map { it.entryId }.toSet().size)
        }
    }

    @Test
    fun encode_programStepsCarryIdsNotTexts() {
        val root = JSONObject(ProgramJson.encode(DefaultProgram.bundle()))
        val items = root.getJSONArray("sections").let { sections ->
            (0 until sections.length()).flatMap { s ->
                val array = sections.getJSONObject(s).getJSONArray("items")
                (0 until array.length()).map { array.getJSONObject(it) }
            }
        }
        val content = items.filter { ItemType.fromKey(it.getString("type")).isContent }
        assertTrue(content.all { it.has("entry") != it.has("group") && !it.has("text") && !it.has("title") })
        assertEquals(DefaultProgram.library().entries.size, root.getJSONArray("library").length())
        assertEquals(DefaultProgram.library().groups.size, root.getJSONArray("groups").length())
    }

    @Test
    fun decode_migratesVersion1TextsIntoTheLibrary() {
        val json = """
            {"name":"x","sections":[{"title":"s","items":[
              {"id":"d1","type":"dhikr","text":"سبحان الله","count":33},
              {"id":"h1","type":"hadith","text":"نص","source":"البخاري","grade":"صحيح"},
              {"type":"task","text":"صلاة الضحى"},
              {"type":"something-new","text":"t"}
            ]}]}
        """.trimIndent()
        assertTrue(ProgramJson.isOutdated(json))
        val (program, library) = ProgramJson.decode(json)
        val items = program.sections.single().items
        assertEquals(ItemType.DHIKR, items[0].type)
        assertEquals(33, items[0].count)
        assertEquals("سبحان الله", library.entry(items[0].entryId)?.text)
        assertEquals("البخاري • صحيح", library.entry(items[1].entryId)?.description)
        assertEquals("صلاة الضحى", items[2].text)
        assertEquals(ItemType.DUA, items[3].type) // unknown types fall back to dua
        assertEquals(3, library.entries.size)
        assertTrue(items.all { it.id.isNotBlank() })
        assertNull(program.sections.single().reminderMinutes)
    }

    @Test
    fun resolve_dropsStepsWhoseEntryWasDeleted() {
        val bundle = DefaultProgram.bundle()
        // Read on its own (the sessions' opening) and in the group of the three Quls.
        assertTrue(bundle.program.usages("al_ikhlas") > 0)
        assertTrue(bundle.library.groupsWith("al_ikhlas").isNotEmpty())
        val library = bundle.library.remove("al_ikhlas")
        val program = bundle.program.withoutEntry("al_ikhlas")
        assertEquals(0, program.usages("al_ikhlas"))
        assertTrue(library.groupsWith("al_ikhlas").isEmpty())
        val shown = program.resolvedWith(library).sections.flatMap { it.items }
        assertFalse(shown.flatMap { listOf(it) + it.members }.any { it.entryId == "al_ikhlas" })
        // The groups that read it keep their other texts.
        assertEquals(2, shown.first { it.groupId == "group_muawwidhat" }.members.size)
    }

    @Test
    fun decode_regeneratesDuplicateIds() {
        val json = """{"sections":[{"id":"a","title":"1","items":[{"id":"a","type":"task","text":"t"}]}]}"""
        val program = ProgramJson.decode(json).program
        assertNotEquals(program.sections[0].id, program.sections[0].items[0].id)
    }

    @Test(expected = ProgramFormatException::class)
    fun decode_rejectsInvalidJson() {
        ProgramJson.decode("not json")
    }

    @Test(expected = ProgramFormatException::class)
    fun decode_rejectsOtherFormats() {
        ProgramJson.decode("""{"format":"other","sections":[]}""")
    }

    @Test
    fun time_parsesAndFormats() {
        assertEquals(5 * 60 + 15, parseTime24("05:15"))
        assertEquals("09:00", formatTime24(9 * 60))
        assertNull(parseTime24("25:00"))
        assertNull(parseTime24(""))
    }

    @Test
    fun program_moveAndDuplicateItems() {
        val section = ProgramSection(
            id = "s", title = "s",
            items = listOf(
                ProgramItem(id = "1", type = ItemType.DUA),
                ProgramItem(id = "2", type = ItemType.DUA),
            ),
        )
        val program = Program(name = "p", sections = listOf(section))
        assertEquals(listOf("2", "1"), program.moveItem("s", "1", 1).sections[0].items.map { it.id })
        assertEquals(program, program.moveItem("s", "1", -1)) // already first: unchanged
        assertEquals(3, program.duplicateItem("s", "1").sections[0].items.size)
    }
}
