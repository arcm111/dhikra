package com.example.dhikra.ui.today

import com.example.dhikra.ui.editor.tintedHeader
import com.example.dhikra.ui.editor.sectionHeaderColor
import com.example.dhikra.ui.editor.sectionTone
import com.example.dhikra.ui.prayer.rememberSecondOfDay
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
import com.example.dhikra.ui.formatClock
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.clipRect
import com.example.dhikra.data.SectionClock
import java.util.TimeZone
import androidx.compose.ui.unit.sp
import com.example.dhikra.ui.components.starLattice
import com.example.dhikra.ui.components.NightInk
import com.example.dhikra.ui.theme.ForestLight
import com.example.dhikra.ui.components.StepNode
import com.example.dhikra.ui.components.TimelineRow
import com.example.dhikra.ui.components.TimelineSegments
import com.example.dhikra.ui.components.StepState
import com.example.dhikra.ui.components.StepNodeSize
import com.example.dhikra.ui.components.LocalSectionComplete
import com.example.dhikra.data.ProgramItem
import com.example.dhikra.data.Prayer
import com.example.dhikra.ui.theme.OrangeLight
import com.example.dhikra.ui.theme.Orange
import com.example.dhikra.ui.home.ProgramItemView
import com.example.dhikra.ui.home.nodeCenter
import com.example.dhikra.ui.components.HeadingNode
import com.example.dhikra.ui.components.HeadingNodeSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.layout.layout
import kotlin.math.roundToInt
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.OutlinedButton

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
    // Each tab's list, to know when the one shown is scrolled: the header then folds the next
    // prayer into one line under the program's name, giving the page more room.
    val listStates = DayTab.entries.associateWith { rememberLazyListState() }
    val folded by remember {
        derivedStateOf {
            val list = listStates.getValue(DayTab.entries[pager.currentPage])
            list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 24
        }
    }
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
                            // Today's date; folded, the coming prayer in its place.
                            AnimatedContent(targetState = folded && state.prayerTimes != null, label = "headerLine") { compact ->
                                if (compact) {
                                    state.prayerTimes?.let { NextPrayerLine(it, nowMinutes, actions.onOpenPrayerTimes) }
                                } else if (dateLine.isNotEmpty()) {
                                    Text(
                                        text = dateLine,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.72f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    },
                    // «تعديل البرنامج»: the program tab, where the program is edited.
                    actions = { OverflowMenu(actions, editProgram = { scope.launch { pager.animateScrollToPage(DayTab.Program.ordinal) } }) },
                    expandedHeight = 64.dp,
                    windowInsets = WindowInsets(0, 0, 0, 0),
                    colors = headerAppBarColors(),
                )
                AnimatedVisibility(
                    visible = !folded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    // The prayer time going on, on the header's glass; without a city, the way to choose it.
                    val timetable = state.prayerTimes
                    val headerCard = Modifier.padding(horizontal = 12.dp)
                    if (timetable != null) {
                        PrayerTimeCard(
                            timeline = timeline,
                            city = timetable.city.shownName,
                            nowMinutes = nowMinutes,
                            onClick = actions.onOpenPrayerTimes,
                            inHeader = true,
                            timeZone = timetable.city.timeZone,
                            modifier = headerCard,
                        )
                    } else {
                        ChooseCityCard(onClick = actions.onOpenPrayerTimes, inHeader = true, modifier = headerCard)
                    }
                }
                DayTabBar(
                    selected = pager.targetPage,
                    onSelect = { page -> scope.launch { pager.animateScrollToPage(page) } },
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
                )
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
                DayTab.Program -> ProgramTab(
                    state = state,
                    actions = actions,
                    timeline = timeline,
                    onEditProgram = onEditProgram,
                    listState = listStates.getValue(DayTab.Program),
                    contentPadding = contentPadding,
                    bottomInset = padding.calculateBottomPadding(),
                )
                DayTab.Home -> LazyColumn(Modifier.fillMaxSize(), state = listStates.getValue(DayTab.Home), contentPadding = contentPadding) {
                    // What to do now, as three panels: today's progress, the sections of the period at
                    // hand, and the daily counters.
                    item(key = "progress") {
                        // Today's progress through the whole program, on a light card (opens the day).
                        ProgressCard(
                            timeline = timeline,
                            ordered = timeline.sectionsInOrder(sections),
                            state = state,
                            message = progressMessage(timeline, sections, state),
                            onClick = { scope.launch { pager.animateScrollToPage(DayTab.Calendar.ordinal) } },
                            light = true,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    item(key = "now") {
                        // Within the prayer time going on: the section(s) whose time came last, and
                        // those coming next before the next prayer.
                        val period = timeline.periods.getOrNull(timeline.currentIndex)
                        val group = period?.let { nowAndNext(sections, it.start, it.end, nowMinutes) } ?: NowAndNext(emptyList(), emptyList())
                        NowCard(
                            group = group,
                            state = state,
                            nowMinutes = nowMinutes,
                            empty = period?.prayer?.let { stringResource(R.string.now_none_in, stringResource(it.label)) }
                                ?: stringResource(if (sections.isEmpty()) R.string.home_now_no_program else R.string.home_now_empty),
                            onOpenSection = onOpenSection,
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                        )
                    }
                    item(key = "counters") {
                        // The daily counter, as the counter screen draws it: open-ended counts of
                        // chosen adhkar, starting over each day.
                        CounterCard(
                            entries = state.counters.mapNotNull(state.library::entry),
                            state = state,
                            onAdd = { pickingCounter = true },
                            onOpen = actions.onOpenCounter,
                            onRemove = actions.onRemoveCounter,
                            modifier = Modifier.padding(start = 8.dp, top = 12.dp),
                        )
                    }
                }
                DayTab.Favourites -> LazyColumn(Modifier.fillMaxSize(), state = listStates.getValue(DayTab.Favourites), contentPadding = contentPadding) {
                    item(key = "favouritesTitle") {
                        TabHeading(
                            title = stringResource(R.string.favourites_title),
                            description = stringResource(R.string.favourites_subtitle),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
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
                DayTab.Library -> LazyColumn(Modifier.fillMaxSize(), state = listStates.getValue(DayTab.Library), contentPadding = contentPadding) {
                    // The collections the steps read from, each opening on its texts and groups.
                    item(key = "libraryTitle") {
                        TabHeading(
                            title = stringResource(R.string.library),
                            description = stringResource(R.string.library_hint),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        )
                    }
                    // The schedule's rail side has 8 dp less padding: the same margin on both sides.
                    libraryCollections(state.library, actions.onOpenCollection, Modifier.padding(start = 8.dp), showHint = false)
                }
                // The day's list starts under the header (and its rounded lower edge) rather than
                // behind it: its prayer bands pin to the list's top, which must be in view.
                DayTab.Calendar -> LazyColumn(
                    state = listStates.getValue(DayTab.Calendar),
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
                    item(key = "prayerTitle") {
                        TabHeading(
                            title = stringResource(R.string.day_schedule),
                            description = stringResource(R.string.day_schedule_hint),
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        )
                    }
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
                            spaceAbove = if (row.first) 0.dp else 14.dp,
                            gap = 0.dp,
                            // Opaque: the sections scroll under it while it is pinned.
                            modifier = Modifier.animateItem().background(scheme.background),
                        ) { PrayerBand(row.period, slice = row.slice) }
                        is ScheduleItem.Entry -> {
                            val color = railColor(row.period)
                            when (val entry = row.entry) {
                                is PeriodEntry.Now -> ScheduleRow(
                                    top = color,
                                    bottom = color.takeIf { !row.last },
                                    dot = NowColor(),
                                    gutter = { GutterTime(entry.minutes, color = NowColor()) },
                                    gap = 0.dp,
                                ) {
                                    PanelSlice(row.slice, current = row.period.current) {
                                        NowLine(Modifier.padding(horizontal = 10.dp))
                                    }
                                }
                                is PeriodEntry.Section -> ScheduleRow(
                                    top = color.takeIf { !row.firstOfDay },
                                    bottom = color.takeIf { !row.last },
                                    gap = 0.dp,
                                    modifier = Modifier.animateItem(),
                                ) {
                                    PanelSlice(row.slice, current = row.period.current, divider = row.divider) {
                                        NowSectionRow(entry.section, state, onClick = { onOpenSection(entry.section.id) })
                                    }
                                }
                            }
                        }
                        ScheduleItem.AllDayLabel -> AllDayDivider(Modifier.padding(top = 18.dp, bottom = 8.dp))
                        is ScheduleItem.AllDay -> ScheduleRow(gap = 0.dp, modifier = Modifier.animateItem()) {
                            PanelSlice(row.slice, current = false, divider = row.divider) {
                                NowSectionRow(row.section, state, onClick = { onOpenSection(row.section.id) })
                            }
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
 * The ground of the home tab's dark cards (the progress and the daily counter): the counter
 * screen's night green, its two shades met in one flat colour.
 */
@Composable
private fun homeCardColor(): Color =
    LocalDhikraPalette.current.let { lerp(it.nightStart, it.nightEnd, 0.5f) }

/**
 * The time of the prayer going on: in a pane of glass along the card's start edge, a ring filling
 * with a timer's dial draining as it passes and the time left in its middle — «حتى المغرب» above
 * it and that prayer's hour below, in light orange — then its name and the city, and the day's
 * prayers on a 24-hour line. Opens the prayer times.
 */
@Composable
private fun PrayerTimeCard(
    timeline: DayTimeline,
    city: String,
    nowMinutes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** On the header's glass (its pattern already behind it), rather than its own dark green. */
    inHeader: Boolean = false,
    /** The prayer times' city's time zone, for the countdown's seconds. */
    timeZone: String? = null,
) {
    val periods = timeline.periods
    val index = timeline.currentIndex
    val period = periods.getOrNull(index) ?: return
    val prayer = period.prayer ?: return
    val next = periods[(index + 1) % periods.size]
    val nextPrayer = next.prayer ?: return
    // The countdown: in minutes (h:mm) while an hour or more is left, then in seconds (m:ss).
    val lastHour = minutesLeft(period.end, nowMinutes) <= 60
    val second = rememberSecondOfDay(timeZone, everySecond = lastHour)
    val secondsLeft = Math.floorMod(period.end * 60 - second, SECONDS_PER_DAY)
    val length = Math.floorMod(period.end - period.start, 24 * 60).let { if (it == 0) 24 * 60 else it } * 60
    val share = (Math.floorMod(second - period.start * 60, SECONDS_PER_DAY).toFloat() / length).coerceIn(0f, 1f)
    val underHour = secondsLeft < 3600
    // The dial: the light gold of the day's line while time is ample, its orange in the last hour.
    val accent = if (underHour) OrangeLight else MustardLight
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (inHeader) HeaderGlass else homeCardColor(),
        contentColor = NightInk,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .then(if (inHeader) Modifier else Modifier.starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .background(HeaderGlass),
                contentAlignment = Alignment.Center,
            ) {
              // The countdown to the next prayer: its name above the ring, its time below.
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
                Text(
                    text = stringResource(nextPrayer.label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
                    TimerRing(elapsed = share, color = accent, modifier = Modifier.fillMaxSize())
                    // The time left: «٧:٣٤» (h:mm), and in the last hour «٤٥:٠٩» (m:ss).
                    val clock = if (underHour) {
                        "%d:%02d".format(secondsLeft / 60, secondsLeft % 60)
                    } else {
                        "%d:%02d".format(secondsLeft / 3600, secondsLeft % 3600 / 60)
                    }
                    val spoken = untilDescription(secondsLeft)
                    Text(
                        text = uiDigits(clock),
                        style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.semantics { contentDescription = spoken },
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = formatClock(next.start),
                    style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"),
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1,
                )
              }
            }
            Column(Modifier.weight(1f).padding(start = 22.dp, end = 14.dp, top = 14.dp, bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.prayer_time_title, stringResource(prayer.label)),
                        style = MaterialTheme.typography.titleMedium,
                        color = NightInk,
                        modifier = Modifier.weight(1f),
                    )
                    // The city the times are for.
                    Icon(Icons.Outlined.LocationOn, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = city,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 110.dp),
                    )
                }
                DayStrip(periods, index, nowMinutes, accent, Modifier.padding(top = 10.dp))
            }
        }
    }
}

/**
 * A timer's dial, unlike the progress rings that fill: a ring of [TIMER_TICKS] ticks, those of the
 * time still left lit in [color] and going out one by one from the top as [elapsed] (0 to 1) grows,
 * turning the other way round from the progress rings.
 */
@Composable
private fun TimerRing(elapsed: Float, color: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier) {
        // The other way round from the progress rings: the time left runs back from the top.
        val direction = if (rtl) 1f else -1f
        val outer = size.minDimension / 2f
        val tick = 3.5.dp.toPx()
        val radius = outer - 2.dp.toPx()
        fun at(share: Float, r: Float): Offset {
            val angle = -PI / 2 + direction * 2 * PI * share
            return Offset(center.x + r * cos(angle).toFloat(), center.y + r * sin(angle).toFloat())
        }
        for (i in 0 until TIMER_TICKS) {
            val share = i / TIMER_TICKS.toFloat()
            drawLine(
                color = if (share >= elapsed) color else Color.White.copy(alpha = 0.14f),
                start = at(share, radius - tick),
                end = at(share, radius),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private const val SECONDS_PER_DAY = 24 * 3600

/** The time left, read aloud: «٧ س ٣٤ د». */
@Composable
private fun untilDescription(secondsLeft: Int): String {
    val hours = secondsLeft / 3600
    val minutes = secondsLeft % 3600 / 60
    return if (hours > 0) {
        stringResource(R.string.prayer_hours_short, uiDigits(hours)) + " " + stringResource(R.string.prayer_minutes_short, uiDigits(minutes))
    } else {
        stringResource(R.string.prayer_minutes_short, uiDigits(minutes))
    }
}

/** The ticks round [TimerRing]'s dial. */
private const val TIMER_TICKS = 48

/** No city chosen yet: the place of the prayer-time card asks for it (opens the prayer times). */
@Composable
private fun ChooseCityCard(onClick: () -> Unit, modifier: Modifier = Modifier, inHeader: Boolean = false) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (inHeader) HeaderGlass else homeCardColor(),
        contentColor = NightInk,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .then(if (inHeader) Modifier else Modifier.starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LocationOn, null, tint = MustardLight, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.day_pick_city), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.White.copy(alpha = 0.6f))
        }
    }
}


/**
 * The day's prayers on a 24-hour line, 00:00 at its start and 24:00 at its end (right to left in
 * Arabic): a mark at each prayer, its letter above and its time below — the one going on in gold —
 * the time going on filled up to now in [accent], and a gold tick at now. Labels too close to fit
 * side by side take a second row.
 */
@Composable
private fun DayStrip(periods: List<DayPeriod>, current: Int, nowMinutes: Int, accent: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val density = LocalDensity.current
    val prayers = periods.mapNotNull { it.prayer }
    if (prayers.size != periods.size) return
    val letters = prayers.map { prayerInitial(it) }
    val times = periods.map { formatClock(it.start) }
    // Its width, once laid out (a card sized to its content cannot ask it beforehand).
    var width by remember { mutableFloatStateOf(0f) }
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat() }) {
        fun x(minutes: Int): Float {
            val f = Math.floorMod(minutes, 24 * 60) / (24f * 60f)
            return if (rtl) width * (1 - f) else width * f
        }
        val positions = periods.map { x(it.start) }
        val letterRows = labelRows(positions, with(density) { 12.dp.toPx() })
        val timeRows = labelRows(positions, with(density) { 30.dp.toPx() })
        val labelStyle = MaterialTheme.typography.labelSmall
        Column {
            for (row in 0..(letterRows.maxOrNull() ?: 0)) {
                Box(Modifier.fillMaxWidth().height(16.dp)) {
                    periods.indices.filter { letterRows[it] == row }.forEach { i ->
                        Text(
                            text = letters[i],
                            style = labelStyle,
                            fontWeight = if (i == current) FontWeight.Bold else FontWeight.Medium,
                            color = if (i == current) MustardLight else Color.White.copy(alpha = 0.6f),
                            maxLines = 1,
                            modifier = Modifier.centredAt(positions[i]),
                        )
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                val y = size.height / 2f
                val line = 3.dp.toPx()
                drawLine(Color.White.copy(alpha = 0.16f), Offset(0f, y), Offset(size.width, y), line, StrokeCap.Round)
                // The time going on, up to now (two pieces when it runs past midnight).
                val start = periods[current].start
                fun fill(from: Int, to: Int) {
                    drawLine(accent, Offset(x(from), y), Offset(if (to >= 24 * 60) x(24 * 60 - 1) else x(to), y), line, StrokeCap.Round)
                }
                if (nowMinutes >= start) fill(start, nowMinutes) else {
                    fill(start, 24 * 60)
                    fill(0, nowMinutes)
                }
                positions.forEachIndexed { i, px ->
                    drawLine(
                        if (i == current) MustardLight else Color.White.copy(alpha = 0.55f),
                        Offset(px, y - 4.dp.toPx()),
                        Offset(px, y + 4.dp.toPx()),
                        1.5.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
                val nowX = x(nowMinutes)
                drawLine(MustardLight, Offset(nowX, 0f), Offset(nowX, size.height), 2.dp.toPx(), StrokeCap.Round)
            }
            for (row in 0..(timeRows.maxOrNull() ?: 0)) {
                Box(Modifier.fillMaxWidth().height(15.dp)) {
                    periods.indices.filter { timeRows[it] == row }.forEach { i ->
                        Text(
                            text = times[i],
                            style = labelStyle.copy(fontFeatureSettings = "tnum"),
                            color = if (i == current) Color.White else Color.White.copy(alpha = 0.55f),
                            maxLines = 1,
                            modifier = Modifier.centredAt(positions[i]),
                        )
                    }
                }
            }
        }
    }
}

/** A label centred on [x] (px from the left of its row), kept inside the row. */
private fun Modifier.centredAt(x: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0))
    val rowWidth = constraints.maxWidth
    layout(rowWidth, placeable.height) {
        placeable.place((x - placeable.width / 2f).roundToInt().coerceIn(0, (rowWidth - placeable.width).coerceAtLeast(0)), 0)
    }
}

/** A prayer's initial: the first letter of its name after the article («ف» الفجر, «ع» العصر and العشاء). */
@Composable
internal fun prayerInitial(prayer: Prayer): String {
    val name = stringResource(prayer.label)
    return name.removePrefix("ال").ifEmpty { name }.take(1)
}

/**
 * «الآن» and what comes next, within the prayer time going on: on top, set apart on a green tint,
 * the section(s) of it whose time came last — the ring of their steps round the icon, the title,
 * a word that spurs on («٩ خطوات بانتظارك», «أنجزت ٣ من ٩ · بقي ٦», or «✓ مكتمل») and a clear button
 * into the section; then, under a rule, the one(s) coming up before the next prayer, compact.
 */
@Composable
private fun NowCard(
    group: NowAndNext,
    state: UiState,
    nowMinutes: Int,
    empty: String,
    onOpenSection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val green = scheme.primary
    Surface(
        shape = MaterialTheme.shapes.large,
        color = scheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, green.copy(alpha = 0.22f)),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            if (group.now.isEmpty() && group.next.isEmpty()) {
                Text(
                    text = empty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            if (group.now.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(green.copy(alpha = 0.07f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    group.now.forEach { NowSection(it, state, onOpen = { onOpenSection(it.id) }) }
                }
            }
            if (group.next.isNotEmpty()) {
                if (group.now.isNotEmpty()) HorizontalDivider(color = green.copy(alpha = 0.22f))
                Text(
                    text = stringResource(R.string.home_next_tag),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
                )
                group.next.forEach { UpcomingRow(it, nowMinutes, onClick = { onOpenSection(it.id) }) }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

/** A section of «الآن», put forward: its steps round its icon, its title and progress, and the way in. */
@Composable
private fun NowSection(section: ProgramSection, state: UiState, onOpen: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val palette = LocalDhikraPalette.current
    val total = section.trackableItems.size
    val done = state.completedIn(section).coerceAtMost(total)
    val finished = total > 0 && done >= total
    val tone = if (finished) palette.done else scheme.primary
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxSize(),
                    color = tone,
                    trackColor = scheme.primary.copy(alpha = 0.14f),
                    strokeWidth = 4.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Box(
                    Modifier.size(42.dp).clip(CircleShape).background(tone.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(SectionIcon, null, tint = if (finished) palette.doneText else scheme.primary, modifier = Modifier.size(22.dp)) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = listOfNotNull(stringResource(R.string.home_now), section.reminderMinutes?.let(::formatClock)).joinToString("  ·  "),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (finished) palette.doneText else scheme.primary,
                )
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (finished) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Check, null, tint = palette.doneText, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(stringResource(R.string.section_finished), style = MaterialTheme.typography.bodySmall, color = palette.doneText)
                    }
                } else if (total > 0) {
                    Text(
                        text = if (done == 0) pluralStringResource(R.plurals.focus_steps_waiting, total, uiDigits(total))
                        else stringResource(R.string.focus_progress, uiDigits(done), uiDigits(total), uiDigits(total - done)),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
        // Into the section, to be started there when the user wants; quieter once it is done.
        val label = @Composable {
            Text(stringResource(R.string.focus_open), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
        }
        val buttonModifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(48.dp)
        if (finished) {
            OutlinedButton(onClick = onOpen, shape = CircleShape, modifier = buttonModifier) { label() }
        } else {
            Button(onClick = onOpen, shape = CircleShape, modifier = buttonModifier) { label() }
        }
    }
}

/** A section coming up before the next prayer: its icon, title, time and how long until it. */
@Composable
private fun UpcomingRow(section: ProgramSection, nowMinutes: Int, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val until = section.reminderMinutes?.let { minutesLeft(it, nowMinutes) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(scheme.onSurfaceVariant.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) { Icon(SectionIcon, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                color = scheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(section.reminderMinutes?.let(::formatClock), until?.let { untilText(it) }).joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
    }
}

/**
 * The daily counter on the home tab, after the counter screen: on the home cards' flat green with the
 * faint lattice of stars, each dhikr with a small dial — the mint ring filling towards the next
 * target round the green pearl with today's count. A row opens the dhikr's counter.
 */
@Composable
private fun CounterCard(
    entries: List<LibraryEntry>,
    state: UiState,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalDhikraPalette.current
    Surface(shape = MaterialTheme.shapes.large, color = homeCardColor(), modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp)
        ) {
            // A slim header on a faint glass.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(start = 16.dp, end = 4.dp, top = 6.5.dp, bottom = 6.5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.counter_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = NightInk,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp) {
                    TextButton(
                        onClick = onAdd,
                        colors = ButtonDefaults.textButtonColors(contentColor = MustardLight),
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(40.dp),
                    ) {
                        Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.counter_add))
                    }
                }
            }
            // What the counter is, said while it is empty: its rows show it once in use.
            if (entries.isEmpty()) {
                Text(
                    text = stringResource(R.string.counter_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(horizontal = 14.dp))
                }
                CounterDialRow(
                    entry = entry,
                    count = state.counterCount(entry.id),
                    onOpen = { onOpen(entry.id) },
                    onRemove = { onRemove(entry.id) },
                )
            }
        }
    }
}

/** One dhikr of the daily counter card: its small dial, its title and today's count. Its menu takes it off. */
@Composable
private fun CounterDialRow(entry: LibraryEntry, count: Int, onOpen: () -> Unit, onRemove: () -> Unit) {
    val target = DailyCounter.target(count)
    var menu by remember { mutableStateOf(false) }
    val description = stringResource(R.string.counter_count_description, entry.title, uiDigits(count), uiDigits(target))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MiniDial(
            count = count,
            modifier = Modifier.size(56.dp).semantics(mergeDescendants = true) { contentDescription = description },
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = NightInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                // Not counted yet today: how to start; else how far, «٥٠٤ من ١٠٠٠».
                text = if (count == 0) stringResource(R.string.counter_tap_hint)
                else uiDigits(count) + " " + stringResource(R.string.counter_of, uiDigits(target)),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f),
            )
        }
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more), tint = Color.White.copy(alpha = 0.7f))
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

/**
 * The counter screen's dial in small: the string with the day's progress in mint (mirrored right
 * to left), round the green pearl lit from the upper left, with today's count.
 */
@Composable
private fun MiniDial(count: Int, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val stage = DailyCounter.stage(count)
    val target = DailyCounter.target(count)
    val digits = uiDigits(count)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2f
            val ring = outer - 3.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.10f), radius = ring, style = Stroke(1.dp.toPx()))
            // While the target is a hundred or less, its beads, as on the counter screen.
            if (target in 1..100) {
                val spacing = (2 * PI * ring / target).toFloat()
                val bead = (spacing * 0.3f).coerceIn(0.6.dp.toPx(), 1.6.dp.toPx())
                for (i in 0 until target) {
                    val angle = -PI / 2 + (if (rtl) -1 else 1) * 2 * PI * i / target
                    val lit = i < count
                    drawCircle(
                        color = if (lit) ForestLight else Color.White.copy(alpha = 0.18f),
                        radius = if (lit) bead else bead * 0.78f,
                        center = Offset(center.x + ring * cos(angle).toFloat(), center.y + ring * sin(angle).toFloat()),
                    )
                }
            }
            drawArc(
                color = ForestLight.copy(alpha = 0.9f),
                startAngle = -90f,
                sweepAngle = 360f * stage * if (rtl) -1f else 1f,
                useCenter = false,
                topLeft = Offset(center.x - ring, center.y - ring),
                size = Size(ring * 2, ring * 2),
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round),
            )
            val pearl = outer * 0.66f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2F7A57), Color(0xFF0F3524)),
                    center = Offset(center.x - pearl * 0.28f, center.y - pearl * 0.44f),
                    radius = pearl * 1.8f,
                ),
                radius = pearl,
            )
            drawCircle(
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.04f)),
                    startY = center.y - pearl,
                    endY = center.y + pearl,
                ),
                radius = pearl - 0.5.dp.toPx(),
                style = Stroke(1.dp.toPx()),
            )
        }
        Text(
            text = digits,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            fontSize = if (digits.length <= 3) 15.sp else 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
        )
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
    title: String?,
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
            if (title != null) {
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
            }
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
 * A section in the day's schedule: its icon, title and time, and the steps done of all («٣/٩») — the
 * icon and a check turn gold once it is finished. The row opens the section.
 */
