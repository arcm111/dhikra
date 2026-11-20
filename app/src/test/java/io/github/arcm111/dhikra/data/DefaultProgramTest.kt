package io.github.arcm111.dhikra.data

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The built-in program: the starter a first launch follows, with only the texts it reads. */
class DefaultProgramTest {

    private val store = DefaultProgram.store()
    private val library = store.library
    private val starter = store.programs.first { it.id == DefaultProgram.STARTER_ID }

    /** The morning & evening adhkar dataset the starter is built from (scripts/starter). */
    private val dataset: List<DatasetText> by lazy {
        val file = listOf("../scripts/starter/adhkar-db/ar.json", "scripts/starter/adhkar-db/ar.json")
            .map(::File).first { it.exists() }
        val array = JSONArray(file.readText())
        (0 until array.length()).map { i ->
            val item = array.getJSONObject(i)
            DatasetText(item.getInt("order"), item.getInt("type"), item.getInt("count"), item.getString("content"))
        }.sortedBy { it.order }
    }

    private data class DatasetText(val order: Int, val type: Int, val count: Int, val content: String)

    @Test
    fun firstLaunch_followsTheStarter_theOnlyBuiltInProgram() {
        assertEquals(listOf(DefaultProgram.STARTER_ID), store.programs.map { it.id })
        assertEquals(DefaultProgram.STARTER_ID, store.active.id)
        assertEquals(starter, DefaultProgram.create())
        assertEquals(ProgramBundle(starter, library), DefaultProgram.bundle())
        assertEquals("الأذكار اليومية", starter.name)
        assertEquals(store.programs, DefaultProgram.programs())
        assertEquals(starter, DefaultProgram.program(DefaultProgram.STARTER_ID))
        // «برنامج المرحلة الثانية» is no longer built in.
        assertEquals(null, DefaultProgram.program(DefaultProgram.PROGRAM_ID))
        assertEquals(null, DefaultProgram.program("mine"))
    }

    @Test
    fun starter_hasMorningEveningAfterEachPrayerAndSleep() {
        assertEquals(
            listOf(
                "بعد صلاة الفجر", "أذكار الصباح", "بعد صلاة الظهر", "بعد صلاة العصر", "أذكار المساء",
                "بعد صلاة المغرب", "بعد صلاة العشاء", "أذكار النوم",
            ),
            starter.sections.map { it.title },
        )
        val anchors = starter.sections.map { it.anchor }
        assertEquals(
            listOf(
                SectionAnchor(Prayer.FAJR), SectionAnchor(Prayer.FAJR, 15), SectionAnchor(Prayer.DHUHR),
                SectionAnchor(Prayer.ASR), SectionAnchor(Prayer.ASR, 15), SectionAnchor(Prayer.MAGHRIB),
                SectionAnchor(Prayer.ISHA), null,
            ),
            anchors,
        )
        assertEquals(22 * 60 + 30, starter.sections.last().reminderMinutes)
        // The five prayers read one subsection.
        val afterPrayer = starter.sections.filter { it.title.startsWith("بعد صلاة") }
        assertEquals(5, afterPrayer.size)
        val blockIds = afterPrayer.map { section -> section.items.single().blockId }.toSet()
        assertEquals(1, blockIds.size)
        val block = library.blocks.single { it.id == blockIds.single() }
        assertEquals("أذكار بعد الصلاة", block.title)
        // Every step finds its text, its group or its subsection.
        val resolved = starter.resolvedWith(library)
        starter.sections.zip(resolved.sections).forEach { (section, withTexts) ->
            assertTrue(section.title, withTexts.items.isNotEmpty())
            assertEquals(section.title, section.shownCount(library), withTexts.items.size)
        }
    }

    @Test
    fun starter_readsTheQuranFromItsAyat() {
        val steps = starter.sections.flatMap { it.items } + library.blocks.flatMap { it.items }
        val quranEntries = steps.filter { it.type == ItemType.QURAN }.flatMap { step ->
            if (step.isGroup) library.group(step.groupId)!!.items.map { it.entryId } else listOf(step.entryId)
        }
        assertTrue(quranEntries.isNotEmpty())
        quranEntries.forEach { id ->
            val entry = library.entry(id)
            assertNotNull(id, entry)
            assertTrue(id, entry!!.quran?.isValid == true && entry.text.isEmpty())
        }
        // No dataset text the starter added is Quran.
        assertTrue(library.entries.filter { it.id.startsWith("me_") }.none { it.type == ItemType.QURAN })
    }

    @Test
    fun morningAndEveningAdhkar_followTheDataset() {
        // The Quran texts the dataset writes out are read from their ayat: Ayat al-Kursi, the end of
        // al-Baqara, and al-Ikhlas, al-Falaq and an-Nas as one step (the bundled group).
        val quran = mapOf(2 to "ayat_al_kursi", 3 to "2da8f77b3cc3", 4 to "group_muawwidhat")
        listOf("أذكار الصباح" to 1, "أذكار المساء" to 2).forEach { (title, type) ->
            val section = starter.sections.single { it.title == title }
            val expected = dataset.filter { it.type == 0 || it.type == type }.filterNot { it.order == 5 || it.order == 6 }
            assertEquals(title, expected.size, section.items.size)
            expected.zip(section.items).forEach { (text, step) ->
                val ref = quran[text.order]
                when {
                    ref?.startsWith("group_") == true -> {
                        assertEquals(ref, step.groupId)
                        assertTrue(library.group(ref)!!.items.all { it.count == text.count })
                    }
                    ref != null -> {
                        assertEquals(ref, step.entryId)
                        assertEquals(text.count, step.count)
                    }
                    else -> {
                        assertEquals("${text.order}", text.count, step.count)
                        val entry = library.entry(step.entryId)
                        assertNotNull("${text.order}", entry)
                        assertEquals("${text.order}", normalised(text.content), normalised(entry!!.text))
                    }
                }
            }
        }
        // The texts the library didn't have keep the dataset's wording and their takhreej.
        val added = library.entries.filter { it.id.startsWith("me_") }
        assertTrue(added.isNotEmpty())
        added.forEach { entry ->
            val text = dataset.single { "me_${it.order}" == entry.id }
            assertEquals(text.content, entry.text)
            assertTrue(entry.id, entry.description.isNotBlank())
        }
    }

