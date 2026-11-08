package com.example.dhikra.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.data.PrayerTimetable
import com.example.dhikra.data.ProgramSection
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.components.DhikraHeader
import com.example.dhikra.ui.components.DhikraLogo
import com.example.dhikra.ui.components.headerAppBarColors
import com.example.dhikra.ui.components.sectionIcon
import com.example.dhikra.ui.formatTimeArabic
import com.example.dhikra.ui.home.HomeActions
import com.example.dhikra.ui.home.NowChip
import com.example.dhikra.ui.home.OverflowMenu
import com.example.dhikra.ui.home.ProgramSwitcher
import com.example.dhikra.ui.home.SectionProgress
import com.example.dhikra.ui.prayer.icon
import com.example.dhikra.ui.prayer.label
import com.example.dhikra.ui.prayer.rememberMinuteOfDay
import com.example.dhikra.ui.prayer.untilText
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.MustardLight

/** Test tag of the day view's list. */
const val DayTimelineTag = "day"

/**
 * The start screen: the day at a glance. The header carries the program switcher, the menu and
 * the coming prayer; below it the day is split into its prayer periods (الفجر until الشروق, …,
 * العشاء until the next الفجر), each a container holding the program's sections whose time falls
 * in it — the current period in green with the current time marked, the others in blue. The
 * sections with no time of day follow at the bottom, under a divider. Tapping a section opens it
 * in the section view ([onOpenSection]).
 */
@Composable
fun TodayScreen(
    state: UiState,
    actions: HomeActions,
    onOpenSection: (String) -> Unit,
    nowMinutes: Int = rememberMinuteOfDay(state.prayerTimes?.city?.timeZone),
) {
    // Another program is another day: its scroll position starts afresh.
    key(state.program.id) { DayView(state, actions, onOpenSection, nowMinutes) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayView(
    state: UiState,
    actions: HomeActions,
    onOpenSection: (String) -> Unit,
    nowMinutes: Int,
) {
    val sections = state.schedule.sections
    val timeline = remember(sections, state.prayerTimes, nowMinutes) {
        buildDayTimeline(sections, state.prayerTimes, nowMinutes)
    }
    Scaffold(
        topBar = {
            DhikraHeader(bottomPadding = 12.dp) {
                TopAppBar(
                    title = {
                        ProgramSwitcher(
                            programs = state.programs,
                            active = state.program,
                            onSelect = actions.onSelectProgram,
                            onCreate = actions.onCreateProgram,
                        )
                    },
                    actions = { OverflowMenu(actions) },
                    expandedHeight = 56.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
                NextPrayerPanel(state.prayerTimes, nowMinutes, onClick = actions.onOpenPrayerTimes)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (sections.isEmpty()) {
            EmptyDay(actions.onEdit, Modifier.padding(padding))
            return@Scaffold
        }
        val listState = rememberLazyListState()
        // Opened for the first time, the day is shown from the current period.
        var placed by rememberSaveable { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            if (!placed) {
                placed = true
                val current = timeline.currentIndex
                if (current > 0) listState.scrollToItem(current)
            }
        }

        val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
        val side = ((windowWidth - MaxContentWidth) / 2).coerceAtLeast(0.dp)
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding).testTag(DayTimelineTag),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + side + 16.dp,
                end = padding.calculateEndPadding(layoutDirection) + side + 16.dp,
                top = padding.calculateTopPadding() + 16.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(timeline.periods, key = { _, period -> period.prayer?.name ?: "day" }) { _, period ->
                if (period.prayer == null) {
                    PeriodEntries(period.entries, state, onOpenSection, Modifier.animateItem())
                } else {
                    PeriodContainer(period, state, onOpenSection, Modifier.animateItem())
                }
            }
            if (timeline.untimed.isNotEmpty()) {
                item(key = "allDay") { AllDayDivider(Modifier.padding(top = 12.dp)) }
                items(timeline.untimed, key = { "allDay/" + it.id }) { section ->
                    SectionCard(section, state, onClick = { onOpenSection(section.id) }, Modifier.animateItem())
                }
            }
        }
    }
}

private val MaxContentWidth = 720.dp

// ------------------------------------------------------------------------------ Header

/**
 * The coming prayer under the app bar — its name, time, the countdown and the city — or, before
 * a city is picked, the invitation to pick one. Either opens the prayer-times screen.
 */
@Composable
private fun NextPrayerPanel(timetable: PrayerTimetable?, nowMinutes: Int, onClick: () -> Unit) {
    val faint = Color.White.copy(alpha = 0.72f)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = Color.White.copy(alpha = 0.10f),
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        if (timetable == null) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, tint = MustardLight, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.day_pick_city), style = MaterialTheme.typography.titleSmall)
            }
            return@Surface
        }
        val next = timetable.next(nowMinutes)
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(next.prayer.icon, null, tint = MustardLight, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.prayer_next_label), style = MaterialTheme.typography.labelMedium, color = faint)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(next.prayer.label), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = formatTimeArabic(next.atMinutes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = CircleShape, color = MustardLight.copy(alpha = 0.18f), contentColor = MustardLight) {
                    Text(
                        text = untilText(next.inMinutes),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocationOn, null, tint = faint, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = timetable.city.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------ Periods

/**
 * One prayer period as a container: a header with the prayer's icon, name and its time range
 * (and the countdown when its prayer is the coming one), then the sections of that time. Blue,
 * or green and outlined for the period the current time is in.
 */
@Composable
private fun PeriodContainer(
    period: DayPeriod,
    state: UiState,
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prayer = period.prayer ?: return
    val scheme = MaterialTheme.colorScheme
    val blue = LocalDhikraPalette.current.dhikr
    val container = if (period.current) scheme.primaryContainer else blue.container
    val outline = if (period.current) scheme.primary.copy(alpha = 0.6f) else blue.border
    val accent = if (period.current) scheme.primary else blue.accent
    val ink = if (period.current) scheme.onPrimaryContainer else blue.ink
    val shape = MaterialTheme.shapes.large
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(container)
            .border(if (period.current) 1.5.dp else 1.dp, outline, shape)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = if (period.entries.isEmpty()) 10.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(prayer.icon, null, tint = if (period.current) scheme.onPrimary else blue.onAccent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(prayer.label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    if (period.current) NowChip()
                }
                Text(
                    text = stringResource(R.string.day_time_range, formatTimeArabic(period.start), formatTimeArabic(period.end)),
                    style = MaterialTheme.typography.labelMedium,
                    color = ink.copy(alpha = 0.75f),
                )
            }
            period.untilPrayer?.let { minutes ->
                Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), contentColor = accent) {
                    Text(
                        text = untilText(minutes),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
            }
        }
        if (period.entries.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            PeriodEntries(period.entries, state, onOpenSection)
        }
    }
}

