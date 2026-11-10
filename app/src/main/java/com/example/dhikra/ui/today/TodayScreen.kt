package com.example.dhikra.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.mutableIntStateOf
import com.example.dhikra.ui.prayer.anchorShortLabel
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
import com.example.dhikra.ui.formatTime
import com.example.dhikra.ui.home.HomeActions
import com.example.dhikra.ui.home.OverflowMenu
import com.example.dhikra.ui.prayer.icon
import com.example.dhikra.ui.prayer.label
import com.example.dhikra.ui.prayer.rememberMinuteOfDay
import com.example.dhikra.ui.prayer.untilText
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.MustardLight
import com.example.dhikra.ui.uiDigits
import com.example.dhikra.ui.prayer.shownName
import com.example.dhikra.ui.AppLanguage
import com.example.dhikra.ui.library.libraryCollections
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.dhikra.ui.components.LibraryIcon
import com.example.dhikra.ui.components.TodayIcon
import com.example.dhikra.ui.components.ProgramIcon
import com.example.dhikra.ui.home.ProgramSwitcher
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawOutline
import com.example.dhikra.data.DailyCounter
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.data.Library
import com.example.dhikra.data.ItemType
import com.example.dhikra.ui.components.FavouriteIcon
import com.example.dhikra.ui.library.EntryCard
import com.example.dhikra.ui.library.GroupEntryCard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.ExtendedFloatingActionButton
import com.example.dhikra.ui.formatClock
import com.example.dhikra.ui.components.SectionIcon
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.ExperimentalFoundationApi
import com.example.dhikra.ui.components.HomeIcon
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalMinimumInteractiveComponentSize

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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    var pickingCounter by rememberSaveable { mutableStateOf(false) }
    if (pickingCounter) {
        CounterPickerDialog(
            library = state.library,
            added = state.counters,
            onPick = { entryId ->
                actions.onAddCounter(entryId)
                pickingCounter = false
            },
            onDismiss = { pickingCounter = false },
        )
    }
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
    // Today's date, in the prayer city's time zone, under the program's name.
    val zone = state.prayerTimes?.city?.timeZone
    val dateLine = remember(zone, state.day) { todayDates(zone).toList().filter { it.isNotEmpty() }.joinToString("  ·  ") }
    // The day view's three tabs, also swiped between: the day, the program's sections, the library.
    val pager = rememberPagerState { DayTab.entries.size }
    val scope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            DhikraHeader(bottomPadding = 10.dp) {
                TopAppBar(
                    title = {
                        // The program followed (tap to switch or start another), and today's date.
                        Column {
                            ProgramSwitcher(
                                programs = state.programs,
                                active = state.program,
                                onSelect = actions.onSelectProgram,
                                onCreate = actions.onCreateProgram,
                            )
                            if (dateLine.isNotEmpty()) {
                                Text(
                                    text = dateLine,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.72f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    },
                    actions = { OverflowMenu(actions) },
                    expandedHeight = 64.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
                NextPrayerPanel(state.prayerTimes, nowMinutes, onClick = actions.onOpenPrayerTimes)
                DayTabBar(
                    selected = pager.targetPage,
                    onSelect = { page -> scope.launch { pager.animateScrollToPage(page) } },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
                )
            }
        },
        // The tab's main action floats in the bottom corner: adding a section to the program.
        floatingActionButton = {
            when (DayTab.entries.getOrNull(pager.targetPage)) {
                DayTab.Program -> MainActionButton(Icons.Filled.Add, stringResource(R.string.add_section)) { addingSection = true }
                else -> {}
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val palette = LocalDhikraPalette.current
        val scheme = MaterialTheme.colorScheme
        val rows = remember(timeline) { scheduleRows(timeline) }

        // The rail's colour along each period: blue, strongest for the current one and faded once
        // over; neutral when there are no prayer times.
        val blue = palette.slate.accent
        fun railColor(period: DayPeriod): Color = when {
            period.prayer == null -> scheme.outlineVariant
            period.current -> blue
            period.passed -> blue.copy(alpha = 0.3f)
            else -> blue.copy(alpha = 0.65f)
        }

        val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
        val side = ((windowWidth - MaxContentWidth) / 2).coerceAtLeast(0.dp)
        val layoutDirection = LocalLayoutDirection.current
        val contentPadding = PaddingValues(
            start = padding.calculateStartPadding(layoutDirection) + side + 8.dp,
            end = padding.calculateEndPadding(layoutDirection) + side + 16.dp,
            top = padding.calculateTopPadding() + 12.dp,
            // Room under the last row for the floating action button.
            bottom = padding.calculateBottomPadding() + 96.dp,
        )
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize().consumeWindowInsets(padding),
            beyondViewportPageCount = 1,
            key = { DayTab.entries[it].name },
        ) { page ->
            when (DayTab.entries[page]) {
                DayTab.Program -> LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                    // The program's sections, to open, add, duplicate or edit.
                    item(key = "sectionsTitle") {
                        BlockTitle(
                            title = stringResource(R.string.program_sections),
                            subtitle = stringResource(
                                R.string.program_summary,
                                uiDigits(state.program.sections.size),
                                uiDigits(state.program.itemCount),
                            ),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    if (sections.isEmpty()) {
                        item(key = "empty") { EmptyDay(actions.onEdit, Modifier.padding(top = 24.dp)) }
                    }
                    items(timeline.sectionsInOrder(sections), key = { "list/" + it.id }) { section ->
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
                }
                DayTab.Home -> LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                    // What to do now, as three panels: today's progress, the sections of the period at
                    // hand, and the daily counters.
                    item(key = "progress") { ProgressCard(sections, state, Modifier.padding(start = 8.dp)) }
                    item(key = "now") {
                        // The period going on, or the next one with a section still to do.
                        val nowPeriod = timeline.periodAtHand { state.completedIn(it) >= it.trackableItems.size }
                        val next = timeline.periods.getOrNull(timeline.periods.indexOf(nowPeriod) + 1)
                        HomePanel(
                            title = stringResource(R.string.home_now),
                            // The colour of time, as the calendar's prayer bands.
                            accent = palette.slate.accent,
                            subtitle = nowPeriod?.let { periodLine(it, next) },
                            action = HomeAction(stringResource(R.string.home_full_schedule), Icons.AutoMirrored.Filled.ArrowForward, trailing = true) {
                                scope.launch { pager.animateScrollToPage(DayTab.Calendar.ordinal) }
                            },
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                        ) {
                            val nowSections = nowPeriod?.sections.orEmpty()
                            if (nowSections.isEmpty()) {
                                PanelNote(stringResource(if (sections.isEmpty()) R.string.home_now_no_program else R.string.home_now_empty))
                            }
                            nowSections.forEachIndexed { index, section ->
                                if (index > 0) PanelDivider()
                                NowSectionRow(section, state, onClick = { onOpenSection(section.id) })
                            }
                        }
                    }
                    item(key = "counters") {
                        // The daily counter: open-ended counts of chosen adhkar, starting over each day.
                        HomePanel(
                            title = stringResource(R.string.counter_title),
                            // The colour of the counters' rings and targets.
                            accent = palette.progress,
                            action = HomeAction(stringResource(R.string.counter_add), Icons.Filled.Add) { pickingCounter = true },
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                        ) {
                            val counted = state.counters.mapNotNull(state.library::entry)
                            // What the counter is, said while it is empty: its rows show it once in use.
                            if (counted.isEmpty()) PanelNote(stringResource(R.string.counter_empty))
                            counted.forEachIndexed { index, entry ->
                                if (index > 0) PanelDivider()
                                CounterRow(
                                    entry = entry,
                                    count = state.counterCount(entry.id),
                                    onOpen = { actions.onOpenCounter(entry.id) },
                                    onRemove = { actions.onRemoveCounter(entry.id) },
                                )
                            }
                        }
                    }
                }
                DayTab.Favourites -> LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                    item(key = "favouritesTitle") {
                        BlockTitle(
                            title = stringResource(R.string.favourites_title),
                            subtitle = stringResource(R.string.favourites_subtitle),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    val library = state.library
                    val shown = state.favourites.filter { ref ->
                        val id = ref.substringAfter(':')
                        if (ref.startsWith("group:")) library.group(id) != null else library.entry(id) != null
                    }
                    if (shown.isEmpty()) {
                        item(key = "favouritesEmpty") { EmptyNote(stringResource(R.string.favourites_empty), Modifier.padding(start = 8.dp)) }
                    }
                    items(shown, key = { "favourite/$it" }) { ref ->
                        val id = ref.substringAfter(':')
                        val cardModifier = Modifier.padding(start = 8.dp, bottom = 10.dp).animateItem()
                        val group = if (ref.startsWith("group:")) library.group(id) else null
                        if (group != null) {
                            GroupEntryCard(
                                group = group,
                                library = library,
                                onClick = { actions.onOpenFavourite(ref) },
                                favourite = true,
                                onToggleFavourite = { actions.onToggleFavourite(ref) },
                                modifier = cardModifier,
                            )
                        } else {
                            library.entry(id)?.let { entry ->
                                EntryCard(
                                    entry = entry,
                                    onClick = { actions.onOpenFavourite(ref) },
                                    favourite = true,
                                    onToggleFavourite = { actions.onToggleFavourite(ref) },
                                    modifier = cardModifier,
                                )
                            }
                        }
                    }
                }
                DayTab.Library -> LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
                    // The collections the steps read from, each opening on its texts and groups.
                    item(key = "libraryGap") { Spacer(Modifier.height(4.dp)) }
                    // The schedule's rail side has 8 dp less padding: the same margin on both sides.
                    libraryCollections(state.library, actions.onOpenCollection, Modifier.padding(start = 8.dp))
                }
                // The day's list starts under the header (and its rounded lower edge) rather than
                // behind it: its prayer bands pin to the list's top, which must be in view.
                DayTab.Calendar -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding() + 8.dp).testTag(DayTimelineTag),
                    contentPadding = PaddingValues(
                        start = contentPadding.calculateStartPadding(layoutDirection),
                        end = contentPadding.calculateEndPadding(layoutDirection),
                        top = 4.dp,
                        bottom = contentPadding.calculateBottomPadding(),
                    ),
                ) {
                    // The whole day: the prayer times with the sections they hold.
                    if (sections.isEmpty()) {
                        item(key = "empty") { EmptyDay(actions.onEdit, Modifier.padding(top = 24.dp)) }
                        return@LazyColumn
                    }
                    item(key = "prayerTitle") { ScheduleHeading(Modifier.padding(start = 8.dp, top = 4.dp)) }
                // One row of the schedule. A prayer's band is a sticky header: it stays on top while
                // its period's sections scroll under it, until the next prayer's band takes its place,
                // so the top of the list always names the period in view.
                val scheduleRow: @Composable LazyItemScope.(ScheduleItem) -> Unit = { row ->
                    when (row) {
                        is ScheduleItem.PrayerBand -> ScheduleRow(
                            top = row.previous?.let(::railColor),
                            bottom = railColor(row.period).takeIf { row.continues },
                            dot = railColor(row.period).copy(alpha = if (row.period.passed) 0.45f else 1f),
                            gutter = { GutterTime(row.period.start, current = row.period.current, passed = row.period.passed) },
                            spaceAbove = if (row.first) 0.dp else 8.dp,
                            // Opaque: the sections scroll under it while it is pinned.
                            modifier = Modifier.animateItem().background(scheme.background),
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
                                colors = timedChipColors(),
                                onClick = { onOpenSection(row.section.id) },
                            )
                        }
                    }
                }
                rows.forEach { row ->
                    if (row is ScheduleItem.PrayerBand) {
                        stickyHeader(key = row.key, contentType = "band") { scheduleRow(row) }
                    } else {
                        item(key = row.key) { scheduleRow(row) }
                    }
                }
                }
            }
        }
    }
}

