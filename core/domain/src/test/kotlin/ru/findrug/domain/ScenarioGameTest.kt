package ru.findrug.domain

import kotlin.test.*

class ScenarioGameTest {
    private fun started() = ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок")))

    private fun planned() = ScenarioGame.plan(started(), BudgetAmounts(70, 70, 60))

    @Test
    fun `start income can only be claimed once`() {
        val s = started()
        assertEquals(200, s.coins)
        assertEquals(s, ScenarioGame.start(s))
        assertEquals(listOf(CoinEntry("Стартовый бюджет", 200, 1)), s.history)
    }

    @Test
    fun `blank names and invalid customization are rejected`() {
        assertFailsWith<ScenarioRule> {
            ScenarioGame.profile(ScenarioState(), ScenarioProfile(" "))
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.profile(ScenarioState(), ScenarioProfile("Игрок", hat = 3))
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.profile(ScenarioState(), ScenarioProfile("Игрок", color = 3))
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.profile(ScenarioState(), ScenarioProfile("Игрок", clothes = -1))
        }
        for (hat in 0..2) for (accessory in 0..2) for (color in 0..2) for (clothes in 0..2) {
            val profile =
                ScenarioProfile(
                    "Игрок",
                    hat = hat,
                    accessory = accessory,
                    color = color,
                    clothes = clothes,
                )
            assertEquals(
                profile,
                ScenarioGame.profile(ScenarioState(ownedItems = setOf("cap")), profile).profile,
            )
        }
    }

