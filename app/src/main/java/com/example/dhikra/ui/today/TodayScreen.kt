package com.example.dhikra.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.mutableIntStateOf
import com.example.dhikra.ui.components.HeaderTab
import com.example.dhikra.ui.prayer.anchorLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalResources
import com.example.dhikra.data.Program
import com.example.dhikra.ui.editor.SectionEditorDialog
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material.icons.outlined.LocationOn
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
 * the coming prayer; below it the day reads like a calendar's daily schedule: a time column on
 * the start side with each prayer's time and a rail running through its period (الفجر until
 * الشروق, …, العشاء until the next الفجر), each prayer a slim band — the period going on in green,
 * the others in blue — the program's sections as compact event blocks in the period their time
 * falls in, and the current time as a line across. The sections with no time of day follow at
 * the bottom, under a divider. Tapping a section opens it in the section view ([onOpenSection]).
 */
@Composable
fun TodayScreen(
    state: UiState,
    actions: HomeActions,
    onOpenSection: (String) -> Unit,
    /** Changes the program: adds or duplicates a section. */
    onEditProgram: ((Program) -> Program) -> Unit = {},
    nowMinutes: Int = rememberMinuteOfDay(state.prayerTimes?.city?.timeZone),
) {
    // Another program is another day: its scroll position starts afresh.
    key(state.program.id) { DayView(state, actions, onOpenSection, onEditProgram, nowMinutes) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayView(
    state: UiState,
    actions: HomeActions,
    onOpenSection: (String) -> Unit,
    onEditProgram: ((Program) -> Program) -> Unit,
    nowMinutes: Int,
) {
    val sections = state.schedule.sections
    var addingSection by rememberSaveable { mutableStateOf(false) }
    if (addingSection) {
        SectionEditorDialog(
            initial = null,
            timetable = state.prayerTimes,
            onDismiss = { addingSection = false },
            onSave = { section ->
                onEditProgram { it.upsertSection(section) }
                addingSection = false
            },
        )
    }
    val resources = LocalResources.current
    val timeline = remember(sections, state.prayerTimes, nowMinutes) {
        buildDayTimeline(sections, state.prayerTimes, nowMinutes)
    }
    // Today's date, in the prayer city's time zone: the screen's title, the Hijri date under it.
    val zone = state.prayerTimes?.city?.timeZone
    val (gregorian, hijri) = remember(zone, state.day) { todayDates(zone) }
    // The day view's two tabs: the day at a glance, and the program's sections.
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            DhikraHeader(bottomPadding = 12.dp) {
                TopAppBar(
                    title = {
                        Column(Modifier.semantics(mergeDescendants = true) { heading() }) {
                            Text(gregorian.ifEmpty { stringResource(R.string.day_title) }, style = MaterialTheme.typography.titleLarge)
                            if (hijri.isNotEmpty()) {
                                Text(hijri, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.72f))
                            }
                        }
                    },
                    actions = { OverflowMenu(actions) },
                    expandedHeight = 56.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
                NextPrayerPanel(state.prayerTimes, nowMinutes, onClick = actions.onOpenPrayerTimes)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp).selectableGroup(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(R.string.tab_day, R.string.program_sections).forEachIndexed { index, label ->
                        HeaderTab(selected = tab == index, onClick = { tab = index }, modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(label),
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val palette = LocalDhikraPalette.current
        val scheme = MaterialTheme.colorScheme
        val rows = remember(timeline) { scheduleRows(timeline) }
        val nowSectionId = timeline.nowSectionId
        // Each tab keeps its own scroll position.
        val dayList = rememberLazyListState()
        val sectionsList = rememberLazyListState()

        // The rail's colour along each period: green for the current one, blue for the others,
        // faded once over; neutral when there are no prayer times.
        val blue = palette.dhikr.accent
        fun railColor(period: DayPeriod): Color = when {
            period.prayer == null -> scheme.outlineVariant
            period.current -> scheme.primary
            period.passed -> blue.copy(alpha = 0.3f)
            else -> blue.copy(alpha = 0.65f)
        }

        val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
        val side = ((windowWidth - MaxContentWidth) / 2).coerceAtLeast(0.dp)
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            state = if (tab == 0) dayList else sectionsList,
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding).testTag(DayTimelineTag),
            contentPadding = PaddingValues(
                start = padding.calculateStartPadding(layoutDirection) + side + 8.dp,
                end = padding.calculateEndPadding(layoutDirection) + side + 16.dp,
                top = padding.calculateTopPadding() + 12.dp,
                bottom = padding.calculateBottomPadding() + 32.dp,
            ),
        ) {
            if (tab == 1) {
                // The program's sections, to open, add, duplicate or edit.
                item(key = "sectionsTitle") {
                    BlockTitle(
                        title = stringResource(R.string.program_sections),
                        modifier = Modifier.padding(start = 8.dp),
                        action = stringResource(R.string.add_section) to { addingSection = true },
                    )
                }
                if (sections.isEmpty()) {
                    item(key = "empty") { EmptyDay(actions.onEdit, Modifier.padding(top = 24.dp)) }
                }
                items(sections, key = { "list/" + it.id }) { section ->
                    SectionListRow(
                        section = section,
                        state = state,
                        onOpen = { onOpenSection(section.id) },
                        onEdit = { actions.onEditSection(section.id) },
                        onDuplicate = {
                            onEditProgram { it.duplicateSection(section.id) { title -> resources.getString(R.string.section_copy, title) } }
                        },
                        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp).animateItem(),
                    )
                }
                return@LazyColumn
            }
            // The day: today's progress, the program followed, the prayer times with their sections.
            item(key = "progress") { ProgressCard(sections, state, Modifier.padding(start = 8.dp, bottom = 10.dp)) }
            item(key = "program") {
                ProgramCard(
                    state = state,
                    onSelect = actions.onSelectProgram,
                    onCreate = actions.onCreateProgram,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (sections.isEmpty()) {
                item(key = "empty") { EmptyDay(actions.onEdit, Modifier.padding(top = 24.dp)) }
                return@LazyColumn
            }
            // 3. The prayer times with the sections they hold.
            item(key = "prayerTitle") { BlockTitle(stringResource(R.string.prayer_times), Modifier.padding(start = 8.dp)) }
            items(rows, key = { it.key }) { row ->
                when (row) {
                    is ScheduleItem.PrayerBand -> ScheduleRow(
                        top = row.previous?.let(::railColor),
                        bottom = railColor(row.period).takeIf { row.continues },
                        dot = railColor(row.period).copy(alpha = if (row.period.passed) 0.45f else 1f),
                        gutter = { GutterTime(row.period.start, current = row.period.current, passed = row.period.passed) },
                        spaceAbove = if (row.first) 0.dp else 8.dp,
                        modifier = Modifier.animateItem(),
                    ) { PrayerBand(row.period) }
                    is ScheduleItem.Entry -> {
                        val color = railColor(row.period)
                        when (val entry = row.entry) {
                            is PeriodEntry.Now -> ScheduleRow(
                                top = color,
                                bottom = color.takeIf { !row.last },
                                dot = NowColor(),
                                gutter = { GutterTime(entry.minutes, color = NowColor()) },
                            ) { NowLine() }
                            is PeriodEntry.Section -> ScheduleRow(
                                top = color.takeIf { !row.firstOfDay },
                                bottom = color.takeIf { !row.last },
                                modifier = Modifier.animateItem(),
                            ) {
                                SectionChip(
                                    section = entry.section,
                                    state = state,
                                    now = entry.section.id == nowSectionId,
                                    colors = timedChipColors(),
                                    onClick = { onOpenSection(entry.section.id) },
                                )
                            }
                        }
                    }
                    ScheduleItem.AllDayLabel -> AllDayDivider(Modifier.padding(top = 18.dp, bottom = 8.dp))
                    is ScheduleItem.AllDay -> ScheduleRow(modifier = Modifier.animateItem()) {
                        SectionChip(
                            section = row.section,
                            state = state,
                            now = false,
                            colors = allDayChipColors(),
                            onClick = { onOpenSection(row.section.id) },
                        )
                    }
                }
            }
        }
    }
}

private val MaxContentWidth = 720.dp

// ------------------------------------------------------------------------------ Overview

/** A block's heading on the day view («مواقيت الصلاة», «أقسام البرنامج»), with an optional action. */
@Composable
private fun BlockTitle(title: String, modifier: Modifier = Modifier, action: Pair<String, () -> Unit>? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        action?.let { (label, onClick) ->
            TextButton(onClick = onClick) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(label)
            }
        }
    }
}

/** Today's progress through the whole program: a ring with the share done, steps and sections. */
@Composable
private fun ProgressCard(sections: List<ProgramSection>, state: UiState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val total = sections.sumOf { it.trackableItems.size }
    val done = sections.sumOf { state.completedIn(it) }
    val withSteps = sections.filter { it.trackableItems.isNotEmpty() }
    val complete = withSteps.count { state.completedIn(it) == it.trackableItems.size }
    val fraction = if (total == 0) 0f else done.toFloat() / total
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    color = scheme.primary,
                    trackColor = scheme.primary.copy(alpha = 0.14f),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Text(
                    text = stringResource(R.string.progress_percent, ArabicText.toArabicDigits(Math.round(fraction * 100))),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_progress), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.progress_steps, ArabicText.toArabicDigits(done), ArabicText.toArabicDigits(total)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.progress_sections, ArabicText.toArabicDigits(complete), ArabicText.toArabicDigits(withSteps.size)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The program being followed: tapping it lists the user's programs to switch to, and «+» starts
 * a new one.
 */
@Composable
private fun ProgramCard(
    state: UiState,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val defaultName = stringResource(R.string.default_program_name)
    val program = state.program
    Box(modifier.fillMaxWidth()) {
        Surface(
            onClick = { open = true },
            shape = MaterialTheme.shapes.large,
            color = scheme.surfaceContainerLowest,
            border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, null, tint = scheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.program_current), style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = program.name.ifBlank { defaultName },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        Icon(Icons.Filled.ArrowDropDown, stringResource(R.string.program_change), tint = scheme.onSurfaceVariant)
                    }
                    Text(
                        text = stringResource(
                            R.string.program_summary,
                            ArabicText.toArabicDigits(program.sections.size),
                            ArabicText.toArabicDigits(program.itemCount),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onCreate) {
                    Icon(Icons.Filled.Add, stringResource(R.string.new_program), tint = scheme.primary)
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            state.programs.forEach { other ->
                val current = other.id == program.id
                DropdownMenuItem(
                    text = { Text(other.name.ifBlank { defaultName }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (current) Icon(Icons.Filled.Check, null) else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        onSelect(other.id)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_program)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
                onClick = {
                    open = false
                    onCreate()
                },
            )
        }
    }
}

/**
 * A section in the program's list: its icon, title, time and steps, and its menu — edit it (the
 * editor on this section only) or duplicate it. Tapping it opens the section.
 */
@Composable
private fun SectionListRow(
    section: ProgramSection,
    state: UiState,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onOpen,
        shape = MaterialTheme.shapes.medium,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 12.dp, end = 2.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(scheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(sectionIcon(section.icon), null, tint = scheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOfNotNull(
                        section.anchor?.let { anchorLabel(it) },
                        section.reminderMinutes?.let(::formatTimeArabic) ?: stringResource(R.string.no_reminder),
                        stringResource(R.string.section_steps_count, ArabicText.toArabicDigits(total)),
                        stringResource(R.string.steps_done, ArabicText.toArabicDigits(done), ArabicText.toArabicDigits(total)),
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit_section)) },
                        leadingIcon = { Icon(Icons.Filled.Edit, null) },
                        onClick = { menu = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.duplicate)) },
                        leadingIcon = { Icon(Icons.Filled.Add, null) },
                        onClick = { menu = false; onDuplicate() },
                    )
                }
            }
        }
    }
}

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