@Composable
private fun NowSectionRow(section: ProgramSection, state: UiState, onClick: () -> Unit) {
    val total = section.trackableItems.size
    val done = state.completedIn(section)
    val finished = total > 0 && done >= total
    val scheme = MaterialTheme.colorScheme
    val accent = if (finished) LocalDhikraPalette.current.doneText else scheme.primary
    // As on the section's tile in the program tab: its icon, and the colour of how its time is set.
    val tone = sectionTone(section)
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
            modifier = Modifier.size(38.dp).clip(CircleShape).background(sectionHeaderColor(section)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(sectionIcon(section.icon), null, tint = if (finished) accent else tone, modifier = Modifier.size(20.dp))
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
                Text(
                    formatClock(it),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (finished) scheme.onSurfaceVariant else tone,
                )
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

/**
 * A tab's heading (the calendar, the program, the favourites, the library): its title followed by a
 * rule fading out to the end, like a chapter's, with what the tab shows under it.
 */
@Composable
internal fun TabHeading(title: String, description: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val rule = scheme.primary.copy(alpha = 0.35f)
    Column(modifier.fillMaxWidth().padding(bottom = 10.dp).semantics(mergeDescendants = true) { heading() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = scheme.primary)
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(Brush.horizontalGradient(if (rtl) listOf(Color.Transparent, rule) else listOf(rule, Color.Transparent)))
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onSurfaceVariant,
        )
    }
}

/**
 * Today's progress through the whole program: a ring with the share of steps done, a line saying
 * what is left (or that the day is complete), the steps done, and beside them the sections as a
 * small borderless table — a column per prayer time, headed by its first letter, with a small
 * rounded box for each of its sections, gold once finished. Opens the day. [light]: a card of the
 * page (white, outlined in green, dark text) rather than the dark green header's glass.
 */
@Composable
private fun ProgressCard(
    timeline: DayTimeline,
    ordered: List<ProgramSection>,
    state: UiState,
    message: String?,
    onClick: () -> Unit,
    light: Boolean,
    modifier: Modifier = Modifier,
) {
    val withSteps = ordered.filter { it.trackableItems.isNotEmpty() }
    val total = withSteps.sumOf { it.trackableItems.size }
    val done = withSteps.sumOf { state.completedIn(it).coerceAtMost(it.trackableItems.size) }
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val scheme = MaterialTheme.colorScheme
    val ink = if (light) scheme.onSurface else Color.White
    val soft = if (light) scheme.onSurfaceVariant else Color.White.copy(alpha = 0.72f)
    // The orange of the ring and the gold of the line, a shade deeper on white.
    val ring = if (light) Orange else OrangeLight
    val highlight = if (light) MustardDeep else MustardLight
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (light) scheme.surfaceContainerLowest else HeaderGlass,
        contentColor = ink,
        border = if (light) BorderStroke(1.dp, scheme.primary.copy(alpha = 0.22f)) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxSize(),
                    color = ring,
                    trackColor = if (light) scheme.onSurface.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.14f),
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Text(
                    text = stringResource(R.string.progress_percent, uiDigits(Math.round(fraction * 100))),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.today_progress), style = MaterialTheme.typography.titleMedium, color = ink)
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = highlight,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = stringResource(R.string.progress_steps, uiDigits(done), uiDigits(total)),
                    style = MaterialTheme.typography.bodySmall,
                    color = soft,
                )
            }
            SectionTable(timeline, state, light, Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * The sections of the day as a borderless table: a column per prayer time, headed by the first
 * letter of its name (العصر and العشاء both «ع»: their order tells them apart), with a small rounded box under
 * it for each of its sections — gold once finished, faint while still to do; the column of the time
 * going on stands out. Sections with no time make a last column. Without prayer times, one row.
 */
@Composable
private fun SectionTable(timeline: DayTimeline, state: UiState, light: Boolean, modifier: Modifier = Modifier) {
    val faint = if (light) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.22f)
    val palette = LocalDhikraPalette.current
    fun finished(section: ProgramSection) = state.completedIn(section) >= section.trackableItems.size
    val all = timeline.periods.flatMap { it.sections } + timeline.untimed
    val withSteps = all.filter { it.trackableItems.isNotEmpty() }
    if (withSteps.isEmpty()) return
    val sectionsLabel = stringResource(
        R.string.progress_sections,
        uiDigits(withSteps.count(::finished)),
        uiDigits(withSteps.size),
    )
    val dot = @Composable { isDone: Boolean ->
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(if (isDone) palette.done else faint)
        )
    }
    val timed = timeline.periods.any { it.prayer != null }
    if (!timed) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            maxItemsInEachRow = 6,
            modifier = modifier.clearAndSetSemantics { contentDescription = sectionsLabel },
        ) { withSteps.forEach { dot(finished(it)) } }
        return
    }
    // The headers: the first letter of the name after its article (العصر and العشاء both «ع»).
    val headers = timeline.periods.map { period -> period.prayer?.let { prayerInitial(it) }.orEmpty() }
    val untimed = timeline.untimed.filter { it.trackableItems.isNotEmpty() }
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier.clearAndSetSemantics { contentDescription = sectionsLabel },
    ) {
        timeline.periods.forEachIndexed { index, period ->
            TableColumn(
                header = headers[index],
                current = period.current,
                light = light,
                dots = period.sections.filter { it.trackableItems.isNotEmpty() }.map(::finished),
                dot = dot,
            )
        }
        if (untimed.isNotEmpty()) {
            TableColumn(header = "•", current = false, light = light, dots = untimed.map(::finished), dot = dot)
        }
    }
}

