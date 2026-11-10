package com.example.dhikra.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.data.groupUsages
import com.example.dhikra.data.usages
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.components.ProgramIcon
import com.example.dhikra.ui.components.sectionIcon
import com.example.dhikra.ui.editor.ItemEditorDialog
import com.example.dhikra.ui.library.EntryEditorDialog
import com.example.dhikra.ui.library.GroupEditorDialog

private enum class StepAction { Menu, Edit, Move }

/**
 * What a step held down in the section view offers: editing it — its own text for a task or a
 * heading, else the library text or group it reads, saying how many places share it and offering
 * to save the edits as a new copy for this step only — or moving it to another section. A text
 * inside a group ([item] is one of its members) goes straight to its editor.
 */
@Composable
internal fun StepActions(
    section: ProgramSection,
    item: ProgramItem,
    state: UiState,
    actions: HomeActions,
    onDone: () -> Unit,
) {
    val step = section.items.firstOrNull { it.id == item.id }
    // The group step holding the text, when a text of a group was held down.
    val parent = if (step == null) section.items.firstOrNull { s -> s.isGroup && s.members.any { it.id == item.id } } else null
    var action by remember(item.id) { mutableStateOf(if (step == null) StepAction.Edit else StepAction.Menu) }
    val library = state.library
    val programs = state.programs.ifEmpty { listOf(state.program) }

    when (action) {
        StepAction.Menu -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text(item.title.ifBlank { item.text }, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    ActionRow(Icons.Outlined.Edit, stringResource(R.string.step_actions_edit)) { action = StepAction.Edit }
                    if (state.program.sections.size > 1) {
                        ActionRow(ProgramIcon, stringResource(R.string.move_to_section)) { action = StepAction.Move }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) } },
        )

        StepAction.Move -> AlertDialog(
            onDismissRequest = onDone,
            title = { Text(stringResource(R.string.move_to_section_title, item.title.ifBlank { item.text }), maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                // A long program scrolls within the dialog.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    state.program.sections.filter { it.id != section.id }.forEach { target ->
                        ActionRow(sectionIcon(target.icon), target.title) {
                            actions.onEditProgram { it.moveItemToSection(section.id, item.id, target.id) }
                            onDone()
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = onDone) { Text(stringResource(R.string.cancel)) } },
        )

        StepAction.Edit -> when {
            // A task or a heading: the step's own words, nothing shared.
            step != null && !step.type.isContent -> {
                val stored = state.program.section(section.id)?.items?.firstOrNull { it.id == step.id } ?: step
                ItemEditorDialog(
                    initial = stored,
                    library = library,
                    onSaveEntry = actions.onSaveEntry,
                    onSaveGroup = actions.onSaveGroup,
                    onDismiss = onDone,
                    onSave = { saved ->
                        actions.onEditProgram { it.upsertItem(section.id, saved) }
                        onDone()
                    },
                )
            }
            // A group step: the library group, read by every step that has it.
            step != null && step.isGroup -> {
                val group = library.group(step.groupId) ?: return onDone()
                val uses = programs.groupUsages(group.id)
                GroupEditorDialog(
                    type = group.type,
                    initial = group,
                    library = library,
                    onDismiss = onDone,
                    onSave = { actions.onSaveGroup(it); onDone() },
                    onSaveEntry = actions.onSaveEntry,
                    usages = uses,
                    sharedCount = uses,
                    onSaveAsNew = { copy ->
                        actions.onSaveGroup(copy)
                        actions.onEditProgram { it.updateItem(section.id, step.id) { s -> s.copy(groupId = copy.id) } }
                        onDone()
                    },
                )
            }
            // A library text, on its own or inside a group.
            else -> {
                val entry = library.entry(item.entryId) ?: return onDone()
                EntryEditorDialog(
                    type = entry.type,
                    initial = entry,
                    onDismiss = onDone,
                    onSave = { actions.onSaveEntry(it); onDone() },
                    sharedCount = programs.usages(entry.id) + library.groupsWith(entry.id).size,
                    onSaveAsNew = { copy ->
                        actions.onSaveEntry(copy)
                        val group = parent?.let { library.group(it.groupId) }
                        if (group != null) {
                            // The text is swapped for its copy in the group it was held down in.
                            actions.onSaveGroup(
                                group.copy(items = group.items.map { if (it.entryId == entry.id) it.copy(entryId = copy.id) else it })
                            )
                        } else if (step != null) {
                            actions.onEditProgram { it.updateItem(section.id, step.id) { s -> s.copy(entryId = copy.id) } }
                        }
                        onDone()
                    },
                )
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(vertical = 12.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(14.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
