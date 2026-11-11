package com.example.dhikra.ui.counter

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
import com.example.dhikra.R
import com.example.dhikra.data.DailyCounter
import com.example.dhikra.data.LibraryEntry
import com.example.dhikra.ui.components.CounterDial
import com.example.dhikra.ui.components.LightNavigationBarIcons
import com.example.dhikra.ui.components.NightButton
import com.example.dhikra.ui.components.NightInk
import com.example.dhikra.ui.components.UndoIcon
import com.example.dhikra.ui.components.starLattice
import com.example.dhikra.ui.components.verticalScrollbar
import com.example.dhikra.ui.theme.LocalDhikraPalette
import com.example.dhikra.ui.theme.ReadingStyles
import com.example.dhikra.ui.uiDigits
import kotlinx.coroutines.delay
import androidx.compose.ui.text.font.FontWeight

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
    onCount: () -> Unit,
    onUndo: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)
    LightNavigationBarIcons()
    val haptic = LocalHapticFeedback.current
    val target = DailyCounter.target(count)
    var pressed by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(confirmReset) {
        if (confirmReset) {
            delay(3_000)
            confirmReset = false
        }
    }
    val count1: () -> Unit = {
        // Reaching a target is felt more strongly than the other repetitions.
        haptic.performHapticFeedback(if (count + 1 == target) HapticFeedbackType.Confirm else HapticFeedbackType.SegmentTick)
        onCount()
    }
    val latestCount by rememberUpdatedState(count1)

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
                Spacer(Modifier.widthIn(min = 48.dp))
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val scroll = rememberScrollState()
                Text(
                    text = entry.text,
                    style = ReadingStyles.dhikr(textScale).copy(textAlign = TextAlign.Center),
                    color = Color.White,
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NightButton(
                    UndoIcon,
                    stringResource(R.string.counter_undo),
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onUndo()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = count > 0,
                )
                NightButton(
                    Icons.Filled.Refresh,
                    stringResource(if (confirmReset) R.string.counter_reset_confirm else R.string.counter_reset),
                    onClick = {
                        if (confirmReset) {
                            confirmReset = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset()
                        } else {
                            confirmReset = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = count > 0,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
