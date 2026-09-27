package ru.findrug.domain

import kotlin.test.*

class ScenarioJourneyTest {
    private fun fresh() = ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок")))

    private fun planned() = ScenarioGame.plan(fresh(), BudgetAmounts(40, 70, 90))

    @Test
    fun `rewards do not freeze plan but spending and saving do`() {
        val earned = ScenarioGame.reward(planned(), 4, TaskAnswer.Choice(1), "task")
        val changed = ScenarioGame.plan(earned, BudgetAmounts(50, 70, 100))
        assertEquals(220, changed.coins)
        assertEquals(BudgetAmounts(), changed.actual)
        assertTrue(changed.planEditable)
        assertFalse(ScenarioGame.buy(changed, "water").planEditable)
        assertFalse(ScenarioGame.deposit(ScenarioGame.selectGoal(changed, "bike"), 10).planEditable)
        assertFailsWith<ScenarioRule> {
            ScenarioGame.plan(changed.copy(periodClosed = true), BudgetAmounts())
        }
    }

    @Test
    fun `locked cap requires ownership and purchase equips it once`() {
        assertFailsWith<ScenarioRule> {
            ScenarioGame.profile(fresh(), ScenarioProfile("Игрок", hat = 1))
        }
        val bought = ScenarioGame.buy(planned(), "cap")
        assertEquals(130, bought.coins)
        assertEquals(1, bought.profile!!.hat)
        assertTrue("cap" in bought.ownedItems)
        assertEquals(70, bought.actual.want)
        assertFailsWith<ScenarioRule> { ScenarioGame.buy(bought, "cap") }
        val off = ScenarioGame.profile(bought, bought.profile!!.copy(hat = 0))
        assertEquals(1, ScenarioGame.profile(off, off.profile!!.copy(hat = 1)).profile!!.hat)
        val decor = ScenarioGame.buy(off, "decor")
        assertTrue("decor" in decor.ownedItems)
        assertFailsWith<ScenarioRule> { ScenarioGame.buy(decor, "decor") }
    }

    @Test
    fun `goal receipt spends savings once preserves plan fact and unlocks next dream`() {
        val s = ScenarioGame.selectGoal(planned().copy(coins = 800), "bike")
        assertFailsWith<ScenarioRule> { ScenarioGame.collectGoal(s, "bike") }
        val funded = ScenarioGame.deposit(s, 600)
        assertFailsWith<ScenarioRule> { ScenarioGame.collectGoal(funded, "house") }
        val collected = ScenarioGame.collectGoal(funded, "bike")
        assertEquals(funded.coins, collected.coins)
        assertEquals(0, collected.savings)
        assertNull(collected.goal)
        assertEquals(1, collected.collectedGoals["bike"])
        assertEquals(funded.actual, collected.actual)
        assertEquals("savings", collected.history.last().account)
        assertEquals(-600, collected.history.last().amount)
        assertFailsWith<ScenarioRule> { ScenarioGame.collectGoal(collected, "bike") }
        assertEquals(300, ScenarioGame.selectGoal(collected, "house").goal!!.price)
        assertEquals(1200, ScenarioGame.selectGoal(collected, "bike").goal!!.price)
    }

    @Test
    fun `collection survives changed goal and allows upgrades after all three goals`() {
        var s = planned().copy(coins = 5000)
        for (id in listOf("house", "telescope", "bike", "house")) {
            s = ScenarioGame.selectGoal(s, id)
            s = ScenarioGame.deposit(s, s.goal!!.price)
            s = ScenarioGame.collectGoal(s, id)
            assertEquals(0, s.savings)
        }
        assertEquals(mapOf("house" to 2, "telescope" to 1, "bike" to 1), s.collectedGoals)
        assertEquals(900, ScenarioGame.selectGoal(s, "house").goal!!.price)
        assertEquals(3050, s.coins)
        assertEquals(1950, s.actual.save)
    }

    @Test
    fun `receiving a previously funded goal before planning does not block new period`() {
        val s = fresh().copy(savings = 300, goalId = "house")
        val got = ScenarioGame.collectGoal(s, "house")
        assertTrue(got.planEditable)
        assertNotNull(ScenarioGame.plan(got, BudgetAmounts(50, 50, 100)).plan)
        val closed = ScenarioGame.collectGoal(s.copy(periodClosed = true), "house")
        assertTrue(closed.periodClosed)
        assertEquals(1, closed.collectedGoals["house"])
    }

    @Test
    fun `repeats and first completions share a finite budget`() {
        var s = planned()
        repeat(12) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "repeat-$it", true) }
        assertEquals(320, s.coins)
        assertEquals(12, s.paidRepeats)
        assertEquals(12, s.periodTasks)
        assertTrue(s.completedTasks.isEmpty())
        val first = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "first")
        assertEquals(340, first.coins)
        assertEquals(first, ScenarioGame.reward(first, 4, TaskAnswer.Choice(1), "first"))
        val capped = ScenarioGame.reward(first, 4, TaskAnswer.Choice(1), "another")
        assertEquals(350, capped.coins)
        assertEquals(0, ScenarioGame.rewardAmount(capped, 4, true))
        assertEquals(350, ScenarioGame.reward(capped, 4, TaskAnswer.Choice(1), "free", true).coins)
        val next = ScenarioGame.next(ScenarioGame.finish(ScenarioGame.buy(first, "food")))
        assertEquals(0, next.paidRepeats)
        assertEquals(10, ScenarioGame.rewardAmount(next, 4, true))
        assertTrue(next.planEditable)
    }

    @Test
    fun `zero wallet with available rewards can recover and finish`() {
        var s = planned().copy(coins = 0, completedTasks = (0..5).toSet(), paidRepeats = 100)
        repeat(2) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "rescue-$it", true) }
        s = ScenarioGame.buy(s, "water")
        assertEquals(0, s.coins)
        s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "third", true)
        assertEquals(10, s.coins)
        s = ScenarioGame.deposit(ScenarioGame.selectGoal(s, "house"), 10)
        assertEquals(0, s.coins)
        assertEquals(103, s.paidRepeats)
        assertTrue(ScenarioGame.finish(s).periodClosed)
        assertTrue(ScenarioGame.next(ScenarioGame.finish(s)).coins > 0)
    }

    @Test
    fun `five period journey grows pet buys cap collects goal and continues saving`() {
        var s = ScenarioGame.selectGoal(fresh(), "house")
        for (period in 1..5) {
            s = ScenarioGame.plan(s, BudgetAmounts(70, if (period == 1) 70 else 0, 60))
            s = ScenarioGame.buy(s, "food")
            s = ScenarioGame.buy(s, "care")
            if (period == 1) s = ScenarioGame.buy(s, "cap")
            s = ScenarioGame.deposit(s, 60)
            s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "period-$period")
            s = ScenarioGame.finish(s)
            assertEquals(
                when {
                    period >= 4 -> 2
                    period >= 2 -> 1
                    else -> 0
                },
                s.growthStage,
            )
            if (period < 5) s = ScenarioGame.next(s)
        }
        assertEquals(300, s.savings)
        s = ScenarioGame.collectGoal(s, "house")
        assertEquals(1, s.collectedGoals["house"])
        s = ScenarioGame.selectGoal(ScenarioGame.next(s), "bike")
        s = ScenarioGame.plan(s, BudgetAmounts(40, 0, 100))
        s = ScenarioGame.deposit(s, 100)
        assertEquals(100, s.savings)
        assertEquals(600, s.goal!!.price)
        assertEquals(2, s.growthStage)
        assertTrue("cap" in s.ownedItems)
    }
}
