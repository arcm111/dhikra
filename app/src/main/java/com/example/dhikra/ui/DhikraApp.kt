package com.example.dhikra.ui

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.ItemType
import com.example.dhikra.data.ProgramJson
import com.example.dhikra.reminder.ReminderScheduler
import com.example.dhikra.ui.editor.EditorScreen
import com.example.dhikra.ui.home.HomeActions
import com.example.dhikra.ui.home.HomeScreen
import com.example.dhikra.ui.library.CollectionScreen
import com.example.dhikra.ui.library.LibraryScreen
import com.example.dhikra.ui.library.SettingsActions
import com.example.dhikra.ui.library.SettingsScreen
import com.example.dhikra.ui.prayer.PrayerTimesScreen
import com.example.dhikra.ui.theme.EmphasizedAccelerate
import com.example.dhikra.ui.theme.EmphasizedDecelerate
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.ReadingStyles
import kotlinx.coroutines.launch

private enum class AppDialog { RestoreDefault, ResetToday, TextSize, NewProgram, DeleteProgram }

/** Screens of the app. Saved as plain strings so the current screen survives recreation. */
private object Route {
    const val HOME = "home"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val LIBRARY = "library"
    const val PRAYER = "prayer"
    const val COLLECTION = "collection/"

    fun collection(type: ItemType) = COLLECTION + type.key

    /** Where back leads from [route]. */
    fun parent(route: String) = if (route.startsWith(COLLECTION)) LIBRARY else HOME

    /** How deep [route] is, to slide forwards or backwards. */
    fun depth(route: String) = when {
        route == HOME -> 0
        route.startsWith(COLLECTION) -> 2
        else -> 1
    }
}

