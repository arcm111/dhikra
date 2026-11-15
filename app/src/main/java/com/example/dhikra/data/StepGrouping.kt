package com.example.dhikra.data

/** Whether a step can be grouped with others: a single library text (not a group, task or heading). */
val ProgramItem.isGroupable: Boolean get() = type.isContent && !isGroup && entryId.isNotBlank()

/** A section edited by moving a text out of a group, or grouping steps: its steps, and the group changed or made. */
data class Regrouped(val items: List<ProgramItem>, val group: LibraryGroup?)

/**
 * Takes the text [entryId] out of the group step [stepId] — out of the group itself, of [library] —
 * as a step of its own, right above the group's step or, when [below], right under it. The text's
 * count in the group goes with it. Taking out the group's last text puts the text in the step's
 * place (the group is left as it is). Null when there is no such step, group or text.
 */
fun List<ProgramItem>.moveOutOfGroup(stepId: String, entryId: String, below: Boolean, library: Library): Regrouped? {
    val at = indexOfFirst { it.id == stepId }.takeIf { it >= 0 } ?: return null
    val step = this[at]
    val group = library.group(step.groupId) ?: return null
    val member = group.items.firstOrNull { it.entryId == entryId } ?: return null
    val type = library.entry(entryId)?.type ?: group.type
    val single = ProgramItem(type = type, entryId = entryId, count = member.count)
    val rest = group.items.filterNot { it.entryId == entryId }
    if (rest.isEmpty()) {
        return Regrouped(toMutableList().also { it[at] = single.copy(note = step.note) }, group = null)
    }
    val items = toMutableList().also { it.add(if (below) at + 1 else at, single) }
    // A choice of more texts than are left (e.g. any 2, of 2 left) is all of them.
    val required = group.required.takeIf { it < rest.size } ?: 0
    return Regrouped(items, group.copy(items = rest, required = required))
}

/**
 * Groups the steps [stepIds] — single texts of one collection — into a new group titled [title]:
 * one group step in the place of the first of them, reading them in order, each its count; their
 * notes go to the group's step. Null when fewer than two such steps are given, or of more than one
 * collection.
 */
fun List<ProgramItem>.groupSteps(stepIds: Set<String>, title: String): Regrouped? {
    val chosen = filter { it.id in stepIds }
    if (chosen.size < 2 || !chosen.all { it.isGroupable } || chosen.map { it.type }.distinct().size != 1) return null
    val group = LibraryGroup(
        type = chosen.first().type,
        title = title.trim(),
        items = chosen.map { GroupItem(it.entryId, it.count.coerceAtLeast(1)) },
    )
    val step = ProgramItem(
        type = group.type,
        groupId = group.id,
        note = chosen.map { it.note.trim() }.filter { it.isNotEmpty() }.joinToString("\n"),
    )
    // Every step before the first chosen one stays: the group goes at its index.
    val at = indexOfFirst { it.id in stepIds }
    val items = filterNot { it.id in stepIds }.toMutableList().also { it.add(at, step) }
    return Regrouped(items, group)
}
