package com.example.dhikra.data

import java.util.UUID

/** The kind of element inside a program section. Each kind has its own visual style. */
enum class ItemType(val key: String) {
    /** A simple instruction to perform (e.g. "pray Duha"), checked off when done. */
    TASK("task"),

    /** A sub heading that groups the following items (e.g. "أذكار الصباح"). Not counted. */
    HEADING("heading"),
    DUA("dua"),
    DHIKR("dhikr"),
    QURAN("quran"),
    HADITH("hadith");

    val isTrackable: Boolean get() = this != HEADING

    /** Texts that live in the library and are only referenced by the program. */
    val isContent: Boolean get() = this in CONTENT

    companion object {
        /** Library collections, in the order they are presented. */
        val CONTENT = listOf(QURAN, HADITH, DUA, DHIKR)

        fun fromKey(key: String?): ItemType = entries.firstOrNull { it.key == key } ?: DUA
    }
}

/** A text of the user's library: a Quran passage, a hadith, a dua or a dhikr. */
data class LibraryEntry(
    val id: String = newId(),
    val type: ItemType,
    val title: String,
    val text: String,
    val description: String = "",
    /** Optional opening line shown above the text (Basmala, Isti'adha...). */
    val opening: String = "",
)

/** One text of a [LibraryGroup]: the library entry [entryId], read [count] times. */
data class GroupItem(val entryId: String, val count: Int = 1)

/**
 * Several texts of one collection read one after the other, e.g. «أدعية الحمد والشكر». The texts
 * stay single library entries (of the group's [type]); the group lists them in order, each with
 * its own repeat count.
 */
data class LibraryGroup(
    val id: String = newId(),
    val type: ItemType,
    val title: String,
    val description: String = "",
    val items: List<GroupItem> = emptyList(),
) {
    fun contains(entryId: String): Boolean = items.any { it.entryId == entryId }
}

/**
 * The four collections (Quran, hadith, dua, dhikr) the daily program picks its texts from: single
 * texts ([entries]) and [groups] of them.
 */
data class Library(
    val entries: List<LibraryEntry> = emptyList(),
    val groups: List<LibraryGroup> = emptyList(),
) {
    private val byId: Map<String, LibraryEntry> by lazy { entries.associateBy { it.id } }
    private val groupsById: Map<String, LibraryGroup> by lazy { groups.associateBy { it.id } }

    fun entry(id: String): LibraryEntry? = byId[id]

    fun group(id: String): LibraryGroup? = groupsById[id]

    fun ofType(type: ItemType): List<LibraryEntry> = entries.filter { it.type == type }

    fun groupsOfType(type: ItemType): List<LibraryGroup> = groups.filter { it.type == type }

    /** The groups that read the entry [entryId]. */
    fun groupsWith(entryId: String): List<LibraryGroup> = groups.filter { it.contains(entryId) }

    fun upsert(entry: LibraryEntry): Library {
        val index = entries.indexOfFirst { it.id == entry.id }
        return copy(
            entries = if (index < 0) entries + entry
            else entries.toMutableList().also { it[index] = entry }
        )
    }

    /** Removes an entry, and takes it out of the groups that read it. */
    fun remove(entryId: String): Library = copy(
        entries = entries.filterNot { it.id == entryId },
        groups = groups.map { group ->
            if (group.contains(entryId)) group.copy(items = group.items.filterNot { it.entryId == entryId })
            else group
        },
    )

    fun upsertGroup(group: LibraryGroup): Library {
        val index = groups.indexOfFirst { it.id == group.id }
        return copy(
            groups = if (index < 0) groups + group
            else groups.toMutableList().also { it[index] = group }
        )
    }

    /** Removes a group; its texts stay in the library. */
    fun removeGroup(groupId: String): Library = copy(groups = groups.filterNot { it.id == groupId })

    /** Adds [other]'s entries and groups, replacing the ones that share an id. */
    fun merge(other: Library): Library {
        val withEntries = other.entries.fold(this) { library, entry -> library.upsert(entry) }
        return other.groups.fold(withEntries) { library, group -> library.upsertGroup(group) }
    }
}

/**
 * A step of the program. Quran / hadith / dua / dhikr steps only reference a [LibraryEntry]
 * ([entryId]) and add how many times to read it, or a [LibraryGroup] ([groupId]); tasks and
 * headings carry their own text.
 */