/** The day view's tabs, in order: their icon, and their name for accessibility. */
private enum class DayTab(val icon: ImageVector, val label: Int) {
    Home(HomeIcon, R.string.tab_home),
    Calendar(TodayIcon, R.string.tab_schedule),
    Program(ProgramIcon, R.string.tab_program),
    Favourites(FavouriteIcon, R.string.tab_favourites),
    Library(LibraryIcon, R.string.library),
}

private val MaxContentWidth = 720.dp

/**
 * The sections in the calendar's order — through the day's prayer periods by time, from Fajr to
 * the night, then the ones with no time — so the program's list reads like its schedule.
 */
private fun DayTimeline.sectionsInOrder(all: List<ProgramSection>): List<ProgramSection> {
    val ordered = periods.flatMap { period -> period.entries.filterIsInstance<PeriodEntry.Section>().map { it.section } } + untimed
    val ids = ordered.mapTo(mutableSetOf()) { it.id }
    return ordered.distinctBy { it.id } + all.filterNot { it.id in ids }
}

// ------------------------------------------------------------------------------ Counter and favourites

/** A short explanation where a list is still empty. */
@Composable
private fun EmptyNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(top = 8.dp, end = 8.dp),
    )
}

/**
 * A dhikr of the daily counter, as a row of the counters panel: today's count in a ring filling
 * towards the next target, the dhikr's title and the target. Tapping it opens its counter; its menu
 * takes it off.
 */
