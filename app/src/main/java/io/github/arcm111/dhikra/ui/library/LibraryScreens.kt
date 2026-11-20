package io.github.arcm111.dhikra.ui.library

import io.github.arcm111.dhikra.ui.formatClock
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Notifications
import io.github.arcm111.dhikra.reminder.InspirationReminders
import io.github.arcm111.dhikra.data.readsGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import io.github.arcm111.dhikra.ui.components.slice
import io.github.arcm111.dhikra.ui.components.panelSlice
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import io.github.arcm111.dhikra.ui.home.label
import io.github.arcm111.dhikra.ui.components.CompactTextField
import io.github.arcm111.dhikra.ui.components.EditorPopup
import io.github.arcm111.dhikra.ui.components.EditorPopupTitle
import io.github.arcm111.dhikra.ui.components.PopupBody
import io.github.arcm111.dhikra.ui.components.PopupNotice

import androidx.compose.foundation.BorderStroke
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.Role
import io.github.arcm111.dhikra.ui.AppLanguage
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.LocationOn
import io.github.arcm111.dhikra.data.ReminderSound
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ArabicText
import io.github.arcm111.dhikra.data.GroupItem
import io.github.arcm111.dhikra.data.ItemType
import io.github.arcm111.dhikra.data.Library
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.data.LibraryGroup
import io.github.arcm111.dhikra.data.Program
import io.github.arcm111.dhikra.data.ProgramJson
import io.github.arcm111.dhikra.data.QuranPassage
import io.github.arcm111.dhikra.data.QuranRef
import io.github.arcm111.dhikra.data.Surah
import io.github.arcm111.dhikra.data.groupUsages
import io.github.arcm111.dhikra.data.reads
import io.github.arcm111.dhikra.data.usages
import io.github.arcm111.dhikra.ui.components.DhikraHeader
import io.github.arcm111.dhikra.ui.components.BeadsIcon
import io.github.arcm111.dhikra.ui.components.MosqueIcon
import io.github.arcm111.dhikra.ui.components.PrayerIcon
import io.github.arcm111.dhikra.ui.components.ReminderSoundDialog
import io.github.arcm111.dhikra.ui.components.reminderSoundLabel
import io.github.arcm111.dhikra.ui.components.HeaderTab
import io.github.arcm111.dhikra.ui.components.Pill
import io.github.arcm111.dhikra.ui.components.QuranIcon
import io.github.arcm111.dhikra.ui.components.StarOrnamentIcon
import io.github.arcm111.dhikra.ui.components.headerAppBarColors
import io.github.arcm111.dhikra.ui.components.icon
import io.github.arcm111.dhikra.ui.home.GroupBadge
import io.github.arcm111.dhikra.ui.home.colors
import io.github.arcm111.dhikra.ui.theme.ReadingStyles
import io.github.arcm111.dhikra.ui.uiDigits
import io.github.arcm111.dhikra.data.Favourites
import io.github.arcm111.dhikra.ui.components.FavouriteIcon
import io.github.arcm111.dhikra.ui.components.FavouriteFilledIcon
import androidx.compose.material.icons.outlined.Info
import io.github.arcm111.dhikra.data.newId
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import io.github.arcm111.dhikra.ui.components.clearButton
import io.github.arcm111.dhikra.ui.components.DialogSystemBarIcons
import androidx.compose.material3.Slider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.height
import io.github.arcm111.dhikra.overlay.OverlayCounterView
import kotlin.math.roundToInt
import androidx.compose.material3.Switch
import androidx.compose.ui.draw.alpha
import io.github.arcm111.dhikra.data.OverlayFade

/** Name of the library collection holding entries of this type. */
val ItemType.collectionName: Int
    get() = when (this) {
        ItemType.QURAN -> R.string.collection_quran
        ItemType.HADITH -> R.string.collection_hadith
        ItemType.DUA -> R.string.collection_dua
        else -> R.string.collection_dhikr
    }

/** What the settings screen can ask for. */
class SettingsActions(
    val onTextSize: () -> Unit,
    val onShare: () -> Unit,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onResetToday: () -> Unit,
    val onRestoreDefault: () -> Unit,
)

/** Screen top bar inside the branded header, with a back arrow and optional [tabs] below it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScreenHeader(title: String, onBack: () -> Unit, tabs: (@Composable () -> Unit)? = null) {
    DhikraHeader(bottomPadding = if (tabs == null) 4.dp else 14.dp) {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            expandedHeight = 56.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = headerAppBarColors(),
        )
        tabs?.invoke()
    }
}

/**
 * The library's four collections, each with how many texts and groups it holds: the library tab of
 * the day view (and [LibraryScreen]).
 */
fun LazyListScope.libraryCollections(
    library: Library,
    onOpenCollection: (ItemType) -> Unit,
    modifier: Modifier = Modifier,
    /** The line saying what the library is for (left out where a heading already says it). */
    showHint: Boolean = true,
) {
    if (showHint) item {
        Text(
            text = stringResource(R.string.library_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        )
    }
    item {
        SettingsGroup(modifier) {
            ItemType.CONTENT.forEachIndexed { index, type ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val colors = type.colors()
                SettingsRow(
                    title = stringResource(type.collectionName),
                    subtitle = stringResource(
                        R.string.collection_counts,
                        uiDigits(library.ofType(type).size),
                        uiDigits(library.groupsOfType(type).size),
                    ),
                    leading = {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(
                                colors.accent.copy(alpha = 0.13f)
                            ),
                            contentAlignment = Alignment.Center,
                        ) { Icon(type.icon, null, tint = colors.accent, modifier = Modifier.size(22.dp)) }
                    },
                    chevron = true,
                    onClick = { onOpenCollection(type) },
                )
            }
        }
    }
}

