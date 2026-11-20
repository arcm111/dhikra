package io.github.arcm111.dhikra.ui.editor

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import io.github.arcm111.dhikra.data.LibraryBlock
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import io.github.arcm111.dhikra.data.isGroupable
import io.github.arcm111.dhikra.data.moveOutOfGroup
import io.github.arcm111.dhikra.data.groupSteps
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.lerp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.Library
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.data.LibraryGroup
import io.github.arcm111.dhikra.data.LibraryReaders
import io.github.arcm111.dhikra.data.Prayer
import io.github.arcm111.dhikra.data.PrayerTimetable
import io.github.arcm111.dhikra.data.Program
import io.github.arcm111.dhikra.data.ProgramItem
import io.github.arcm111.dhikra.data.ProgramSection
import io.github.arcm111.dhikra.data.SectionEdit
import io.github.arcm111.dhikra.data.SharedImpact
import io.github.arcm111.dhikra.data.newId
import io.github.arcm111.dhikra.data.sharedImpact
import io.github.arcm111.dhikra.data.withCopiesOfShared
import io.github.arcm111.dhikra.ui.components.BellOffIcon
import io.github.arcm111.dhikra.ui.components.EditorPopup
import io.github.arcm111.dhikra.ui.components.NoteIcon
import io.github.arcm111.dhikra.ui.components.PopupLabel
import io.github.arcm111.dhikra.ui.components.icon
import io.github.arcm111.dhikra.ui.components.label
import io.github.arcm111.dhikra.ui.components.accent
import io.github.arcm111.dhikra.ui.components.reorderable
import io.github.arcm111.dhikra.ui.components.rememberReorderState
import io.github.arcm111.dhikra.ui.components.sectionIcon
import io.github.arcm111.dhikra.ui.home.colors
import io.github.arcm111.dhikra.ui.home.label
import io.github.arcm111.dhikra.ui.library.EntryEditorDialog
import io.github.arcm111.dhikra.ui.library.GroupEditorDialog
import io.github.arcm111.dhikra.ui.theme.LocalDhikraPalette
import io.github.arcm111.dhikra.ui.uiDigits

/**
 * The section being edited and the library texts and groups changed with it — a draft: nothing is
 * stored until it is saved ([edit]).
 */
@Stable
private class SectionDraft(val start: ProgramSection, val base: Library) {
    var section by mutableStateOf(start)
    var entries by mutableStateOf(emptyMap<String, LibraryEntry>())
    var groups by mutableStateOf(emptyMap<String, LibraryGroup>())
    var blocks by mutableStateOf(emptyMap<String, LibraryBlock>())

    /** The library as the draft has it: its edited and new texts and groups in place. */
    val library: Library by derivedStateOf {
        base.merge(Library(entries.values.toList(), groups.values.toList(), blocks.values.toList()))
    }

    fun entryEdited(id: String): Boolean = entries[id]?.let { it != base.entry(id) } == true
    fun groupEdited(id: String): Boolean = groups[id]?.let { it != base.group(id) } == true
    fun blockEdited(id: String): Boolean = blocks[id]?.let { it != base.block(id) } == true

    /** Whether the step is new to the section, or changed. */
    fun stepEdited(item: ProgramItem): Boolean = start.items.firstOrNull { it.id == item.id } != item

    val dirty: Boolean by derivedStateOf {
        section != start || entries.keys.any(::entryEdited) || groups.keys.any(::groupEdited) || blocks.keys.any(::blockEdited)
    }

    fun putEntry(entry: LibraryEntry) { entries = entries + (entry.id to entry) }
    fun putGroup(group: LibraryGroup) { groups = groups + (group.id to group) }
    fun putBlock(block: LibraryBlock) { blocks = blocks + (block.id to block) }

    /**
     * Makes the heading [headingId] and its steps (up to the next heading) a library subsection, read
     * here by one step in their place — and readable by other sections.
     */
    fun makeBlock(headingId: String) = items { list ->
        val at = list.indexOfFirst { it.id == headingId }
        if (at < 0) return@items list
        val heading = list[at]
        val end = (at + 1 until list.size).firstOrNull { list[it].type == ItemType.HEADING } ?: list.size
        val block = LibraryBlock(title = heading.title.ifBlank { heading.text }, items = list.subList(at + 1, end).toList())
        putBlock(block)
        list.subList(0, at) + ProgramItem(type = ItemType.HEADING, blockId = block.id) + list.subList(end, list.size)
    }

    /** Puts the subsection read by the step [stepId] back as this section's own steps: its title and copies of its steps. */
    fun detachBlock(stepId: String) = items { list ->
        val at = list.indexOfFirst { it.id == stepId }
        val block = list.getOrNull(at)?.let { library.block(it.blockId) } ?: return@items list
        list.subList(0, at) + ProgramItem(type = ItemType.HEADING, title = block.title) +
            block.items.map { it.copy(id = newId()) } + list.subList(at + 1, list.size)
    }

    /** A step reading the subsection [blockId], right above the step [before]. */
    fun insertBlock(blockId: String, before: String) =
        upsertStep(ProgramItem(type = ItemType.HEADING, blockId = blockId), before = before)

    fun items(transform: (List<ProgramItem>) -> List<ProgramItem>) {
        section = section.copy(items = transform(section.items))
    }

    /** Replaces the step, or adds it: above the step [before] when given, else at the end. */
    fun upsertStep(item: ProgramItem, before: String? = null) = items { list ->
        val index = list.indexOfFirst { it.id == item.id }
        val at = before?.let { id -> list.indexOfFirst { it.id == id } } ?: -1
        when {
            index >= 0 -> list.toMutableList().also { it[index] = item }
            at >= 0 -> list.toMutableList().also { it.add(at, item) }
            else -> list + item
        }
    }

    /** Takes the text [entryId] out of the group step [stepId], as a step right above or below it. */
    fun moveOutOfGroup(stepId: String, entryId: String, below: Boolean) {
        val result = section.items.moveOutOfGroup(stepId, entryId, below, library) ?: return
        result.group?.let(::putGroup)
        items { result.items }
    }

