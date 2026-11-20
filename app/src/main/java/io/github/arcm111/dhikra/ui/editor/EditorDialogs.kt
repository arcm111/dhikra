package io.github.arcm111.dhikra.ui.editor

import io.github.arcm111.dhikra.ui.components.CompactTextField
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ArabicText
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.Library
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.data.LibraryGroup
import io.github.arcm111.dhikra.data.NoticeKind
import io.github.arcm111.dhikra.data.Prayer
import io.github.arcm111.dhikra.data.PrayerTimetable
import io.github.arcm111.dhikra.data.ProgramItem
import io.github.arcm111.dhikra.data.ProgramJson
import io.github.arcm111.dhikra.data.ProgramSection
import io.github.arcm111.dhikra.data.SectionAnchor
import io.github.arcm111.dhikra.data.SectionClock
import io.github.arcm111.dhikra.data.SectionNotice
import io.github.arcm111.dhikra.data.SectionZone
import io.github.arcm111.dhikra.data.newId
import io.github.arcm111.dhikra.reminder.ReminderScheduler
import io.github.arcm111.dhikra.ui.components.EditorPopup
import io.github.arcm111.dhikra.ui.components.EditorPopupTitle
import io.github.arcm111.dhikra.ui.components.PopupBody
import io.github.arcm111.dhikra.ui.components.PopupAddButton
import io.github.arcm111.dhikra.ui.components.PopupLabel
import io.github.arcm111.dhikra.ui.components.SectionIconChoices
import io.github.arcm111.dhikra.ui.components.accent
import io.github.arcm111.dhikra.ui.components.clearButton
import io.github.arcm111.dhikra.ui.components.icon
import io.github.arcm111.dhikra.ui.components.label
import io.github.arcm111.dhikra.ui.formatClock
import io.github.arcm111.dhikra.ui.home.colors
import io.github.arcm111.dhikra.ui.home.label
import io.github.arcm111.dhikra.ui.library.EntryEditorDialog
import io.github.arcm111.dhikra.ui.library.GroupEditorDialog
import io.github.arcm111.dhikra.ui.prayer.anchorLabel
import io.github.arcm111.dhikra.ui.prayer.anchorOffsetText
import io.github.arcm111.dhikra.ui.prayer.label
import io.github.arcm111.dhikra.ui.theme.ReadingStyles
import io.github.arcm111.dhikra.ui.uiDigits
import java.util.TimeZone

internal val SectionIcons = SectionIconChoices.map { it.first }

// ------------------------------------------------------------------------------ Section

/**
 * The details of a section, as a popup: its title, icon, time and the note and messages shown at its
 * top. (The program tab edits a section — details and steps — in [SectionEditPopup].)
 */
