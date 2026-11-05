package com.example.dhikra.ui.home

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameMillis
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How long a card takes to fold or unfold. The cards use it too, so both motions stay in sync. */
internal const val FOLD_MILLIS = 360

/** The tapped card stays pinned a little longer than the fold, until every size has settled. */
private const val HOLD_MILLIS = FOLD_MILLIS + 120

/**
 * Which step of the schedule is open. Only one card is open at a time, and only the user opens
 * or folds cards, by tapping them: nothing opens by scrolling or by finishing a step.
 *
 * While cards fold and unfold, the tapped card is pinned at its place on screen every frame, so
 * the cards changing size around it can never make the page jump. Scrolling immediately hands
 * control back to the user.
 */
@Stable
class StepFocus internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val focused: MutableState<String?>,
) {
    /** The open step, or null when every card is folded. */
    val focusedId: String? get() = focused.value

    private var motion: Job? = null

    /** Tap on a card: open it (folding the open one), or fold it if it is the open one. */
    fun toggle(id: String) {
        motion?.cancel()
        // Position before anything changes size: the tapped card keeps it.
        val onScreen = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == id }
        focused.value = if (focused.value == id) null else id
        if (onScreen != null) motion = scope.launch { hold(onScreen.index, onScreen.offset) }
    }

    /**
     * Pins the item at [index] at [offset] every frame until the folds are over. The position is
     * requested before each layout pass, so the item cannot move by even one frame when the cards
     * around it grow or shrink.
     */
    private suspend fun hold(index: Int, offset: Int) {
        val start = withFrameMillis { it }
        var elapsed = 0L
        while (elapsed <= HOLD_MILLIS) {
            // The user is scrolling: never fight the finger (a request would cancel their scroll).
            if (listState.isScrollInProgress) return
            listState.requestScrollToItem(index, -offset)
            elapsed = withFrameMillis { it } - start
        }
    }
}

/** Remembers the open step of one schedule page; it starts on [initial] and survives recreation. */
@Composable
fun rememberStepFocus(listState: LazyListState, key: Any, initial: () -> String?): StepFocus {
    val focused = rememberSaveable(key) { mutableStateOf(initial()) }
    val scope = rememberCoroutineScope()
    return remember(listState, focused, scope) { StepFocus(listState, scope, focused) }
}
