package com.example.dhikra.ui.editor

import com.example.dhikra.ui.components.CompactTextField
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.NoticeKind
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramJson
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.data.SectionAnchor
import com.example.dhikra.data.SectionClock
import com.example.dhikra.data.SectionNotice
import com.example.dhikra.data.SectionZone
import com.example.dhikra.data.newId
import com.example.dhikra.reminder.ReminderScheduler
import com.example.dhikra.ui.components.EditorPopup
import com.example.dhikra.ui.components.EditorPopupTitle
import com.example.dhikra.ui.components.PopupBody
import com.example.dhikra.ui.components.PopupAddButton
import com.example.dhikra.ui.components.PopupLabel
import com.example.dhikra.ui.components.SectionIconChoices
import com.example.dhikra.ui.components.accent
import com.example.dhikra.ui.components.clearButton
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.components.label
import com.example.dhikra.ui.formatClock
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.home.label
import com.example.dhikra.ui.library.EntryEditorDialog
import com.example.dhikra.ui.library.GroupEditorDialog
import com.example.dhikra.ui.prayer.anchorLabel
import com.example.dhikra.ui.prayer.anchorOffsetText
import com.example.dhikra.ui.prayer.label
import com.example.dhikra.ui.theme.ReadingStyles
import com.example.dhikra.ui.uiDigits
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
    // The section's messages, kept across rotation as "kind|text" lines.
    var notices by rememberSaveable(stateSaver = NoticesSaver) { mutableStateOf(initial?.notices.orEmpty()) }
    var showError by remember { mutableStateOf(false) }

    fun edited(): ProgramSection = time.applyTo(
        (initial ?: ProgramSection(title = "")).copy(
            title = title.trim(),
            icon = icon,
            note = note.trim(),
            notices = notices.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() },
        ),
        timetable,
    )

    val dirty = if (initial == null) {
        title.isNotBlank() || note.isNotBlank() || notices.isNotEmpty()
    } else {
        title.trim() != initial.title || icon != initial.icon || time != startTime || note.trim() != initial.note ||
            notices != initial.notices
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
            NoticesEditor(notices = notices, onChange = { notices = it })
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

internal val NoticesSaver = listSaver<List<SectionNotice>, String>(
    save = { list -> list.map { it.kind.key + "|" + it.text } },
    restore = { saved -> saved.map { SectionNotice(NoticeKind.fromKey(it.substringBefore('|')), it.substringAfter('|')) } },
)

/**
 * The section's messages shown above its steps, compact: each one a line in its kind's colour, its
 * kind's icon first (a tap changes the kind), then its text and a remove button.
 */
@Composable
internal fun NoticesEditor(notices: List<SectionNotice>, onChange: (List<SectionNotice>) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PopupLabel(stringResource(R.string.notices_title)) {
            PopupAddButton(stringResource(R.string.notice_add)) { onChange(notices + SectionNotice(NoticeKind.INFO, "")) }
        }
        notices.forEachIndexed { index, notice ->
            NoticeRow(
                notice = notice,
                onChange = { changed -> onChange(notices.toMutableList().also { it[index] = changed }) },
                onRemove = { onChange(notices.toMutableList().also { it.removeAt(index) }) },
            )
        }
    }
}

