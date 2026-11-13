package com.example.dhikra.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectDragGestures

/**
 * Press-and-hold reordering for a `LazyColumn`: a long press picks an item up, it follows the
 * finger, and as soon as its middle crosses another item it may take the place of ([canMove]) the
 * move is made ([onMove], with the two items' keys) — the list reorders under it while it is held.
 * Near the top or bottom of the list, the list scrolls along.
 */
@Stable
class ReorderState internal constructor(
    val listState: LazyListState,
    private val scope: CoroutineScope,
    private val canMove: (from: Any, to: Any) -> Boolean,
    private val onMove: (from: Any, to: Any) -> Unit,
) {
    /** Key of the item being dragged, or null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    /** Where the item was in the viewport when picked up, and how far the finger has moved since. */
    private var startOffset = 0
    private var dragged by mutableFloatStateOf(0f)

    private fun info(key: Any): LazyListItemInfo? = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }

    /** How far the item [key] is drawn from its place in the list: the finger's, while dragged. */
    fun offsetOf(key: Any): Float {
        if (key != draggingKey) return 0f
        val current = info(key) ?: return 0f
        return startOffset + dragged - current.offset
    }

    fun isDragging(key: Any): Boolean = key == draggingKey

    internal fun start(key: Any) {
        val item = info(key) ?: return
        draggingKey = key
        startOffset = item.offset
        dragged = 0f
    }

    internal fun drag(dy: Float) {
        val key = draggingKey ?: return
        dragged += dy
        val current = info(key) ?: return
        val top = startOffset + dragged
        val middle = top + current.size / 2f
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull { other ->
            other.key != key && middle >= other.offset && middle <= other.offset + other.size && canMove(key, other.key)
        }
        if (target != null) {
            // A list keeps its first visible item in place: moving that item would scroll the
            // list along with it, so the scroll position is put back.
            val firstIndex = listState.firstVisibleItemIndex
            val firstOffset = listState.firstVisibleItemScrollOffset
            val keepsFirst = current.index == firstIndex || target.index == firstIndex
            onMove(key, target.key)
            if (keepsFirst) scope.launch { listState.scrollToItem(firstIndex, firstOffset) }
        }
        // Held near an edge, the list scrolls so the item can travel further.
        val layout = listState.layoutInfo
        val edge = EDGE_PX
        val step = when {
            top < layout.viewportStartOffset + edge -> -(layout.viewportStartOffset + edge - top) / 6f
            top + current.size > layout.viewportEndOffset - edge ->
                (top + current.size - (layout.viewportEndOffset - edge)) / 6f
            else -> 0f
        }
        if (step != 0f) scope.launch { listState.scrollBy(step) }
    }

    internal fun end() {
        draggingKey = null
        dragged = 0f
    }

    private companion object {
        const val EDGE_PX = 120
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    canMove: (from: Any, to: Any) -> Boolean,
    onMove: (from: Any, to: Any) -> Unit,
): ReorderState {
    val scope = rememberCoroutineScope()
    val currentCanMove by rememberUpdatedState(canMove)
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(listState) {
        ReorderState(listState, scope, { from, to -> currentCanMove(from, to) }, { from, to -> currentOnMove(from, to) })
    }
}

/**
 * A drag handle for the list item [key] (which takes [reorderable] too): it picks the item up at once,
 * without holding first, and moves it with the finger. [onPickUp] as for [reorderable].
 */
fun Modifier.reorderHandle(state: ReorderState, key: Any, onPickUp: () -> Unit = {}): Modifier = this
    .pointerInput(state, key) {
        detectDragGestures(
            onDragStart = {
                onPickUp()
                state.start(key)
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() },
        )
    }

/**
 * Makes the list item [key] reorderable: press and hold, then move. [onPickUp] runs when it is
 * picked up (for haptic feedback, or to fold an open section first).
 */
fun Modifier.reorderable(state: ReorderState, key: Any, onPickUp: () -> Unit = {}): Modifier = this
    .zIndex(if (state.isDragging(key)) 1f else 0f)
    .graphicsLayer {
        val dragging = state.isDragging(key)
        translationY = state.offsetOf(key)
        val scale = if (dragging) 1.02f else 1f
        scaleX = scale
        scaleY = scale
        shadowElevation = if (dragging) 12f else 0f
    }
    .pointerInput(state, key) {
        detectDragGesturesAfterLongPress(
            onDragStart = {
                onPickUp()
                state.start(key)
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { state.end() },
            onDragCancel = { state.end() },
        )
    }