data class ProgramItem(
    val id: String = newId(),
    val type: ItemType,
    val title: String = "",
    val text: String = "",
    /** Filled in from the library entry when the program is resolved for display. */
    val opening: String = "",
    /** How many times the item should be read. */
    val count: Int = 1,
    /** Filled in from the library entry's description when resolved for display. */
    val source: String = "",
    /** Free note: virtue, instructions... */
    val note: String = "",
    /** The referenced library entry, for content steps reading a single text. */
    val entryId: String = "",
    /** The referenced library group, for content steps reading a group of texts. */
    val groupId: String = "",
    /**
     * Filled in from the library group when resolved for display: one item per text of the group,
     * each tracked on its own (its id is this step's id, a slash and the entry id).
     */
    val members: List<ProgramItem> = emptyList(),
) {
    val isGroup: Boolean get() = groupId.isNotEmpty()

    /**
     * Number of completions needed for this item to be considered done; for a group, the number of
     * its texts (each one is done on its own).
     */
    val target: Int
        get() = when {
            type == ItemType.TASK -> 1
            type == ItemType.HEADING -> 0
            isGroup -> members.size
            else -> count.coerceAtLeast(1)
        }

    val isRepeatable: Boolean get() = !isGroup && type != ItemType.TASK && type.isTrackable && count > 1

    /**
     * This step with its library text (or its group's texts) filled in, or null when what it reads
     * no longer exists.
     */
    fun resolvedWith(library: Library): ProgramItem? {
        if (!type.isContent) return this
        if (isGroup) {
            val group = library.group(groupId) ?: return null
            val members = group.items.mapNotNull { member ->
                library.entry(member.entryId)?.let { entry ->
                    ProgramItem(
                        id = "$id/${entry.id}",
                        type = entry.type,
                        title = entry.title,
                        text = entry.text,
                        opening = entry.opening,
                        count = member.count,
                        source = entry.description,
                        entryId = entry.id,
                    )
                }
            }
            if (members.isEmpty()) return null
            return copy(type = group.type, title = group.title, source = group.description, members = members)
        }
        val entry = library.entry(entryId) ?: return null
        return copy(
            type = entry.type,
            title = entry.title,
            text = entry.text,
            opening = entry.opening,
            source = entry.description,
        )
    }
}

data class ProgramSection(
    val id: String = newId(),
    val title: String,
    val icon: String = "🕌",
    /** Daily reminder time as minutes from midnight, or null for no reminder. */
    val reminderMinutes: Int? = null,
    val note: String = "",
    val items: List<ProgramItem> = emptyList(),
) {
    val trackableItems: List<ProgramItem> get() = items.filter { it.type.isTrackable }
}

data class Program(
    val name: String,
    val sections: List<ProgramSection> = emptyList(),
) {
    val itemCount: Int get() = sections.sumOf { it.trackableItems.size }

    fun section(id: String?): ProgramSection? = sections.firstOrNull { it.id == id }

    /** The program as displayed: library texts filled in, dangling references dropped. */
    fun resolvedWith(library: Library): Program = copy(
        sections = sections.map { section ->
            section.copy(items = section.items.mapNotNull { it.resolvedWith(library) })
        }
    )

    /** How many steps read the library entry [entryId] on its own (not through a group). */
    fun usages(entryId: String): Int =
        sections.sumOf { section -> section.items.count { it.type.isContent && it.entryId == entryId } }

    /** How many steps read the library group [groupId]. */
    fun groupUsages(groupId: String): Int =
        sections.sumOf { section -> section.items.count { it.type.isContent && it.groupId == groupId } }

    /** Whether [entryId] is read by a step, on its own or through one of [library]'s groups. */
    fun reads(entryId: String, library: Library): Boolean =
        usages(entryId) > 0 || library.groupsWith(entryId).any { groupUsages(it.id) > 0 }

    /** Removes every step that references the library entry [entryId]. */
    fun withoutEntry(entryId: String): Program = copy(
        sections = sections.map { section ->
            section.copy(items = section.items.filterNot { it.type.isContent && it.entryId == entryId })
        }
    )

    /** Removes every step that references the library group [groupId]. */
    fun withoutGroup(groupId: String): Program = copy(
        sections = sections.map { section ->
            section.copy(items = section.items.filterNot { it.type.isContent && it.groupId == groupId })
        }
    )

    fun upsertSection(section: ProgramSection): Program {
        val index = sections.indexOfFirst { it.id == section.id }
        return copy(
            sections = if (index < 0) sections + section
            else sections.toMutableList().also { it[index] = section }
        )
    }

    fun removeSection(sectionId: String): Program =
        copy(sections = sections.filterNot { it.id == sectionId })

    fun moveSection(sectionId: String, delta: Int): Program =
        copy(sections = sections.move(sections.indexOfFirst { it.id == sectionId }, delta))

    fun updateSection(sectionId: String, transform: (ProgramSection) -> ProgramSection): Program =
        copy(sections = sections.map { if (it.id == sectionId) transform(it) else it })

    fun upsertItem(sectionId: String, item: ProgramItem): Program = updateSection(sectionId) { s ->
        val index = s.items.indexOfFirst { it.id == item.id }
        s.copy(
            items = if (index < 0) s.items + item
            else s.items.toMutableList().also { it[index] = item }
        )
    }

    fun removeItem(sectionId: String, itemId: String): Program = updateSection(sectionId) { s ->
        s.copy(items = s.items.filterNot { it.id == itemId })
    }

    fun moveItem(sectionId: String, itemId: String, delta: Int): Program =
        updateSection(sectionId) { s ->
            s.copy(items = s.items.move(s.items.indexOfFirst { it.id == itemId }, delta))
        }

    fun duplicateItem(sectionId: String, itemId: String): Program = updateSection(sectionId) { s ->
        val index = s.items.indexOfFirst { it.id == itemId }
        if (index < 0) s
        else s.copy(items = s.items.toMutableList().also { it.add(index + 1, it[index].copy(id = newId())) })
    }
}

/** A program together with the library it references: what is stored, exported and shared. */
data class ProgramBundle(val program: Program, val library: Library)

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

private fun <T> List<T>.move(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().also { list -> list.add(target, list.removeAt(index)) }
}