@Composable
private fun NoticeRow(notice: SectionNotice, onChange: (SectionNotice) -> Unit, onRemove: () -> Unit) {
    val accent = notice.kind.accent()
    var menu by remember { mutableStateOf(false) }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(notice.kind.icon, stringResource(R.string.notice_kind) + ": " + stringResource(notice.kind.label), tint = accent)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    NoticeKind.entries.forEach { kind ->
                        DropdownMenuItem(
                            text = { Text(stringResource(kind.label)) },
                            leadingIcon = { Icon(kind.icon, null, tint = kind.accent()) },
                            trailingIcon = if (kind == notice.kind) ({ Icon(Icons.Filled.Check, null) }) else null,
                            onClick = { menu = false; onChange(notice.copy(kind = kind)) },
                        )
                    }
                }
            }
            BasicTextField(
                value = notice.text,
                onValueChange = { onChange(notice.copy(text = it)) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(accent),
                modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                decorationBox = { field ->
                    Box {
                        if (notice.text.isEmpty()) {
                            Text(
                                stringResource(R.string.notice_text),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        field()
                    }
                },
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Filled.Close, stringResource(R.string.notice_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }
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
private val StepTypes = ItemType.CONTENT + listOf(ItemType.TASK, ItemType.HEADING)

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
    var query by rememberSaveable { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var addingEntry by remember { mutableStateOf(false) }

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

    val dirty = if (initial == null) {
        entryId.isNotEmpty() || groupId.isNotEmpty() || title.isNotBlank() || text.isNotBlank() || note.isNotBlank()
    } else {
        type != initial.type || note.trim() != initial.note || when {
            !isContent -> title.trim() != initial.title || text.trim() != initial.text
            groupMode -> groupId != initial.groupId
            else -> entryId != initial.entryId || count != initial.count
        }
    }

    fun save() {
        when {
            isContent && groupMode && selectedGroup == null -> { showError = true; return }
            isContent && !groupMode && selectedEntry == null -> { showError = true; return }
            !isContent && title.isBlank() && text.isBlank() -> { showError = true; return }
        }
        val base = ProgramItem(id = initial?.id ?: newId(), type = type, note = if (type == ItemType.HEADING) "" else note.trim())
        onSave(
            when {
                !isContent -> base.copy(title = title.trim(), text = text.trim())
                groupMode -> base.copy(groupId = selectedGroup!!.id)
                else -> base.copy(entryId = selectedEntry!!.id, count = count)
            }
        )
    }

    EditorPopup(
        title = { EditorPopupTitle(stringResource(if (initial == null) R.string.new_item else R.string.edit_item)) },
        onDismiss = onDismiss,
        onSave = ::save,
        dirty = dirty,
        accent = accent,
        saveLabel = saveLabel ?: stringResource(R.string.save),
    ) {
        PopupBody {
            // The kind of step: all of them in view (two short rows), the one chosen filled.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepTypes.forEach { option ->
                    FilterChip(
                        selected = type == option,
                        onClick = { type = option; showError = false },
                        label = { Text(stringResource(option.label)) },
                        leadingIcon = { Icon(option.icon, null, Modifier.size(18.dp), tint = option.colors().accent) },
                    )
                }
            }
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
                CountAndSearch(
                    label = stringResource(if (groupMode) R.string.pick_group else R.string.pick_entry),
                    error = showError,
                    addLabel = stringResource(if (groupMode) R.string.add_group else R.string.add_entry),
                    onAdd = { if (groupMode) addingGroup = true else addingEntry = true },
                    searchable = (if (groupMode) groups.size else entries.size) > 5,
                    query = query,
                    onQuery = { query = it },
                )
                if (showError) {
                    Text(
                        stringResource(if (groupMode) R.string.pick_group_error else R.string.pick_entry_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (isContent && groupMode) {
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
                            onClick = { groupId = group.id; showError = false },
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
                            onClick = { entryId = entry.id; showError = false },
                        )
                    }
                }
                CountEditor(countText = countText, count = count, accent = accent, onCountTextChange = { countText = it })
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
            }
            if (type != ItemType.HEADING) {
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
                showError = false
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
                showError = false
                addingGroup = false
            },
            onSaveEntry = onSaveEntry,
            saveLabel = saveLabel,
        )
    }
}

/** Over a list to pick from: its label and an «add» button, then a search field for a long list. */
@Composable
private fun CountAndSearch(
    label: String,
    error: Boolean,
    addLabel: String,
    onAdd: () -> Unit,
    searchable: Boolean,
    query: String,
    onQuery: (String) -> Unit,
) {
    PopupLabel(label, color = if (error) MaterialTheme.colorScheme.error else Color.Unspecified) {
        PopupAddButton(addLabel, onAdd)
    }
    if (searchable) {
        CompactTextField(
            value = query,
            onValueChange = onQuery,
            trailingIcon = clearButton(query) { onQuery("") },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            singleLine = true,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth(),
        )
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
                if (detail.isNotBlank()) {
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
