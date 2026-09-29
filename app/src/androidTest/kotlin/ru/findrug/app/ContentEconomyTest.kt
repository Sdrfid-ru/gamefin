package ru.findrug.app

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.app.ui.screens.tasks.TaskExercisePage
import ru.findrug.app.ui.theme.GameTheme
import ru.findrug.domain.*

class ContentEconomyTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "economy-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, name)

    @After
    fun cleanup() {
        context.deleteSharedPreferences(name)
    }

    private fun start(state: ScenarioState) {
        store.save(state.copy(sound = false, animations = false))
        ui.setContent { FinDrugApp(name) }
        ui.waitUntil(30000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun base() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
            BudgetAmounts(100, 100),
        )

    private fun tap(text: String) {
        ui.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    private fun back() {
        ui.onNodeWithContentDescription("Назад").performClick()
    }

    private fun notice() {
        ui.waitUntil(30000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
    }

    private fun training() {
        ui.onNodeWithTag("page-next").performClick()
        ui.onNodeWithTag("training-4").assertIsDisplayed().performClick()
        tap("2 упаковки — 70 🪙")
    }

    @Test
    fun exhaustedRewardsStillAllowCompletionAndNextPeriod() {
        start(
            base()
                .copy(
                    coins = 100,
                    taskEarnings = 150,
                    actual = BudgetAmounts(40),
                    planLocked = true,
                )
        )
        tap("Задания")
        tap("Тренировка")
        ui.onNodeWithTag("page-next").performClick()
        ui.onNodeWithTag("task-4").performClick()
        tap("2 упаковки — 70 🪙")
        ui.onNodeWithText("Без денежной награды: лимит периода исчерпан. Прогресс сохранится.")
            .assertIsDisplayed()
        tap("Завершить задание")
        notice()
        assertEquals(100, store.read().coins)
        assertEquals(1, store.read().periodTasks)
        tap("Завершить")
        tap("Дальше")
        tap("Дальше")
        tap("Начать следующий период")
        notice()
        assertEquals(2, store.read().period)
        assertEquals(320, store.read().coins)
        assertEquals(0, store.read().taskEarnings)
    }

    @Test
    fun zeroBalanceRecoveryIsFiniteAndOffersPeriodExit() {
        start(
            base()
                .copy(coins = 0, taskEarnings = 150, actual = BudgetAmounts(100), planLocked = true)
        )
        repeat(2) {
            ui.onNodeWithTag("recovery-home").performClick()
            training()
            tap("Получить награду")
            notice()
        }
        assertEquals(20, store.read().coins)
        assertEquals(20, store.read().recoveryEarnings)
        tap("Магазин")
        repeat(2) { ui.onNodeWithTag("page-next").performClick() }
        tap("20 🪙")
        tap("Подтвердить")
        notice()
        back()
        assertEquals(0, store.read().coins)
        ui.onNodeWithTag("recovery-home").performClick()
        training()
        tap("Завершить задание")
        notice()
        assertEquals(0, store.read().coins)
        ui.onNodeWithTag("recovery-home").performClick()
        tap("Подвести итоги")
        ui.waitUntil(30000) { store.read().periodClosed }
        assertEquals(20, ScenarioStore(context, name).read().recoveryEarnings)
    }

    @Test
    fun carriedBalanceCanFundPurchasesBeyondBaseIncome() {
        start(base().copy(coins = 1000, actual = BudgetAmounts(180), planLocked = true))
        tap("Магазин")
        tap("40 🪙")
        tap("Подтвердить")
        notice()
        assertEquals(960, store.read().coins)
        assertEquals(220, store.read().actual.need)
    }

    @Test
    fun desireWarningExplainsTradeoffAndAllowsCancelOrPurchase() {
        start(ScenarioGame.plan(base(), BudgetAmounts(70, 30, 100)))
        tap("Магазин")
        tap("Хочу")
        tap("40 🪙")
        ui.onNodeWithText("не хватит 10 монет", substring = true).assertIsDisplayed()
        val file = java.io.File(context.getExternalFilesDir(null), "education/wants-warning.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        tap("Пока нет")
        assertEquals(200, store.read().coins)
        tap("40 🪙")
        tap("Подтвердить")
        notice()
        assertEquals(160, store.read().coins)
        assertEquals(40, store.read().actual.want)
    }

    @Test
    fun newSituationsRenderAndEvaluateFromDataOnly() {
        val definitions =
            listOf(
                ScenarioTask(
                    71,
                    "Новые покупки",
                    "Бюджет",
                    "Разложи",
                    20,
                    Exercise.Categories(
                        listOf(
                            CategoryItem(10, "Обед", "🥣", true),
                            CategoryItem(90, "Мяч", "🏀", false),
                        ),
                        "Нужен обед",
                        "Верно",
                    ),
                ),
                ScenarioTask(
                    88,
                    "План поездки",
                    "Бюджет",
                    "Распредели",
                    20,
                    Exercise.Budget(80, 30, 20, "Оставь на обед и цель", "Осталось {remaining}"),
                ),
                ScenarioTask(
                    103,
                    "Сравни цены",
                    "Покупки",
                    "Выбери",
                    20,
                    Exercise.Choice(
                        "Одинаковые товары",
                        "Найди выгоднее",
                        listOf(
                            ChoiceOption(11, "Цена 60", false, "Есть дешевле"),
                            ChoiceOption(42, "Цена 40", true, "Выгоднее"),
                        ),
                    ),
                ),
                ScenarioTask(
                    209,
                    "Новая корзина",
                    "Покупки",
                    "Выбери",
                    20,
                    Exercise.Basket(
                        70,
                        "Выбери обед",
                        listOf(
                            BasketItem(7, "Обед", "🥣", 25, true),
                            BasketItem(93, "Мяч", "🏀", 50),
                        ),
                        "Осталось {remaining}",
                    ),
                ),
            )
        TaskDefinitions.validate(definitions)
        var index by mutableIntStateOf(0)
        val results = mutableListOf<String>()
        ui.setContent {
            GameTheme {
                key(index) {
                    var hint by remember { mutableStateOf<String?>(null) }
                    TaskExercisePage(definitions[index], hint, {}) { answer ->
                        try {
                            results += TaskEvaluator.evaluate(definitions[index], answer)
                            if (index < definitions.lastIndex) index++
                        } catch (e: ScenarioRule) {
                            hint = e.message
                        }
                    }
                }
            }
        }
        ui.onNodeWithTag("category-item-10").performClick()
        ui.onNodeWithTag("category-need").performClick()
        ui.onNodeWithTag("category-item-90").performClick()
        ui.onNodeWithTag("category-want").performClick()
        tap("Проверить")
        ui.onNodeWithText("80 монет").assertIsDisplayed()
        repeat(3) { ui.onNodeWithContentDescription("Увеличить Нужно").performClick() }
        repeat(2) { ui.onNodeWithContentDescription("Увеличить Коплю").performClick() }
        tap("Проверить")
        tap("Цена 60")
        ui.onNodeWithText("Есть дешевле").assertIsDisplayed()
        tap("Цена 40")
        tap("Обед — 25")
        tap("Проверить")
        ui.runOnIdle {
            assertEquals(listOf("Верно", "Осталось 30", "Выгоднее", "Осталось 45"), results)
        }
    }
}
