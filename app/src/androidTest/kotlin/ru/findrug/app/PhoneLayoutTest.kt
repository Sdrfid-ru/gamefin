package ru.findrug.app

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import ru.findrug.app.data.ScenarioStore
import ru.findrug.domain.*

/** 360 × 640 dp emulator. A hidden action or an overflowing normal-size body fails the test. */
class PhoneLayoutTest {
    @get:Rule val ui = createComposeRule()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "layout-test-${UUID.randomUUID()}"

    @After
    fun clean() {
        context.deleteSharedPreferences(name)
    }

    private fun planned() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
            BudgetAmounts(40, 70, 90),
        )

    private fun start(s: ScenarioState = planned(), fontScale: Float = 1f) {
        ScenarioStore(context, name).save(s.copy(sound = false, animations = false))
        ui.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                FinDrugApp(name)
            }
        }
        ui.waitUntil(30_000) { ui.onAllNodesWithText("Игрок").fetchSemanticsNodes().isNotEmpty() }
    }

    private fun tap(text: String) {
        ui.onNodeWithText(text).assertIsDisplayed().performClick()
    }

    private fun next() {
        ui.onNodeWithTag("page-next").assertIsDisplayed().performClick()
    }

    private fun back() {
        ui.onNodeWithContentDescription("Назад").assertIsDisplayed().performClick()
    }

    private fun pageTo(tag: String) {
        repeat(8) {
            if (ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()) return
            next()
        }
        fail("Missing $tag")
    }

    private fun fits(label: String) {
        ui.waitForIdle()
        val body = ui.onNodeWithTag("page-content").fetchSemanticsNode()
        val scroll = body.config[SemanticsProperties.VerticalScrollAxisRange].maxValue()
        assertEquals("$label needs vertical scrolling ($scroll px)", 0f, scroll, 1f)
        val footer = ui.onNodeWithTag("page-actions").fetchSemanticsNode().boundsInRoot
        if (footer.height > 0f)
            assertTrue("$label footer overlaps content", footer.top >= body.boundsInRoot.bottom)
        ui.onNodeWithTag("bottom-navigation").assertIsDisplayed()
        val navigation = ui.onNodeWithTag("bottom-navigation").fetchSemanticsNode().boundsInRoot
        assertTrue("$label navigation overlaps actions", navigation.top >= footer.bottom)
        listOf("home", "pet", "progress", "achievements", "profile").forEach {
            ui.onNodeWithTag("nav-$it").assertIsDisplayed()
        }
    }

    private fun action(text: String) {
        ui.onNode(hasText(text) and hasAnyAncestor(hasTestTag("page-actions"))).assertIsDisplayed()
    }

    private fun shot(name: String) {
        val bitmap = ui.onRoot().captureToImage().asAndroidBitmap()
        val file = java.io.File(context.getExternalFilesDir(null), "layout-v7/$name.png")
        file.parentFile!!.mkdirs()
        file.outputStream().use {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    @Test
    fun bottomMenuNavigatesFromExercisesAndSettingsAndHighlightsCurrentSection() {
        start()
        listOf("pet", "progress", "achievements", "profile", "home").forEach {
            ui.onNodeWithTag("nav-$it").performClick().assertIsSelected()
        }
        tap("Задания")
        ui.onNodeWithTag("task-0").performClick()
        fits("task with bottom menu")
        ui.onNodeWithTag("nav-pet").performClick().assertIsSelected()
        action("Сохранить")
        shot("09-pet-bottom-menu")
        ui.onNodeWithTag("nav-home").performClick()
        ui.onNodeWithContentDescription("Настройки").performClick()
        tap("Как играть?")
        fits("help with bottom menu")
        ui.onNodeWithTag("nav-profile").assertIsSelected()
        shot("10-help-bottom-menu")
        ui.onNodeWithTag("nav-home").performClick()
        ui.onNodeWithText("Игрок").assertExists()
        assertEquals(
            BudgetAmounts(40, 70, 90),
            kotlinx.coroutines.runBlocking { ScenarioStore(context, name).read().plan },
        )
    }

    @Test
    fun allTaskShopGoalAndRecoveryPagesFitWithoutScrolling() {
        start(planned().copy(coins = 0))
        tap("Давай заработаем новые")
        for (id in ScenarioContent.trainingTasks) {
            fits("recovery $id")
            ui.onNodeWithTag("training-$id").assertIsDisplayed()
            if (id != 1) next()
        }
        shot("01-recovery")
        back()
        tap("Задания")
        for (id in 0..5) {
            fits("task list $id")
            ui.onNodeWithTag("task-$id").assertIsDisplayed()
            if (id < 5) next()
        }
        ui.onNodeWithTag("page-next").assertIsNotEnabled()
        tap("Тренировка")
        for ((i, id) in ScenarioContent.trainingTasks.withIndex()) {
            fits("training list $id")
            ui.onNodeWithTag("task-$id").assertIsDisplayed()
            if (i < 2) next()
        }
        back()
        tap("Магазин")
        for (category in listOf("Нужно", "Хочу")) {
            tap(category)
            for (i in 0..3) {
                fits("shop $category $i")
                if (i < 3) next()
            }
        }
        shot("02-shop")
        back()
        ui.onAllNodesWithText("Копилка")[0].assertIsDisplayed().performClick()
        repeat(3) {
            fits("goal $it")
            action("Выбрать цель")
            shot("goal-$it")
            if (it < 2) next()
        }
        shot("03-goals")
        tap("Выбрать цель")
        tap("Продолжить")
        fits("active savings, insufficient balance")
        action("Отложить 30 монет")
    }

    @Test
    fun everyExerciseAndItsPrimaryActionsFitEvenAfterSortingErrors() {
        start()
        tap("Задания")
        for (id in 0..5) {
            pageTo("task-$id")
            ui.onNodeWithTag("task-$id").performClick()
            fits("exercise $id")
            if (id == 0) {
                shot("sort-unassigned")
                for (item in 0..5) {
                    ui.onNodeWithTag("category-item-$item").assertIsDisplayed().performClick()
                    ui.onNodeWithTag("category-want").assertIsDisplayed().performClick()
                }
                fits("all six items in one category")
                tap("Проверить")
                fits("sorting errors")
                action("Сбросить")
                action("Проверить")
                shot("04-sort")
                tap("Подсказка")
                tap("Понятно")
                tap("Сбросить")
            }
            if (id == 1) {
                action("Проверить")
                tap("Проверить")
                fits("budget error")
            }
            if (id == 4) {
                tap("1 упаковка — 50 🪙")
                fits("comparison error")
                tap("2 упаковки — 70 🪙")
                fits("reflection question")
                ui.onNodeWithTag("reflection-option-1").assertIsDisplayed().performClick()
                fits("reflection explanation")
                ui.onNodeWithTag("reflection-done").assertIsDisplayed().performClick()
                fits("reward")
                action("Получить награду")
                shot("05-reward")
            }
            back()
        }
    }

    @Test
    fun budgetReviewAndAllResultStepsKeepTheirActionsVisible() {
        var s = ScenarioGame.buy(planned(), "food")
        s = ScenarioGame.deposit(ScenarioGame.selectGoal(s, "house"), 30)
        s = ScenarioGame.finish(ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "result"))
        start(s)
        tap("Итоги")
        fits("result table")
        action("Дальше")
        tap("Дальше")
        fits("result explanation")
        shot("06-result-feedback")
        tap("Дальше")
        fits("result growth")
        action("Начать следующий период")
        tap("Начать следующий период")
        tap("Продолжить")
        fits("budget editor")
        action("Подтвердить план")
        tap("Подтвердить план")
        fits("budget review")
        action("Продолжить")
        action("Изменить план")
        shot("07-budget-review")
    }

    @Test
    fun longHistorySettingsHelpAndAdultReportsArePaged() {
        val reports =
            (1..12).map {
                PeriodReport(it, BudgetAmounts(40, 30, 30), BudgetAmounts(40, 20, 30), true)
            }
        val history = (0..59).map { CoinEntry("Операция $it", 10, 1) }
        start(
            planned()
                .copy(
                    history = history,
                    reports = reports,
                    collectedGoals = mapOf("house" to 1, "bike" to 1, "telescope" to 1),
                )
        )
        tap("Профиль")
        fits("profile")
        repeat(20) {
            next()
            fits("history $it")
        }
        ui.onNodeWithText("Операция 0").assertIsDisplayed()
        ui.onNodeWithTag("page-next").assertIsNotEnabled()
        back()
        tap("Прогресс")
        fits("progress")
        next()
        fits("growth stages")
        tap("Моя коллекция")
        repeat(3) {
            fits("collection $it")
            if (it < 2) next()
        }
        back()
        tap("Достижения")
        repeat(3) {
            fits("achievements $it")
            if (it < 2) next()
        }
        back()
        tap("Питомец")
        fits("appearance")
        tap("Шапки")
        fits("locked cap explanation")
        next()
        fits("names")
        action("Сохранить")
        back()
        ui.onNodeWithContentDescription("Настройки").performClick()
        fits("settings")
        tap("Как играть?")
        repeat(7) {
            fits("help $it")
            if (it < 6) next()
        }
        tap("Понятно")
        tap("Словарик")
        repeat(5) {
            fits("dictionary $it")
            if (it < 4) next()
        }
        back()
        tap("Для взрослых")
        fits("gate")
        ui.onNodeWithText("Ответ").performTextInput("56")
        tap("Войти")
        repeat(15) {
            fits("adult page $it")
            if (it < 14) next()
        }
    }

    @Test
    fun enlargedTextStillLeavesBudgetActionsOnScreen() {
        start(fontScale = 1.5f)
        tap("Бюджет")
        action("Сохранить план")
        tap("Сохранить план")
        action("Продолжить")
        action("Изменить план")
        shot("08-large-text")
    }
}