/** One column of [SectionTable]: its header, and a dot per section under it. */
@Composable
private fun TableColumn(header: String, current: Boolean, light: Boolean, dots: List<Boolean>, dot: @Composable (Boolean) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.widthIn(min = 11.dp),
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (current) FontWeight.Bold else FontWeight.Medium,
            color = when {
                current -> if (light) MustardDeep else MustardLight
                light -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> Color.White.copy(alpha = 0.6f)
            },
            maxLines = 1,
        )
        dots.forEach { dot(it) }
    }
}

/**
 * The progress card's line: the day done, or how many sections are left — before the next prayer
 * while the period going on still has some, else for the rest of the day.
 */
@Composable
private fun progressMessage(timeline: DayTimeline, sections: List<ProgramSection>, state: UiState): String? {
    fun open(section: ProgramSection) = section.trackableItems.isNotEmpty() && state.completedIn(section) < section.trackableItems.size
    if (sections.none { it.trackableItems.isNotEmpty() }) return null
    val left = sections.count(::open)
    if (left == 0) return stringResource(R.string.progress_all_done)
    val index = timeline.currentIndex
    val nowLeft = timeline.periods.getOrNull(index)?.sections?.count(::open) ?: 0
    val nextPrayer = timeline.periods.getOrNull(index + 1)?.prayer
    return if (nowLeft > 0 && nextPrayer != null) {
        pluralStringResource(R.plurals.progress_left_before, nowLeft, uiDigits(nowLeft), stringResource(nextPrayer.label))
    } else {
        pluralStringResource(R.plurals.progress_left_today, left, uiDigits(left))
    }
}