@Composable
fun SectionEditorDialog(
    initial: ProgramSection?,
    onDismiss: () -> Unit,
    onSave: (ProgramSection) -> Unit,
    /** Today's prayer times, to show what a time tied to a prayer comes to. */
    timetable: PrayerTimetable? = null,
    /** For a new section: the prayer its time is tied to at first (added to that prayer's time). */
    prayer: Prayer? = null,
) {
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: SectionIcons.first()) }
    val startTime = remember { ReminderDraft.of(initial, prayer, timetable) }
    var time by rememberSaveable(stateSaver = ReminderDraft.Saver) { mutableStateOf(startTime) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var showError by remember { mutableStateOf(false) }

    fun edited(): ProgramSection = time.applyTo(
        (initial ?: ProgramSection(title = "")).copy(
            title = title.trim(),
            icon = icon,
            note = note.trim(),
        ),
        timetable,
    )

    val dirty = if (initial == null) {
        title.isNotBlank() || note.isNotBlank()
    } else {
        title.trim() != initial.title || icon != initial.icon || time != startTime || note.trim() != initial.note
    }

    EditorPopup(
        title = { EditorPopupTitle(stringResource(if (initial == null) R.string.new_section else R.string.edit_section)) },
        onDismiss = onDismiss,
        onSave = {
            if (title.isBlank()) showError = true else onSave(edited())
        },
        dirty = dirty,
    ) {
        PopupBody {
            CompactTextField(
                value = title,
                onValueChange = { title = it; showError = false },
                trailingIcon = clearButton(title) { title = ""; showError = false },
                label = { Text(stringResource(R.string.section_title)) },
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.section_empty_error)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PopupLabel(stringResource(R.string.section_icon))
            SectionIconRow(selected = icon, onPick = { icon = it })
            ReminderEditor(draft = time, timetable = timetable, onChange = { time = it })
            CompactTextField(
                value = note,
                onValueChange = { note = it },
                trailingIcon = clearButton(note) { note = "" },
                label = { Text(stringResource(R.string.section_note)) },
                minLines = 1,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Only a section's time, as a popup: what the section editor's time chip opens. */
@Composable
internal fun SectionTimeDialog(
    section: ProgramSection,
    timetable: PrayerTimetable?,
    onDismiss: () -> Unit,
    onApply: (ProgramSection) -> Unit,
) {
    val start = remember { ReminderDraft.of(section, null, timetable) }
    var draft by rememberSaveable(stateSaver = ReminderDraft.Saver) { mutableStateOf(start) }
    EditorPopup(
        title = { EditorPopupTitle(stringResource(R.string.section_time_title), section.title) },
        onDismiss = onDismiss,
        onSave = { onApply(draft.applyTo(section, timetable)) },
        dirty = draft != start,
        saveLabel = stringResource(R.string.done),
    ) {
        PopupBody { ReminderEditor(draft = draft, timetable = timetable, onChange = { draft = it }) }
    }
}

/** The section icons in one row that scrolls, the one chosen ringed. */
@Composable
internal fun SectionIconRow(selected: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionIconChoices.forEach { (key, image) -> SectionIconChoice(image, key == selected) { onPick(key) } }
    }
}

/** The section icons as a grid (in a menu). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SectionIconGrid(selected: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SectionIconChoices.forEach { (key, image) -> SectionIconChoice(image, key == selected) { onPick(key) } }
    }
}

@Composable
private fun SectionIconChoice(image: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
        contentColor = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        border = if (selected) BorderStroke(2.dp, scheme.primary) else null,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(image, null, Modifier.size(22.dp)) }
    }
}

/**
 * A section's time as edited: none, a fixed time (on the local clock or Madinah's), or a prayer's
 * time and minutes after or before it. [minutes] is kept while the time is off, to come back to.
 */
internal data class ReminderDraft(
    val enabled: Boolean,
    val minutes: Int,
    val byPrayer: Boolean,
    val prayer: Prayer,
    val offset: Int,
    val zone: SectionZone,
) {
    /** [section] at this time (today's, for one tied to a prayer). */
    fun applyTo(section: ProgramSection, timetable: PrayerTimetable?): ProgramSection {
        val anchor = if (enabled && byPrayer) SectionAnchor(prayer, offset) else null
        return section.copy(
            reminderMinutes = if (enabled) minutes else null,
            anchor = anchor,
            zone = if (anchor == null) zone else SectionZone.LOCAL,
        ).withPrayerTimes(timetable)
    }

    companion object {
        /** The time of [section]; for a new one added to [prayer]'s time, tied to that prayer. */
        fun of(section: ProgramSection?, prayer: Prayer?, timetable: PrayerTimetable?): ReminderDraft {
            val fresh = section == null && prayer != null
            return ReminderDraft(
                enabled = section?.reminderMinutes != null || fresh,
                minutes = section?.reminderMinutes ?: prayer?.let { timetable?.at(it) } ?: (5 * 60),
                byPrayer = section?.anchor != null || fresh,
                prayer = section?.anchor?.prayer ?: prayer ?: Prayer.FAJR,
                offset = section?.anchor?.offsetMinutes ?: 0,
                zone = section?.zone ?: SectionZone.LOCAL,
            )
        }

        val Saver: Saver<ReminderDraft, Any> = listSaver(
            save = { listOf(it.enabled, it.minutes, it.byPrayer, it.prayer.name, it.offset, it.zone.name) },
            restore = {
                ReminderDraft(
                    enabled = it[0] as Boolean,
                    minutes = it[1] as Int,
                    byPrayer = it[2] as Boolean,
                    prayer = Prayer.valueOf(it[3] as String),
                    offset = it[4] as Int,
                    zone = SectionZone.valueOf(it[5] as String),
                )
            },
        )
    }
}

/** A section's time in a few words: «الفجر + ١٠ د · ٠٥:١٢», «٢٢:٣٠», or that it has none. */
@Composable
internal fun sectionTimeText(section: ProgramSection): String {
    val minutes = section.reminderMinutes ?: return stringResource(R.string.no_reminder)
    val anchor = section.anchor
    return when {
        anchor != null -> anchorLabel(anchor) + "  ·  " + formatClock(minutes)
        section.zone == SectionZone.MADINAH -> formatClock(minutes) + "  ·  " + stringResource(R.string.section_zone_madinah)
        else -> formatClock(minutes)
    }
}

/**
 * A section's time: whether it has one (and a daily reminder), then a fixed time — on the user's
 * clock or Madinah's — or a prayer's time with minutes after or before it.
 */
@Composable
internal fun ReminderEditor(draft: ReminderDraft, timetable: PrayerTimetable?, onChange: (ReminderDraft) -> Unit) {
    val context = LocalContext.current
    var picking by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.section_reminder),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = draft.enabled, onCheckedChange = { onChange(draft.copy(enabled = it)) })
        }
        if (draft.enabled) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !draft.byPrayer,
                    onClick = { onChange(draft.copy(byPrayer = false)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.section_time_fixed)) }
                SegmentedButton(
                    selected = draft.byPrayer,
                    onClick = { onChange(draft.copy(byPrayer = true)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.section_time_prayer)) }
            }
            if (draft.byPrayer) {
                PrayerAnchorEditor(
                    prayer = draft.prayer,
                    offset = draft.offset,
                    timetable = timetable,
                    onPrayer = { onChange(draft.copy(prayer = it)) },
                    onOffset = { onChange(draft.copy(offset = it)) },
                )
            } else {
                // Whose clock the time is on — the user's own, or Madinah's — and the time itself.
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionZone.entries.forEach { choice ->
                        FilterChip(
                            selected = draft.zone == choice,
                            onClick = { onChange(draft.copy(zone = choice)) },
                            label = {
                                Text(stringResource(if (choice == SectionZone.MADINAH) R.string.section_zone_madinah else R.string.section_zone_local))
                            },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    OutlinedButton(onClick = { picking = true }) {
                        Text(formatClock(draft.minutes), style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (draft.zone == SectionZone.MADINAH) {
                    val local = timetable?.city?.timeZone?.let(TimeZone::getTimeZone) ?: TimeZone.getDefault()
                    Text(
                        text = stringResource(R.string.section_zone_local_equivalent, formatClock(SectionClock.madinahToLocal(draft.minutes, local))),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !ReminderScheduler.canScheduleExactAlarms(context)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.exact_alarm_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).setData("package:${context.packageName}".toUri())
                        )
                    }) { Text(stringResource(R.string.allow)) }
                }
            }
        }
    }
    if (picking) {
        TimePickerDialog(
            initialMinutes = draft.minutes,
            onDismiss = { picking = false },
            onConfirm = { minutes ->
                onChange(draft.copy(minutes = minutes))
                picking = false
            },
        )
    }
}

