package com.example.dhikra.ui.editor

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
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
import com.example.dhikra.R
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.LibraryReaders
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.Program
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.data.SectionEdit
import com.example.dhikra.data.SharedImpact
import com.example.dhikra.data.newId
import com.example.dhikra.data.sharedImpact
import com.example.dhikra.data.withCopiesOfShared
import com.example.dhikra.ui.components.BellOffIcon
import com.example.dhikra.ui.components.EditorPopup
import com.example.dhikra.ui.components.NoteIcon
import com.example.dhikra.ui.components.PopupLabel
import com.example.dhikra.ui.components.UnsavedNote
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.components.reorderHandle
import com.example.dhikra.ui.components.reorderable
import com.example.dhikra.ui.components.rememberReorderState
import com.example.dhikra.ui.components.sectionIcon
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.home.label
import com.example.dhikra.ui.library.EntryEditorDialog
import com.example.dhikra.ui.library.GroupEditorDialog
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.uiDigits

/**
 * The section being edited and the library texts and groups changed with it — a draft: nothing is
 * stored until it is saved ([edit]).
 */
@Stable
private class SectionDraft(val start: ProgramSection, val base: Library) {
    var section by mutableStateOf(start)
    var entries by mutableStateOf(emptyMap<String, LibraryEntry>())
    var groups by mutableStateOf(emptyMap<String, LibraryGroup>())

    /** The library as the draft has it: its edited and new texts and groups in place. */
    val library: Library by derivedStateOf { base.merge(Library(entries.values.toList(), groups.values.toList())) }

    fun entryEdited(id: String): Boolean = entries[id]?.let { it != base.entry(id) } == true
    fun groupEdited(id: String): Boolean = groups[id]?.let { it != base.group(id) } == true

    /** Whether the step is new to the section, or changed. */
    fun stepEdited(item: ProgramItem): Boolean = start.items.firstOrNull { it.id == item.id } != item

    val dirty: Boolean by derivedStateOf {
        section != start || entries.keys.any(::entryEdited) || groups.keys.any(::groupEdited)
    }

    fun putEntry(entry: LibraryEntry) { entries = entries + (entry.id to entry) }
    fun putGroup(group: LibraryGroup) { groups = groups + (group.id to group) }

    fun items(transform: (List<ProgramItem>) -> List<ProgramItem>) {
        section = section.copy(items = transform(section.items))
    }

    fun upsertStep(item: ProgramItem) = items { list ->
        val index = list.indexOfFirst { it.id == item.id }
        if (index < 0) list + item else list.toMutableList().also { it[index] = item }
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
        )
}

/** A new section, before it is edited: added to [prayer]'s time (tied to it), or without a time. */
internal fun newSection(prayer: Prayer?, timetable: PrayerTimetable?): ProgramSection =
    ReminderDraft.of(null, prayer, timetable).applyTo(ProgramSection(title = "", icon = SectionIcons.first()), timetable)

