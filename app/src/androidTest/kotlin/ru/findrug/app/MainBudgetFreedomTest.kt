package ru.findrug.app

import android.graphics.Bitmap
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

/** Exercises the actual budget screen, independently of the 100-coin learning exercise. */
class MainBudgetFreedomTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val storageName = "main-budget-freedom-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, storageName)

    @After
    fun clean() {
        context.deleteSharedPreferences(storageName)
    }

    private fun start(budget: Int = 200, existingPlan: BudgetAmounts? = null) {
        val state =
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок", "Рыжик")))
                .copy(coins = budget, plan = existingPlan, sound = false, animations = false)
        store.save(state)
        ui.setContent { FinDrugApp(storageName) }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
        tap("Бюджет")
        ui.onNodeWithText("План на период 1").assertIsDisplayed()
    }

    private fun tap(text: String) {
        ui.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    private fun increase(category: String, count: Int = 1) {
        repeat(count) {
            ui.onNodeWithContentDescription("Увеличить $category")
                .assertIsDisplayed()
                .performClick()
        }
    }

    private fun screenshot(name: String) {
        ui.waitForIdle()
        val file = File(context.getExternalFilesDir(null), "main-budget-freedom/$name.png")
        file.parentFile!!.mkdirs()
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }

    private fun confirm(expected: BudgetAmounts, budget: Int = 200, existing: Boolean = false) {
        val original = store.read()
        ui.onNodeWithText(if (existing) "Сохранить план" else "Подтвердить план")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
        ui.onNodeWithText("Проверь свой план").assertIsDisplayed()
        ui.onNodeWithText("Из новых монет свободно: ${budget - expected.total}").assertIsDisplayed()
        assertEquals(
            "The review must not silently confirm the plan",
            original.plan,
            store.read().plan,
        )
        tap("Продолжить")
        ui.waitUntil(30_000) { store.read().plan == expected }
        ui.onNodeWithText("Игрок").assertIsDisplayed()
        val saved = store.read()
        assertEquals(expected, saved.plan)
        assertEquals(budget, saved.coins)
        assertEquals(original.savings, saved.savings)
        assertEquals(original.actual, saved.actual)
        assertEquals(original.history, saved.history)
        assertTrue(saved.planEditable)
    }

    private fun allocateEverything(category: String, expected: BudgetAmounts) {
        start()
        increase(category, 20)
        ui.onNodeWithText("Весь бюджет распределён").assertIsDisplayed()
        if (category == "Хочу") screenshot("all-wants-plan")
        confirm(expected)
    }

    @Test fun allCoinsCanGoToNeeds() = allocateEverything("Нужно", BudgetAmounts(200, 0, 0))

    @Test
    fun allCoinsCanGoToWantsWithZeroNeedsAndSavings() =
        allocateEverything("Хочу", BudgetAmounts(0, 200, 0))

    @Test fun allCoinsCanGoToSavings() = allocateEverything("Коплю", BudgetAmounts(0, 0, 200))

    @Test
    fun aZeroPlanCanBeConfirmedWithTheEntireBudgetUnallocated() {
        start()
        ui.onNodeWithContentDescription("Уменьшить Нужно").performClick()
        ui.onNodeWithContentDescription("Уменьшить Хочу").performClick()
        ui.onNodeWithContentDescription("Уменьшить Коплю").performClick()
        ui.onNodeWithText("Осталось распределить: 200").assertIsDisplayed()
        screenshot("zero-plan")
        confirm(BudgetAmounts())
    }

    @Test
    fun aPartialPlanCanLeaveFreeCoinsWithoutFundingEveryCategory() {
        start()
        increase("Хочу", 7)
        ui.onNodeWithText("Осталось распределить: 130").assertIsDisplayed()
        screenshot("partial-wants-plan")
        confirm(BudgetAmounts(0, 70, 0))
    }

    @Test
    fun overspendingDisablesConfirmationUntilThePlayerReducesAnyCategory() {
        start()
        increase("Хочу", 20)
        increase("Нужно")
        ui.onNodeWithText("Подтвердить план").assertIsDisplayed().assertIsNotEnabled()
        ui.onNodeWithText("В плане лишние 10 монет. Уменьши сумму в любой категории.")
            .assertIsDisplayed()
        assertNull(store.read().plan)
        screenshot("over-budget")
        ui.onNodeWithContentDescription("Уменьшить Хочу").assertIsDisplayed().performClick()
        ui.onNodeWithText("Весь бюджет распределён").assertIsDisplayed()
        confirm(BudgetAmounts(10, 190, 0))
    }

    @Test
    fun theEditorCanCrossTenThousandWhenTheRealBudgetAllowsIt() {
        start(budget = 20_000, existingPlan = BudgetAmounts(9_990, 0, 0))
        increase("Нужно", 2)
        ui.onNodeWithText("10010").assertIsDisplayed()
        ui.onNodeWithText("Осталось распределить: 9990").assertIsDisplayed()
        confirm(BudgetAmounts(10_010, 0, 0), budget = 20_000, existing = true)
    }

    @Test
    fun anOverflowingDraftShowsItsTrueExcessAndCanBeCorrected() {
        start(budget = Int.MAX_VALUE, existingPlan = BudgetAmounts(Int.MAX_VALUE, 10, 0))
        ui.onNodeWithText("Сохранить план").assertIsDisplayed().assertIsNotEnabled()
        ui.onNodeWithText("В плане лишние 10 монет. Уменьши сумму в любой категории.")
            .assertIsDisplayed()
        ui.onNodeWithText("Уменьши сумму на 10").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Суммы в плане должны быть положительными или равны нулю.")
            .assertDoesNotExist()
        ui.onNodeWithContentDescription("Уменьшить Хочу")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        confirm(BudgetAmounts(Int.MAX_VALUE, 0, 0), budget = Int.MAX_VALUE, existing = true)
    }
}