@Composable
private fun CounterRow(
    entry: LibraryEntry,
    count: Int,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDhikraPalette.current
    val target = DailyCounter.target(count)
    var menu by remember { mutableStateOf(false) }
    val description = stringResource(R.string.counter_count_description, entry.title, uiDigits(count), uiDigits(target))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(48.dp).semantics(mergeDescendants = true) { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { DailyCounter.stage(count) },
                modifier = Modifier.fillMaxSize(),
                color = palette.progress,
                trackColor = scheme.primary.copy(alpha = 0.12f),
                strokeWidth = 3.5.dp,
                strokeCap = StrokeCap.Round,
            )
            Text(
                text = uiDigits(count),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = stringResource(R.string.counter_target, uiDigits(target)),
                style = MaterialTheme.typography.labelMedium,
                color = palette.progress,
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.counter_remove)) },
                    leadingIcon = { Icon(Icons.Filled.Close, null) },
                    onClick = { menu = false; onRemove() },
                )
            }
        }
    }
}

/** A panel header's action: a text button with its icon before ([trailing] false) or after the label. */
private data class HomeAction(val label: String, val icon: ImageVector, val trailing: Boolean = false, val onClick: () -> Unit)

/**
 * A panel of the home tab, like the progress card: white, outlined, its [title] and [subtitle] in
 * a header with an optional [action] at its end, then its rows under a divider. The header is
 * tinted with the panel's [accent] — the colour of what it holds — and its title and action are
 * written in it (deepened on a light theme, to read).
 */