/** What is open over the section editor. */
private sealed interface SubEditor {
    data class Step(val item: ProgramItem?) : SubEditor
    data class Entry(val entryId: String) : SubEditor
    data class Group(val groupId: String) : SubEditor
    data object Time : SubEditor
    data object Delete : SubEditor
    data class Shared(val impact: SharedImpact) : SubEditor
}

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
    var sub by remember { mutableStateOf<SubEditor?>(null) }
    var titleError by remember { mutableStateOf(false) }
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
        title = {
            SectionHeader(
                section = section,
                isNew = isNew,
                titleError = titleError,
                onTitle = { draft.section = draft.section.copy(title = it); titleError = false },
                onIcon = { draft.section = draft.section.copy(icon = it) },
            )
        },
        onDismiss = onDismiss,
        onSave = ::save,
        dirty = draft.dirty,
        accent = sectionTone(section),
        footerStart = { UnsavedNote(draft.dirty) },
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "time") { TimeChip(section.withPrayerTimes(timetable), onClick = { sub = SubEditor.Time }) }
            item(key = "note") {
                InlineTextField(
                    value = section.note,
                    onValueChange = { draft.section = draft.section.copy(note = it) },
                    label = stringResource(R.string.section_description),
                    placeholder = stringResource(R.string.section_description_hint),
                )
            }
            item(key = "notices") {
                NoticesEditor(notices = section.notices, onChange = { draft.section = draft.section.copy(notices = it) })
            }
            item(key = "stepsHeader") {
                PopupLabel(
                    text = stringResource(R.string.steps_title, uiDigits(section.items.size)),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    TextButton(onClick = { sub = SubEditor.Step(null) }, contentPadding = PaddingValues(horizontal = 10.dp)) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.add_step))
                    }
                }
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
                StepCard(
                    item = item,
                    resolved = item.resolvedWith(draft.library),
                    draft = draft,
                    readers = readers,
                    handle = Modifier.reorderHandle(reorder, key) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                    canMoveUp = index > 0,
                    canMoveDown = index < section.items.lastIndex,
                    onEdit = { sub = SubEditor.Step(item) },
                    onEditEntry = { sub = SubEditor.Entry(it) },
                    onEditGroup = { sub = SubEditor.Group(item.groupId) },
                    onMove = { draft.moveStep(item.id, it) },
                    onDuplicate = {
                        draft.items { list ->
                            val at = list.indexOfFirst { it.id == item.id }
                            list.toMutableList().also { it.add(at + 1, item.copy(id = newId())) }
                        }
                    },
                    onDelete = { draft.items { list -> list.filterNot { it.id == item.id } } },
                    modifier = Modifier
                        .animateItem(
                            placementSpec = if (reorder.isDragging(key)) null
                            else spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold),
                        )
                        .reorderable(reorder, key) { haptics.performHapticFeedback(HapticFeedbackType.LongPress) },
                )
            }
            if (onDelete != null) {
                item(key = "delete") {
                    TextButton(onClick = { sub = SubEditor.Delete }, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Filled.Delete, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.delete_section), color = MaterialTheme.colorScheme.error)
                    }
                }
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
            onSave = { draft.upsertStep(it); close() },
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
                color = scheme.primaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(sectionIcon(section.icon), stringResource(R.string.change_icon), tint = scheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
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
                else -> scheme.outlineVariant
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
                    .padding(bottom = 4.dp),
                decorationBox = { field ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            if (section.title.isEmpty()) {
                                Text(
                                    stringResource(if (isNew) R.string.new_section else R.string.section_title),
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
            Text(
                text = if (titleError) stringResource(R.string.section_empty_error)
                else stringResource(if (isNew) R.string.new_section else R.string.edit_section) + "  ·  " +
                    stringResource(R.string.section_steps_count, uiDigits(section.trackableItems.size)),
                style = MaterialTheme.typography.labelSmall,
                color = if (titleError) scheme.error else scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/** The section's time as a chip in its colour (grey when it has none); a tap edits it. */
@Composable
private fun TimeChip(section: ProgramSection, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val tone = if (section.reminderMinutes == null) scheme.onSurfaceVariant else sectionTone(section)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = tone.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, tone.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (section.reminderMinutes == null) BellOffIcon else Icons.Filled.Notifications,
                null,
                tint = tone,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.section_time_title), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
                Text(
                    sectionTimeText(section),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (section.reminderMinutes == null) scheme.onSurfaceVariant else tone,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Filled.Edit, stringResource(R.string.section_time_title), tint = tone.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}

/**
 * A field typed into in place, without a heavy frame: a soft box with its [label] over the text and a
 * pencil while it is not being typed into; its border turns the accent colour while it is.
 */
@Composable
private fun InlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val scheme = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    // A tap anywhere on the box (its label too) puts the cursor in the field.
    Surface(
        onClick = { focus.requestFocus() },
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLow,
        border = BorderStroke(if (focused) 1.5.dp else 1.dp, if (focused) scheme.primary else scheme.outlineVariant.copy(alpha = 0.7f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = if (focused) scheme.primary else scheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = textStyle.copy(color = scheme.onSurface),
                    cursorBrush = SolidColor(scheme.primary),
                    maxLines = 6,
                    modifier = Modifier.weight(1f).padding(top = 2.dp).focusRequester(focus).onFocusChanged { focused = it.isFocused },
                    decorationBox = { field ->
                        Box {
                            if (value.isEmpty()) Text(placeholder, style = textStyle, color = scheme.onSurfaceVariant.copy(alpha = 0.6f))
                            field()
                        }
                    },
                )
                if (!focused) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.Edit, null, tint = scheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                }
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
    handle: Modifier,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onEditEntry: (String) -> Unit,
    onEditGroup: () -> Unit,
    onMove: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val shown = resolved ?: item
    val colors = shown.type.colors()
    val edited = draft.stepEdited(item)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(if (edited) 1.5.dp else 1.dp, if (edited) scheme.tertiary.copy(alpha = 0.7f) else scheme.primary.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit).padding(top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(width = 30.dp, height = 40.dp).then(handle), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(R.drawable.ic_drag_handle),
                        stringResource(R.string.reorder_handle),
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(MaterialTheme.shapes.small).background(colors.accent.copy(alpha = 0.18f).compositeOver(scheme.surfaceContainerLowest)),
                    contentAlignment = Alignment.Center,
                ) { Icon(shown.type.icon, null, tint = colors.accent, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = shown.title.ifBlank { shown.text }.ifBlank { stringResource(if (resolved == null) R.string.step_missing else R.string.untitled) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (resolved == null) scheme.error else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val details = listOfNotNull(
                        stringResource(shown.type.label),
                        when {
                            shown.isChoice -> stringResource(R.string.group_choose, uiDigits(shown.choose), uiDigits(shown.members.size))
                            shown.isGroup -> stringResource(R.string.group_count, uiDigits(shown.members.size))
                            shown.type == ItemType.TASK && shown.title.isNotBlank() -> shown.text.ifBlank { null }
                            else -> null
                        },
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (edited) {
                            MarkPill(stringResource(R.string.edited), scheme.tertiary)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (item.note.isNotBlank()) {
                            Icon(NoteIcon, stringResource(R.string.item_note), tint = scheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            details.joinToString("  ·  "),
                            style = MaterialTheme.typography.labelSmall,
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
                StepMenu(
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
                )
            }
            if (resolved != null && item.type.isContent) {
                ContentPanel(item = resolved, draft = draft, readers = readers, onEditEntry = onEditEntry, onEditGroup = onEditGroup)
            }
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
) {
    val colors = item.type.colors()
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.small,
        color = colors.container,
        border = if (colors.border.alpha > 0f) BorderStroke(1.dp, colors.border) else null,
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
    ) {
        Column(Modifier.padding(vertical = 2.dp)) {
            if (item.isGroup) {
                Row(Modifier.padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.group_items) + " (" + uiDigits(item.members.size) + ")",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.accent,
                        modifier = Modifier.weight(1f, fill = false),
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
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 10.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (number != null) {
            Text(uiDigits(number), style = MaterialTheme.typography.labelMedium, color = accent, modifier = Modifier.widthIn(min = 22.dp))
        }
        Column(Modifier.weight(1f)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                uiDigits(preview),
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.75f),
                maxLines = if (title == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(6.dp))
        Marks(edited = edited, shared = shared)
        if (count > 1) {
            Text(stringResource(R.string.times_count, uiDigits(count)), style = MaterialTheme.typography.labelMedium, color = accent, modifier = Modifier.padding(horizontal = 4.dp))
        }
        Icon(Icons.Filled.Edit, stringResource(R.string.edit_entry), tint = accent.copy(alpha = 0.75f), modifier = Modifier.size(15.dp))
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
            if (canMoveUp) MenuRow(R.string.move_up, Icons.Filled.KeyboardArrowUp, pick { onMove(-1) })
            if (canMoveDown) MenuRow(R.string.move_down, Icons.Filled.KeyboardArrowDown, pick { onMove(1) })
            MenuRow(R.string.duplicate, Icons.Filled.Add, pick(onDuplicate))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                onClick = pick(onDelete),
            )
        }
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
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Button(onClick = onUpdateAll) { Text(stringResource(R.string.shared_update_all, uiDigits(impact.steps))) }
                OutlinedButton(onClick = onSaveCopy) { Text(stringResource(R.string.shared_save_copy)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.keep_editing)) }
            }
        },
    )
}
