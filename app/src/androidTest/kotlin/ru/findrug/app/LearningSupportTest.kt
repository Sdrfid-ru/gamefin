package ru.findrug.app

import androidx.compose.ui.graphics.asAndroidBitmap
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

class LearningSupportTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "learning-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, name)

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    private fun planned() =
        ScenarioGame.plan(
                ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
                BudgetAmounts(40, 70, 90),
            )
            .copy(sound = false, animations = false)

    private fun start(state: ScenarioState = planned()): StateRestorationTester {
        store.save(state)
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FinDrugApp(name) }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
        return restoration
    }

    private fun tap(text: String) {
        ui.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    private fun notice() {
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
    }

    private fun openTask(id: Int) {
        ui.onNodeWithTag("nav-home").performClick()
        tap("Задания")
        ui.openCatalogTask(id)
    }

    private fun screenshot(fileName: String) {
        ui.waitForIdle()
        val file = java.io.File(context.getExternalFilesDir(null), "learning/$fileName.png")
        file.parentFile!!.mkdirs()
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }

    private fun fits() {
        val body = ui.onNodeWithTag("page-content").fetchSemanticsNode()
        assertEquals(0f, body.config[SemanticsProperties.VerticalScrollAxisRange].maxValue(), 1f)
        ui.onNodeWithTag("bottom-navigation").assertIsDisplayed()
    }

    @Test
    fun forecastUpdatesWithoutMovingMoneyAndRecalculatesAfterDepositAndGoalChange() {
        val initial = ScenarioGame.selectGoal(planned(), "house").copy(savings = 120)
        start(initial)
        tap("Домик для лисёнка")
        ui.onNodeWithTag("savings-forecast").assertTextContains("6 периодов", substring = true)
        fits()
        screenshot("savings-forecast")
        ui.onNodeWithContentDescription("Увеличить Отложить").performClick()
        ui.onNodeWithTag("savings-forecast").assertTextContains("5 периодов", substring = true)
        repeat(4) { ui.onNodeWithContentDescription("Уменьшить Отложить").performClick() }
        ui.onNodeWithTag("savings-forecast").assertTextContains("больше нуля", substring = true)
        ui.onNodeWithText("Отложить 0 монет").assertIsNotEnabled()
        assertEquals(initial, store.read())
        repeat(3) { ui.onNodeWithContentDescription("Увеличить Отложить").performClick() }
        tap("Отложить 30 монет")
        tap("Подтвердить")
        notice()
        assertEquals(170, store.read().coins)
        assertEquals(150, store.read().savings)
        ui.onNodeWithTag("savings-forecast").assertTextContains("5 периодов", substring = true)
        tap("Изменить цель")
        ui.onNodeWithTag("page-next").performClick()
        tap("Выбрать цель")
        notice()
        ui.onNodeWithTag("savings-forecast").assertTextContains("5 периодов", substring = true)
    }

    @Test
    fun hypotheticalForecastNeverEnablesAnUnaffordableDeposit() {
        start(ScenarioGame.selectGoal(planned(), "house").copy(savings = 120, coins = 10))
        tap("Домик для лисёнка")
        ui.onNodeWithTag("savings-forecast").assertTextContains("6 периодов", substring = true)
        ui.onNodeWithText("Отложить 30 монет").assertIsNotEnabled()
        fits()
        tap("Отложить 10")
        ui.onNodeWithTag("savings-forecast").assertTextContains("18 периодов", substring = true)
        ui.onNodeWithText("Отложить 10 монет").assertIsEnabled()
        assertEquals(10, store.read().coins)
    }

    @Test
    fun incorrectComparisonExplainsRetryAndOnlyCorrectSolutionEnablesRewardAfterRestore() {
        val restoration = start()
        val before = store.read()
        openTask(4)
        tap("1 упаковка — 50 🪙")
        ui.onNodeWithTag("task-retry").assertIsDisplayed()
        ui.onNodeWithText("Получить награду").assertDoesNotExist()
        fits()
        screenshot("incorrect-comparison")
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithTag("task-error-explanation").assertTextContains("50", substring = true)
        fits()
        assertEquals(before.coins, store.read().coins)
        assertEquals(before.savings, store.read().savings)
        assertEquals(before.history, store.read().history)
        assertEquals(before.rewardClaims, store.read().rewardClaims)
        assertEquals(before.periodTasks, store.read().periodTasks)
        tap("Попробовать ещё")
        ui.solveComparisonLevels()
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Награда: +20 монет").assertExists()
        tap("Получить награду")
        notice()
        assertEquals(220, store.read().coins)
        assertEquals(1, store.read().periodTasks)
        openTask(4)
        ui.solveComparisonLevels()
        tap("Получить награду")
        notice()
        assertEquals(230, store.read().coins)
    }

    @Test
    fun reasonableSavingAndCorrectTrainingAnswersReceiveTheirNormalRewards() {
        start()
        openTask(2)
        tap("30 монет")
        ui.onNodeWithText("У тебя получилось!").assertIsDisplayed()
        assertEquals(200, store.read().coins)
        tap("Получить награду")
        notice()
        assertEquals(220, store.read().coins)
        ui.onNodeWithTag("nav-home").performClick()
        tap("Задания")
        tap("Тренировка")
        ui.onNodeWithTag("page-next").performClick()
        ui.onNodeWithTag("task-4").performClick()
        ui.dismissTaskIntroduction()
        tap("2 упаковки — 70 🪙")
        ui.onNodeWithText("У тебя получилось!").assertIsDisplayed()
        tap("Получить награду")
        notice()
        assertEquals(230, store.read().coins)
    }
}