@Composable
private fun HomePanel(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: HomeAction? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val ink = if (scheme.surface.luminance() > 0.5f) lerp(accent, Color.Black, 0.22f) else accent
    val headerTint = accent.copy(alpha = 0.09f).compositeOver(scheme.surfaceContainerLowest)
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier.background(headerTint).padding(start = 16.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = ink)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    }
                }
                action?.let { (label, icon, trailing, onClick) ->
                    // A compact button, so the header stays a slim bar: 44 dp to touch.
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 44.dp) {
                    TextButton(
                        onClick = onClick,
                        colors = ButtonDefaults.textButtonColors(contentColor = ink),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(44.dp),
                    ) {
                        if (!trailing) {
                            Icon(icon, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(label)
                        if (trailing) {
                            Spacer(Modifier.width(4.dp))
                            Icon(icon, null, Modifier.size(18.dp))
                        }
                    }
                    }
                }
            }
            HorizontalDivider(color = accent.copy(alpha = 0.22f))
            content()
        }
    }
}

/** The line between two rows of a panel, inset from its edges. */
@Composable
private fun PanelDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        modifier = Modifier.padding(horizontal = 14.dp),
    )
}

/** A panel's note when it has no rows. */
@Composable
private fun PanelNote(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

/**
 * Under «الآن»: the period's prayer, and how long until it, or until the next prayer when it is
 * going on — «العصر · المغرب بعد ٣ د».
 */
@Composable
private fun periodLine(period: DayPeriod, next: DayPeriod?): String? {
    val prayer = period.prayer ?: return null
    val name = stringResource(prayer.label)
    val until = when {
        period.untilPrayer != null -> untilText(period.untilPrayer)
        period.current && next?.prayer != null && next.untilPrayer != null ->
            stringResource(next.prayer.label) + " " + untilText(next.untilPrayer)
        else -> null
    }
    return listOfNotNull(name, until).joinToString("  ·  ")
}

/**
 * A section in the «الآن» panel: its icon, title and time, and the steps done of all («٣/٩») — the
 * icon and a check turn gold once it is finished. The row opens the section.
 */
@Composable
private fun NowSectionRow(section: ProgramSection, state: UiState, onClick: () -> Unit) {
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    val finished = total > 0 && done >= total
    val scheme = MaterialTheme.colorScheme
    val accent = if (finished) LocalDhikraPalette.current.doneText else scheme.primary
    val steps = stringResource(R.string.steps_done, uiDigits(done), uiDigits(total))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { if (total > 0) stateDescription = steps }
            .padding(start = 14.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(SectionIcon, null, tint = accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface.copy(alpha = if (finished) 0.75f else 1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            section.reminderMinutes?.let {
                Text(formatClock(it), style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
            }
        }
        if (finished) {
            Icon(Icons.Filled.Check, stringResource(R.string.section_finished), tint = accent, modifier = Modifier.size(22.dp))
        } else if (total > 0) {
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.1f),
                contentColor = accent,
                modifier = Modifier.clearAndSetSemantics {},
            ) {
                Text(
                    text = uiDigits("$done/$total"),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** Picks a dhikr of the library for the daily counter; the ones already on it are ticked. */
@Composable
private fun CounterPickerDialog(library: Library, added: List<String>, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val adhkar = library.ofType(ItemType.DHIKR)
    val key = ArabicText.searchKey(query)
    val shown = if (key.isEmpty()) adhkar else adhkar.filter { ArabicText.searchKey(it.title + " " + it.text).contains(key) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.counter_pick)) },
        text = {
            Column {
                if (adhkar.size > 6) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text(stringResource(R.string.search)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
                if (adhkar.isEmpty()) Text(stringResource(R.string.counter_pick_empty))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(shown, key = { it.id }) { entry ->
                        val isAdded = entry.id in added
                        Surface(
                            onClick = { onPick(entry.id) },
                            enabled = !isAdded,
                            shape = MaterialTheme.shapes.small,
                            color = Color.Transparent,
                        ) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (entry.text != entry.title) {
                                        Text(
                                            text = entry.text,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                if (isAdded) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

// ------------------------------------------------------------------------------ Overview

/** A block's heading on the day view («الآن», «أقسام البرنامج»), with an optional line under it and action. */
@Composable
private fun BlockTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: Pair<String, () -> Unit>? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp).heightIn(min = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (subtitle != null) {
                Text(text = subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        action?.let { (label, onClick) ->
            TextButton(onClick = onClick) {
                Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(label)
            }
        }
    }
}

/**
 * The schedule's heading at the top of the calendar: the title followed by a rule fading out to the
 * end, like a chapter's, with what the schedule shows under it.
 */
@Composable
private fun ScheduleHeading(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rule = scheme.primary.copy(alpha = 0.35f)
    Column(modifier.fillMaxWidth().padding(bottom = 10.dp).semantics(mergeDescendants = true) { heading() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.day_schedule), style = MaterialTheme.typography.titleMedium, color = scheme.primary)
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(Brush.horizontalGradient(if (rtl) listOf(Color.Transparent, rule) else listOf(rule, Color.Transparent)))
            )
        }
        Text(
            text = stringResource(R.string.day_schedule_hint),
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
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
                    text = stringResource(R.string.progress_percent, uiDigits(Math.round(fraction * 100))),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.primary,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_progress), style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.progress_steps, uiDigits(done), uiDigits(total)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.progress_sections, uiDigits(complete), uiDigits(withSteps.size)),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
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
                    // «الشروق بعد ٢٠ د · ٧:٤٣ ص · أُنجز ٣ من ٦»: when, and how far along.
                    text = listOfNotNull(
                        section.anchor?.let { anchorShortLabel(it) },
                        section.reminderMinutes?.let(::formatTime) ?: stringResource(R.string.no_reminder),
                        stringResource(R.string.steps_done, uiDigits(done), uiDigits(total)),
                    ).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
 * The coming prayer under the app bar — its name and time, the countdown and the city — or, before
 * a city is picked, the invitation to pick one. Either opens the prayer-times screen.
 */
@Composable
private fun NextPrayerPanel(timetable: PrayerTimetable?, nowMinutes: Int, onClick: () -> Unit) {
    val faint = Color.White.copy(alpha = 0.72f)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = HeaderGlass,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
    ) {
        if (timetable == null) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
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
                        text = formatTime(next.atMinutes),
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
                        text = timetable.city.shownName,
                        style = MaterialTheme.typography.labelMedium,
                        color = faint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 140.dp),
                    )
                }
            }
        }
    }
}

/** The translucent white of the header's panels (the next prayer, the tabs). */
private val HeaderGlass = Color.White.copy(alpha = 0.10f)

/**
 * The day view's tabs as one bar under the next prayer, in the same translucent glass: the icons
 * side by side, parted by faint lines; the selected one a brighter pane of the glass. Only
 * transparency draws it.
 */
@Composable
private fun DayTabBar(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = HeaderGlass,
        contentColor = Color.White,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.height(48.dp).selectableGroup(), verticalAlignment = Alignment.CenterVertically) {
            DayTab.entries.forEachIndexed { index, tab ->
                if (index > 0) {
                    // No line beside the selected pane: it is set apart by its own glass.
                    val quiet = index == selected || index - 1 == selected
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(24.dp)
                            .background(Color.White.copy(alpha = if (quiet) 0f else 0.16f))
                    )
                }
                val isSelected = index == selected
                val label = stringResource(tab.label)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = if (isSelected) 0.18f else 0f))
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) })
                        .semantics { contentDescription = label },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        tab.icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = if (isSelected) 1f else 0.62f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