    /** Groups the steps [stepIds] (single texts of one collection) into a new group titled [title]. */
    fun groupSteps(stepIds: Set<String>, title: String) {
        val result = section.items.groupSteps(stepIds, title) ?: return
        result.group?.let(::putGroup)
        items { result.items }
    }

    fun moveStep(itemId: String, delta: Int) = items { list ->
        val from = list.indexOfFirst { it.id == itemId }
        val to = from + delta
        if (from < 0 || to !in list.indices) list else list.toMutableList().also { it.add(to, it.removeAt(from)) }
    }

    /** What saving stores: the section tidied up, and the texts and groups really changed or added. */
    val edit: SectionEdit
        get() = SectionEdit(
            section = section.copy(
                title = section.title.trim(),
                note = section.note.trim(),
                notices = section.notices.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() },
            ),
            entries = entries.values.filter { it != base.entry(it.id) },
            groups = groups.values.filter { it != base.group(it.id) },
            blocks = blocks.values.filter { it != base.block(it.id) },
        )
}

/** A new section, before it is edited: added to [prayer]'s time (tied to it), or without a time. */
internal fun newSection(prayer: Prayer?, timetable: PrayerTimetable?): ProgramSection =
    ReminderDraft.of(null, prayer, timetable).applyTo(ProgramSection(title = "", icon = SectionIcons.first()), timetable)

/** What is open over the section editor. */
private sealed interface SubEditor {
    /** A step to edit, or a new one ([item] null) — placed above the step [before] when given, else last. */
    data class Step(val item: ProgramItem?, val before: String? = null) : SubEditor
    data class Entry(val entryId: String) : SubEditor
    data class Group(val groupId: String) : SubEditor
    data object Time : SubEditor
    data object Delete : SubEditor
    data class Shared(val impact: SharedImpact) : SubEditor
    /** The name of the new group the steps [stepIds] are gathered in. */
    data class NameGroup(val stepIds: Set<String>) : SubEditor
    /** A library subsection to edit. */
    data class Block(val blockId: String) : SubEditor
    /** A library subsection to read, in a step above the step [before]. */
    data class PickBlock(val before: String) : SubEditor
}

/** A step while steps are picked to be grouped: whether it can be picked, and whether it is. */
private class StepPick(val selectable: Boolean, val selected: Boolean, val onToggle: () -> Unit)

private const val STEP_KEY = "step/"

/**
 * A section of the program — all of it — edited in one popup over the program tab: its icon and
 * title (in the header, typed into in place), its time, description and messages, and its steps;
 * a group step shows each of its texts. Steps, texts and groups open their own small popups. It all
 * stays a draft until saved: then, when texts or groups changed are read by other sections too, it
 * says how many of their steps would change and offers to save copies for this section instead.
 */
