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
        repeat(id) { ui.onNodeWithTag("page-next").performClick() }
        ui.onNodeWithTag("task-$id").performClick()
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
        ui.onNodeWithTag("savings-forecast").assertTextContains("10 периодов", substring = true)
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
    fun reflectionAndExplanationSurviveRestoreAndNeverChangeReward() {
        val restoration = start()
        val before = store.read()
        openTask(4)
        tap("2 упаковки — 70 🪙")
        ui.onNodeWithText("Почему ты так выбрал?").assertExists()
        fits()
        screenshot("reflection-question")
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithTag("reflection-option-1").assertIsDisplayed().performClick()
        ui.onNodeWithText("Не всегда.", substring = true).assertIsDisplayed()
        fits()
        screenshot("reflection-explanation")
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Не всегда.", substring = true).assertIsDisplayed()
        assertEquals(before, store.read())
        ui.onNodeWithTag("reflection-done").performClick()
        restoration.emulateSavedInstanceStateRestore()
        ui.onNodeWithText("Награда: +20 монет").assertExists()
        tap("Получить награду")
        notice()
        assertEquals(220, store.read().coins)
        assertEquals(1, store.read().periodTasks)
        openTask(4)
        tap("2 упаковки — 70 🪙")
        ui.onNodeWithTag("reflection-skip").assertDoesNotExist()
        tap("Получить награду")
        notice()
        assertEquals(230, store.read().coins)
    }

    @Test
    fun skippingReflectionKeepsNormalRewardAndTrainingDoesNotAsk() {
        start()
        openTask(2)
        tap("30 монет")
        ui.onNodeWithTag("reflection-skip").assertIsDisplayed().performClick()
        assertEquals(200, store.read().coins)
        tap("Получить награду")
        notice()
        assertEquals(220, store.read().coins)
        ui.onNodeWithTag("nav-home").performClick()
        tap("Задания")
        tap("Тренировка")
        ui.onNodeWithTag("page-next").performClick()
        ui.onNodeWithTag("task-4").performClick()
        tap("2 упаковки — 70 🪙")
        ui.onNodeWithTag("reflection-skip").assertDoesNotExist()
        tap("Получить награду")
        notice()
        assertEquals(230, store.read().coins)
    }
}