/** A screen's main action, floating in the bottom corner (the end side: the left in Arabic). */
@Composable
internal fun MainActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        icon = { Icon(icon, null) },
        text = { Text(label) },
    )
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

/**
 * Width of the time column on the start side, for a 24-hour time («١٣:٠٤»); the rail runs
 * [RailInset] from its inner edge.
 */
private val GutterWidth = 60.dp
private val RailInset = 9.dp
private val RailWidth = 3.dp

@Composable
private fun NowColor(): Color = LocalDhikraPalette.current.now

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
        text = formatClock(minutes),
        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
        fontWeight = if (current || color != null) FontWeight.SemiBold else null,
        color = color ?: when {
            current -> LocalDhikraPalette.current.slate.accent
            passed -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
            else -> scheme.onSurfaceVariant
        },
        maxLines = 1,
        softWrap = false,
    )
}

/**
 * A prayer as the slim band that opens its period: its icon, name and, for the coming prayer, the
 * countdown. Slate blue like the rail — the colour of time on the calendar — a deeper blue for the
 * period going on, faded once over.
 */
@Composable
private fun PrayerBand(period: DayPeriod, modifier: Modifier = Modifier) {
    val prayer = period.prayer ?: return
    val scheme = MaterialTheme.colorScheme
    // The current prayer is a deeper shade of the bands' blue: that is what marks it as going on.
    val blue = LocalDhikraPalette.current.slate
    val tint = if (period.current) blue.accent.copy(alpha = 0.22f).compositeOver(blue.container) else blue.container
    val accent = blue.accent
    val ink = blue.ink
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 34.dp)
            .alpha(if (period.passed) 0.6f else 1f)
            .clip(MaterialTheme.shapes.medium)
            .background(tint)
            .padding(horizontal = 12.dp, vertical = 3.dp)
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
        Spacer(Modifier.weight(1f))
        period.untilPrayer?.let { minutes ->
            Surface(shape = CircleShape, color = accent.copy(alpha = 0.14f), contentColor = accent) {
                Text(
                    text = untilText(minutes),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

/** The colours of a section block: green for the day's timed sections, sand for the all-day ones. */
private data class ChipColors(val container: Color, val border: Color, val accent: Color, val onAccent: Color, val ink: Color)

@Composable
private fun timedChipColors(): ChipColors {
    val scheme = MaterialTheme.colorScheme
    return ChipColors(
        container = scheme.surfaceContainerLowest,
        border = scheme.primary.copy(alpha = 0.32f),
        accent = scheme.primary,
        onAccent = scheme.onPrimary,
        ink = scheme.onSurface,
    )
}

/**
 * A section in the calendar: a strip and the section icon in green while to do, its title and
 * time, and at its end the steps done of all («٣/٩»). A finished section turns gold all over, as
 * the section's own steps do, with a check in place of the count. The whole block opens the section.
 */
@Composable
private fun SectionChip(
    section: ProgramSection,
    state: UiState,
    colors: ChipColors,
    onClick: () -> Unit,
) {
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    val finished = total > 0 && done >= total
    val shown = if (finished) doneChipColors() else colors
    val steps = stringResource(R.string.steps_done, uiDigits(done), uiDigits(total))
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = shown.container,
        border = BorderStroke(1.dp, shown.border),
        // The steps done are read out: the groove only shows them.
        modifier = Modifier.fillMaxWidth().semantics { if (total > 0) stateDescription = steps },
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(shown.accent))
            Column(Modifier.weight(1f).padding(start = 10.dp, end = 12.dp, top = 9.dp, bottom = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(shown.accent.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(SectionIcon, null, tint = shown.accent, modifier = Modifier.size(19.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = shown.ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        section.reminderMinutes?.let { minutes ->
                            Text(
                                text = formatClock(minutes),
                                style = MaterialTheme.typography.labelSmall,
                                color = shown.ink.copy(alpha = 0.7f),
                                maxLines = 1,
                            )
                        }
                    }
                    if (finished) {
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = stringResource(R.string.section_finished),
                            tint = shown.accent,
                            modifier = Modifier.size(22.dp),
                        )
                    } else if (total > 0) {
                        // Steps done of all, e.g. «٣/٩»; read out in full by the card's state.
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = shown.accent.copy(alpha = 0.1f),
                            contentColor = shown.accent,
                            modifier = Modifier.clearAndSetSemantics {},
                        ) {
                            Text(
                                text = uiDigits("$done/$total"),
                                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A section all done: the gold of everything finished in the app (the section's own steps turn the
 * same) — a light golden tint and outline, a gold strip, icon and check — beside the green ones
 * still to do.
 */
@Composable
private fun doneChipColors(): ChipColors {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDhikraPalette.current
    return ChipColors(
        container = palette.done.copy(alpha = 0.06f).compositeOver(scheme.surfaceContainerLowest),
        border = palette.done.copy(alpha = 0.5f),
        accent = palette.doneText,
        onAccent = scheme.onPrimary,
        ink = scheme.onSurface.copy(alpha = 0.8f),
    )
}

/** The current time across the schedule, like a calendar's: a line from the time column's dot. */
@Composable
private fun NowLine() {
    Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
        HorizontalDivider(thickness = 2.dp, color = NowColor())
    }
}

/** Today's Gregorian date (with the weekday) and Hijri (Umm al-Qura) date, in the app's language, in [timeZone]. */
private fun todayDates(timeZone: String?): Pair<String, String> {
    val zone = android.icu.util.TimeZone.getTimeZone(timeZone ?: java.util.TimeZone.getDefault().id)
    val now = java.util.Date()
    fun format(style: Int, locale: String) =
        android.icu.text.DateFormat.getDateInstance(style, android.icu.util.ULocale(locale))
            .apply { this.timeZone = zone }
            .format(now)
    // The weekday, day and month: the year is the Hijri date's.
    fun dayAndMonth(locale: String) =
        android.icu.text.DateFormat.getInstanceForSkeleton("EEEEdMMMM", android.icu.util.ULocale(locale))
            .apply { this.timeZone = zone }
            .format(now)
    return runCatching {
        if (AppLanguage.current == AppLanguage.AR) {
            dayAndMonth("ar@numbers=arab") to
                format(android.icu.text.DateFormat.LONG, "ar@calendar=islamic-umalqura;numbers=arab")
        } else {
            dayAndMonth("en") to
                format(android.icu.text.DateFormat.LONG, "en@calendar=islamic-umalqura")
        }
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