    @Test
    fun `plan checks total and locks only after the first financial action`() {
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(started(), BudgetAmounts(100, 100, 10)) }
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(started(), BudgetAmounts(-10, 0, 0)) }
        assertEquals(
            BudgetAmounts(100, 20, 80),
            ScenarioGame.plan(planned(), BudgetAmounts(100, 20, 80)).plan,
        )
        assertFailsWith<ScenarioRule> {
            ScenarioGame.plan(ScenarioGame.buy(planned(), "food"), BudgetAmounts())
        }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.plan(
                ScenarioGame.deposit(ScenarioGame.selectGoal(planned(), "house"), 10),
                BudgetAmounts(),
            )
        }
        assertEquals(200, ScenarioGame.plan(started(), BudgetAmounts(100, 20, 80)).coins)
    }

    @Test
    fun `purchases require a plan and sufficient available coins`() {
        assertFailsWith<ScenarioRule> { ScenarioGame.buy(started(), "food") }
        val s = planned().copy(coins = 30, savings = 200, goalId = "house")
        assertFailsWith<ScenarioRule> { ScenarioGame.buy(s, "ball") }
        assertEquals(30, s.coins)
        assertEquals(200, s.savings)
        assertTrue(s.purchases.isEmpty())
    }

    @Test
    fun `purchase changes matching stat ledger and actual category`() {
        val s = ScenarioGame.buy(planned(), "food")
        assertEquals(160, s.coins)
        assertEquals(40, s.actual.need)
        assertEquals(95, s.satiety)
        assertEquals(listOf("food"), s.purchases)
        assertEquals(-40, s.history.last().amount)
        val toy = ScenarioGame.buy(s, "toy")
        assertEquals(60, toy.actual.want)
        assertEquals(100, toy.mood)
    }

    @Test
    fun `spending above plan is allowed when wallet covers it`() {
        val s = ScenarioGame.plan(started(), BudgetAmounts(100, 10, 90))
        assertEquals(140, ScenarioGame.buy(s, "toy").coins)
    }

    @Test
    fun `deposits remain separate and cannot exceed wallet or goal`() {
        val s = ScenarioGame.selectGoal(planned(), "house")
        assertFailsWith<ScenarioRule> { ScenarioGame.deposit(s, 201) }
        assertFailsWith<ScenarioRule> { ScenarioGame.deposit(s, 0) }
        val saved = ScenarioGame.deposit(s, 60)
        assertEquals(140, saved.coins)
        assertEquals(60, saved.savings)
        assertEquals(60, saved.actual.save)
        assertFailsWith<ScenarioRule> { ScenarioGame.deposit(saved.copy(savings = 299), 2) }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.selectGoal(saved.copy(savings = 450), "house")
        }
    }

    @Test
    fun `reward validates answer is idempotent and reduces repeats`() {
        val s = planned()
        assertFailsWith<ScenarioRule> { ScenarioGame.reward(s, 4, TaskAnswer.Choice(0), "bad") }
        val first = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "first")
        assertEquals(220, first.coins)
        assertEquals(first, ScenarioGame.reward(first, 4, TaskAnswer.Choice(1), "first"))
        val repeat = ScenarioGame.reward(first, 4, TaskAnswer.Choice(1), "repeat")
        assertEquals(230, repeat.coins)
        assertEquals(setOf(4), repeat.completedTasks)
    }

    @Test
    fun `training restores empty wallet without inflating curriculum progress`() {
        val s =
            ScenarioGame.reward(
                planned().copy(coins = 0),
                4,
                TaskAnswer.Choice(1),
                "training",
                true,
            )
        assertEquals(10, s.coins)
        assertTrue(s.completedTasks.isEmpty())
    }

    @Test
    fun `all six tasks validate their educational action`() {
        val valid =
            listOf(
                TaskAnswer.Categories(setOf(1, 2, 5)),
                TaskAnswer.Budget(BudgetAmounts(50, 20, 30)),
                TaskAnswer.Choice(80),
                TaskAnswer.Choice(2),
                TaskAnswer.Choice(1),
                TaskAnswer.Basket(setOf(0, 1)),
            )
        valid.forEachIndexed { id, answer ->
            assertTrue(ScenarioGame.answer(id, answer).isNotBlank())
        }
        assertFailsWith<ScenarioRule> { ScenarioGame.answer(0, TaskAnswer.Categories(setOf(0, 1))) }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.answer(1, TaskAnswer.Budget(BudgetAmounts(50, 40, 30)))
        }
        assertFailsWith<ScenarioRule> { ScenarioGame.answer(5, TaskAnswer.Basket(setOf(0, 1, 2))) }
        assertFailsWith<ScenarioRule> { ScenarioGame.answer(5, TaskAnswer.Basket(setOf(2))) }
    }

    @Test
    fun `period requires plan action and task and cannot close twice`() {
        assertFailsWith<ScenarioRule> { ScenarioGame.finish(started()) }
        assertFailsWith<ScenarioRule> { ScenarioGame.finish(planned()) }
        val s = ScenarioGame.buy(planned(), "food")
        assertFailsWith<ScenarioRule> { ScenarioGame.finish(s) }
        val closed = ScenarioGame.finish(ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "one"))
        assertEquals(closed, ScenarioGame.finish(closed))
        assertFailsWith<ScenarioRule> { ScenarioGame.buy(closed, "water") }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.reward(closed, 4, TaskAnswer.Choice(1), "two")
        }
    }

    @Test
    fun `five periods carry money savings and develop fox from decisions`() {
        var s = ScenarioGame.selectGoal(started().copy(demo = true), "bike")
        for (period in 1..5) {
            assertEquals(period, s.period)
            s = ScenarioGame.plan(s, BudgetAmounts(70, 20, 60))
            s = ScenarioGame.buy(s, "food")
            s = ScenarioGame.buy(s, "care")
            s = ScenarioGame.deposit(s, 60)
            s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "period-$period")
            s = ScenarioGame.finish(s)
            assertTrue(s.reports.last().successful)
            assertEquals(60 * period, s.savings)
            assertEquals(
                when {
                    period >= 4 -> "Знаток"
                    period >= 2 -> "Исследователь"
                    else -> "Малыш"
                },
                s.stage,
            )
            if (period < 5) {
                val balance = s.coins
                s = ScenarioGame.next(s)
                assertEquals(balance + ScenarioContent.incomes[period], s.coins)
                assertNull(s.plan)
                assertEquals(BudgetAmounts(), s.actual)
                assertEquals(0, s.periodTasks)
            }
        }
        assertEquals(5, s.reports.size)
        assertEquals(300, s.savings)
    }

    @Test
    fun `buying alone never grows fox and a later poor period never demotes it`() {
        assertEquals("Малыш", ScenarioGame.buy(planned(), "toy").stage)
        val reports = List(4) { PeriodReport(it + 1, BudgetAmounts(), BudgetAmounts(), true) }
        val s = planned().copy(reports = reports, period = 5)
        val closed =
            ScenarioGame.finish(
                ScenarioGame.reward(ScenarioGame.buy(s, "decor"), 4, TaskAnswer.Choice(1), "five")
            )
        assertFalse(closed.reports.last().successful)
        assertEquals("Знаток", closed.stage)
    }
}