/** A period's sections in time order, with the current time marked among them. */
@Composable
private fun PeriodEntries(
    entries: List<PeriodEntry>,
    state: UiState,
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEach { entry ->
            when (entry) {
                is PeriodEntry.Section -> SectionCard(entry.section, state, onClick = { onOpenSection(entry.section.id) })
                is PeriodEntry.Now -> NowLine(entry.minutes)
            }
        }
    }
}

/** The current time among the sections: a dot, «الآن · ٢:٥٦ م» and a line. */
@Composable
private fun NowLine(minutes: Int) {
    val color = MaterialTheme.colorScheme.secondary
    Row(Modifier.fillMaxWidth().height(24.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.day_now_at, formatTimeArabic(minutes)),
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
        Spacer(Modifier.width(10.dp))
        HorizontalDivider(Modifier.weight(1f), thickness = 1.5.dp, color = color.copy(alpha = 0.6f))
    }
}

/** Sets the sections with no time of day apart from the day's periods: «طوال اليوم» between two rules. */
@Composable
private fun AllDayDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(Modifier.weight(1f), color = color)
        Text(
            text = stringResource(R.string.day_all_day),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp).semantics { heading() },
        )
        HorizontalDivider(Modifier.weight(1f), color = color)
    }
}

/**
 * A section of the program: its icon, title (with «الآن» when it is its time), reminder time and
 * steps done, and its progress ring. Tapping it opens the section.
 */
@Composable
private fun SectionCard(
    section: ProgramSection,
    state: UiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDhikraPalette.current
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    val now = section.id == state.nowSectionId
    val meta = scheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = if (now) BorderStroke(1.5.dp, scheme.primary) else BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = if (now) 2.dp else 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(palette.ornament.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sectionIcon(section.icon), null, tint = palette.ornamentText, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (now) NowChip()
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    section.reminderMinutes?.let { minutes ->
                        Icon(Icons.Outlined.Notifications, null, tint = meta, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(formatTimeArabic(minutes), style = MaterialTheme.typography.labelMedium, color = meta)
                        Text("  •  ", style = MaterialTheme.typography.labelMedium, color = meta.copy(alpha = 0.5f))
                    }
                    Text(
                        text = stringResource(
                            R.string.steps_done,
                            ArabicText.toArabicDigits(done),
                            ArabicText.toArabicDigits(total),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = meta,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            SectionProgress(if (total == 0) 0f else done.toFloat() / total, scheme.primary)
        }
    }
}

@Composable
private fun EmptyDay(onEdit: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DhikraLogo(size = 88.dp)
        Spacer(Modifier.height(20.dp))
        Text(stringResource(R.string.empty_program), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.empty_program_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onEdit, shape = CircleShape) {
            Icon(Icons.Filled.Add, null)
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_section))
        }
    }
}
