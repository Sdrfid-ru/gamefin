package ru.findrug.domain

import kotlin.test.*

class PeriodEconomyTest {
    private fun fresh() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
            BudgetAmounts(100, 100),
        )

    @Test
    fun `hundreds of new claims cannot mint unlimited money`() {
        var s = fresh()
        repeat(100) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "practice-$it", true) }
        assertEquals(350, s.coins)
        assertEquals(150, s.taskEarnings)
        assertEquals(15, s.paidRepeats)
        val first = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "first")
        assertEquals(350, first.coins)
        assertTrue(4 in first.completedTasks)
        assertEquals(first, ScenarioGame.reward(first, 4, TaskAnswer.Choice(1), "first"))
    }

    @Test
    fun `partial last reward cannot overshoot the shared allowance`() {
        val s = fresh().copy(taskEarnings = 145)
        assertEquals(5, ScenarioGame.rewardAmount(s, 1))
        val paid = ScenarioGame.reward(s, 1, TaskAnswer.Budget(BudgetAmounts(50, 20, 30)), "last")
        assertEquals(205, paid.coins)
        assertEquals(150, paid.taskEarnings)
        assertEquals(0, ScenarioGame.rewardAmount(paid, 4))
    }

    @Test
    fun `recovery reserve works once even after draining coins again`() {
        var s =
            fresh()
                .copy(coins = 0, taskEarnings = 150, actual = BudgetAmounts(100), planLocked = true)
        repeat(2) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "rescue-$it", true) }
        assertEquals(20, s.coins)
        assertEquals(20, s.recoveryEarnings)
        s = ScenarioGame.buy(s, "water")
        assertEquals(0, s.coins)
        repeat(30) { s = ScenarioGame.reward(s, 4, TaskAnswer.Choice(1), "free-$it", true) }
        assertEquals(0, s.coins)
        assertEquals(20, s.recoveryEarnings)
        val next = ScenarioGame.next(ScenarioGame.finish(s))
        assertEquals(220, next.coins)
        assertEquals(0, next.taskEarnings)
        assertEquals(0, next.recoveryEarnings)
        assertEquals(10, ScenarioGame.rewardAmount(next, 4, true))
    }

    @Test
    fun `reserve pays only missing coins regardless of earlier purchases`() {
        val s = fresh().copy(coins = 17, taskEarnings = 150)
        assertEquals(3, ScenarioGame.rewardAmount(s, 4, true))
        assertEquals(0, ScenarioGame.rewardAmount(s.copy(coins = 20), 4, true))
        assertEquals(3, ScenarioGame.rewardAmount(s.copy(actual = BudgetAmounts(200)), 4, true))
        assertEquals(0, ScenarioGame.rewardAmount(s, 4, false))
    }

    @Test
    fun `saved money can buy the catalog but balance still limits purchases`() {
        var s = fresh().copy(coins = 465)
        ScenarioContent.products.forEach { s = ScenarioGame.buy(s, it.id) }
        assertEquals(90, s.coins)
        assertEquals(375, s.actual.need + s.actual.want)
        assertFailsWith<ScenarioRule> { ScenarioGame.buy(s.copy(coins = 19), "water") }
    }

    @Test
    fun `planning uses available money including carried balance`() {
        val s = fresh().copy(coins = 465, period = 3)
        assertEquals(
            BudgetAmounts(140, 150, 175),
            ScenarioGame.plan(s, BudgetAmounts(140, 150, 175)).plan,
        )
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(s, BudgetAmounts(140, 150, 180)) }
    }

    @Test
    fun `wants warn about planned needs and savings without blocking choice`() {
        val s = ScenarioGame.plan(fresh(), BudgetAmounts(70, 30, 100))
        val toy = ScenarioContent.products.first { it.id == "toy" }
        assertTrue(PeriodEconomy.purchaseAdvice(s, toy)!!.contains("не хватит 30"))
        assertEquals(140, ScenarioGame.buy(s, toy.id).coins)
        assertNull(PeriodEconomy.purchaseAdvice(s.copy(coins = 300), toy))
        assertNull(PeriodEconomy.purchaseAdvice(s, ScenarioContent.products.first { it.need }))
    }
}
