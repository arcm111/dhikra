package com.example.dhikra.ui.prayer

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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.Prayer
import com.example.dhikra.data.PrayerCity
import com.example.dhikra.data.PrayerTimesSource
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.UpcomingPrayer
import com.example.dhikra.ui.components.DhikraHeader
import com.example.dhikra.ui.components.FajrIcon
import com.example.dhikra.ui.components.MoonStarIcon
import com.example.dhikra.ui.components.PrayerIcon
import com.example.dhikra.ui.components.SunIcon
import com.example.dhikra.ui.components.headerAppBarColors
import com.example.dhikra.ui.components.starLattice
import com.example.dhikra.ui.formatTimeArabic
import com.example.dhikra.ui.theme.MustardLight
import com.example.dhikra.ui.theme.LocalDhikraPalette
import kotlinx.coroutines.delay
import java.util.Calendar

/**
 * Prayer times, opened from the main menu: the next prayer and the countdown to it, then the
 * day's six times in the city the user picked. The city is chosen from a list (no location
 * permission); until one is chosen the screen asks for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    timetable: PrayerTimetable?,
    source: PrayerTimesSource,
    onSelectCity: (String) -> Unit,
    onBack: () -> Unit,
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    val nowMinutes = rememberMinuteOfDay()
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
                item { NextPrayerCard(timetable, next, onChangeCity = { picking = true }) }
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
                item { DayTimes(timetable, next, nowMinutes) }
                item {
                    Text(
                        text = stringResource(R.string.prayer_mock_note),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = MaxWidth).padding(top = 16.dp, start = 8.dp, end = 8.dp),
                    )
                }
            }
        }
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

/** The current time as minutes after midnight, updated every minute while shown. */
@Composable
private fun rememberMinuteOfDay(): Int {
    var minute by remember { mutableIntStateOf(minuteOfDay()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay((60 - Calendar.getInstance().get(Calendar.SECOND)) * 1000L)
            minute = minuteOfDay()
        }
    }
    return minute
}

private fun minuteOfDay(): Int =
    Calendar.getInstance().let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

private val Prayer.label: Int
    get() = when (this) {
        Prayer.FAJR -> R.string.prayer_fajr
        Prayer.SUNRISE -> R.string.prayer_sunrise
        Prayer.DHUHR -> R.string.prayer_dhuhr
        Prayer.ASR -> R.string.prayer_asr
        Prayer.MAGHRIB -> R.string.prayer_maghrib
        Prayer.ISHA -> R.string.prayer_isha
    }

private val Prayer.icon: ImageVector
    get() = when (this) {
        Prayer.FAJR, Prayer.SUNRISE, Prayer.MAGHRIB -> FajrIcon
        Prayer.DHUHR, Prayer.ASR -> SunIcon
        Prayer.ISHA -> MoonStarIcon
    }

/** "بعد ١ س ٢٠ د" / "بعد ٢٠ د". */
@Composable
private fun untilText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) {
        stringResource(R.string.prayer_in_hours, ArabicText.toArabicDigits(h), ArabicText.toArabicDigits(m))
    } else {
        stringResource(R.string.prayer_in_minutes, ArabicText.toArabicDigits(m))
    }
}

/** The next prayer, large, on the brand's dark green with the countdown and the city. */
@Composable
private fun NextPrayerCard(timetable: PrayerTimetable, next: UpcomingPrayer, onChangeCity: () -> Unit) {
    val palette = LocalDhikraPalette.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color.Transparent,
        shadowElevation = 4.dp,
        modifier = Modifier.widthIn(max = MaxWidth).fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
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
                text = formatTimeArabic(next.atMinutes),
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
                    text = timetable.city.name + "، " + timetable.city.country,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

/** The day's six times in one card; the next prayer is highlighted and those passed are dimmed. */
@Composable
private fun DayTimes(timetable: PrayerTimetable, next: UpcomingPrayer, nowMinutes: Int) {
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
                        text = formatTimeArabic(timetable.at(prayer)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isNext) FontWeight.SemiBold else null,
                        color = when {
                            isNext -> scheme.primary
                            passed -> scheme.onSurfaceVariant
                            else -> scheme.onSurface
                        },
                    )
                }
            }
        }
    }
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
private fun CityPickerSheet(
    source: PrayerTimesSource,
    selectedId: String?,
    onSelect: (PrayerCity) -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    var query by rememberSaveable { mutableStateOf("") }
    val cities = remember(query, source) { source.search(query) }
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
                            Text(city.name, style = MaterialTheme.typography.titleSmall)
                            Text(city.country, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                        }
                        if (selected) Icon(Icons.Filled.Check, null, tint = scheme.primary)
                    }
                }
            }
        }
    }
}
