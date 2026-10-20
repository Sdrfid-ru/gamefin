package ru.findrug.app

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

/** Phone acceptance coverage for inline hints and the order of this period's tasks. */
class TaskHintsAndOrderTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storageName = "task-hints-order-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, storageName)

    @After
    fun clean() {
        context.deleteSharedPreferences(storageName)
    }

    private fun planned() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок", "Рыжик"))),
            BudgetAmounts(80, 50, 70),
        )

    private fun start(state: ScenarioState = planned()) {
        store.save(state.copy(sound = false, animations = false))
        ui.setContent { FinDrugApp(storageName) }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
        tap("Задания")
    }

    private fun tap(label: String) {
        ui.onNodeWithText(label).assertIsDisplayed().performClick()
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "task-hints-order/$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun openTask(id: Int) {
        tap("Основные")
        repeat(ScenarioContent.tasks.size) {
            if (ui.onAllNodesWithTag("task-$id").fetchSemanticsNodes().isEmpty())
                ui.onNodeWithTag("page-next").assertIsDisplayed().performClick()
        }
        ui.onNodeWithTag("task-$id").assertIsDisplayed().performClick()
    }

    private fun assertInside(label: String, child: Rect, parent: Rect) {
        assertTrue("$label left edge is clipped", child.left >= parent.left - 1f)
        assertTrue("$label right edge is clipped", child.right <= parent.right + 1f)
        assertTrue("$label top edge is clipped", child.top >= parent.top - 1f)
        assertTrue("$label bottom edge is clipped", child.bottom <= parent.bottom + 1f)
    }

    private fun assertInlineHintFits() {
        ui.onAllNodes(isDialog()).assertCountEquals(0)
        val panel =
            ui.onNodeWithTag("task-introduction-panel")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        val fox =
            ui.onNodeWithTag("task-introduction-fox")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        val bubble =
            ui.onNodeWithTag("task-introduction-bubble")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        val close =
            ui.onNodeWithTag("task-introduction-close")
                .assertIsDisplayed()
                .assertHasClickAction()
                .fetchSemanticsNode()
                .boundsInRoot
        val text =
            ui.onNodeWithTag("task-introduction-text")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        val content = ui.onNodeWithTag("page-content").fetchSemanticsNode().boundsInRoot
        val actions =
            ui.onNodeWithTag("page-actions").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val navigation =
            ui.onNodeWithTag("bottom-navigation")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        assertInside("Hint", panel, ui.onRoot().fetchSemanticsNode().boundsInRoot)
        assertInside("Fox", fox, panel)
        assertInside("Bubble", bubble, panel)
        assertInside("Close button", close, bubble)
        val safeTextArea =
            Rect(
                bubble.left + bubble.width * (64f / 814f),
                bubble.top + bubble.height * (56f / 613f),
                bubble.right - bubble.width * (64f / 814f),
                bubble.top + bubble.height * (476f / 613f),
            )
        assertInside("Hint text viewport", text, safeTextArea)
        ui.onAllNodes(hasClickAction() and hasAnyAncestor(hasTestTag("page-actions")))
            .fetchSemanticsNodes()
            .forEach { node ->
                ui.onNode(SemanticsMatcher("action ${node.id}") { it.id == node.id })
                    .assertIsDisplayed()
                assertInside("Task action", node.boundsInRoot, actions)
            }
        assertTrue("Fox must be left of the bubble", fox.right <= bubble.left + 1f)
        assertTrue("Close button must be in the upper right", close.center.x > bubble.center.x)
        assertTrue("Close button must stay above the hint text", close.bottom <= text.top + 1f)
        assertTrue("Hint must not cover the exercise", content.bottom <= panel.top + 1f)
        assertTrue("Hint must not cover task actions", panel.bottom <= actions.top + 1f)
        assertTrue("Task actions must not cover navigation", actions.bottom <= navigation.top + 1f)
    }

    @Test
    fun allSixHintsAreInlineClosableReopenableAndFullyReachableOnCompactPhone() {
        start()
        val expectedPrefixes =
            listOf(
                "“Нужно” — это то, без чего трудно обойтись",
                "Сначала подумай о важном",
                "Копить — значит откладывать часть монет сейчас",
                "Большая мечта становится ближе",
                "Выгодно — не всегда значит просто “дешевле”",
                "Сначала — важное, потом — желаемое",
            )
        expectedPrefixes.forEachIndexed { id, prefix ->
            openTask(id)
            assertInlineHintFits()
            ui.onNodeWithTag("task-introduction-text").assertTextContains(prefix, substring = true)
            screenshot("hint-${id + 1}-inline")

            // Long copy is reachable by scrolling within its bubble; the close button stays fixed.
            val text = ui.onNodeWithTag("task-introduction-text")
            val scroll =
                text.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
            if (scroll.maxValue() > 0f) {
                text.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 10_000f) }
                ui.waitForIdle()
                val end =
                    text.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
                assertEquals(
                    "The end of the hint must be reachable",
                    end.maxValue(),
                    end.value(),
                    1f,
                )
                assertInlineHintFits()
                screenshot("hint-${id + 1}-end")
            }

            ui.onNodeWithTag("task-introduction-close").performClick()
            ui.onNodeWithTag("task-introduction-panel").assertDoesNotExist()
            ui.onNodeWithTag("task-introduction-open").assertIsDisplayed().performClick()
            assertInlineHintFits()
            ui.onNodeWithTag("task-introduction-text").assertTextContains(prefix, substring = true)
            ui.onNodeWithTag("task-introduction-close").performClick()
            ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
        }
        assertEquals(200, store.read().coins)
        assertEquals(0, store.read().periodTasks)
        assertTrue(store.read().dailyTaskCompletions.isEmpty())
    }

    private fun assertOrder(expected: List<Int>, completed: Set<Int>) {
        tap("Основные")
        expected.forEachIndexed { index, id ->
            ui.onNodeWithTag("task-$id").assertIsDisplayed()
            ui.onNodeWithText(ScenarioContent.task(id).title).assertIsDisplayed()
            ui.onNodeWithText("${index + 1} из ${expected.size}").assertIsDisplayed()
            if (id in completed) {
                ui.onNodeWithText("✓ Выполнено в этом периоде").assertIsDisplayed()
                ui.onNodeWithTag("task-$id").assertTextContains("Повторить", substring = true)
            } else {
                ui.onNodeWithText("✓ Выполнено в этом периоде").assertDoesNotExist()
                ui.onNodeWithTag("task-$id").assertTextContains("Начать", substring = true)
            }
            if (index == 0 || index == expected.lastIndex) screenshot("order-page-${index + 1}")
            if (index < expected.lastIndex)
                ui.onNodeWithTag("page-next").assertIsDisplayed().performClick()
        }
        ui.onNodeWithTag("page-next").assertIsNotEnabled()
    }

    @Test
    fun unfinishedTasksComeFirstAndOnlyCurrentPeriodCompletionsMoveToTheEnd() {
        start(
            planned()
                .copy(
                    period = 2,
                    completedTasks = (0..5).toSet(),
                    periodCompletedTasks = setOf(0, 2, 4),
                )
        )
        assertOrder(listOf(1, 3, 5, 0, 2, 4), setOf(0, 2, 4))
        assertEquals((0..5).toSet(), store.read().completedTasks)
    }

    @Test
    fun completingATaskMovesItBehindEveryUnfinishedTask() {
        start(planned().copy(completedTasks = setOf(2, 4), periodCompletedTasks = setOf(2, 4)))
        ui.onNodeWithTag("task-0").assertIsDisplayed().performClick()
        ui.onNodeWithTag("task-introduction-close").assertIsDisplayed().performClick()
        ui.displayedCategoryItems().forEach { item ->
            ui.onNodeWithTag("category-item-${item.id}").assertIsDisplayed().performClick()
            ui.onNodeWithTag(if (item.needed) "category-need" else "category-want")
                .assertIsDisplayed()
                .performClick()
        }
        tap("Проверить")
        tap("Получить награду")
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
        ui.onNodeWithTag("task-1").assertIsDisplayed()
        assertOrder(listOf(1, 3, 5, 0, 2, 4), setOf(0, 2, 4))
        assertEquals(220, store.read().coins)
        assertEquals(1, ScenarioGame.taskCompletionsToday(store.read(), 0))
    }
}