/** The library on a screen of its own: its four collections and how many texts each holds. */
@Composable
fun LibraryScreen(library: Library, onBack: () -> Unit, onOpenCollection: (ItemType) -> Unit) {
    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.library), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            libraryCollections(library, onOpenCollection)
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    actions: SettingsActions,
    sectionSound: ReminderSound = ReminderSound.Default,
    onSectionSound: (ReminderSound) -> Unit = {},
    /** The floating counter's size, a share of its default. */
    overlayScale: Float = 1f,
    onOverlayScale: (Float) -> Unit = {},
    /** How the floating counter fades while left alone. */
    overlayFade: OverlayFade = OverlayFade(),
    onOverlayFade: (OverlayFade) -> Unit = {},
) {
    var choosingOverlayFade by rememberSaveable { mutableStateOf(false) }
    if (choosingOverlayFade) {
        OverlayFadeDialog(
            initial = overlayFade,
            onConfirm = { onOverlayFade(it); choosingOverlayFade = false },
            onDismiss = { choosingOverlayFade = false },
        )
    }
    var choosingSound by rememberSaveable { mutableStateOf(false) }
    var choosingOverlaySize by rememberSaveable { mutableStateOf(false) }
    if (choosingOverlaySize) {
        OverlaySizeDialog(
            initial = overlayScale,
            onConfirm = { onOverlayScale(it); choosingOverlaySize = false },
            onDismiss = { choosingOverlaySize = false },
        )
    }
    var choosingLanguage by rememberSaveable { mutableStateOf(false) }
    val activity = LocalActivity.current
    if (choosingLanguage) {
        LanguageDialog(
            current = AppLanguage.current,
            onSelect = { language ->
                choosingLanguage = false
                if (language != AppLanguage.current && activity != null) {
                    // The screen is rebuilt in the new language (and direction).
                    AppLanguage.save(activity, language)
                    activity.recreate()
                }
            },
            onDismiss = { choosingLanguage = false },
        )
    }
    if (choosingSound) {
        ReminderSoundDialog(
            current = sectionSound,
            allowAzan = false,
            title = stringResource(R.string.section_sound),
            onSelect = { onSectionSound(it); choosingSound = false },
            onDismiss = { choosingSound = false },
        )
    }
    Scaffold(
        topBar = { ScreenHeader(stringResource(R.string.settings), onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { GroupTitle(R.string.settings_display) }
            item {
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.action_text_size),
                        leading = { RowIcon { Text(stringResource(R.string.text_size_glyph), style = MaterialTheme.typography.titleMedium) } },
                        onClick = actions.onTextSize,
                    )
                    SettingsRow(
                        title = stringResource(R.string.language_title),
                        leading = { RowIcon { Text(stringResource(R.string.language_glyph), style = MaterialTheme.typography.titleMedium) } },
                        subtitle = stringResource(AppLanguage.current.label),
                        onClick = { choosingLanguage = true },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        title = stringResource(R.string.overlay_size),
                        leading = { RowIcon { Icon(painterResource(R.drawable.ic_pop_out), null, Modifier.size(20.dp)) } },
                        subtitle = stringResource(R.string.progress_percent, uiDigits((overlayScale * 100).roundToInt())),
                        onClick = { choosingOverlaySize = true },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        title = stringResource(R.string.overlay_fade),
                        leading = { RowIcon { Icon(painterResource(R.drawable.ic_pop_out), null, Modifier.size(20.dp).alpha(0.5f)) } },
                        subtitle = if (overlayFade.enabled) {
                            stringResource(
                                R.string.overlay_fade_summary,
                                stringResource(R.string.progress_percent, uiDigits((overlayFade.opacity * 100).roundToInt())),
                                uiDigits(overlayFade.delaySeconds),
                            )
                        } else {
                            stringResource(R.string.overlay_fade_off)
                        },
                        onClick = { choosingOverlayFade = true },
                    )
                }
            }
            item { GroupTitle(R.string.settings_reminders) }
            item {
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.section_sound),
                        subtitle = reminderSoundLabel(sectionSound),
                        leading = { RowIcon(Icons.Outlined.Notifications) },
                        onClick = { choosingSound = true },
                        chevron = true,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    InspirationSettings()
                }
            }
            item { GroupTitle(R.string.settings_data) }
            item {
                SettingsGroup {
                    SettingsRow(stringResource(R.string.action_share), leading = { RowIcon(Icons.Outlined.Share) }, onClick = actions.onShare)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        stringResource(R.string.action_export),
                        leading = { RowIcon { Icon(painterResource(R.drawable.ic_upload), null) } },
                        onClick = actions.onExport,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        stringResource(R.string.action_import),
                        leading = { RowIcon { Icon(painterResource(R.drawable.ic_download), null) } },
                        onClick = actions.onImport,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(stringResource(R.string.action_reset_today), leading = { RowIcon(Icons.Outlined.Refresh) }, onClick = actions.onResetToday)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(stringResource(R.string.action_restore_default), leading = { RowIcon(StarOrnamentIcon) }, onClick = actions.onRestoreDefault)
                }
            }
            item { GroupTitle(R.string.settings_about) }
            item {
                // Tanzil's terms: the source of the Quran text is named, with a link to tanzil.net.
                val uriHandler = LocalUriHandler.current
                val url = stringResource(R.string.quran_source_url)
                SettingsGroup {
                    SettingsRow(
                        title = stringResource(R.string.quran_source),
                        subtitle = stringResource(R.string.quran_source_details),
                        leading = { RowIcon(QuranIcon) },
                        onClick = { uriHandler.openUri(url) },
                    )
                    // The MIT licence of the morning and evening adhkar texts asks for this notice.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val adhkarUrl = stringResource(R.string.adhkar_source_url)
                    SettingsRow(
                        title = stringResource(R.string.adhkar_source),
                        subtitle = stringResource(R.string.adhkar_source_details),
                        leading = { RowIcon(BeadsIcon) },
                        onClick = { uriHandler.openUri(adhkarUrl) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val azanUrl = stringResource(R.string.azan_source_url)
                    SettingsRow(
                        title = stringResource(R.string.azan_source),
                        subtitle = stringResource(R.string.azan_source_details),
                        leading = { RowIcon(MosqueIcon) },
                        onClick = { uriHandler.openUri(azanUrl) },
                    )
                    // GeoNames' licence (CC BY 4.0) asks for this credit.
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val geonamesUrl = stringResource(R.string.geonames_source_url)
                    SettingsRow(
                        title = stringResource(R.string.geonames_source),
                        subtitle = stringResource(R.string.geonames_source_details),
                        leading = { RowIcon(Icons.Outlined.LocationOn) },
                        onClick = { uriHandler.openUri(geonamesUrl) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val adhanUrl = stringResource(R.string.adhan_source_url)
                    SettingsRow(
                        title = stringResource(R.string.adhan_source),
                        subtitle = stringResource(R.string.adhan_source_details),
                        leading = { RowIcon(PrayerIcon) },
                        onClick = { uriHandler.openUri(adhanUrl) },
                    )
                }
            }
        }
    }
}

/**
 * The floating counter's size: the bubble itself, drawn as it floats, and a slider from 60 % to
 * 160 % of its default. A bubble shown over the apps takes the new size once saved.
 */
@Composable
private fun OverlaySizeDialog(initial: Float, onConfirm: (Float) -> Unit, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.overlay_size)) },
        text = {
            Column {
                Box(
                    // As tall as the largest bubble, so the dialog keeps its size while sliding.
                    Modifier.fillMaxWidth().height(176.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AndroidView(
                        factory = { context ->
                            OverlayCounterView(context, onTap = {}, onLongPress = {}, onDrag = { _, _ -> }, onDragEnd = {})
                                .apply { count = 33 }
                        },
                        update = { it.scale = scale },
                    )
                }
                Text(
                    text = stringResource(R.string.progress_percent, uiDigits((scale * 100).roundToInt())),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Slider(
                    value = scale,
                    onValueChange = { scale = it },
                    valueRange = OverlayCounterView.MIN_SCALE..OverlayCounterView.MAX_SCALE,
                    steps = 9,
                )
                Text(
                    text = stringResource(R.string.overlay_size_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(scale) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * How the floating counter fades while left alone: on or off, how faint it turns, and after how
 * many seconds without a touch. A touch always makes it solid again.
 */
@Composable
private fun OverlayFadeDialog(initial: OverlayFade, onConfirm: (OverlayFade) -> Unit, onDismiss: () -> Unit) {
    var enabled by remember { mutableStateOf(initial.enabled) }
    var opacity by remember { mutableFloatStateOf(initial.opacity) }
    var delay by remember { mutableFloatStateOf(initial.delaySeconds.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.overlay_fade)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.overlay_fade_switch),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(
                        R.string.overlay_fade_opacity,
                        stringResource(R.string.progress_percent, uiDigits((opacity * 100).roundToInt())),
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = opacity,
                    onValueChange = { opacity = it },
                    valueRange = OverlayFade.MIN_OPACITY..OverlayFade.MAX_OPACITY,
                    steps = 6,
                    enabled = enabled,
                )
                Text(
                    text = stringResource(R.string.overlay_fade_delay, uiDigits(delay.roundToInt())),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = delay,
                    onValueChange = { delay = it },
                    valueRange = OverlayFade.MIN_DELAY_SECONDS.toFloat()..OverlayFade.MAX_DELAY_SECONDS.toFloat(),
                    steps = OverlayFade.MAX_DELAY_SECONDS - OverlayFade.MIN_DELAY_SECONDS - 1,
                    enabled = enabled,
                )
                Text(
                    text = stringResource(R.string.overlay_fade_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(OverlayFade(enabled, (opacity * 10).roundToInt() / 10f, delay.roundToInt()))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun GroupTitle(title: Int) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.fillMaxWidth(),
    ) { Column { content() } }
}

@Composable
private fun RowIcon(icon: ImageVector) = RowIcon { Icon(icon, null) }

@Composable
private fun RowIcon(content: @Composable () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides MaterialTheme.colorScheme.onSecondaryContainer,
            content = content,
        )
    }
}

/** The interface language: the app's own interface only, the texts stay as they are written. */
@Composable
private fun LanguageDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = language == current, role = Role.RadioButton) { onSelect(language) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = language == current, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(stringResource(language.label), style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Text(
                    text = stringResource(R.string.language_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/**
 * The random reminders' settings: on or off; how many a day and between which hours; how many
 * texts are picked (with the bell in the library); and a reminder shown at once, to see one.
 */
@Composable
private fun InspirationSettings() {
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var settings by remember { mutableStateOf(InspirationReminders.settings(context)) }
    val picked = remember { InspirationReminders.ids(context).size }
    val save = { changed: InspirationReminders.Settings ->
        settings = changed
        InspirationReminders.saveSettings(context, changed)
    }
    val scheme = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RowIcon { Icon(Icons.Outlined.Notifications, null) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.inspiration_settings), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.inspiration_channel_desc), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
            }
            Switch(checked = settings.enabled, onCheckedChange = { save(settings.copy(enabled = it)) })
        }
        if (settings.enabled) {
            StepperRow(
                label = stringResource(R.string.inspiration_per_day),
                value = uiDigits(settings.perDay),
                onMinus = { save(settings.copy(perDay = settings.perDay - 1)) }.takeIf { settings.perDay > 1 },
                onPlus = { save(settings.copy(perDay = settings.perDay + 1)) }.takeIf { settings.perDay < InspirationReminders.MAX_PER_DAY },
            )
            StepperRow(
                label = stringResource(R.string.inspiration_from),
                value = formatClock(settings.fromMinutes),
                onMinus = { save(settings.copy(fromMinutes = settings.fromMinutes - 60)) }.takeIf { settings.fromMinutes >= 60 },
                onPlus = { save(settings.copy(fromMinutes = settings.fromMinutes + 60)) }.takeIf { settings.fromMinutes + 60 < settings.toMinutes },
            )
            StepperRow(
                label = stringResource(R.string.inspiration_to),
                value = formatClock(settings.toMinutes % (24 * 60)),
                onMinus = { save(settings.copy(toMinutes = settings.toMinutes - 60)) }.takeIf { settings.toMinutes - 60 > settings.fromMinutes },
                onPlus = { save(settings.copy(toMinutes = settings.toMinutes + 60)) }.takeIf { settings.toMinutes + 60 <= 24 * 60 },
            )
            Text(
                stringResource(R.string.inspiration_picked, uiDigits(picked)) + " — " + stringResource(R.string.inspiration_pick_hint),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
            )
            TextButton(
                enabled = picked > 0,
                onClick = {
                    scope.launch {
                        val entry = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            InspirationReminders.pick(context, io.github.arcm111.dhikra.data.ProgramRepository(context).load().library.entries)
                        }
                        entry?.let { InspirationReminders.show(context, it) }
                    }
                },
            ) { Text(stringResource(R.string.inspiration_preview)) }
        }
    }
}

/** A setting's value between − and + (each null when it cannot go further that way). */
@Composable
private fun StepperRow(label: String, value: String, onMinus: (() -> Unit)?, onPlus: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(start = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = { onMinus?.invoke() }, enabled = onMinus != null) { Text("−", style = MaterialTheme.typography.titleLarge) }
        Text(value, style = MaterialTheme.typography.titleSmall, modifier = Modifier.widthIn(min = 48.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onPlus?.invoke() }, enabled = onPlus != null) { Text("+", style = MaterialTheme.typography.titleLarge) }
    }
}

private val AppLanguage.label: Int
    get() = when (this) {
        AppLanguage.AR -> R.string.language_arabic
        AppLanguage.EN -> R.string.language_english
    }

@Composable
private fun SettingsRow(
    title: String,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    subtitle: String? = null,
    chevron: Boolean = false,
) {
    Surface(onClick = onClick, color = androidx.compose.ui.graphics.Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (chevron) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------------------ Collection

/** Index of the tab showing the groups of a collection (the first one shows its single texts). */
const val GROUPS_TAB = 1

/**
 * One library collection, in two tabs: its single texts, and its groups of texts (read one after
 * the other). Both are searchable, with add / edit / delete.
 */
@Composable
fun CollectionScreen(
    type: ItemType,
    library: Library,
    /** All the user's programs: a text is "in the program" when one of them reads it. */
    programs: List<Program>,
    onBack: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    onDelete: (String) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
    onDeleteGroup: (String) -> Unit,
    initialTab: Int = 0,
    /** Saves a copy of a group (the copy, the original's id), placed right after the original. */
    onSaveGroupCopy: (LibraryGroup, String) -> Unit = { copy, _ -> onSaveGroup(copy) },
    /** The favourites ([Favourites] references), and marking or unmarking one. */
    favourites: List<String> = emptyList(),
    onToggleFavourite: (String) -> Unit = {},
) {
    var tab by rememberSaveable { mutableIntStateOf(initialTab) }
    var query by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    var editing by remember { mutableStateOf<LibraryEntry?>(null) }
    var editingGroup by remember { mutableStateOf<LibraryGroup?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val groupsTab = tab == GROUPS_TAB
    val entries = library.ofType(type)
    val groups = library.groupsOfType(type)
    val key = ArabicText.searchKey(query)
    val shownEntries = remember(entries, key) { if (key.isEmpty()) entries else entries.filter { it.matches(key) } }
    val shownGroups = remember(groups, library, key) {
        if (key.isEmpty()) groups
        else groups.filter { group ->
            ArabicText.searchKey(group.title + " " + group.description).contains(key) ||
                group.items.any { library.entry(it.entryId)?.matches(key) == true }
        }
    }

    Scaffold(
        topBar = {
            ScreenHeader(stringResource(type.collectionName), onBack) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CollectionTab(R.string.library_tab_singles, entries.size, selected = !groupsTab) { tab = 0 }
                    CollectionTab(R.string.library_tab_groups, groups.size, selected = groupsTab) { tab = GROUPS_TAB }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(if (groupsTab) R.string.add_group else R.string.add_entry)) },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (type == ItemType.QURAN) {
                item(key = "source") { QuranSourceNote() }
            }
            if (entries.isNotEmpty() || groups.isNotEmpty()) {
                item(key = "search") { SearchField(query, onQueryChange = { query = it }) }
            }
            if (groupsTab) {
                if (shownGroups.isEmpty()) {
                    item(key = "empty") { EmptyText(if (groups.isEmpty()) R.string.groups_empty else R.string.no_results) }
                }
                items(shownGroups, key = { "group:" + it.id }) { group ->
                    GroupEntryCard(
                        group = group,
                        library = library,
                        inProgram = programs.readsGroup(group.id, library),
                        onClick = { editingGroup = group },
                        favourite = Favourites.group(group.id) in favourites,
                        onToggleFavourite = { onToggleFavourite(Favourites.group(group.id)) },
                        onDuplicate = {
                            // The copy is saved at once, right after the original, and opened to rename it.
                            library.duplicateGroup(group.id) { resources.getString(R.string.section_copy, it) }?.let { (_, copy) ->
                                onSaveGroupCopy(copy, group.id)
                                editingGroup = copy
                            }
                        },
                        modifier = Modifier.animateItem(),
                    )
                }
            } else {
                if (shownEntries.isEmpty()) {
                    item(key = "empty") { EmptyText(if (entries.isEmpty()) R.string.collection_empty else R.string.no_results) }
                }
                items(shownEntries, key = { "entry:" + it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        inProgram = programs.reads(entry.id, library),
                        onClick = { editing = entry },
                        favourite = Favourites.entry(entry.id) in favourites,
                        onToggleFavourite = { onToggleFavourite(Favourites.entry(entry.id)) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (adding) {
        if (groupsTab) {
            GroupEditorDialog(
                type = type,
                initial = null,
                library = library,
                onDismiss = { adding = false },
                onSave = { onSaveGroup(it); adding = false },
                onSaveEntry = onSave,
            )
        } else {
            EntryEditorDialog(
                type = type,
                initial = null,
                onDismiss = { adding = false },
                onSave = { onSave(it); adding = false },
            )
        }
    }
    editing?.let { entry ->
        EntryEditorDialog(
            type = type,
            initial = entry,
            usages = programs.usages(entry.id),
            groups = library.groupsWith(entry.id).size,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(entry.id); editing = null },
        )
    }
    editingGroup?.let { group ->
        GroupEditorDialog(
            type = type,
            initial = group,
            library = library,
            usages = programs.groupUsages(group.id),
            onDismiss = { editingGroup = null },
            onSave = { onSaveGroup(it); editingGroup = null },
            onSaveEntry = onSave,
            onDelete = { onDeleteGroup(group.id); editingGroup = null },
        )
    }
}

/** A tab of a collection: its name and how many items it holds. */
@Composable
private fun CollectionTab(label: Int, count: Int, selected: Boolean, onClick: () -> Unit) {
    HeaderTab(selected = selected, onClick = onClick) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        Spacer(Modifier.width(8.dp))
        Text(uiDigits(count), style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    CompactTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, stringResource(R.string.close)) } }
        } else null,
        singleLine = true,
        shape = CircleShape,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun EmptyText(text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(32.dp),
    )
}

/** The entry's text as previewed in the lists: a Quran text's ayah numbers in Arabic digits. */
private val LibraryEntry.previewText: String
    get() = if (type == ItemType.QURAN) uiDigits(text) else text

/** Whether the entry's title, description or text contains the search [key] (see [ArabicText.searchKey]). */
private fun LibraryEntry.matches(key: String): Boolean =
    ArabicText.searchKey("$title $description $text").contains(key)

/** A library text, tinted with its collection's colors (the Quran on its dark green). */
@Composable
fun EntryCard(
    entry: LibraryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inProgram: Boolean = false,
    selected: Boolean = false,
    /** Adds the favourite heart: filled when [favourite], tapping it marks or unmarks the text. */
    onToggleFavourite: (() -> Unit)? = null,
    favourite: Boolean = false,
) {
    val colors = entry.type.colors()
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = colors.container,
        border = when {
            selected -> androidx.compose.foundation.BorderStroke(2.dp, colors.accent)
            colors.border.alpha > 0f -> androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            else -> null
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (entry.type == ItemType.QURAN) colors.accent else colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (inProgram) {
                    io.github.arcm111.dhikra.ui.components.Pill(stringResource(R.string.entry_in_program), colors.accent)
                }
                onToggleFavourite?.let { FavouriteButton(favourite, colors.accent, it) }
            }
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.ink.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entry.previewText,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.ink.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

private const val BASMALA = "بِسۡمِ ٱللَّهِ ٱلرَّحۡمَٰنِ ٱلرَّحِيمِ"
private const val ISTIADHA = "أَعُوذُ بِاللَّهِ مِنَ الشَّيْطَانِ الرَّجِيمِ"

/**
 * Full-screen editor of a library text: title, optional description, optional opening (Quran
 * and dua) and the text itself. A Quran text is not typed in: its surah and ayat are picked, and
 * its text read from the bundled Quran text. [onDelete] adds a delete action (with confirmation,
 * which tells how many program steps and [groups] read the text).
 */
@Composable
fun EntryEditorDialog(
    type: ItemType,
    initial: LibraryEntry?,
    onDismiss: () -> Unit,
    onSave: (LibraryEntry) -> Unit,
    usages: Int = 0,
    groups: Int = 0,
    onDelete: (() -> Unit)? = null,
    /**
     * How many places read the text (program steps and groups): when more than one, the editor says
     * so and offers [onSaveAsNew] — the edits saved as a new text, under a new name, for one step.
     */
    sharedCount: Int = 0,
    onSaveAsNew: ((LibraryEntry) -> Unit)? = null,
    /** Something to know shown at the top (e.g. that other steps read the text too). */
    notice: String? = null,
    /** The save button's label, when not «حفظ» (e.g. «تم» back in the section being edited). */
    saveLabel: String? = null,
) {
    var savingAsNew by remember { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var opening by rememberSaveable { mutableStateOf(initial?.opening.orEmpty()) }
    var text by rememberSaveable { mutableStateOf(initial?.text.orEmpty()) }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val hasOpening = type == ItemType.QURAN || type == ItemType.DUA

    // A Quran text: the surah and the two ayah fields as typed, and the passage they name.
    val isQuran = type == ItemType.QURAN
    val resources = LocalResources.current
    val quranLookup = LocalQuranLookup.current
    val surahs by produceState(emptyList<Surah>(), quranLookup, isQuran) {
        if (isQuran) value = quranLookup?.surahs().orEmpty()
    }
    var sura by rememberSaveable { mutableIntStateOf(initial?.quran?.sura ?: 0) }
    var fromAya by rememberSaveable { mutableStateOf(initial?.quran?.from?.let { uiDigits(it) }.orEmpty()) }
    var toAya by rememberSaveable { mutableStateOf(initial?.quran?.to?.let { uiDigits(it) }.orEmpty()) }
    val ref = if (isQuran) quranRefOf(sura, fromAya, toAya) else null
    var loaded by remember { mutableStateOf<Pair<QuranRef, QuranPassage?>?>(null) }
    LaunchedEffect(ref, quranLookup) {
        if (ref != null && loaded?.first != ref) loaded = ref to quranLookup?.passage(ref)
    }
    val passage = loaded?.takeIf { it.first == ref }?.second
    // A Quran text typed in before the Quran text was bundled, kept until a passage is picked.
    val legacyText = isQuran && initial != null && initial.quran == null && sura == 0 && initial.text.isNotBlank()

    // The title and description last filled in from the passage: they follow it until edited.
    var autoTitle by rememberSaveable { mutableStateOf("") }
    var autoDescription by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(surahs) {
        val initialRef = initial?.quran ?: return@LaunchedEffect
        if (autoTitle.isEmpty()) surahs.firstOrNull { it.number == initialRef.sura }?.let { autoTitle = quranTitle(resources, it) }
        if (autoDescription.isEmpty()) autoDescription = quranRangeDescription(resources, initialRef)
    }

    fun pick(newSura: Int, newFrom: String, newTo: String) {
        if (newSura != sura) {
            surahs.firstOrNull { it.number == newSura }?.let { surah ->
                val auto = quranTitle(resources, surah)
                if (title.isBlank() || title == autoTitle) title = auto
                autoTitle = auto
            }
        }
        quranRefOf(newSura, newFrom, newTo)?.let { newRef ->
            val auto = quranRangeDescription(resources, newRef)
            if (description.isBlank() || description == autoDescription) description = auto
            autoDescription = auto
        }
        sura = newSura
        fromAya = newFrom
        toAya = newTo
    }

    /** The text as edited, or null (with the errors shown) while something is missing. */
    fun edited(): LibraryEntry? {
        val hasText = if (isQuran) passage != null || legacyText else text.isNotBlank()
        if (title.isBlank() || !hasText) {
            showErrors = true
            return null
        }
        return (initial ?: LibraryEntry(type = type, title = "", text = "")).copy(
            title = title.trim(),
            description = description.trim(),
            opening = if (hasOpening) opening.trim() else "",
            text = passage?.text ?: text.trim(),
            quran = passage?.ref,
        )
    }

    fun save() {
        edited()?.let(onSave)
    }

    if (savingAsNew && onSaveAsNew != null) {
        SaveAsNewDialog(
            initialName = stringResource(R.string.section_copy, title.trim()),
            onDismiss = { savingAsNew = false },
            onSave = { name ->
                savingAsNew = false
                edited()?.let { onSaveAsNew(it.copy(id = newId(), title = name)) }
            },
        )
    }

    val dirty = if (initial == null) {
        title.isNotBlank() || description.isNotBlank() || opening.isNotBlank() || text.isNotBlank() || sura != 0
    } else {
        title.trim() != initial.title || description.trim() != initial.description || opening.trim() != initial.opening ||
            (!isQuran && text.trim() != initial.text) || (isQuran && sura != 0 && ref != initial.quran)
    }
    EditorPopup(
        title = {
            EditorPopupTitle(
                title = stringResource(if (initial == null) R.string.new_entry else R.string.edit_entry),
                subtitle = stringResource(type.collectionName),
                subtitleColor = type.colors().accent,
            )
        },
        onDismiss = onDismiss,
        onSave = ::save,
        dirty = dirty,
        accent = type.colors().accent,
        saveLabel = saveLabel ?: stringResource(R.string.save),
        headerActions = {
            if (onDelete != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
                PopupBody {
                    notice?.let { PopupNotice(it) }
                    if (onSaveAsNew != null && sharedCount > 1) {
                        SharedNotice(
                            text = stringResource(R.string.shared_text_notice, uiDigits(sharedCount)),
                            onSaveAsNew = { if (edited() != null) savingAsNew = true },
                        )
                    }
                    // A Quran text starts with its ayat: they fill in the title and description below.
                    if (isQuran) {
                        QuranPassagePicker(
                            surahs = surahs,
                            sura = sura,
                            from = fromAya,
                            to = toAya,
                            onPick = ::pick,
                            passage = passage,
                            loading = ref != null && loaded?.first != ref,
                            showErrors = showErrors && !legacyText,
                        )
                    }
                    CompactTextField(
                        value = title,
                        onValueChange = { title = it },
                        trailingIcon = clearButton(title) { title = "" },
                        label = { Text(stringResource(R.string.entry_title)) },
                        isError = showErrors && title.isBlank(),
                        supportingText = if (showErrors && title.isBlank()) {
                            { Text(stringResource(R.string.entry_title_error)) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    CompactTextField(
                        value = description,
                        onValueChange = { description = it },
                        trailingIcon = clearButton(description) { description = "" },
                        label = { Text(stringResource(R.string.entry_description)) },
                        placeholder = { Text(stringResource(R.string.entry_description_hint)) },
                        minLines = 1,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (hasOpening) {
                        CompactTextField(
                            value = opening,
                            onValueChange = { opening = it },
                            trailingIcon = clearButton(opening) { opening = "" },
                            label = { Text(stringResource(R.string.item_opening)) },
                            supportingText = if (opening.isBlank() && ref?.opensWithBasmala == true) {
                                { Text(stringResource(R.string.quran_basmala_auto)) }
                            } else null,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = opening == BASMALA,
                                onClick = { opening = if (opening == BASMALA) "" else BASMALA },
                                label = { Text(stringResource(R.string.opening_basmala)) },
                            )
                            FilterChip(
                                selected = opening == ISTIADHA,
                                onClick = { opening = if (opening == ISTIADHA) "" else ISTIADHA },
                                label = { Text(stringResource(R.string.opening_istiadha)) },
                            )
                        }
                    }
                    if (!isQuran || legacyText) {
                        CompactTextField(
                            value = text,
                            onValueChange = { text = it },
                            trailingIcon = clearButton(text) { text = "" },
                            label = { Text(stringResource(R.string.entry_text)) },
                            readOnly = legacyText,
                            isError = !isQuran && showErrors && text.isBlank(),
                            supportingText = when {
                                legacyText -> {
                                    { Text(stringResource(R.string.quran_legacy_text)) }
                                }
                                showErrors && text.isBlank() -> {
                                    { Text(stringResource(R.string.entry_text_error)) }
                                }
                                else -> null
                            },
                            textStyle = (if (isQuran) ReadingStyles.quran(0.85f) else ReadingStyles.dua(0.9f))
                                .copy(textAlign = TextAlign.Start, color = MaterialTheme.colorScheme.onSurface),
                            minLines = 3,
                            maxLines = 12,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
    }

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Delete, null) },
            text = {
                val message = if (usages > 0) {
                    stringResource(R.string.delete_entry_used_message, initial.title, uiDigits(usages))
                } else {
                    stringResource(R.string.delete_entry_message, initial.title)
                }
                val groupsNote = if (groups > 0) {
                    " " + stringResource(R.string.delete_entry_groups_note, uiDigits(groups))
                } else ""
                Text(message + groupsNote)
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * Said at the top of an editor when what is edited is read in more than one place: the edits will
 * show in all of them, or can be saved as a new copy for this step only.
 */
@Composable
private fun SharedNotice(text: String, onSaveAsNew: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.medium, color = scheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = scheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSecondaryContainer)
            }
            TextButton(onClick = onSaveAsNew, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.save_as_new))
            }
        }
    }
}

/** Asks the name of the new copy (prefilled with "<title> (نسخة)") before saving it. */
@Composable
private fun SaveAsNewDialog(initialName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_as_new_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.save_as_new_message), style = MaterialTheme.typography.bodyMedium)
                CompactTextField(
                    value = name,
                    onValueChange = { name = it },
                    trailingIcon = clearButton(name) { name = "" },
                    label = { Text(stringResource(R.string.save_as_new_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ------------------------------------------------------------------------------ Groups

/** Accent of a collection on a light surface. */
@Composable
private fun ItemType.surfaceAccent(): Color = colors().accent

/** A group of the library: its badge, title, description and the titles of its texts in order. */
@Composable
fun GroupEntryCard(
    group: LibraryGroup,
    library: Library,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inProgram: Boolean = false,
    selected: Boolean = false,
    /** Adds a menu to the card with «تعديل» ([onClick]) and «تكرار» (this). */
    onDuplicate: (() -> Unit)? = null,
    /** Adds the favourite heart: filled when [favourite], tapping it marks or unmarks the group. */
    onToggleFavourite: (() -> Unit)? = null,
    favourite: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val accent = group.type.surfaceAccent()
    val border = group.type.colors().border
    val titles = group.items.mapNotNull { item ->
        library.entry(item.entryId)?.let { entry ->
            if (item.count > 1) entry.title + " " + stringResource(R.string.times_count, uiDigits(item.count))
            else entry.title
        }
    }
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            GroupBadge(group.type, group.items.size, modifier = Modifier.padding(top = 2.dp), size = 40.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (inProgram) Pill(stringResource(R.string.entry_in_program), accent)
                    onToggleFavourite?.let { FavouriteButton(favourite, accent, it) }
                    if (onDuplicate != null) GroupCardMenu(onEdit = onClick, onDuplicate = onDuplicate)
                }
                Text(
                    text = listOf(
                        stringResource(R.string.group_texts_count, uiDigits(titles.size)),
                        if (group.isChoice) {
                            stringResource(R.string.group_required_short, uiDigits(group.required))
                        } else "",
                        group.description,
                    ).filter { it.isNotBlank() }.joinToString(" • "),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = titles.joinToString(stringResource(R.string.list_separator)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/** The heart on a library card: filled once the text or group is a favourite. */
@Composable
private fun FavouriteButton(favourite: Boolean, accent: Color, onToggle: () -> Unit) {
    IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = if (favourite) FavouriteFilledIcon else FavouriteIcon,
            contentDescription = stringResource(if (favourite) R.string.favourite_remove else R.string.favourite_add),
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** A library group's menu: edit it, or make a copy of it. */
@Composable
private fun GroupCardMenu(onEdit: () -> Unit, onDuplicate: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_group)) },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
                onClick = { open = false; onEdit() },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.duplicate)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = { open = false; onDuplicate() },
            )
        }
    }
}

/** Keeps a group's texts across recreation, as "count:entryId" strings. */
private val GroupItemsSaver = listSaver<List<GroupItem>, String>(
    save = { items -> items.map { "${it.count}:${it.entryId}" } },
    restore = { saved ->
        saved.map { GroupItem(entryId = it.substringAfter(':'), count = it.substringBefore(':').toInt()) }
    },
)

/**
 * Full-screen editor of a group: title, optional description and its texts in reading order, each
 * with its repeat count. Texts are picked from the collection, or written here (which adds them to
 * the library too). [onDelete] adds a delete action (with confirmation, which tells how many
 * program steps read the group).
 */
@Composable
fun GroupEditorDialog(
    type: ItemType,
    initial: LibraryGroup?,
    library: Library,
    onDismiss: () -> Unit,
    onSave: (LibraryGroup) -> Unit,
    onSaveEntry: (LibraryEntry) -> Unit,
    usages: Int = 0,
    onDelete: (() -> Unit)? = null,
    /** As in [EntryEditorDialog]: how many places read the group, and saving the edits as a new group. */
    sharedCount: Int = 0,
    onSaveAsNew: ((LibraryGroup) -> Unit)? = null,
    /** As in [EntryEditorDialog]: something to know at the top, and the save button's label. */
    notice: String? = null,
    saveLabel: String? = null,
) {
    var savingAsNew by remember { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var description by rememberSaveable { mutableStateOf(initial?.description.orEmpty()) }
    var items by rememberSaveable(stateSaver = GroupItemsSaver) { mutableStateOf(initial?.items.orEmpty()) }
    // All the texts required (0), or a minimum the reader chooses them to.
    var required by rememberSaveable { mutableIntStateOf(initial?.required ?: 0) }
    var query by rememberSaveable { mutableStateOf("") }
    var showErrors by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var writingEntry by remember { mutableStateOf(false) }
    val accent = type.surfaceAccent()
    val available = library.ofType(type).filter { entry -> items.none { it.entryId == entry.id } }
    val key = ArabicText.searchKey(query)
    val shown = if (key.isEmpty()) available else available.filter { it.matches(key) }

    /** The group as edited, or null (with the errors shown) while something is missing. */
    fun edited(): LibraryGroup? {
        if (title.isBlank() || items.isEmpty()) {
            showErrors = true
            return null
        }
        return (initial ?: LibraryGroup(type = type, title = "")).copy(
            title = title.trim(),
            description = description.trim(),
            items = items,
            required = required.takeIf { it in 1 until items.size } ?: 0,
        )
    }

    fun save() {
        edited()?.let(onSave)
    }

    if (savingAsNew && onSaveAsNew != null) {
        SaveAsNewDialog(
            initialName = stringResource(R.string.section_copy, title.trim()),
            onDismiss = { savingAsNew = false },
            onSave = { name ->
                savingAsNew = false
                edited()?.let { onSaveAsNew(it.copy(id = newId(), title = name)) }
            },
        )
    }

    val dirty = if (initial == null) {
        title.isNotBlank() || description.isNotBlank() || items.isNotEmpty()
    } else {
        title.trim() != initial.title || description.trim() != initial.description || items != initial.items ||
            (required.takeIf { it in 1 until items.size } ?: 0) != initial.required
    }
    EditorPopup(
        title = {
            EditorPopupTitle(
                title = stringResource(if (initial == null) R.string.new_group else R.string.edit_group),
                subtitle = stringResource(type.collectionName),
                subtitleColor = accent,
            )
        },
        onDismiss = onDismiss,
        onSave = ::save,
        dirty = dirty,
        accent = accent,
        saveLabel = saveLabel ?: stringResource(R.string.save),
        headerActions = {
            if (onDelete != null) {
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                }
            }
        },
    ) {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    notice?.let { text -> item(key = "notice") { PopupNotice(text) } }
                    if (onSaveAsNew != null && sharedCount > 1) {
                        item(key = "shared") {
                            SharedNotice(
                                text = stringResource(R.string.shared_group_notice, uiDigits(sharedCount)),
                                onSaveAsNew = { if (edited() != null) savingAsNew = true },
                            )
                        }
                    }
                    item(key = "title") {
                        CompactTextField(
                            value = title,
                            onValueChange = { title = it },
                            trailingIcon = clearButton(title) { title = "" },
                            label = { Text(stringResource(R.string.group_title)) },
                            isError = showErrors && title.isBlank(),
                            supportingText = if (showErrors && title.isBlank()) {
                                { Text(stringResource(R.string.group_title_error)) }
                            } else null,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item(key = "description") {
                        CompactTextField(
                            value = description,
                            onValueChange = { description = it },
                            trailingIcon = clearButton(description) { description = "" },
                            label = { Text(stringResource(R.string.entry_description)) },
                            placeholder = { Text(stringResource(R.string.entry_description_hint)) },
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    item(key = "required") {
                        RequiredEditor(required = required, size = items.size, accent = accent, onChange = { required = it })
                    }
                    item(key = "items") {
                        val missing = showErrors && items.isEmpty()
                        Column(Modifier.padding(top = 6.dp)) {
                            Text(
                                text = stringResource(R.string.group_items) + " (" + uiDigits(items.size) + ")",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (missing) MaterialTheme.colorScheme.error else Color.Unspecified,
                            )
                            if (items.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.group_items_error),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (missing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    itemsIndexed(items, key = { _, item -> "item:" + item.entryId }) { index, item ->
                        GroupItemRow(
                            number = index + 1,
                            entry = library.entry(item.entryId),
                            type = type,
                            count = item.count,
                            canMoveUp = index > 0,
                            canMoveDown = index < items.lastIndex,
                            onCountChange = { count ->
                                items = items.map { if (it.entryId == item.entryId) it.copy(count = count) else it }
                            },
                            onMove = { delta -> items = items.moved(index, delta) },
                            onRemove = { items = items.filterNot { it.entryId == item.entryId } },
                            modifier = Modifier.animateItem(),
                        )
                    }
                    item(key = "add") {
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.group_add_items),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { writingEntry = true }) {
                                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.add_entry))
                            }
                        }
                    }
                    if (available.size > 4) {
                        item(key = "search") { SearchField(query, onQueryChange = { query = it }) }
                    }
                    if (available.isEmpty()) {
                        item(key = "all_added") {
                            Text(
                                text = stringResource(R.string.group_all_added),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(shown, key = { "available:" + it.id }) { entry ->
                        AvailableEntryRow(
                            entry = entry,
                            accent = accent,
                            onAdd = { items = items + GroupItem(entry.id); showErrors = false },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
    }

    if (writingEntry) {
        EntryEditorDialog(
            type = type,
            initial = null,
            onDismiss = { writingEntry = false },
            onSave = { entry ->
                onSaveEntry(entry)
                items = items + GroupItem(entry.id)
                showErrors = false
                writingEntry = false
            },
            saveLabel = saveLabel,
        )
    }

    if (confirmDelete && onDelete != null && initial != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            icon = { Icon(Icons.Filled.Delete, null) },
            text = {
                Text(
                    if (usages > 0) stringResource(R.string.delete_group_used_message, initial.title, uiDigits(usages))
                    else stringResource(R.string.delete_group_message, initial.title)
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * How many of the group's texts are required: all of them, or a custom minimum (1 up to one less
 * than the texts), the reader choosing which.
 */
@Composable
private fun RequiredEditor(required: Int, size: Int, accent: Color, onChange: (Int) -> Unit) {
    // A minimum needs at least two texts; one at or over the texts' number is "all".
    val custom = required in 1 until size
    Column(Modifier.fillMaxWidth().padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.choose_title), style = MaterialTheme.typography.labelLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !custom,
                onClick = { onChange(0) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text(stringResource(R.string.required_all)) }
            SegmentedButton(
                selected = custom,
                enabled = size > 1,
                onClick = { if (!custom) onChange((size - 1).coerceAtLeast(1)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            ) { Text(stringResource(R.string.required_custom)) }
        }
        if (custom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StepButton("−", stringResource(R.string.decrease), enabled = required > 1) { onChange(required - 1) }
                Text(
                    text = stringResource(R.string.choose_of, uiDigits(required), uiDigits(size)),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                StepButton("+", stringResource(R.string.increase), enabled = required < size - 1) { onChange(required + 1) }
            }
        }
        Text(
            text = when {
                size < 2 -> stringResource(R.string.required_needs_two)
                custom -> stringResource(R.string.required_custom_hint, uiDigits(required))
                else -> stringResource(R.string.choose_all, uiDigits(size))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StepButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.size(48.dp).semantics { contentDescription = description },
        contentPadding = PaddingValues(0.dp),
    ) {
        Text(symbol, fontSize = 22.sp)
    }
}

/** A text of the group being edited: its place, title, repeat count and a menu to move or remove it. */
@Composable
private fun GroupItemRow(
    number: Int,
    entry: LibraryEntry?,
    type: ItemType,
    count: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCountChange: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val colors = type.colors()
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = uiDigits(number),
                style = MaterialTheme.typography.titleSmall,
                color = type.surfaceAccent(),
                modifier = Modifier.width(28.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = entry?.title ?: stringResource(R.string.untitled),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (entry != null) {
                    Text(
                        text = entry.previewText,
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            CountButton(count, type.surfaceAccent(), onCountChange)
            GroupItemMenu(canMoveUp, canMoveDown, onMove, onRemove)
        }
    }
}

private val CountPresets = listOf(1, 3, 7, 10, 33, 34, 100)

/** The repeat count of a text of the group; opens a menu to step it or pick a usual count. */
@Composable
private fun CountButton(count: Int, accent: Color, onCountChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(stringResource(R.string.times_count, uiDigits(count)), color = accent)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val decrease = stringResource(R.string.decrease)
            val increase = stringResource(R.string.increase)
            Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { onCountChange((count - 1).coerceAtLeast(1)) },
                    enabled = count > 1,
                    modifier = Modifier.semantics { contentDescription = decrease },
                ) { Text("−", fontSize = 20.sp) }
                Text(
                    text = uiDigits(count),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 40.dp),
                )
                TextButton(
                    onClick = { onCountChange((count + 1).coerceAtMost(ProgramJson.MAX_COUNT)) },
                    modifier = Modifier.semantics { contentDescription = increase },
                ) { Text("+", fontSize = 20.sp) }
            }
            HorizontalDivider()
            CountPresets.forEach { preset ->
                DropdownMenuItem(
                    text = { Text("×" + uiDigits(preset)) },
                    trailingIcon = if (preset == count) {
                        { Icon(Icons.Filled.Check, null, tint = accent) }
                    } else null,
                    onClick = { onCountChange(preset); open = false },
                )
            }
        }
    }
}

@Composable
private fun GroupItemMenu(canMoveUp: Boolean, canMoveDown: Boolean, onMove: (Int) -> Unit, onRemove: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (canMoveUp) DropdownMenuItem(
                text = { Text(stringResource(R.string.move_up)) },
                leadingIcon = { Icon(Icons.Filled.KeyboardArrowUp, null) },
                onClick = { open = false; onMove(-1) },
            )
            if (canMoveDown) DropdownMenuItem(
                text = { Text(stringResource(R.string.move_down)) },
                leadingIcon = { Icon(Icons.Filled.KeyboardArrowDown, null) },
                onClick = { open = false; onMove(1) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.remove_from_group), color = MaterialTheme.colorScheme.error) },
                leadingIcon = { Icon(Icons.Filled.Close, null, tint = MaterialTheme.colorScheme.error) },
                onClick = { open = false; onRemove() },
            )
        }
    }
}

/** A text of the collection that is not in the group yet: tapping it adds it at the end. */
@Composable
private fun AvailableEntryRow(entry: LibraryEntry, accent: Color, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onAdd,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = entry.previewText,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_to_group), tint = accent)
        }
    }
}

private fun <T> List<T>.moved(index: Int, delta: Int): List<T> {
    val target = index + delta
    if (index !in indices || target !in indices) return this
    return toMutableList().also { it.add(target, it.removeAt(index)) }
}

// ------------------------------------------------------------------------------ Library tab

/**
 * The library tab of the day view, compact: one toolbar row (the collection, its texts or groups,
 * search behind an icon, adding), then its texts as slim rows in one framed panel — as the day's
 * prayer panels — and the collections switched in a bar at the bottom.
 */
@Composable
fun LibraryTab(
    library: Library,
    programs: List<Program>,
    favourites: List<String>,
    onSave: (LibraryEntry) -> Unit,
    onDelete: (String) -> Unit,
    onSaveGroup: (LibraryGroup) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onSaveGroupCopy: (LibraryGroup, String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    contentPadding: PaddingValues,
    /** The room the system bars take at the bottom (the collections' bar sits above it). */
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    var typeKey by rememberSaveable { mutableStateOf(ItemType.QURAN.key) }
    val type = ItemType.fromKey(typeKey)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LibraryEntry?>(null) }
    var editingGroup by remember { mutableStateOf<LibraryGroup?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    val resources = LocalResources.current
    val context = LocalContext.current
    // The texts picked for the random reminders (the bell on each text).
    var reminderIds by remember { mutableStateOf(InspirationReminders.ids(context)) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val layoutDirection = LocalLayoutDirection.current
    val groupsTab = tab == GROUPS_TAB
    val entries = library.ofType(type)
    val groups = library.groupsOfType(type)
    val key = ArabicText.searchKey(query)
    val shownEntries = remember(entries, key) { if (key.isEmpty()) entries else entries.filter { it.matches(key) } }
    val shownGroups = remember(groups, library, key) {
        if (key.isEmpty()) groups
        else groups.filter { group ->
            ArabicText.searchKey(group.title + " " + group.description).contains(key) ||
                group.items.any { library.entry(it.entryId)?.matches(key) == true }
        }
    }
    val start = contentPadding.calculateStartPadding(layoutDirection) + 8.dp
    val end = contentPadding.calculateEndPadding(layoutDirection)
    fun toTop() = scope.launch { listState.scrollToItem(0) }
    val searchOpen = searching || query.isNotEmpty()
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = start,
                    end = end,
                    top = contentPadding.calculateTopPadding(),
                    // Above the collections' bar.
                    bottom = bottomInset + 80.dp,
                )
        ) {
            LibraryToolbar(
                type = type,
                texts = entries.size,
                groups = groups.size,
                groupsTab = groupsTab,
                onTab = { tab = it; toTop() },
                searching = searchOpen,
                query = query,
                onQuery = { query = it },
                onSearch = { searching = true },
                onCloseSearch = { query = ""; searching = false },
                onAdd = { adding = true },
                modifier = Modifier.padding(bottom = 12.dp),
            )
            // The texts (or groups) in their own framed, scrolling container: the toolbar stays.
            val shape = RoundedCornerShape(16.dp)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), shape),
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                if (groupsTab) {
                    if (shownGroups.isEmpty()) {
                        item(key = "empty") { EmptyText(if (groups.isEmpty()) R.string.groups_empty else R.string.no_results) }
                    }
                    itemsIndexed(shownGroups, key = { _, it -> "group:" + it.id }) { index, group ->
                        GroupRow(
                            group = group,
                            library = library,
                            slice = slice(top = index == 0, bottom = index == shownGroups.lastIndex),
                            inProgram = programs.readsGroup(group.id, library),
                            favourite = Favourites.group(group.id) in favourites,
                            onToggleFavourite = { onToggleFavourite(Favourites.group(group.id)) },
                            onClick = { editingGroup = group },
                            onDuplicate = {
                                library.duplicateGroup(group.id) { resources.getString(R.string.section_copy, it) }?.let { (_, copy) ->
                                    onSaveGroupCopy(copy, group.id)
                                    editingGroup = copy
                                }
                            },
                        )
                    }
                } else {
                    if (shownEntries.isEmpty()) {
                        item(key = "empty") { EmptyText(if (entries.isEmpty()) R.string.collection_empty else R.string.no_results) }
                    }
                    itemsIndexed(shownEntries, key = { _, it -> "entry:" + it.id }) { index, entry ->
                        EntryRow(
                            entry = entry,
                            slice = slice(top = index == 0, bottom = index == shownEntries.lastIndex),
                            inProgram = programs.reads(entry.id, library),
                            favourite = Favourites.entry(entry.id) in favourites,
                            onToggleFavourite = { onToggleFavourite(Favourites.entry(entry.id)) },
                            reminder = entry.id in reminderIds,
                            onToggleReminder = { reminderIds = InspirationReminders.toggle(context, entry.id) },
                            onClick = { editing = entry },
                        )
                    }
                    // Where the Quran's text comes from, after its passages.
                    if (type == ItemType.QURAN && shownEntries.isNotEmpty()) {
                        item(key = "source") { QuranSourceNote(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) }
                    }
                }
            }
        }
        CollectionBar(
            library = library,
            selected = type,
            onSelect = { picked ->
                if (picked != type) {
                    typeKey = picked.key
                    query = ""
                    searching = false
                    toTop()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = start, end = end, bottom = bottomInset + 12.dp),
        )
    }

    if (adding) {
        if (groupsTab) {
            GroupEditorDialog(
                type = type,
                initial = null,
                library = library,
                onDismiss = { adding = false },
                onSave = { onSaveGroup(it); adding = false },
                onSaveEntry = onSave,
            )
        } else {
            EntryEditorDialog(
                type = type,
                initial = null,
                onDismiss = { adding = false },
                onSave = { onSave(it); adding = false },
            )
        }
    }
    editing?.let { entry ->
        EntryEditorDialog(
            type = entry.type,
            initial = entry,
            usages = programs.usages(entry.id),
            groups = library.groupsWith(entry.id).size,
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(entry.id); editing = null },
        )
    }
    editingGroup?.let { group ->
        GroupEditorDialog(
            type = group.type,
            initial = group,
            library = library,
            usages = programs.groupUsages(group.id),
            onDismiss = { editingGroup = null },
            onSave = { onSaveGroup(it); editingGroup = null },
            onSaveEntry = onSave,
            onDelete = { onDeleteGroup(group.id); editingGroup = null },
        )
    }
}

/**
 * The library tab's toolbar: its texts or its groups (one choice of the two, with their counts),
 * search and adding. Searching, the field takes the toolbar's width and the other tools go in a
 * menu at its end.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryToolbar(
    type: ItemType,
    texts: Int,
    groups: Int,
    groupsTab: Boolean,
    onTab: (Int) -> Unit,
    searching: Boolean,
    query: String,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = type.colors()
    val accent = colors.accent
    val addLabel = stringResource(if (groupsTab) R.string.add_group else R.string.add_entry)
    Row(modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
        if (searching) {
            LibrarySearch(query = query, onQuery = onQuery, onClose = onCloseSearch, modifier = Modifier.weight(1f))
            // The tools the field covers: the two kinds and adding.
            var menu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf(0 to "${stringResource(R.string.library_tab_singles)} ${uiDigits(texts)}", GROUPS_TAB to "${stringResource(R.string.library_tab_groups)} ${uiDigits(groups)}")
                        .forEach { (tab, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                leadingIcon = { RadioButton(selected = (tab == GROUPS_TAB) == groupsTab, onClick = null) },
                                onClick = { menu = false; onTab(tab) },
                            )
                        }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(addLabel) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = { menu = false; onAdd() },
                    )
                }
            }
            return@Row
        }
        // Texts or groups: one choice of two.
        SingleChoiceSegmentedButtonRow(Modifier.weight(1f)) {
            listOf(0 to (R.string.library_tab_singles to texts), GROUPS_TAB to (R.string.library_tab_groups to groups))
                .forEachIndexed { index, (tab, labelAndCount) ->
                    SegmentedButton(
                        selected = (tab == GROUPS_TAB) == groupsTab,
                        onClick = { onTab(tab) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = accent.copy(alpha = 0.16f),
                            activeContentColor = accent,
                            activeBorderColor = accent.copy(alpha = 0.7f),
                            inactiveBorderColor = accent.copy(alpha = 0.4f),
                        ),
                        label = {
                            Text(
                                "${stringResource(labelAndCount.first)} ${uiDigits(labelAndCount.second)}",
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                            )
                        },
                    )
                }
        }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onSearch) {
            Icon(Icons.Filled.Search, stringResource(R.string.search), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Surface(
            onClick = onAdd,
            shape = CircleShape,
            color = accent.copy(alpha = 0.14f),
            contentColor = accent,
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, addLabel, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** The search field, opened from the toolbar: focused at once; its ✕ empties and closes it. */
@Composable
private fun LibrarySearch(query: String, onQuery: (String) -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    CompactTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = { Text(stringResource(R.string.search)) },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Close, stringResource(R.string.clear_text), modifier = Modifier.size(18.dp))
            }
        },
        singleLine = true,
        shape = CircleShape,
        modifier = modifier.focusRequester(focus),
    )
}

/** A thin line between two rows of the panel. */
@Composable
private fun RowDivider(slice: io.github.arcm111.dhikra.ui.components.Slice) {
    if (slice == io.github.arcm111.dhikra.ui.components.Slice.Middle || slice == io.github.arcm111.dhikra.ui.components.Slice.Bottom) {
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    }
}

/** A dot in the collection's colour: the text (or group) is read by a program. */
@Composable
private fun InProgramDot(accent: Color) {
    val label = stringResource(R.string.entry_in_program)
    Box(
        Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(accent)
            .semantics { contentDescription = label },
    )
}

/** A text of the library as a slim row of the panel: its title, its opening words, the marks and the heart. */
@Composable
private fun EntryRow(
    entry: LibraryEntry,
    slice: io.github.arcm111.dhikra.ui.components.Slice,
    inProgram: Boolean,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    /** Picked for the random reminders (the bell). */
    reminder: Boolean,
    onToggleReminder: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = entry.type.colors()
    val scheme = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth()) {
        RowDivider(slice)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (entry.type == ItemType.QURAN) colors.accent else scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (inProgram) {
                        Spacer(Modifier.width(6.dp))
                        InProgramDot(colors.accent)
                    }
                }
                Text(
                    listOf(entry.description, entry.previewText).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggleReminder, modifier = Modifier.size(40.dp)) {
                Icon(
                    if (reminder) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                    stringResource(if (reminder) R.string.inspiration_toggle_off else R.string.inspiration_toggle_on),
                    tint = if (reminder) colors.accent else scheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp),
                )
            }
            FavouriteButton(favourite, colors.accent, onToggleFavourite)
        }
    }
}

