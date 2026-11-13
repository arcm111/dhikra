package com.example.dhikra.ui.prayer

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.ui.semantics.contentDescription
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.AsrMadhab
import com.example.dhikra.data.PrayerDefaults
import com.example.dhikra.data.PrayerMethod
import com.example.dhikra.data.PrayerSettings
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Switch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import com.example.dhikra.data.PrayerAlert
import com.example.dhikra.reminder.ReminderScheduler
import com.example.dhikra.ui.components.ReminderSoundDialog
import com.example.dhikra.ui.components.reminderSoundLabel
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerCity
import com.example.dhikra.data.PrayerTimesSource
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.UpcomingPrayer
import com.example.dhikra.ui.components.DhikraHeader
import com.example.dhikra.ui.components.PrayerIcon
import com.example.dhikra.ui.components.headerAppBarColors
import com.example.dhikra.ui.components.starLattice
import com.example.dhikra.ui.formatClock
import com.example.dhikra.ui.theme.MustardLight
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.uiDigits
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Notifications
import com.example.dhikra.ui.components.BellOffIcon

/**
 * Prayer times, opened from the main menu: the next prayer and the countdown to it (its gear opens
 * the alerts and the calculation settings), then the day's six times, each prayer with a bell
 * saying whether its alert is set, in the city the user picked. The city is chosen from a list (no location
 * permission); until one is chosen the screen asks for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    timetable: PrayerTimetable?,
    source: PrayerTimesSource,
    onSelectCity: (String) -> Unit,
    onBack: () -> Unit,
    alerts: Map<Prayer, PrayerAlert> = emptyMap(),
    onAlertChange: (Prayer, PrayerAlert) -> Unit = { _, _ -> },
    settings: PrayerSettings = PrayerSettings(),
    onSettingsChange: (PrayerSettings) -> Unit = {},
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    // The settings — the alerts and how the times are calculated — open from the gear.
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val nowMinutes = rememberMinuteOfDay(timetable?.city?.timeZone)
    Scaffold(
        topBar = {
            DhikraHeader(bottomPadding = 4.dp) {
                TopAppBar(
                    title = { Text(stringResource(R.string.prayer_times), style = MaterialTheme.typography.titleLarge) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (timetable != null) {
                            IconButton(onClick = { picking = true }) {
                                Icon(Icons.Outlined.LocationOn, stringResource(R.string.prayer_change_city))
                            }
                        }
                    },
                    expandedHeight = 56.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (timetable == null) {
            NoCity(onPick = { picking = true }, modifier = Modifier.padding(padding))
        } else {
            val layoutDirection = LocalLayoutDirection.current
            LazyColumn(
                modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
                contentPadding = PaddingValues(
                    start = padding.calculateStartPadding(layoutDirection) + 16.dp,
                    end = padding.calculateEndPadding(layoutDirection) + 16.dp,
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = padding.calculateBottomPadding() + 24.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val next = timetable.next(nowMinutes)
                item { NextPrayerCard(timetable, next, onChangeCity = { picking = true }, onSettings = { settingsOpen = true }) }
                item {
                    Text(
                        text = stringResource(R.string.prayer_today),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .widthIn(max = MaxWidth)
                            .fillMaxWidth()
                            .padding(start = 8.dp, top = 24.dp, bottom = 8.dp)
                            .semantics { heading() },
                    )
                }
                item { DayTimes(timetable, next, nowMinutes, alerts) }

            }
        }
    }

    if (settingsOpen && timetable != null) {
        PrayerSettingsSheet(
            countryCode = timetable.city.countryCode,
            alerts = alerts,
            onAlertChange = onAlertChange,
            settings = settings,
            onSettingsChange = onSettingsChange,
            onDismiss = { settingsOpen = false },
        )
    }
    if (picking) {
        CityPickerSheet(
            source = source,
            selectedId = timetable?.city?.id,
            onSelect = { city ->
                onSelectCity(city.id)
                picking = false
            },
            onDismiss = { picking = false },
        )
    }
}

private val MaxWidth = 560.dp

/** The next prayer, large, on the brand's dark green with the countdown and the city. */
@Composable
private fun NextPrayerCard(timetable: PrayerTimetable, next: UpcomingPrayer, onChangeCity: () -> Unit, onSettings: () -> Unit) {
    val palette = LocalDhikraPalette.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        shadowElevation = 4.dp,
        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth(),
    ) {
      Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(palette.headerStart, palette.headerEnd)))
                .starLattice(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.prayer_next_label),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.75f),
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(next.prayer.icon, null, tint = MustardLight, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(next.prayer.label),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
            }
            Text(
                text = formatClock(next.atMinutes),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            Spacer(Modifier.height(10.dp))
            Surface(shape = CircleShape, color = MustardLight.copy(alpha = 0.18f), contentColor = MustardLight) {
                Text(
                    text = untilText(next.inMinutes),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            TextButton(onClick = onChangeCity) {
                Icon(Icons.Outlined.LocationOn, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = timetable.city.shownName + stringResource(R.string.list_separator) + timetable.city.shownCountry,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
        // The settings: the prayers' alerts and how the times are calculated.
        IconButton(onClick = onSettings, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.prayer_settings), tint = Color.White.copy(alpha = 0.9f))
        }
      }
    }
}

