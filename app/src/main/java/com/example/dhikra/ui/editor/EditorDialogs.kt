package com.example.dhikra.ui.editor

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.Library
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.LibraryGroup
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramJson
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.data.newId
import com.example.dhikra.reminder.ReminderScheduler
import com.example.dhikra.ui.components.SectionIconChoices
import com.example.dhikra.ui.components.icon
import com.example.dhikra.ui.formatTimeArabic
import com.example.dhikra.ui.home.colors
import com.example.dhikra.ui.home.label
import com.example.dhikra.ui.library.EntryCard
import com.example.dhikra.ui.library.EntryEditorDialog
import com.example.dhikra.ui.library.GroupEditorDialog
import com.example.dhikra.ui.library.GroupEntryCard
import com.example.dhikra.ui.theme.ReadingStyles

private val SectionIcons = SectionIconChoices.map { it.first }

// ------------------------------------------------------------------------------ Section

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionEditorDialog(
    initial: ProgramSection?,
    onDismiss: () -> Unit,
    onSave: (ProgramSection) -> Unit,
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var icon by rememberSaveable { mutableStateOf(initial?.icon ?: SectionIcons.first()) }
    var reminder by rememberSaveable { mutableStateOf(initial?.reminderMinutes) }
    var lastReminder by rememberSaveable { mutableIntStateOf(initial?.reminderMinutes ?: (5 * 60)) }
    var note by rememberSaveable { mutableStateOf(initial?.note.orEmpty()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (initial == null) R.string.new_section else R.string.edit_section))
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    label = { Text(stringResource(R.string.section_title)) },
                    isError = showError,
                    supportingText = if (showError) {
                        { Text(stringResource(R.string.section_empty_error)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.section_icon), style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SectionIconChoices.forEach { (emojiKey, imageVector) ->
                        val selected = emojiKey == icon
                        Surface(
                            onClick = { icon = emojiKey },
                            shape = CircleShape,
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.size(44.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(imageVector, null, Modifier.size(24.dp))
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.section_reminder),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = reminder != null,
                        onCheckedChange = { enabled -> reminder = if (enabled) lastReminder else null },
                    )
                }
                reminder?.let { minutes ->
                    OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(formatTimeArabic(minutes), style = MaterialTheme.typography.titleMedium)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !ReminderScheduler.canScheduleExactAlarms(context)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.exact_alarm_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData("package:${context.packageName}".toUri())
                                )
                            }) { Text(stringResource(R.string.allow)) }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.section_note)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    showError = true
                } else {
                    onSave(
                        (initial ?: ProgramSection(title = "")).copy(
                            title = title.trim(),
                            icon = icon,
                            reminderMinutes = reminder,
                            note = note.trim(),
                        )
                    )
                }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )

    if (showTimePicker) {
        TimePickerDialog(
            initialMinutes = reminder ?: lastReminder,
            onDismiss = { showTimePicker = false },
            onConfirm = { minutes ->
                reminder = minutes
                lastReminder = minutes
                showTimePicker = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinutes / 60,
        initialMinute = initialMinutes % 60,
        is24Hour = false,
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
 * Editor of one program step. Quran, hadith, dua and dhikr steps read a single text or a group of
 * texts, picked from the library (a new text or group can be added to the library right here);
 * only tasks and headings have their own text. A single text has a repeat count (a group's texts
 * have their own), and every step can have a note.
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
    val entries = library.ofType(type)
    val selectedEntry = library.entry(entryId)?.takeIf { it.type == type }
    val shown = remember(entries, query) {
        val q = ArabicText.searchKey(query)
        if (q.isEmpty()) entries
        else entries.filter { ArabicText.searchKey(it.title + " " + it.description + " " + it.text).contains(q) }
    }
    val groups = library.groupsOfType(type)
    val selectedGroup = library.group(groupId)?.takeIf { it.type == type }
    val shownGroups = remember(groups, query) {
        val q = ArabicText.searchKey(query)
        if (q.isEmpty()) groups
        else groups.filter { ArabicText.searchKey(it.title + " " + it.description).contains(q) }
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

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.imePadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, stringResource(R.string.close)) }
                    Text(
                        text = stringResource(if (initial == null) R.string.new_item else R.string.edit_item),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = ::save, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.save))
                    }
                }
                HorizontalDivider()
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.item_type), style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StepTypes.forEach { option ->
                            FilterChip(
                                selected = type == option,
                                onClick = { type = option; showError = false },
                                label = { Text(stringResource(option.label)) },
                                leadingIcon = { Icon(option.icon, null, Modifier.size(18.dp)) },
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
                    }
                    if (isContent && groupMode) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.pick_group),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (showError) MaterialTheme.colorScheme.error else Color.Unspecified,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { addingGroup = true }) {
                                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_group))
                            }
                        }
                        if (showError) {
                            Text(
                                stringResource(R.string.pick_group_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (groups.size > 4) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text(stringResource(R.string.search)) },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                singleLine = true,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (groups.isEmpty()) {
                            Text(
                                stringResource(R.string.groups_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        shownGroups.forEach { group ->
                            GroupEntryCard(
                                group = group,
                                library = library,
                                selected = group.id == groupId,
                                onClick = { groupId = group.id; showError = false },
                            )
                        }
                    } else if (isContent) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.pick_entry),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (showError) MaterialTheme.colorScheme.error else Color.Unspecified,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { addingEntry = true }) {
                                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_entry))
                            }
                        }
                        if (showError) {
                            Text(
                                stringResource(R.string.pick_entry_error),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (entries.size > 4) {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = { Text(stringResource(R.string.search)) },
                                leadingIcon = { Icon(Icons.Filled.Search, null) },
                                singleLine = true,
                                shape = CircleShape,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (entries.isEmpty()) {
                            Text(
                                stringResource(R.string.collection_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        shown.forEach { entry ->
                            EntryCard(
                                entry = entry,
                                selected = entry.id == entryId,
                                onClick = { entryId = entry.id; showError = false },
                            )
                        }
                        CountEditor(
                            countText = countText,
                            count = count,
                            accent = type.colors().accent,
                            onCountTextChange = { countText = it },
                        )
                    } else {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it; showError = false },
                            label = {
                                Text(stringResource(if (type == ItemType.HEADING) R.string.item_title_heading else R.string.item_title))
                            },
                            singleLine = true,
                            isError = showError,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it; showError = false },
                            label = {
                                Text(stringResource(if (type == ItemType.TASK) R.string.item_text_task else R.string.item_text_heading))
                            },
                            isError = showError,
                            supportingText = if (showError) {
                                { Text(stringResource(R.string.item_empty_error)) }
                            } else null,
                            textStyle = ReadingStyles.task(1f).copy(
                                textAlign = TextAlign.Start,
                                color = MaterialTheme.colorScheme.onSurface,
                            ),
                            minLines = if (type == ItemType.HEADING) 1 else 3,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (type != ItemType.HEADING) {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text(stringResource(R.string.item_note)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
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
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CountEditor(
    countText: String,
    count: Int,
    accent: Color,
    onCountTextChange: (String) -> Unit,
) {
    Text(stringResource(R.string.item_count), style = MaterialTheme.typography.labelLarge)
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconStep("−", stringResource(R.string.decrease)) {
            onCountTextChange((count - 1).coerceAtLeast(1).toString())
        }
        OutlinedTextField(
            value = countText,
            onValueChange = { value -> onCountTextChange(value.filter { it.isDigit() }.take(6)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center),
            modifier = Modifier.width(96.dp).padding(horizontal = 8.dp),
        )
        OutlinedIconStep("+", stringResource(R.string.increase)) {
            onCountTextChange((count + 1).coerceAtMost(ProgramJson.MAX_COUNT).toString())
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CountPresets.forEach { preset ->
            FilterChip(
                selected = count == preset,
                onClick = { onCountTextChange(preset.toString()) },
                label = {
                    Text(
                        text = "×" + ArabicText.toArabicDigits(preset),
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
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .size(48.dp)
            .semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbol, fontSize = 22.sp)
    }
}