/**
 * A section's time tied to a prayer: the prayer (the six times of the day) and how many minutes
 * after — or before — it, with what that comes to today.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrayerAnchorEditor(
    prayer: Prayer,
    offset: Int,
    timetable: PrayerTimetable?,
    onPrayer: (Prayer) -> Unit,
    onOffset: (Int) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Prayer.entries.forEach { option ->
            FilterChip(
                selected = option == prayer,
                onClick = { onPrayer(option) },
                label = { Text(stringResource(option.label)) },
            )
        }
    }
    val limit = ProgramJson.MAX_OFFSET
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconStep("−", stringResource(R.string.anchor_earlier)) { onOffset((offset - 5).coerceAtLeast(-limit)) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = anchorOffsetText(offset), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(
                text = timetable?.let {
                    stringResource(R.string.anchor_today, formatClock(Math.floorMod(it.at(prayer) + offset, 24 * 60)))
                } ?: stringResource(R.string.anchor_no_city),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        OutlinedIconStep("+", stringResource(R.string.anchor_later)) { onOffset((offset + 5).coerceAtMost(limit)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.pick_time)) },
        text = {
            // The clock dial is designed for left-to-right layouts.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ------------------------------------------------------------------------------ Item

private val CountPresets = listOf(1, 3, 7, 10, 33, 100)

/** Step types, in the order offered: library texts first, then tasks and headings. */
private val StepTypes = ItemType.CONTENT + listOf(ItemType.TASK, ItemType.HEADING, ItemType.NOTICE)

