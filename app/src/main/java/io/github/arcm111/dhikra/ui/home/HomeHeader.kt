package io.github.arcm111.dhikra.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.ArabicText
import io.github.arcm111.dhikra.data.Program
import io.github.arcm111.dhikra.data.ProgramSection
import io.github.arcm111.dhikra.ui.UiState
import io.github.arcm111.dhikra.ui.components.DhikraHeader
import io.github.arcm111.dhikra.ui.components.GoldInk
import io.github.arcm111.dhikra.ui.components.PrayerIcon
import io.github.arcm111.dhikra.ui.components.headerAppBarColors
import io.github.arcm111.dhikra.ui.components.sectionIcon
import io.github.arcm111.dhikra.ui.formatClock
import io.github.arcm111.dhikra.ui.theme.EmphasizedDecelerate
import io.github.arcm111.dhikra.ui.theme.LocalDhikraPalette
import io.github.arcm111.dhikra.ui.theme.MustardLight
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import io.github.arcm111.dhikra.ui.uiDigits
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton

/**
 * The section view's header: back to the day, the section shown (its icon, title, time and steps
 * done) following the pager as it turns, and a check once the section is finished. Its main
 * action, reading it, floats at the bottom of the screen ([SectionPlayButton]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeHeader(
    state: UiState,
    sections: List<ProgramSection>,
    shownIndex: Int,
    onBack: () -> Unit,
    onHeight: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    DhikraHeader(modifier = modifier.onSizeChanged { onHeight(it.height) }, bottomPadding = 8.dp) {
        TopAppBar(
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
            },
            title = {
                val section = sections.getOrNull(shownIndex)
                if (section != null) {
                    AnimatedContent(targetState = section, contentKey = { it.id }, label = "headerSection") { shown ->
                        val total = shown.trackableItems.size
                        val done = state.completedIn(shown)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SectionDisc(shown, size = 36.dp)
                            Spacer(Modifier.width(12.dp))
                            // One line, centred with the back arrow and the disc: the title, then its time.
                            Text(
                                text = shown.title,
                                style = MaterialTheme.typography.titleLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (shown.reminderMinutes != null) {
                                Spacer(Modifier.width(10.dp))
                                SectionMeta(shown)
                            }
                            if (total > 0 && done >= total) {
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.section_finished),
                                    tint = MustardLight,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                    }
                }
            },
            expandedHeight = 72.dp,
            windowInsets = WindowInsets(0, 0, 0, 0),
            colors = headerAppBarColors(),
        )
    }
}

/**
 * Reading the section shown, from its first unfinished step: the section view's main action,
 * floating in the bottom corner — «ابدأ», or «تابع» once begun; none once it is finished.
 */
@Composable
internal fun SectionPlayButton(section: ProgramSection, state: UiState, onPlay: (stepId: String) -> Unit) {
    val nextStep = section.items.firstOrNull { it.type.isTrackable && !state.isComplete(it) } ?: return
    val started = state.completedIn(section) > 0
    val description = stringResource(if (started) R.string.continue_now_section else R.string.start_now_section, section.title)
    ExtendedFloatingActionButton(
        onClick = { onPlay(nextStep.id) },
        icon = { Icon(Icons.Filled.PlayArrow, null) },
        text = { Text(stringResource(if (started) R.string.play_continue else R.string.play_start)) },
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    )
}

/** The section's icon on a soft light disc, for the dark header. */
@Composable
private fun SectionDisc(section: ProgramSection, size: Dp) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(Color.White.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(sectionIcon(section.icon), contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.54f))
    }
}

/** The section's reminder time, e.g. "🔔 ٥:١٥", after its title. */
@Composable
private fun SectionMeta(section: ProgramSection) {
    val minutes = section.reminderMinutes ?: return
    val color = Color.White.copy(alpha = 0.72f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(4.dp))
        Text(formatClock(minutes), style = MaterialTheme.typography.labelMedium, color = color)
    }
}


/**
 * The program's name in the app bar: it drops down the user's programs, to switch to another one,
 * and the action that adds a new program.
 */
@Composable
internal fun ProgramSwitcher(
    programs: List<Program>,
    active: Program,
    onSelect: (String) -> Unit,
    onCreate: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val defaultName = stringResource(R.string.default_program_name)
    Box {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClickLabel = stringResource(R.string.programs)) { open = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = active.name.ifBlank { defaultName },
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            programs.forEach { program ->
                val current = program.id == active.id
                DropdownMenuItem(
                    text = { Text(program.name.ifBlank { defaultName }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        if (current) Icon(Icons.Filled.Check, contentDescription = null)
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        open = false
                        onSelect(program.id)
                    },
                    modifier = Modifier.semantics { selected = current },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_program)) },
                leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = {
                    open = false
                    onCreate()
                },
            )
        }
    }
}

@Composable
internal fun OverflowMenu(actions: HomeActions, editSection: (() -> Unit)? = null, editProgram: (() -> Unit)? = null) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, stringResource(R.string.action_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            @Composable
            fun entry(text: Int, icon: @Composable () -> Unit, onClick: () -> Unit) =
                DropdownMenuItem(
                    text = { Text(stringResource(text)) },
                    leadingIcon = icon,
                    onClick = { open = false; onClick() },
                )
            if (editSection != null) {
                entry(R.string.edit_this_section, { Icon(Icons.Outlined.Edit, null) }, editSection)
            } else {
                entry(R.string.action_edit, { Icon(Icons.Outlined.Edit, null) }, editProgram ?: actions.onEdit)
            }
            entry(R.string.prayer_times, { Icon(PrayerIcon, null) }, actions.onOpenPrayerTimes)
            entry(R.string.settings, { Icon(Icons.Outlined.Settings, null) }, actions.onOpenSettings)
            HorizontalDivider()
            entry(R.string.action_text_size, { Text(stringResource(R.string.text_size_glyph), style = MaterialTheme.typography.titleMedium) }, actions.onTextSize)
            entry(R.string.action_reset_today, { Icon(Icons.Outlined.Refresh, null) }, actions.onResetToday)
        }
    }
}