@Composable
internal fun SectionEditPopup(
    /** The section as stored, or a new one ([isNew]). */
    initial: ProgramSection,
    isNew: Boolean,
    library: Library,
    /** All the programs, to tell what else reads a text or a group (they share the library). */
    programs: List<Program>,
    timetable: PrayerTimetable?,
    onDismiss: () -> Unit,
    onSave: (SectionEdit) -> Unit,
    /** Deletes the section (after confirmation here); null for a new one. */
    onDelete: (() -> Unit)?,
) {
    val draft = remember(initial.id) { SectionDraft(initial, library) }
    val readers = remember(programs, library, initial.id) { LibraryReaders(programs, library, initial.id) }
    val blockUses = remember(programs) {
        programs.flatMap { it.sections }.flatMap { section -> section.items.filter { it.isBlock }.map { it.blockId }.distinct() }
            .groupingBy { it }.eachCount()
    }
    var sub by remember { mutableStateOf<SubEditor?>(null) }
    var titleError by remember { mutableStateOf(false) }
    // The steps picked to be grouped, while picking; null otherwise.
    var picked by remember { mutableStateOf<Set<String>?>(null) }
    val resources = LocalResources.current
    val section = draft.section
    val done = stringResource(R.string.done)

    // What is saved: the draft, its time (when tied to a prayer) at today's prayer times.
    fun finalEdit(): SectionEdit = draft.edit.let { it.copy(section = it.section.withPrayerTimes(timetable)) }

    fun save() {
        if (section.title.isBlank()) {
            titleError = true
            return
        }
        val edit = finalEdit()
        val impact = edit.sharedImpact(library, programs)
        if (impact.isEmpty) onSave(edit) else sub = SubEditor.Shared(impact)
    }

    val listState = rememberLazyListState()
    val haptics = LocalHapticFeedback.current
    val reorder = rememberReorderState(
        listState = listState,
        canMove = { from, to -> (from as String).startsWith(STEP_KEY) && (to as String).startsWith(STEP_KEY) },
        onMove = { from, to ->
            val moved = (from as String).removePrefix(STEP_KEY)
            val target = (to as String).removePrefix(STEP_KEY)
            val items = draft.section.items
            draft.moveStep(moved, items.indexOfFirst { it.id == target } - items.indexOfFirst { it.id == moved })
        },
    )

    EditorPopup(
        // The section's own settings — icon, title, time — in the header, in its tile's colour; its
        // steps in the body below.
        title = {
            SectionHeader(
                section = section,
                isNew = isNew,
                titleError = titleError,
                onTitle = { draft.section = draft.section.copy(title = it); titleError = false },
                onIcon = { draft.section = draft.section.copy(icon = it) },
            )
        },
        headerExtra = { TimeChip(section.withPrayerTimes(timetable), onClick = { sub = SubEditor.Time }) },
        headerActions = {
            if (onDelete != null) {
                IconButton(onClick = { sub = SubEditor.Delete }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete_section), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        onDismiss = onDismiss,
        onSave = ::save,
        dirty = draft.dirty,
        accent = sectionTone(section),
        // The header (and footer) in the tinted colour of the section's tile in the program tab.
        barColor = sectionHeaderColor(section),
        unsavedDot = true,
        // Adding a step, at hand at the bottom whatever the scroll.
        footerStart = {
            val green = MaterialTheme.colorScheme.primary
            val picking = picked
            if (picking != null) {
                IconButton(onClick = { picked = null }) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) }
                Button(
                    onClick = { sub = SubEditor.NameGroup(picking) },
                    enabled = picking.size >= 2,
                    contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                ) {
                    Icon(GroupIcon, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.group_selected, uiDigits(picking.size)), fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
                return@EditorPopup
            }
            OutlinedButton(
                onClick = { sub = SubEditor.Step(null) },
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
                border = BorderStroke(1.5.dp, green),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = green.copy(alpha = 0.10f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest),
                    contentColor = green,
                ),
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.add_step), fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "stepsHeader") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    PopupLabel(text = stringResource(R.string.steps_title, uiDigits(section.items.size)))
                    if (section.items.size > 1) {
                        Text(
                            stringResource(if (picked != null) R.string.group_select_hint else R.string.reorder_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }
                }
            }
            // The section's description, as the note on top of its steps (as in the section's page).
            item(key = "note") {
                DescriptionNote(
                    value = section.note,
                    onValueChange = { draft.section = draft.section.copy(note = it) },
                )
            }
            if (section.items.isEmpty()) {
                item(key = "empty") {
                    OutlinedButton(onClick = { sub = SubEditor.Step(null) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.steps_empty))
                    }
                }
            }
            items(section.items, key = { STEP_KEY + it.id }) { item ->
                val key = STEP_KEY + item.id
                val index = section.items.indexOf(item)
                val picking = picked
                // While picking: the single texts of the collection picked first can be picked.
                val pickType = picking?.firstOrNull()?.let { id -> section.items.firstOrNull { it.id == id }?.type }
                val pick = picking?.let { chosen ->
                    StepPick(
                        selectable = item.isGroupable && (pickType == null || item.type == pickType),
                        selected = item.id in chosen,
                        onToggle = { picked = if (item.id in chosen) chosen - item.id else chosen + item.id },
                    )
                }
                StepCard(
                    item = item,
                    resolved = item.resolvedWith(draft.library),
                    draft = draft,
                    readers = readers,
                    canMoveUp = index > 0,
                    canMoveDown = index < section.items.lastIndex,
                    onEdit = { sub = SubEditor.Step(item) },
                    onEditEntry = { sub = SubEditor.Entry(it) },
                    onEditGroup = { sub = SubEditor.Group(item.groupId) },
                    onMove = { draft.moveStep(item.id, it) },
                    onAddAbove = { sub = SubEditor.Step(null, before = item.id) },
                    onMoveToTop = { draft.moveStep(item.id, -index) },
                    onDuplicate = {
                        draft.items { list ->
                            val at = list.indexOfFirst { it.id == item.id }
                            list.toMutableList().also { it.add(at + 1, item.copy(id = newId())) }
                        }
                    },
                    onDelete = { draft.items { list -> list.filterNot { it.id == item.id } } },
                    pick = pick,
                    onPickToGroup = if (item.isGroupable) ({ picked = setOf(item.id) }) else null,
                    onMoveOut = { entryId, below -> draft.moveOutOfGroup(item.id, entryId, below) },
                    onEditBlock = { sub = SubEditor.Block(item.blockId) },
                    onDetachBlock = { draft.detachBlock(item.id) },
                    onMakeBlock = { draft.makeBlock(item.id) },
                    onInsertBlock = if (draft.library.blocks.isNotEmpty()) ({ sub = SubEditor.PickBlock(item.id) }) else null,
                    blockUses = blockUses[item.blockId] ?: 1,
                    modifier = Modifier
                        .animateItem(
                            placementSpec = if (reorder.isDragging(key)) null
                            else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                        )
                        // Moved only after a long press: a scroll or a tap never moves it by mistake.
                        // Not while picking steps to group, when the steps that cannot be picked fade.
                        .then(if (pick == null) Modifier.reorderable(reorder, key) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) } else Modifier)
                        .alpha(if (pick != null && !pick.selectable) 0.4f else 1f),
                )
            }
        }
    }

    val close = { sub = null }
    when (val open = sub) {
        null -> Unit
        is SubEditor.Step -> ItemEditorDialog(
            initial = open.item,
            library = draft.library,
            onSaveEntry = draft::putEntry,
            onSaveGroup = draft::putGroup,
            onDismiss = close,
            onSave = { draft.upsertStep(it, before = open.before); close() },
            saveLabel = done,
        )
        is SubEditor.Entry -> {
            val entry = draft.library.entry(open.entryId)
            if (entry == null) LaunchedEffect(Unit) { close() } else EntryEditorDialog(
                type = entry.type,
                initial = entry,
                onDismiss = close,
                onSave = { draft.putEntry(it); close() },
                notice = sharedNotice(readers.ofEntry(entry.id).size),
                saveLabel = done,
            )
        }
        is SubEditor.Group -> {
            val group = draft.library.group(open.groupId)
            if (group == null) LaunchedEffect(Unit) { close() } else GroupEditorDialog(
                type = group.type,
                initial = group,
                library = draft.library,
                onDismiss = close,
                onSave = { draft.putGroup(it); close() },
                onSaveEntry = draft::putEntry,
                notice = sharedNotice(readers.ofGroup(group.id).size),
                saveLabel = done,
            )
        }
        SubEditor.Time -> SectionTimeDialog(
            section = section,
            timetable = timetable,
            onDismiss = close,
            onApply = { draft.section = it; close() },
        )
        SubEditor.Delete -> AlertDialog(
            onDismissRequest = close,
            icon = { Icon(Icons.Filled.Delete, null) },
            text = { Text(stringResource(R.string.delete_section_message, initial.title)) },
            confirmButton = {
                TextButton(onClick = { close(); onDelete?.invoke() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.cancel)) } },
        )
        is SubEditor.Block -> {
            val block = draft.library.block(open.blockId)
            if (block == null) LaunchedEffect(Unit) { close() } else BlockEditorDialog(
                block = block,
                library = draft.library,
                uses = blockUses[block.id] ?: 1,
                onSaveEntry = draft::putEntry,
                onSaveGroup = draft::putGroup,
                onDismiss = close,
                onSave = { draft.putBlock(it); close() },
            )
        }
        is SubEditor.PickBlock -> BlockPickerDialog(
            blocks = draft.library.blocks,
            onPick = { draft.insertBlock(it, before = open.before); close() },
            onDismiss = close,
        )
        is SubEditor.NameGroup -> NewGroupDialog(
            onDismiss = close,
            onCreate = { title ->
                draft.groupSteps(open.stepIds, title)
                picked = null
                close()
            },
        )
        is SubEditor.Shared -> SharedSaveDialog(
            impact = open.impact,
            onUpdateAll = { close(); onSave(finalEdit()) },
            onSaveCopy = {
                close()
                onSave(finalEdit().withCopiesOfShared(library, programs, copyTitle = { resources.getString(R.string.section_copy, it) }))
            },
            onDismiss = close,
        )
    }
}

