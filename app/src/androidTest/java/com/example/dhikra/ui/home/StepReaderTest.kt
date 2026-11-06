package com.example.dhikra.ui.home

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.dhikra.R
import com.example.dhikra.ui.UiState
import com.example.dhikra.ui.theme.DhikraTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The full-screen reader of the schedule: tapping a step (or a text of an unfolded group) opens it,
 * a swipe up goes on to the next step without going back to the list, and closing it brings the
 * step being read back into view.
 */
@RunWith(AndroidJUnit4::class)
class StepReaderTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val bundle = ScheduleFixture.bundle()
    private val schedule = bundle.program.resolvedWith(bundle.library)

    // Titles of the cards of the list.
    private val dhikr = "سبحان الله وبحمده"
    private val dua = "دعاء العهد"
    private val hadith = "الاستعاذة من علم لا ينفع"
    private val kahf = "سورة الكهف"
    private val kursi = "آية الكرسي"
    private val muawwidhat = "المعوّذتان"
    private val falaq = "سورة الفلق"
    private val nas = "سورة الناس"

    // Words only read in the reader: the list only shows the titles.
    private val dhikrText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ"
    private val duaText = "لبيك اللهم لبيك"
    private val hadithText = "يَخْشَعُ"
    private val kahfText = "ٱلۡكَهۡفِ"
    private val falaqText = "حَاسِدٍ"
    private val nasText = "الْخَنَّاسِ"
    private val kursiText = "الْعَلِيُّ الْعَظِيمُ"

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    /** Every step of the section [id] done, the texts of its groups included. */
    private fun allDone(id: String): Map<String, Int> =
        checkNotNull(schedule.section(id)).trackableItems
            .flatMap { if (it.isGroup) it.members else listOf(it) }
            .associate { it.id to it.target }

    /** Shows the home screen; [height] shrinks it, so that the list has to scroll. */
    private fun show(
        section: String = "shurooq",
        progress: Map<String, Int> = emptyMap(),
        height: Dp? = null,
    ): MutableState<UiState> {
        val state = mutableStateOf(
            UiState(
                loading = false,
                program = bundle.program,
                library = bundle.library,
                schedule = schedule,
                day = "2026-11-04",
                progress = progress,
                selectedSectionId = section,
                nowSectionId = section,
            )
        )
        rule.setContent {
            DhikraTheme {
                Box(if (height == null) Modifier.fillMaxSize() else Modifier.height(height)) {
                    HomeScreen(
                        state = state.value,
                        snackbarHostState = remember { SnackbarHostState() },
                        actions = HomeActions(
                            onSelectSection = {},
                            onSelectProgram = {},
                            onCreateProgram = {},
                            onEdit = {},
                            onOpenLibrary = {},
                            onOpenSettings = {},
                            onTextSize = {},
                            onResetToday = {},
                            onIncrement = { item ->
                                state.value = state.value.let {
                                    it.copy(progress = it.progress + (item.id to ((it.progress[item.id] ?: 0) + 1)))
                                }
                            },
                            onDecrement = {},
                            onRepetitionRead = { _, _ -> },
                            onSetComplete = { item, done ->
                                val texts = if (item.isGroup) item.members else listOf(item)
                                state.value = state.value.let { current ->
                                    current.copy(
                                        progress = texts.fold(current.progress) { map, text ->
                                            if (done) map + (text.id to text.target) else map - text.id
                                        }
                                    )
                                }
                            },
                            onResetItem = {},
                        ),
                    )
                }
            }
        }
        rule.waitForIdle()
        return state
    }

    private fun boundsOf(text: String): Rect =
        rule.onNode(hasText(text)).fetchSemanticsNode().boundsInRoot

    private fun isShown(text: String): Boolean =
        rule.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private val list get() = rule.onNodeWithTag(ScheduleListTag)
    private val reader get() = rule.onNodeWithTag(StepReaderTag)

    /** Taps the card titled [title] in the list. */
    private fun open(title: String) {
        val card = hasText(title) and hasClickAction()
        list.performScrollToNode(card)
        rule.onNode(card).performClick()
        rule.waitForIdle()
    }

    private fun click(@StringRes label: Int) {
        rule.onNode(hasText(string(label)) and hasClickAction()).performClick()
        rule.waitForIdle()
    }

    private fun swipeUpInReader() {
        reader.performTouchInput { swipeUp(startY = height * 0.8f, endY = height * 0.25f, durationMillis = 250) }
        rule.waitForIdle()
    }

    private fun swipeDownInReader() {
        reader.performTouchInput { swipeDown(startY = height * 0.25f, endY = height * 0.8f, durationMillis = 250) }
        rule.waitForIdle()
    }

    @Test
    fun theListOnlyShowsTheTitles() {
        show()
        reader.assertDoesNotExist()
        assertFalse(isShown(dhikrText))
        assertFalse(isShown(hadithText))
        assertFalse(isShown(kahfText))
    }

    @Test
    fun scrollingTheList_opensNothing() {
        show()
        repeat(4) {
            list.performTouchInput { swipeUp(startY = bottom * 0.85f, endY = top + height * 0.25f, durationMillis = 250) }
            rule.waitForIdle()
        }
        repeat(4) {
            list.performTouchInput { swipeDown(startY = top + height * 0.2f, endY = bottom * 0.9f, durationMillis = 250) }
            rule.waitForIdle()
        }
        reader.assertDoesNotExist()
    }

    @Test
    fun tappingACard_opensTheReaderAtThatStep() {
        show()
        open(kahf)
        reader.assertExists()
        assertTrue(isShown(kahfText))
        assertFalse("the steps before it are not shown", isShown(dhikrText))
    }

    @Test
    fun swipingUp_goesToTheNextStep_andSwipingDownBackToThePreviousOne() {
        val state = show()
        open(dhikr)
        assertTrue(isShown(dhikrText))

        swipeUpInReader()
        assertTrue("the next step is shown", isShown(duaText))
        assertFalse(isShown(dhikrText))
        assertEquals("a swipe does not count", 0, state.value.progress["subhanallah_wabihamdihi"] ?: 0)

        swipeDownInReader()
        assertTrue(isShown(dhikrText))
        assertFalse(isShown(duaText))
    }

    @Test
    fun tappingACountedStep_countsIt() {
        val state = show()
        open(dhikr)
        reader.performClick()
        rule.waitForIdle()
        assertEquals(1, state.value.progress["subhanallah_wabihamdihi"])
    }

    @Test
    fun finishingAStep_thenSwipingUp_goesToTheNextOne() {
        val state = show()
        open(hadith)
        assertTrue(isShown(hadithText))

        click(R.string.mark_done)
        assertEquals(1, state.value.progress["hadith_nasai_5536"])
        assertTrue("finishing does not move on by itself", isShown(hadithText))

        swipeUpInReader()
        assertTrue(isShown(kahfText))
        assertFalse(isShown(hadithText))
    }

    @Test
    fun nextStepButton_goesToTheNextStep() {
        show(progress = mapOf("dua_al_ahd" to 1))
        open(dua)
        assertTrue(isShown(duaText))

        click(R.string.reader_next_step)
        assertTrue(isShown(hadithText))
        assertFalse(isShown(duaText))
    }

    @Test
    fun groups_unfoldInTheList_andHaveOneScreenPerText() {
        show(section = "protection")
        assertFalse("folded, a group only shows its title", isShown(falaq))

        open(muawwidhat)
        reader.assertDoesNotExist()
        assertTrue(isShown(falaq) && isShown(nas))

        open(falaq)
        assertTrue(isShown(falaqText))
        assertFalse(isShown(nasText))

        swipeUpInReader()
        assertTrue(isShown(nasText))
        assertFalse(isShown(falaqText))
    }

    @Test
    fun aTextOfAGroup_opensTheReaderAtThatText() {
        show(section = "protection")
        open(muawwidhat)
        open(nas)
        reader.assertExists()
        assertTrue(isShown(nasText))
        assertFalse(isShown(falaqText))
    }

    @Test
    fun aGroup_foldsBackOnASecondTap() {
        show(section = "protection")
        open(muawwidhat)
        assertTrue(isShown(nas))
        open(muawwidhat)
        assertFalse(isShown(nas))
    }

    @Test
    fun readingToTheLastStep_andClosing_bringsItIntoView() {
        val steps = checkNotNull(schedule.section("shurooq")).trackableItems.size
        show(progress = allDone("shurooq"), height = 560.dp)
        open(dhikr)
        repeat(steps - 1) { click(R.string.reader_next_step) }
        assertTrue("the last step is shown", isShown(kursiText))

        click(R.string.reader_back_to_list)
        reader.assertDoesNotExist()
        rule.onNode(hasText(kursi) and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun back_closesTheReader() {
        show(height = 560.dp)
        open(kahf)
        reader.assertExists()

        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        reader.assertDoesNotExist()
        rule.onNode(hasText(kahf) and hasClickAction()).assertIsDisplayed()
    }

    @Test
    fun tappingATask_checksItWithoutOpeningTheReader() {
        val state = show(section = "duha")
        rule.onNode(hasText("صلاة الضحى والدعاء بعدها")).performClick()
        rule.waitForIdle()
        reader.assertDoesNotExist()
        assertEquals(1, state.value.progress["salat_duha"])
    }

    @Test
    fun shortSection_swipesChangeNothing() {
        show(section = "duha")
        val task = "صلاة الضحى والدعاء بعدها"
        val before = boundsOf(task).top
        list.performTouchInput { swipeUp() }
        rule.waitForIdle()
        reader.assertDoesNotExist()
        assertEquals(before, boundsOf(task).top, 1.5f)
    }
}