// ------------------------------------------------------------------------------ Header

/**
 * The coming prayer in one line, for the folded header: «🌙 العشاء ١٨:١٤ · بعد ١ س ٣٧ د». Opens
 * the prayer times.
 */
@Composable
private fun NextPrayerLine(timetable: PrayerTimetable, nowMinutes: Int, onClick: () -> Unit) {
    val next = timetable.next(nowMinutes)
    val name = stringResource(next.prayer.label)
    Row(
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(next.prayer.icon, null, tint = MustardLight, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(
            text = "$name ${formatClock(next.atMinutes)}  ·  ${untilText(next.inMinutes)}",
            style = MaterialTheme.typography.labelMedium,
            color = MustardLight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The gold of the progress card's line on a light card: [MustardLight] would be too pale on white. */
private val MustardDeep = Color(0xFFA9862C)

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

    /**
     * A prayer opening its period, as its panel's header ([slice]: the panel's top, or all of it
     * when the period holds nothing); [previous] is the period before it (the rail comes down from it).
     */
    data class PrayerBand(val period: DayPeriod, val previous: DayPeriod?, val first: Boolean, val continues: Boolean, val slice: Slice) : ScheduleItem {
        override val key = "prayer/" + period.prayer?.name
    }

    /**
     * A section or the current time inside [period]; [last] ends the day's rail. [slice] is its piece
     * of the period's panel, [divider] whether a line parts it from the section above.
     */
    data class Entry(
        val period: DayPeriod,
        val entry: PeriodEntry,
        val firstOfDay: Boolean,
        val last: Boolean,
        val slice: Slice = Slice.Middle,
        val divider: Boolean = false,
    ) : ScheduleItem {
        override val key = when (entry) {
            is PeriodEntry.Section -> "section/" + entry.section.id
            is PeriodEntry.Now -> "now"
        }
    }

    data object AllDayLabel : ScheduleItem {
        override val key = "allDay"
    }

    data class AllDay(val section: ProgramSection, val slice: Slice, val divider: Boolean) : ScheduleItem {
        override val key = "allDay/" + section.id
    }
}

/** A row's piece of its panel: the top (rounded above), a middle, the bottom (rounded below), or all of it. */
private enum class Slice { Top, Middle, Bottom, Single }

private fun slice(top: Boolean, bottom: Boolean) = when {
    top && bottom -> Slice.Single
    top -> Slice.Top
    bottom -> Slice.Bottom
    else -> Slice.Middle
}

/**
 * The schedule's rows: each prayer period one panel — its band as the header, its sections (and
 * the current time, if it falls there) as the rows under it — then the sections with no time in a
 * panel of their own.
 */
private fun scheduleRows(timeline: DayTimeline): List<ScheduleItem> = buildList {
    val periods = timeline.periods
    periods.forEachIndexed { index, period ->
        val lastPeriod = index == periods.lastIndex
        val header = period.prayer != null
        if (header) {
            add(
                ScheduleItem.PrayerBand(
                    period, periods.getOrNull(index - 1), first = index == 0,
                    continues = !lastPeriod || period.entries.isNotEmpty(),
                    slice = if (period.entries.isEmpty()) Slice.Single else Slice.Top,
                )
            )
        }
        period.entries.forEachIndexed { at, entry ->
            val previous = period.entries.getOrNull(at - 1)
            add(
                ScheduleItem.Entry(
                    period = period,
                    entry = entry,
                    firstOfDay = period.prayer == null && at == 0,
                    last = lastPeriod && at == period.entries.lastIndex,
                    slice = slice(top = !header && at == 0, bottom = at == period.entries.lastIndex),
                    divider = entry is PeriodEntry.Section && previous is PeriodEntry.Section,
                )
            )
        }
    }
    if (timeline.untimed.isNotEmpty()) {
        add(ScheduleItem.AllDayLabel)
        timeline.untimed.forEachIndexed { at, section ->
            add(ScheduleItem.AllDay(section, slice(top = at == 0, bottom = at == timeline.untimed.lastIndex), divider = at > 0))
        }
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
    /** Space above and below the content; 0 for the slices of a panel, which join. */
    gap: Dp = 3.dp,
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
        Box(Modifier.weight(1f).padding(top = spaceAbove + gap, bottom = gap)) { content() }
    }
}

/** A period panel's outline: the green frame of the program's section tiles, firmer for the period going on. */
@Composable
private fun panelBorder(current: Boolean): Color =
    MaterialTheme.colorScheme.primary.copy(alpha = if (current) 0.85f else 0.55f)

private fun panelBorderWidth(current: Boolean): Dp = if (current) 2.dp else 1.5.dp

/** The section tiles' corners. */
private val PanelRadius = 12.dp

/**
 * Draws a row's [slice] of a panel: its [fill], rounded at the panel's top or bottom, and the
 * panel's outline — only the sides of a middle slice, so the slices join into one panel.
 */
private fun Modifier.panelSlice(slice: Slice, fill: Color, border: Color, borderWidth: Dp): Modifier = drawBehind {
    val r = PanelRadius.toPx()
    val stroke = borderWidth.toPx()
    val top = slice == Slice.Top || slice == Slice.Single
    val bottom = slice == Slice.Bottom || slice == Slice.Single
    fun shape(extendTop: Float, extendBottom: Float) = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(0f, -extendTop, size.width, size.height + extendBottom),
                topLeft = CornerRadius(if (top) r else 0f),
                topRight = CornerRadius(if (top) r else 0f),
                bottomRight = CornerRadius(if (bottom) r else 0f),
                bottomLeft = CornerRadius(if (bottom) r else 0f),
            )
        )
    }
    drawPath(shape(0f, 0f), fill)
    // The outline, inset by half its width; an open edge is pushed out of the row and clipped off.
    val inset = stroke / 2f
    clipRect {
        drawPath(
            Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(inset, if (top) inset else -stroke * 2, size.width - inset, if (bottom) size.height - inset else size.height + stroke * 2),
                        topLeft = CornerRadius(if (top) r else 0f),
                        topRight = CornerRadius(if (top) r else 0f),
                        bottomRight = CornerRadius(if (bottom) r else 0f),
                        bottomLeft = CornerRadius(if (bottom) r else 0f),
                    )
                )
            },
            border,
            style = Stroke(stroke),
        )
    }
}

