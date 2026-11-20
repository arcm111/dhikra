package io.github.arcm111.dhikra.ui.counter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.arcm111.dhikra.R
import io.github.arcm111.dhikra.data.DailyCounter
import io.github.arcm111.dhikra.data.LibraryEntry
import io.github.arcm111.dhikra.ui.components.CounterDial
import io.github.arcm111.dhikra.ui.components.LightNavigationBarIcons
import io.github.arcm111.dhikra.ui.components.NightButton
import io.github.arcm111.dhikra.ui.components.NightInk
import io.github.arcm111.dhikra.ui.components.UndoIcon
import io.github.arcm111.dhikra.ui.components.starLattice
import io.github.arcm111.dhikra.ui.components.verticalScrollbar
import io.github.arcm111.dhikra.ui.theme.LocalDhikraPalette
import io.github.arcm111.dhikra.ui.theme.ReadingStyles
import io.github.arcm111.dhikra.ui.uiDigits
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight
import io.github.arcm111.dhikra.ui.components.CounterBar
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.arcm111.dhikra.overlay.CounterOverlayService
import io.github.arcm111.dhikra.ui.components.findActivity
import android.os.SystemClock
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import io.github.arcm111.dhikra.ui.components.ReaderText
import io.github.arcm111.dhikra.ui.components.NightTextColors
import io.github.arcm111.dhikra.ui.components.PlayToggle
import io.github.arcm111.dhikra.ui.components.PlayButtonSize
import io.github.arcm111.dhikra.ui.home.followHighlight

/**
 * The daily counter of one dhikr, full screen on the night background of the reader: its text,
 * and the bead dial counting today's repetitions with no end. The dial's ring fills from one
 * target to the next (100, 300, 500, 1 000 … see [DailyCounter]); a tap anywhere counts.
 */
@Composable
fun DailyCounterScreen(
    entry: LibraryEntry,
    count: Int,
    textScale: Float,
    lettersPerSecond: Float,
    onCount: () -> Unit,
    /** A repetition read along with play, in so many ms: the speed learns from it, then +1. */
    onRepetitionRead: (Long) -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    LightNavigationBarIcons()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val target = DailyCounter.target(count)
    var pressed by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    // Reading along, as in the section reader: the words light up at the learned speed, one
    // repetition per cycle, and a tap counts it and starts the next.
    var playing by rememberSaveable { mutableStateOf(false) }
    var cycle by remember { mutableIntStateOf(0) }
    var cycleStart by remember { mutableLongStateOf(0L) }
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(3_000)
            confirmReset = false
        }
    }
    val count1: () -> Unit = {
        // Reaching a target is felt more strongly than the other repetitions.
        haptic.performHapticFeedback(if (count + 1 == target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick)
        if (playing) {
            onRepetitionRead(SystemClock.uptimeMillis() - cycleStart)
            cycle++
        } else {
            onCount()
        }
    }
    val latestCount by rememberUpdatedState(count1)

    // The floating counter: shown at once when allowed, else after "Display over other apps" is
    // turned on in Settings and the user comes back.
    val activity = context.findActivity()
    var explainOverlay by remember { mutableStateOf(false) }
    var awaitingOverlay by remember { mutableStateOf(false) }
    val popOut = {
        playing = false
        CounterOverlayService.start(context, entry.id)
        activity?.moveTaskToBack(true)
    }
    LifecycleResumeEffect(awaitingOverlay) {
        if (awaitingOverlay && CounterOverlayService.canShow(context)) {
            awaitingOverlay = false
            popOut()
        }
        onPauseOrDispose {}
    }
    if (explainOverlay) {
        AlertDialog(
            onDismissRequest = { explainOverlay = false },
            icon = { Icon(painterResource(R.drawable.ic_pop_out), null) },
            title = { Text(stringResource(R.string.overlay_permission_title)) },
            text = { Text(stringResource(R.string.overlay_permission_text)) },
            confirmButton = {
                TextButton(onClick = {
                    explainOverlay = false
                    awaitingOverlay = true
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                        )
                    }
                }) { Text(stringResource(R.string.overlay_permission_go)) }
            },
            dismissButton = {
                TextButton(onClick = { explainOverlay = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    val palette = LocalDhikraPalette.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.nightStart, palette.nightEnd)))
            .starLattice(Color.White.copy(alpha = 0.035f), spacing = 58.dp, starRadius = 15.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = { latestCount() },
                )
            }
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().widthIn(max = 560.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, stringResource(R.string.close), tint = NightInk)
                }
                // Only today's count of the target it climbs to, e.g. «٤٠٠/٥٠٠».
                Text(
                    text = uiDigits("$count/$target"),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.SemiBold,
                    color = NightInk,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                // Sharing a picture of the day's count: small and faint, out of the way.
                IconButton(onClick = {
                    CounterShareCard.share(
                        context = context,
                        title = entry.title,
                        text = entry.text,
                        count = count,
                        nightTop = palette.nightStart.toArgb(),
                        nightBottom = palette.nightEnd.toArgb(),
                    )
                }) {
                    Icon(Icons.Filled.Share, stringResource(R.string.counter_share), tint = Color.White.copy(alpha = 0.55f), modifier = Modifier.size(20.dp))
                }
                // Floating the counter over other apps: as quiet as the share icon.
                IconButton(onClick = { if (CounterOverlayService.canShow(context)) popOut() else explainOverlay = true }) {
                    Icon(
                        painterResource(R.drawable.ic_pop_out),
                        stringResource(R.string.overlay_pop_out),
                        tint = Color.White.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val scroll = rememberScrollState()
                ReaderText(
                    text = entry.text,
                    style = ReadingStyles.dhikr(textScale).copy(textAlign = TextAlign.Center),
                    colors = NightTextColors,
                    playing = playing,
                    cycle = cycle,
                    lettersPerSecond = lettersPerSecond,
                    onCycleStart = { cycleStart = it },
                    onProgress = { followHighlight(scroll, it) },
                    modifier = Modifier
                        .verticalScrollbar(scroll, thumb = Color.White.copy(alpha = 0.35f))
                        .verticalScroll(scroll)
                        .padding(vertical = 8.dp)
                        .padding(horizontal = if (scroll.maxValue > 0) 12.dp else 0.dp),
                )
            }
            CounterDial(
                done = count,
                target = target,
                complete = false,
                pressed = pressed,
                onCount = count1,
                ringProgress = DailyCounter.stage(count),
            )
            Spacer(Modifier.height(24.dp))
            // The same bar as a counted step's, without its play button.
            CounterBar(
                undoEnabled = count > 0,
                resetEnabled = count > 0,
                confirmReset = confirmReset,
                onUndo = {
                    haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onUndo()
                },
                onReset = {
                    if (confirmReset) {
                        confirmReset = false
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onReset()
                    } else {
                        confirmReset = true
                    }
                },
                centerSize = PlayButtonSize,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                PlayToggle(playing = playing, onToggle = { playing = !playing })
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