/**
 * A task's instructions, edited: a numbered field a line, each with ✕ to remove it, and a button
 * to add one.
 */
@Composable
private fun InstructionsEditor(lines: List<String>, onChange: (List<String>) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.task_instructions), style = MaterialTheme.typography.labelLarge, color = scheme.onSurfaceVariant)
        lines.forEachIndexed { index, line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    uiDigits(index + 1),
                    style = MaterialTheme.typography.labelLarge,
                    color = scheme.primary,
                    modifier = Modifier.width(24.dp),
                )
                CompactTextField(
                    value = line,
                    onValueChange = { changed -> onChange(lines.toMutableList().also { it[index] = changed }) },
                    placeholder = { Text(stringResource(R.string.task_instruction_hint)) },
                    maxLines = 3,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { onChange(lines.toMutableList().also { it.removeAt(index) }) }) {
                    Icon(Icons.Filled.Close, stringResource(R.string.task_instruction_remove), modifier = Modifier.size(18.dp))
                }
            }
        }
        TextButton(onClick = { onChange(lines + "") }) {
            Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.task_instruction_add))
        }
    }
}

/**
 * Editor of one program step, as a compact popup. Quran, hadith, dua and dhikr steps read a single
 * text or a group of texts, picked from the library (a new text or group can be added right here);
 * only tasks and headings have their own text. A single text has a repeat count (a group's texts
 * have their own), and every step can have a note. [saveLabel] names the save button («تم» when the
 * step only goes back to the section being edited).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemEditorDialog(
    initial: ProgramItem?,
    library: Library,
    onSaveEntry: (LibraryEntry) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
    onDismiss: () -> Unit,
    onSave: (ProgramItem) -> Unit,
    saveLabel: String? = null,
) {
    var type by rememberSaveable { mutableStateOf(initial?.type ?: ItemType.QURAN) }
    var entryId by rememberSaveable { mutableStateOf(initial?.entryId.orEmpty()) }
    var groupMode by rememberSaveable { mutableStateOf(initial?.isGroup == true) }
    var groupId by rememberSaveable { mutableStateOf(initial?.groupId.orEmpty()) }
    var addingGroup by remember { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(if (initial?.type?.isContent == true) "" else initial?.title.orEmpty()) }
    var text by rememberSaveable { mutableStateOf(if (initial?.type?.isContent == true) "" else initial?.text.orEmpty()) }
    var countText by rememberSaveable { mutableStateOf((initial?.count ?: 1).toString()) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    // A message step: its kind (information, warning, important).
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: NoticeKind.INFO) }
    // A task's instructions, one a line (blank lines are dropped on saving).
    var instructions by rememberSaveable { mutableStateOf(initial?.instructions.orEmpty()) }
    var query by rememberSaveable { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var addingEntry by remember { mutableStateOf(false) }
    // Changing what the step reads: the list (and its search at the bottom) shows again.
    var changing by rememberSaveable { mutableStateOf(false) }

    val count = ArabicText.toLatinDigits(countText).toIntOrNull()?.coerceIn(1, ProgramJson.MAX_COUNT) ?: 1
    val isContent = type.isContent
    val accent = type.colors().accent
    // What the step read when the editor opened comes first in the lists.
    val pinned = remember { listOfNotNull(initial?.entryId, initial?.groupId).filter { it.isNotEmpty() }.toSet() }
    val entries = library.ofType(type)
    val selectedEntry = library.entry(entryId)?.takeIf { it.type == type }
    val shown = remember(entries, query) {
        val q = ArabicText.searchKey(query)
        val found = if (q.isEmpty()) entries else entries.filter { ArabicText.searchKey(it.title + " " + it.description + " " + it.text).contains(q) }
        found.sortedByDescending { it.id in pinned }
    }
    val groups = library.groupsOfType(type)
    val selectedGroup = library.group(groupId)?.takeIf { it.type == type }
    val shownGroups = remember(groups, query) {
        val q = ArabicText.searchKey(query)
        val found = if (q.isEmpty()) groups else groups.filter { ArabicText.searchKey(it.title + " " + it.description).contains(q) }
        found.sortedByDescending { it.id in pinned }
    }

    // The list of texts (or groups) shows until one is chosen, then only the one chosen does.
    val chosen = if (groupMode) selectedGroup != null else selectedEntry != null
    val listing = isContent && (changing || !chosen)
    val listSize = if (groupMode) groups.size else entries.size
    fun picked() {
        changing = false
        query = ""
        showError = false
    }

    val dirty = if (initial == null) {
        entryId.isNotEmpty() || groupId.isNotEmpty() || title.isNotBlank() || text.isNotBlank() || note.isNotBlank()
    } else {
        type != initial.type || note.trim() != initial.note || when {
            type == ItemType.NOTICE -> text.trim() != initial.text || kind != initial.kind
            !isContent -> title.trim() != initial.title || text.trim() != initial.text
            groupMode -> groupId != initial.groupId
            else -> entryId != initial.entryId || count != initial.count
        }
    }

    fun save() {
        when {
            isContent && groupMode && selectedGroup == null -> { showError = true; return }
            isContent && !groupMode && selectedEntry == null -> { showError = true; return }
            type == ItemType.NOTICE && text.isBlank() -> { showError = true; return }
            !isContent && title.isBlank() && text.isBlank() -> { showError = true; return }
        }
        val quiet = type == ItemType.HEADING || type == ItemType.NOTICE
        val base = ProgramItem(id = initial?.id ?: newId(), type = type, note = if (quiet) "" else note.trim())
        onSave(
            when {
                type == ItemType.NOTICE -> base.copy(text = text.trim(), kind = kind)
                !isContent -> base.copy(
                    title = title.trim(),
                    text = text.trim(),
                    instructions = if (type == ItemType.TASK) instructions.map { it.trim() }.filter { it.isNotEmpty() } else emptyList(),
                )
                groupMode -> base.copy(groupId = selectedGroup!!.id)
                else -> base.copy(entryId = selectedEntry!!.id, count = count)
            }
        )
    }

    EditorPopup(
        // The kind of step, in the header: a pill that opens the list of kinds.
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(if (initial == null) R.string.new_item else R.string.edit_item),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StepTypeSelector(selected = type, onSelect = { type = it; showError = false })
            }
        },
        onDismiss = onDismiss,
        onSave = ::save,
        dirty = dirty,
        accent = accent,
        saveLabel = saveLabel ?: stringResource(R.string.save),
        // While choosing from a list, its search stays at hand at the bottom.
        bottomBar = if (listing && listSize > 0) {
            {
                CompactTextField(
                    value = query,
                    onValueChange = { query = it },
                    trailingIcon = clearButton(query) { query = "" },
                    placeholder = { Text(stringResource(R.string.search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        } else null,
    ) {
        PopupBody {
            if (isContent) {
                // A single text of the collection, or one of its groups of texts.
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !groupMode,
                        onClick = { groupMode = false; showError = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text(stringResource(R.string.step_single)) }
                    SegmentedButton(
                        selected = groupMode,
                        onClick = { groupMode = true; showError = false },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text(stringResource(R.string.step_group)) }
                }
                if (listing) {
                    PopupLabel(
                        stringResource(if (groupMode) R.string.pick_group else R.string.pick_entry),
                        color = if (showError) MaterialTheme.colorScheme.error else Color.Unspecified,
                    ) {
                        PopupAddButton(stringResource(if (groupMode) R.string.add_group else R.string.add_entry)) {
                            if (groupMode) addingGroup = true else addingEntry = true
                        }
                    }
                }
                if (showError) {
                    Text(
                        stringResource(if (groupMode) R.string.pick_group_error else R.string.pick_entry_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (isContent && !listing) {
                // The text or group chosen, and a way to choose another.
                if (groupMode) {
                    val group = selectedGroup!!
                    ChosenCard(
                        title = group.title,
                        detail = group.items.mapNotNull { library.entry(it.entryId)?.title }.joinToString(stringResource(R.string.list_separator)),
                        badge = stringResource(R.string.group_texts_count, uiDigits(group.items.size)),
                        accent = accent,
                        onChange = { changing = true },
                    )
                } else {
                    val entry = selectedEntry!!
                    ChosenCard(
                        title = entry.title,
                        detail = entry.text,
                        badge = entry.description.ifBlank { null },
                        accent = accent,
                        onChange = { changing = true },
                    )
                    CountEditor(countText = countText, count = count, accent = accent, onCountTextChange = { countText = it })
                }
            } else if (isContent && groupMode) {
                if (groups.isEmpty()) EmptyNote(stringResource(R.string.groups_empty))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shownGroups.forEach { group ->
                        PickRow(
                            title = group.title,
                            detail = listOfNotNull(
                                stringResource(R.string.group_texts_count, uiDigits(group.items.size)),
                                if (group.isChoice) stringResource(R.string.group_required_short, uiDigits(group.required)) else null,
                                group.description.ifBlank { null },
                            ).joinToString(" · "),
                            selected = group.id == groupId,
                            accent = accent,
                            onClick = { groupId = group.id; picked() },
                        )
                    }
                }
            } else if (isContent) {
                if (entries.isEmpty()) EmptyNote(stringResource(R.string.collection_empty))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    shown.forEach { entry ->
                        PickRow(
                            title = entry.title,
                            detail = entry.description.ifBlank { entry.text },
                            selected = entry.id == entryId,
                            accent = accent,
                            onClick = { entryId = entry.id; picked() },
                        )
                    }
                }
            } else if (type == ItemType.NOTICE) {
                // A message: its kind, in its colour and icon, then its words.
                PopupLabel(stringResource(R.string.notice_kind_label))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    NoticeKind.entries.forEach { option ->
                        FilterChip(
                            selected = kind == option,
                            onClick = { kind = option },
                            label = { Text(stringResource(option.label)) },
                            leadingIcon = { Icon(option.icon, null, Modifier.size(18.dp), tint = option.accent()) },
                        )
                    }
                }
                CompactTextField(
                    value = text,
                    onValueChange = { text = it; showError = false },
                    trailingIcon = clearButton(text) { text = ""; showError = false },
                    label = { Text(stringResource(R.string.notice_text)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.item_empty_error)) }
                    } else null,
                    minLines = 2,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                CompactTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    trailingIcon = clearButton(title) { title = ""; showError = false },
                    label = { Text(stringResource(if (type == ItemType.HEADING) R.string.item_title_heading else R.string.item_title)) },
                    singleLine = true,
                    isError = showError,
                    modifier = Modifier.fillMaxWidth(),
                )
                CompactTextField(
                    value = text,
                    onValueChange = { text = it; showError = false },
                    trailingIcon = clearButton(text) { text = ""; showError = false },
                    label = { Text(stringResource(if (type == ItemType.TASK) R.string.item_text_task else R.string.item_text_heading)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.item_empty_error)) }
                    } else null,
                    textStyle = ReadingStyles.task(1f).copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurface),
                    minLines = if (type == ItemType.HEADING) 1 else 2,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == ItemType.TASK) {
                    InstructionsEditor(instructions, onChange = { instructions = it })
                }
            }
            if (type != ItemType.HEADING && type != ItemType.NOTICE && !listing) {
                CompactTextField(
                    value = note,
                    onValueChange = { note = it },
                    trailingIcon = clearButton(note) { note = "" },
                    label = { Text(stringResource(R.string.item_note)) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (addingEntry) {
        EntryEditorDialog(
            type = type,
            initial = null,
            onDismiss = { addingEntry = false },
            onSave = { entry ->
                onSaveEntry(entry)
                entryId = entry.id
                picked()
                addingEntry = false
            },
            saveLabel = saveLabel,
        )
    }
    if (addingGroup) {
        GroupEditorDialog(
            type = type,
            initial = null,
            library = library,
            onDismiss = { addingGroup = false },
            onSave = { group ->
                onSaveGroup(group)
                groupId = group.id
                picked()
                addingGroup = false
            },
            onSaveEntry = onSaveEntry,
            saveLabel = saveLabel,
        )
    }
}

/**
 * The kind of step, as a selector: a pill in the kind's colour — its icon, its name and a ▾ — that
 * opens the list of kinds (each with its icon, the one chosen checked).
 */