@Composable
fun DhikraApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var route by rememberSaveable { mutableStateOf(Route.HOME) }
    var dialog by rememberSaveable { mutableStateOf<AppDialog?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Message -> snackbarHostState.showSnackbar(resources.getString(event.text))
            }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshDay()
        onPauseOrDispose { }
    }

    // Ask for the notification permission once, when the program has reminders.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    val hasReminders = state.program.sections.any { it.reminderMinutes != null }
    LaunchedEffect(hasReminders, state.loading) {
        if (!state.loading && hasReminders && !askedNotifications &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ReminderScheduler.hasNotificationPermission(context)
        ) {
            askedNotifications = true
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ProgramJson.MIME_TYPE)
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(viewModel::loadImport) }

    if (state.loading) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    val homeActions = HomeActions(
        onSelectSection = viewModel::selectSection,
        onSelectProgram = viewModel::selectProgram,
        onCreateProgram = { dialog = AppDialog.NewProgram },
        onEdit = { route = Route.EDITOR },
        onOpenLibrary = { route = Route.LIBRARY },
        onOpenSettings = { route = Route.SETTINGS },
        onTextSize = { dialog = AppDialog.TextSize },
        onResetToday = { dialog = AppDialog.ResetToday },
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onRepetitionRead = viewModel::onRepetitionRead,
        onSetComplete = viewModel::setComplete,
        onResetItem = viewModel::resetItem,
        onOpenPrayerTimes = { route = Route.PRAYER },
    )
    val settingsActions = SettingsActions(
        onTextSize = { dialog = AppDialog.TextSize },
        onShare = {
            scope.launch {
                val uri = viewModel.prepareShareFile() ?: return@launch
                val send = Intent(Intent.ACTION_SEND)
                    .setType(ProgramJson.MIME_TYPE)
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, state.program.name)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                send.clipData = ClipData.newRawUri(state.program.name, uri)
                context.startActivity(
                    Intent.createChooser(send, resources.getString(R.string.action_share))
                )
            }
        },
        onExport = { exportLauncher.launch(viewModel.exportFileName()) },
        onImport = {
            importLauncher.launch(arrayOf(ProgramJson.MIME_TYPE, "text/plain", "application/octet-stream"))
        },
        onResetToday = { dialog = AppDialog.ResetToday },
        onRestoreDefault = { dialog = AppDialog.RestoreDefault },
    )

    BackHandler(enabled = route != Route.HOME) { route = Route.parent(route) }
    // Keeps each screen's scroll positions while the other one is shown.
    val screenStates = rememberSaveableStateHolder()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    AnimatedContent(
        targetState = route,
        transitionSpec = {
            // Material shared-axis X: going forward slides toward the end side (left in RTL).
            val forward = Route.depth(targetState) >= Route.depth(initialState)
            val direction = (if (forward) 1 else -1) * (if (rtl) -1 else 1)
            (slideInHorizontally(tween(420, easing = EmphasizedDecelerate)) { direction * it / 5 } +
                fadeIn(tween(220, delayMillis = 90))) togetherWith
                (slideOutHorizontally(tween(300, easing = EmphasizedAccelerate)) { -direction * it / 5 } +
                    fadeOut(tween(120)))
        },
        label = "screen",
    ) { current ->
        screenStates.SaveableStateProvider(current) {
            when {
                current == Route.EDITOR -> EditorScreen(
                    program = state.program,
                    library = state.library,
                    onBack = { route = Route.HOME },
                    onEdit = viewModel::editProgram,
                    onSaveEntry = viewModel::saveEntry,
                    onSaveGroup = viewModel::saveGroup,
                    onDelete = if (state.programs.size > 1) ({ dialog = AppDialog.DeleteProgram }) else null,
                )
                current == Route.SETTINGS -> SettingsScreen(
                    onBack = { route = Route.HOME },
                    actions = settingsActions,
                )
                current == Route.PRAYER -> PrayerTimesScreen(
                    timetable = state.prayerTimes,
                    source = viewModel.prayerSource,
                    onSelectCity = viewModel::setPrayerCity,
                    onBack = { route = Route.HOME },
                )
                current == Route.LIBRARY -> LibraryScreen(
                    library = state.library,
                    onBack = { route = Route.HOME },
                    onOpenCollection = { route = Route.collection(it) },
                )
                current.startsWith(Route.COLLECTION) -> CollectionScreen(
                    type = ItemType.fromKey(current.removePrefix(Route.COLLECTION)),
                    library = state.library,
                    programs = state.programs,
                    onBack = { route = Route.LIBRARY },
                    onSave = viewModel::saveEntry,
                    onDelete = viewModel::deleteEntry,
                    onSaveGroup = viewModel::saveGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                )
                else -> HomeScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    actions = homeActions,
                )
            }
        }
    }

    state.pendingImport?.let { bundle ->
        val program = bundle.program
        ConfirmDialog(
            title = stringResource(R.string.import_title),
            message = stringResource(
                R.string.import_message,
                program.name.ifBlank { stringResource(R.string.default_program_name) },
                ArabicText.toArabicDigits(program.sections.size),
                ArabicText.toArabicDigits(program.itemCount),
            ),
            confirm = stringResource(R.string.import_confirm),
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::dismissImport,
        )
    }

    when (dialog) {
        AppDialog.RestoreDefault -> ConfirmDialog(
            title = stringResource(R.string.action_restore_default),
            message = stringResource(R.string.restore_message),
            confirm = stringResource(R.string.confirm),
            onConfirm = { viewModel.restoreDefault(); dialog = null },
            onDismiss = { dialog = null },
        )
        AppDialog.ResetToday -> ConfirmDialog(
            title = stringResource(R.string.action_reset_today),
            message = stringResource(R.string.reset_today_message),
            confirm = stringResource(R.string.confirm),
            onConfirm = { viewModel.resetToday(); dialog = null },
            onDismiss = { dialog = null },
        )
        AppDialog.TextSize -> TextSizeDialog(
            initial = state.textScale,
            onConfirm = { viewModel.setTextScale(it); dialog = null },
            onDismiss = { dialog = null },
        )
        AppDialog.NewProgram -> NewProgramDialog(
            onCreate = { name -> viewModel.createProgram(name); dialog = null },
            onDismiss = { dialog = null },
        )
        AppDialog.DeleteProgram -> ConfirmDialog(
            title = stringResource(R.string.delete_program),
            message = stringResource(
                R.string.delete_program_message,
                state.program.name.ifBlank { stringResource(R.string.default_program_name) },
            ),
            confirm = stringResource(R.string.delete),
            onConfirm = {
                viewModel.deleteProgram(state.program.id)
                dialog = null
                route = Route.HOME
            },
            onDismiss = { dialog = null },
        )
        null -> Unit
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun TextSizeDialog(initial: Float, onConfirm: (Float) -> Unit, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_text_size)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.text_size_sample),
                    style = ReadingStyles.dua(scale),
                    color = LocalDhikraPalette.current.dua.ink,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                )
                Slider(value = scale, onValueChange = { scale = it }, valueRange = 0.8f..1.6f, steps = 7)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(scale) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Asks for the name of a new, empty program. */
@Composable
private fun NewProgramDialog(onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val create = { if (name.isNotBlank()) onCreate(name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_program)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.program_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { create() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = {
            TextButton(onClick = create, enabled = name.isNotBlank()) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}