// ------------------------------------------------------------------------------ Schedule

/** The rows of the day's schedule, in order, with what each needs to draw its piece of the rail. */
private sealed interface ScheduleItem {
    val key: String

    /** A prayer opening its period; [previous] is the period before it (the rail comes down from it). */
    data class PrayerBand(val period: DayPeriod, val previous: DayPeriod?, val first: Boolean, val continues: Boolean) : ScheduleItem {
        override val key = "prayer/" + period.prayer?.name
    }

    /** A section or the current time inside [period]; [last] ends the day's rail. */
    data class Entry(val period: DayPeriod, val entry: PeriodEntry, val firstOfDay: Boolean, val last: Boolean) : ScheduleItem {
        override val key = when (entry) {
            is PeriodEntry.Section -> "section/" + entry.section.id
            is PeriodEntry.Now -> "now"
        }
    }

    data object AllDayLabel : ScheduleItem {
        override val key = "allDay"
    }

    data class AllDay(val section: ProgramSection) : ScheduleItem {
        override val key = "allDay/" + section.id
    }
}

private fun scheduleRows(timeline: DayTimeline): List<ScheduleItem> = buildList {
    val periods = timeline.periods
    periods.forEachIndexed { index, period ->
        val lastPeriod = index == periods.lastIndex
        if (period.prayer != null) {
            add(ScheduleItem.PrayerBand(period, periods.getOrNull(index - 1), first = index == 0, continues = !lastPeriod || period.entries.isNotEmpty()))
        }
        period.entries.forEachIndexed { at, entry ->
            add(
                ScheduleItem.Entry(
                    period = period,
                    entry = entry,
                    firstOfDay = period.prayer == null && at == 0,
                    last = lastPeriod && at == period.entries.lastIndex,
                )
            )
        }
    }
    if (timeline.untimed.isNotEmpty()) {
        add(ScheduleItem.AllDayLabel)
        timeline.untimed.forEach { add(ScheduleItem.AllDay(it)) }
    }
}

