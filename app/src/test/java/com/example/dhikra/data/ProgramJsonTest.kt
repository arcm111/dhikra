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
        assertEquals(bundle, decoded.withProgramId(bundle.program.id))
    }

    @Test
    fun export_doesNotCarryTheProgramId_soAnImportIsANewProgram() {
        val bundle = DefaultProgram.bundle()
        val json = ProgramJson.encode(bundle)
        assertFalse(JSONObject(json).has("id"))
        assertNotEquals(bundle.program.id, ProgramJson.decode(json).program.id)
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
    fun defaultBundle_hasUniqueIds_andEveryStepFindsItsText() {
        val (program, library) = DefaultProgram.bundle()
        // Sections and steps share one id space: progress is stored per step id.
        val ids = program.sections.map { it.id } + program.sections.flatMap { s -> s.items.map { it.id } }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals(library.entries.size, library.entries.map { it.id }.toSet().size)
        assertEquals(library.groups.size, library.groups.map { it.id }.toSet().size)
        // The default is the user's own program and library: the library may hold texts no step
        // reads yet, but every step must find what it reads.
        val resolved = program.resolvedWith(library)
        program.sections.zip(resolved.sections).forEach { (section, withTexts) ->
            assertEquals(section.title, section.items.size, withTexts.items.size)
        }
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
    fun defaultQuranPassages_areReferencesToTheirAyat() {
        val quran = DefaultProgram.library().ofType(ItemType.QURAN)
        assertTrue(quran.isNotEmpty())
        assertTrue(quran.all { it.quran?.isValid == true && it.text.isEmpty() })
        assertEquals(QuranRef(2, 255), DefaultProgram.library().entry("ayat_al_kursi")?.quran)
    }

    @Test
    fun encode_quranPassagesCarryTheirAyatNotTheirText() {
        val source = QuranSource { ref -> Quran.passage(ref, List(ref.ayahCount) { "آية" }) }
        val bundle = DefaultProgram.bundle().let { it.copy(library = it.library.withQuranText(source)) }
        assertTrue(bundle.library.ofType(ItemType.QURAN).all { it.text.isNotEmpty() })

        val json = ProgramJson.encode(bundle)
        val library = JSONObject(json).getJSONArray("library").let { array ->
            (0 until array.length()).map { array.getJSONObject(it) }
        }
        val kahf = library.single { it.getString("id") == "kahf_1_10" }
        assertEquals("18:1-10", kahf.getString("quran"))
        assertFalse(kahf.has("text"))
        assertTrue(library.filter { it.getString("type") != "quran" }.all { it.has("text") && !it.has("quran") })

        // Read back, the passages are references again: their text comes from the Quran text.
        assertEquals(DefaultProgram.bundle(), ProgramJson.decode(json).withProgramId(DefaultProgram.PROGRAM_ID))
    }

    @Test
    fun decode_quranEntries() {
        val json = """
            {"version":4,"library":[
              {"id":"ref","type":"quran","title":"آية الكرسي","quran":"2:255","text":"ignored"},
              {"id":"untitled","type":"quran","quran":"3:190-200"},
              {"id":"typed","type":"quran","title":"الإخلاص","text":"قل هو الله أحد"},
              {"id":"out_of_range","type":"quran","title":"x","quran":"2:300"},
              {"id":"dua","type":"dua","title":"d","quran":"2:255","text":"دعاء"}
            ],"sections":[]}
        """.trimIndent()
        val library = ProgramJson.decode(json).library
        assertEquals(LibraryEntry("ref", ItemType.QURAN, "آية الكرسي", text = "", quran = QuranRef(2, 255)), library.entry("ref"))
        assertEquals("٣:١٩٠-٢٠٠", library.entry("untitled")?.title)
        // A Quran text typed in before the Quran text was bundled stays a text.
        assertEquals("قل هو الله أحد", library.entry("typed")?.text)
        assertNull(library.entry("typed")?.quran)
        assertNull(library.entry("out_of_range"))
        assertNull(library.entry("dua")?.quran)
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

/** This bundle with its program's [id]: exported files don't carry it, decoding gives a new one. */
internal fun ProgramBundle.withProgramId(id: String) = copy(program = program.copy(id = id))
