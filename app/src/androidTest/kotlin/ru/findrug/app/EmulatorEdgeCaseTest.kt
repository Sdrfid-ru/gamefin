package ru.findrug.app

import android.accessibilityservice.AccessibilityService
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

/** Extended emulator journeys, with an isolated profile for every test. */
class EmulatorEdgeCaseTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "emulator-audit-${UUID.randomUUID()}"
    private val store
        get() = ScenarioStore(context, name)

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    private fun planned() =
        ScenarioGame.plan(
                ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
                BudgetAmounts(40, 70, 60),
            )
            .copy(sound = false, animations = false)

    private fun start(state: ScenarioState = planned()): StateRestorationTester {
        store.save(state)
        val restoration = StateRestorationTester(ui)
        restoration.setContent { FinDrugApp(name) }
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText(if (state.profile == null) "Начать" else "Игрок")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        return restoration
    }

    private fun pressBack() {
        assertTrue(
            InstrumentationRegistry.getInstrumentation()
                .uiAutomation
                .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        )
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        ui.waitForIdle()
    }

    private fun closeSoftKeyboard() {
        ui.runOnIdle {
            val activity =
                ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED)
                    .first()
            activity
                .getSystemService(InputMethodManager::class.java)
                .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        ui.waitForIdle()
    }

    private fun tap(text: String) {
        if (!ui.mainClock.autoAdvance)
            ui.waitUntil(30_000) {
                ui.mainClock.advanceTimeBy(32)
                ui.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        ui.onNodeWithText(text).assertIsDisplayed().performClick()
        if (!ui.mainClock.autoAdvance) ui.mainClock.advanceTimeBy(100)
    }

    private fun back() {
        ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
    }

    private fun next() {
        ui.onNodeWithTag("page-next").assertIsDisplayed().performClick()
    }

    private fun notice() {
        ui.waitUntil(30_000) {
            ui.onAllNodesWithText("Продолжить").fetchSemanticsNodes().isNotEmpty()
        }
        tap("Продолжить")
    }

    private fun openTask(id: Int) {
        tap("Задания")
        repeat(id) { next() }
        ui.onNodeWithTag("task-$id").assertIsDisplayed().performClick()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("task-$id").fetchSemanticsNodes().isEmpty() }
    }

    private fun increase(category: String, times: Int) {
        repeat(times) { ui.onNodeWithContentDescription("Увеличить $category").performClick() }
    }

    private fun parents() {
        ui.waitUntil(30_000) {
            if (!ui.mainClock.autoAdvance) ui.mainClock.advanceTimeBy(32)
            ui.onAllNodesWithContentDescription("Настройки").fetchSemanticsNodes().isNotEmpty()
        }
        ui.onNodeWithContentDescription("Настройки").performClick()
        tap("Для взрослых")
        ui.onNodeWithText("Ответ").performTextInput("56")
        closeSoftKeyboard()
        tap("Войти")
        tap("Управление профилем")
    }

    private fun solveTask(id: Int) {
        when (id) {
            0 -> {
                for (item in 0..5) {
                    ui.onNodeWithTag("category-item-$item").performClick()
                    ui.onNodeWithTag(
                            if (item in setOf(1, 2, 5)) "category-need" else "category-want"
                        )
                        .performClick()
                }
                tap("Проверить")
            }
            1 -> {
                tap("Проверить")
                increase("Нужно", 5)
                increase("Хочу", 2)
                increase("Коплю", 3)
                tap("Проверить")
            }
            2 -> tap("30 монет")
            3 -> {
                tap("Откладывать по 30 каждый период")
            }
            4 -> {
                tap("1 упаковка — 50 🪙")
                tap("2 упаковки — 70 🪙")
            }
            5 -> {
                tap("🧸 Игрушка — 60")
                tap("Проверить")
                tap("Корм — 40")
                tap("Проверить")
            }
        }
        if (ui.onAllNodesWithTag("reflection-skip").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithTag("reflection-skip").performClick()
        }
    }

    @Test
    fun completedExerciseKeepsUnclaimedRewardAfterScreenRestoration() {
        val restoration = start()
        for (id in 0..5) {
            openTask(id)
            solveTask(id)
            ui.onNodeWithText("Получить награду").assertIsDisplayed()
            restoration.emulateSavedInstanceStateRestore()
            ui.onNodeWithText("Получить награду").assertIsDisplayed()
            tap("Получить награду")
            notice()
            back()
        }
        assertEquals(350, store.read().coins)
        assertEquals(6, store.read().periodTasks)
    }

    @Test
    fun systemBackMatchesVisibleBackButtonInNestedScreens() {
        start()
        openTask(4)
        pressBack()
        ui.waitUntil(10_000) { ui.onAllNodesWithTag("task-0").fetchSemanticsNodes().isNotEmpty() }
        ui.onNodeWithTag("task-0").assertIsDisplayed()
        back()
        ui.onNodeWithContentDescription("Настройки").performClick()
        tap("Словарик")
        pressBack()
        ui.onNodeWithText("Звуки лисёнка").assertIsDisplayed()
        tap("Для взрослых")
        pressBack()
        ui.onNodeWithText("Звуки лисёнка").assertIsDisplayed()
    }

    @Test
    fun demoResetExitAndCancelledDeletionKeepNormalProfileIntact() {
        val regular = planned().copy(coins = 123)
        start(regular)
        ui.mainClock.autoAdvance = false
        parents()
        tap("Начать демо")
        ui.waitUntil(30_000) { store.read().demo }
        assertEquals(200, store.read().coins)
        tap("Бюджет")
        tap("Подтвердить план")
        tap("Продолжить")
        parents()
        tap("Сбросить тестовый профиль")
        tap("Пока нет")
        assertNotNull(store.read().plan)
        tap("Сбросить тестовый профиль")
        tap("Подтвердить")
        ui.waitUntil(30_000) { store.read().plan == null }
        parents()
        tap("Выйти из Demo Mode")
        ui.waitUntil(30_000) { !store.read().demo }
        assertEquals(regular, store.read())
        parents()
        tap("Удалить локальный профиль")
        tap("Пока нет")
        assertEquals(regular, store.read())
        tap("Удалить локальный профиль")
        tap("Подтвердить")
        ui.waitUntil(30_000) {
            ui.mainClock.advanceTimeBy(32)
            ui.onAllNodesWithText("Начать").fetchSemanticsNodes().isNotEmpty()
        }
        assertNull(store.read().profile)
        assertNull(store.exitDemo().profile)
    }

    @Test
    fun newPlayerCompletesAllExercisesFivePeriodsAndReceivesGoal() {
        start(ScenarioState(sound = false, animations = false))
        tap("Начать")
        ui.onNodeWithText("Продолжить").assertIsNotEnabled()
        ui.onNodeWithText("Игровое имя").performTextInput("Игрок")
        closeSoftKeyboard()
        tap("Продолжить")
        next()
        ui.onNodeWithText("Как его зовут?").performTextReplacement("Пушок")
        closeSoftKeyboard()
        tap("Сохранить")
        tap("Получить")
        notice()
        assertEquals(200, store.read().coins)
        tap("Завершить")
        tap("Понятно")
        assertFalse(store.read().periodClosed)
        tap("Выбери свою цель")
        tap("Выбрать цель")
        notice()
        back()
        for (period in 1..5) {
            if (period == 1) tap("Бюджет")
            increase("Нужно", 7)
            if (period == 1) increase("Хочу", 7)
            increase("Коплю", 6)
            tap("Подтвердить план")
            tap("Продолжить")
            tap("Магазин")
            tap("40 🪙")
            tap("Пока нет")
            val before = store.read().coins
            tap("40 🪙")
            tap("Подтвердить")
            notice()
            assertEquals(before - 40, store.read().coins)
            next()
            tap("30 🪙")
            tap("Подтвердить")
            notice()
            if (period == 1) {
                tap("Хочу")
                next()
                next()
                tap("70 🪙")
                tap("Подтвердить")
                notice()
                assertTrue("cap" in store.read().ownedItems)
            }
            back()
            tap("Домик для лисёнка")
            increase("Отложить", 3)
            tap("Отложить 60 монет")
            tap("Подтвердить")
            notice()
            back()
            assertEquals(period * 60, store.read().savings)
            for (id in if (period == 1) 0..5 else 4..4) {
                openTask(id)
                solveTask(id)
                tap("Получить награду")
                notice()
                back()
            }
            assertEquals(listOf(150, 250, 370, 510, 670)[period - 1], store.read().coins)
            tap("Завершить")
            ui.waitUntil(30_000) { store.read().reports.size == period }
            assertEquals(period, store.read().reports.size)
            assertTrue(store.read().reports.last().successful)
            tap("Дальше")
            tap("Дальше")
            if (period < 5) {
                tap("Начать следующий период")
                notice()
            }
        }
        assertEquals(2, store.read().growthStage)
        assertEquals((0..5).toSet(), store.read().completedTasks)
        back()
        tap("Домик для лисёнка")
        tap("Получить цель")
        tap("Подтвердить")
        notice()
        assertEquals(0, store.read().savings)
        assertEquals(1, store.read().collectedGoals["house"])
        assertEquals(670, store.read().coins)
        assertEquals("Пушок", store.read().profile!!.pet)
        assertEquals(store.read(), ScenarioStore(context, name).read())
    }
}
