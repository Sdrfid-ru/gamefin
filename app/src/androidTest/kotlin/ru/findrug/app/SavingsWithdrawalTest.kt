package ru.findrug.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

/** Exercises transfers through confirmation using an isolated, persisted player profile. */
class SavingsWithdrawalTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "savings-withdrawal-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, name)

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    private fun planned(savings: Int = 120) =
        ScenarioGame.selectGoal(
                ScenarioGame.plan(
                    ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
                    BudgetAmounts(40, 70, 90),
                ),
                "house",
            )
            .copy(savings = savings, sound = false, animations = false)

    private fun start(state: ScenarioState): StateRestorationTester {
        store.save(state)
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FinDrugApp(name) }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
        tap(state.goal?.title ?: "Выбери свою цель")
        return restoration
    }

    private fun tap(text: String) {
        ui.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    private fun openWithdrawal() {
        ui.onNodeWithTag("savings-withdraw-open").assertIsDisplayed().performClick()
        ui.onNodeWithText("Забрать из копилки").assertIsDisplayed()
    }

    private fun confirmAndContinue() {
        tap("Подтвердить")
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
    }

    private fun backToSavings() {
        ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
        ui.onNodeWithText("Моя копилка").assertIsDisplayed()
    }

    private fun fits() {
        val body = ui.onNodeWithTag("page-content").fetchSemanticsNode()
        assertEquals(0f, body.config[SemanticsProperties.VerticalScrollAxisRange].maxValue(), 1f)
        ui.onNodeWithTag("bottom-navigation").assertIsDisplayed()
    }

    private fun screenshot(fileName: String) {
        ui.waitForIdle()
        val file =
            java.io.File(context.getExternalFilesDir(null), "savings-withdrawal/$fileName.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    @Test
    fun partialWithdrawalNeedsConfirmationAndCancellationKeepsBothAccounts() {
        val initial = planned()
        start(initial)
        fits()
        screenshot("01-savings")
        openWithdrawal()
        ui.onNodeWithContentDescription("Увеличить Забрать").performClick()
        ui.onNodeWithTag("withdrawal-submit").assertTextContains("Забрать 40 монет")
        fits()
        screenshot("02-withdrawal")
        ui.onNodeWithTag("withdrawal-submit").performClick()
        ui.onNodeWithText("В копилке останется 80 монет", substring = true).assertIsDisplayed()
        assertEquals(initial, store.read())
        screenshot("03-confirmation")
        tap("Пока нет")
        assertEquals(initial, store.read())
        ui.onNodeWithTag("withdrawal-submit").performClick()
        confirmAndContinue()

        val saved = store.read()
        assertEquals(240, saved.coins)
        assertEquals(80, saved.savings)
        assertEquals(-40, saved.actual.save)
        assertEquals(initial.coins + initial.savings, saved.coins + saved.savings)
        assertFalse(saved.planEditable)
        assertEquals(initial.history.size + 1, saved.history.size)
        assertEquals(40, saved.history.last().amount)
        assertEquals("balance", saved.history.last().account)
        assertEquals(initial.goalId, saved.goalId)
        backToSavings()
        ui.onNodeWithText("Баланс: 240 🪙 · Накопления: 80 🐷").assertIsDisplayed()
        ui.onNodeWithTag("nav-home").performClick()
        tap("Бюджет")
        ui.onNodeWithText("План зафиксирован").assertIsDisplayed()
    }

    @Test
    fun allFundsIncludesRemainderAndEmptiesTheSavingsAccount() {
        val initial = planned(35)
        start(initial)
        openWithdrawal()
        ui.onNodeWithTag("withdrawal-submit").assertTextContains("Забрать 30 монет")
        ui.onNodeWithTag("withdrawal-all").assertIsDisplayed().performClick()
        ui.onNodeWithText("Забрать 35 монет из копилки?", substring = true).assertIsDisplayed()
        assertEquals(initial, store.read())
        confirmAndContinue()
        assertEquals(235, store.read().coins)
        assertEquals(0, store.read().savings)
        assertEquals(-35, store.read().actual.save)
        ui.onNodeWithTag("withdrawal-submit").assertIsNotEnabled()
        ui.onNodeWithTag("withdrawal-all").assertIsNotEnabled()
        backToSavings()
        ui.onNodeWithTag("savings-withdraw-open").assertIsNotEnabled()
    }

    @Test
    fun smallRemainderClampsAmountAndZeroCannotBeSubmitted() {
        start(planned(5))
        openWithdrawal()
        ui.onNodeWithTag("withdrawal-submit").assertTextContains("Забрать 5 монет")
        ui.onNodeWithContentDescription("Увеличить Забрать").performClick()
        ui.onNodeWithTag("withdrawal-submit").assertTextContains("Забрать 5 монет")
        ui.onNodeWithContentDescription("Уменьшить Забрать").performClick()
        ui.onNodeWithTag("withdrawal-submit").assertTextContains("Забрать 0 монет")
        ui.onNodeWithTag("withdrawal-submit").assertIsNotEnabled()
        ui.onNodeWithContentDescription("Увеличить Забрать").performClick()
        ui.onNodeWithTag("withdrawal-submit").performClick()
        confirmAndContinue()
        assertEquals(205, store.read().coins)
        assertEquals(0, store.read().savings)
    }

    @Test
    fun emptySavingsCannotOpenWithdrawal() {
        val initial = planned(0)
        start(initial)
        ui.onNodeWithTag("savings-withdraw-open").assertIsDisplayed().assertIsNotEnabled()
        assertEquals(initial, store.read())
    }

    @Test
    fun closedPeriodCannotWithdrawCarriedSavings() {
        val initial = planned().copy(periodClosed = true)
        start(initial)
        ui.onNodeWithTag("savings-withdraw-open").assertIsDisplayed().assertIsNotEnabled()
        assertEquals(initial, store.read())
    }

    @Test
    fun withdrawalReducesReachedGoalAndKeepsTheSelectedGoal() {
        start(planned(300))
        ui.onNodeWithText("Получить цель").assertIsEnabled()
        openWithdrawal()
        ui.onNodeWithTag("withdrawal-submit").performClick()
        confirmAndContinue()
        backToSavings()
        ui.onNodeWithText("Получить цель").assertDoesNotExist()
        ui.onNodeWithText("Накоплено 270 / 300 · осталось 30").assertIsDisplayed()
        assertEquals("house", store.read().goalId)
        assertEquals(270, store.read().savings)
        assertTrue(store.read().collectedGoals.isEmpty())
        fits()
        screenshot("04-goal-after-withdrawal")
    }

    @Test
    fun withdrawalWithoutGoalRestoresPageAndChosenAmountWithoutRepeatingTransfer() {
        val initial = planned(85).copy(goalId = null)
        val restoration = start(initial)
        openWithdrawal()
        repeat(2) { ui.onNodeWithContentDescription("Увеличить Забрать").performClick() }
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Забрать из копилки").assertIsDisplayed()
        ui.onNodeWithTag("withdrawal-submit").assertTextContains("Забрать 50 монет")
        assertEquals(initial, store.read())
        fits()
        screenshot("05-without-goal")
        ui.onNodeWithTag("withdrawal-submit").performClick()
        confirmAndContinue()
        val saved = store.read()
        assertEquals(250, saved.coins)
        assertEquals(35, saved.savings)
        assertNull(saved.goalId)
        restoration.emulateSavedInstanceStateRestore()
        assertEquals(saved, store.read())
        backToSavings()
        ui.onNodeWithText("Выбрать цель").assertIsDisplayed()
    }
}
