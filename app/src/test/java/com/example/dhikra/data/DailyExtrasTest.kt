package com.example.dhikra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyExtrasTest {

    @Test
    fun targets_riseThroughTheLadder_thenEveryTenThousand() {
        assertEquals(100, DailyCounter.target(0))
        assertEquals(100, DailyCounter.target(99))
        // Reaching a target moves on to the next.
        assertEquals(300, DailyCounter.target(100))
        assertEquals(500, DailyCounter.target(300))
        assertEquals(1_000, DailyCounter.target(500))
        assertEquals(3_000, DailyCounter.target(1_000))
        assertEquals(5_000, DailyCounter.target(3_000))
        assertEquals(10_000, DailyCounter.target(5_000))
        assertEquals(20_000, DailyCounter.target(10_000))
        assertEquals(30_000, DailyCounter.target(25_000))
    }

    @Test
    fun stage_fillsFromOneTargetToTheNext() {
        assertEquals(0f, DailyCounter.stage(0))
        assertEquals(0.5f, DailyCounter.stage(50))
        assertEquals(0f, DailyCounter.stage(100)) // a new stage starts
        assertEquals(0.5f, DailyCounter.stage(200))
        assertEquals(0.5f, DailyCounter.stage(15_000))
        assertEquals(5_000, DailyCounter.previous(9_999))
        assertEquals(10_000, DailyCounter.previous(10_000))
    }

    private val library = Library(
        entries = listOf(
            LibraryEntry(id = "a", type = ItemType.DHIKR, title = "سبحان الله", text = "سبحان الله"),
            LibraryEntry(id = "b", type = ItemType.DUA, title = "دعاء", text = "…"),
        ),
        groups = listOf(LibraryGroup(id = "g", type = ItemType.DUA, title = "أدعية", items = listOf(GroupItem("b")))),
    )

    @Test
    fun favourites_makeASection_ofTheirTextsAndGroups_inOrder() {
        val refs = listOf(Favourites.group("g"), Favourites.entry("a"), Favourites.entry("gone"))
        val section = Favourites.section(refs, library, "المفضلة")
        // What the library no longer holds is left out.
        assertEquals(listOf("fav:group:g", "fav:entry:a"), section.items.map { it.id })
        assertEquals("سبحان الله", section.items[1].text)
        // A group's texts get their own ids, apart from the programs' steps.
        assertEquals(listOf("fav:group:g/b"), section.items[0].members.map { it.id })
    }
}
