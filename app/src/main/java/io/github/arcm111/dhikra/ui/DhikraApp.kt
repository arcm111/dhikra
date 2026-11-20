package io.github.arcm111.dhikra.ui

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
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ArabicText
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.ProgramJson
import io.github.arcm111.dhikra.reminder.ReminderScheduler
import io.github.arcm111.dhikra.ui.editor.EditorScreen
import io.github.arcm111.dhikra.ui.home.HomeActions
import io.github.arcm111.dhikra.ui.home.HomeScreen
import io.github.arcm111.dhikra.ui.library.CollectionScreen
import io.github.arcm111.dhikra.ui.library.SettingsActions
import io.github.arcm111.dhikra.ui.library.SettingsScreen
import io.github.arcm111.dhikra.ui.prayer.PrayerTimesScreen
import io.github.arcm111.dhikra.ui.today.TodayScreen
import io.github.arcm111.dhikra.ui.theme.EmphasizedAccelerate
import io.github.arcm111.dhikra.ui.theme.EmphasizedDecelerate
import io.github.arcm111.dhikra.ui.theme.LocalDhikraPalette
import io.github.arcm111.dhikra.ui.theme.ReadingStyles
import kotlinx.coroutines.launch
import io.github.arcm111.dhikra.data.Favourites
import io.github.arcm111.dhikra.ui.counter.DailyCounterScreen
import io.github.arcm111.dhikra.ui.home.StepReader
import io.github.arcm111.dhikra.ui.prayer.FirstCityDialog

private enum class AppDialog { RestoreDefault, ResetToday, TextSize, NewProgram, DeleteProgram }

/** Screens of the app. Saved as plain strings so the current screen survives recreation. */
private object Route {
    /** The start screen: the day's prayer times and sections. */
    const val TODAY = "today"

    /** The section view: one section's steps, a page per section. */
    const val HOME = "home"
    const val EDITOR = "editor"
    const val SETTINGS = "settings"
    const val PRAYER = "prayer"
    const val COLLECTION = "collection/"

    /** The daily counter of one dhikr (its library entry id follows). */
    const val COUNTER = "counter/"

    /** The favourites in the reader, opened on one of them (its reference follows). */
    const val FAVOURITE = "favourite/"

    /** The editor on one section only. */
    const val EDITOR_SECTION = "editor-section/"

    fun editorSection(sectionId: String) = EDITOR_SECTION + sectionId

    /** Separates the screens of the saved back stack. */
    const val SEPARATOR = "|"

    fun collection(type: ItemType) = COLLECTION + type.key

    /** How deep [route] is, to slide forwards or backwards. */
    fun depth(route: String) = when {
        route == TODAY -> 0
        route == HOME -> 1
        route.startsWith(COLLECTION) -> 3
        else -> 2
    }
}