/** Width of the time column on the start side; the rail runs [RailInset] from its inner edge. */
private val GutterWidth = 64.dp
private val RailInset = 9.dp
private val RailWidth = 3.dp

@Composable
private fun NowColor(): Color = MaterialTheme.colorScheme.secondary

/**
 * One row of the schedule, like a calendar's: the time column (a [gutter] label and its rail
 * segments — [top] half and [bottom] half, each in its period's colour, with an optional [dot] at
 * the middle) and the row's [content]. [spaceAbove] separates the row from the one before while
 * the rail runs on through it.
 */
@Composable
private fun ScheduleRow(
    modifier: Modifier = Modifier,
    spaceAbove: Dp = 0.dp,
    top: Color? = null,
    bottom: Color? = null,
    dot: Color? = null,
    gutter: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(GutterWidth)
                .fillMaxHeight()
                .drawBehind {
                    // The inner edge of the time column: the left in Arabic, the right in LTR.
                    val x = if (layoutDirection == LayoutDirection.Rtl) RailInset.toPx() else size.width - RailInset.toPx()
                    val space = spaceAbove.toPx()
                    val middle = space + (size.height - space) / 2f
                    val width = RailWidth.toPx()
                    top?.let { drawLine(it, Offset(x, 0f), Offset(x, middle), width) }
                    bottom?.let { drawLine(it, Offset(x, middle), Offset(x, size.height), width) }
                    dot?.let { drawCircle(it, radius = 5.dp.toPx(), center = Offset(x, middle)) }
                }
                .padding(start = 2.dp, end = RailInset * 2 + 2.dp, top = spaceAbove),
            contentAlignment = Alignment.CenterStart,
        ) { gutter() }
        Box(Modifier.weight(1f).padding(top = spaceAbove + 3.dp, bottom = 3.dp)) { content() }
    }
}

