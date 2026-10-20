package ru.findrug.app

import android.graphics.Bitmap
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

/** Acceptance journeys on a 360 × 640 dp phone, using an isolated persisted profile. */
class RevisionV2Test {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storageName = "revision-v2-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, storageName)

    private var generation by mutableIntStateOf(0)
    private lateinit var viewModels: ViewModelStoreOwner

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
        ui.setContent {
            viewModels = checkNotNull(LocalViewModelStoreOwner.current)
            key(generation) { FinDrugApp(storageName) }
        }
        waitForHome()
    }

    private fun waitForHome() {
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun tap(label: String) {
        ui.onNodeWithText(label).assertIsDisplayed().performClick()
    }

    private fun notice() {
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
    }

    private fun home() {
        ui.onNodeWithTag("nav-home").assertIsDisplayed().performClick()
        waitForHome()
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "revisions-v2/$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun chooseCategory(item: CategoryItem) {
        ui.onNodeWithTag("category-item-${item.id}").assertIsDisplayed().performClick()
        ui.onNodeWithTag(if (item.needed) "category-need" else "category-want")
            .assertIsDisplayed()
            .performClick()
    }

    private fun openFromList(id: Int) {
        tap("Основные")
        repeat(ScenarioContent.tasks.size) {
            if (ui.onAllNodesWithTag("task-$id").fetchSemanticsNodes().isEmpty())
                ui.onNodeWithTag("page-next").assertIsDisplayed().performClick()
        }
        ui.onNodeWithTag("task-$id").assertIsDisplayed().performClick()
    }

    @Test
    fun partialRandomEightSurvivesLeavingAndReloadingAndCompletionStartsANewAttempt() {
        start()
        tap("Задания")
        openFromList(0)
        ui.onNodeWithText("Подсказка лисёнка").assertIsDisplayed()
        ui.onNodeWithTag("task-introduction-close").assertIsDisplayed().performClick()
        val shown = ui.displayedCategoryItems()
        assertEquals(8, shown.map { it.id }.distinct().size)
        val first = shown.first()
        chooseCategory(first)
        val description = "${first.title}: ${if (first.needed) "Нужно" else "Хочу"}"
        ui.onNodeWithTag("category-item-${first.id}").assertContentDescriptionEquals(description)
        ui.waitUntil(30_000) { store.read().taskAttempt?.draft?.count { it != -1 } == 1 }
        val saved = checkNotNull(store.read().taskAttempt)
        assertEquals(0, ScenarioGame.taskCompletionsToday(store.read(), 0))
        screenshot("01-sorting-partial")

        home()
        tap("Задания")
        ui.onNodeWithTag("category-item-${first.id}").assertContentDescriptionEquals(description)
        ui.onNodeWithTag("task-introduction-close").assertDoesNotExist()
        assertEquals(shown.map { it.id }, ui.displayedCategoryItems().map { it.id })
        assertEquals(saved.claimId, store.read().taskAttempt!!.claimId)

        home()
        // Discard the ViewModel and composition, then read the same on-disk profile again.
        ui.runOnIdle {
            viewModels.viewModelStore.clear()
            generation++
        }
        waitForHome()
        tap("Задания")
        ui.onNodeWithTag("category-item-${first.id}").assertContentDescriptionEquals(description)
        assertEquals(saved.categoryItemIds, store.read().taskAttempt!!.categoryItemIds)
        assertEquals(saved.claimId, store.read().taskAttempt!!.claimId)
        screenshot("02-sorting-reloaded")

        shown.forEach(::chooseCategory)
        tap("Проверить")
        tap("Получить награду")
        notice()
        ui.waitUntil(30_000) { ScenarioGame.taskCompletionsToday(store.read(), 0) == 1 }
        assertEquals(220, store.read().coins)
        assertEquals(setOf(0), store.read().periodCompletedTasks)
        assertNull(store.read().taskAttempt)
        screenshot("03-sorting-completed")

        home()
        tap("Задания")
        // The next unfinished task is offered; the completed task is not restarted automatically.
        ui.onNodeWithTag("task-1").assertIsDisplayed()
        openFromList(0)
        ui.onNodeWithTag("task-introduction-close").assertIsDisplayed().performClick()
        val next = ui.displayedCategoryItems()
        ui.waitUntil(30_000) {
            store.read().taskAttempt?.claimId?.let { it != saved.claimId } == true
        }
        assertEquals(8, next.size)
        assertEquals(8, next.map { it.id }.distinct().size)
        assertNotEquals(saved.claimId, store.read().taskAttempt!!.claimId)
        next.forEach {
            ui.onNodeWithTag("category-item-${it.id}")
                .assertContentDescriptionEquals("${it.title}: не распределено")
        }
        assertEquals(1, ScenarioGame.taskCompletionsToday(store.read(), 0))
        screenshot("04-sorting-new-attempt")
    }

    @Test
    fun everyTaskShowsItsOwnFoxTipWithAVisibleCloseButton() {
        start()
        tap("Задания")
        val expected =
            listOf(
                "“Нужно” — это то, без чего трудно обойтись",
                "Сначала подумай о важном",
                "Копить — значит откладывать часть монет сейчас",
                "Большая мечта становится ближе",
                "Выгодно — не всегда значит просто “дешевле”",
                "Сначала — важное, потом — желаемое",
            )
        expected.forEachIndexed { id, prefix ->
            openFromList(id)
            ui.onNodeWithText("Подсказка лисёнка").assertIsDisplayed()
            ui.onNodeWithText(prefix, substring = true).assertExists()
            ui.onNodeWithTag("task-introduction-close").assertIsDisplayed().assertHasClickAction()
            screenshot("tip-${id + 1}")
            ui.onNodeWithTag("task-introduction-close").performClick()
            ui.onNodeWithText("Подсказка лисёнка").assertDoesNotExist()
            ui.onNodeWithTag("page-actions").assertIsDisplayed()
            ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
        }
        assertEquals(200, store.read().coins)
        assertTrue(store.read().dailyTaskCompletions.isEmpty())
    }

    @Test
    fun receivingHouseUnlocksItsRoomAndLetsThePlayerSwitchBackAndReselect() {
        var state = ScenarioGame.selectGoal(planned().copy(coins = 500), "house")
        state = ScenarioGame.deposit(state, 300)
        start(state)
        assertEquals(setOf("default"), store.read().unlockedRooms)
        assertEquals("default", store.read().roomId)
        screenshot("05-room-before-goal")
        tap("Домик для лисёнка")
        tap("Получить цель")
        tap("Подтвердить")
        notice()
        ui.waitUntil(30_000) { store.read().roomId == "pets" }
        assertEquals(1, store.read().collectedGoals["house"])
        assertEquals(0, store.read().savings)
        assertEquals(200, store.read().coins)
        assertTrue("pets" in store.read().unlockedRooms)
        screenshot("06-room-reward")

        ui.onNodeWithText("Вернуть обычную комнату").performScrollTo().performClick()
        notice()
        assertEquals("default", store.read().roomId)
        ui.onNodeWithTag("select-room-pets").performScrollTo().assertIsEnabled().performClick()
        notice()
        assertEquals("pets", store.read().roomId)
        ui.onNodeWithTag("select-room-pets").performScrollTo().assertIsNotEnabled()
        home()
        screenshot("07-room-selected-home")
        assertEquals("pets", ScenarioStore(context, storageName).read().roomId)
    }

    @Test
    fun nextPeriodAllocatesOnlyNewCoinsAndReviewAddsThemToThePreviousPlan() {
        var closed = ScenarioGame.selectGoal(planned(), "house")
        closed = ScenarioGame.buy(closed, "food")
        closed = ScenarioGame.deposit(closed, 70)
        closed =
            ScenarioGame.reward(
                closed,
                1,
                TaskAnswer.Budget(BudgetAmounts(50, 20, 30)),
                "first-period-task",
            )
        closed = ScenarioGame.finish(closed)
        start(closed)
        tap("Посмотреть итоги")
        tap("Дальше")
        tap("Дальше")
        tap("Начать следующий период")
        notice()
        assertEquals(2, store.read().period)
        assertEquals(340, store.read().coins)
        assertEquals(BudgetAmounts(80, 50, 70), store.read().carriedPlan)
        assertNull(store.read().plan)
        ui.onNodeWithText("Новые монеты: 220", substring = true).assertExists()
        ui.onNodeWithText("Сохранено: Нужно 80 · Хочу 50 · Коплю 70", substring = true)
            .assertExists()
        screenshot("08-budget-carried")

        listOf("Нужно" to 10, "Хочу" to 5, "Коплю" to 7).forEach { (category, count) ->
            val increase = ui.onNodeWithContentDescription("Увеличить $category")
            increase.performScrollTo().assertIsDisplayed()
            repeat(count) { increase.performClick() }
        }
        ui.onNodeWithText("Весь бюджет распределён").performScrollTo().assertIsDisplayed()
        tap("Подтвердить план")
        ui.onNodeWithText("80 + 100 = 180").assertIsDisplayed()
        ui.onNodeWithText("50 + 50 = 100").assertIsDisplayed()
        ui.onNodeWithText("70 + 70 = 140").assertIsDisplayed()
        assertNull(store.read().plan)
        screenshot("09-budget-review")
        tap("Продолжить")
        ui.waitUntil(30_000) { store.read().plan == BudgetAmounts(100, 50, 70) }
        assertEquals(BudgetAmounts(180, 100, 140), store.read().cumulativePlan)
        assertEquals(340, store.read().coins)
        assertEquals(70, store.read().savings)
    }
}