/** The day's six times in one card; the next prayer is highlighted and those passed are dimmed. */
@Composable
private fun DayTimes(timetable: PrayerTimetable, next: UpcomingPrayer, nowMinutes: Int, alerts: Map<Prayer, PrayerAlert>) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDhikraPalette.current
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth(),
    ) {
        Column(Modifier.padding(6.dp)) {
            Prayer.entries.forEachIndexed { index, prayer ->
                val isNext = prayer == next.prayer
                val passed = next.tomorrow || timetable.at(prayer) <= nowMinutes
                if (index > 0 && !isNext && Prayer.entries[index - 1] != next.prayer) {
                    HorizontalDivider(
                        color = scheme.outlineVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(if (isNext) scheme.primaryContainer else Color.Transparent)
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isNext) scheme.primary else palette.ornament.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = prayer.icon,
                            contentDescription = null,
                            tint = if (isNext) scheme.onPrimary else palette.ornamentText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(prayer.label),
                        style = MaterialTheme.typography.titleMedium,
                        color = when {
                            isNext -> scheme.onPrimaryContainer
                            passed -> scheme.onSurfaceVariant
                            else -> scheme.onSurface
                        },
                        modifier = Modifier.weight(1f),
                    )
                    if (isNext) {
                        Text(
                            text = untilText(next.inMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                    }
                    Text(
                        text = formatClock(timetable.at(prayer)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.SemiBold else null,
                        color = when {
                            isNext -> scheme.primary
                            passed -> scheme.onSurfaceVariant
                            else -> scheme.onSurface
                        },
                    )
                    // Whether this prayer's alert is set: a bell, or a bell struck through.
                    Spacer(Modifier.width(12.dp))
                    if (prayer.isPrayer) {
                        val on = alerts[prayer]?.enabled == true
                        Icon(
                            imageVector = if (on) Icons.Filled.Notifications else BellOffIcon,
                            contentDescription = stringResource(if (on) R.string.prayer_alert_on else R.string.prayer_alert_off),
                            tint = if (on) scheme.primary else scheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Spacer(Modifier.width(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * The five prayers' alerts: a switch for each, and its sound («الافتراضي» until another is
 * chosen; the azan is one of the choices). Turning one on asks for the notification permission
 * when it was not granted yet.
 */
@Composable
private fun PrayerAlertsCard(alerts: Map<Prayer, PrayerAlert>, onChange: (Prayer, PrayerAlert) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDhikraPalette.current
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    var choosingFor by rememberSaveable { mutableStateOf<Prayer?>(null) }
    val prayers = Prayer.entries.filter { it.isPrayer }
    // Without the exact-alarm permission Android may deliver an alert minutes late: say so while
    // an alert is on, and re-check when the user comes back from the system setting.
    var exact by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    LifecycleResumeEffect(Unit) {
        exact = ReminderScheduler.canScheduleExactAlarms(context)
        onPauseOrDispose { }
    }
    if (!exact && alerts.values.any { it.enabled } && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer,
            modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth().padding(bottom = 8.dp),
        ) {
            Row(Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.exact_alarm_note), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
                    )
                }) { Text(stringResource(R.string.exact_alarm_allow)) }
            }
        }
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            prayers.forEachIndexed { index, prayer ->
                val alert = alerts[prayer] ?: PrayerAlert()
                val toggle = { on: Boolean ->
                    if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !ReminderScheduler.hasNotificationPermission(context)
                    ) {
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    onChange(prayer, alert.copy(enabled = on))
                }
                if (index > 0) {
                    HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 12.dp))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = alert.enabled, role = Role.Switch, onValueChange = toggle)
                        .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(palette.ornament.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(prayer.icon, null, tint = palette.ornamentText, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(prayer.label), style = MaterialTheme.typography.titleMedium)
                        // The sound, as a small button of its own: it opens the sound choices.
                        Surface(
                            onClick = { choosingFor = prayer },
                            shape = CircleShape,
                            color = scheme.secondaryContainer.copy(alpha = if (alert.enabled) 1f else 0.5f),
                            contentColor = scheme.onSecondaryContainer,
                            modifier = Modifier.padding(top = 4.dp),
                        ) {
                            Row(Modifier.padding(start = 10.dp, end = 6.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.prayer_alert_sound, reminderSoundLabel(alert.sound)),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Switch(checked = alert.enabled, onCheckedChange = null)
                }
            }
        }
    }
    choosingFor?.let { prayer ->
        val alert = alerts[prayer] ?: PrayerAlert()
        ReminderSoundDialog(
            current = alert.sound,
            allowAzan = true,
            title = stringResource(R.string.prayer_alert_sound_title, stringResource(prayer.label)),
            onSelect = { sound ->
                // Choosing a sound turns the alert on: that is what the user is setting up.
                if (!alert.enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ReminderScheduler.hasNotificationPermission(context)
                ) {
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                onChange(prayer, PrayerAlert(enabled = true, sound = sound))
                choosingFor = null
            },
            onDismiss = { choosingFor = null },
        )
    }
}

/** The prayer times' settings, from the gear: the five prayers' alerts, then how the times are calculated. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrayerSettingsSheet(
    countryCode: String,
    alerts: Map<Prayer, PrayerAlert>,
    onAlertChange: (Prayer, PrayerAlert) -> Unit,
    settings: PrayerSettings,
    onSettingsChange: (PrayerSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Text(
                    text = stringResource(R.string.prayer_settings),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth().padding(start = 8.dp, bottom = 4.dp).semantics { heading() },
                )
            }
            item { SheetHeading(stringResource(R.string.prayer_alerts), stringResource(R.string.prayer_alerts_note)) }
            item { PrayerAlertsCard(alerts, onAlertChange) }
            item { SheetHeading(stringResource(R.string.prayer_calculation), stringResource(R.string.prayer_calculation_note)) }
            item { CalculationCard(countryCode, settings, onSettingsChange) }
        }
    }
}

/** A part of the settings sheet: its title, and a line on what it does. */
@Composable
private fun SheetHeading(title: String, note: String) {
    Column(Modifier.widthIn(max = MaxWidth).fillMaxWidth().padding(start = 8.dp, top = 20.dp, bottom = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        Text(text = note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val PrayerMethod.label: Int
    get() = when (this) {
        PrayerMethod.MUSLIM_WORLD_LEAGUE -> R.string.method_mwl
        PrayerMethod.UMM_AL_QURA -> R.string.method_umm_al_qura
        PrayerMethod.EGYPTIAN -> R.string.method_egyptian
        PrayerMethod.KARACHI -> R.string.method_karachi
        PrayerMethod.DUBAI -> R.string.method_dubai
        PrayerMethod.KUWAIT -> R.string.method_kuwait
        PrayerMethod.QATAR -> R.string.method_qatar
        PrayerMethod.TURKEY -> R.string.method_turkey
        PrayerMethod.SINGAPORE -> R.string.method_singapore
        PrayerMethod.NORTH_AMERICA -> R.string.method_north_america
        PrayerMethod.MOON_SIGHTING_COMMITTEE -> R.string.method_moonsighting
        PrayerMethod.MOROCCO -> R.string.method_morocco
    }

private val AsrMadhab.label: Int
    get() = if (this == AsrMadhab.HANAFI) R.string.madhab_hanafi else R.string.madhab_shafi

private enum class CalculationDialog { Method, Madhab, Adjust }

/**
 * How the times are calculated: the method (by default the Moonsighting Committee's) and the Asr
 * school (by default the city's country's, [countryCode]) and the minutes added to or taken from each time. Each row opens its
 * choices.
 */
@Composable
private fun CalculationCard(countryCode: String, settings: PrayerSettings, onChange: (PrayerSettings) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    var dialog by rememberSaveable { mutableStateOf<CalculationDialog?>(null) }
    val defaultMethod = PrayerDefaults.method(countryCode)
    val defaultMadhab = PrayerDefaults.madhab(countryCode)

    @Composable
    fun row(title: Int, value: String, onClick: () -> Unit) {
        Surface(onClick = onClick, color = Color.Transparent) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                    Text(value, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ArrowDropDown, null, tint = scheme.onSurfaceVariant)
            }
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) {
            row(
                R.string.prayer_method,
                settings.method?.let { stringResource(it.label) }
                    ?: stringResource(R.string.prayer_auto, stringResource(defaultMethod.label)),
            ) { dialog = CalculationDialog.Method }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 12.dp))
            row(
                R.string.prayer_madhab,
                settings.madhab?.let { stringResource(it.label) }
                    ?: stringResource(R.string.prayer_auto, stringResource(defaultMadhab.label)),
            ) { dialog = CalculationDialog.Madhab }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.6f), modifier = Modifier.padding(horizontal = 12.dp))
            val adjusted = Prayer.entries.filter { settings.adjustment(it) != 0 }
            row(
                R.string.prayer_adjust,
                if (adjusted.isEmpty()) stringResource(R.string.prayer_adjust_none)
                else adjusted.map { prayer -> stringResource(R.string.prayer_adjust_value, stringResource(prayer.label), signedMinutes(settings.adjustment(prayer))) }
                    .joinToString("، "),
            ) { dialog = CalculationDialog.Adjust }
        }
    }

    when (dialog) {
        CalculationDialog.Method -> ChoiceDialog(
            title = stringResource(R.string.prayer_method),
            options = listOf<PrayerMethod?>(null) + PrayerMethod.entries,
            selected = settings.method,
            label = { it?.let { method -> stringResource(method.label) } ?: stringResource(R.string.prayer_auto, stringResource(defaultMethod.label)) },
            // The default method is the same everywhere (لجنة رؤية الهلال), not the country's.
            note = { null },
            onSelect = { onChange(settings.copy(method = it)); dialog = null },
            onDismiss = { dialog = null },
        )
        CalculationDialog.Madhab -> ChoiceDialog(
            title = stringResource(R.string.prayer_madhab),
            options = listOf<AsrMadhab?>(null) + AsrMadhab.entries,
            selected = settings.madhab,
            label = { it?.let { madhab -> stringResource(madhab.label) } ?: stringResource(R.string.prayer_auto, stringResource(defaultMadhab.label)) },
            note = { if (it == null) stringResource(R.string.prayer_auto_note) else null },
            onSelect = { onChange(settings.copy(madhab = it)); dialog = null },
            onDismiss = { dialog = null },
        )
        CalculationDialog.Adjust -> AdjustDialog(settings, onChange, onDismiss = { dialog = null })
        null -> Unit
    }
}

/** "+٢" / "−٣": minutes added to or taken from a time. */
private fun signedMinutes(minutes: Int): String =
    (if (minutes > 0) "+" else "−") + uiDigits(kotlin.math.abs(minutes))

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    note: @Composable (T) -> String?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.selectableGroup().heightIn(max = 460.dp)) {
                items(options) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, onClick = { onSelect(option) }, role = Role.RadioButton)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(label(option), style = MaterialTheme.typography.bodyLarge)
                            note(option)?.let {
                                Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** Moves each time by whole minutes, up to [PrayerSettings.MAX_ADJUSTMENT] either way. */
@Composable
private fun AdjustDialog(settings: PrayerSettings, onChange: (PrayerSettings) -> Unit, onDismiss: () -> Unit) {
    var adjustments by remember { mutableStateOf(settings.adjustments) }
    val limit = PrayerSettings.MAX_ADJUSTMENT
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prayer_adjust)) },
        text = {
            Column {
                Prayer.entries.forEach { prayer ->
                    val value = adjustments[prayer] ?: 0
                    fun set(minutes: Int) {
                        adjustments = (adjustments + (prayer to minutes.coerceIn(-limit, limit))).filterValues { it != 0 }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(prayer.label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        val earlier = stringResource(R.string.prayer_minus_minute)
                        IconButton(
                            onClick = { set(value - 1) },
                            enabled = value > -limit,
                            modifier = Modifier.semantics { contentDescription = earlier },
                        ) {
                            Text("−", style = MaterialTheme.typography.titleLarge)
                        }
                        Text(
                            text = if (value == 0) uiDigits(0) else signedMinutes(value),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(44.dp),
                        )
                        IconButton(onClick = { set(value + 1) }, enabled = value < limit) {
                            Icon(Icons.Filled.Add, stringResource(R.string.prayer_plus_minute))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onChange(settings.copy(adjustments = adjustments)); onDismiss() }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { adjustments = emptyMap() }) { Text(stringResource(R.string.prayer_reset)) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    )
}

/** Before a city is picked: what the screen shows and the button that opens the city list. */
@Composable
private fun NoCity(onPick: () -> Unit, modifier: Modifier) {
    val palette = LocalDhikraPalette.current
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(88.dp).clip(CircleShape).background(palette.ornament.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PrayerIcon, null, tint = palette.ornamentText, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.prayer_pick_city), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.prayer_pick_city_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onPick, shape = CircleShape) {
            Icon(Icons.Outlined.LocationOn, null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.prayer_choose_city))
        }
    }
}

