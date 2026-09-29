package ru.findrug.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

/** Isolated local profile; these tests never replace the user's main or demo save. */
class ScenarioFlowTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "flow-test-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, name)

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    private fun start(state: ScenarioState) {
        store.save(state.copy(animations = false, sound = false))
        ui.setContent { FinDrugApp(name) }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun state() = runBlocking { store.read() }

    private fun tap(text: String) {
        val node = ui.onNodeWithText(text)
        node.assertIsDisplayed().performClick()
    }

    private fun nextPage() {
        ui.onNodeWithTag("page-next").assertIsDisplayed().performClick()
    }

    private fun pageToTag(tag: String) {
        repeat(8) {
            if (ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
            nextPage()
        }
        fail("Page with $tag not found")
    }

    private fun continueNotice() {
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithText("Продолжить").performClick()
    }

    private fun planned() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
            BudgetAmounts(40, 70, 90),
        )

    @Test
    fun pettingWithoutAnimationsStillRespondsAndNeverChangesMoney() {
        start(planned())
        val before = state()
        ui.onNodeWithContentDescription("Погладить лисёнка").performClick()
        ui.onAllNodesWithText("♥").assertCountEquals(2)
        assertEquals(before, state())
        ui.onNodeWithContentDescription("Настройки").performClick()
        ui.onNodeWithText("Звуки лисёнка").assertExists()
    }

    @Test
    fun editConfirmedPlanThenPurchaseCapAndLockPlan() {
        start(planned())
        ui.onNodeWithText("Бюджет").performClick()
        ui.onNodeWithContentDescription("Увеличить Нужно").assertIsDisplayed().performClick()
        ui.onNodeWithContentDescription("Уменьшить Коплю").assertIsDisplayed().performClick()
        tap("Сохранить план")
        ui.onNodeWithText("Наш план готов!").assertExists()
        assertEquals(BudgetAmounts(40, 70, 90), state().plan)
        tap("Продолжить")
        ui.waitUntil(30_000) { state().plan == BudgetAmounts(50, 70, 80) }
        assertEquals(BudgetAmounts(50, 70, 80), state().plan)
        ui.onNodeWithText("Магазин").performClick()
        tap("Хочу")
        nextPage()
        nextPage()
        tap("70 🪙")
        ui.onNodeWithText("Подтвердить").performClick()
        continueNotice()
        assertEquals(130, state().coins)
        assertEquals(1, state().profile!!.hat)
        assertTrue("cap" in state().ownedItems)
        ui.onNodeWithText("Кепка куплена").assertExists()
        tap("Гардероб")
        tap("Шапки")
        ui.onNodeWithText("Кепка").assertExists()
        ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
        ui.onNodeWithText("Бюджет").performClick()
        ui.onNodeWithText("План зафиксирован").assertExists()
        assertFalse(state().planEditable)
    }

    @Test
    fun receiveGoalAndSelectNextWithoutLosingCollection() {
        var s = ScenarioGame.selectGoal(planned().copy(coins = 500), "house")
        s = ScenarioGame.deposit(s, 300)
        start(s)
        ui.onNodeWithText("Домик для лисёнка").performClick()
        tap("Получить цель")
        ui.onNodeWithText("Подтвердить").performClick()
        continueNotice()
        ui.onNodeWithText("Моя коллекция").assertExists()
        assertEquals(200, state().coins)
        assertEquals(0, state().savings)
        assertEquals(1, state().collectedGoals["house"])
        tap("Выбрать новую цель")
        nextPage()
        tap("Выбрать цель")
        continueNotice()
        assertEquals("telescope", state().goalId)
        assertEquals(1, state().collectedGoals["house"])
    }

    @Test
    fun fourthRepeatStillPaysAndReturnsHome() {
        var s = planned().copy(completedTasks = (0..5).toSet())
        repeat(3) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "paid-$it", true) }
        start(s)
        ui.onNodeWithText("Задания").performClick()
        tap("Тренировка")
        pageToTag("task-4")
        ui.onNodeWithTag("task-4").assertIsDisplayed().performClick()
        tap("2 упаковки — 70 🪙")
        tap("Получить награду")
        continueNotice()
        ui.onNodeWithText("Игрок").assertExists()
        assertEquals(240, state().coins)
        assertEquals(4, state().periodTasks)
        assertEquals(4, state().paidRepeats)
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val file = java.io.File(context.getExternalFilesDir(null), "layout-v7/$name.png")
        file.parentFile!!.mkdirs()
        InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().let { bitmap ->
            file.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
            bitmap.recycle()
        }
    }

    private fun chooseCategory(id: Int, needed: Boolean) {
        ui.onNodeWithTag("category-item-$id").assertIsDisplayed().performClick()
        ui.onNodeWithTag(if (needed) "category-need" else "category-want")
            .assertIsDisplayed()
            .performClick()
    }

    private fun openRecovery(task: Int) {
        tap("Давай заработаем новые")
        ui.onNodeWithText("Монеты закончились — бывает!").assertExists()
        pageToTag("training-$task")
        ui.onNodeWithTag("training-$task").assertIsDisplayed().performClick()
    }

    @Test
    fun budgetAllocatesLastFiveCoinsAndAllowsFreeRemainder() {
        start(planned().copy(period = 3, coins = 465, plan = BudgetAmounts(140, 100, 220)))
        tap("Бюджет")
        ui.onNodeWithText("Осталось распределить: 5").assertExists()
        ui.onNodeWithText("Сохранить план").assertIsEnabled()
        tap("Сохранить план")
        ui.onNodeWithText("5 монет").assertExists()
        tap("Изменить план")
        ui.onNodeWithContentDescription("Увеличить Коплю").performClick()
        ui.onNodeWithText("Осталось распределить: 0").assertExists()
        tap("Сохранить план")
        ui.onNodeWithText("225 монет").assertExists()
        tap("Продолжить")
        ui.waitUntil(30_000) { state().plan == BudgetAmounts(140, 100, 225) }
        assertEquals(465, state().coins)
    }

    @Test
    fun budgetAcceptsScreenshotAmountsUsingActualBalance() {
        start(planned().copy(period = 3, coins = 465, plan = BudgetAmounts(140, 150, 170)))
        tap("Бюджет")
        ui.onNodeWithText("Сохранить план").assertIsEnabled()
        ui.onNodeWithContentDescription("Увеличить Коплю").performClick()
        ui.onNodeWithText("Осталось распределить: 0").assertExists()
        tap("Сохранить план")
        tap("Продолжить")
        ui.waitUntil(30_000) { state().plan == BudgetAmounts(140, 150, 175) }
        assertEquals(465, state().coins)
    }

    @Test
    fun budgetReviewCanBeEditedAndOnlyContinueCommitsIt() {
        start(planned())
        tap("Бюджет")
        ui.onNodeWithContentDescription("Увеличить Нужно").assertIsDisplayed().performClick()
        ui.onNodeWithContentDescription("Уменьшить Коплю").assertIsDisplayed().performClick()
        tap("Сохранить план")
        ui.onNodeWithText("Наш план готов!").assertExists()
        assertEquals(BudgetAmounts(40, 70, 90), state().plan)
        assertEquals(200, state().coins)
        tap("Изменить план")
        ui.onNodeWithContentDescription("Уменьшить Хочу").assertIsDisplayed().performClick()
        tap("Сохранить план")
        ui.onNodeWithText("10 монет").assertExists()
        screenshot("01-budget-review")
        ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
        tap("Сохранить план")
        ui.onNodeWithText("10 монет").assertExists()
        tap("Продолжить")
        ui.waitUntil(30_000) { state().plan == BudgetAmounts(50, 60, 80) }
        assertEquals(200, state().coins)
        assertTrue(state().planEditable)
    }

    @Test
    fun classificationChecksWholeLayoutAllowsCorrectionAndReset() {
        start(planned())
        tap("Задания")
        ui.onNodeWithTag("task-0").assertIsDisplayed().performClick()
        (0..5).forEach { chooseCategory(it, false) }
        ui.onNodeWithText("У тебя получилось!").assertDoesNotExist()
        tap("Проверить")
        screenshot("02-sort-first-check")
        ui.onNodeWithTag("category-item-1")
            .assertContentDescriptionEquals("Корм: Хочу, проверь выбор")
        assertEquals(200, state().coins)
        tap("Подсказка")
        tap("Понятно")
        tap("Сбросить")
        listOf("category-need", "category-want").forEach { tag ->
            ui.onNodeWithTag(tag).assertTextContains("Предметов: 0")
        }
        ui.onNodeWithText("Проверить").assertIsNotEnabled()
        // Arrange incorrectly first, then correct an already placed item.
        (0..5).forEach { chooseCategory(it, it in setOf(2, 5)) }
        tap("Проверить")
        ui.onNodeWithTag("category-item-1")
            .assertContentDescriptionEquals("Корм: Хочу, проверь выбор")
        ui.onNodeWithTag("category-item-1").assertIsDisplayed()
        screenshot("02-sort-correction")
        val source = ui.onNodeWithTag("category-item-1")
        val destination = ui.onNodeWithTag("category-need").fetchSemanticsNode().boundsInRoot.center
        val origin = source.fetchSemanticsNode().boundsInRoot.topLeft
        source.performTouchInput { swipe(center, destination - origin, 400) }
        ui.onNodeWithTag("category-item-1").assertContentDescriptionEquals("Корм: Нужно")
        tap("Проверить")
        tap("Получить награду")
        continueNotice()
        assertEquals(220, state().coins)
        assertEquals(setOf(0), state().completedTasks)
    }

    @Test
    fun emptyWalletOffersThreeTrainingsAndComparisonReturnsHomeWithTenCoins() {
        start(planned().copy(coins = 0, paidRepeats = 20))
        tap("Давай заработаем новые")
        ui.onNodeWithTag("training-0").assertIsDisplayed()
        nextPage()
        ui.onNodeWithTag("training-4").assertIsDisplayed()
        nextPage()
        ui.onNodeWithTag("training-1").assertIsDisplayed()
        ui.onNodeWithTag("page-previous").performClick()
        ui.onNodeWithText("Монеты закончились — бывает!").assertIsDisplayed()
        screenshot("03-empty-wallet")
        ui.onNodeWithTag("training-4").assertIsDisplayed().performClick()
        tap("1 упаковка — 50 🪙")
        assertEquals(0, state().coins)
        tap("2 упаковки — 70 🪙")
        tap("Получить награду")
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Получено +10 монет!\nБаланс: 0 → 10")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        screenshot("04-recovery-reward")
        continueNotice()
        ui.onNodeWithText("Игрок").assertExists()
        assertEquals(10, state().coins)
        assertEquals(21, state().paidRepeats)
        assertTrue(state().completedTasks.isEmpty())
    }

    @Test
    fun shortSortingHasThreeItemsAndPaysWithoutCompletingMainTask() {
        start(planned().copy(coins = 0))
        openRecovery(0)
        ui.onNodeWithTag("category-item-2").assertDoesNotExist()
        listOf("category-need", "category-want").forEach { tag ->
            ui.onNodeWithTag(tag).assertTextContains("Предметов: 0")
        }
        listOf(0, 1, 5).forEach { chooseCategory(it, it != 0) }
        tap("Проверить")
        tap("Получить награду")
        continueNotice()
        ui.onNodeWithText("Игрок").assertExists()
        assertEquals(10, state().coins)
        assertTrue(state().completedTasks.isEmpty())
    }

    @Test
    fun shortBudgetUsesFiftyAndRewardsOnlyAfterValidAllocation() {
        start(planned().copy(coins = 0))
        openRecovery(1)
        ui.onNodeWithText("50 монет").assertExists()
        repeat(4) {
            ui.onNodeWithContentDescription("Увеличить Нужно").assertIsDisplayed().performClick()
        }
        repeat(2) {
            ui.onNodeWithContentDescription("Увеличить Коплю").assertIsDisplayed().performClick()
        }
        ui.onNodeWithText("Проверить").assertIsNotEnabled()
        assertEquals(0, state().coins)
        ui.onNodeWithContentDescription("Уменьшить Нужно").assertIsDisplayed().performClick()
        tap("Проверить")
        tap("Получить награду")
        continueNotice()
        assertEquals(10, state().coins)
        assertEquals(BudgetAmounts(40, 70, 90), state().plan)
        assertEquals(BudgetAmounts(), state().actual)
        assertEquals(0, state().savings)
    }

    @Test
    fun periodResultsPraiseSavingsAndExplainOverspendingTogether() {
        var s = ScenarioGame.plan(planned(), BudgetAmounts(40, 30, 30))
        s = ScenarioGame.buy(s, "food")
        s = ScenarioGame.buy(s, "toy")
        s = ScenarioGame.deposit(ScenarioGame.selectGoal(s, "house"), 30)
        s = ScenarioGame.finish(ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "task"))
        start(s)
        tap("Итоги")
        tap("Дальше")
        ui.onNodeWithText("Ты отложил 30 монет на свою цель. Это шаг к мечте!").assertExists()
        ui.onNodeWithText("На желания ушло на 30 монет больше плана.", substring = true)
            .assertExists()
        ui.onNodeWithText("Что получилось").assertIsDisplayed()
        screenshot("05-personal-result")
        assertEquals(2, state().reports.last().score)
        assertFalse(state().reports.last().successful)
    }
}