/** A time in the time column, like a calendar's hour label. */
@Composable
private fun GutterTime(
    minutes: Int,
    current: Boolean = false,
    passed: Boolean = false,
    color: Color? = null,
) {
    val scheme = MaterialTheme.colorScheme
    Text(
        text = formatTimeArabic(minutes),
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = if (current || color != null) FontWeight.SemiBold else null,
        color = color ?: when {
            current -> scheme.primary
            passed -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
            else -> scheme.onSurfaceVariant
        },
        maxLines = 1,
        softWrap = false,
    )
}

/**
 * A prayer as the slim band that opens its period: its icon, name, the end of the period and, for
 * the coming prayer, the countdown. Blue, green for the period going on, faded once over.
 */
@Composable
private fun PrayerBand(period: DayPeriod) {
    val prayer = period.prayer ?: return
    val scheme = MaterialTheme.colorScheme
    val blue = LocalDhikraPalette.current.dhikr
    val tint = if (period.current) scheme.primaryContainer else blue.container
    val accent = if (period.current) scheme.primary else blue.accent
    val ink = if (period.current) scheme.onPrimaryContainer else blue.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 42.dp)
            .alpha(if (period.passed) 0.6f else 1f)
            .clip(MaterialTheme.shapes.medium)
            .background(tint)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(prayer.icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(prayer.label),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
        )
        if (period.current) NowChip()
        Spacer(Modifier.weight(1f))
        period.untilPrayer?.let { minutes ->
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), contentColor = accent) {
                Text(
                    text = untilText(minutes),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = stringResource(R.string.day_until, formatTimeArabic(period.end)),
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.72f),
            maxLines = 1,
        )
    }
}