@Composable
private fun StepTypeSelector(selected: ItemType, onSelect: (ItemType) -> Unit) {
    val accent = selected.colors().accent
    var open by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.medium,
            color = accent.copy(alpha = 0.12f).compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f)),
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(selected.icon, null, tint = accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(selected.label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                )
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.item_type), tint = accent)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            StepTypes.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.label), fontWeight = if (option == selected) FontWeight.SemiBold else null) },
                    leadingIcon = { Icon(option.icon, null, tint = option.colors().accent) },
                    trailingIcon = if (option == selected) ({ Icon(Icons.Filled.Check, null, tint = option.colors().accent) }) else null,
                    onClick = { open = false; onSelect(option) },
                )
            }
        }
    }
}

/**
 * What a step reads, once chosen: a card in the collection's colours with its title, a line about it
 * ([badge]) and its opening words, and a button to choose another.
 */
@Composable
private fun ChosenCard(title: String, detail: String, badge: String?, accent: Color, onChange: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = accent.copy(alpha = 0.07f).compositeOver(scheme.surfaceContainerLowest),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, null, tint = accent, modifier = Modifier.size(18.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
                badge?.let { Text(it, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (detail.isNotBlank() && detail.trim() != title.trim()) {
                    Text(uiDigits(detail), style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            TextButton(onClick = onChange) { Text(stringResource(R.string.change_selection)) }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A text or group to pick, compact: its title and one line under it; the one picked ringed and checked. */
@Composable
private fun PickRow(title: String, detail: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) accent.copy(alpha = 0.10f) else scheme.surfaceContainerLowest,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else scheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 12.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, color = if (selected) accent else scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                // The opening words, unless they are the title again (a short dhikr titled by its words).
                if (detail.isNotBlank() && detail.trim() != title.trim()) {
                    Text(detail, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Filled.Check, null, tint = accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** How many times a single text is read: − the number +, then the usual counts in a row. */
@Composable
private fun CountEditor(
    countText: String,
    count: Int,
    accent: Color,
    onCountTextChange: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.item_count),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        OutlinedIconStep("−", stringResource(R.string.decrease)) {
            onCountTextChange((count - 1).coerceAtLeast(1).toString())
        }
        CompactTextField(
            value = countText,
            onValueChange = { value -> onCountTextChange(value.filter { it.isDigit() }.take(6)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.width(84.dp).padding(horizontal = 6.dp),
        )
        OutlinedIconStep("+", stringResource(R.string.increase)) {
            onCountTextChange((count + 1).coerceAtMost(ProgramJson.MAX_COUNT).toString())
        }
    }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        CountPresets.forEach { preset ->
            FilterChip(
                selected = count == preset,
                onClick = { onCountTextChange(preset.toString()) },
                label = {
                    Text(
                        text = "×" + uiDigits(preset),
                        color = if (count == preset) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        }
    }
}

@Composable
private fun OutlinedIconStep(symbol: String, description: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbol, fontSize = 20.sp)
    }
}