/** A row of a period panel (a section, or the current time): white, outlined, a line above it when [divider]. */
@Composable
private fun PanelSlice(slice: Slice, current: Boolean, divider: Boolean = false, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .panelSlice(slice, fill = MaterialTheme.colorScheme.surfaceContainerLowest, border = panelBorder(current), borderWidth = panelBorderWidth(current))
            .padding(bottom = if (slice == Slice.Bottom || slice == Slice.Single) 2.dp else 0.dp)
    ) {
        if (divider) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                modifier = Modifier.padding(horizontal = 14.dp),
            )
        }
        content()
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
private fun PrayerBand(period: DayPeriod, modifier: Modifier = Modifier, slice: Slice? = null) {
    val prayer = period.prayer ?: return
    val scheme = MaterialTheme.colorScheme
    // The current prayer is a deeper shade of the bands' blue: that is what marks it as going on.
    val blue = LocalDhikraPalette.current.slate
    // The tinted header of the program's section tiles, in the blue of time; deeper while going on.
    val tint = tintedHeader(blue.accent, strength = if (period.current) 0.5f else 0.3f)
    val accent = blue.accent
    val ink = blue.ink
    val surface = MaterialTheme.colorScheme.surfaceContainerLowest
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (slice != null) 48.dp else 34.dp)
            .then(
                // As a panel's header: its top (or all of it), on the period's tint.
                if (slice != null) {
                    Modifier.panelSlice(slice, fill = tint.compositeOver(surface), border = panelBorder(period.current), borderWidth = panelBorderWidth(period.current))
                } else {
                    Modifier.clip(MaterialTheme.shapes.medium).background(tint)
                }
            )
            .alpha(if (period.passed) 0.6f else 1f)
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .semantics(mergeDescendants = true) { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // As on a section tile: the icon on a pale disc.
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(surface.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center,
        ) { Icon(prayer.icon, null, tint = accent, modifier = Modifier.size(17.dp)) }
        Spacer(Modifier.width(10.dp))
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

/** The current time across the schedule, like a calendar's: a line from the time column's dot. */
@Composable
private fun NowLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.Center) {
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