/** The colours of a section block: green for the day's timed sections, sand for the all-day ones. */
private data class ChipColors(val container: Color, val border: Color, val accent: Color, val onAccent: Color, val ink: Color)

@Composable
private fun timedChipColors(): ChipColors {
    val scheme = MaterialTheme.colorScheme
    return ChipColors(
        container = scheme.surfaceContainerLowest,
        border = scheme.primary.copy(alpha = 0.22f),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        ink = scheme.onSurface,
    )
}

@Composable
private fun allDayChipColors(): ChipColors {
    val sand = LocalDhikraPalette.current.quran
    return ChipColors(
        container = sand.container,
        border = sand.border,
        accent = sand.accent,
        onAccent = sand.onAccent,
        ink = sand.ink,
    )
}

/**
 * A section as an actionable set of tasks, the same everywhere in the calendar: a strip and
 * icon in its colours, the title (with «الآن» while it is its time), its time and steps done, a
 * progress bar, and an action pill saying where it stands — «ابدأ», «تابع» or «تمّ». The whole
 * block opens the section.
 */
@Composable
private fun SectionChip(
    section: ProgramSection,
    state: UiState,
    now: Boolean,
    colors: ChipColors,
    onClick: () -> Unit,
) {
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    // A finished set turns golden, like the finished steps inside it.
    val finished = total > 0 && done >= total
    val palette = LocalDhikraPalette.current
    val accent = if (finished) palette.done else colors.accent
    val meta = listOfNotNull(
        section.reminderMinutes?.let(::formatTimeArabic),
        stringResource(R.string.steps_done, ArabicText.toArabicDigits(done), ArabicText.toArabicDigits(total)),
    ).joinToString("  ·  ")
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = colors.container,
        border = BorderStroke(1.dp, if (finished) palette.done.copy(alpha = 0.6f) else colors.border),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(accent))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 9.dp, bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(accent.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(sectionIcon(section.icon), null, tint = accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.ink,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (now) NowChip()
                        }
                        Text(meta, style = MaterialTheme.typography.labelSmall, color = colors.ink.copy(alpha = 0.7f), maxLines = 1)
                    }
                    Spacer(Modifier.width(8.dp))
                    ActionPill(done = done, total = total, colors = colors.copy(accent = accent))
                }
                if (total > 0) {
                    LinearProgressIndicator(
                        progress = { done.toFloat() / total },
                        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp).height(3.dp),
                        color = accent,
                        trackColor = accent.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

/** Where a set of tasks stands, as its call to action: «ابدأ», «تابع», or «تمّ» once all are done. */
@Composable
private fun ActionPill(done: Int, total: Int, colors: ChipColors) {
    if (total == 0) return
    val finished = done >= total
    Surface(
        shape = CircleShape,
        color = if (finished) colors.accent.copy(alpha = 0.13f) else colors.accent,
        contentColor = if (finished) colors.accent else colors.onAccent,
    ) {
        Row(
            modifier = Modifier.height(30.dp).padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (finished) Icons.Filled.Check else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(
                    when {
                        finished -> R.string.action_set_done
                        done > 0 -> R.string.action_set_continue
                        else -> R.string.action_set_start
                    }
                ),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** The current time across the schedule, like a calendar's: a line from the time column's dot. */
@Composable
private fun NowLine() {
    Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
        HorizontalDivider(thickness = 1.5.dp, color = NowColor())
    }
}

/** Today's Gregorian date (with the weekday) and Hijri (Umm al-Qura) date, in Arabic, in [timeZone]. */
private fun todayDates(timeZone: String?): Pair<String, String> {
    val zone = android.icu.util.TimeZone.getTimeZone(timeZone ?: java.util.TimeZone.getDefault().id)
    val now = java.util.Date()
    fun format(style: Int, locale: String) =
        android.icu.text.DateFormat.getDateInstance(style, android.icu.util.ULocale(locale))
            .apply { this.timeZone = zone }
            .format(now)
    return runCatching {
        format(android.icu.text.DateFormat.FULL, "ar@numbers=arab") to
            format(android.icu.text.DateFormat.LONG, "ar@calendar=islamic-umalqura;numbers=arab")
    }.getOrDefault("" to "")
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
