package com.example.dhikra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.uiDigits
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.compositionLocalOf

/** Progress of one step in the schedule. */
enum class StepState { Done, Current, Upcoming }

/**
 * Which line segments a timeline row draws, and whether each one is already "reached" (coloured)
 * — the line fills in from the first step down to the current one.
 */
@Immutable
data class TimelineSegments(
    val hasTop: Boolean,
    val hasBottom: Boolean,
    val topReached: Boolean,
    val bottomReached: Boolean,
)

/**
 * Which side of the schedule the timeline runs along. [Start] is the reading side (the right in
 * Arabic, the left in an LTR layout); [End] is the other one.
 */
enum class TimelineSide { Start, End }

/** Width of the timeline column. */
val TimelineGutter = 40.dp
/** A step's circle on the timeline, numbered or checked: one size, so the timeline stays even. */
val StepNodeSize = 26.dp
private val LineWidth = 2.dp
private val NodeGap = 5.dp
private val RowSpacing = 14.dp

/**
 * One row of the step schedule: the row [content], with a vertical line and its [node] in a
 * gutter on the [side] of the schedule (by default the start: the right, in Arabic). Consecutive rows join their line segments into one continuous track.
 *
 * @param nodeCenter vertical position of the node's centre, measured from the top of the row,
 * so it can line up with the first line of the content.
 */
@Composable
fun TimelineRow(
    segments: TimelineSegments,
    nodeCenter: Dp,
    nodeSize: Dp,
    node: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    side: TimelineSide = TimelineSide.Start,
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // A section all done: its line turns gold from end to end, like its circles.
    val gold = LocalSectionComplete.current
    val reached = if (gold) LocalDhikraPalette.current.done else scheme.primary
    val pending = if (gold) reached else scheme.outlineVariant
    val top by animateColorAsState(
        targetValue = if (segments.topReached) reached else pending,
        animationSpec = tween(450),
        label = "timelineTop",
    )
    val bottom by animateColorAsState(
        targetValue = if (segments.bottomReached) reached else pending,
        animationSpec = tween(450),
        label = "timelineBottom",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val half = TimelineGutter.toPx() / 2f
                // The start side is on the right in RTL, on the left in LTR.
                val onRight = (side == TimelineSide.Start) == (layoutDirection == LayoutDirection.Rtl)
                val x = if (onRight) size.width - half else half
                val centerY = nodeCenter.toPx()
                val gap = (nodeSize / 2 + NodeGap).toPx()
                val width = LineWidth.toPx()
                if (segments.hasTop && centerY - gap > 0f) {
                    drawLine(top, Offset(x, 0f), Offset(x, centerY - gap), width, StrokeCap.Round)
                }
                if (segments.hasBottom) {
                    drawLine(bottom, Offset(x, centerY + gap), Offset(x, size.height), width, StrokeCap.Round)
                }
            },
    ) {
        val gutter = @Composable {
            Box(
                modifier = Modifier
                    .width(TimelineGutter)
                    .padding(top = nodeCenter - nodeSize / 2),
                contentAlignment = Alignment.TopCenter,
            ) { node() }
        }
        if (side == TimelineSide.Start) gutter()
        Box(Modifier.weight(1f).padding(bottom = RowSpacing)) { content() }
        if (side == TimelineSide.End) gutter()
    }
}

/**
 * A step's circle on the timeline: a light green disc with its number while still to do (with a
 * soft breathing halo on the current step), filled with a check once done — the checked and
 * unchecked states of one round check box. [number] is shown inside while the step is to do.
 */
@Composable
fun StepNode(number: Int, state: StepState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    // The whole section done: its circles turn gold, like everything finished in the app.
    val gold = LocalSectionComplete.current
    val tone = if (gold) LocalDhikraPalette.current.done else scheme.primary
    // To do: a light green disc with the step's number; done: filled green with a check.
    val container by animateColorAsState(
        targetValue = when (state) {
            StepState.Done -> tone
            else -> scheme.primary.copy(alpha = 0.10f).compositeOver(scheme.surface)
        },
        label = "nodeContainer",
    )
    val description = stringResource(R.string.step_label, uiDigits(number)) +
        when (state) {
            StepState.Done -> stringResource(R.string.list_separator) + stringResource(R.string.step_state_done)
            StepState.Current -> stringResource(R.string.list_separator) + stringResource(R.string.step_state_current)
            StepState.Upcoming -> ""
        }
    Box(
        modifier = modifier
            .size(StepNodeSize)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (state == StepState.Current) Halo(scheme.primary)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(container)
                .border(if (state == StepState.Current) 2.dp else 1.5.dp, if (state == StepState.Done) tone else scheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (state != StepState.Done) {
                Text(
                    text = uiDigits(number),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.primary,
                    maxLines = 1,
                )
            }
            AnimatedVisibility(
                visible = state == StepState.Done,
                enter = scaleIn(initialScale = 0.3f) + fadeIn(),
                exit = scaleOut(targetScale = 0.3f) + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (gold) GoldInk else scheme.onPrimary,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** Whether the section shown is all done: its step circles and check boxes then turn gold. */
val LocalSectionComplete = compositionLocalOf { false }

/** Soft ring that breathes out of the current step's node. */
@Composable
private fun Halo(color: Color) {
    val transition = rememberInfiniteTransition(label = "halo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearOutSlowInEasing)),
        label = "haloProgress",
    )
    Canvas(Modifier.size(StepNodeSize)) {
        drawCircle(
            color = color.copy(alpha = 0.30f * (1f - t)),
            radius = size.minDimension / 2f + 10.dp.toPx() * t,
        )
    }
}

val HeadingNodeSize = 14.dp

/** Small, soft mustard star marking a group heading on the timeline. */
@Composable
fun HeadingNode(modifier: Modifier = Modifier) {
    Icon(
        imageVector = StarOrnamentIcon,
        contentDescription = null,
        tint = LocalDhikraPalette.current.ornament.copy(alpha = 0.55f),
        modifier = modifier.size(HeadingNodeSize),
    )
}
