package ru.findrug.domain

import kotlin.test.*

class SavingsWithdrawalTest {
    private fun planned() =
        ScenarioGame.plan(
            ScenarioGame.start(ScenarioState(profile = ScenarioProfile("Игрок"))),
            BudgetAmounts(70, 70, 60),
        )

    @Test
    fun `partial withdrawal returns savings to wallet and records one balance credit`() {
        val before =
            ScenarioGame.deposit(ScenarioGame.selectGoal(planned(), "house"), 60)
                .copy(actual = BudgetAmounts(40, 20, 60))

        val after = ScenarioGame.withdraw(before, 25)

        assertEquals(165, after.coins)
        assertEquals(35, after.savings)
        assertEquals(before.coins + before.savings, after.coins + after.savings)
        assertEquals(BudgetAmounts(40, 20, 35), after.actual)
        assertEquals(before.history + CoinEntry("Из копилки", 25, before.period), after.history)
        assertEquals(before.goalId, after.goalId)
        assertEquals(140, before.coins)
        assertEquals(60, before.savings)
    }

    @Test
    fun `all saved money can be withdrawn without a selected goal`() {
        val before = planned().copy(savings = 80)

        val after = ScenarioGame.withdraw(before, 80)

        assertEquals(280, after.coins)
        assertEquals(0, after.savings)
        assertEquals(-80, after.actual.save)
        assertNull(after.goalId)
        assertFailsWith<ScenarioRule> { ScenarioGame.withdraw(after, 1) }
    }

    @Test
    fun `invalid withdrawal amounts cannot change the state`() {
        val before = planned().copy(savings = 80)
        for (amount in listOf(Int.MIN_VALUE, -1, 0, 81, Int.MAX_VALUE)) {
            assertFailsWith<ScenarioRule>("Amount $amount must be rejected") {
                ScenarioGame.withdraw(before, amount)
            }
        }
        assertEquals(200, before.coins)
        assertEquals(80, before.savings)
        assertEquals(BudgetAmounts(), before.actual)
        assertEquals(listOf(CoinEntry("Стартовый бюджет", 200, 1)), before.history)
    }

    @Test
    fun `withdrawal requires a started open period and a confirmed plan`() {
        val before = planned().copy(savings = 80)
        for (state in
            listOf(
                before.copy(started = false),
                before.copy(plan = null),
                before.copy(periodClosed = true),
            )) {
            assertFailsWith<ScenarioRule> { ScenarioGame.withdraw(state, 10) }
        }
    }

    @Test
    fun `withdrawal locks the plan even after the same amount is redeposited`() {
        val before = ScenarioGame.selectGoal(planned().copy(savings = 80), "house")
        assertTrue(before.planEditable)

        val withdrawn = ScenarioGame.withdraw(before, 20)
        assertTrue(withdrawn.planLocked)
        assertFalse(withdrawn.planEditable)
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(withdrawn, BudgetAmounts()) }

        val restored = ScenarioGame.deposit(withdrawn, 20)
        assertEquals(0, restored.actual.save)
        assertTrue(restored.planLocked)
        assertFalse(restored.planEditable)
        assertFailsWith<ScenarioRule> { ScenarioGame.plan(restored, BudgetAmounts()) }
    }

    @Test
    fun `withdraw and redeposit cycles cannot earn saving credit from previous periods`() {
        val saved = ScenarioGame.deposit(ScenarioGame.selectGoal(planned(), "house"), 60)
        val closed =
            ScenarioGame.finish(
                ScenarioGame.reward(saved, 4, TaskAnswer.Comparison(listOf(1, 1, 2, 2, 1)), "first")
            )
        val before = ScenarioGame.plan(ScenarioGame.next(closed), BudgetAmounts(70, 70, 60))
        var state = before

        repeat(3) {
            state = ScenarioGame.withdraw(state, 60)
            assertEquals(-60, state.actual.save)
            state = ScenarioGame.deposit(state, 60)
            assertEquals(0, state.actual.save)
        }

        assertEquals(before.coins, state.coins)
        assertEquals(before.savings, state.savings)
        assertEquals(before.taskEarnings, state.taskEarnings)
        assertEquals(before.recoveryEarnings, state.recoveryEarnings)
        assertEquals(before.rewardClaims, state.rewardClaims)
        assertEquals(before.reports, state.reports)
        val report =
            ScenarioGame.finish(
                    ScenarioGame.reward(
                        state,
                        4,
                        TaskAnswer.Comparison(listOf(1, 1, 2, 2, 1)),
                        "second",
                    )
                )
                .reports
                .last()
        assertFalse(report.hasSavings)
        assertFalse(report.savingsPlanMet)
        assertFalse(report.planFollowed)
        assertFalse(report.successful)
        assertEquals(0, report.score)
    }

    @Test
    fun `withdrawing prior savings preserves earned reports goals and rewards`() {
        val reports =
            List(4) {
                PeriodReport(it + 1, BudgetAmounts(40, 20, 30), BudgetAmounts(40, 20, 30), true)
            }
        val before =
            planned()
                .copy(
                    savings = 80,
                    period = 5,
                    reports = reports,
                    collectedGoals = mapOf("house" to 1),
                    completedTasks = setOf(4),
                    rewardClaims = setOf("earned"),
                    taskEarnings = 20,
                    recoveryEarnings = 10,
                    periodTasks = 1,
                    paidRepeats = 1,
                )

        val after = ScenarioGame.withdraw(before, 80)

        assertEquals(
            before.copy(
                coins = 280,
                savings = 0,
                actual = BudgetAmounts(save = -80),
                planLocked = true,
                history = before.history + CoinEntry("Из копилки", 80, 5),
            ),
            after,
        )
        assertEquals(4, after.successfulPeriods)
        assertEquals(2, after.growthStage)
    }

    @Test
    fun `negative net saving can close a period and receives truthful feedback`() {
        val withdrawn = ScenarioGame.withdraw(planned().copy(savings = 80, periodTasks = 1), 25)

        val report = ScenarioGame.finish(withdrawn).reports.last()
        val feedback = ScenarioGame.feedback(report)

        assertEquals(-25, report.actual.save)
        assertFalse(report.successful)
        assertFalse(report.savingsPlanMet)
        assertFalse(feedback.strengths.any { it.contains("Ты отложил") })
        assertTrue(feedback.nextStep.contains("забрано на 25 монет больше"))
    }

    @Test
    fun `withdrawal cannot overflow wallet or net savings`() {
        val before = planned().copy(coins = Int.MAX_VALUE - 5, savings = 10)
        assertEquals(Int.MAX_VALUE, ScenarioGame.withdraw(before, 5).coins)
        assertFailsWith<ScenarioRule> { ScenarioGame.withdraw(before, 6) }
        assertFailsWith<ScenarioRule> {
            ScenarioGame.withdraw(
                before.copy(coins = 0, actual = BudgetAmounts(save = Int.MIN_VALUE)),
                1,
            )
        }
        val all =
            ScenarioGame.withdraw(before.copy(coins = 0, savings = Int.MAX_VALUE), Int.MAX_VALUE)
        assertEquals(Int.MAX_VALUE, all.coins)
        assertEquals(0, all.savings)
        assertEquals(-Int.MAX_VALUE, all.actual.save)
    }
}