/** A group of the library as a slim row of the panel: its badge, title, texts, the marks, heart and menu. */
@Composable
private fun GroupRow(
    group: LibraryGroup,
    library: Library,
    slice: io.github.arcm111.dhikra.ui.components.Slice,
    inProgram: Boolean,
    favourite: Boolean,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = group.type.colors().accent
    val scheme = MaterialTheme.colorScheme
    val titles = group.items.mapNotNull { library.entry(it.entryId)?.title }
    Column(modifier.fillMaxWidth()) {
        RowDivider(slice)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 16.dp, end = 2.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GroupBadge(group.type, group.items.size, size = 32.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        group.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (inProgram) {
                        Spacer(Modifier.width(6.dp))
                        InProgramDot(accent)
                    }
                }
                Text(
                    titles.joinToString(stringResource(R.string.list_separator)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FavouriteButton(favourite, accent, onToggleFavourite)
            GroupCardMenu(onEdit = onClick, onDuplicate = onDuplicate)
        }
    }
}

/**
 * The four collections, as a bar at the bottom of the library tab: a button each — its icon and name
 * — the one shown filled in its colour.
 */
@Composable
private fun CollectionBar(library: Library, selected: ItemType, onSelect: (ItemType) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.7f)),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth().selectableGroup(),
    ) {
        Row(Modifier.height(56.dp).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            ItemType.CONTENT.forEach { type ->
                val colors = type.colors()
                val on = type == selected
                val spoken = stringResource(type.collectionName)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (on) colors.accent else Color.Transparent)
                        .selectable(selected = on, role = Role.Tab) { onSelect(type) }
                        .semantics(mergeDescendants = true) { contentDescription = spoken },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(type.icon, null, tint = if (on) colors.onAccent else colors.accent, modifier = Modifier.size(20.dp))
                    Text(
                        stringResource(type.label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (on) FontWeight.SemiBold else null,
                        color = if (on) colors.onAccent else scheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