/** Said in a text's or group's editor when other steps read it too; null when none does. */
@Composable
private fun sharedNotice(steps: Int): String? =
    if (steps > 0) stringResource(R.string.shared_edit_notice, uiDigits(steps)) else null

/** A grey the sections' header colours are muted towards. */
private val HeaderShade = Color(0xFF6B7470)

/**
 * The tinted top of a section's tile in the program tab — the colour of how its time is set, a little
 * muted — also the section editor's header, so the two read as the same thing.
 */
@Composable
internal fun sectionHeaderColor(section: ProgramSection): Color = tintedHeader(sectionTone(section))

/**
 * The tinted header of the program's cards (the section tiles, the editor, the day's prayer panels):
 * [tone] a little muted, at [strength] over white.
 */
@Composable
internal fun tintedHeader(tone: Color, strength: Float = 0.32f): Color =
    lerp(tone, HeaderShade, 0.35f).copy(alpha = strength).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)

/** The colour of how a section's time is set: Madinah's clock, a prayer's very time, or otherwise. */
@Composable
internal fun sectionTone(section: ProgramSection): Color {
    val palette = LocalDhikraPalette.current
    return when {
        section.onMadinahClock -> palette.purple
        // At the prayer's very time; a time before or after it is like any other.
        section.anchor?.offsetMinutes == 0 -> palette.quran.accent
        else -> palette.dua.accent
    }
}

/**
 * The popup's header: the section's icon (a tap picks another, its small pencil says so) and its
 * title, typed into in place — underlined, with a pencil, to show it can be — and its steps' count.
 */