    @Test
    fun library_holdsOnlyWhatTheStarterReads() {
        val entries = mutableSetOf<String>()
        val groups = mutableSetOf<String>()
        val blocks = mutableSetOf<String>()
        fun read(steps: List<ProgramItem>) {
            steps.forEach { step ->
                if (step.entryId.isNotEmpty()) entries += step.entryId
                if (step.isGroup) {
                    groups += step.groupId
                    entries += library.group(step.groupId)!!.items.map { it.entryId }
                }
                if (step.isBlock && blocks.add(step.blockId)) read(library.block(step.blockId)!!.items)
            }
        }
        starter.sections.forEach { read(it.items) }
        assertEquals(entries, library.entries.map { it.id }.toSet())
        assertEquals(groups, library.groups.map { it.id }.toSet())
        assertEquals(blocks, library.blocks.map { it.id }.toSet())
    }

    @Test
    fun aNewVersion_replacesTheStarter_andKeepsTheUsersOwnPrograms() {
        // The program of older versions (now the user's own), followed; an older, edited starter;
        // and another program of the user's.
        val secondStage = Program(
            id = DefaultProgram.PROGRAM_ID,
            name = "برنامج المرحلة الثانية",
            sections = listOf(
                ProgramSection(
                    id = "qiyam",
                    title = "قيام الليل",
                    items = listOf(ProgramItem(id = "qiyam_dua", type = ItemType.DUA, entryId = "dua_qiyam")),
                ),
            ),
        )
        val olderStarter = starter.copy(sections = starter.sections.drop(2))
        val mine = Program(
            id = "mine",
            name = "برنامجي",
            sections = listOf(
                ProgramSection(id = "my_section", title = "وردي", items = listOf(ProgramItem(id = "my_step", type = ItemType.TASK, text = "ورد"))),
            ),
        )
        val myText = LibraryEntry(id = "dua_qiyam", type = ItemType.DUA, title = "دعاء", text = "اللهم لك الحمد")
        val stored = ProgramStore(listOf(secondStage, olderStarter, mine), DefaultProgram.PROGRAM_ID, Library(listOf(myText)))

        val updated = stored.withDefaults(store)
        assertEquals(listOf(DefaultProgram.PROGRAM_ID, DefaultProgram.STARTER_ID, "mine"), updated.programs.map { it.id })
        assertEquals(secondStage, updated.programs[0])
        assertEquals(starter, updated.programs[1])
        assertEquals(mine, updated.programs[2])
        // Still following the same program.
        assertEquals(DefaultProgram.PROGRAM_ID, updated.activeId)
        assertEquals("mine", stored.copy(activeId = "mine").withDefaults(store).active.id)
        // The user's texts stay, the built-in ones join them.
        assertEquals(myText, updated.library.entry("dua_qiyam"))
        assertTrue(library.entries.all { updated.library.entry(it.id) == it })
        assertTrue(library.blocks.all { updated.library.block(it.id) == it })
        // Once more (the next version): nothing is added twice.
        assertEquals(updated, updated.withDefaults(store))
    }

    @Test
    fun aNewVersion_givesTheAddedProgramIdsOutsideTheUsersOwn() {
        val section = ProgramSection(
            id = "st_sabah",
            title = "صباحي",
            items = listOf(ProgramItem(id = "st_sabah_1", type = ItemType.TASK, text = "ورد")),
        )
        val mine = Program(id = "mine", name = "برنامجي", sections = listOf(section))
        val updated = ProgramStore(listOf(mine), "mine", Library()).withDefaults(store)
        assertEquals("mine", updated.activeId)
        assertEquals(mine, updated.programs.first { it.id == "mine" })
        val ids = updated.programs.flatMap { it.ids }
        assertEquals(ids.size, ids.toSet().size)
        val added = updated.programs.first { it.id == DefaultProgram.STARTER_ID }
        assertEquals(starter.sections.map { it.title }, added.sections.map { it.title })
    }

    @Test
    fun aStoreWhoseProgramIsGone_followsTheStarter() {
        val mine = Program(id = "mine", name = "برنامجي", sections = emptyList())
        val updated = ProgramStore(listOf(mine), "gone", Library()).withDefaults(store)
        assertEquals(DefaultProgram.STARTER_ID, updated.activeId)
    }

    private fun normalised(text: String): String = text
        .replace(Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "")
        .replace("صلى الله عليه وسلم", "ﷺ")
        .replace(Regex("[أإآٱ]"), "ا").replace('ى', 'ي').replace('ة', 'ه').replace('ؤ', 'و').replace('ئ', 'ي')
        .replace(Regex("[^\\u0621-\\u064A\\s]"), " ")
        .split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")
}
