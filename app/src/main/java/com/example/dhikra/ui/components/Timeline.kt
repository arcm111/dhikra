package com.example.dhikra.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.dhikra.R
import com.example.dhikra.data.ArabicText
import com.example.dhikra.ui.theme.LocalDhikraPalette

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

/** Width of the timeline column on the start (right) side of the schedule. */
val TimelineGutter = 40.dp
val StepNodeSize = 28.dp
/** A finished step's check circle: smaller than the numbered ones, so the steps still to read stand out. */
private val DoneNodeSize = 22.dp
private val LineWidth = 2.dp
private val NodeGap = 5.dp
private val RowSpacing = 14.dp

/**
 * One row of the step schedule: a vertical line with a [node] in the start gutter and the row
 * [content] next to it. Consecutive rows join their line segments into one continuous track.
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
    content: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val reached = scheme.primary
    val pending = scheme.outlineVariant
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
                val x = if (layoutDirection == LayoutDirection.Rtl) size.width - half else half
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
        Box(
            modifier = Modifier
                .width(TimelineGutter)
                .padding(top = nodeCenter - nodeSize / 2),
            contentAlignment = Alignment.TopCenter,
        ) { node() }
        Box(Modifier.weight(1f).padding(bottom = RowSpacing)) { content() }
    }
}

/**
 * Numbered circle of a step: an outlined number while upcoming, a tinted ring with a soft
 * breathing halo for the current step, and a smaller filled check once done. The node keeps its
 * full [StepNodeSize] slot either way, so the timeline does not move when a step is finished.
 */
@Composable
fun StepNode(number: Int, state: StepState, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = when (state) {
            StepState.Done -> scheme.primary
            StepState.Current -> scheme.primaryContainer
            StepState.Upcoming -> scheme.surface
        },
        label = "nodeContainer",
    )
    val border by animateColorAsState(
        targetValue = when (state) {
            StepState.Upcoming -> scheme.outline.copy(alpha = 0.55f)
            else -> scheme.primary
        },
        label = "nodeBorder",
    )
    val circle by animateDpAsState(
        targetValue = if (state == StepState.Done) DoneNodeSize else StepNodeSize,
        label = "nodeSize",
    )
    val description = stringResource(R.string.step_label, ArabicText.toArabicDigits(number)) +
        when (state) {
            StepState.Done -> "، " + stringResource(R.string.step_state_done)
            StepState.Current -> "، " + stringResource(R.string.step_state_current)
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
                .size(circle)
                .clip(CircleShape)
                .background(container)
                .border(if (state == StepState.Current) 2.dp else 1.5.dp, border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = state == StepState.Done,
                transitionSpec = {
                    (scaleIn(initialScale = 0.3f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.3f) + fadeOut())
                },
                label = "nodeContent",
            ) { done ->
                if (done) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(13.dp),
                    )
                } else {
                    Text(
                        text = ArabicText.toArabicDigits(number),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state == StepState.Current) scheme.onPrimaryContainer
                        else scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

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

/** Gold star marking a group heading on the timeline. */
@Composable
fun HeadingNode(modifier: Modifier = Modifier) {
    Icon(
        imageVector = StarOrnamentIcon,
        contentDescription = null,
        tint = LocalDhikraPalette.current.ornament,
        modifier = modifier.size(HeadingNodeSize),
    )
}

/** Gold medal that closes a finished schedule. */
@Composable
fun FinishNode(modifier: Modifier = Modifier) {
    val palette = LocalDhikraPalette.current
    Box(
        modifier = modifier
            .size(StepNodeSize)
            .clip(CircleShape)
            .background(palette.ornament),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = StarOrnamentIcon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
    }
}