@Composable
private fun SectionHeader(
    section: ProgramSection,
    isNew: Boolean,
    titleError: Boolean,
    onTitle: (String) -> Unit,
    onIcon: (String) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var picking by remember { mutableStateOf(false) }
    var focused by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box {
            Surface(
                onClick = { picking = true },
                shape = CircleShape,
                // As on the section's tile: a pale disc, the icon in the colour of how its time is set.
                color = scheme.surfaceContainerLowest.copy(alpha = 0.75f),
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(sectionIcon(section.icon), stringResource(R.string.change_icon), tint = sectionTone(section), modifier = Modifier.size(24.dp))
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Edit, null, tint = scheme.onPrimary, modifier = Modifier.size(11.dp)) }
            DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                Text(
                    stringResource(R.string.section_icon),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
                SectionIconGrid(
                    selected = section.icon,
                    onPick = { onIcon(it); picking = false },
                    modifier = Modifier.width(236.dp).padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val line = when {
                titleError -> scheme.error
                focused -> scheme.primary
                else -> scheme.onSurfaceVariant.copy(alpha = 0.35f)
            }
            BasicTextField(
                value = section.title,
                onValueChange = onTitle,
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    .drawBehind {
                        val stroke = (if (focused || titleError) 2.dp else 1.dp).toPx()
                        drawLine(line, Offset(0f, size.height - stroke / 2), Offset(size.width, size.height - stroke / 2), stroke)
                    }
                    .padding(top = 2.dp, bottom = 6.dp),
                decorationBox = { field ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (section.title.isEmpty()) {
                                Text(
                                    stringResource(R.string.section_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            field()
                        }
                        if (!focused) Icon(Icons.Filled.Edit, null, tint = scheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    }
                },
            )
            if (titleError) {
                Text(
                    text = stringResource(R.string.section_empty_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/**
 * The section's time, in the header: one line in its colour (grey when it has none) on a pale chip,
 * with the bell and a pencil; a tap edits it.
 */
@Composable
private fun TimeChip(section: ProgramSection, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val none = section.reminderMinutes == null
    val tone = if (none) scheme.onSurfaceVariant else sectionTone(section)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (none) BellOffIcon else Icons.Filled.Notifications, stringResource(R.string.section_time_title), tint = tone, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                sectionTimeText(section),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = tone,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Filled.Edit, null, tint = tone.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * The section's description, as the note on top of its steps — as its page shows it: a faint ⓘ and
 * the words — typed into in place, a pencil saying so while it is not.
 */
@Composable
private fun DescriptionNote(value: String, onValueChange: (String) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSurfaceVariant
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    Surface(
        onClick = { focus.requestFocus() },
        shape = MaterialTheme.shapes.medium,
        color = if (focused) scheme.surfaceContainerLowest else Color.Transparent,
        border = BorderStroke(1.dp, if (focused) scheme.primary else scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Info, stringResource(R.string.section_description), tint = ink.copy(alpha = 0.55f), modifier = Modifier.padding(top = 2.dp).size(16.dp))
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = ink),
                cursorBrush = SolidColor(scheme.primary),
                maxLines = 6,
                modifier = Modifier.weight(1f).focusRequester(focus).onFocusChanged { focused = it.isFocused },
                decorationBox = { field ->
                    Box {
                        if (value.isEmpty()) {
                            Text(stringResource(R.string.section_description_hint), style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.55f))
                        }
                        field()
                    }
                },
            )
            if (!focused) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Edit, null, tint = ink.copy(alpha = 0.5f), modifier = Modifier.padding(top = 2.dp).size(15.dp))
            }
        }
    }
}

/**
 * A step of the section being edited: a row with its drag handle, kind, title, count and menu (a tap
 * edits the step); under it, for a library step, what it reads — the text, or each text of the group
 * — tinted in its collection's colours, each one a tap away from its own editor. Marks tell what was
 * changed («معدّل») and what other sections read too.
 */
@Composable
private fun StepCard(
    item: ProgramItem,
    resolved: ProgramItem?,
    draft: SectionDraft,
    readers: LibraryReaders,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onEditEntry: (String) -> Unit,
    onEditGroup: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    /** A new step right above this one; this step to the top of the section. */
    onAddAbove: () -> Unit,
    onMoveToTop: () -> Unit,
    /** While steps are picked to be grouped: this one's pick; null otherwise. */
    pick: StepPick?,
    /** Starts picking steps to group, with this one; null when it cannot be grouped. */
    onPickToGroup: (() -> Unit)?,
    /** Takes a text of this group step out of it, as a step above it or ([below]) under it. */
    onMoveOut: (entryId: String, below: Boolean) -> Unit,
    /** For a subsection's step: edit the subsection, or put it back as this section's own steps. */
    onEditBlock: () -> Unit,
    onDetachBlock: () -> Unit,
    /** For a heading: make it and its steps a subsection. */
    onMakeBlock: () -> Unit,
    /** Read a subsection in a step above this one; null when the library has none. */
    onInsertBlock: (() -> Unit)?,
    /** How many sections read this step's subsection. */
    blockUses: Int,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shown = resolved ?: item
    val colors = shown.type.colors()
    val edited = draft.stepEdited(item)
    if (item.isBlock) {
        BlockStepCard(
            item = item,
            draft = draft,
            uses = blockUses,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onEditBlock = onEditBlock,
            onDetach = onDetachBlock,
            onMove = onMove,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onAddAbove = onAddAbove,
            onMoveToTop = onMoveToTop,
            onInsertBlock = onInsertBlock,
            modifier = modifier,
        )
        return
    }
    if (item.type == ItemType.HEADING) {
        HeadingRow(item, edited, canMoveUp, canMoveDown, onEdit, onMove, onDuplicate, onDelete, onAddAbove, onMoveToTop, onMakeBlock, onInsertBlock, modifier)
        return
    }
    if (item.type == ItemType.NOTICE) {
        NoticeStepRow(item, edited, canMoveUp, canMoveDown, onEdit, onMove, onDuplicate, onDelete, onAddAbove, onMoveToTop, modifier)
        return
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(if (edited) 1.5.dp else 1.dp, if (edited) scheme.tertiary.copy(alpha = 0.7f) else scheme.primary.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        when {
                            pick == null -> Modifier.clickable(onClick = onEdit)
                            pick.selectable -> Modifier.toggleable(value = pick.selected, role = Role.Checkbox, onValueChange = { pick.onToggle() })
                            else -> Modifier
                        }
                    )
                    .padding(top = 7.dp, bottom = 7.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(width = 34.dp, height = 40.dp), contentAlignment = Alignment.Center) {
                    if (pick != null && pick.selectable) {
                        Checkbox(checked = pick.selected, onCheckedChange = null)
                    } else {
                        Icon(
                            painterResource(R.drawable.ic_drag_handle),
                            stringResource(R.string.reorder_handle),
                            tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(colors.accent.copy(alpha = 0.16f).compositeOver(scheme.surfaceContainerLowest)),
                    contentAlignment = Alignment.Center,
                ) { Icon(shown.type.icon, stringResource(shown.type.label), tint = colors.accent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = shown.title.ifBlank { shown.text }.ifBlank { stringResource(if (resolved == null) R.string.step_missing else R.string.untitled) },
                        style = MaterialTheme.typography.titleSmall,
                        color = if (resolved == null) scheme.error else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // A second line only when there is something to add: the kind is the icon's to tell.
                    val details = when {
                        shown.isChoice -> stringResource(R.string.group_choose, uiDigits(shown.choose), uiDigits(shown.members.size))
                        shown.isGroup -> stringResource(R.string.group_count, uiDigits(shown.members.size))
                        shown.type == ItemType.TASK && shown.title.isNotBlank() -> shown.text.ifBlank { null }
                        else -> null
                    }
                    if (edited || item.note.isNotBlank() || details != null) Row(verticalAlignment = Alignment.CenterVertically) {
                        if (edited) {
                            MarkPill(stringResource(R.string.edited), scheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (item.note.isNotBlank()) {
                            Icon(NoteIcon, stringResource(R.string.item_note), tint = scheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            details.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (!shown.isGroup && shown.target > 1) {
                    Text(
                        stringResource(R.string.times_count, uiDigits(shown.target)),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.accent,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                if (pick == null) StepMenu(
                    contentLabel = when {
                        resolved == null || !item.type.isContent -> null
                        item.isGroup -> R.string.edit_group
                        else -> R.string.edit_entry
                    },
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onEdit = onEdit,
                    onEditContent = { if (item.isGroup) onEditGroup() else onEditEntry(item.entryId) },
                    onMove = onMove,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    onAddAbove = onAddAbove,
                    onMoveToTop = onMoveToTop,
                    onPickToGroup = onPickToGroup,
                    onInsertBlock = onInsertBlock,
                ) else Spacer(Modifier.width(8.dp))
            }
            if (resolved != null && item.type.isContent) {
                ContentPanel(
                    item = resolved,
                    draft = draft,
                    readers = readers,
                    onEditEntry = onEditEntry,
                    onEditGroup = onEditGroup,
                    onMoveOut = onMoveOut.takeIf { pick == null },
                )
            }
        }
    }
}

/**
 * A heading step, set apart from the steps and quieter than them: no card, its title small and grey
 * with a rule running on from it, as a divider between the steps it groups. A tap edits it.
 */
@Composable
private fun HeadingRow(
    item: ProgramItem,
    edited: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    /** A new step right above this one; this step to the top of the section. */
    onAddAbove: () -> Unit,
    onMoveToTop: () -> Unit,
    /** Makes this heading and its steps a subsection. */
    onMakeBlock: () -> Unit,
    onInsertBlock: (() -> Unit)?,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = scheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onEdit)
            .padding(top = 2.dp, bottom = 2.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 34.dp, height = 36.dp), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(R.drawable.ic_drag_handle),
                stringResource(R.string.reorder_handle),
                tint = ink.copy(alpha = 0.4f),
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = item.title.ifBlank { item.text }.ifBlank { stringResource(R.string.untitled) },
            style = MaterialTheme.typography.labelLarge,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // The rule takes the rest of the row, so the menu lines up with the cards' menus.
            modifier = Modifier.widthIn(max = 220.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(1.dp).background(scheme.outlineVariant))
        if (edited) {
            Spacer(Modifier.width(8.dp))
            MarkPill(stringResource(R.string.edited), scheme.tertiary)
        }
        StepMenu(
            contentLabel = null,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onEdit = onEdit,
            onEditContent = {},
            onMove = onMove,
            onDuplicate = onDuplicate,
            onDelete = onDelete,
            onAddAbove = onAddAbove,
            onMoveToTop = onMoveToTop,
            onMakeBlock = onMakeBlock,
            onInsertBlock = onInsertBlock,
        )
    }
}

/**
 * A message step: read-only on the section's page, so here a compact line in its kind's colour — its
 * icon and words on a soft tint — with the handle and menu of a step. A tap edits it.
 */
@Composable
private fun NoticeStepRow(
    item: ProgramItem,
    edited: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    /** A new step right above this one; this step to the top of the section. */
    onAddAbove: () -> Unit,
    onMoveToTop: () -> Unit,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = item.kind.accent()
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = 0.10f).compositeOver(scheme.surfaceContainerLowest),
        border = BorderStroke(if (edited) 1.5.dp else 1.dp, if (edited) scheme.tertiary.copy(alpha = 0.7f) else accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 34.dp, height = 36.dp), contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(R.drawable.ic_drag_handle),
                    stringResource(R.string.reorder_handle),
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp),
                )
            }
            Icon(item.kind.icon, stringResource(item.kind.label), tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
            if (edited) {
                Spacer(Modifier.width(6.dp))
                MarkPill(stringResource(R.string.edited), scheme.tertiary)
            }
            StepMenu(
                contentLabel = null,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onEdit = onEdit,
                onEditContent = {},
                onMove = onMove,
                onDuplicate = onDuplicate,
                onDelete = onDelete,
                onAddAbove = onAddAbove,
                onMoveToTop = onMoveToTop,
            )
        }
    }
}

/**
 * What a library step reads, under its row: the text (its opening words), or the group's texts one
 * per line — number, title, count — then a button to edit the group itself (its title, texts,
 * order). In the collection's colours, with the marks of what changed and what is shared.
 */
@Composable
private fun ContentPanel(
    item: ProgramItem,
    draft: SectionDraft,
    readers: LibraryReaders,
    onEditEntry: (String) -> Unit,
    onEditGroup: () -> Unit,
    /** Takes a text out of the group, above it or ([below]) under it; null when not offered. */
    onMoveOut: ((entryId: String, below: Boolean) -> Unit)?,
) {
    val colors = item.type.colors()
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        border = if (colors.border.alpha > 0f) BorderStroke(1.dp, colors.border) else null,
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            if (item.isGroup) {
                Row(Modifier.padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.group_texts_count, uiDigits(item.members.size)),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(6.dp))
                    Marks(edited = draft.groupEdited(item.groupId), shared = readers.ofGroup(item.groupId).size)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onEditGroup, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Filled.Edit, null, Modifier.size(14.dp), tint = colors.accent)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.edit_group), style = MaterialTheme.typography.labelMedium, color = colors.accent)
                    }
                }
                HorizontalDivider(color = colors.accent.copy(alpha = 0.15f))
                item.members.forEachIndexed { index, member ->
                    if (index > 0) HorizontalDivider(Modifier.padding(horizontal = 14.dp), color = colors.accent.copy(alpha = 0.08f))
                    ContentLine(
                        number = index + 1,
                        title = member.title,
                        preview = member.text,
                        count = member.count,
                        accent = colors.accent,
                        ink = colors.ink,
                        edited = draft.entryEdited(member.entryId),
                        shared = 0,
                        onClick = { onEditEntry(member.entryId) },
                        onMoveOut = onMoveOut?.let { move -> { below -> move(member.entryId, below) } },
                    )
                }
            } else {
                ContentLine(
                    number = null,
                    title = null,
                    preview = item.text,
                    count = 1,
                    accent = colors.accent,
                    ink = colors.ink,
                    edited = draft.entryEdited(item.entryId),
                    shared = readers.ofEntry(item.entryId).size,
                    onClick = { onEditEntry(item.entryId) },
                )
            }
        }
    }
    if (item.isChoice) {
        Text(
            stringResource(R.string.required_custom_hint, uiDigits(item.choose)),
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 8.dp),
        )
    }
}

