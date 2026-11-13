package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionEditTest {
    private val ayat = LibraryEntry(id = "e1", type = ItemType.DUA, title = "Dua A", text = "a")
    private val other = LibraryEntry(id = "e2", type = ItemType.DUA, title = "Dua B", text = "b")
    private val group = LibraryGroup(id = "g1", type = ItemType.DUA, title = "Morning", items = listOf(GroupItem("e1"), GroupItem("e2")))
    private val library = Library(listOf(ayat, other), listOf(group))

    private val here = ProgramSection(
        id = "s1",
        title = "Here",
        items = listOf(
            ProgramItem(id = "i1", type = ItemType.DUA, entryId = "e1"),
            ProgramItem(id = "i2", type = ItemType.DUA, groupId = "g1"),
        ),
    )
    private val elsewhere = ProgramSection(
        id = "s2",
        title = "Elsewhere",
        items = listOf(
            ProgramItem(id = "j1", type = ItemType.DUA, groupId = "g1"),
            ProgramItem(id = "j2", type = ItemType.DUA, entryId = "e1"),
        ),
    )
    private val programs = listOf(Program(name = "P", sections = listOf(here, elsewhere)))
    private var ids = 0
    private val freshId = { "new${++ids}" }

    @Test
    fun unchangedAndNewContentIsNotShared() {
        val added = LibraryEntry(id = "e9", type = ItemType.DUA, title = "New", text = "n")
        val edit = SectionEdit(here, entries = listOf(ayat, added))
        assertTrue(edit.sharedImpact(library, programs).isEmpty)
    }

    @Test
    fun countsEachOtherStepOnce() {
        // e1 is read by j2 directly and by j1 through g1; g1 by j1.
        val edit = SectionEdit(here, entries = listOf(ayat.copy(text = "changed")), groups = listOf(group.copy(title = "Evening")))
        val impact = edit.sharedImpact(library, programs)
        assertEquals(2, impact.changes.size)
        assertEquals(2, impact.changes.first { !it.isGroup }.steps)
        assertEquals(1, impact.changes.first { it.isGroup }.steps)
        assertEquals(2, impact.steps)
    }

    @Test
    fun theEditedSectionItselfIsNotCounted() {
        val alone = Program(name = "P", sections = listOf(here))
        val edit = SectionEdit(here, entries = listOf(ayat.copy(text = "changed")))
        assertTrue(edit.sharedImpact(library, listOf(alone)).isEmpty)
    }

    @Test
    fun copiesKeepTheOriginalsForOtherSections() {
        val edit = SectionEdit(here, entries = listOf(ayat.copy(text = "changed")))
        val copied = edit.withCopiesOfShared(library, programs, copyTitle = { "$it (copy)" }, newId = freshId)
        val after = copied.libraryAfter(library)

        // The original text is untouched.
        assertEquals("a", after.entry("e1")!!.text)
        // The single step reads a copy with the edits, under a copy's title.
        val single = copied.section.items.first { it.id == "i1" }
        assertNotEquals("e1", single.entryId)
        assertEquals("changed", after.entry(single.entryId)!!.text)
        assertEquals("Dua A (copy)", after.entry(single.entryId)!!.title)
        // The group, read elsewhere too, is copied to read the new text; the original still reads e1.
        val groupStep = copied.section.items.first { it.id == "i2" }
        assertNotEquals("g1", groupStep.groupId)
        assertEquals(listOf(single.entryId, "e2"), after.group(groupStep.groupId)!!.items.map { it.entryId })
        assertEquals(listOf("e1", "e2"), after.group("g1")!!.items.map { it.entryId })
    }

    @Test
    fun aRenamedCopyKeepsItsNewTitle() {
        val edit = SectionEdit(here, entries = listOf(ayat.copy(title = "Mine", text = "changed")))
        val copied = edit.withCopiesOfShared(library, programs, copyTitle = { "$it (copy)" }, newId = freshId)
        val single = copied.section.items.first { it.id == "i1" }
        assertEquals("Mine", copied.libraryAfter(library).entry(single.entryId)!!.title)
    }

    @Test
    fun aGroupOnlyThisSectionReadsIsChangedInPlace() {
        val alone = listOf(Program(name = "P", sections = listOf(here, elsewhere.copy(items = listOf(elsewhere.items[1])))))
        val edit = SectionEdit(here, groups = listOf(group.copy(required = 1)))
        val copied = edit.withCopiesOfShared(library, alone, copyTitle = { "$it (copy)" }, newId = freshId)
        assertEquals("g1", copied.section.items.first { it.id == "i2" }.groupId)
        assertEquals(1, copied.libraryAfter(library).group("g1")!!.required)
    }

    @Test
    fun applyToSavesSectionAndLibraryTogether() {
        val bundle = ProgramBundle(programs.first(), library)
        val renamed = here.copy(title = "Renamed")
        val saved = SectionEdit(renamed, entries = listOf(other.copy(text = "B!"))).applyTo(bundle)
        assertEquals("Renamed", saved.program.section("s1")!!.title)
        assertEquals("B!", saved.library.entry("e2")!!.text)
        assertEquals(2, saved.program.sections.size)
    }
}
