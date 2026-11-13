package com.example.dhikra.data

/**
 * What the section editor saves in one go: the [section] as edited, and the library texts
 * ([entries]) and [groups] it changed or added along the way. Nothing is stored until it is saved,
 * then all of it at once (see [applyTo]).
 */
data class SectionEdit(
    val section: ProgramSection,
    val entries: List<LibraryEntry> = emptyList(),
    val groups: List<LibraryGroup> = emptyList(),
) {
    /** The [library] with this edit's texts and groups added or replaced. */
    fun libraryAfter(library: Library): Library = library.merge(Library(entries, groups))

    /** [bundle] with this edit saved: the section added or replaced, the library updated. */
    fun applyTo(bundle: ProgramBundle): ProgramBundle = bundle.copy(
        program = bundle.program.upsertSection(section),
        library = libraryAfter(bundle.library),
    )
}

/** A library text or group the edit changed that steps of other sections read too. */
data class SharedChange(
    val id: String,
    val type: ItemType,
    val title: String,
    val isGroup: Boolean,
    /** How many steps outside the section edited read it. */
    val steps: Int,
)

/** The changed texts and groups other sections read, and how many of their steps would change. */
data class SharedImpact(val changes: List<SharedChange>, val steps: Int) {
    val isEmpty: Boolean get() = changes.isEmpty()
}

/**
 * Who reads what, outside the section [exceptSectionId], across [programs] (they share [library]):
 * the steps that would show a change made to a text or a group.
 */
class LibraryReaders(programs: List<Program>, private val library: Library, exceptSectionId: String?) {
    private val steps: List<ProgramItem> = programs.flatMap { program ->
        program.sections.filter { it.id != exceptSectionId }.flatMap { section -> section.items.filter { it.type.isContent } }
    }

    /** The steps (ids) reading the entry [entryId], on its own or through one of its groups. */
    fun ofEntry(entryId: String): Set<String> = steps.filter { step ->
        step.entryId == entryId || (step.isGroup && library.group(step.groupId)?.contains(entryId) == true)
    }.mapTo(mutableSetOf()) { it.id }

    /** The steps (ids) reading the group [groupId]. */
    fun ofGroup(groupId: String): Set<String> = steps.filter { it.groupId == groupId }.mapTo(mutableSetOf()) { it.id }
}

/** The entries of this edit that change a text already in [base] (not the new ones). */
fun SectionEdit.changedEntries(base: Library): List<LibraryEntry> =
    entries.filter { entry -> base.entry(entry.id)?.let { it != entry } == true }

/** The groups of this edit that change a group already in [base] (not the new ones). */
fun SectionEdit.changedGroups(base: Library): List<LibraryGroup> =
    groups.filter { group -> base.group(group.id)?.let { it != group } == true }

/**
 * The texts and groups this edit changes that other sections read too ([programs], the section
 * itself left out), with the number of their steps that would change (each counted once).
 */
fun SectionEdit.sharedImpact(base: Library, programs: List<Program>): SharedImpact {
    val readers = LibraryReaders(programs, base, exceptSectionId = section.id)
    val affected = mutableSetOf<String>()
    val changes = buildList {
        changedEntries(base).forEach { entry ->
            val steps = readers.ofEntry(entry.id)
            if (steps.isNotEmpty()) {
                affected += steps
                add(SharedChange(entry.id, entry.type, entry.title, isGroup = false, steps = steps.size))
            }
        }
        changedGroups(base).forEach { group ->
            val steps = readers.ofGroup(group.id)
            if (steps.isNotEmpty()) {
                affected += steps
                add(SharedChange(group.id, group.type, group.title, isGroup = true, steps = steps.size))
            }
        }
    }
    return SharedImpact(changes, affected.size)
}

/**
 * This edit with the shared texts and groups it changed kept to the section: each one saved as a
 * new copy that only the section reads, the originals left as they were for the other sections.
 * A copy keeps the title it was given, or [copyTitle] of it when it was not renamed. A group the
 * section reads that holds a copied text reads the copy instead — itself copied when other sections
 * read it too.
 */
fun SectionEdit.withCopiesOfShared(
    base: Library,
    programs: List<Program>,
    copyTitle: (String) -> String,
    newId: () -> String = ::newId,
): SectionEdit {
    val readers = LibraryReaders(programs, base, exceptSectionId = section.id)
    val draft = libraryAfter(base)

    // Shared texts changed: each one becomes a copy.
    val entryCopies = mutableMapOf<String, LibraryEntry>()
    changedEntries(base).forEach { entry ->
        if (readers.ofEntry(entry.id).isNotEmpty()) {
            val original = base.entry(entry.id)!!
            entryCopies[entry.id] = entry.copy(
                id = newId(),
                title = if (entry.title == original.title) copyTitle(entry.title) else entry.title,
            )
        }
    }
    fun entryId(id: String) = entryCopies[id]?.id ?: id

    // The groups the section reads, their texts swapped for the copies; a shared one changed is copied.
    val groupOut = mutableMapOf<String, LibraryGroup>()
    val groupIds = mutableMapOf<String, String>()
    section.items.filter { it.isGroup }.map { it.groupId }.distinct().forEach { id ->
        val edited = draft.group(id) ?: return@forEach
        val group = edited.copy(items = edited.items.map { it.copy(entryId = entryId(it.entryId)) })
        val original = base.group(id)
        when {
            original == null -> groupOut[id] = group // new: only this section reads it
            original == group -> Unit // unchanged
            readers.ofGroup(id).isNotEmpty() -> {
                val copy = group.copy(
                    id = newId(),
                    title = if (group.title == original.title) copyTitle(group.title) else group.title,
                )
                groupOut[copy.id] = copy
                groupIds[id] = copy.id
            }
            else -> groupOut[id] = group
        }
    }
    // Groups changed that the section does not read (any more): kept unless others read them.
    groups.forEach { group ->
        val handled = group.id in groupOut || section.items.any { it.groupId == group.id }
        if (!handled && (base.group(group.id) == null || readers.ofGroup(group.id).isEmpty())) groupOut[group.id] = group
    }

    val items = section.items.map { item ->
        when {
            item.isGroup -> groupIds[item.groupId]?.let { item.copy(groupId = it) } ?: item
            item.type.isContent && item.entryId.isNotEmpty() -> item.copy(entryId = entryId(item.entryId))
            else -> item
        }
    }
    // The section's texts after the swap: a copy nothing reads is left out.
    val read = items.mapTo(mutableSetOf()) { it.entryId } + groupOut.values.flatMap { g -> g.items.map { it.entryId } }
    val entriesOut = entries.filter { it.id !in entryCopies } + entryCopies.values.filter { it.id in read }
    return SectionEdit(section.copy(items = items), entriesOut, groupOut.values.toList())
}