/**
 * The cities the prayer times are available for, with a search field: the user picks their city
 * here instead of granting the location permission.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CityPickerSheet(
    source: PrayerTimesSource,
    selectedId: String?,
    onSelect: (PrayerCity) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    // The search reads the bundled database: off the main thread.
    val cities by produceState(initialValue = emptyList<PrayerCity>(), query, source) {
        value = withContext(Dispatchers.IO) { source.search(query) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Text(
            text = stringResource(R.string.prayer_pick_city),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp),
        )
        Text(
            text = stringResource(R.string.prayer_pick_city_note),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 2.dp, bottom = 12.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(stringResource(R.string.prayer_search_city)) },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            singleLine = true,
            shape = CircleShape,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.navigationBarsPadding().heightIn(max = 480.dp),
        ) {
            if (cities.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.prayer_no_city),
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(cities, key = { it.id }) { city ->
                val selected = city.id == selectedId
                Surface(
                    onClick = { onSelect(city) },
                    shape = MaterialTheme.shapes.medium,
                    color = if (selected) scheme.secondaryContainer else Color.Transparent,
                    modifier = Modifier.fillMaxWidth().semantics { this.selected = selected },
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(city.shownName, style = MaterialTheme.typography.titleSmall)
                            // Many cities share a name: the state / province tells them apart.
                            Text(
                                text = listOf(city.shownRegion, city.shownCountry).filter { it.isNotBlank() }.distinct()
                                    .joinToString(stringResource(R.string.list_separator)),
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        if (selected) Icon(Icons.Filled.Check, null, tint = scheme.primary)
                    }
                }
            }
        }
    }
}

/**
 * The first launch: a welcome asking for the user's city, which the prayer times and the day's
 * schedule follow. «اختيار المدينة» opens the city list ([onPick] with the city chosen); «لاحقًا»
 * puts it off ([onLater]) — the city can still be chosen from the prayer times.
 */
@Composable
fun FirstCityDialog(source: PrayerTimesSource, onPick: (String) -> Unit, onLater: () -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    if (picking) {
        CityPickerSheet(
            source = source,
            selectedId = null,
            onSelect = { city -> onPick(city.id) },
            onDismiss = { picking = false },
        )
        return
    }
    AlertDialog(
        onDismissRequest = {},
        icon = { Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.first_city_title), textAlign = TextAlign.Center) },
        text = {
            Text(
                text = stringResource(R.string.first_city_text),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Button(onClick = { picking = true }) { Text(stringResource(R.string.first_city_pick)) }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text(stringResource(R.string.first_city_later)) }
        },
    )
}
