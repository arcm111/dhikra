package com.example.dhikra.ui.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.theme.DhikraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Cards of the step schedule open only when tapped: the first card starts open, scrolling and
 * finishing steps never open anything, and a tapped card opens or folds in place.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleFocusTest {

    @get:Rule
    val rule = createComposeRule()

    private val bundle = ScheduleFixture.bundle()

    private val dhikr = "سبحان الله وبحمده"
    private val kahf = "سورة الكهف"

    // Words only visible while each card is open.
    private val dhikrText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ"
    private val hadithText = "يَخْشَعُ"
    private val kahfText = "ٱلۡكَهۡفِ"
    private val ikhlasText = "الصَّمَدُ"

    private fun show(section: String = "shurooq", progress: Map<String, Int> = emptyMap()): MutableState<UiState> {
        val state = mutableStateOf(
            UiState(
                loading = false,
                program = bundle.program,
                library = bundle.library,
                schedule = bundle.program.resolvedWith(bundle.library),
                day = "2026-11-04",
                progress = progress,
                selectedSectionId = section,
                nowSectionId = section,
            )
        )
        rule.setContent {
            DhikraTheme {
                HomeScreen(
                    state = state.value,
                    snackbarHostState = remember { SnackbarHostState() },
                    actions = HomeActions(
                        onSelectSection = {},
                        onEdit = {},
                        onOpenLibrary = {},
                        onOpenSettings = {},
                        onTextSize = {},
                        onResetToday = {},
                        onIncrement = {},
                        onDecrement = {},
                        onRepetitionRead = { _, _ -> },
                        onSetComplete = { item, done ->
                            state.value = state.value.let {
                                it.copy(progress = if (done) it.progress + (item.id to item.target) else it.progress - item.id)
                            }
                        },
                        onResetItem = {},
                    ),
                )
            }
        }
        rule.waitForIdle()
        return state
    }

    private fun bounds(text: String): Rect? =
        rule.onAllNodes(hasText(text)).fetchSemanticsNodes().firstOrNull()?.boundsInRoot

    private fun boundsOf(text: String): Rect = checkNotNull(bounds(text)) { "«$text» is not shown" }

    private fun isShown(text: String): Boolean =
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private val list get() = rule.onNodeWithTag(ScheduleListTag)

    /** Plays [millis] of animation frame by frame, recording the top of [text] each frame. */
    private fun track(text: String, millis: Long = 900): List<Float> {
        val tops = mutableListOf<Float>()
        repeat((millis / 16).toInt()) {
            rule.mainClock.advanceTimeByFrame()
            bounds(text)?.let { tops += it.top }
        }
        return tops
    }

    @Test
    fun onlyTheFirstCardStartsOpen() {
        show()
        assertTrue(isShown(dhikrText))
        assertFalse(isShown(hadithText))
        assertFalse(isShown(kahfText))
    }

    @Test
    fun tappingACard_opensItInPlace_andFoldsTheOpenOne() {
        show()
        val before = boundsOf(kahf).top
        rule.mainClock.autoAdvance = false
        rule.onNode(hasText(kahf)).performClick()
        track(kahf).forEach { assertEquals("the tapped card must not move", before, it, 1.5f) }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertTrue(isShown(kahfText))
        assertFalse(isShown(dhikrText))
    }

    @Test
    fun tappingTheOpenCard_foldsItInPlace() {
        show()
        val before = boundsOf(dhikr).top
        rule.mainClock.autoAdvance = false
        rule.onNode(hasText(dhikr)).performClick()
        track(dhikr).forEach { assertEquals("the folding card must not move", before, it, 1.5f) }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        assertFalse(isShown(dhikrText))
    }

    @Test
    fun finishingAStep_opensNothingElse() {
        show(progress = mapOf("subhanallah_wabihamdihi" to 100, "dua_al_ahd" to 1, "hadith_nasai_5536" to 1, "kahf_1_10" to 1))
        // Open the dua and finish it: the next card must stay folded.
        rule.onNode(hasText("الاستعاذة بالله العظيم")).performClick()
        rule.waitForIdle()
        rule.onNode(hasText("تمّ") and hasClickAction()).performClick()
        rule.mainClock.advanceTimeBy(2_000)
        rule.waitForIdle()
        assertFalse(isShown(ikhlasText))
    }

    @Test
    fun scrolling_opensNothing() {
        show()
        repeat(6) {
            list.performTouchInput { swipeUp(startY = bottom * 0.85f, endY = top + height * 0.25f, durationMillis = 250) }
            rule.waitForIdle()
        }
        repeat(6) {
            list.performTouchInput { swipeDown(startY = top + height * 0.2f, endY = bottom * 0.9f, durationMillis = 250) }
            rule.waitForIdle()
        }
        assertTrue("the first card is still the open one", isShown(dhikrText))
        assertFalse(isShown(hadithText))
        assertFalse(isShown(kahfText))
    }

    @Test
    fun shortSection_swipesChangeNothing() {
        show(section = "duha")
        val task = "صلاة الضحى والدعاء بعدها"
        val before = boundsOf(task).top
        list.performTouchInput { swipeUp() }
        rule.waitForIdle()
        assertEquals(before, boundsOf(task).top, 1.5f)
    }
}