/** One text under a library step: its number in the group, title and opening words, count, marks and pencil. */
@Composable
private fun ContentLine(
    number: Int?,
    title: String?,
    preview: String,
    count: Int,
    accent: Color,
    ink: Color,
    edited: Boolean,
    shared: Int,
    onClick: () -> Unit,
    /** For a text of a group: takes it out, above the group or ([below]) under it. */
    onMoveOut: ((below: Boolean) -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 14.dp, end = if (onMoveOut != null) 0.dp else 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(uiDigits(number), style = MaterialTheme.typography.labelLarge, color = accent, modifier = Modifier.widthIn(min = 26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            // The opening words, unless they are the title again (a short dhikr titled by its words).
            if (title == null || preview.trim() != title.trim()) {
                Text(
                    uiDigits(preview),
                    style = MaterialTheme.typography.bodySmall,
                    color = ink.copy(alpha = 0.75f),
                    maxLines = if (title == null) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Marks(edited = edited, shared = shared)
        if (count > 1) {
            Text(stringResource(R.string.times_count, uiDigits(count)), style = MaterialTheme.typography.labelMedium, color = accent, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Icon(Icons.Filled.Edit, stringResource(R.string.edit_entry), tint = accent.copy(alpha = 0.75f), modifier = Modifier.size(15.dp))
        if (onMoveOut != null) TextMenu(accent, onMoveOut)
    }
}

/** A group's text's menu: taking it out of the group, as a step above the group or under it. */
@Composable
private fun TextMenu(accent: Color, onMoveOut: (below: Boolean) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.text_actions), tint = accent, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuRow(R.string.move_out_above, Icons.Filled.KeyboardArrowUp) { open = false; onMoveOut(false) }
            MenuRow(R.string.move_out_below, Icons.Filled.KeyboardArrowDown) { open = false; onMoveOut(true) }
        }
    }
}

/**
 * A step reading a library subsection: its title, its steps' count and how many sections read it,
 * then its steps one a line. A tap edits the subsection (for every section reading it); its menu
 * also puts it back as this section's own steps.
 */
@Composable
private fun BlockStepCard(
    item: ProgramItem,
    draft: SectionDraft,
    uses: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEditBlock: () -> Unit,
    onDetach: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onAddAbove: () -> Unit,
    onMoveToTop: () -> Unit,
    onInsertBlock: (() -> Unit)?,
    modifier: Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = scheme.primary
    val block = draft.library.block(item.blockId)
    val edited = draft.stepEdited(item) || draft.blockEdited(item.blockId)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = 0.06f).compositeOver(scheme.surfaceContainerLowest),
        border = BorderStroke(if (edited) 1.5.dp else 1.dp, if (edited) scheme.tertiary.copy(alpha = 0.7f) else accent.copy(alpha = 0.45f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEditBlock).padding(top = 7.dp, bottom = 7.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(width = 34.dp, height = 40.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_drag_handle),
                        stringResource(R.string.reorder_handle),
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(BlockIcon, null, tint = accent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        block?.title ?: stringResource(R.string.step_missing),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (block == null) scheme.error else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (edited) {
                            MarkPill(stringResource(R.string.edited), scheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            stringResource(R.string.block_summary, uiDigits(block?.items?.size ?: 0)),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                        if (uses > 1) {
                            Spacer(Modifier.width(6.dp))
                            MarkPill(stringResource(R.string.shared_with, uiDigits(uses - 1)), scheme.secondary)
                        }
                    }
                }
                StepMenu(
                    contentLabel = R.string.block_detach,
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onEdit = onEditBlock,
                    onEditContent = onDetach,
                    onMove = onMove,
                    onDuplicate = onDuplicate,
                    onDelete = onDelete,
                    onAddAbove = onAddAbove,
                    onMoveToTop = onMoveToTop,
                    onInsertBlock = onInsertBlock,
                )
            }
            if (block != null) {
                Column(
                    Modifier.padding(start = 48.dp, end = 14.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    block.items.forEach { step ->
                        val shown = step.resolvedWith(draft.library) ?: step
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(shown.type.icon, null, tint = shown.type.colors().accent, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                shown.title.ifBlank { shown.text }.ifBlank { stringResource(R.string.untitled) },
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A library subsection, edited: its title, and its steps — each a tap from its editor, moved up or
 * down, removed — with a button to add one. Read by [uses] sections, all of which change with it.
 */
@Composable
private fun BlockEditorDialog(
    block: LibraryBlock,
    library: Library,
    uses: Int,
    onSaveEntry: (LibraryEntry) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
    onDismiss: () -> Unit,
    onSave: (LibraryBlock) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var title by remember { mutableStateOf(block.title) }
    var steps by remember { mutableStateOf(block.items) }
    var editingStep by remember { mutableStateOf<ProgramItem?>(null) }
    var addingStep by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(BlockIcon, null) },
        title = { Text(stringResource(R.string.block_editor_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (uses > 1) {
                    Text(stringResource(R.string.block_shared_notice, uiDigits(uses)), style = MaterialTheme.typography.bodySmall, color = scheme.tertiary)
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.block_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                steps.forEachIndexed { index, step ->
                    val shown = step.resolvedWith(library) ?: step
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { editingStep = step },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(shown.type.icon, null, tint = shown.type.colors().accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            shown.title.ifBlank { shown.text }.ifBlank { stringResource(R.string.untitled) },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { steps = steps.toMutableList().also { it.add(index - 1, it.removeAt(index)) } }, enabled = index > 0, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { steps = steps.toMutableList().also { it.add(index + 1, it.removeAt(index)) } }, enabled = index < steps.lastIndex, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { steps = steps.toMutableList().also { it.removeAt(index) } }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.Close, stringResource(R.string.delete), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                TextButton(onClick = { addingStep = true }) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.add_step))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(block.copy(title = title.trim(), items = steps)) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    val open = editingStep
    if (open != null || addingStep) {
        val close = { editingStep = null; addingStep = false }
        ItemEditorDialog(
            initial = open,
            library = library,
            onSaveEntry = onSaveEntry,
            onSaveGroup = onSaveGroup,
            onDismiss = close,
            onSave = { saved ->
                // A subsection's steps never read another subsection.
                if (!saved.isBlock) {
                    val at = steps.indexOfFirst { it.id == saved.id }
                    steps = if (at >= 0) steps.toMutableList().also { it[at] = saved } else steps + saved
                }
                close()
            },
            saveLabel = stringResource(R.string.done),
        )
    }
}

/** The library's subsections to pick one from, to read in a new step. */
@Composable
private fun BlockPickerDialog(blocks: List<LibraryBlock>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(BlockIcon, null) },
        title = { Text(stringResource(R.string.block_pick)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                blocks.forEach { block ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable { onPick(block.id) }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(BlockIcon, null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(block.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                stringResource(R.string.block_summary, uiDigits(block.items.size)),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A heading bar over two lines: a subsection, its title over its steps. */
private val BlockIcon: ImageVector = materialIcon("DhikraBlock") {
    materialPath {
        moveTo(3f, 4f); lineTo(21f, 4f); lineTo(21f, 9f); lineTo(3f, 9f); close()
        moveTo(6f, 12f); lineTo(21f, 12f); lineTo(21f, 14f); lineTo(6f, 14f); close()
        moveTo(6f, 17f); lineTo(21f, 17f); lineTo(21f, 19f); lineTo(6f, 19f); close()
    }
}

/** Asks the name of the new group the picked steps are gathered in. */
@Composable
private fun NewGroupDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(GroupIcon, null) },
        title = { Text(stringResource(R.string.new_group_title)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(title) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.new_group_create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Stacked sheets: several texts gathered as one. */
private val GroupIcon: ImageVector = materialIcon("DhikraGroup") {
    materialPath {
        moveTo(4f, 6f); lineTo(2f, 6f); lineTo(2f, 20f); curveTo(2f, 21.1f, 2.9f, 22f, 4f, 22f); lineTo(18f, 22f); lineTo(18f, 20f); lineTo(4f, 20f); close()
        moveTo(20f, 2f); lineTo(8f, 2f); curveTo(6.9f, 2f, 6f, 2.9f, 6f, 4f); lineTo(6f, 16f); curveTo(6f, 17.1f, 6.9f, 18f, 8f, 18f); lineTo(20f, 18f)
        curveTo(21.1f, 18f, 22f, 17.1f, 22f, 16f); lineTo(22f, 4f); curveTo(22f, 2.9f, 21.1f, 2f, 20f, 2f); close()
    }
}

/** The small marks of a text or group: changed in this draft, and read by [shared] other steps. */
@Composable
private fun Marks(edited: Boolean, shared: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        if (edited) MarkPill(stringResource(R.string.edited), MaterialTheme.colorScheme.tertiary)
        if (shared > 0) MarkPill(stringResource(R.string.shared_with, uiDigits(shared)), MaterialTheme.colorScheme.secondary)
    }
}

@Composable
private fun MarkPill(text: String, color: Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
    )
}

/** A step's menu: edit it, edit what it reads, move it, duplicate it, delete it. */
@Composable
private fun StepMenu(
    contentLabel: Int?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onEditContent: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    /** A new step right above this one; this step to the top of the section. */
    onAddAbove: () -> Unit,
    onMoveToTop: () -> Unit,
    /** Starts picking steps to group with this one; null when it cannot be grouped. */
    onPickToGroup: (() -> Unit)? = null,
    /** For a heading: makes it and its steps a subsection. */
    onMakeBlock: (() -> Unit)? = null,
    /** Reads a library subsection in a step above this one. */
    onInsertBlock: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more), modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            fun pick(action: () -> Unit): () -> Unit = { open = false; action() }
            MenuRow(R.string.edit_item, Icons.Filled.Edit, pick(onEdit))
            if (contentLabel != null) MenuRow(contentLabel, Icons.Outlined.Edit, pick(onEditContent))
            MenuRow(R.string.add_step_above, Icons.Filled.AddCircle, pick(onAddAbove))
            if (canMoveUp) MenuRow(R.string.move_to_top, MoveToTopIcon, pick(onMoveToTop))
            if (canMoveUp) MenuRow(R.string.move_up, Icons.Filled.KeyboardArrowUp, pick { onMove(-1) })
            if (canMoveDown) MenuRow(R.string.move_down, Icons.Filled.KeyboardArrowDown, pick { onMove(1) })
            MenuRow(R.string.duplicate, Icons.Filled.Add, pick(onDuplicate))
            if (onPickToGroup != null) MenuRow(R.string.select_to_group, GroupIcon, pick(onPickToGroup))
            if (onMakeBlock != null) MenuRow(R.string.block_make, BlockIcon, pick(onMakeBlock))
            if (onInsertBlock != null) MenuRow(R.string.block_insert, BlockIcon, pick(onInsertBlock))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = pick(onDelete),
            )
        }
    }
}

/** Two chevrons up: to the top of the section. */
private val MoveToTopIcon: ImageVector = materialIcon("DhikraMoveToTop") {
    materialPath {
        moveTo(7.41f, 18.41f); lineTo(12f, 13.83f); lineTo(16.59f, 18.41f); lineTo(18f, 17f); lineTo(12f, 11f); lineTo(6f, 17f); close()
        moveTo(7.41f, 11.41f); lineTo(12f, 6.83f); lineTo(16.59f, 11.41f); lineTo(18f, 10f); lineTo(12f, 4f); lineTo(6f, 10f); close()
    }
}

@Composable
private fun MenuRow(label: Int, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(stringResource(label)) }, leadingIcon = { Icon(icon, null) }, onClick = onClick)
}

/**
 * Asked on saving when texts or groups changed are read by other sections too: which ones, and how
 * many steps would change — then updating them everywhere, saving copies for this section only, or
 * going back to editing.
 */
@Composable
private fun SharedSaveDialog(impact: SharedImpact, onUpdateAll: () -> Unit, onSaveCopy: () -> Unit, onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Info, null) },
        title = { Text(stringResource(R.string.shared_save_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.shared_save_message, uiDigits(impact.steps)), style = MaterialTheme.typography.bodyMedium)
                impact.changes.forEach { change ->
                    val accent = change.type.colors().accent
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(change.type.icon, null, tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            change.title + if (change.isGroup) "  ·  " + stringResource(R.string.step_group) else "",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        MarkPill(stringResource(R.string.section_steps_count, uiDigits(change.steps)), scheme.secondary)
                    }
                }
                Text(stringResource(R.string.shared_save_hint), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = onUpdateAll, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.shared_update_all, uiDigits(impact.steps)))
                }
                OutlinedButton(onClick = onSaveCopy, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.shared_save_copy)) }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.keep_editing)) }
            }
        },
    )
}