@Composable
fun DhikraApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    // The screens back leads through, the shown one last ("today|home|settings"): a plain string,
    // so it survives recreation. Each screen goes back to the one it was opened from.
    var backStack by rememberSaveable { mutableStateOf(Route.TODAY) }
    val route = backStack.substringAfterLast(Route.SEPARATOR)
    val navigate = { to: String -> backStack += Route.SEPARATOR + to }
    val back = { backStack = backStack.substringBeforeLast(Route.SEPARATOR, Route.TODAY) }
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

    // Sharing the program followed: from the settings, and from the program tab.
    val shareProgram: () -> Unit = {
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
    }
    val homeActions = HomeActions(
        onSelectSection = viewModel::selectSection,
        onSelectProgram = viewModel::selectProgram,
        onCreateProgram = { dialog = AppDialog.NewProgram },
        onEdit = { navigate(Route.EDITOR) },
        onOpenCollection = { navigate(Route.collection(it)) },
        onOpenSettings = { navigate(Route.SETTINGS) },
        onTextSize = { dialog = AppDialog.TextSize },
        onResetToday = { dialog = AppDialog.ResetToday },
        onIncrement = viewModel::increment,
        onDecrement = viewModel::decrement,
        onRepetitionRead = viewModel::onRepetitionRead,
        onSetComplete = viewModel::setComplete,
        onResetItem = viewModel::resetItem,
        onOpenPrayerTimes = { navigate(Route.PRAYER) },
        onEditSection = { sectionId -> navigate(Route.editorSection(sectionId)) },
        onOpenCounter = { entryId -> navigate(Route.COUNTER + entryId) },
        onAddCounter = viewModel::addCounter,
        onRemoveCounter = viewModel::removeCounter,
        onOpenFavourite = { ref -> navigate(Route.FAVOURITE + ref) },
        onToggleFavourite = viewModel::toggleFavourite,
        onEditProgram = viewModel::editProgram,
        onSaveEntry = viewModel::saveEntry,
        onSaveGroup = viewModel::saveGroup,
        onDeleteEntry = viewModel::deleteEntry,
        onDeleteGroup = viewModel::deleteGroup,
        onSaveGroupCopy = viewModel::saveGroupCopy,
        onSaveSectionEdit = viewModel::saveSectionEdit,
        onDuplicateProgram = { viewModel.duplicateProgram(resources.getString(R.string.section_copy, state.program.name)) },
        onShareProgram = shareProgram,
        onExportProgram = { exportLauncher.launch(viewModel.exportFileName()) },
        onDeleteProgram = if (state.programs.size > 1) ({ dialog = AppDialog.DeleteProgram }) else null,
    )
    val settingsActions = SettingsActions(
        onTextSize = { dialog = AppDialog.TextSize },
        onShare = shareProgram,
        onExport = { exportLauncher.launch(viewModel.exportFileName()) },
        onImport = {
            importLauncher.launch(arrayOf(ProgramJson.MIME_TYPE, "text/plain", "application/octet-stream"))
        },
        onResetToday = { dialog = AppDialog.ResetToday },
        onRestoreDefault = { dialog = AppDialog.RestoreDefault },
    )

    BackHandler(enabled = route != Route.TODAY) { back() }
    // Keeps each screen's scroll positions while the other one is shown.
    val screenStates = rememberSaveableStateHolder()
    // A section was tapped on the day view, or in a reminder: show it in the section view, with
    // the day view to go back to. The section view starts afresh on it, unless it is the one shown.
    LaunchedEffect(state.openSectionRequest) {
        if (state.openSectionRequest == null) return@LaunchedEffect
        if (route != Route.HOME) screenStates.removeState(Route.HOME)
        backStack = Route.TODAY + Route.SEPARATOR + Route.HOME
        viewModel.consumeOpenSection()
    }
    // The first launch: ask for the user's city, which the prayer times and the schedule follow.
    if (state.askCity && !state.loading) {
        FirstCityDialog(
            source = viewModel.prayerSource,
            onPick = viewModel::chooseFirstCity,
            onLater = viewModel::dismissCityQuestion,
        )
    }
    // A widget's counter was tapped: show that counter, with the day view to go back to.
    LaunchedEffect(state.openCounterRequest) {
        val entryId = state.openCounterRequest ?: return@LaunchedEffect
        backStack = Route.TODAY + Route.SEPARATOR + Route.COUNTER + entryId
        viewModel.consumeOpenCounter()
    }
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
                    onBack = back,
                    onEdit = viewModel::editProgram,
                    onSaveEntry = viewModel::saveEntry,
                    onSaveGroup = viewModel::saveGroup,
                    onDelete = if (state.programs.size > 1) ({ dialog = AppDialog.DeleteProgram }) else null,
                    prayerTimes = state.prayerTimes,
                )
                current == Route.SETTINGS -> SettingsScreen(
                    onBack = back,
                    actions = settingsActions,
                    sectionSound = state.sectionSound,
                    onSectionSound = viewModel::setSectionSound,
                    overlayScale = state.overlayScale,
                    onOverlayScale = viewModel::setOverlayScale,
                    overlayFade = state.overlayFade,
                    onOverlayFade = viewModel::setOverlayFade,
                )
                current == Route.PRAYER -> PrayerTimesScreen(
                    timetable = state.prayerTimes,
                    source = viewModel.prayerSource,
                    onSelectCity = viewModel::setPrayerCity,
                    onBack = back,
                    alerts = state.prayerAlerts,
                    onAlertChange = viewModel::setPrayerAlert,
                    settings = state.prayerSettings,
                    onSettingsChange = viewModel::setPrayerSettings,
                )
                current.startsWith(Route.COLLECTION) -> CollectionScreen(
                    type = ItemType.fromKey(current.removePrefix(Route.COLLECTION)),
                    library = state.library,
                    programs = state.programs,
                    onBack = back,
                    onSave = viewModel::saveEntry,
                    onDelete = viewModel::deleteEntry,
                    onSaveGroup = viewModel::saveGroup,
                    onDeleteGroup = viewModel::deleteGroup,
                    onSaveGroupCopy = viewModel::saveGroupCopy,
                    favourites = state.favourites,
                    onToggleFavourite = viewModel::toggleFavourite,
                )
                current == Route.TODAY -> TodayScreen(
                    state = state,
                    actions = homeActions,
                    onOpenSection = viewModel::openSection,
                    onEditProgram = viewModel::editProgram,
                )
                current.startsWith(Route.EDITOR_SECTION) -> EditorScreen(
                    program = state.program,
                    library = state.library,
                    onBack = back,
                    onEdit = viewModel::editProgram,
                    onSaveEntry = viewModel::saveEntry,
                    onSaveGroup = viewModel::saveGroup,
                    sectionId = current.removePrefix(Route.EDITOR_SECTION),
                    prayerTimes = state.prayerTimes,
                )
                current.startsWith(Route.COUNTER) -> {
                    val entry = state.library.entry(current.removePrefix(Route.COUNTER))
                    if (entry == null) {
                        LaunchedEffect(Unit) { back() }
                    } else {
                        DailyCounterScreen(
                            entry = entry,
                            count = state.counterCount(entry.id),
                            textScale = state.textScale,
                            lettersPerSecond = state.counterSpeed(entry.id),
                            onCount = { viewModel.countDhikr(entry.id) },
                            onRepetitionRead = { elapsed -> viewModel.onCounterRepetitionRead(entry.id, entry.text, elapsed) },
                            onUndo = { viewModel.undoDhikr(entry.id) },
                            onReset = { viewModel.resetDhikr(entry.id) },
                            onClose = back,
                        )
                    }
                }
                current.startsWith(Route.FAVOURITE) -> {
                    // The favourites read one after the other, like a section's steps.
                    val title = stringResource(R.string.favourites_title)
                    val section = remember(state.favourites, state.library, title) {
                        Favourites.section(state.favourites, state.library, title)
                    }
                    StepReader(
                        section = section,
                        startId = Favourites.STEP_PREFIX + current.removePrefix(Route.FAVOURITE),
                        state = state,
                        actions = homeActions,
                        onClose = { back() },
                    )
                }
                else -> HomeScreen(
                    state = state,
                    snackbarHostState = snackbarHostState,
                    actions = homeActions,
                    onBack = back,
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
                uiDigits(program.sections.size),
                uiDigits(program.itemCount),
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
                backStack = Route.TODAY
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
