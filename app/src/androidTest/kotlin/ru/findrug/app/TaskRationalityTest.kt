package ru.findrug.app

import android.graphics.Bitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
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

class TaskRationalityTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storage = "task-reasoning-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, storage)

    @After
    fun clean() {
        context.deleteSharedPreferences(storage)
    }

    private fun initial() =
        ScenarioGame.plan(
                ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
                BudgetAmounts(80, 50, 70),
            )
            .copy(sound = false, animations = false)

    private fun start(state: ScenarioState = initial()): StateRestorationTester {
        store.save(state)
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FinDrugApp(storage) }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithText("Задания").performClick()
        return restoration
    }

    private fun tap(label: String) {
        ui.onNodeWithText(label).assertIsDisplayed().performClick()
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "rationality-v014/$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun unchanged(before: ScenarioState = initial()) {
        val current = store.read()
        assertEquals(before.coins, current.coins)
        assertEquals(before.savings, current.savings)
        assertEquals(before.history, current.history)
        assertEquals(before.periodTasks, current.periodTasks)
        assertEquals(before.rewardClaims, current.rewardClaims)
        assertEquals(before.dailyTaskCompletions, current.dailyTaskCompletions)
        assertEquals(before.periodCompletedTasks, current.periodCompletedTasks)
    }

    private fun retryPage() {
        ui.onNodeWithTag("task-error-explanation").assertIsDisplayed()
        ui.onNodeWithTag("task-retry").assertIsDisplayed()
        ui.onNodeWithText("Получить награду").assertDoesNotExist()
        ui.onNodeWithText("У тебя получилось!").assertDoesNotExist()
    }

    private fun claim(expectedCoins: Int, task: Int) {
        ui.onNodeWithText("У тебя получилось!").assertIsDisplayed()
        tap("Получить награду")
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
        assertEquals(expectedCoins, store.read().coins)
        assertEquals(1, ScenarioGame.taskCompletionsToday(store.read(), task))
    }

    @Test
    fun excessiveSavingRequiresExplanationRetryAndCorrectAnswerEvenAfterRestoration() {
        val restoration = start()
        ui.openCatalogTask(2)
        tap("80 монет")
        retryPage()
        ui.onNodeWithTag("task-error-explanation")
            .assertTextContains("не хватает 50", substring = true)
        unchanged()
        screenshot("01-wrong-80-explanation")
        restoration.emulateSavedInstanceStateRestore()
        retryPage()
        unchanged()
        tap("Попробовать ещё")
        ui.onNodeWithText("80 монет").assertIsDisplayed()
        tap("30 монет")
        unchanged()
        screenshot("02-correct-30-before-reward")
        claim(220, 2)
    }

    @Test
    fun legacySavedWrongAnswerRestoresTheExerciseAndCannotClaimAReward() {
        start(
            initial()
                .copy(
                    taskAttempt =
                        TaskAttempt(
                            2,
                            false,
                            "old-accepted",
                            acceptedAnswer = listOf(2, 80),
                            tipDismissed = true,
                        )
                )
        )
        ui.onNodeWithText("80 монет").assertIsDisplayed()
        ui.onNodeWithText("Получить награду").assertDoesNotExist()
        tap("80 монет")
        retryPage()
        unchanged()
        tap("Попробовать ещё")
        tap("10 монет")
        claim(220, 2)
    }

    @Test
    fun spendingAndUnplannedLeftoversDoNotCompleteTheDeadlineTask() {
        start()
        ui.openCatalogTask(3)
        for (wrong in listOf("Потратить накопления на приятное", "Откладывать случайный остаток")) {
            tap(wrong)
            retryPage()
            unchanged()
            tap("Попробовать ещё")
        }
        tap("Откладывать по 30 каждый период")
        claim(230, 3)
    }

    @Test
    fun comparisonKeepsTheCurrentLevelUntilRetryAndOnlyAllFiveCorrectLevelsCompleteIt() {
        start()
        ui.openCatalogTask(4)
        val levels = (ScenarioContent.task(4).exercise as Exercise.Comparison).levels
        levels.forEachIndexed { index, level ->
            tap(level.options.first { !it.accepted }.label)
            retryPage()
            unchanged()
            tap("Попробовать ещё")
            ui.onNodeWithText("Уровень ${index + 1} из 5").assertExists()
            tap(level.options.single { it.accepted }.label)
            unchanged()
        }
        claim(220, 4)
    }

    @Test
    fun duplicateClickCallbacksCannotSkipTheSecondComparisonLevel() {
        start()
        ui.openCatalogTask(4)
        val click =
            checkNotNull(
                ui.onNodeWithText("2 упаковки — 70 🪙")
                    .fetchSemanticsNode()
                    .config[SemanticsActions.OnClick]
                    .action
            )
        // Deliver two queued clicks to the same rendered action before any recomposition.
        // This is deterministic and does not depend on gesture timing or the device frame rate.
        ui.runOnIdle {
            click()
            click()
        }
        ui.onNodeWithText("Уровень 2 из 5").assertExists()
        ui.onNodeWithText("Уровень 3 из 5").assertDoesNotExist()
        ui.waitUntil(30_000) { store.read().taskAttempt?.draft == listOf(1) }
        unchanged()
    }

    @Test
    fun wrongCategoriesAndBasketContentsReturnToTheSavedDraftWithoutRewards() {
        start()
        ui.openCatalogTask(0)
        val items = ui.displayedCategoryItems()
        items.forEach {
            ui.onNodeWithTag("category-item-${it.id}").performClick()
            ui.onNodeWithTag("category-want").performClick()
        }
        tap("Проверить")
        retryPage()
        unchanged()
        tap("Попробовать ещё")
        items.forEach {
            ui.onNodeWithTag("category-item-${it.id}")
                .assertContentDescriptionEquals("${it.title}: Хочу")
        }
        items
            .filter { it.needed }
            .forEach {
                ui.onNodeWithTag("category-item-${it.id}").performClick()
                ui.onNodeWithTag("category-need").performClick()
            }
        tap("Проверить")
        claim(220, 0)

        val beforeBasket = store.read()
        ui.openCatalogTask(5)
        tap("Игрушка — 60")
        tap("Проверить")
        retryPage()
        unchanged(beforeBasket)
        tap("Попробовать ещё")
        ui.onNodeWithText("Корзина: 60 / 100 🪙").assertExists()
        tap("Корм — 40")
        tap("Проверить")
        claim(250, 5)
    }

    @Test
    fun educationalBudgetShowsItsConditionsAndRequiresCorrectionBeforeSuccess() {
        start()
        ui.openCatalogTask(1)
        ui.onNodeWithText("Учебная задача:", substring = true).assertExists()
        tap("Проверить")
        retryPage()
        unchanged()
        tap("Попробовать ещё")
        ui.onNodeWithContentDescription("Увеличить Нужно").performClick()
        ui.onNodeWithContentDescription("Увеличить Коплю").performClick()
        tap("Проверить")
        claim(230, 1)
    }
}
